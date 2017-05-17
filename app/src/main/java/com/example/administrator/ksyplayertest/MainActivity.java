package com.example.administrator.ksyplayertest;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaMeta;
import com.ksyun.media.player.KSYMediaPlayer;
import com.ksyun.media.player.KSYTextureView;
import com.ksyun.media.player.misc.KSYQosInfo;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements PlayClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
//    private static final String path = "http://playback.ks.zb.mi.com/record/live/107578_1467605748/hls/107578_1467605748.m3u8";
    private static final String path = "http://baobab.wdjcdn.com/145076769089714.mp4";
    public static final int UPDATE_SEEKBAR = 0;
    public static final int HIDDEN_SEEKBAR = 1;

    private Context mContext;
    private KSYTextureView mTextureView;
    private ProgressBar mProgressBar;
    private TextView download_rate;
    private TextView load_rate;
    private CustomMediaController mCustomMediaController;
    private View mediaControllerView;
    private ImageButton backBtn;
    private TextView fileName;
    private ImageButton playPauseBtn;
    private ImageView scaleBtn;
    private TextView totalTime;
    private SeekBar seekBar;
    private GestureDetector mGestureDetector;
    private boolean mPause = false;
    private long mStartTime = 0;
    private long mPauseStartTime = 0;
    private long mPausedTime = 0;

    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    private View.OnClickListener playPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onPlayOrPause();
        }
    };

    private View.OnClickListener scaleListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mHandler.removeMessages(HIDDEN_SEEKBAR);
            Message msg = new Message();
            msg.what = HIDDEN_SEEKBAR;
            mHandler.sendMessageDelayed(msg, 3000);
            if(mTextureView != null) {
                switch (getResources().getConfiguration().orientation) {
                    case Configuration.ORIENTATION_LANDSCAPE://横屏
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        mTextureView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                        break;
                    case Configuration.ORIENTATION_PORTRAIT://竖屏
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        mTextureView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_NOSCALE_TO_FIT);
                        break;
                }
            }
        }
    };

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case UPDATE_SEEKBAR:
                    setSeekBarProgress(0);
                    break;
                case HIDDEN_SEEKBAR:
//                    onHideOrShow();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //定义全屏参数
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        //获得当前窗体对象
        Window window = MainActivity.this.getWindow();
        //设置当前窗体为全屏显示
        window.setFlags(flag, flag);

        setContentView(R.layout.activity_main);
        mContext = MainActivity.this;

//        KSYMediaPlayer ksyMediaPlayer = new KSYMediaPlayer.Builder(this.getApplicationContext()).build();



        mTextureView = (KSYTextureView) findViewById(R.id.textureView);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        download_rate = (TextView) findViewById(R.id.download_rate);
        load_rate = (TextView) findViewById(R.id.load_rate);

        mediaControllerView = findViewById(R.id.media_control_view);
        backBtn = (ImageButton) findViewById(R.id.mediacontroller_top_back);
        fileName = (TextView) findViewById(R.id.mediacontroller_filename);
        playPauseBtn = (ImageButton) findViewById(R.id.mediacontroller_play_pause);
        scaleBtn = (ImageView) findViewById(R.id.mediacontroller_scale);
        totalTime = (TextView) findViewById(R.id.mediacontroller_time_total);
        seekBar = (SeekBar) findViewById(R.id.mediacontroller_seekbar);
        mProgressBar.setVisibility(View.GONE);
        download_rate.setVisibility(View.GONE);
        load_rate.setVisibility(View.GONE);

        mGestureDetector = new GestureDetector(this, new MyGestureListener(this, this));
        backBtn.setOnClickListener(backListener);
        fileName.setText("白火锅 x 红火锅");
        playPauseBtn.setOnClickListener(playPauseListener);
        scaleBtn.setOnClickListener(scaleListener);
//        seekBar.setVisibility(View.GONE);
        seekBar.setOnSeekBarChangeListener(mSeekBarChangeListenr);
        seekBar.setEnabled(true);
        seekBar.bringToFront();

