package cn.bingoogolapple.scaffolding.util;

import com.trello.rxlifecycle.LifecycleProvider;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * 作者:王浩 邮件:bingoogolapple@gmail.com
 * 创建时间:16/1/2 上午12:23
 * 描述:使用 RxJava 来实现 EventBus
 */
public class RxBus {
    private Subject<Object, Object> mBus;
    private static RxBus sInstance;

    private RxBus() {
        mBus = new SerializedSubject<>(PublishSubject.create());
    }

    public static RxBus getInstance() {
        if (sInstance == null) {
            // [1]
            synchronized (RxBus.class) {
                if (sInstance == null) {
                    //单例模式之双重检测：线程一在此之前线程二到达了位置[1],如果此处不二次判断，那么线程二执行到这里的时候还会重新new
                    sInstance = new RxBus();
                }
            }
        }
        return sInstance;
    }

    private Subject<Object, Object> getBus() {
        return mBus;
    }

    public static boolean hasObservers() {
        return getInstance().getBus().hasObservers();
    }

    public static void send(Object obj) {
        if (getInstance().hasObservers()) {
            getInstance().getBus().onNext(obj);
        }
    }

    public static Observable<Object> toObservable() {
        return getInstance().getBus();
    }

    public static <T> Observable<T> toObservable(Class<T> clazz) {
        return getInstance().toObservable().ofType(clazz).observeOn(AndroidSchedulers.mainThread());
    }

    public static <T> Observable<T> toObservableAndBindToLifecycle(Class<T> clazz, LifecycleProvider lifecycleProvider) {
        return getInstance().toObservable(clazz).compose(lifecycleProvider.bindToLifecycle());
    }
}