package com.headset.bluetooth.bluetooth;

import com.headset.bluetooth.bluetooth.detect.DetectThread;
import com.headset.bluetooth.headset.Headset;
import com.headset.bluetooth.listener.HeadsetStateChangeListener;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.Log;

/**
 * <p>
 *
 * @author kangsen
 * @version v1.0
 * @since 17/12/20
 */

public abstract class AbsBluetoothHeadset extends Headset {
    
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothDevice mConnectedHeadset;
    
    protected boolean mIsScoOn;
    protected boolean mBluetoothSetOn;
    
    /** 使用一个后台线程每隔2秒检测一次SCO */
    private DetectThread mDetectThread;
    /** 是否已经启动SCO切换 */
    private boolean mIsStartDetect;
    
    public AbsBluetoothHeadset(Context context) {
        super(context);
        mIsScoOn = (mAudioManager != null && mAudioManager.isBluetoothScoOn());
        mBluetoothSetOn = (mAudioManager != null && mAudioManager.isBluetoothA2dpOn());
    }
    
    @Override
    protected void registerHeadsetReceiver(IntentFilter headsetFilter) {
        // 1.注册蓝牙耳机主动断开和链接的事件监听
        headsetFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        // 2.注册手机主动关闭和打开蓝牙开关的事件监听
        headsetFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerBluetoothHeadsetReceiver(headsetFilter);
    }
    
    protected abstract void registerBluetoothHeadsetReceiver(IntentFilter headsetFilter);
    protected abstract void registerBluetoothHeadsetReceiverProcessor(Context context, Intent intent);
    
    @Override
    protected void registerHeadsetReceiverProcessor(Context context, Intent intent) {
        String action = intent.getAction();
        int state;
        // 蓝牙设备链接
        if (TextUtils.equals(action, BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
            state = intent.getIntExtra(
                    android.bluetooth.BluetoothHeadset.EXTRA_STATE,
                    android.bluetooth.BluetoothHeadset.STATE_DISCONNECTED);
            if (state == android.bluetooth.BluetoothHeadset.STATE_CONNECTED) {
                mConnectedHeadset = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBluetoothSetOn = true;
                if (mHeadsetStateChangeListeners != null) {
                    for (HeadsetStateChangeListener listener : mHeadsetStateChangeListeners) {
                        listener.onBluetoothHeadsetConnected();
                    }
                }
            
            } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                if (mHeadsetStateChangeListeners != null) {
                    for (HeadsetStateChangeListener listener : mHeadsetStateChangeListeners) {
                        listener.onBluetoothHeadsetDisconnected();
                    }
                }
                mConnectedHeadset = null;
                mBluetoothSetOn = false;
            }
        }
        // 手机蓝牙按钮关闭
        else if (TextUtils.equals(action, BluetoothAdapter.ACTION_STATE_CHANGED)) {
            state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if (state == BluetoothAdapter.STATE_OFF) {
                if (mHeadsetStateChangeListeners != null) {
                    for (HeadsetStateChangeListener listener : mHeadsetStateChangeListeners) {
                        listener.onBluetoothHeadsetDisconnected();
                    }
                }
                mConnectedHeadset = null;
                mBluetoothSetOn = false;
                mIsScoOn = false;
            }
        }
        // SCO切换模式
        else {
            registerBluetoothHeadsetReceiverProcessor(context, intent);
        }
    }
    
    public boolean isSupportBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null;
    }
    
    public boolean isSupportScoMode() {
        return mAudioManager.isBluetoothScoAvailableOffCall();
    }
    
    public boolean isBluetoothSetScoOn() {
        return mIsScoOn;
    }
    
    public boolean isBluetoothSetOn() {
        return (mConnectedHeadset != null) || mIsScoOn || mBluetoothSetOn;
    }
    
    @Override
    public void release() {
        super.release();
        if (DEBUG) {
            Log.d(TAG, "releaseBluetooth");
        }
        
        // 1.停止模式切换计时器
        stopDetect();
    }
    
    public void startBluetooth() {
        // 注册监听
        super.start();
        startDetect();
    }
    
    private void enterScoMode() {
        if (mAudioManager != null) {
            if (DEBUG) {
                Log.d(TAG, "enter sco");
            }
            if (isBluetoothSetOn()) {
                // 严重注意！！！！！！！！！！！！！！！
                // setMode 必须在startVoiceRecognition之前调用，否则不生效
                // 小米6手机上setMode会导致手机卡顿一下，就算放到异步线程也不行
                mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                startScoMode();
            }
            mAudioManager.setBluetoothScoOn(true);
            mAudioManager.setSpeakerphoneOn(false);
        }
    }
    
    private void exitScoMode() {
        if (mAudioManager != null) {
            if (DEBUG) {
                Log.d(TAG, "exit sco");
            }
            if (isBluetoothSetOn()) {
                mAudioManager.setMode(AudioManager.MODE_NORMAL);
                stopScoMode();
            }
            mAudioManager.setBluetoothScoOn(false);
            mAudioManager.setSpeakerphoneOn(true);
        }
    }
    
    protected abstract void startScoMode();
    
    protected abstract void stopScoMode();
    
    
    private void startDetect() {
        if (!mIsStartDetect) {
            mIsStartDetect = true;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) {
                        Log.d(TAG, "detect thread tick");
                    }
                    if (isSupportBluetooth()
                            && isBluetoothSetOn()
                            && isSupportScoMode()
                            && !isBluetoothSetScoOn()
                            && !isWiredHeadsetOn()) {
                        if (DEBUG) {
                            Log.d(TAG, "startToggleSco");
                        }
                        enterScoMode();
                    }
                }
            };
            mDetectThread = new DetectThread(runnable);
            mDetectThread.start();
        }
    }
    
    private void stopDetect() {
        if (mIsStartDetect && mDetectThread != null) {
            if (DEBUG) {
                Log.d(TAG, "sco detect stop");
            }
            mDetectThread.stop();
            mIsStartDetect = false;
            exitScoMode();
        }
    }
}
