package git.dzc.downloadmanager;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import git.dzc.downloadmanagerlib.download.DownloadManager;
import git.dzc.downloadmanagerlib.download.DownloadTask;
import git.dzc.downloadmanagerlib.download.DownloadTaskListener;

public class Main2Activity extends AppCompatActivity {

    private static final String TAG = "Main2Activity";
    private TextView textView;
    private DownloadManager downloadManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        textView = (TextView) findViewById(R.id.tv);

        String taskId = getIntent().getStringExtra("taskId");
        downloadManager = DownloadManager.getInstance(getApplicationContext());
        DownloadTask downloadTask = downloadManager.getTaskById(taskId);
        downloadManager.addDownloadListener(downloadTask, new DownloadTaskListener() {
            @Override
            public void onPrepare(DownloadTask downloadTask) {
                Log.d(TAG,"onPrepare");
            }

            @Override
            public void onStart(DownloadTask downloadTask) {
                Log.d(TAG,"onStart");

            }

            @Override
            public void onDownloading(DownloadTask downloadTask) {
                Log.d(TAG,"onDownloading");

            }

            @Override
            public void onPause(DownloadTask downloadTask) {
                Log.d(TAG,"onPause");

            }

            @Override
            public void onCompleted(DownloadTask downloadTask) {
                Log.d(TAG,"onCompleted");

            }

            @Override
            public void onError(DownloadTask downloadTask, int errorCode) {
                Log.d(TAG,"onError");

            }
        });
        textView.setText("taskId:"+taskId+"   savePath:"+downloadTask.getSaveDirPath()+"    name:"+downloadTask.getFileName());
    }
}
