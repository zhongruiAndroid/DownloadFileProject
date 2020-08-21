package com.test.downloadfileproject;


import android.Manifest;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.github.downloadfile.DownloadConfig;
import com.github.downloadfile.DownloadInfo;
import com.github.downloadfile.DownloadManager;
import com.github.downloadfile.bean.DownloadRecord;
import com.github.downloadfile.helper.DownloadHelper;
import com.github.downloadfile.listener.DownloadListener;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

//    private AtomicReference<DownloadRecord> atomicReference;
    private DownloadRecord downloadRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View bt = findViewById(R.id.bt);
        bt.setOnClickListener(this);

        DownloadManager.init(this);

    }
    public void addddsf(){
        downloadRecord = new DownloadRecord(30000,"");
        downloadRecord.setThreadNum(3);
        for (DownloadRecord.FileRecord fileRecord: downloadRecord.getFileRecordList()) {
            fileRecord.setStartPoint(0);
        }
//        atomicReference=new AtomicReference<>();
//        atomicReference.set(downloadRecord);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10000; i++) {
                    downloadRecord.getFileRecordList().get(0).setStartPoint(downloadRecord.getFileRecordList().get(0).getStartPoint()+1);
                }
                Log.i("======","1======"+downloadRecord.toJson());
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10000; i++) {
                    downloadRecord.getFileRecordList().get(1).setStartPoint(downloadRecord.getFileRecordList().get(1).getStartPoint()+1);
                }
                Log.i("======","2======"+downloadRecord.toJson());
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10000; i++) {
                    downloadRecord.getFileRecordList().get(2).setStartPoint(downloadRecord.getFileRecordList().get(2).getStartPoint()+1);
                }
                Log.i("======","3======"+downloadRecord.toJson());
                DownloadRecord downloadRecord = MainActivity.this.downloadRecord.fromJson(MainActivity.this.downloadRecord.toJson());
                Log.i("======","4======"+ downloadRecord.toJson());
            }
        }).start();

    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt:
                if(false){
                    addddsf();
                    return;
                }
                start();

                break;
        }
    }

    private void start() {
        DownloadHelper.get().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
//                String url = "https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png";
                String url = "https://b4fc69b7b91b11258cf93c80ebe77d53.dd.cdntips.com/imtt.dd.qq.com/16891/apk/FBAF111EE8D5AE9810A79EFA794901AA.apk?mkey=5f0db2308ccf356e&f=9870&fsname=cn.nubia.nubiashop_1.6.3.1021_77.apk&csr=1bbd&cip=140.207.19.155&proto=https";
                DownloadConfig.Builder config=new DownloadConfig.Builder();
                config.setFileDownloadUrl(url);
                config.setIfExistAgainDownload(true);
                DownloadInfo downloadInfo = new DownloadInfo(config.build(), new DownloadListener() {
                    @Override
                    public void onConnect(long fileSizeKB) {
                        Log.i("=====", "=====onConnect:" + fileSizeKB);
                    }
                    @Override
                    public void onProgress(long downloadSizeKB) {
                        Log.i("=====", "=====onProgress:" + downloadSizeKB);
                    }
                    @Override
                    public void onSuccess(File file) {
                        Log.i("=====", "=====onSuccess:" + file.getAbsolutePath());
                    }
                    @Override
                    public void onPause() {
                        Log.i("=====", "=====onPause");
                    }
                    @Override
                    public void onCancel() {
                        Log.i("=====", "=====onCancel");
                    }
                    @Override
                    public void onError() {
                        Log.i("=====", "=====onError");
                    }
                });
                downloadInfo.download(url);
            }
        });
    }
}
