
package com.android.external.downloader.func;

import android.content.Context;
import android.content.SharedPreferences;
import com.android.external.downloader.bean.SdkSQLDownLoadInfo;
import com.android.external.downloader.db.SdkDownloadDataKeeper;
import com.android.external.downloader.db.SdkDownloadFileHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 * 类功能描述：下载管理类</br>
 */
public class SdkDownLoadManager {
    private Context mycontext;

    private ArrayList<SdkDownLoader> taskList = new ArrayList<SdkDownLoader>();

    private final int MAX_DOWNLOADING_TASK = 5; // 最大同时下载数

    private SdkDownLoader.DownLoadSuccess downloadsuccessListener = null;

    /**服务器是否支持断点续传*/
    private boolean isSupportBreakpoint = false;
    
    //线程池
    private ThreadPoolExecutor pool;
    
    /**用户ID,默认值man*/
    private String userID = "downloader";

    private SharedPreferences sharedPreferences;

    private SdkDownLoadListener alltasklistener;

    public SdkDownLoadManager(Context context) {
        mycontext = context;
        init(context);
    }

    private void init(Context context) {
        pool = new ThreadPoolExecutor(
                MAX_DOWNLOADING_TASK, MAX_DOWNLOADING_TASK, 30, TimeUnit.SECONDS, 
                new ArrayBlockingQueue<Runnable>(2000));

        downloadsuccessListener = new SdkDownLoader.DownLoadSuccess() {
            @Override
            public void onTaskSeccess(String TaskID) {
                int taskSize = taskList.size();
                for (int i = 0; i < taskSize; i++) {
                    SdkDownLoader deletedownloader = taskList.get(i);
                    if (deletedownloader.getTaskID().equals(TaskID)) {
                        taskList.remove(deletedownloader);
                        return;
                    }
                }
            }
        };
        sharedPreferences = mycontext.getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        userID = sharedPreferences.getString("UserID","downloader");
        recoverData(mycontext, userID);
    }
    
   
    /**
     * (从数据库恢复下载任务信息) 
     * @param context 上下文
     * @param userID  用户ID
     */
    private void recoverData(Context context,String userID){
        stopAllTask();
        taskList = new ArrayList<SdkDownLoader>();
        SdkDownloadDataKeeper datakeeper = new SdkDownloadDataKeeper(context);
        ArrayList<SdkSQLDownLoadInfo> sqlDownloadInfoList = null;
        if(userID == null){
            sqlDownloadInfoList = datakeeper.getAllDownLoadInfo();
        }else{
            sqlDownloadInfoList = datakeeper.getUserDownLoadInfo(userID);
        }
        if (sqlDownloadInfoList.size() > 0) {
            int listSize = sqlDownloadInfoList.size();
            for (int i = 0; i < listSize; i++) {
                SdkSQLDownLoadInfo sqlDownLoadInfo = sqlDownloadInfoList.get(i);
                SdkDownLoader sqlDownLoader = new SdkDownLoader(context, sqlDownLoadInfo, pool,userID,isSupportBreakpoint,false);
                sqlDownLoader.setDownLodSuccesslistener(downloadsuccessListener);
                sqlDownLoader.setDownLoadListener("public",alltasklistener);
                taskList.add(sqlDownLoader);
            }
        }
    }
    

    /**
     * (设置下载管理是否支持断点续传) 
     * @param isSupportBreakpoint
     */
    public void setSupportBreakpoint(boolean isSupportBreakpoint) {
        if((!this.isSupportBreakpoint) && isSupportBreakpoint){
            int taskSize = taskList.size();
            for (int i = 0; i < taskSize; i++) {
                SdkDownLoader downloader = taskList.get(i);
                downloader.setSupportBreakpoint(true);
            } 
        }
        this.isSupportBreakpoint = isSupportBreakpoint;
    }
    
    /**
     * (切换用户)
     * @param userID 用户ID
     */
    public void changeUser(String userID){
        this.userID = userID;
        SharedPreferences.Editor editor = sharedPreferences.edit();// 获取编辑器
        editor.putString("UserID",userID);
        editor.commit();// 提交修改
        SdkDownloadFileHelper.setUserID(userID);
        recoverData(mycontext, userID);
    }

    public String getUserID(){
        return userID;
    }

    /**
     * (增加一个任务，默认开始执行下载任务)
     * 
     * @param TaskID 任务号
     * @param url 请求下载的路径
     * @param fileName 文件名
     * @return -1 : 文件已存在 ，0 ： 已存在任务列表 ， 1 ： 添加进任务列表
     */
    public int addTask(String TaskID, String url, String fileName) {
        return addTask(TaskID, url, fileName, null);
    }

