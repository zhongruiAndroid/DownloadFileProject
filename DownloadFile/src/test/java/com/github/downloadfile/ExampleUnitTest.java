package com.github.downloadfile;

import android.os.SystemClock;
import android.util.Log;

import com.github.downloadfile.bean.DownloadRecord;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
    private AtomicReference<DownloadRecord> atomicReference;
    @Test
    public void addddsf(){
        DownloadRecord downloadRecord = new DownloadRecord(30000, 3);
        for (DownloadRecord.FileRecord fileRecord:downloadRecord.getFileRecordList()) {
            fileRecord.setStartPoint(0);
        }
        atomicReference=new AtomicReference<>();
        atomicReference.set(downloadRecord);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    atomicReference.get().getFileRecordList().get(0).setStartPoint(atomicReference.get().getFileRecordList().get(0).getStartPoint()+1);
                }

            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    atomicReference.get().getFileRecordList().get(1).setStartPoint(atomicReference.get().getFileRecordList().get(0).getStartPoint()+1);
                }

            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    atomicReference.get().getFileRecordList().get(2).setStartPoint(atomicReference.get().getFileRecordList().get(0).getStartPoint()+1);
                }

            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(atomicReference.get().toString());
                Log.i("======","======"+atomicReference.get().toString());
                SystemClock.sleep(1000);
            }
        }).start();
    }
    @Test
    public void asf(){
        String url="";
        String encode = MD5Coder.encode(url);
        System.out.println(encode);
    }
    @Test
    public void adsf(){
        File file=new File("f:/a/aa/aa.txt");
        System.out.println(file.isDirectory());
        System.out.println(file.isFile());
        if(!file.exists()){
            file.mkdirs();
        }
        boolean directory = file.isDirectory();
        boolean file1 = file.isFile();
        System.out.println(directory+"===="+file1);
        try {
            boolean newFile = file.createNewFile();
            System.out.println(newFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}