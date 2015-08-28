package com.nexgo.rxtest;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.ReplaySubject;

public class MainActivity extends AppCompatActivity {
    final Handler handler = new Handler(); // bound to this thread
    private BehaviorSubject<LocalEvent> subject_start = BehaviorSubject.create(LocalEvent.START);
    private ReplaySubject<LocalEvent> subject = ReplaySubject.create();
    private Logger log;
    private Action1<String> onNextAction = new Action1<String>() {
        @Override
        public void call(String s) {
            log.debug(s);
        }
    };
    private Action1<LocalEvent> next = new Action1<LocalEvent>() {

        @Override
        public void call(final LocalEvent localEvent) {
            log.debug("onNext {}.", localEvent);
        }
    };
    private Action1<Throwable> error = new Action1<Throwable>() {

        @Override
        public void call(final Throwable throwable) {
            log.debug("onError {}.", throwable.toString());
        }
    };
    private Action0 end = new Action0() {

        @Override
        public void call() {
            log.debug("onComplete.");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        log = LoggerFactory.getLogger(this.getClass().getSimpleName());
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    public void onUIThread(View view) {
/*
        Observable.just("one", "two", "three", "four", "five")
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onNextAction);
*/

        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                subscriber.onNext("1");
                subscriber.onNext("2");
                subscriber.onNext("3");
                subscriber.onError(new Throwable("myerror"));
                subscriber.onCompleted();
            }
        })
                .retry(2)
//                .subscribeOn(Schedulers.newThread())
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onNextAction);
    }

    public void onThread(View view) {
/*
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
*/
        subject_start.asObservable()
                .timeout(1, TimeUnit.SECONDS)
                .retry(2)
                .subscribe(next, error);
    }

    public void onTimeout(View view) {
        testsubject2();
    }

    private void testsubject2() {
        log.debug("test start!");
        subject_start = BehaviorSubject.create(LocalEvent.START);
        subject_start.asObservable()
                .doOnNext(next)
                .doOnCompleted(end)
                .doOnError(error)
                .retry(2)
                .subscribe(next, error, end);
        subject_start.onNext(LocalEvent.NEXT);

        Observable.timer(500, TimeUnit.MILLISECONDS)
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(final Long aLong) {
                        subject_start.onNext(LocalEvent.END);
                    }
                });

        Observable.timer(2, TimeUnit.SECONDS)
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(final Long aLong) {
                        subject_start.onError(new Throwable("EventFail"));
                    }
                });

        log.debug("has called");
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

    private void testsubject() {
        log.debug("test start!");
        log.debug("timer start1");
        subject.asObservable()
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        log.debug("doOnError {}.", throwable.toString());
                    }
                })

                .doOnNext(new Action1<LocalEvent>() {
                    @Override
                    public void call(final LocalEvent event) {
                        log.debug("doOnNext {}.", event);
                        if (event == LocalEvent.FAIL) {
                            throw new RuntimeException("Item exceeds FAIL value");
                        }
                    }
                })

                .retry(2)
//                .timeout(5, TimeUnit.SECONDS)
                .subscribe(
                        new Action1<LocalEvent>() {
                            @Override
                            public void call(final LocalEvent event) {
                                log.debug("onNext {}.", event);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(final Throwable throwable) {
                                log.debug("onError {}.", throwable.toString());
                            }
                        },
                        new Action0() {
                            @Override
                            public void call() {
                                log.debug("onComplete.");
                            }
                        });
        log.debug("timer start1");
        subject.onNext(LocalEvent.START);

        Observable.timer(500, TimeUnit.MILLISECONDS)
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(final Long aLong) {
                        EventBus.getDefault().post(new EventNext());
                    }
                });

        Observable.timer(2, TimeUnit.SECONDS)
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(final Long aLong) {
                        EventBus.getDefault().post(new EventFail());
                    }
                });

        log.debug("has called");
    }

    public void onEvent(EventDone event) {
        log.debug("has get {}", event.getClass().getSimpleName());
        subject.onCompleted();
    }

    public void onEvent(EventNext event) {
        log.debug("has get {}", event.getClass().getSimpleName());
        subject.onNext(LocalEvent.NEXT);
    }

    public void onEvent(EventFail event) {
        log.debug("has get {}", event.getClass().getSimpleName());
        subject.onNext(LocalEvent.FAIL);
//        subject.onError(new Throwable("EventFail"));
    }

    private void test() {

        BehaviorSubject<LocalEvent> subject2 = BehaviorSubject.create(LocalEvent.START);
        subject2.asObservable()
                .timeout(60, TimeUnit.MILLISECONDS)
                .subscribe(new Action1<LocalEvent>() {
                               @Override
                               public void call(final LocalEvent event) {
                                   log.debug("onNext2 {}.", event);
                               }
                           },
                        new Action1<Throwable>() {
                            @Override
                            public void call(final Throwable throwable) {
                                log.debug("onError2 {}.", throwable);
                            }
                        },
                        new Action0() {
                            @Override
                            public void call() {
                                log.debug("onComplete2.");
                            }
                        });

        BehaviorSubject<LocalEvent> subject3 = BehaviorSubject.create(LocalEvent.START);
        subject3.asObservable()
                .timeout(60, TimeUnit.MILLISECONDS)
                .subscribe(new Action1<LocalEvent>() {
                               @Override
                               public void call(final LocalEvent event) {
                                   log.debug("onNext3 {}.", event);
                               }
                           },
                        new Action1<Throwable>() {
                            @Override
                            public void call(final Throwable throwable) {
                                log.debug("onError3 {}.", throwable);
                            }
                        },
                        new Action0() {
                            @Override
                            public void call() {
                                log.debug("onComplete3.");
                            }
                        });
    }

    private enum LocalEvent {
        START,
        NEXT,
        FAIL,
        END
    }

    public static class EventDone {
    }

    public static class EventNext {
    }

    public static class EventFail {
    }
}
