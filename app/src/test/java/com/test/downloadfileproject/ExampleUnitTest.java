package com.test.downloadfileproject;

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
    public void asddf() throws IOException {
        File file=new File("f:/a/b/c.txt");
        System.out.println(file.getParent());
        String name = file.getName();
        System.out.println(name);

        String replace = name.replace("\\.", "(" + 1 + ")\\.");
        System.out.println(replace);
      /*  if(!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }*/
//        file.createNewFile();
        boolean delete = file.delete();
        System.out.println(delete);
        /*System.out.println(file.getName());
        System.out.println(file.getPath());
        System.out.println(file.getAbsolutePath());
        String absolutePath = file.getAbsolutePath();
        int i = absolutePath.lastIndexOf("\\");
        String substring = absolutePath.substring(i  );
        String substring2 = absolutePath.substring(i);
        System.out.println(substring);
        System.out.println(substring2);*/
    }
}