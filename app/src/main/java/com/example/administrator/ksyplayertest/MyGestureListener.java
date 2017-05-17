package com.example.administrator.ksyplayertest;

import android.app.Activity;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created by Administrator on 2017/5/17.
 */

public class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

    private Activity activity;
    private PlayClickListener listener;

    public MyGestureListener(Activity activity, PlayClickListener listener){
        this.activity = activity;
        this.listener = listener;
    }


    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    /**
     * 因为使用的是自定义的mediaController 当显示后，mediaController会铺满屏幕，
     * 所以VideoView的点击事件会被拦截，所以重写控制器的手势事件，
     * 将全部的操作全部写在控制器中，
     * 因为点击事件被控制器拦截，无法传递到下层的VideoView，
     * 所以 原来的单机隐藏会失效，作为代替，
     * 在手势监听中onSingleTapConfirmed（）添加自定义的隐藏/显示，
     *
     * @param e
     * @return
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        //当手势结束，并且是单击结束时，控制器隐藏/显示
//        toggleMediaControlsVisiblity();
        listener.onHideOrShow();
        return super.onSingleTapConfirmed(e);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    //滑动事件监听
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float mOldX = e1.getX(), mOldY = e1.getY();
        int y = (int) e2.getRawY();
        int x = (int) e2.getRawX();
        Display disp = activity.getWindowManager().getDefaultDisplay();
        int windowWidth = disp.getWidth();
        int windowHeight = disp.getHeight();
        if (mOldX > windowWidth * 3.0 / 4.0) {// 右边滑动 屏幕 3/4
//            onVolumeSlide((mOldY - y) / windowHeight);
        } else if (mOldX < windowWidth * 1.0 / 4.0) {// 左边滑动 屏幕 1/4
//            onBrightnessSlide((mOldY - y) / windowHeight);
        }
        return super.onScroll(e1, e2, distanceX, distanceY);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        listener.onPlayOrPause();
        return true;
    }

}
