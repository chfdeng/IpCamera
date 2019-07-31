package org.zero.ipcamera.utils;

import android.util.Base64;

import org.zero.ipcamera.BaseApplication;
import org.zero.ipcamera.model.AuthInfo;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * Created by cfd on 2019/7/17.
 */
public class IpcUtils {

    public static AuthInfo createAuthInfo(String name, String password) {
        AuthInfo authInfo = new AuthInfo();
        authInfo.setName(name);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'", Locale.getDefault());
        String date = format.format(new Date());
        authInfo.setCreate(date);
        String nonce = getNonce();
        authInfo.setNonce(nonce);

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] b1 = Base64.decode(nonce.getBytes(), Base64.DEFAULT);
            byte[] b2 = date.getBytes(); // "2013-09-17T09:13:35Z"
            byte[] b3 = password.getBytes();
            md.update(b1, 0, b1.length);
            md.update(b2, 0, b2.length);
            md.update(b3, 0, b3.length);
            byte[] b4 = md.digest();
            String result = Base64.encodeToString(b4, Base64.DEFAULT);
            authInfo.setAuthPassword(result.replace("\n", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return authInfo;
    }

    private static String getNonce() {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 24; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    public static byte[] readFromAsset(String filePath) {
        InputStream is = null;
        byte[] bytes = null;
        try {
            is = BaseApplication.getAppContext().getAssets().open(filePath);
            int length = is.available();
            bytes = new byte[length];
            is.read(bytes, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (bytes != null) {
            //xml文本放入xml文件并格式化时会自动添加换行和缩进，会造成解析出来的文本多了换行和缩进而与预期不符，也会增加字节数有可能超出接收方的限制导致无法解析
            bytes = new String(bytes).replaceAll("\r\n", "").replaceAll("    ", "").getBytes();
        }
        return bytes;
    }
}
