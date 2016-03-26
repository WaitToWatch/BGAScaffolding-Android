package cn.bingoogolapple.basenote.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.zhy.changeskin.SkinManager;

import java.util.Stack;

import cn.bingoogolapple.basenote.App;
import cn.bingoogolapple.basenote.R;

/**
 * 作者:王浩 邮件:bingoogolapple@gmail.com
 * 创建时间:16/3/21 上午1:25
 * 描述:
 */
public class AppManager implements Application.ActivityLifecycleCallbacks {
    private static AppManager sInstance;
    private int mActivityStartedCount = 0;
    private long mLastPressBackKeyTime;
    private Stack<Activity> mActivityStack = new Stack<>();
    private Delegate mDelegate;
    private Context mContext;

    private AppManager() {
    }

    public static final AppManager getInstance() {
        if (sInstance == null) {
            synchronized (AppManager.class) {
                if (sInstance == null) {
                    sInstance = new AppManager();
                }
            }
        }
        return sInstance;
    }

    public AppManager init(Context context) {
        mContext = context.getApplicationContext();
        return this;
    }

    public void setDelegate(Delegate delegate) {
        mDelegate = delegate;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        mActivityStack.add(activity);

        SkinManager.getInstance().register(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (mActivityStartedCount == 0 && mDelegate != null) {
            mDelegate.onEnterFrontStage();
        }
        mActivityStartedCount++;
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        mActivityStartedCount--;
        if (mActivityStartedCount == 0 && mDelegate != null) {
            mDelegate.onEnterBackStage();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        mActivityStack.remove(activity);

        SkinManager.getInstance().unregister(activity);
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
        while (true) {
            Activity activity = currentActivity();
            if (activity == null) {
                break;
            }
            popOneActivity(activity);
        }
    }

    /**
     * 获取当前版本名称
     *
     * @return
     */
    public String getCurrentVersionName() {
        try {
            return App.getInstance().getPackageManager().getPackageInfo(App.getInstance().getPackageName(), 0).versionName;
        } catch (Exception e) {
            // 利用系统api getPackageName()得到的包名，这个异常根本不可能发生
            return null;
        }
    }

    public interface Delegate {
        void onEnterFrontStage();

        void onEnterBackStage();
    }
}