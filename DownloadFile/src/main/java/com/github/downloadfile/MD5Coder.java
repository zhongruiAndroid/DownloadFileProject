//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.github.downloadfile;

import java.io.File;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Coder {
    private static final char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public MD5Coder() {
    }

    public static String md5(String inStr) {
        byte[] inStrBytes = inStr.getBytes();

        try {
            MessageDigest MD = MessageDigest.getInstance("MD5");
            MD.update(inStrBytes);
            byte[] mdByte = MD.digest();
            char[] str = new char[mdByte.length * 2];
            int k = 0;

            for (int i = 0; i < mdByte.length; ++i) {
                byte temp = mdByte[i];
                str[k++] = hexDigits[temp >>> 4 & 15];
                str[k++] = hexDigits[temp & 15];
            }

            return new String(str);
        } catch (NoSuchAlgorithmException var8) {
            var8.printStackTrace();
            return null;
        }
    }
    public static String encode(String str) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes("UTF-8"));
            byte[] messageDigest = md5.digest();
            StringBuilder hexString = new StringBuilder();
            byte[] var4 = messageDigest;
            int var5 = messageDigest.length;

            for (int var6 = 0; var6 < var5; ++var6) {
                byte b = var4[var6];
                hexString.append(String.format("%02X", b));
            }

            return hexString.toString().toLowerCase();
        } catch (Exception var8) {
            var8.printStackTrace();
            return "";
        }
    }


}
