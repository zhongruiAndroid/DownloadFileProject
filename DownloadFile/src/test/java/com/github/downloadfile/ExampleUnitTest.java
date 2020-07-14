package com.github.downloadfile;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

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