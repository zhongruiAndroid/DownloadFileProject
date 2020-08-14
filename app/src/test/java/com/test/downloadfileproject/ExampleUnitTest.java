package com.test.downloadfileproject;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
        String url = "http://vfx.mtime.cn/Video/2019/03/19/mp4/190319222227698228.mp4";
        String url2 = "http://vfx.mtime.cn/Video/2019/03/19/mp4/190319222227698228.mp4/";
        int i = url.lastIndexOf(".");
        String substring = url.substring(i);
        System.out.println(url.hashCode());
        System.out.println(url2.hashCode());
        System.out.println(url.hashCode() & Integer.MAX_VALUE);
        System.out.println(url2.hashCode() & Integer.MAX_VALUE);
    }

    @Test
    public void adsddf() throws IOException {
//        String url = "https://b4fc69b7b91b11258cf93c80ebe77d53.dd.cdntips.com/imtt.dd.qq.com/16891/apk/FBAF111EE8D5AE9810A79EFA794901AA.apk?mkey=5f0db2308ccf356e&f=9870&fsname=cn.nubia.nubiashop_1.6.3.1021_77.apk&csr=1bbd&cip=140.207.19.155&proto=https";
        String url = "https://b4fc69b7b91b11258cf93c80ebe77d53.dd.cdntips.com/imtt.dd.qq.com/16891/apk/FBAF111EE8D5AE9810A79EFA794901AA.apk";
        System.out.println(url.split("\\?")[0]);
    }



    public void asddf() throws IOException {
        File file = new File("f:/a/b/ccc.txt");
        boolean delete = file.delete();

        System.out.println(delete);
        System.out.println("==========d===========");


    }

    @Test
    public void adasfdsdddddf() {
        System.out.println(new Date());
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyMMdd");
        System.out.println(simpleDateFormat.format(new Date()));
    }

    @Test
    public void adasfdasfsdddddf() {
        StringBuilder stringBuilder=new StringBuilder();
        stringBuilder.append("1,");
        stringBuilder.append("2,");
        stringBuilder.append("3,");
        int index = stringBuilder.lastIndexOf(",");
        System.out.println(index);
        if(index!=-1){
            stringBuilder.deleteCharAt(index);
        }
        System.out.println(stringBuilder.toString());
    }
    @Test
    public void adasfdsddddf() {
        Map<String, String> userInfo = new HashMap<>();
        String json = "{\"level\":\"10\",\"attribute\":\"11,22,33,44\",\"equipment\":\"1001,1002,1003,1004,null\",\"spbeing\":\"null,3002,3003,null,3005,null\"}";
        try {
            JSONObject jsonObject = new JSONObject(json);
            String string2 = jsonObject.optString("level");
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String next = keys.next();
                String string = jsonObject.optString(next);
                userInfo.put(next, string);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void adasfdsdddf() {
        File file = new File("f:/a/test.txt");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        // 创建文件
        try {
//            file.createNewFile();
            // creates a FileWriter Object
            FileWriter writer = new FileWriter(file, false);
            // 向文件写入内容
            writer.write("This\n is\n an\n example\n");
            writer.write("33");
            writer.flush();
            writer.close();
            // 创建 FileReader 对象
            FileReader fr = new FileReader(file);
            char[] a = new char[50];
            fr.read(a); // 从数组中读取内容
            for (char c : a)
                System.out.print(c); // 一个个打印字符
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void adasfdsddf() {

        File file = new File("f:/a/aa.txt");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        ObjectOutputStream outputStream = null;
        try {
            outputStream = new ObjectOutputStream(new FileOutputStream(file, true));


            TestA t = new TestA();
            t.setName("a");
            outputStream.writeObject(t);
            outputStream.flush();
            outputStream.close();

            outputStream = new ObjectOutputStream(new FileOutputStream(file));
            TestA t2 = new TestA();
            t2.setName("b");
            outputStream.writeObject(t2);

            outputStream.flush();
            outputStream.close();

            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            TestA user = (TestA) in.readObject();
//            TestA user1 = (TestA) in.readObject();
            in.close();
            System.out.println(user.getName());
//            System.out.println(user1.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class TestA implements Serializable {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}