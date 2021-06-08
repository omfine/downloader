package com.android.external.downloader.func;

import com.android.external.downloader.bean.SdkSQLDownLoadInfo;

/**
 * 类功能描述：</br>
 */
public interface SdkDownLoadListener {
    
    /**
     * (开始下载文件) 
     * @param sqlDownLoadInfo 下载任务对象
     */
    public void onStart(SdkSQLDownLoadInfo sqlDownLoadInfo);
    
    /**
     * (文件下载进度情况) 
     * @param sqlDownLoadInfo 下载任务对象
     * @param isSupportBreakpoint 服务器是否支持断点续传
     */
    public void onProgress(SdkSQLDownLoadInfo sqlDownLoadInfo, boolean isSupportBreakpoint);
    
    /**
     * (停止下载完毕) 
     * @param sqlDownLoadInfo 下载任务对象
      * @param isSupportBreakpoint 服务器是否支持断点续传
     */
    public void onStop(SdkSQLDownLoadInfo sqlDownLoadInfo, boolean isSupportBreakpoint);
    
    /**
     * (文件下载失败) 
     * @param sqlDownLoadInfo 下载任务对象
     */
    public void onError(SdkSQLDownLoadInfo sqlDownLoadInfo);
    
    
    /**
     * (文件下载成功) 
     * @param sqlDownLoadInfo 下载任务对象
     */
    public void onSuccess(SdkSQLDownLoadInfo sqlDownLoadInfo);
}
