package com.test.downloadfileproject;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

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
    public void asdf() {
        String url="http://vfx.mtime.cn/Video/2019/03/19/mp4/190319222227698228.mp4";
        String url2="http://vfx.mtime.cn/Video/2019/03/19/mp4/190319222227698228.mp4/";
        int i = url.lastIndexOf(".");
        String substring = url.substring(i);
        System.out.println(url.hashCode() );
        System.out.println(url2.hashCode());
        System.out.println(url.hashCode()& Integer.MAX_VALUE);
        System.out.println(url2.hashCode()& Integer.MAX_VALUE);
    }
    @Test
    public void adsddf() throws IOException {
//        String url = "https://b4fc69b7b91b11258cf93c80ebe77d53.dd.cdntips.com/imtt.dd.qq.com/16891/apk/FBAF111EE8D5AE9810A79EFA794901AA.apk?mkey=5f0db2308ccf356e&f=9870&fsname=cn.nubia.nubiashop_1.6.3.1021_77.apk&csr=1bbd&cip=140.207.19.155&proto=https";
        String url = "https://b4fc69b7b91b11258cf93c80ebe77d53.dd.cdntips.com/imtt.dd.qq.com/16891/apk/FBAF111EE8D5AE9810A79EFA794901AA.apk";
        System.out.println(url.split("\\?")[0]);
    }
    @Test
    public void asddf() throws IOException {
        File file=new File("f:/a/b/ccc.txt");
        boolean delete = file.delete();

        System.out.println(delete );
        System.out.println("==========d===========");


    }
    @Test
    public void adasfdsddf() throws IOException {
        AtomicBoolean atomicBoolean=new AtomicBoolean(true);
        System.out.println(atomicBoolean.get());
        System.out.println(atomicBoolean.getAndSet(false));
        System.out.println(atomicBoolean.get());
    }
}