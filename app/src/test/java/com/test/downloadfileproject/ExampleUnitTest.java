package com.test.downloadfileproject;

import org.junit.Test;

import java.io.File;

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
        int i = url.lastIndexOf(".");
        String substring = url.substring(i);
        System.out.println(url.hashCode());
    }
    @Test
    public void asddf() {
        File file=new File("f:/a/b/c");
        System.out.println(file.getName());
        System.out.println(file.getPath());
        System.out.println(file.getAbsolutePath());
        String absolutePath = file.getAbsolutePath();
        int i = absolutePath.lastIndexOf("\\");
        String substring = absolutePath.substring(i  );
        String substring2 = absolutePath.substring(i);
        System.out.println(substring);
        System.out.println(substring2);
    }
}