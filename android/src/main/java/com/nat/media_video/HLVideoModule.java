package com.nat.media_video;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Created by xuqinchao on 17/1/7.
 *  Copyright (c) 2017 Nat. All rights reserved.
 */

public class HLVideoModule {

    public static final String VIDEO_EORRO = "video_eorro";
    HLModuleResultListener mListener;
    String mVideoActivityName = HLVideoActivity.class.getName();

    private Context mContext;
    private static volatile HLVideoModule instance = null;

    private HLVideoModule(Context context){
        mContext = context;
    }

    public static HLVideoModule getInstance(Context context) {
        if (instance == null) {
            synchronized (HLVideoModule.class) {
                if (instance == null) {
                    instance = new HLVideoModule(context);
                }
            }
        }

        return instance;
    }

    public void play(String path, HLModuleResultListener HLModuleResultListener) {

        boolean vilaid = path.startsWith("http://") || path.startsWith("https://") || path.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath());

        if (path.equals("") || path == null || !vilaid) {
            HLModuleResultListener.onResult(HLUtil.getError(HLConstant.MEDIA_SRC_NOT_SUPPORTED, 1));
            return;
        }
        mListener = HLModuleResultListener;
        Intent intent = new Intent(mContext, HLVideoActivity.class);
        intent.putExtra("path", path);
        mContext.startActivity(intent);
        HLModuleResultListener.onResult(null);
        EventBus.getDefault().register(this);
    }

    public void pause(HLModuleResultListener HLModuleResultListener) {
        if (!HLUtil.getCurrentActivity(mContext).equals(mVideoActivityName)) {
            HLModuleResultListener.onResult(HLUtil.getError(HLConstant.MEDIA_PLAYER_NOT_STARTED, HLConstant.MEDIA_PLAYER_NOT_STARTED_CODE));
        } else {
            Intent intent = new Intent();
            intent.setAction(HLConstant.VIDEO_PAUSE_OPERATE);
            mContext.sendBroadcast(intent);
            HLModuleResultListener.onResult(null);
        }
    }

    public void stop(HLModuleResultListener HLModuleResultListener) {
        if (!HLUtil.getCurrentActivity(mContext).equals(mVideoActivityName)) {
            HLModuleResultListener.onResult(HLUtil.getError(HLConstant.MEDIA_PLAYER_NOT_STARTED, HLConstant.MEDIA_PLAYER_NOT_STARTED_CODE));
        } else {
            Intent intent = new Intent();
            intent.setAction(HLConstant.VIDEO_STOP_OPERATE);
            mContext.sendBroadcast(intent);
            HLModuleResultListener.onResult(null);
        }
    }

    @Subscribe
    public void onMessageEvent(MessageEvent messageEvent) {
        if (messageEvent.mType.equals(VIDEO_EORRO)) {
            switch (messageEvent.mMsg) {
                case HLConstant.MEDIA_FILE_TYPE_NOT_SUPPORTED:
                    mListener.onResult(HLUtil.getError(HLConstant.MEDIA_FILE_TYPE_NOT_SUPPORTED, HLConstant.MEDIA_FILE_TYPE_NOT_SUPPORTED_CODE));
                    break;
                case HLConstant.MEDIA_DECODE_ERROR:
                    mListener.onResult(HLUtil.getError(HLConstant.MEDIA_DECODE_ERROR, HLConstant.MEDIA_DECODE_ERROR_CODE));
                    break;
            }
        }
    }
}
