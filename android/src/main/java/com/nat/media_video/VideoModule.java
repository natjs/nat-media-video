package com.nat.media_video;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Created by xuqinchao on 17/1/7.
 *  Copyright (c) 2017 Instapp. All rights reserved.
 */

public class VideoModule {

    public static final String VIDEO_EORRO = "video_eorro";
    ModuleResultListener mListener;
    String mVideoActivityName = VideoActivity.class.getName();

    private Context mContext;
    private static volatile VideoModule instance = null;

    private VideoModule(Context context){
        mContext = context;
        EventBus.getDefault().register(this);
    }

    public static VideoModule getInstance(Context context) {
        if (instance == null) {
            synchronized (VideoModule.class) {
                if (instance == null) {
                    instance = new VideoModule(context);
                }
            }
        }

        return instance;
    }

    public void play(String path, ModuleResultListener ModuleResultListener) {
        boolean vilaid = path.startsWith("http://") || path.startsWith("https://") || path.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath());

        if (path.equals("") || path == null || !vilaid) {
            ModuleResultListener.onResult(Util.getError(Constant.MEDIA_SRC_NOT_SUPPORTED, 1));
            return;
        }
        mListener = ModuleResultListener;
        Intent intent = new Intent(mContext, VideoActivity.class);
        intent.putExtra("path", path);
        mContext.startActivity(intent);
        ModuleResultListener.onResult(null);
    }

    public void pause(ModuleResultListener ModuleResultListener) {
        if (!Util.getCurrentActivity(mContext).equals(mVideoActivityName)) {
            ModuleResultListener.onResult(Util.getError(Constant.MEDIA_PLAYER_NOT_STARTED, Constant.MEDIA_PLAYER_NOT_STARTED_CODE));
        } else {
            Intent intent = new Intent();
            intent.setAction(Constant.VIDEO_PAUSE_OPERATE);
            mContext.sendBroadcast(intent);
            ModuleResultListener.onResult(null);
        }
    }

    public void stop(ModuleResultListener ModuleResultListener) {
        if (!Util.getCurrentActivity(mContext).equals(mVideoActivityName)) {
            ModuleResultListener.onResult(Util.getError(Constant.MEDIA_PLAYER_NOT_STARTED, Constant.MEDIA_PLAYER_NOT_STARTED_CODE));
        } else {
            Intent intent = new Intent();
            intent.setAction(Constant.VIDEO_STOP_OPERATE);
            mContext.sendBroadcast(intent);
            ModuleResultListener.onResult(null);
        }
    }

    @Subscribe
    public void onMessageEvent(MessageEvent messageEvent) {
        if (messageEvent.mType.equals(VIDEO_EORRO)) {
            switch (messageEvent.mMsg) {
                case Constant.MEDIA_FILE_TYPE_NOT_SUPPORTED:
                    mListener.onResult(Util.getError(Constant.MEDIA_FILE_TYPE_NOT_SUPPORTED, Constant.MEDIA_FILE_TYPE_NOT_SUPPORTED_CODE));
                    break;
                case Constant.MEDIA_DECODE_ERROR:
                    mListener.onResult(Util.getError(Constant.MEDIA_DECODE_ERROR, Constant.MEDIA_DECODE_ERROR_CODE));
                    break;
            }
        }
    }
}
