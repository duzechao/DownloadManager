package git.dzc.downloadmanagerlib.download;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.OkHttpClient;

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

    private void init(InputStream in,OkHttpClient okHttpClient){
        executorService = Executors.newFixedThreadPool(mPoolSize);
        futureMap = new HashMap<>();
        DaoMaster.OpenHelper openHelper = new DaoMaster.DevOpenHelper(context,"downloadDB",null);
        DaoMaster daoMaster = new DaoMaster(openHelper.getWritableDatabase());
        downloadDao = daoMaster.newSession().getDownloadDao();
        if(okHttpClient!=null){
            client = okHttpClient;
        }else{
            OkHttpClient.Builder buider = new OkHttpClient.Builder();
            if(in!=null){
                buider.sslSocketFactory(initCertificates(in));
            }
            client = buider.build();
        }

    }
    private void init(){
        init(null,null);
    }

    public DownloadManager(OkHttpClient client, Context context) {
        this.client = client;
        this.context = context;
    }

    public static SSLSocketFactory initCertificates(InputStream... certificates) {
        CertificateFactory certificateFactory;

        SSLContext sslContext = null;
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            int index = 0;
            for (InputStream certificate : certificates) {
                String certificateAlias = Integer.toString(index++);
                keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));

                try {
                    if (certificate != null)
                        certificate.close();
                } catch (IOException e) {
                }
            }

            sslContext = SSLContext.getInstance("TLS");

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            trustManagerFactory.init(keyStore);
            sslContext.init(null, trustManagerFactory.getTrustManagers(),new SecureRandom());
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(sslContext!=null){
            return sslContext.getSocketFactory();
        }
        return null;

    }

    private DownloadManager() {
        init();
    }


    private DownloadManager(Context context,InputStream in) {
        this.context = context;
        init(in,null);
    }

    /**
     *
     * @param context
     * @param sslKey https签名文件
     * @return
     */
    public static DownloadManager getInstance(Context context,InputStream sslKey){
        if(downloadManager==null){
            downloadManager = new DownloadManager(context,sslKey);
        }
        return downloadManager;
    }
    public static DownloadManager getInstance(Context context){
        return getInstance(context,null);
    }

    public static DownloadManager getInstance(OkHttpClient okHttpClient,Context context){
        if(downloadManager==null){
            downloadManager = new DownloadManager(okHttpClient,context);
        }
        return downloadManager;
    }

    /**
     * if task already exist,return the task,else return null
     * @param task
     * @param listener
     * @return
     */
    public DownloadTask addDownloadTask(DownloadTask task,DownloadTaskListener listener){
        DownloadTask downloadTask = currentTaskList.get(task.getId());
        if(null!=downloadTask&&downloadTask.getDownloadStatus()!=DownloadStatus.DOWNLOAD_STATUS_CANCEL){
            Log.d(TAG,"task already exist");
            return downloadTask;
        }
        currentTaskList.put(task.getId(), task);
        task.setDownloadStatus(DownloadStatus.DOWNLOAD_STATUS_PREPARE);
        task.setDownloadDao(downloadDao);
        task.setHttpClient(client);
        task.addDownloadListener(listener);
        if(getDBTaskById(task.getId())==null){
            DownloadDBEntity dbEntity = new DownloadDBEntity(task.getId(), task.getToolSize(), task.getCompletedSize(), task.getUrl(), task.getSaveDirPath(), task.getFileName(), task.getDownloadStatus());
            downloadDao.insertOrReplace(dbEntity);
        }
        Future future =  executorService.submit(task);
        futureMap.put(task.getId(),future);
        return null;
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

    public DownloadTask addDownloadTask(DownloadTask task){
        return addDownloadTask(task, null);
    }

    public void cancel(DownloadTask task){
        task.cancel();
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

    /**
     * if not exists return null
     * the task maybe not in the running task list,
     * you can add{@link #addDownloadTask(DownloadTask) addDownloadTask}
     * @return
     */
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

    /**
     * return all task include running and db
     * @return
     */
    public List<DownloadTask> loadAllTask(){
        List<DownloadTask> list = loadAllDownloadTaskFromDB();
        Map<String,DownloadTask> currentTaskMap = getCurrentTaskList();
        List<DownloadTask> currentList = new ArrayList<>();
        if(currentTaskMap!=null){
            currentList.addAll(currentTaskMap.values());
        }
        if(!currentList.isEmpty()&&list!=null){
            for(DownloadTask task:list){
                if(!currentList.contains(task)){
                    currentList.add(task);
                }
            }
        }else{
            if(list!=null)currentList.addAll(list);
        }
        return currentList;
    }

    /**
     * return the task in the running task list
     * @param taskId
     * @return
     */
    public DownloadTask getCurrentTaskById(String taskId){
        return currentTaskList.get(taskId);
    }
    /**
     * if not exists return null
     * the task maybe not in the running task list,
     * you can add{@link #addDownloadTask(DownloadTask) addDownloadTask}
     * @param taskId
     * @return
     */
    public DownloadTask getTaskById(String taskId){
        DownloadTask task = null;
        task = getCurrentTaskById(taskId);
        if(task!=null){
            return task;
        }
        return getDBTaskById(taskId);
    }

    /**
     * if not exists return null
     * the task maybe not in the running task list,
     * you can add{@link #addDownloadTask(DownloadTask) addDownloadTask}
     * @param taskId
     * @return
     */
    public DownloadTask getDBTaskById(String taskId){
        DownloadDBEntity entity = downloadDao.load(taskId);
        if(entity!=null){
            return DownloadTask.parse(entity);
        }
        return null;
    }

}
