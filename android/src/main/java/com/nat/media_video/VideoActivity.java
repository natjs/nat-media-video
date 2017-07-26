package com.nat.media_video;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

public class VideoActivity extends AppCompatActivity {
    private SurfaceView mSurfaceView;
    private MediaPlayer mMediaPlayer;
    String path = "";
    private RelativeLayout mRootView;
    private Controller mController;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_video);


        Intent intent = getIntent();
        if (intent != null) {
            path = intent.getStringExtra("path");
        }

        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mRootView = (RelativeLayout) findViewById(R.id.activity_video);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        SurfaceHolder holder = mSurfaceView.getHolder();

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setDisplay(holder);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setScreenOnWhilePlaying(true);
                mController = new Controller(VideoActivity.this);
                mController.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (mController != null) {
                            if (mController.isShowing()) {
                                mController.hide();
                            } else {
                                mController.show();
                            }
                        }
                        return false;
                    }
                });
                mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                        switch (i1) {
                            case MediaPlayer.MEDIA_ERROR_IO:
                                EventBus.getDefault().post(new MessageEvent(Constant.MEDIA_FILE_TYPE_NOT_SUPPORTED, VideoModule.VIDEO_EORRO));
                                break;
                            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                                EventBus.getDefault().post(new MessageEvent(Constant.MEDIA_DECODE_ERROR, VideoModule.VIDEO_EORRO));
                                break;
                        }
                        VideoActivity.this.finish();
                        return false;
                    }
                });
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        int videoWidth = mMediaPlayer.getVideoWidth();
                        int videoHeight = mMediaPlayer.getVideoHeight();

                        if (videoHeight == 0 || videoWidth == 0) {
                            EventBus.getDefault().post(new MessageEvent(Constant.MEDIA_FILE_TYPE_NOT_SUPPORTED, VideoModule.VIDEO_EORRO));
                            VideoActivity.this.finish();
                        }

                        int screenWidth = Util.getScreenWidth(VideoActivity.this);
                        int screenHeight = Util.getScreenHeight(VideoActivity.this);
                        if (videoHeight <= screenHeight || videoWidth <= screenWidth) {
                            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
                            double heightScale = videoHeight / (screenHeight + 0.0);
                            double widthScale = videoWidth / (screenWidth + 0.0);

                            if (widthScale >= heightScale) {
                                layoutParams.width = screenWidth;
                                layoutParams.height = (int) (videoHeight / widthScale);
                            } else {
                                layoutParams.height = screenHeight;
                                layoutParams.width = (int) (videoWidth / heightScale);
                            }
                            mSurfaceView.setLayoutParams(layoutParams);
                        }

                        mController.setMediaPlayer(new Controller.ControlOper() {
                            @Override
                            public void start() {
                                try {
                                    mMediaPlayer.start();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void pause() {
                                try {
                                    mMediaPlayer.pause();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public int getDuration() {
                                int duration = 0;
                                try {
                                    duration = mMediaPlayer.getDuration();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return duration;
                            }

                            @Override
                            public int getCurPosition() {
                                int position = 0;
                                try {
                                    position = mMediaPlayer.getCurrentPosition();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return position;
                            }

                            @Override
                            public void seekTo(int i) {
                                try {
                                    mMediaPlayer.seekTo(i);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            @Override
                            public boolean isPlaying() {
                                boolean isPlaying = true;
                                try {
                                    isPlaying = mMediaPlayer.isPlaying();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return isPlaying;

                            }

                            @Override
                            public int getBufPercent() {
                                return 0;
                            }

                            @Override
                            public boolean canPause() {
                                return true;

                            }
                            @Override
                            public boolean canSeekBackward() {
                                return true;

                            }
                            @Override
                            public boolean canSeekForward() {
                                return true;

                            }

                            @Override
                            public boolean isFullScreen() {
                                return false;
                            }

                            @Override
                            public void fullScreen() {

                            }
                        });

                        mp.start();

                        mProgressBar.setVisibility(View.GONE);
                        mController.setKeepScreenOn(true);
                        mController.setAnchorView(mRootView);
                        mController.show();
                    }
                });

                start(path);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        mRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mController != null) {
                    if (mController.isShowing()) {
                        mController.hide();
                    } else {
                        mController.show();
                    }
                }
            }
        });
    }

    public void start(String videoUrl) {
        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(videoUrl);
            mMediaPlayer.prepareAsync();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.stop();
                mMediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Constant.VIDEO_PAUSE_OPERATE)) {
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                }
            } else if (action.equals(Constant.VIDEO_STOP_OPERATE)) {
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.VIDEO_PAUSE_OPERATE);
        intentFilter.addAction(Constant.VIDEO_STOP_OPERATE);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

}
