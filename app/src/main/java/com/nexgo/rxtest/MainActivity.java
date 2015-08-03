package com.nexgo.rxtest;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    final Handler handler = new Handler(); // bound to this thread
    private Logger log;
    private Action1<String> onNextAction = new Action1<String>() {
        @Override
        public void call(String s) {
            log.debug(s);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        log = LoggerFactory.getLogger(this.getClass().getSimpleName());
    }

    public void onUIThread(View view) {
        Observable.just("one", "two", "three", "four", "five")
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onNextAction);
    }

    public void onThread(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Observable.just("one", "two", "three", "four", "five")
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.handlerThread(handler))
                        .subscribe(onNextAction);

                // perform work, ...
            }
        }, "custom-thread-1").start();
    }

    public void onTimeout(View view) {
        testTimer();
    }

    private Observable<Long> testTimer() {
        log.debug("timer start!");
        Observable<Long> obs = Observable.timer(6, TimeUnit.SECONDS)
                //set timeout
                .timeout(5, TimeUnit.SECONDS)
                        //set retry
                .retry(1)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());

        obs.subscribe(new Action1<Long>() {
                          @Override
                          public void call(final Long aLong) {
                              log.debug("timer up {}.", aLong);
                          }
                      },
                new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        log.debug("timer error:", throwable);
                    }
                },
                new Action0() {
                    @Override
                    public void call() {
                        log.debug("timer complete");
                    }
                });
        return obs;
    }
}
