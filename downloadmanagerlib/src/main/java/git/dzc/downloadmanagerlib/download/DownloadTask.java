package git.dzc.downloadmanagerlib.download;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

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
    private long completedSize;         //  Download section has been completed
    //    private float percent;        //  Percent Complete
    private String url;
    private String saveDirPath;
    private RandomAccessFile file;
    private int UPDATE_SIZE = 50 * 1024;    // The database is updated once every 50k
    private int downloadStatus = DownloadStatus.DOWNLOAD_STATUS_INIT;

    private String fileName;    // File name when saving


    private List<DownloadTaskListener> listeners = new ArrayList<>();;

    public DownloadTask() {

    }

    @Override
    public void run() {
        downloadStatus = DownloadStatus.DOWNLOAD_STATUS_PREPARE;
        onPrepare();
        InputStream inputStream = null;
        BufferedInputStream bis = null;
        try {
            dbEntity = downloadDao.load(id);
            file = new RandomAccessFile(saveDirPath + fileName, "rwd");
            if(dbEntity!=null){
                completedSize = dbEntity.getCompletedSize();
                toolSize = dbEntity.getToolSize();
            }
            if (file.length() < completedSize) {
                completedSize = file.length();
            }
            long fileLength = file.length();
            if(fileLength!=0&&toolSize<=fileLength){
                downloadStatus = DownloadStatus.DOWNLOAD_STATUS_COMPLETED;
                toolSize = completedSize = fileLength;
                dbEntity = new DownloadDBEntity(id, toolSize, toolSize, url, saveDirPath, fileName, downloadStatus);
                downloadDao.insertOrReplace(dbEntity);
                onCompleted();
                return;
            }
            downloadStatus = DownloadStatus.DOWNLOAD_STATUS_START;
            onStart();
            Request request = new Request.Builder()
                    .url(url)
                    .header("RANGE", "bytes=" + completedSize + "-")    //  Http value set breakpoints RANGE
                    .build();
            Response response = client.newCall(request).execute();
            if(response!=null&&response.isSuccessful()){
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    downloadStatus = DownloadStatus.DOWNLOAD_STATUS_DOWNLOADING;
                    if(toolSize<=0){
                        toolSize = responseBody.contentLength();
                        dbEntity.setToolSize(toolSize);
                        downloadDao.update(dbEntity);
                    }
                    if(TextUtils.isEmpty(response.header("Content-Range"))){
                        //返回的没有Content-Range 不支持断点下载 需要重新下载
                        File alreadyDownloadedFile = new File(saveDirPath + fileName);
                        if(alreadyDownloadedFile.exists()){
                            alreadyDownloadedFile.delete();
                        }
                        file = new RandomAccessFile(saveDirPath + fileName, "rwd");
                        completedSize = 0;
                    }
                    file.seek(completedSize);
                    inputStream = responseBody.byteStream();
                    bis = new BufferedInputStream(inputStream);
                    byte[] buffer = new byte[2 * 1024];
                    int length = 0;
                    int buffOffset = 0;
                    if (dbEntity == null) {
                        dbEntity = new DownloadDBEntity(id, toolSize, 0L, url, saveDirPath, fileName, downloadStatus);
                        downloadDao.insertOrReplace(dbEntity);
                    }
                    while ((length = bis.read(buffer)) > 0 && downloadStatus != DownloadStatus.DOWNLOAD_STATUS_CANCEL &&downloadStatus!=DownloadStatus.DOWNLOAD_STATUS_PAUSE) {
                        file.write(buffer, 0, length);
                        completedSize += length;
                        buffOffset += length;
                        if (buffOffset >= UPDATE_SIZE) {
                            // Update download information database
                            buffOffset = 0;
                            //这两句根据需要自行选择是否注释，注释掉的话由于少了数据库的读取，速度会快一点，但同时如果在下载过程程序崩溃的话，程序不会保存最新的下载进度,并且下载过程不会更新进度
                            dbEntity.setCompletedSize(completedSize);
                            downloadDao.update(dbEntity);
                            Log.d("onDownloading1",dbEntity.toString());

                            onDownloading();
                        }
                    }
                    dbEntity.setCompletedSize(completedSize);
                    downloadDao.update(dbEntity);
                    Log.d("onDownloading2",dbEntity.toString());

                    onDownloading();
                }
            }else{
                downloadStatus = DownloadStatus.DOWNLOAD_STATUS_ERROR;
                onError(DownloadTaskListener.DOWNLOAD_ERROR_IO_ERROR);
            }

        } catch (FileNotFoundException e) {
            downloadStatus = DownloadStatus.DOWNLOAD_STATUS_ERROR;
            onError(DownloadTaskListener.DOWNLOAD_ERROR_FILE_NOT_FOUND);
            return;
//            e.printStackTrace();
        } catch (IOException e) {
            downloadStatus = DownloadStatus.DOWNLOAD_STATUS_ERROR;
            onError(DownloadTaskListener.DOWNLOAD_ERROR_IO_ERROR);
            return;
        } finally {
            dbEntity.setCompletedSize(completedSize);
            downloadDao.update(dbEntity);
            Log.d("onDownloadComplete",dbEntity.toString());
            if (bis != null) try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (inputStream != null) try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (file != null) try {
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(toolSize==completedSize)downloadStatus=DownloadStatus.DOWNLOAD_STATUS_COMPLETED;
        dbEntity.setDownloadStatus(downloadStatus);
        downloadDao.update(dbEntity);
        Log.d("onDownloadComplete2",dbEntity.toString());



        switch (downloadStatus){
            case DownloadStatus.DOWNLOAD_STATUS_COMPLETED:
                onCompleted();
                break;
            case DownloadStatus.DOWNLOAD_STATUS_PAUSE:
                onPause();
                break;
            case DownloadStatus.DOWNLOAD_STATUS_CANCEL:
                downloadDao.delete(dbEntity);
                File temp = new File(saveDirPath + fileName);
                if(temp.exists())temp.delete();
                onCancel();
                break;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public float getPercent() {
        return toolSize==0?0:completedSize * 100 / toolSize;
    }


    public long getToolSize() {
        return toolSize;
    }

    public void setTotalSize(long toolSize) {
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


    public void cancel() {
        setDownloadStatus(DownloadStatus.DOWNLOAD_STATUS_CANCEL);
        File temp = new File(saveDirPath + fileName);
        if(temp.exists())temp.delete();
    }

    public void pause(){
        setDownloadStatus(DownloadStatus.DOWNLOAD_STATUS_PAUSE);
    }
    private void onPrepare() {
        for (DownloadTaskListener listener : listeners) {
            listener.onPrepare(this);
        }
    }

    private void onStart() {
        for (DownloadTaskListener listener : listeners) {
            listener.onStart(this);
        }
    }

    private void onDownloading() {
        Log.d("onDownloading",id+"listener size:"+listeners.size());
        for (DownloadTaskListener listener : listeners) {
            listener.onDownloading(this);
        }
    }

    private void onCompleted() {
        for (DownloadTaskListener listener : listeners) {
            listener.onCompleted(this);
        }
    }

    private void onPause() {
        for (DownloadTaskListener listener : listeners) {
            listener.onPause(this);
        }
    }

    private void onCancel() {
        for (DownloadTaskListener listener : listeners) {
            listener.onCancel(this);
        }
    }

    private void onError(int errorCode) {
        for (DownloadTaskListener listener : listeners) {
            listener.onError(this, errorCode);
        }
    }

    public void addDownloadListener(DownloadTaskListener listener) {
        listeners.add(listener);
    }

    /**
     * if listener is null,clear all listener
     * @param listener
     */
    public void removeDownloadListener(DownloadTaskListener listener) {
        if(listener!=null){
            listeners.remove(listener);
        }
    }

    public void removeAllDownloadListener(){
        this.listeners.clear();
    }

    public void setDownloadManager(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadTask that = (DownloadTask) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return url != null ? url.equals(that.url) : that.url == null;

    }

    @Override
    public int hashCode() {
        return 0;
    }

    public static DownloadTask parse(DownloadDBEntity entity) {
        DownloadTask task = new DownloadTask();
        task.setDownloadStatus(entity.getDownloadStatus());
        task.setId(entity.getDownloadId());
        task.setUrl(entity.getUrl());
        task.setFileName(entity.getFileName());
        task.setSaveDirPath(entity.getSaveDirPath());
        task.setCompletedSize(entity.getCompletedSize());
        task.setDbEntity(entity);
        task.setTotalSize(entity.getToolSize());
        return task;
    }
}
