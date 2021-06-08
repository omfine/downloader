package com.android.external.downloader.func;

import android.content.Context;
import com.android.external.downloader.db.SdkDownloadFileHelper;
import java.io.File;

public class SdkDownLoadManagerHelper {

    private static Object obj = new Object();
    private static SdkDownLoadManagerHelper downLoadManagerHelper = null;
    private static SdkDownLoadManager downLoadManager = null;
    private Context context = null;
    //设置过期时间
    private long expiredPeriodTime = 86400;

    public static SdkDownLoadManagerHelper getInstance(){
        synchronized (obj){
            if (null == downLoadManagerHelper){
                synchronized (obj){
                    downLoadManagerHelper = new SdkDownLoadManagerHelper();
                }
            }
        }
        return downLoadManagerHelper;
    }

    public SdkDownLoadManagerHelper init(Context context){
        this.context = context;
        synchronized (obj){
            if (null == downLoadManager){
                downLoadManager = new SdkDownLoadManager(context);
            }
        }
        return this;
    }

    public long getExpiredPeriodTime(){
        return expiredPeriodTime;
    }

    public void setExpiredPeriodTime(long expiredPeriodTime){
        this.expiredPeriodTime = expiredPeriodTime;
    }

    public Context getContext(){
        return this.context;
    }

    public SdkDownLoadManager getDownLoadManager(){
        synchronized (obj){
            if (null == downLoadManager){
                synchronized (obj){
                    downLoadManager = new SdkDownLoadManager(context);
                }
            }
        }
        return downLoadManager;
    }

    public void stopAllTask(){
        if (null == downLoadManager){
            return;
        }
        //释放downLoadManager
        downLoadManager.stopAllTask();
        downLoadManager = null;
    }

    public boolean fileExist(String taskID, String fileName){
        return SdkDownloadFileHelper.fileExist(taskID , fileName);
    }

    public File getExistFile(String taskID, String fileName){
        return SdkDownloadFileHelper.getExistFile(taskID, fileName);
    }

    public void clearAllDownloadedFiles(){
        SdkDownloadFileHelper.deleteDir();
    }

    public boolean clearFile(String taskID, String fileName){
        return SdkDownloadFileHelper.deleteFile(taskID, fileName);
    }

    public boolean deleteFile(String filePath){
        return SdkDownloadFileHelper.deleteFile(filePath);
    }

}
