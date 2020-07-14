package com.test.downloadfileproject;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.github.downloadfile.DownloadInfo;
import com.github.downloadfile.helper.DownloadHelper;

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
        switch (v.getId()){
            case R.id.bt:
                start();
            break;
        }
    }

    private void start() {
        DownloadHelper.get().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                DownloadInfo downloadInfo = new DownloadInfo(null,null);
                String url="https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png";
                downloadInfo.download(url);
            }
        });
    }
}
