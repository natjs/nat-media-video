package com.nat.media_video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

/**
 * Created by xuqinchao on 17/1/20.
 */

public class Controller extends RelativeLayout{
        private static final String LOG_TAG = Controller.class.getName();

        private static final int    FADE_OUT = 1;
        private static final int    DEFTIMEOUT = 3000;
        private static final int    SHOW_PROGRESS = 2;

        private ImageButton     mBtnPause;
        private ImageButton     mBtnFfwd;
        private ImageButton     mBtnRew;
        private ImageButton     mBtnNext;
        private ImageButton     mBtnPrev;
        private ImageButton     mBtnFullscreen;
        private Handler mHandler = new MsgHandler(this);

        private ControlOper     mPlayerCtrl;
        private Context         mContext;
        private RelativeLayout  mAnchorVGroup;
        private View            mRootView;
        private SeekBar         mProgress;
        private TextView        mEndTime, mCurTime;
        private boolean         mIsShowing;
        private boolean         mIsDragging;
        private boolean         mUseFastForward;
        private boolean         mFromXml;
        private boolean         mIsListenersSet;
        private OnClickListener mNextListener, mPrevListener;
        StringBuilder           mStrBuilder;
        Formatter               mFormatter;

        public Controller(Context context) {
            this(context, true);
        }

        public Controller(Context context, AttributeSet attrs) {
            super(context, attrs);

            mRootView = null;
            mContext = context;
            mUseFastForward = true;
            mFromXml = true;
        }

        public Controller(Context context, boolean useFastForward) {
            super(context);
            mContext = context;
            mUseFastForward = useFastForward;
        }

        public void removeHandlerCallback() {
            if(mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler = null;
            }
        }

        @Override
        public void onFinishInflate() {
            if (mRootView != null) {
                initCtrlView(mRootView);
            }
        }

        public void setMediaPlayer(ControlOper player) {
            mPlayerCtrl = player;
            updatePausePlay();
            updateFullScreen();
        }

        public void setAnchorView(RelativeLayout view) {

            mAnchorVGroup = view;
            LayoutParams frameParams = new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    (int) Util.dp2px(mContext, 100)
            );
            frameParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            removeAllViews();

