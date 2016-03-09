package git.dzc.downloadmanagerlib.download;

import android.content.Context;
import android.util.Log;

import okhttp3.OkHttpClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by dzc on 15/11/21.
 */
public class DownloadManager {
    private static final String TAG = "DownloadManager";
    private Context context;
    private static DownloadManager downloadManager;
    private static DownloadDao downloadDao;
    private int mPoolSize = 5;
    private ExecutorService executorService;
    private Map<String,Future> futureMap;
    private OkHttpClient client;

    public Map<String, DownloadTask> getCurrentTaskList() {
        return currentTaskList;
    }

    private Map<String,DownloadTask> currentTaskList = new HashMap<>();
    public void init(){
        executorService = Executors.newFixedThreadPool(mPoolSize);
        futureMap = new HashMap<>();
        DaoMaster.OpenHelper openHelper = new DaoMaster.DevOpenHelper(context,"downloadDB",null);
        DaoMaster daoMaster = new DaoMaster(openHelper.getWritableDatabase());
        downloadDao = daoMaster.newSession().getDownloadDao();
        client = new OkHttpClient();
    }

    private DownloadManager() {
        init();
    }

    private DownloadManager(Context context) {
        this.context = context;
        init();
    }

    public static DownloadManager getInstance(Context context){
        if(downloadManager==null){
            downloadManager = new DownloadManager(context);
        }
        return downloadManager;
    }

    public void addDownloadTask(DownloadTask task,DownloadTaskListener listener){
        if(null!=currentTaskList.get(task.getId())&&task.getDownloadStatus()!=DownloadStatus.DOWNLOAD_STATUS_INIT){
            Log.d(TAG,"task already exist");
            return ;
        }
        currentTaskList.put(task.getId(), task);
        task.setDownloadStatus(DownloadStatus.DOWNLOAD_STATUS_PREPARE);
        task.setDownloadDao(downloadDao);
        task.setHttpClient(client);
        task.addDownloadListener(listener);
        Future future =  executorService.submit(task);
        futureMap.put(task.getId(),future);
    }

    /**
     * if return null,the task does not exist
     * @param taskId
     * @return
     */
    public DownloadTask resume(String taskId){
        DownloadTask downloadTask = getCurrentTaskById(taskId);
        if(downloadTask!=null){
            if(downloadTask.getDownloadStatus()==DownloadStatus.DOWNLOAD_STATUS_PAUSE){
                Future future =  executorService.submit(downloadTask);
                futureMap.put(downloadTask.getId(),future);
            }

        }else{
            downloadTask = getDBTaskById(taskId);
            if(downloadTask!=null){
                currentTaskList.put(taskId,downloadTask);
                Future future =  executorService.submit(downloadTask);
                futureMap.put(downloadTask.getId(),future);
            }
        }
        return downloadTask;
    }


    public void addDownloadListener(DownloadTask task,DownloadTaskListener listener){
        task.addDownloadListener(listener);
    }

    public void removeDownloadListener(DownloadTask task,DownloadTaskListener listener){
        task.removeDownloadListener(listener);
    }

    public void addDownloadTask(DownloadTask task){
        addDownloadTask(task, null);
    }

    public void cancel(DownloadTask task){
        currentTaskList.remove(task.getId());
        futureMap.remove(task.getId());
        task.setDownloadStatus(DownloadStatus.DOWNLOAD_STATUS_CANCEL);
        downloadDao.deleteByKey(task.getId());
    }
    public void cancel(String taskId){
        DownloadTask task = getTaskById(taskId);
        if(task!=null){
            cancel(task);
        }
    }
    public void pause(DownloadTask task){
        task.setDownloadStatus(DownloadStatus.DOWNLOAD_STATUS_PAUSE);
    }
    public void pause(String taskId){
        DownloadTask task = getTaskById(taskId);
        if(task!=null){
            pause(task);
        }
    }



    public List<DownloadDBEntity> loadAllDownloadEntityFromDB(){
        return downloadDao.loadAll();
    }

    public List<DownloadTask> loadAllDownloadTaskFromDB(){
        List<DownloadDBEntity> list = loadAllDownloadEntityFromDB();
        List<DownloadTask> downloadTaskList = null;
        if(list!=null&&!list.isEmpty()){
            downloadTaskList = new ArrayList<>();
            for(DownloadDBEntity entity:list){
                downloadTaskList.add(DownloadTask.parse(entity));
            }
        }
        return downloadTaskList;
    }

    public DownloadTask getCurrentTaskById(String taskId){
        return currentTaskList.get(taskId);
    }
    public DownloadTask getTaskById(String taskId){
        DownloadTask task = null;
        task = getCurrentTaskById(taskId);
        if(task!=null){
            return task;
        }
        return getDBTaskById(taskId);
    }
    public DownloadTask getDBTaskById(String taskId){
        DownloadDBEntity entity = downloadDao.load(taskId);
        if(entity!=null){
            return DownloadTask.parse(entity);
        }
        return null;
    }

}
