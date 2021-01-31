//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.github.downloadfile;

import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Coder {
    @Deprecated
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
            return System.currentTimeMillis()+"";
        }
    }
    /*目前发现的方案中最快的一种*/
    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return null;
        }
        try {
            // 生成一个MD5加密计算摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算md5函数
            md.update(string.getBytes());
            // digest()最后确定返回md5 hash值，返回值为8位字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
            // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
            //一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方）
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
