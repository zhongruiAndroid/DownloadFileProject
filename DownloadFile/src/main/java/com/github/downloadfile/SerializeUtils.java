package com.github.downloadfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializeUtils {
    public static <T> T toObjectSyn(final File file) {
        if (file == null) {
            return null;
        }
        if (!file.exists()) {
            return null;
        }
        ObjectInputStream in = null;
        T user = null;
        try {
            in = new ObjectInputStream(new FileInputStream(file));
            user = (T) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return user;
    }
    public static <T extends Object> boolean toSerialSyn(T data,File file) {
        if (data == null || file==null) {
            return false;
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        ObjectOutputStream out = null;
        boolean status = false;
        try {
            out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(data);
            status = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return status;
    }

}
