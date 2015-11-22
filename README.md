# DownloadManager
一个下载Android框架，网络连接用了okhttp，数据库方面用GreenDao，
性能相对用原生的有所提升，支持断点下载、加载下载的任务并可给同一个任务设置多个监听，
省去只能设置一个监听要添加一个广播来解决多个页面同时监听同一个下载线程的烦恼，
具备了基本功能，暂时想不出需要添加什么功能，请有需求的提出来，我加上去，
dzc_ze@foxmail.com 需求请通过邮箱发给我

使用方法：
downloadManager = DownloadManager.getInstance(this);
DownloadTask task = new DownloadTask();
String id = MD5.MD5(url);
task.setId(id);
task.setSaveDirPath(getExternalCacheDir().getPath() + "/");
task.setFileName(id+".jpg");
task.setUrl(url);
downloadManager.addDownloadTask(task);


添加监听
downloadManager.addDownloadListener(task,listener);


加载数据库存在的下载任务
downloadManager.loadAllDownloadTaskFromDB();
