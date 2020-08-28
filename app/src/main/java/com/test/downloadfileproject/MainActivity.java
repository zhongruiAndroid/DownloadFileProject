package com.test.downloadfileproject;


import android.Manifest;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    private ProgressBar pbProgress;
    private TextView tvProgress;
    private TextView tvSpeed;

    private Button btPause;
    private Button btDelete;
    private DownloadInfo downloadInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvSpeed= findViewById(R.id.tvSpeed);
        btPause= findViewById(R.id.btPause);
        btPause.setOnClickListener(this);
        btDelete= findViewById(R.id.btDelete);
        btDelete.setOnClickListener(this);
        tvProgress= findViewById(R.id.tvProgress);
        pbProgress= findViewById(R.id.pbProgress);
        View bt = findViewById(R.id.bt);
        bt.setOnClickListener(this);

        DownloadManager.init(this);


        long totalSpace = DownloadManager.getContext().getFilesDir().getFreeSpace();

        long totalSpace1 = DownloadManager.getContext().getCacheDir().getFreeSpace();
        long totalSpace2 = DownloadManager.getContext().getExternalFilesDir("download").getFreeSpace();
        long totalSpace3= DownloadManager.getContext().getExternalCacheDir().getFreeSpace();
        Log.i("=====",totalSpace+"====="+totalSpace1+"====="+totalSpace2+"====="+totalSpace3);

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
            case R.id.btPause:
                downloadInfo.changeStatus(DownloadInfo.STATUS_PAUSE);
                break;
            case R.id.btDelete:
                if (downloadInfo != null) {
                    downloadInfo.changeStatus(DownloadInfo.STATUS_DELETE);
                }
                break;
            case R.id.bt:
                if(false){
                    addddsf();
                    return;
                }
                start();

                break;
        }
    }
    long pre;
    long startTime;
    int i=0;
    private void start() {
        DownloadHelper.get().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
//                String url = "https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png";
                String url = "https://b4fc69b7b91b11258cf93c80ebe77d53.dd.cdntips.com/imtt.dd.qq.com/16891/apk/FBAF111EE8D5AE9810A79EFA794901AA.apk?mkey=5f0db2308ccf356e&f=9870&fsname=cn.nubia.nubiashop_1.6.3.1021_77.apk&csr=1bbd&cip=140.207.19.155&proto=https";
                DownloadConfig.Builder config=new DownloadConfig.Builder();
                config.setFileDownloadUrl(url);
                config.setThreadNum(3);
                config.setNeedSpeed(true);
                config.setIfExistAgainDownload(true);
                downloadInfo = new DownloadInfo(config.build(), new DownloadListener() {
                    @Override
                    public void onConnect(long totalSize) {
//                        tvProgress.setText("0/"+totalSize);
                        pbProgress.setMax((int) totalSize);
                        startTime=System.currentTimeMillis();
                        Log.i("=====", "=====onConnect:" + totalSize);
                    }

                    @Override
                    public void onSpeed(float speedBySecond) {
                        tvSpeed.setText("网速:"+speedBySecond+"kb/s");
                    }

                    @Override
                    public void onProgress(long progress,long totalSize) {
                        tvProgress.setText(progress+"/"+totalSize);
                        pbProgress.setProgress((int) progress);
                        if(pre>=progress){
                            Log.i("=====", pre+"=====onProgress:" + progress);
                        }
                        pre=progress;
                        Log.i("=====", totalSize+"=====Progress:" + progress);
                        i+=1;
                    }
                    @Override
                    public void onSuccess(File file) {
                       long now=System.currentTimeMillis();
                        Log.i("=====", (now-startTime)*1f/1000+"=====onSuccess:" + file.getAbsolutePath());
                    }
                    @Override
                    public void onPause() {
                        Log.i("=====", "=====onPause");
                    }

                    @Override
                    public void onDelete() {
                        Log.i("=====", "=====onDelete");
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
