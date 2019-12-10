package xunsky.net.okhttp;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by junx on 2018/7/14.
 */

public class NetUtils {
    private static boolean logEnabled = false;
    private static LogInterceptor.Logger localLogger;
    private static LogInterceptor sLogInterceptor = new LogInterceptor(
            new LogInterceptor.Logger() {
                @Override
                public void log(String message) {
                    if (logEnabled)
                        Log.d("LogInterceptor", message);
                    if (localLogger!=null){
                        localLogger.log(message);
                    }
                }
            }
    );
    public static void setLocalLogger(LogInterceptor.Logger logger){
        localLogger=logger;
    }

    public static void enableLog(boolean enabale) {
        logEnabled = enabale;
    }

    /**
     * 简单的配置
     */
    private static final OkHttpClient sClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addNetworkInterceptor(sLogInterceptor)
            .cookieJar(new CookieJar() {//okhttp默认不保存cookies
                private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    cookieStore.put(url.host(), cookies);
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> cookies = cookieStore.get(url.host());
                    return cookies != null ? cookies : new ArrayList<Cookie>();
                }
            })
            .build();

    public static OkHttpClient getOkhttpClient() {
        return sClient;
    }

    /**
     * 回调
     */
    private static Handler sHandler = new Handler(Looper.getMainLooper());

    /**
     * 线程池
     */
    private static ExecutorService sCacheExector = new ThreadPoolExecutor(4, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

    /*---------------------------------------------------Get-----------------------------------------------------------*/
    public static void get(String url, Params params, final OnNet getter) {
        get(url, params.commit(), getter);
    }

    public static void get(String url, HashMap<String, String> map, final OnNet getter) {
        final String urls = Utils.getUrl(url, map);
        Request request = new Request.Builder().url(urls).build();
        sClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                sHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        getter.onFailed("NetUtils get:" + e.getMessage() + " \n" + e.getCause(), e);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (response.code() == 200) {
                    try {
                        final String string = response.body().string();
                        sHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                getter.onSuccessed(string);
                            }
                        });
                    } catch (final IOException e) {
                        sHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                getter.onFailed("NetUtils get onSuccess:" + e.getMessage(), e);
                            }
                        });
                    }
                } else {
                    sHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            getter.onFailed("NetUtils get:" + response.code(), new RuntimeException("code != 200"));
                        }
                    });
                }
            }
        });
    }

    /*---------------------------------------------------Post-----------------------------------------------------------*/
    public static void post(final String url, Params params, final OnNet callback) {
        post(url, params.commit(), callback);
    }

    public static void post(final String url, final HashMap<String, String> map, final OnNet callback) {
        FormBody.Builder builder = new FormBody.Builder();
        for (String key : map.keySet()) {
            String value = map.get(key);
            builder.add(key, value);
        }
        FormBody body = builder.build();
        Request request = new Request.Builder().url(url).post(body).build();
        sClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                sHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailed("请求失败:" + e.getMessage(), e);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (response.code() == 200) {
                    final String string = response.body().string();
                    sHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccessed(string);
                        }
                    });
                } else {
                    sHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailed("http post code:" + response.code(), new RuntimeException("code != 200"));
                        }
                    });
                }
            }
        });
    }

    /*---------------------------------------------------restful-----------------------------------------------------------*/
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    public static void post(final String url, final String json, final OnNet callback) {
        RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, json);
        Request request = new Request.Builder().url(url).post(requestBody).build();
        sClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                sHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailed("请求失败:" + e.getMessage(), e);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (response.code() == 200) {
                    final String string = response.body().string();
                    sHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccessed(string);
                        }
                    });
                } else {
                    sHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailed("http post code:" + response.code(), new RuntimeException("code != 200"));
                        }
                    });
                }
            }
        });
    }
    /*---------------------------------------------------下载-----------------------------------------------------------*/

    public static void download(final String url, final String destFileDir, final String destFileName, final OnDownloadListener listener) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        getOkhttpClient()
                .newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call,final IOException e) {
                        // 下载失败监听回调
                        sHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onDownloadFailed(e);
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        InputStream is = null;
                        byte[] buf = new byte[2048];
                        int len = 0;
                        FileOutputStream fos = null;
                        File dir = new File(destFileDir);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }

                        final File file = new File(dir, destFileName);
                        try {
                            is = response.body().byteStream();
                            long total = response.body().contentLength();
                            fos = new FileOutputStream(file);
                            long sum = 0;
                            while ((len = is.read(buf)) != -1) {
                                fos.write(buf, 0, len);
                                sum += len;
                                final int progress = (int) (sum * 1.0f / total * 100);
                                sHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onDownloading(progress);
                                    }
                                });
                            }
                            fos.flush();
                            sHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onDownloadSuccess(file);
                                }
                            });

                        } catch (final Exception e) {
                            sHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onDownloadFailed(e);
                                }
                            });

                        } finally {
                            try {
                                if (is != null)
                                    is.close();
                            } catch (IOException e) {
                            }
                            try {
                                if (fos != null)
                                    fos.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                });
    }

    public interface OnDownloadListener {
        void onDownloadSuccess(File file);
        void onDownloading(int progress);
        void onDownloadFailed(Exception e);
    }

    /*---------------------------------------------------上传----------------------------------------------------------*/
    private static final MediaType MEDIA_OBJECT_STREAM = MediaType.parse("application/octet-stream");

    public static void upload(String url, HashMap<String, String> params, final OnNet callback, File... files) {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        if (params != null && params.size() != 0) {
            for (String key : params.keySet()) {
                String value = params.get(key);
                builder.addFormDataPart(key, value);
            }
        }
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                builder.addFormDataPart("" + i, file.getName(), RequestBody.create(MEDIA_OBJECT_STREAM, file));
            }
        }
        MultipartBody body = builder.build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        sClient.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, final IOException e) {
                        sHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailed("上传失败:" + e, e);
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (response.code() == 200) {
                            try {
                                final String string = response.body().string();
                                sHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onSuccessed(string);
                                    }
                                });
                            } catch (final IOException e) {
                                sHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onFailed("上传失败: on code==200" + e.getMessage(), e);
                                    }
                                });
                            }
                        } else {
                            sHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFailed("上传失败:" + response.code(), new RuntimeException("code != 200"));
                                }
                            });
                        }
                    }
                });
    }


    /*---------------------------------------------------工具方法-----------------------------------------------------------*/

    /**
     * 判断当前是否为主线程的工具方法
     */
    public static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }

    /**
     * 回调
     */
    public interface OnNet {
        void onSuccessed(String data);
        void onFailed(String cause, Exception ex);
    }
}
