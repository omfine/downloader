package com.android.external.downloader.func;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
/**
 * 类功能描述：下载器后台服务</br>
 */
public class SdkDownLoadService extends Service {
    private static SdkDownLoadManager downLoadManager;
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    public static SdkDownLoadManager getDownLoadManager(){
        return downLoadManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        downLoadManager = new SdkDownLoadManager(SdkDownLoadService.this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //释放downLoadManager
        downLoadManager.stopAllTask();
        downLoadManager = null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if(downLoadManager == null){
            downLoadManager = new SdkDownLoadManager(SdkDownLoadService.this);
        }
    }
    
    
    
    
    

}
