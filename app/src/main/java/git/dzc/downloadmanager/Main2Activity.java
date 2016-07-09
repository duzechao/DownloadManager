package git.dzc.downloadmanager;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import git.dzc.downloadmanagerlib.download.DownloadManager;
import git.dzc.downloadmanagerlib.download.DownloadTask;
import git.dzc.downloadmanagerlib.download.DownloadTaskListener;

public class Main2Activity extends AppCompatActivity {

    private static final String TAG = "Main2Activity";
    private RecyclerView rv;
    private DownloadManager downloadManager;
    private DownloadAdapter adapter;
    static Map<DownloadTask,DownloadTaskListener> listenerMap = new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        rv = (RecyclerView) findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        downloadManager = DownloadManager.getInstance(getApplicationContext());
        List<DownloadTask> downloadTaskList = downloadManager.loadAllTask();
        adapter = new DownloadAdapter(this,downloadTaskList);
        rv.setAdapter(adapter);
    }

    private static class DownloadAdapter extends RecyclerView.Adapter<DownloadViewHolder>{
        List<DownloadTask> taskList;

        public DownloadAdapter(Context context, List<DownloadTask> taskList) {
            this.context = context;
            this.taskList = taskList;
        }

        private Context context;
        @Override
        public DownloadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(context).inflate(R.layout.download_item,parent,false);
            return new DownloadViewHolder(item);
        }

        @Override
        public void onBindViewHolder(DownloadViewHolder holder, int position) {
            DownloadTask downloadTask = taskList.get(position);
            holder.tvFileName.setText(downloadTask.getFileName());
            holder.tvUrl.setText(downloadTask.getUrl());
            holder.tvProgress.setText(downloadTask.getPercent()+"%");
            addListener(downloadTask,holder.tvProgress,holder.listener);
        }



        @Override
        public int getItemCount() {
            return taskList==null?0:taskList.size();
        }
    }

    private static class DownloadViewHolder extends RecyclerView.ViewHolder{
        TextView tvFileName;
        TextView tvProgress;
        TextView tvUrl;
        DownloadTaskListener listener;
        public DownloadViewHolder(View itemView) {
            super(itemView);
            tvFileName = (TextView) itemView.findViewById(R.id.tv_name);
            tvUrl = (TextView) itemView.findViewById(R.id.tv_url);
            tvProgress = (TextView) itemView.findViewById(R.id.tv_progress);
        }
    }


    private static void addListener(DownloadTask downloadTask, final TextView tv, DownloadTaskListener listener){
        downloadTask.removeDownloadListener(listener);
        DownloadTaskListener downloadTaskListener = new DownloadTaskListener() {
            @Override
            public void onPrepare(DownloadTask downloadTask) {
                Log.d(TAG,"onPrepare");
            }

            @Override
            public void onStart(DownloadTask downloadTask) {
                Log.d(TAG,"onStart");

            }

            @Override
            public void onDownloading(final DownloadTask downloadTask) {
                Log.d(TAG,"onDownloading");
                tv.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText(downloadTask.getPercent()+"%");
                    }
                });
            }

            @Override
            public void onPause(DownloadTask downloadTask) {
                Log.d(TAG,"onPause");

            }

            @Override
            public void onCancel(DownloadTask downloadTask) {
                Log.d(TAG,"onCancel");

            }

            @Override
            public void onCompleted(final DownloadTask downloadTask) {
                Log.d(TAG,"onCompleted");
                tv.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText(downloadTask.getPercent()+"%");
                    }
                });
            }

            @Override
            public void onError(DownloadTask downloadTask, int errorCode) {
                Log.d(TAG,"onError");

            }
        };
        downloadTask.addDownloadListener(downloadTaskListener);
        listenerMap.remove(downloadTask);
        listenerMap.put(downloadTask,downloadTaskListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Set<DownloadTask> taskSet = listenerMap.keySet();
        if(taskSet!=null){
            Iterator<DownloadTask> iterator = taskSet.iterator();
            while (iterator.hasNext()){
                DownloadTask downloadTask = iterator.next();
                downloadTask.removeDownloadListener(listenerMap.get(downloadTask));
            }
        }
    }
}
