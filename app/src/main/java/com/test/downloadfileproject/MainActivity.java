package com.test.downloadfileproject;


import android.Manifest;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.github.downloadfile.DownloadConfig;
import com.github.downloadfile.DownloadInfo;
import com.github.downloadfile.helper.DownloadHelper;
import com.github.downloadfile.listener.DownloadListener;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View bt = findViewById(R.id.bt);
        bt.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt:
//                start();
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                        Manifest.permission.READ_PHONE_STATE},10001);
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},100);
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},102);
                break;
        }
    }

    private void start() {
        DownloadHelper.get().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
//                String url = "https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png";
                String url = "https://b4fc69b7b91b11258cf93c80ebe77d53.dd.cdntips.com/imtt.dd.qq.com/16891/apk/FBAF111EE8D5AE9810A79EFA794901AA.apk?mkey=5f0db2308ccf356e&f=9870&fsname=cn.nubia.nubiashop_1.6.3.1021_77.apk&csr=1bbd&cip=140.207.19.155&proto=https";
                DownloadConfig.Builder config=new DownloadConfig.Builder(MainActivity.this);
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
