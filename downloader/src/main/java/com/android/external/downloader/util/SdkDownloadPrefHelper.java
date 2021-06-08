package com.android.external.downloader.util;

import com.android.external.downloader.func.SdkDownLoadManagerHelper;

public class SdkDownloadPrefHelper {

    //最后一次更新App的时间
    public static void updateLastAppUpdateTimestamp(String taskId , String fileName) {
        SdkDownloadPrefUtil.instance().setLongPref("lastUpdateAppTime_" + taskId + "_" + fileName, System.currentTimeMillis() / 1000);
    }

    public static Boolean isUpdateExpired(String taskId , String fileName) {
        long ctime = System.currentTimeMillis() / 1000;
        long lastUpdateTime = SdkDownloadPrefUtil.instance().getLongPref("lastUpdateAppTime_" + taskId + "_" + fileName, ctime);
        long diff = Math.abs(ctime - lastUpdateTime);
        //超过一天时设过过期//86400
        return diff >= SdkDownLoadManagerHelper.getInstance().getExpiredPeriodTime();
    }

}
