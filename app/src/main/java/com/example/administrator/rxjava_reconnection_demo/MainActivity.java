package com.example.administrator.rxjava_reconnection_demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    public static final String TAG="RxJava";
    public static final String base_url= "http://fy.iciba.com/";
    private int maxConnectCount=10;
    private int currentRetryCount=0;
    private int waitRetryTime=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //①创建Retrofit对象
        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl(base_url)
                .addConverterFactory(GsonConverterFactory.create())//支持gson解析
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())//支持RxJava
                .build();
        //②创建网络请求接口实例
        GetRequest_Interface request=retrofit.create(GetRequest_Interface.class);
        //③封装网络请求
        final Observable<Translation> observable=request.getCall();
        //④发送网络请求&通过retryWhen（）进行重试
        observable.retryWhen(new Function<Observable<Throwable>, ObservableSource<?>>() {
            @Override
            public ObservableSource<?> apply(Observable<Throwable> throwableObservable) throws Exception {
                // 参数Observable<Throwable>中的泛型 = 上游操作符抛出的异常，可通过该条件来判断异常的类型
                return throwableObservable.flatMap(new Function<Throwable, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Throwable throwable) throws Exception {
                        Log.e(TAG,"发生异常="+throwable.toString());//输出异常信息
                        /**
                         * 需求1：根据异常类型选择是否重试
                         * 即，当发生的异常 = 网络异常 = IO异常 才选择重试
                         */
                        if (throwable instanceof IOException){
                            Log.e(TAG,"属于IO异常，需重试");
                            /**
                             * 需求2：限制重试次数
                             * 即，当重试次数<设置的重试次数，才选择重试
                             */
                            if (currentRetryCount<maxConnectCount){
                                currentRetryCount++;
                                Log.d(TAG,"重试次数="+currentRetryCount);

                                /**
                                 * 需求2：实现重试
                                 * 通过返回的Observable发送的事件 = Next事件，从而使得retryWhen（）重订阅，最终实现重试功能
                                 *
                                 * 需求3：延迟1段时间再重试
                                 * 采用delay操作符 = 延迟一段时间发送，以实现重试间隔设置
                                 *
                                 * 需求4：遇到的异常越多，时间越长
                                 * 在delay操作符的等待时间内设置 = 每重试1次，增多延迟重试时间1s
                                 **/
                                    waitRetryTime=1000+currentRetryCount*1000;
                                    Log.d(TAG,"等待时间="+waitRetryTime);
                                    return observable.just(1).delay(waitRetryTime,TimeUnit.MILLISECONDS);


                            }else{
                                // 若重试次数已 > 设置重试次数，则不重试
                                // 通过发送error来停止重试（可在观察者的onError（）中获取信息）
                                return Observable.error(new Throwable("重试次数已超过次数="+currentRetryCount+",不在重试"));

                            }
                        }
                        //若发生的异常不为IO异常,则不重试
                        else{
                            return Observable.error(new Throwable("发生了非网络异常（非I/O异常）"));
                        }

                    }
                });
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Translation>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Translation value) {
                          Log.d(TAG,"发送成功");
                          value.getContent().showOut();
                    }

                    @Override
                    public void onError(Throwable e) {
                      Log.v(TAG,e.toString());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}
