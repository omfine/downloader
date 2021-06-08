package com.android.external.downloader.impl;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.android.external.downloader.bean.SdkSQLDownLoadInfo;
import com.android.external.downloader.func.SdkDownLoadListener;
import com.android.external.downloader.func.SdkDownLoadManager;
import com.android.external.downloader.func.SdkDownLoadManagerHelper;
import com.android.external.downloader.util.SdkDownloadPrefHelper;
import java.io.File;

public class SdkAppDownloadManager {

    private String downloadPath  = null;
    private String fileName = null;
    private OnSdkDownloadListener onSdkDownloadListener = null;

    private String taskId = "1001";
    private long firstErrorTime = 0L;

    //下载异常
    private int downLoadError = 3;
    //超时时间（秒）
    private int maxOverTime = 120;
    //是否支持断点续传
    private boolean supportBreakpoint = true;

    private Handler handler = new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(Message msg) {
            int msgWhat = msg.what;
            if (msgWhat == downLoadError){
                //下载错误
                long cTime = System.currentTimeMillis() / 1000;
                if (0L == firstErrorTime) {
                    firstErrorTime = cTime;
                }
                //一定时间内可以继续请求下载
                long differ = Math.abs(cTime - firstErrorTime);
                if (differ < maxOverTime) {
                    SdkDownLoadManagerHelper.getInstance().getDownLoadManager().startTask(taskId);
                } else {
                    //返回下载异常
                    if (null != onSdkDownloadListener){
                        onSdkDownloadListener.onError();
                    }
                }
            }
        }
    };

    public SdkAppDownloadManager setMaxOverTime(int maxOverTime){
        this.maxOverTime = maxOverTime;
        return this;
    }

    public SdkAppDownloadManager setParams(String taskId , String downloadPath, String fileName , OnSdkDownloadListener onSdkDownloadListener){
        this.taskId = taskId;
        this.downloadPath = downloadPath;
        this.fileName = fileName;
        this.onSdkDownloadListener = onSdkDownloadListener;
        return this;
    }

    public SdkAppDownloadManager setSupportBreakpoint(boolean SupportBreakpoint){
        this.supportBreakpoint = supportBreakpoint;
        return this;
    }

    public void startDownLoad(){
        if (null == downloadPath || null == fileName){
            if (null != onSdkDownloadListener){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onSdkDownloadListener.onError();
                    }
                });
            }
            return;
        }
        if (null == downloadPath || TextUtils.isEmpty(downloadPath) || null == fileName || fileName.isEmpty()){
            if (null != onSdkDownloadListener){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onSdkDownloadListener.onError();
                    }
                });
            }
            return;
        }

        boolean isUpdateExpired = SdkDownloadPrefHelper.isUpdateExpired(this.taskId, this.fileName);
        if (isUpdateExpired) {
            SdkDownLoadManagerHelper.getInstance().clearFile(this.taskId , this.fileName);
        }
        SdkDownloadPrefHelper.updateLastAppUpdateTimestamp(this.taskId, this.fileName);

        boolean exist = SdkDownLoadManagerHelper.getInstance().fileExist(taskId, fileName);
        if (exist){
            final File file = SdkDownLoadManagerHelper.getInstance().getExistFile(taskId, fileName);
            if (null != file){
                //文件已存在，返回
                if (null != onSdkDownloadListener){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onSdkDownloadListener.onSuccess(file);
                        }
                    });
                }
            }
            return;
        }

        Log.e("sdkDownloader", "=====下载地址==: " + downloadPath  + "    文件名: " + fileName);

        SdkDownLoadManager sdkDownLoadManager = SdkDownLoadManagerHelper.getInstance().getDownLoadManager();
        sdkDownLoadManager.setSupportBreakpoint(supportBreakpoint);
        sdkDownLoadManager.addTask(taskId, downloadPath, fileName);

        sdkDownLoadManager.setSingleTaskListener(taskId, new SdkDownLoadListener() {
            @Override
            public void onStart(SdkSQLDownLoadInfo sqlDownLoadInfo) {
                if (null != onSdkDownloadListener){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onSdkDownloadListener.onStart();
                        }
                    });
                }
            }

            @Override
            public void onProgress(SdkSQLDownLoadInfo it, boolean isSupportBreakpoint) {
                final int progress = (int) (it.getDownloadSize() * 100 / it.getFileSize());
                firstErrorTime = 0;

                if (null != onSdkDownloadListener){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onSdkDownloadListener.onProgress(progress);
                        }
                    });
                }
            }

            @Override
            public void onStop(SdkSQLDownLoadInfo sqlDownLoadInfo, boolean isSupportBreakpoint) {

            }

            @Override
            public void onError(SdkSQLDownLoadInfo sqlDownLoadInfo) {
                handler.removeMessages(downLoadError);
                handler.sendEmptyMessageDelayed(downLoadError , 2000);
            }

            @Override
            public void onSuccess(SdkSQLDownLoadInfo it) {
                firstErrorTime = 0;
                try {
                    final File file = new File(it.getFilePath());

                    //回调，下载成功
                    if (null != onSdkDownloadListener){
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                onSdkDownloadListener.onSuccess(file);
                            }
                        });
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    if (null != onSdkDownloadListener){
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                onSdkDownloadListener.onError();
                            }
                        });
                    }
                }
            }
        });
        sdkDownLoadManager.startTask(taskId);
    }


}
