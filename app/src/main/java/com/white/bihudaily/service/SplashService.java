package com.white.bihudaily.service;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

import com.white.bihudaily.api.BihuClient;
import com.white.bihudaily.app.BihuDailyApplication;
import com.white.bihudaily.app.Constant;
import com.white.bihudaily.bean.StartImg;
import com.white.bihudaily.utils.CommonUtil;
import com.white.bihudaily.utils.SPUtils;
import com.white.bihudaily.utils.TransformUtils;

import java.io.File;
import java.io.IOException;

import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;

/**
 * Author White
 * Date 2016/8/14
 * Time 12:16
 */
public class SplashService extends IntentService {

    public SplashService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // 后台下载图片
        BihuClient.getBihuService()
                .getStartImg().compose(TransformUtils.<StartImg>allIO())
                .doOnNext(new Consumer<StartImg>() {
                    @Override
                    public void accept(StartImg startImg) throws Exception {
                        //缓存版权信息
                        SPUtils.put(BihuDailyApplication.getAppContext(), Constant.KEY_START_IMG_TEXT, startImg.getText());
                        //保存当前日期
                        SPUtils.put(getApplicationContext(), Constant.KEY_TODAY, CommonUtil.getToday());
                        SPUtils.put(getApplicationContext(), Constant.IMAGE_URL, startImg.getImg());

                    }
                })
                .flatMap(new Function<StartImg, ObservableSource<ResponseBody>>() {
                    @Override
                    public ObservableSource<ResponseBody> apply(StartImg startImg) throws Exception {
                        return BihuClient.getBihuService().downloadImage(startImg.getImg());
                    }
                })
                .map(new Function<ResponseBody, String>() {
                    @Override
                    public String apply(ResponseBody responseBody) throws Exception {
                        String imageUrl = (String) SPUtils.get(getApplicationContext(), Constant.IMAGE_URL, "");
                        File file = saveFile(responseBody, getCacheDir().getAbsolutePath(),
                                TextUtils.isEmpty(imageUrl) ? "splash.jpg" :
                                        imageUrl.substring(imageUrl.lastIndexOf("/")));
                        return file.getAbsolutePath();
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        // 缓存图片
                        SPUtils.put(BihuDailyApplication.getAppContext(), Constant.KEY_START_IMG_PATH, s);

                    }
                });
    }

    public File saveFile(ResponseBody responseBodyResponse, String path, String fileName) {
        File futureStudioIconFile = new File(path, fileName);
        BufferedSink sink = null;
        try {
            sink = Okio.buffer(Okio.sink(futureStudioIconFile));
            sink.writeAll(responseBodyResponse.source());
            return futureStudioIconFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (sink != null) {
                    sink.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
