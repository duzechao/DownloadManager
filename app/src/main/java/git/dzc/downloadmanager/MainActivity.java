package git.dzc.downloadmanager;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import git.dzc.downloadmanagerlib.download.DownloadManager;
import git.dzc.downloadmanagerlib.download.DownloadStatus;
import git.dzc.downloadmanagerlib.download.DownloadTask;
import git.dzc.downloadmanagerlib.download.DownloadTaskListener;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "DownloadManager";
    private TextView tv1;
    private TextView tv2;
    private TextView tv3;
    private TextView tv4;
    private TextView tv5;

    private Button btControl;
    private Button btCancel;
    private String taskId4;

    private List<String> taskIds = new ArrayList<>();

    private DownloadManager downloadManager;
    private Handler handler;
    NotificationManager mNotificationManager;
    HashMap<Integer, NotificationCompat.Builder> notificationHashMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();
        downloadManager = DownloadManager.getInstance(getApplicationContext());

        downloadManager = DownloadManager.getInstance(getApplicationContext());

        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        initView();

    }


    private void initView() {
        tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        tv3 = (TextView) findViewById(R.id.tv3);

        tv1.setOnClickListener(listener);
        tv2.setOnClickListener(listener);
        tv3.setOnClickListener(listener);

        tv5 = (TextView) findViewById(R.id.tv5);
        tv5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!taskIds.isEmpty()) {
                    Intent intent = new Intent(MainActivity.this, Main2Activity.class);
                    intent.putExtra("taskId", taskIds.get(0));
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "请先开始一个下载", Toast.LENGTH_LONG).show();
                }
            }
        });

        tv4 = (TextView) findViewById(R.id.tv4);
        tv4.setOnClickListener(listener);
        btControl = (Button) findViewById(R.id.bt_control);
        btCancel = (Button) findViewById(R.id.bt_cancel);
        btCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv4.setEnabled(true);
                tv4.setText("download4");
                btCancel.setVisibility(View.INVISIBLE);
                btControl.setVisibility(View.INVISIBLE);
                downloadManager.cancel(taskId4);
            }
        });
        btControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadTask downloadTask = downloadManager.getCurrentTaskById(taskId4);
                if(downloadTask.getDownloadStatus()== DownloadStatus.DOWNLOAD_STATUS_PAUSE){
                    downloadManager.resume(taskId4);
                }else{
                    downloadManager.pause(taskId4);
                }
            }
        });

    }

    private void finishedDownload(int notifyId) {
        notificationHashMap.get(notifyId).setContentText("Finished.");
        notificationHashMap.get(notifyId).setSmallIcon(android.R.drawable.stat_sys_download_done);

        mNotificationManager.notify(notifyId, notificationHashMap.get(notifyId).build());

    }

    private void updateNotification(String fileTitle, int progress, long totalBytes, int notifyId) {


        notificationHashMap.get(notifyId).setContentText(getBytesDownloaded(progress, totalBytes));
        notificationHashMap.get(notifyId).setContentTitle(progress + "% | " + fileTitle);
        notificationHashMap.get(notifyId).setProgress(100, progress, false);
        mNotificationManager.notify(notifyId, notificationHashMap.get(notifyId).build());
    }

    private void setNotification(String title, String content, int notifyId) {

        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_sys_download);


        mNotificationManager.notify(
                notifyId,
                mNotifyBuilder.build());

        notificationHashMap.put(notifyId, mNotifyBuilder);
    }


    private String getUrl(View v){
        String url = "";
        switch (v.getId()){
            case R.id.tv1:
                url = "http://cdn0.sbnation.com/assets/3400207/DSC00845.JPG";
                break;
            case R.id.tv2:
                url = "http://g-ecx.images-amazon.com/images/S/amazon-dp.dpreview.com/sample_galleries/sony_a7r/2814738.jpg";
                break;
            case R.id.tv3:
            case R.id.tv4:
                url = "http://ac-nf6zosq5.clouddn.com/2c0298999ade5d6e.epub";
                break;
        }
        return url;
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            download(getUrl(v), (TextView) v);
            v.setEnabled(false);
        }
    };

    int counter = 1001;

    private void download(String url, final TextView tv) {
        tv.setEnabled(false);
        DownloadTask task = new DownloadTask();
        if(tv.getId()==R.id.tv4){
            taskId4 = counter+"";
        }
        taskIds.add(counter + "");
        task.setId(counter + ""); counter ++;
        task.setSaveDirPath(getExternalCacheDir().getPath() + "/");
        task.setFileName(counter +url.substring(url.lastIndexOf('.')-1));

        task.setUrl(url);
        downloadManager.addDownloadTask(task, new DownloadTaskListener() {
            @Override
            public void onPrepare(final DownloadTask downloadTask) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        btControl.setVisibility(View.VISIBLE);
                        btCancel.setVisibility(View.VISIBLE);
                        tv.setText("preparing ");

                        setNotification(downloadTask.getFileName(),
                                "Will start to download counter " + counter, Integer.parseInt(downloadTask.getId()));

                    }
                });
                Log.d(TAG,"onPrepare");
            }

            @Override
            public void onStart(final DownloadTask downloadTask) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText("start");
                    }
                });
                Log.d(TAG,"onStart");

            }

            @Override
            public void onDownloading(final DownloadTask downloadTask) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText((int) downloadTask.getPercent() + "%       ");

                        updateNotification(downloadTask.getFileName(),
                                (int) downloadTask.getPercent(), downloadTask.getToolSize(), Integer.parseInt(downloadTask.getId()));

                    }
                });
                Log.d(TAG,"onDownloading");
            }

            @Override
            public void onPause(DownloadTask downloadTask) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        btControl.setText("resume");
                        Toast.makeText(getApplicationContext(),"onPause",Toast.LENGTH_LONG).show();
                    }
                });
                Log.d(TAG,"onPause");

            }

            @Override
            public void onCancel(DownloadTask downloadTask) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        btControl.setVisibility(View.INVISIBLE);
                        btCancel.setVisibility(View.INVISIBLE);
                        tv4.setText("download4");
                        tv4.setEnabled(true);
                        Toast.makeText(getApplicationContext(),"onCancel",Toast.LENGTH_LONG).show();
                    }
                });
                Log.d(TAG,"onCancel");

            }

            @Override
            public void onCompleted(final DownloadTask downloadTask) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText("finished");
                        finishedDownload(Integer.parseInt(downloadTask.getId()));

                    }
                });
                Log.d(TAG,"onCompleted");

            }

            @Override
            public void onError(DownloadTask downloadTask, int errorCode) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText("error");

                    }
                });
                Log.d(TAG,"onError");

            }
        });

    }


    private String getBytesDownloaded(int progress, long totalBytes) {
        //Greater than 1 MB
        long bytesCompleted = (progress * totalBytes) / 100;
        if (totalBytes >= 1000000) {
            return ("" + (String.format(Locale.ENGLISH, "%.1f",
                    (float) bytesCompleted / 1000000)) + "/" + (String.format(Locale.ENGLISH,
                    "%.1f", (float) totalBytes / 1000000)) + " MB");
        }
        if (totalBytes >= 1000) {
            return ("" + (String.format(Locale.ENGLISH, "%.1f",
                    (float) bytesCompleted / 1000)) + "/" + (String.format(Locale.ENGLISH,
                    "%.1f", (float) totalBytes / 1000)) + " Kb");

        } else {
            return ("" + bytesCompleted + "/" + totalBytes);
        }
    }

}
