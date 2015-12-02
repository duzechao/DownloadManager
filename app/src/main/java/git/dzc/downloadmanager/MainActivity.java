package git.dzc.downloadmanager;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;

import git.dzc.downloadmanagerlib.download.DownloadManager;
import git.dzc.downloadmanagerlib.download.DownloadTask;
import git.dzc.downloadmanagerlib.download.DownloadTaskListener;

public class MainActivity extends AppCompatActivity {

    private TextView tv1;
    private TextView tv2;
    private TextView tv3;
    private TextView tv4;
    private DownloadManager downloadManager;
    private Handler handler;

    private List<String> taskIds = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();
        downloadManager = DownloadManager.getInstance(getApplicationContext());
        initView();

    }


    private void initView(){
        tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        tv3 = (TextView) findViewById(R.id.tv3);

        tv1.setOnClickListener(listener);
        tv2.setOnClickListener(listener);
        tv3.setOnClickListener(listener);

        tv4 = (TextView) findViewById(R.id.tv4);
        tv4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!taskIds.isEmpty()){
                    Intent intent = new Intent(MainActivity.this,Main2Activity.class);
                    intent.putExtra("taskId",taskIds.get(0));
                    startActivity(intent);
                }else{
                    Toast.makeText(MainActivity.this,"请先开始一个下载",Toast.LENGTH_LONG).show();
                }
            }
        });

    }
    private String getUrl(View v){
        String url = "";
        switch (v.getId()){
            case R.id.tv1:
                url = "http://img2.3lian.com/2014/f4/102/d/89.jpg";
                break;
            case R.id.tv2:
                url = "http://img3.3lian.com/2013/s1/65/d/109.jpg";
                break;
            case R.id.tv3:
                url = "http://www.qqya.com/qqyaimg/allimg/140909/094F643R-8.jpg";
                break;
        }
        return url;
    }
    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            download(getUrl(v), (TextView) v);
        }
    };

    private void download(String url, final TextView tv){
        tv.setClickable(false);
        DownloadTask task = new DownloadTask();
        String id = MD5.MD5(url);
        taskIds.add(id);
        task.setId(id);
        task.setSaveDirPath(getExternalCacheDir().getPath() + "/");
        task.setFileName(id+".jpg");
        task.setUrl(url);
        downloadManager.addDownloadTask(task, new DownloadTaskListener() {
            @Override
            public void onPrepare(DownloadTask downloadTask) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText("prepare");
                        tv.setClickable(true);
                    }
                });
            }

            @Override
            public void onStart(DownloadTask downloadTask) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText("start");
                        tv.setClickable(true);
                    }
                });
            }

            @Override
            public void onDownloading(final DownloadTask downloadTask) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText(downloadTask.getPercent()+"%       ");
                    }
                });
            }

            @Override
            public void onPause(DownloadTask downloadTask) {

            }

            @Override
            public void onCompleted(DownloadTask downloadTask) {

            }

            @Override
            public void onError(DownloadTask downloadTask, int errorCode) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText("error");
                        tv.setClickable(true);
                    }
                });
            }
        });

    }

}
