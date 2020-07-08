package com.github.downloadfile.helper;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadHelper {
    /**********************************************************/
    private static DownloadHelper singleObj;
    private DownloadHelper() {
        handler=new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
    }
    public static DownloadHelper get(){
        if(singleObj==null){
            synchronized (DownloadHelper.class){
                if(singleObj==null){
                    singleObj=new DownloadHelper();
                }
            }
        }
        return singleObj;
    }
    /**********************************************************/

    private Handler handler;
    private ExecutorService executorService;
    public Handler getHandler() {
        return handler;
    }
    public ExecutorService getExecutorService() {
        return executorService;
    }

}
