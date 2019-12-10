package xunsky.utils.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;

import xunsky.net.okhttp.NetUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
    }

    private void test(){
    }

    private void testNetUtils(){
        String url="https://acj4.pc6.com/pc6_soure/2019-10/com.taobao.idlefish_176.apk";
        String dir = getCacheDir().getAbsolutePath();
        Log.d("meee","("+Thread.currentThread().getStackTrace()[2].getFileName()+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+")\n"
                +"click");

        NetUtils.download(url, dir, "xianyu.apk", new NetUtils.OnDownloadListener() {
            @Override
            public void onDownloadSuccess(File file) {
//                Log.d("meee","("+Thread.currentThread().getStackTrace()[2].getFileName()+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+")\n"
//                        +"s:"+file.length());
            }

            @Override
            public void onDownloading(int progress) {
//                Log.d("meee","("+Thread.currentThread().getStackTrace()[2].getFileName()+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+")\n"
//                        +"p:"+progress);
            }

            @Override
            public void onDownloadFailed(Exception e) {
//                Log.d("meee","("+Thread.currentThread().getStackTrace()[2].getFileName()+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+")\n"
//                        +"fail:"+e.getMessage());
            }
        });
    }
}
