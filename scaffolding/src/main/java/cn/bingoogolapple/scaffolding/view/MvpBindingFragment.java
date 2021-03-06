package cn.bingoogolapple.scaffolding.view;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cn.bingoogolapple.scaffolding.presenter.BasePresenter;

/**
 * 作者:王浩 邮件:bingoogolapple@gmail.com
 * 创建时间:15/9/2 下午10:57
 * 描述:
 */
public abstract class MvpBindingFragment<B extends ViewDataBinding, P extends BasePresenter> extends MvpFragment<P> {
    protected B mBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 避免多次从xml中加载布局文件
        if (mBinding == null) {
            mBinding = DataBindingUtil.inflate(inflater, getRootLayoutResID(), container, false);
            initView(savedInstanceState);
            setListener();
            processLogic(savedInstanceState);
        } else {
            ViewGroup parent = (ViewGroup) mBinding.getRoot().getParent();
            if (parent != null) {
                parent.removeView(mBinding.getRoot());
            }
        }
        return mBinding.getRoot();
    }
}