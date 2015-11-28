package git.dzc.downloadmanagerlib.download;

import android.text.TextUtils;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dzc on 15/11/21.
 */
public class DownloadTask implements Runnable {
    private DownloadDBEntity dbEntity;
    private DownloadDao downloadDao;
    private DownloadManager downloadManager;
    private OkHttpClient client;



    private String id;
    private long toolSize;
    private long completedSize;//已经下载完的部分
//    private float percent;//完成百分比
    private String url;
    private String saveDirPath;
    private RandomAccessFile file;
    private int UPDATE_SIZE = 40 * 1024;//每40k更新一次数据库
    private int downloadStatus = DownloadStatus.DOWNLOAD_STATUS_INIT;

    private String fileName;// 保存时的文件名



    private List<DownloadTaskListener> listeners;

    public DownloadTask() {
        listeners = new ArrayList<>();
    }

    @Override
    public void run() {
        downloadStatus = DownloadStatus.DOWNLOAD_STATUS_PREPARE;
        onPrepare();
        InputStream inputStream = null;
        BufferedInputStream bis = null;
        try {
            dbEntity = downloadDao.load(id);
            file = new RandomAccessFile(saveDirPath +fileName,"rwd");
            if(file.length()< completedSize){
                completedSize = 0;
            }
            downloadStatus = DownloadStatus.DOWNLOAD_STATUS_START;
            onStart();
            Request request = new Request.Builder()
                    .url(url)
                    .header("RANGE", "bytes=" + completedSize + "-")//设置http断点RANGE值
                    .build();
            Response response = client.newCall(request).execute();
            ResponseBody responseBody = response.body();
            if(responseBody!=null){
                downloadStatus = DownloadStatus.DOWNLOAD_STATUS_DOWNLOADING;
                toolSize = responseBody.contentLength();
                inputStream = responseBody.byteStream();
                bis = new BufferedInputStream(inputStream);
                byte[] buffer = new byte[2 * 1024];
                int length = 0;
                int buffOffset = 0;
                if(dbEntity==null){
                    dbEntity = new DownloadDBEntity(id,toolSize,0L,url, saveDirPath,fileName,downloadStatus);
                    downloadDao.insertOrReplace(dbEntity);
                }
                while ((length = bis.read(buffer)) > 0 && downloadStatus!=DownloadStatus.DOWNLOAD_STATUS_CANCEL) {
                    file.write(buffer, 0, length);
                    completedSize += length;
                    buffOffset += length;
                    if (buffOffset >= UPDATE_SIZE) {
                        // 更新数据库中的下载信息
                        buffOffset = 0;
                        dbEntity.setCompletedSize(completedSize);
                        downloadDao.update(dbEntity);
                        onDownloading();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            downloadStatus = DownloadStatus.DOWNLOAD_STATUS_ERROR;
            onError(DownloadTaskListener.DOWNLOAD_ERROR_FILE_NOT_FOUND);
            e.printStackTrace();
        } catch (IOException e) {
            downloadStatus = DownloadStatus.DOWNLOAD_STATUS_ERROR;
            onError(DownloadTaskListener.DOWNLOAD_ERROR_IO_ERROR);
            e.printStackTrace();
        }finally {
            if(bis!=null) try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(inputStream!=null) try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(file!=null) try {
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        downloadStatus = DownloadStatus.DOWNLOAD_STATUS_COMPLETED;
        dbEntity.setDownloadStatus(downloadStatus);
        downloadDao.update(dbEntity);

        onCompleted();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public float getPercent() {
        return completedSize*100 /toolSize;
    }


    public long getToolSize() {
        return toolSize;
    }

    public void setToolSize(long toolSize) {
        this.toolSize = toolSize;
    }



    public long getCompletedSize() {
        return completedSize;
    }

    public void setCompletedSize(long completedSize) {
        this.completedSize = completedSize;
    }

    public String getSaveDirPath() {
        return saveDirPath;
    }

    public void setSaveDirPath(String saveDirPath) {
        this.saveDirPath = saveDirPath;
    }

    public int getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(int downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public void setDownloadDao(DownloadDao downloadDao) {
        this.downloadDao = downloadDao;
    }

    public void setDbEntity(DownloadDBEntity dbEntity) {
        this.dbEntity = dbEntity;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    public void setHttpClient(OkHttpClient client) {
        this.client = client;
    }
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


    public void cancel(){
        downloadStatus = DownloadStatus.DOWNLOAD_STATUS_CANCEL;
    }

    private void onPrepare(){
        for(DownloadTaskListener listener:listeners){
            listener.onPrepare(this);
        }
    }
    private void onStart(){
        for(DownloadTaskListener listener:listeners){
            listener.onStart(this);
        }
    }
    private void onDownloading(){
        for(DownloadTaskListener listener:listeners){
            listener.onDownloading(this);
        }
    }
    private void onCompleted(){
        for(DownloadTaskListener listener:listeners){
            listener.onCompleted(this);
        }
    }
    private void onError(int errorCode){
        for(DownloadTaskListener listener:listeners){
            listener.onError(this, errorCode);
        }
    }

    public void addDownloadListener(DownloadTaskListener listener){
        listeners.add(listener);
    }

    public void removeDownloadListener(DownloadTaskListener listener){
        listeners.remove(listener);
    }
    public void setDownloadManager(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }


    @Override
    public boolean equals(Object o) {
        if(this==o){
            return true;
        }
        if(!(o instanceof DownloadTask)){
            return false;
        }
        if(TextUtils.isEmpty(url)||TextUtils.isEmpty(saveDirPath)){
            return false;
        }
        return url.equals(((DownloadTask) o).url) && saveDirPath.equals(((DownloadTask) o).saveDirPath);
    }

    public static DownloadTask parse(DownloadDBEntity entity){
        DownloadTask task = new DownloadTask();
        task.setDownloadStatus(entity.getDownloadStatus());
        task.setId(entity.getDownloadId());
        task.setUrl(entity.getUrl());
        task.setFileName(entity.getFileName());
        task.setSaveDirPath(entity.getSaveDirPath());
        task.setCompletedSize(entity.getCompletedSize());
        task.setDbEntity(entity);
        task.setToolSize(entity.getToolSize());
        return task;
    }
}
