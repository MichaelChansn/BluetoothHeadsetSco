package com.headset.bluetooth.headset;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.headset.bluetooth.listener.HeadsetStateChangeListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

/**
 * <p>
 *
 * @author kangsen
 * @version v1.0
 * @since 17/12/19
 */

public abstract class Headset {
    protected static final boolean DEBUG = true;
    protected static final String TAG = "Headset";
    private static final String KEY_HEADSET_STATE = "state";
    protected Context mContext;
    
    protected AudioManager mAudioManager;
    private boolean mIsRegistered;
    protected List<HeadsetStateChangeListener> mHeadsetStateChangeListeners;
    private BroadcastReceiver mHeadsetBroadcastReceiver;
    
    public Headset(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mHeadsetStateChangeListeners = new ArrayList<>(2);
        initHeadsetBroadcastReceiver();
    }
    
    private void initHeadsetBroadcastReceiver() {
        mHeadsetBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DEBUG) {
                    Log.d(TAG, "onReceive===>" + intent);
                    Log.i(TAG, "extra=" + getBundleInfo(intent.getExtras()));
                }
                processHeadsetBroadcast(context, intent);
            }
        };
    }
    
    private void registerHeadsetReceiver() {
        if (!mIsRegistered) {
            mIsRegistered = true;
            IntentFilter headsetFilter = new IntentFilter();
            // 有线耳机监听
            headsetFilter.addAction(Intent.ACTION_HEADSET_PLUG);
            // 断开的时候，此广播立即发送，比其他断开广播速度快
            headsetFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            registerHeadsetReceiver(headsetFilter);
            mContext.registerReceiver(mHeadsetBroadcastReceiver, headsetFilter);
        }
    }
    
    protected abstract void registerHeadsetReceiver(IntentFilter headsetFilter);
    protected abstract void registerHeadsetReceiverProcessor(Context context, Intent intent);
    
    private void processHeadsetBroadcast(Context context, Intent intent) {
        String action = intent.getAction();
        
        // 1.有线耳机插入拔出事件，广播发送有延迟
        if (TextUtils.equals(action, Intent.ACTION_HEADSET_PLUG)) {
            if (intent.hasExtra(KEY_HEADSET_STATE)) {
                if (intent.getIntExtra(KEY_HEADSET_STATE, 0) == 0) {
                    if (mHeadsetStateChangeListeners != null) {
                        for (HeadsetStateChangeListener listener : mHeadsetStateChangeListeners) {
                            listener.onWiredHeadsetDisconnected();
                        }
                    }
                } else {
                    if (mHeadsetStateChangeListeners != null) {
                        for (HeadsetStateChangeListener listener : mHeadsetStateChangeListeners) {
                            listener.onWiredHeadsetConnected();
                        }
                    }
                }
            }
        }
        // 耳机拔出时的广播（包括蓝牙和有线），速度较快
        else if (TextUtils.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY, action)) {
            if (mHeadsetStateChangeListeners != null) {
                for (HeadsetStateChangeListener listener : mHeadsetStateChangeListeners) {
                    listener.onAudioBecomeNoisy();
                }
            }
        }
        // 剩下的是蓝牙的监听
        else {
            registerHeadsetReceiverProcessor(context, intent);
        }
    }
    
    public boolean isWiredHeadsetOn() {
        return mAudioManager != null && mAudioManager.isWiredHeadsetOn();
    }
    
    public void unRegisterHeadsetReceiver() {
        if (mIsRegistered) {
            mIsRegistered = false;
            mContext.unregisterReceiver(mHeadsetBroadcastReceiver);
        }
    }
    
    public void addHeadsetStateChangeListener(HeadsetStateChangeListener listener) {
        if (listener != null) {
            mHeadsetStateChangeListeners.add(listener);
        }
    }
    
    public void removeHeadsetStateChangeListener(HeadsetStateChangeListener listener) {
        if (listener != null) {
            mHeadsetStateChangeListeners.remove(listener);
        }
    }
    
    public void clearHeadsetStateChangeListener() {
        mHeadsetStateChangeListeners.clear();
    }
    
    protected String getBundleInfo(Bundle b) {
        StringBuilder sb = new StringBuilder("[");
        if (b != null) {
            Set<String> set = b.keySet();
            for (String key : set) {
                sb.append(key).append(":");
                sb.append(b.get(key)).append(";");
            }
        }
        sb.append("]");
        return sb.toString();
    }
    
    protected void start() {
        registerHeadsetReceiver();
    }
    
    protected void release() {
        unRegisterHeadsetReceiver();
        clearHeadsetStateChangeListener();
    }
}