    /**
     * (增加一个任务，默认开始执行下载任务)
     * 
     * @param TaskID 任务号
     * @param url 请求下载的路径
     * @param fileName 文件名
     * @param filepath 下载到本地的路径
     * @return -1 : 文件已存在 ，0 ： 已存在任务列表 ， 1 ： 添加进任务列表
     */
    public int addTask(String TaskID, String url, String fileName, String filepath) {
        if(TaskID == null){
            TaskID = fileName;
        }
        int state = getAttachmentState(TaskID, fileName, filepath);
        if (state != 1) {
            return state;
        }

        SdkSQLDownLoadInfo downloadinfo = new SdkSQLDownLoadInfo();
        downloadinfo.setUserID(userID);
        downloadinfo.setDownloadSize(0);
        downloadinfo.setFileSize(0);
        downloadinfo.setTaskID(TaskID);
        downloadinfo.setFileName(fileName);
        downloadinfo.setUrl(url);
        if (filepath == null) {
            downloadinfo.setFilePath(SdkDownloadFileHelper.getFileDefaultPath() + "/(" + SdkDownloadFileHelper.filterIDChars(TaskID) + ")" + fileName);
        } else {
            downloadinfo.setFilePath(filepath);
        }
        SdkDownLoader taskDownLoader = new SdkDownLoader(mycontext, downloadinfo, pool,userID,isSupportBreakpoint,true);
        taskDownLoader.setDownLodSuccesslistener(downloadsuccessListener);
        if(isSupportBreakpoint){
            taskDownLoader.setSupportBreakpoint(true);
        }else{
            taskDownLoader.setSupportBreakpoint(false);
        }
        taskDownLoader.start();
        taskDownLoader.setDownLoadListener("public",alltasklistener);
        taskList.add(taskDownLoader);
        return 1;
    }

    /**
     * 获取附件状态
     * 
     * @param TaskID 任务号
     * @param fileName 文件名
     * @param filepath 下载到本地的路径
     * @return  -1 : 文件已存在 ，0 ： 已存在任务列表 ， 1 ： 添加进任务列表
     */
     private int getAttachmentState(String TaskID, String fileName, String filepath) {

        int taskSize = taskList.size();
        for (int i = 0; i < taskSize; i++) {
            SdkDownLoader downloader = taskList.get(i);
            if (downloader.getTaskID().equals(TaskID)) {
                return 0;
            }
        }
        File file = null;
        if (filepath == null) {
            file = new File(SdkDownloadFileHelper.getFileDefaultPath() + "/(" + SdkDownloadFileHelper.filterIDChars(TaskID) + ")" + fileName);
            if (file.exists()) {
                return -1;
            }
        } else {
            file = new File(filepath);
            if (file.exists()) {
                return -1;
            }
        }
        return 1;
    }

    /**
     * (删除一个任务，包括已下载的本地文件)
     * 
     * @param taskID
     */
    public void deleteTask(String taskID) {
        int taskSize = taskList.size();
        for (int i = 0; i < taskSize; i++) {
            SdkDownLoader deletedownloader = taskList.get(i);
            if (deletedownloader.getTaskID().equals(taskID)) {
                deletedownloader.destroy();
                taskList.remove(deletedownloader);
                break;
            }
        }
    }

    /**
     * (获取当前任务列表的所有任务ID)
     * 
     * @return
     */
    public ArrayList<String> getAllTaskID() {
        ArrayList<String> taskIDlist = new ArrayList<String>();
        int listSize = taskList.size();
        for (int i = 0; i < listSize; i++) {
            SdkDownLoader deletedownloader = taskList.get(i);
            taskIDlist.add(deletedownloader.getTaskID());
        }
        return taskIDlist;
    }

    /**
     * (获取当前任务列表的所有任务，以TaskInfo列表的方式返回)
     * 
     * @return
     */
    public ArrayList<SdkDownloadTaskInfo> getAllTask() {
        ArrayList<SdkDownloadTaskInfo> taskInfolist = new ArrayList<SdkDownloadTaskInfo>();
        int listSize = taskList.size();
        for (int i = 0; i < listSize; i++) {
            SdkDownLoader deletedownloader = taskList.get(i);
            SdkSQLDownLoadInfo sqldownloadinfo = deletedownloader.getSQLDownLoadInfo();
            SdkDownloadTaskInfo taskinfo = new SdkDownloadTaskInfo();
            taskinfo.setFileName(sqldownloadinfo.getFileName());
            taskinfo.setOnDownloading(deletedownloader.isDownLoading());
            taskinfo.setTaskID(sqldownloadinfo.getTaskID());
            taskinfo.setFileSize(sqldownloadinfo.getFileSize());
            taskinfo.setDownFileSize(sqldownloadinfo.getDownloadSize());
            taskInfolist.add(taskinfo);
        }
        return taskInfolist;
    }

