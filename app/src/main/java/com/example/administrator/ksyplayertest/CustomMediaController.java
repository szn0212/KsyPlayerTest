package com.example.administrator.ksyplayertest;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ksyun.media.player.KSYTextureView;

/**
 * Created by Administrator on 2017/5/17.
 */

public class CustomMediaController {

    private Context context;
    private Activity activity;
    private KSYTextureView textureView;
    private int controllerWidth;
    private GestureDetector mGestureDetector;
    private String videoName;

    private View controllerView;
    private ImageButton img_back;//返回按钮
    private TextView mFileName;//文件名
    private ImageButton mShareButton;
    private ImageButton mFavoriteButton;
    private ImageButton mPlayPause;
    private ImageView mIvScale;
    private TextView mTimeCurrent;
    private TextView mTimeTotal;
    private SeekBar mSeekBar;
    private View mVolumeBrightnessLayout;//提示窗口
    private ImageView mOperationBg;//提示图片
    private TextView mOperationTv;//提示文字
    private AudioManager mAudioManager;
    private int mMaxVolume;//最大声音
    private int mVolume = -1;//当前声音
    private float mBrightness = -1f;//当前亮度

    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(activity != null){
                activity.finish();
            }
        }
    };

    private View.OnClickListener scaleListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (activity != null) {
                switch (activity.getResources().getConfiguration().orientation) {
                    case Configuration.ORIENTATION_LANDSCAPE://横屏
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        break;
                    case Configuration.ORIENTATION_PORTRAIT://竖屏
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        break;
                }
            }
        }
    };

    public CustomMediaController(Context context, KSYTextureView textureView, Activity activity, View controllerView){
        this.context = context;
        this.textureView = textureView;
        this.activity = activity;
        this.controllerView = controllerView;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        controllerWidth = wm.getDefaultDisplay().getWidth();

        initControllerView(controllerView);

        mGestureDetector = new GestureDetector(context, new MyGestureListener());
    }


    //设置视频文件名
    public void setVideoName(String name) {
        videoName = name;
        if (mFileName != null) {
            mFileName.setText(name);
        }
    }

    private void initControllerView(View v) {
        v.setMinimumHeight(controllerWidth);
        //获取控件
        img_back = (ImageButton) v.findViewById(context.getResources().getIdentifier("mediacontroller_top_back", "id", context.getPackageName()));
        mFileName = (TextView) v.findViewById(context.getResources().getIdentifier("mediacontroller_filename", "id", context.getPackageName()));
        mShareButton = (ImageButton) v.findViewById(context.getResources().getIdentifier("mediacontroller_share", "id", context.getPackageName()));
        mFavoriteButton = (ImageButton) v.findViewById(context.getResources().getIdentifier("mediacontroller_favorite", "id", context.getPackageName()));
        //缩放控件
        mIvScale = (ImageView) v.findViewById(context.getResources().getIdentifier("mediacontroller_scale", "id", context.getPackageName()));

        mPlayPause = (ImageButton) v.findViewById(R.id.mediacontroller_play_pause);


        if (mFileName != null && videoName != null) {
            mFileName.setText(videoName);
        }
        //声音控制
        mVolumeBrightnessLayout = (RelativeLayout) v.findViewById(R.id.operation_volume_brightness);
        mOperationBg = (ImageView) v.findViewById(R.id.operation_bg);
        mOperationTv = (TextView) v.findViewById(R.id.operation_tv);
        mOperationTv.setVisibility(View.GONE);

        mTimeCurrent = (TextView) v.findViewById(R.id.mediacontroller_time_current);
        mTimeTotal = (TextView) v.findViewById(R.id.mediacontroller_time_total);
        mSeekBar = (SeekBar) v.findViewById(R.id.mediacontroller_seekbar);

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        //注册事件监听
        img_back.setOnClickListener(backListener);
        mIvScale.setOnClickListener(scaleListener);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener{
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
            toggleMediaControlsVisiblity();
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
                onVolumeSlide((mOldY - y) / windowHeight);
            } else if (mOldX < windowWidth * 1.0 / 4.0) {// 左边滑动 屏幕 1/4
                onBrightnessSlide((mOldY - y) / windowHeight);
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            playOrPause();
            return true;
        }

    }

    /**
     * 滑动改变声音大小
     *
     * @param percent
     */
    private void onVolumeSlide(float percent) {
        if (mVolume == -1) {
            mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (mVolume < 0)
                mVolume = 0;

            // 显示
            mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
            mOperationTv.setVisibility(View.VISIBLE);
        }

        int index = (int) (percent * mMaxVolume) + mVolume;
        if (index > mMaxVolume)
            index = mMaxVolume;
        else if (index < 0)
            index = 0;
        if (index >= 10) {
            mOperationBg.setImageResource(R.drawable.volmn_100);
        } else if (index >= 5 && index < 10) {
            mOperationBg.setImageResource(R.drawable.volmn_60);
        } else if (index > 0 && index < 5) {
            mOperationBg.setImageResource(R.drawable.volmn_30);
        } else {
            mOperationBg.setImageResource(R.drawable.volmn_no);
        }
        //DecimalFormat    df   = new DecimalFormat("######0.00");
        mOperationTv.setText((int) (((double) index / mMaxVolume) * 100) + "%");
        // 变更声音
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

    }

    /**
     * 滑动改变亮度
     *
     * @param percent
     */
    private void onBrightnessSlide(float percent) {
        if (mBrightness < 0) {
            mBrightness = activity.getWindow().getAttributes().screenBrightness;
            if (mBrightness <= 0.00f)
                mBrightness = 0.50f;
            if (mBrightness < 0.01f)
                mBrightness = 0.01f;

            // 显示
            mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
            mOperationTv.setVisibility(View.VISIBLE);

        }


        WindowManager.LayoutParams lpa = activity.getWindow().getAttributes();
        lpa.screenBrightness = mBrightness + percent;
        if (lpa.screenBrightness > 1.0f)
            lpa.screenBrightness = 1.0f;
        else if (lpa.screenBrightness < 0.01f)
            lpa.screenBrightness = 0.01f;
        activity.getWindow().setAttributes(lpa);

        mOperationTv.setText((int) (lpa.screenBrightness * 100) + "%");
        if (lpa.screenBrightness * 100 >= 90) {
            mOperationBg.setImageResource(R.drawable.light_100);
        } else if (lpa.screenBrightness * 100 >= 80 && lpa.screenBrightness * 100 < 90) {
            mOperationBg.setImageResource(R.drawable.light_90);
        } else if (lpa.screenBrightness * 100 >= 70 && lpa.screenBrightness * 100 < 80) {
            mOperationBg.setImageResource(R.drawable.light_80);
        } else if (lpa.screenBrightness * 100 >= 60 && lpa.screenBrightness * 100 < 70) {
            mOperationBg.setImageResource(R.drawable.light_70);
        } else if (lpa.screenBrightness * 100 >= 50 && lpa.screenBrightness * 100 < 60) {
            mOperationBg.setImageResource(R.drawable.light_60);
        } else if (lpa.screenBrightness * 100 >= 40 && lpa.screenBrightness * 100 < 50) {
            mOperationBg.setImageResource(R.drawable.light_50);
        } else if (lpa.screenBrightness * 100 >= 30 && lpa.screenBrightness * 100 < 40) {
            mOperationBg.setImageResource(R.drawable.light_40);
        } else if (lpa.screenBrightness * 100 >= 20 && lpa.screenBrightness * 100 < 20) {
            mOperationBg.setImageResource(R.drawable.light_30);
        } else if (lpa.screenBrightness * 100 >= 10 && lpa.screenBrightness * 100 < 20) {
            mOperationBg.setImageResource(R.drawable.light_20);
        }

    }


    /**
     * 隐藏或显示
     */
    private void toggleMediaControlsVisiblity() {
        if (controllerView.getVisibility() == View.VISIBLE) {
            controllerView.setVisibility(View.GONE);
        } else {
            controllerView.setVisibility(View.VISIBLE);
        }
    }


    /**
     * 播放/暂停
     */
    private void playOrPause() {
        if (textureView != null)
            if (textureView.isPlaying()) {
                textureView.pause();
            } else {
                textureView.start();
            }
    }
}