//        customMediaController = new CustomMediaController(this, mTextureView, this, mediaControllerView);
//        customMediaController.setVideoName("白火锅 x 红火锅");

        setListener();

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setPrepareSetting();

        try {
            mTextureView.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mTextureView.prepareAsync();
    }

    private int mVideoProgress = 0;
    private int setSeekBarProgress(int currentProgress){
        if(mTextureView == null){
            return -1;
        }
        long time = currentProgress > 0 ? currentProgress : mTextureView.getCurrentPosition();
        long duration = mTextureView.getDuration();

        seekBar.setMax((int) duration);
        seekBar.setProgress((int) time);

        if(time > 0){
            String progress = Util.millisToString(time) + "/" + Util.millisToString(duration);
            totalTime.setText(progress);
        }

        Message message = new Message();
        message.what = UPDATE_SEEKBAR;
        if(mHandler != null){
            mHandler.sendMessageDelayed(message, 1000);
        }
        return (int) time;
    }

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListenr = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser){
                mVideoProgress = progress;
                mHandler.removeMessages(HIDDEN_SEEKBAR);
                Message msg = new Message();
                msg.what = HIDDEN_SEEKBAR;
                mHandler.sendMessageDelayed(msg, 3000);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mTextureView.seekTo(mVideoProgress);
            setSeekBarProgress(mVideoProgress);
        }
    };


    private void setPrepareSetting() {
        //播放器可缓存时长的最大值,单位:秒, 默认值:2s  只对直播有效
        mTextureView.setBufferTimeMax(5.0f);
        //播放器缓存数据可占用内存的最大值，单位:MB, 默认值:15MB  设置范围为0～100MB，设置为0时不缓冲
        mTextureView.setBufferSize(15);
        /**
         * @param prepareTimeout 网络链接超时阈值，单位为秒，默认值为10s
         * @param readTimeout 读取数据超时阈值，单位为秒，默认值30s
         */
        mTextureView.setTimeout(5,30);
        //如果硬解码的话加上
//        mTextureView.setDecodeMode(KSYMediaPlayer.KSYDecodeMode.KSY_DECODE_MODE_AUTO);
    }

    private void setListener() {
//        mTextureView.setOnTouchListener(mTouchListener);
        mTextureView.setKeepScreenOn(true);
        mTextureView.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        mTextureView.setOnCompletionListener(mOnCompletionListener);
        mTextureView.setOnPreparedListener(mOnPreparedListener);
        mTextureView.setOnInfoListener(mOnInfoListener);
        mTextureView.setOnVideoSizeChangedListener(mOnVideoSizeChangeListener);
        mTextureView.setOnErrorListener(mOnErrorListener);
        mTextureView.setOnSeekCompleteListener(mOnSeekCompletedListener);
        mTextureView.setOnMessageListener(mOnMessageListener);
        mTextureView.setVideoRawDataListener(mOnVideoRawDataListener);
        mTextureView.setScreenOnWhilePlaying(true);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if(mTextureView != null){
            switch (newConfig.orientation){
                case Configuration.ORIENTATION_LANDSCAPE://横屏
                    mTextureView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_NOSCALE_TO_FIT);
//                    mTextureView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    break;
                case Configuration.ORIENTATION_PORTRAIT://竖屏
                    mTextureView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                    break;
            }
        }
        super.onConfigurationChanged(newConfig);
    }

    private KSYMediaPlayer.OnVideoRawDataListener mOnVideoRawDataListener = new KSYMediaPlayer.OnVideoRawDataListener() {
        @Override
        public void onVideoRawDataAvailable(IMediaPlayer iMediaPlayer, byte[] bytes, int i, int i1, int i2, int i3, long l) {
            Log.d(TAG, "video raw data-----byte length" + bytes.length);
            Log.d(TAG, "video raw data-----i" + i);
            Log.d(TAG, "video raw data-----i1" + i1);
            Log.d(TAG, "video raw data-----i2" + i2);
            Log.d(TAG, "video raw data-----i3" + i3);
            Log.d(TAG, "video raw data-----l" + l);
        }
    };

    private IMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int percent) {
            //更改进度条
            if(mTextureView != null){
                long duration = mTextureView.getDuration();
                long progress = duration * percent / 100;
                //...更改
//                String totalTimes = Util.millisToString(duration);
//                totalTime.setText(totalTimes);

                seekBar.setSecondaryProgress((int) progress);
                //percent是视频播放进度, progress是播放时长；
            }

        }
    };

    private IMediaPlayer.OnCompletionListener mOnCompletionListener = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer iMediaPlayer) {
            //播放器缓存完成
            Toast.makeText(MainActivity.this, "OnCompletionListener, play complete.", Toast.LENGTH_LONG).show();
            videoPlayEnd();
        }
    };

    private void videoPlayEnd() {
        if(mTextureView != null){
            mTextureView.stop();
            mTextureView.release();
            mTextureView = null;
        }
        //停止线程、handler

        finish();
    }

    private int mVideoWidth;
    private int mVideoHeight;
    private IMediaPlayer.OnPreparedListener mOnPreparedListener = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer iMediaPlayer) {
            mVideoWidth = mTextureView.getVideoWidth();
            mVideoHeight = mTextureView.getVideoHeight();

//            mTextureView.setVideoScaleRatio(0.5f, mVideoWidth/2, mVideoHeight/2);
            mTextureView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            mTextureView.start();

            mStartTime = System.currentTimeMillis();
            setSeekBarProgress(0);

            //线程开启

            //获取HTTP请求、DNS连接时间等信息
//            Bundle bundle = mTextureView.getMediaMeta();
//            KSYMediaMeta meta = KSYMediaMeta.parse(bundle);
//            if(meta != null){
//
//            }

        }
    };

    private long bits;
    private KSYQosInfo info;
    private IMediaPlayer.OnInfoListener mOnInfoListener = new IMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer mp, int what, int extra) {
            switch (what) {
                case KSYMediaPlayer.MEDIA_INFO_BUFFERING_START:
                    Log.d(TAG, "开始缓冲数据, 卡顿开始");
                    if(mTextureView.isPlaying()){
                        mPause = true;
                        mTextureView.pause();
                        mProgressBar.setVisibility(View.VISIBLE);
                        download_rate.setText("");
                        load_rate.setText("");
                        download_rate.setVisibility(View.VISIBLE);
                        download_rate.setVisibility(View.VISIBLE);
                    }
                    if (mTextureView != null) {
                        bits = mTextureView.getDecodedDataSize() * 8 / (mPause ? mPauseStartTime - mPausedTime - mStartTime : System.currentTimeMillis() - mPausedTime - mStartTime);
                        info = mTextureView.getStreamQosInfo();
                        download_rate.setText(bits + "kb/s");
//                        load_rate.setText();
                        Log.d(TAG, "开始缓冲数据, 卡顿开始" + info.videoBufferTimeLength + "------------" + info.videoTotalDataSize);
                    }
                    break;
                case KSYMediaPlayer.MEDIA_INFO_BUFFERING_END:
                    Log.d(TAG, "数据缓冲完毕，卡顿结束");
                    mPause = false;
                    mTextureView.start();
                    mProgressBar.setVisibility(View.GONE);
                    download_rate.setVisibility(View.GONE);
                    load_rate.setVisibility(View.GONE);
                    Log.d(TAG, "数据缓冲完毕，卡顿结束" + info.videoBufferTimeLength + "------------" + info.videoTotalDataSize);
                    break;
                case KSYMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                    Toast.makeText(mContext, "Audio Rendering Start", Toast.LENGTH_SHORT).show();
                    break;
                case KSYMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                    Toast.makeText(mContext, "Video Rendering Start", Toast.LENGTH_SHORT).show();
                    break;
                case KSYMediaPlayer.MEDIA_INFO_RELOADED:
                    Toast.makeText(mContext, "Succeed to reload video.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Succeed to reload video.");
                    return false;
            }
            return false;
        }
    };

    private IMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangeListener = new IMediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
            if(mVideoWidth > 0 && mVideoHeight > 0){
                if(width != mVideoWidth || height != mVideoHeight){
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();
                    if(mTextureView != null){
                        mTextureView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    }
                }
            }
        }
    };

    private IMediaPlayer.OnErrorListener mOnErrorListener = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            switch (what) {
                //case KSYVideoView.MEDIA_ERROR_UNKNOWN:
                // Log.e(TAG, "OnErrorListener, Error Unknown:" + what + ",extra:" + extra);
                //  break;
                default:
                    Log.e(TAG, "OnErrorListener, Error:" + what + ",extra:" + extra);
            }

            videoPlayEnd();

            return false;
        }
    };

    private IMediaPlayer.OnSeekCompleteListener mOnSeekCompletedListener = new IMediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(IMediaPlayer iMediaPlayer) {
            Log.e(TAG, "onSeekComplete...............");
        }
    };

    private IMediaPlayer.OnMessageListener mOnMessageListener = new IMediaPlayer.OnMessageListener() {
        @Override
        public void onMessage(IMediaPlayer iMediaPlayer, String name, String info, double number) {
            Log.e(TAG, "name:" + name + ",info:" + info + ",number:" + number);
        }
    };


    @Override
    protected void onPause() {
        super.onPause();
        if (mTextureView != null) {
            mTextureView.runInBackground(true);
            mTextureView.stop();
            mPause = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mTextureView != null){
            mTextureView.runInForeground();
            mTextureView.start();
            mPause = false;
        }
    }

    @Override
    public void onPlayOrPause() {
        if(mTextureView.isPlaying()){
            mPause = true;
            mTextureView.pause();
            playPauseBtn.setImageResource(R.drawable.ic_player_pause);
            mPauseStartTime = System.currentTimeMillis();
        }else {
            mPause = false;
            mTextureView.start();
            playPauseBtn.setImageResource(R.drawable.ic_player_play);
            mPausedTime += System.currentTimeMillis() - mPauseStartTime;
            mPauseStartTime = 0;
        }
    }

    @Override
    public void onHideOrShow() {
        if(mediaControllerView.getVisibility() == View.VISIBLE){
            mediaControllerView.setVisibility(View.GONE);
        }else {
            mediaControllerView.setVisibility(View.VISIBLE);
        }
    }
}
