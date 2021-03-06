package cn.bingoogolapple.scaffolding.util;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.orhanobut.logger.LogLevel;
import com.orhanobut.logger.Logger;

import java.util.Iterator;
import java.util.Stack;

import cn.bingoogolapple.scaffolding.R;

/**
 * 作者:王浩 邮件:bingoogolapple@gmail.com
 * 创建时间:16/3/21 上午1:25
 * 描述:
 */
public class AppManager implements Application.ActivityLifecycleCallbacks {
    private static final AppManager sInstance;
    private static final Application sApp;

    private int mActivityStartedCount = 0;
    private long mLastPressBackKeyTime;
    private Stack<Activity> mActivityStack = new Stack<>();

    private boolean mIsBuildDebug;
    private Delegate mDelegate;

    static {
        Application app = null;
        try {
            app = (Application) Class.forName("android.app.AppGlobals").getMethod("getInitialApplication").invoke(null);
            if (app == null)
                throw new IllegalStateException("Static initialization of Applications must be on main thread.");
        } catch (final Exception e) {
            Log.e(AppManager.class.getSimpleName(), "Failed to get current application from AppGlobals." + e.getMessage());
            try {
                app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
            } catch (final Exception ex) {
                Log.e(AppManager.class.getSimpleName(), "Failed to get current application from ActivityThread." + e.getMessage());
            }
        } finally {
            sApp = app;

            sInstance = new AppManager();
            sApp.registerActivityLifecycleCallbacks(sInstance);
        }
    }

    private AppManager() {
        // 初始化崩溃日志统计
        CrashHandler.getInstance().init();

        sApp.registerReceiver(new BroadcastReceiver() {
            private boolean mIsFirstReceiveBroadcast = true;

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    if (!mIsFirstReceiveBroadcast) {
                        if (NetUtil.isNetworkAvailable()) {
                            RxBus.send(new RxEvent.NetworkConnectedEvent());
                        } else {
                            RxBus.send(new RxEvent.NetworkDisconnectedEvent());
                        }
                    } else {
                        mIsFirstReceiveBroadcast = false;
                    }
                }
            }
        }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    /**
     * 必须在 Application 的 onCreate 方法中调用
     *
     * @param isBuildDebug 是否构建的是 debug
     */
    public void init(boolean isBuildDebug, Delegate delegate) {
        mIsBuildDebug = isBuildDebug;
        mDelegate = delegate;

        // 初始化日志打印库
        Logger.init(getInstance().getAppName()).logLevel(mIsBuildDebug ? LogLevel.FULL : LogLevel.NONE);
    }

    public static AppManager getInstance() {
        return sInstance;
    }

    public static Application getApp() {
        return sApp;
    }

    /**
     * 是否构建的是 debug
     *
     * @return
     */
    public boolean isBuildDebug() {
        return mIsBuildDebug;
    }

    public void refWatcherWatchFragment(Fragment fragment) {
        if (mDelegate != null) {
            mDelegate.refWatcherWatchFragment(fragment);
        }
    }

    public void handleServerException(HttpRequestException httpRequestException) {
        if (mDelegate != null) {
            mDelegate.handleServerException(httpRequestException);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        mActivityStack.add(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (mActivityStartedCount == 0) {
            RxBus.send(new RxEvent.AppEnterForegroundEvent());
        }
        mActivityStartedCount++;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        UmengUtil.onActivityResumed(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        UmengUtil.onActivityPaused(activity);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        mActivityStartedCount--;
        if (mActivityStartedCount == 0) {
            RxBus.send(new RxEvent.AppEnterBackgroundEvent());
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        mActivityStack.remove(activity);
    }

    public Activity currentActivity() {
        Activity activity = null;
        if (!mActivityStack.empty()) {
            activity = mActivityStack.lastElement();
        }
        return activity;
    }

    public void popOneActivity(Activity activity) {
        if (activity == null || mActivityStack.isEmpty()) {
            return;
        }
        if (!activity.isFinishing()) {
            activity.finish();
        }
        mActivityStack.remove(activity);
    }

    /**
     * 应用场景：支付完后，关闭 MainActivity 之外的其他页面
     *
     * @param activityClass
     */
    public void popOthersActivity(Class<Activity> activityClass) {
        if (activityClass == null || mActivityStack.isEmpty()) {
            return;
        }

        Iterator<Activity> iterator = mActivityStack.iterator();
        while (iterator.hasNext()) {
            Activity activity = iterator.next();
            if (!activity.getClass().equals(activityClass)) {
                activity.finish();
                iterator.remove();
            }
        }
    }

    /**
     * 双击后 全退出应用程序
     */
    public void exitWithDoubleClick() {
        if (System.currentTimeMillis() - mLastPressBackKeyTime <= 1500) {
            exit();
        } else {
            mLastPressBackKeyTime = System.currentTimeMillis();
            ToastUtil.show(R.string.toast_exit_tip);
        }
    }

    /**
     * 退出应用程序
     */
    public void exit() {
        try {
            while (true) {
                Activity activity = currentActivity();
                if (activity == null) {
                    break;
                }
                popOneActivity(activity);
            }

            // 如果开发者调用Process.kill或者System.exit之类的方法杀死进程，请务必在此之前调用MobclickAgent.onKillProcess(Context context)方法，用来保存统计数据
            UmengUtil.onKillProcess();

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        } catch (Exception e) {
            Logger.e("退出错误");
        }
    }

    /**
     * 获取应用名称
     *
     * @return
     */
    public String getAppName() {
        try {
            return sApp.getPackageManager().getPackageInfo(sApp.getPackageName(), 0).applicationInfo.loadLabel(sApp.getPackageManager()).toString();
        } catch (Exception e) {
            // 利用系统api getPackageName()得到的包名，这个异常根本不可能发生
            return "";
        }
    }

    /**
     * 获取当前版本名称
     *
     * @return
     */
    public String getCurrentVersionName() {
        try {
            return sApp.getPackageManager().getPackageInfo(sApp.getPackageName(), 0).versionName;
        } catch (Exception e) {
            // 利用系统api getPackageName()得到的包名，这个异常根本不可能发生
            return "";
        }
    }

    /**
     * 获取当前版本号
     *
     * @return
     */
    public int getCurrentVersionCode() {
        try {
            return sApp.getPackageManager().getPackageInfo(sApp.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            // 利用系统api getPackageName()得到的包名，这个异常根本不可能发生
            return 0;
        }
    }

    /**
     * 获取渠道号
     *
     * @return
     */
    private String getChannel() {
        try {
            ApplicationInfo appInfo = sApp.getPackageManager().getApplicationInfo(sApp.getPackageName(), PackageManager.GET_META_DATA);
            return appInfo.metaData.getString("UMENG_CHANNEL");
        } catch (Exception e) {
            return "";
        }
    }

    public interface Delegate {
        void refWatcherWatchFragment(Fragment fragment);

        void handleServerException(HttpRequestException httpRequestException);
    }
}
