package cn.bingoogolapple.bottomnavigation.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.PopupWindow;

import java.util.List;

import cn.bingoogolapple.basenote.util.ToastUtil;
import cn.bingoogolapple.bottomnavigation.R;
import cn.bingoogolapple.bottomnavigation.activity.WebViewActivity;
import cn.bingoogolapple.bottomnavigation.model.HomeCategory;
import cn.bingoogolapple.bottomnavigation.pw.HomeCategoryPopupWindow;

/**
 * 作者:王浩 邮件:bingoogolapple@gmail.com
 * 创建时间:15/7/3 下午8:29
 * 描述:
 */
public class HomeFragment extends BaseMainFragment {
    private HomeCategoryPopupWindow mCategoryPw;
    private List<HomeCategory> mHomeCategorys;

    @Override
    protected void initView(Bundle savedInstanceState) {
        setContentView(R.layout.fragment_home);
    }

    @Override
    protected void setListener() {
    }

    @Override
    protected void processLogic(Bundle savedInstanceState) {
        mTitlebar.setLeftText("WebView");

        mTitlebar.setRightText("Scheme");

        setTitle(R.string.home);
        setTitleDrawable(R.drawable.selector_nav_arrow_orange);
    }

    @Override
    protected void onClickLeft() {
        mActivity.forward(WebViewActivity.class);
    }

    @Override
    protected void onClickRight() {
        Intent intent = new Intent();
        Uri uri = new Uri.Builder()
                .scheme("bga")
                .authority("www.bingoogolapple.cn")
                .path("/path1/path2")
                .appendPath("path3")
                .query("param1=param1value&param2=param2value") // 这种方式传递后通过getQueryParameter方法拿不到参数,只能通过getQuery方法获取到参数
                .appendQueryParameter("param3", "param3value")
                .build();
        intent.setData(uri);

        try {
            // 这里try一下,避免ActivityNotFoundException导致应用闪退
            mActivity.forward(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onClickTitle() {
        if (mCategoryPw == null) {
            mCategoryPw = new HomeCategoryPopupWindow(getActivity(), mTitlebar.getTitleCtv());
            mCategoryPw.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    mTitlebar.setTitleCtvChecked(false);
                }
            });
            mCategoryPw.setDelegate(new HomeCategoryPopupWindow.HomeCategoryPopupWindowDelegate() {
                @Override
                public void onSelectCategory(HomeCategory category) {
                    ToastUtil.show("选择了分类：" + category.title);
                }
            });
        }

        if (mHomeCategorys == null) {
            mHomeCategorys = HomeCategory.getTestDatas();
        }
        mCategoryPw.setCategorys(mHomeCategorys);
        mCategoryPw.show();
        mTitlebar.setTitleCtvChecked(true);
    }
}