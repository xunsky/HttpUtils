package xunsky.net.okhttp;

import android.os.Looper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by junx on 2018/8/15.
 */

class Utils {
    /**
     * get请求时拼接url的工具方法
     */
    protected static String getUrl(String url, HashMap map) {
        if (map == null || map.size() == 0) {
            return url;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        sb.append("?");

        Iterator iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            sb.append(key);
            sb.append("=");
            sb.append(value);
            sb.append("&");
        }
        url = sb.substring(0, sb.length() - 1);
        return url;
    }

    /**
     * 判断当前是否为主线程的工具方法
     */
    protected static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }

    /**
     * 回调
     */
    protected interface OnNet {
        void onSuccessed(String dataString);
        void onFailed(String ErrorString);
    }

    protected  static void closeIO(InputStream inputStream) {
        if (inputStream == null)
            return;
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected  static void closeIO(OutputStream outputStream) {
        if (outputStream == null)
            return;
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected  static String isExistDir(String saveDir) throws IOException {
        // 下载位置
        File downloadFile = new File(saveDir + File.separator + "xls");
        if (!downloadFile.mkdirs()) {
            downloadFile.createNewFile();
        }
        String savePath = downloadFile.getAbsolutePath();
        return savePath;
    }

    protected  static String getNameFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
}
