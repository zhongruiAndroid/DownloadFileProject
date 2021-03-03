package com.github.downloadfile;

import android.os.SystemClock;
import android.util.Log;

import com.github.downloadfile.bean.DownloadRecord;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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

    @Test
    public void sfs() {
        AtomicLong atomicLong = new AtomicLong(0);
        System.out.println(atomicLong.getAndAdd(10) + "=========");
        System.out.println(atomicLong.get() + "=========");
    }

    @Test
    public void ko() {
        try {
            int a = 1;
            while (a < 3) {
                System.out.println("1111111131111");
                return;
            }
            System.out.println("222");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("finally");
        }
    }

    private AtomicReference<DownloadRecord> atomicReference;

    @Test
    public void adddd2sf() {
        String u = "";
        System.out.println(u.hashCode());
    }

    @Test
    public void addddsf() {
        DownloadRecord downloadRecord = new DownloadRecord(0, 1);
        for (DownloadRecord.FileRecord fileRecord : downloadRecord.getFileRecordList()) {
            fileRecord.setStartPoint(0);
        }
        atomicReference = new AtomicReference<>();
        atomicReference.set(downloadRecord);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    atomicReference.get().getFileRecordList().get(0).setStartPoint(atomicReference.get().getFileRecordList().get(0).getStartPoint() + 1);
                }

            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    atomicReference.get().getFileRecordList().get(1).setStartPoint(atomicReference.get().getFileRecordList().get(0).getStartPoint() + 1);
                }

            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    atomicReference.get().getFileRecordList().get(2).setStartPoint(atomicReference.get().getFileRecordList().get(0).getStartPoint() + 1);
                }

            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(1000);
            }
        }).start();
    }

    @Test
    public void asf() {
        String url = "";
        String encode = MD5Coder.encode(url);
        System.out.println(encode);
    }

    @Test
    public void adsf() {
        File file = new File("f:/a/aa/aa.txt");
        System.out.println(file.isDirectory());
        System.out.println(file.isFile());
        if (!file.exists()) {
            file.mkdirs();
        }
        boolean directory = file.isDirectory();
        boolean file1 = file.isFile();
        System.out.println(directory + "====" + file1);
        try {
            boolean newFile = file.createNewFile();
            System.out.println(newFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void asdf() {
        long l = System.currentTimeMillis();
        String encode = MD5Coder.encode("http://flashmedia.eastday.com/newdate/news/2016-11/shznews1125-19.mp4");
        for (int i = 0; i < 1000; i++) {
            MD5Coder.encode("http://flashmedia.eastday.com/newdate/news/2016-11/shznews1125-19.mp4");
        }
        long l2 = System.currentTimeMillis();
        System.out.println(l2 - l);
        System.out.println(encode);

    }

    @Test
    public void as2df() {
        long l = System.currentTimeMillis();
        String encode2 = MD5Coder.encode("http://flashmedia.eastday.com/newdate/news/2016-11/shznews1125-19.mp4");
        for (int i = 0; i < 1000; i++) {
            MD5Coder.encode("http://flashmedia.eastday.com/newdate/news/2016-11/shznews1125-19.mp4");
        }
        long l2 = System.currentTimeMillis();
        System.out.println(l2 - l);
        System.out.println(encode2);

        //1d269889fd5d2cc197221f63db4d7343
        //1d269889fd5d2cc197221f63db4d7343

    }

    @Test
    public void asasdf2df() {
        AtomicInteger atomicInteger = new AtomicInteger();
        long l = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            atomicInteger.addAndGet(10);
        }
        long l2 = System.currentTimeMillis();
        System.out.println(l2 - l);
    }

    @Test
    public void asasddf2df() {
        int atomicInteger = 0;
        long l = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            atomicInteger = atomicInteger + 10;
        }
        long l2 = System.currentTimeMillis();
        System.out.println(l2 - l);
    }
}