            View v = createCtrlView();
            addView(v, frameParams);
        }

        protected View createCtrlView() {

            LayoutInflater inflate = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            mRootView = inflate.inflate(R.layout.controller, null);
            initCtrlView(mRootView);

            return mRootView;
        }

        private void initCtrlView(View v) {

            mBtnPause = (ImageButton) v.findViewById(R.id.pause);
            if (mBtnPause != null) {
                mBtnPause.requestFocus();
                mBtnPause.setOnClickListener(mPauseListener);
            }

            mBtnFullscreen = (ImageButton) v.findViewById(R.id.fullscreen);
            if (mBtnFullscreen != null) {
                mBtnFullscreen.requestFocus();
                mBtnFullscreen.setOnClickListener(mFullscreenListener);
            }

            mBtnFfwd = (ImageButton) v.findViewById(R.id.ffwd);
            if (mBtnFfwd != null) {
                mBtnFfwd.setOnClickListener(mFfwdListener);
                if (!mFromXml) {
                    mBtnFfwd.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
                }
            }

            mBtnRew = (ImageButton) v.findViewById(R.id.rew);
            if (mBtnRew != null) {
                mBtnRew.setOnClickListener(mRewListener);
                if (!mFromXml) {
                    mBtnRew.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
                }
            }

            // By default these are hidden. They will be enabled when setPrevNextListeners() is called
            mBtnNext = (ImageButton) v.findViewById(R.id.next);
            if (mBtnNext != null && !mFromXml && !mIsListenersSet) {
                mBtnNext.setVisibility(View.GONE);
            }

            mBtnPrev = (ImageButton) v.findViewById(R.id.prev);
            if (mBtnPrev != null && !mFromXml && !mIsListenersSet) {
                mBtnPrev.setVisibility(View.GONE);
            }

            mProgress = (SeekBar) v.findViewById(R.id.mediacontroller_progress);
            if (mProgress != null) {
                mProgress.setOnSeekBarChangeListener(mSeekListener);
                mProgress.setMax(1000);
            }

            mEndTime = (TextView) v.findViewById(R.id.time);
            mCurTime = (TextView) v.findViewById(R.id.time_current);
            mStrBuilder = new StringBuilder();
            mFormatter = new Formatter(mStrBuilder, Locale.getDefault());

            installPrevNextListeners();
        }

        /**
         * Show the controller on screen. It will go away automatically after
         * 3 seconds of inactivity.
         */
        public void show() {
            show(DEFTIMEOUT);
        }

        /**
         * Disable pause or seek buttons if the stream cannot be paused or seeked.
         * This requires the control interface to be a MediaPlayerControlExt
         */
        private void disableUnsupportedButtons() {
            if (mPlayerCtrl == null) {
                return;
            }

            try {
                if (mBtnPause != null && !mPlayerCtrl.canPause()) {
                    mBtnPause.setEnabled(false);
                }
                if (mBtnRew != null && !mPlayerCtrl.canSeekBackward()) {
                    mBtnRew.setEnabled(false);
                }
                if (mBtnFfwd != null && !mPlayerCtrl.canSeekForward()) {
                    mBtnFfwd.setEnabled(false);
                }
            } catch (IncompatibleClassChangeError ex) {

            }
        }

        public void show(int timeout) {
            if (!mIsShowing && mAnchorVGroup != null) {
                setProgress();
                if (mBtnPause != null) {
                    mBtnPause.requestFocus();
                }
                disableUnsupportedButtons();

//                FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        Gravity.BOTTOM
//                );
                LayoutParams frameParams = new LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        (int) Util.dp2px(mContext, 100)
                );
                frameParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                mAnchorVGroup.addView(this, frameParams);
                mIsShowing = true;
            }
            updatePausePlay();
            updateFullScreen();

            mHandler.sendEmptyMessage(SHOW_PROGRESS);

            Message msg = mHandler.obtainMessage(FADE_OUT);
            if (timeout != 0) {
                mHandler.removeMessages(FADE_OUT);
                mHandler.sendMessageDelayed(msg, timeout);
            }
        }

        public boolean isShowing() {
            return mIsShowing;
        }

        /**
         * Remove the controller from the screen.
         */
        public void hide() {
            if (mAnchorVGroup == null) {
                return;
            }

            try {
                mAnchorVGroup.removeView(this);
                if(mHandler != null) {
                    mHandler.removeMessages(SHOW_PROGRESS);
                }
            } catch (IllegalArgumentException ex) {
                Log.w("MediaController", "already removed");
            }
            mIsShowing = false;
        }

        private String stringForTime(int timeMs) {
            int totalSeconds = timeMs / 1000;

            int seconds = totalSeconds % 60;
            int minutes = (totalSeconds / 60) % 60;
            int hours   = totalSeconds / 3600;

            mStrBuilder.setLength(0);
            if (hours > 0) {
                return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
            } else {
                return mFormatter.format("%02d:%02d", minutes, seconds).toString();
            }
        }

        private int setProgress() {
            if (mPlayerCtrl == null || mIsDragging) {
                return 0;
            }

            int position = mPlayerCtrl.getCurPosition();
            int duration = mPlayerCtrl.getDuration();
            if (mProgress != null) {
                if (duration > 0) {
                    // use long to avoid overflow
                    long pos = 1000L * position / duration;
                    mProgress.setProgress( (int) pos);
                }
                int percent = mPlayerCtrl.getBufPercent();
                mProgress.setSecondaryProgress(percent * 10);
            }

            if (mEndTime != null)
                mEndTime.setText(stringForTime(duration));
            if (mCurTime != null)
                mCurTime.setText(stringForTime(position));

            return position;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            show(DEFTIMEOUT);
            return true;
        }

        @Override
        public boolean onTrackballEvent(MotionEvent ev) {
            show(DEFTIMEOUT);
            return false;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (mPlayerCtrl == null) {
                return true;
            }

            int keyCode = event.getKeyCode();
            final boolean uniqueDown = event.getRepeatCount() == 0
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (keyCode ==  KeyEvent.KEYCODE_HEADSETHOOK
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    || keyCode == KeyEvent.KEYCODE_SPACE) {
                if (uniqueDown) {
                    doPauseResume();
                    show(DEFTIMEOUT);
                    if (mBtnPause != null) {
                        mBtnPause.requestFocus();
                    }
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (uniqueDown && !mPlayerCtrl.isPlaying()) {
                    mPlayerCtrl.start();
                    updatePausePlay();
                    show(DEFTIMEOUT);
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (uniqueDown && mPlayerCtrl.isPlaying()) {
                    mPlayerCtrl.pause();
                    updatePausePlay();
                    show(DEFTIMEOUT);
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                    || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                    || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
                // don't show the controls for volume adjustment
                return super.dispatchKeyEvent(event);
            } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
                if (uniqueDown) {
                    hide();
                }
                return true;
            }

            show(DEFTIMEOUT);
            return super.dispatchKeyEvent(event);
        }

        private OnClickListener mPauseListener = new OnClickListener() {
            public void onClick(View v) {
                doPauseResume();
                show(DEFTIMEOUT);
            }
        };

        private OnClickListener mFullscreenListener = new OnClickListener() {
            public void onClick(View v) {
                doToggleFullscreen();
                show(DEFTIMEOUT);
            }
        };

        public void updatePausePlay() {
            if (mRootView == null || mBtnPause == null || mPlayerCtrl == null) {
                return;
            }

            if (mPlayerCtrl.isPlaying()) {
                mBtnPause.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                mBtnPause.setImageResource(android.R.drawable.ic_media_play);
            }
        }

        public void updateFullScreen() {
            if (mRootView == null || mBtnFullscreen == null || mPlayerCtrl == null) {
                return;
            }

            if (mPlayerCtrl.isFullScreen()) {
                System.out.println("MediaController fullScreen" + "true");
            }
            else {
                System.out.println("MediaController fullScreen" + "false");
            }
        }

        private void doPauseResume() {
            if (mPlayerCtrl == null) {
                return;
            }

            if (mPlayerCtrl.isPlaying()) {
                mPlayerCtrl.pause();
            } else {
                mPlayerCtrl.start();
            }
            updatePausePlay();
        }

        private void doToggleFullscreen() {
            if (mPlayerCtrl == null) {
                return;
            }
            Log.i(LOG_TAG, "doToggleFullscreen");
            mPlayerCtrl.fullScreen();
        }

        private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar bar) {
                show(3600000);

                mIsDragging = true;
                mHandler.removeMessages(SHOW_PROGRESS);
            }

            public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
                if (mPlayerCtrl == null) {
                    return;
                }

                if (!fromuser) {
                    // We're not interested in programmatically generated changes to
                    // the progress bar's position.
                    return;
                }

                long duration = mPlayerCtrl.getDuration();
                long newposition = (duration * progress) / 1000L;
                mPlayerCtrl.seekTo( (int) newposition);
                if (mCurTime != null)
                    mCurTime.setText(stringForTime( (int) newposition));
            }

            public void onStopTrackingTouch(SeekBar bar) {
                mIsDragging = false;
                setProgress();
                updatePausePlay();
                show(DEFTIMEOUT);

                mHandler.sendEmptyMessage(SHOW_PROGRESS);
            }
        };

        @Override
        public void setEnabled(boolean enabled) {
            if (mBtnPause != null) {
                mBtnPause.setEnabled(enabled);
            }
            if (mBtnFfwd != null) {
                mBtnFfwd.setEnabled(enabled);
            }
            if (mBtnRew != null) {
                mBtnRew.setEnabled(enabled);
            }
            if (mBtnNext != null) {
                mBtnNext.setEnabled(enabled && mNextListener != null);
            }
            if (mBtnPrev != null) {
                mBtnPrev.setEnabled(enabled && mPrevListener != null);
            }
            if (mProgress != null) {
                mProgress.setEnabled(enabled);
            }
            disableUnsupportedButtons();
            super.setEnabled(enabled);
        }

        @SuppressLint("NewApi")
        @Override
        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            event.setClassName(Controller.class.getName());
        }

        @SuppressLint("NewApi")
        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName(Controller.class.getName());
        }

        private OnClickListener mRewListener = new OnClickListener() {
            public void onClick(View v) {
                if (mPlayerCtrl == null) {
                    return;
                }

                int pos = mPlayerCtrl.getCurPosition();
                pos -= 5000; // milliseconds
                mPlayerCtrl.seekTo(pos);
                setProgress();

                show(DEFTIMEOUT);
            }
        };

        private OnClickListener mFfwdListener = new OnClickListener() {
            public void onClick(View v) {
                if (mPlayerCtrl == null) {
                    return;
                }

                int pos = mPlayerCtrl.getCurPosition();
                pos += 15000; // milliseconds
                mPlayerCtrl.seekTo(pos);
                setProgress();

                show(DEFTIMEOUT);
            }
        };

        private void installPrevNextListeners() {
            if (mBtnNext != null) {
                mBtnNext.setOnClickListener(mNextListener);
                mBtnNext.setEnabled(mNextListener != null);
            }

            if (mBtnPrev != null) {
                mBtnPrev.setOnClickListener(mPrevListener);
                mBtnPrev.setEnabled(mPrevListener != null);
            }
        }

        public void setPrevNextListeners(OnClickListener next, OnClickListener prev) {
            mNextListener = next;
            mPrevListener = prev;
            mIsListenersSet = true;

            if (mRootView != null) {
                installPrevNextListeners();

                if (mBtnNext != null && !mFromXml) {
                    mBtnNext.setVisibility(View.VISIBLE);
                }
                if (mBtnPrev != null && !mFromXml) {
                    mBtnPrev.setVisibility(View.VISIBLE);
                }
            }
        }

        public interface ControlOper {
            void    start();
            void    pause();
            int     getDuration();
            int     getCurPosition();
            void    seekTo(int pos);
            boolean isPlaying();
            int     getBufPercent();
            boolean canPause();
            boolean canSeekBackward();
            boolean canSeekForward();
            boolean isFullScreen();
            void    fullScreen();
        }

        private static class MsgHandler extends Handler {

            private final WeakReference<Controller> mView;

            MsgHandler(Controller view) {
                mView = new WeakReference<Controller>(view);
            }

            @Override
            public void handleMessage(Message msg) {

                Controller view = mView.get();
                if (view == null || view.mPlayerCtrl == null) {
                    return;
                }

                int pos;
                switch (msg.what) {

                    case FADE_OUT: {
                        view.hide();
                        break;
                    }

                    case SHOW_PROGRESS: {

                        if(view.mPlayerCtrl.isPlaying()) {
                            pos = view.setProgress();
                        } else {
                            return;
                        }

                        if (!view.mIsDragging && view.mIsShowing && view.mPlayerCtrl.isPlaying()) {
                            msg = obtainMessage(SHOW_PROGRESS);
                            sendMessageDelayed(msg, 1000 - (pos % 1000));
                        }
                        break;
                    }
                }
            }
        }
}