    /**
     * (根据任务ID开始执行下载任务)
     * 
     * @param taskID
     */
    public void startTask(String taskID) {
        int listSize = taskList.size();
        for (int i = 0; i < listSize; i++) {
            SdkDownLoader deletedownloader = taskList.get(i);
            if (deletedownloader.getTaskID().equals(taskID)) {
                deletedownloader.start();
                break;
            }
        }
    }

    /**
     * (根据任务ID停止相应的下载任务)
     * 
     * @param taskID
     */
    public void stopTask(String taskID) {
        int listSize = taskList.size();
        for (int i = 0; i < listSize; i++) {
            SdkDownLoader deletedownloader = taskList.get(i);
            if (deletedownloader.getTaskID().equals(taskID)) {
                deletedownloader.stop();
                break;
            }
        }
    }

    /**
     * (开始当前任务列表里的所有任务)
     */
    public void startAllTask() {
        int listSize = taskList.size();
        for (int i = 0; i < listSize; i++) {
            SdkDownLoader deletedownloader = taskList.get(i);
            deletedownloader.start();
        }
    }

    /**
     * (停止当前任务列表里的所有任务)
     */
    public void stopAllTask() {
        int listSize = taskList.size();
        for (int i = 0; i < listSize; i++) {
            SdkDownLoader deletedownloader = taskList.get(i);
            deletedownloader.stop();
        }
    }

    /**
     * (根据任务ID将监听器设置到相对应的下载任务)
     * 
     * @param taskID
     * @param listener
     */
    public void setSingleTaskListener(String taskID, SdkDownLoadListener listener) {
        int listSize = taskList.size();
        for (int i = 0; i < listSize; i++) {
            SdkDownLoader deletedownloader = taskList.get(i);
            if (deletedownloader.getTaskID().equals(taskID)) {
                deletedownloader.setDownLoadListener("private", listener);
                break;
            }
        }
    }

    /**
     * (将监听器设置到当前任务列表所有任务)
     * 
     * @param listener
     */
    public void setAllTaskListener(SdkDownLoadListener listener) {
        alltasklistener = listener;
        int listSize = taskList.size();
        for (int i = 0; i < listSize; i++) {
            SdkDownLoader deletedownloader = taskList.get(i);
            deletedownloader.setDownLoadListener("public", listener);
        }
    }

    /**
     * (根据任务ID移除相对应的下载任务的监听器)
     * 
     * @param taskID
     */
    public void removeDownLoadListener(String taskID) {
        SdkDownLoader downLoader = getDownloader(taskID);
        if (downLoader != null) {
            downLoader.removeDownLoadListener("private");
        }
    }

    /**
     * (删除监听所有任务的监听器)
     */
    public void removeAllDownLoadListener() {
        int listSize = taskList.size();
        for (int i = 0; i < listSize; i++) {
            SdkDownLoader deletedownloader = taskList.get(i);
            deletedownloader.removeDownLoadListener("public");
        }
    }

    /**
     * (根据任务号获取当前任务是否正在下载)
     * 
     * @param taskID
     * @return
     */
    public boolean isTaskdownloading(String taskID) {
        SdkDownLoader downLoader = getDownloader(taskID);
        if (downLoader != null) {
            return downLoader.isDownLoading();
        }
        return false;
    }

    /**
     * 根据附件id获取下载器
     */
    private SdkDownLoader getDownloader(String taskID) {
        for (int i = 0; i < taskList.size(); i++) {
            SdkDownLoader downloader = taskList.get(i);
            if (taskID != null && taskID.equals(downloader.getTaskID())) {
                return downloader;
            }
        }
        return null;
    }

    /**
     * 根据id获取下载任务列表中某个任务
     */
    public SdkDownloadTaskInfo getTaskInfo(String taskID) {
        SdkDownLoader downloader = getDownloader(taskID);
        if (downloader==null) {
            return null;
        }
        SdkSQLDownLoadInfo sqldownloadinfo = downloader.getSQLDownLoadInfo();
        if (sqldownloadinfo==null) {
            return null;
        }
        SdkDownloadTaskInfo taskinfo = new SdkDownloadTaskInfo();
        taskinfo.setFileName(sqldownloadinfo.getFileName());
        taskinfo.setOnDownloading(downloader.isDownLoading());
        taskinfo.setTaskID(sqldownloadinfo.getTaskID());
        taskinfo.setDownFileSize(sqldownloadinfo.getDownloadSize());
        taskinfo.setFileSize(sqldownloadinfo.getFileSize());
        return taskinfo;
    }
}
