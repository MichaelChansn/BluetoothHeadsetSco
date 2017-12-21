/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.headset.bluetooth.bluetooth.v2;

import java.util.List;

import com.headset.bluetooth.bluetooth.AbsBluetoothHeadset;
import com.headset.bluetooth.listener.HeadsetStateChangeListener;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.Log;

/**
 *
 * <p>打开蓝牙SCO模式的第二种方案：
 * <p>1.此方案使用API11才有的API，直接操作蓝牙耳机BluetoothHeadset。
 * <p>2.startVoiceRecognition()和stopVoiceRecognition()配对使用。
 * <p>3.如果极端情况下无法调用stopVoiceRecognition()，如杀进程时，进程结束之后，<strong>系统不会自动复原蓝牙状态，会搞乱蓝牙模式引起系统蓝牙无声</strong>。
 * <p>4.为了防止在SCO切换之后，被其他应用如微信之类的语音打断，采用了一种保活机制，每隔2秒检测一次状态。
 * <p>5.使用此方案，耳机在切换到SCO和退出SCO时 没有 提示音。
 * <p>6.此方案切换到SCO的速度 较慢
 * <p>
 *
 * @author kangsen
 * @version v1.0
 * @since 17/12/7
 */

public class BluetoothHeadsetV2 extends AbsBluetoothHeadset {
    private android.bluetooth.BluetoothHeadset mBluetoothHeadset;
    private BluetoothProfile.ServiceListener mHeadsetProfileListener;
    
    public BluetoothHeadsetV2(Context context) {
        super(context);
        initProfileListener();
        registerProxy();
    }
    
    @Override
    protected void registerBluetoothHeadsetReceiver(IntentFilter headsetFilter) {
        // 注册使用startVoiceRecognition 启动的SCO监听
         headsetFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
    }
    
    @Override
    protected void registerBluetoothHeadsetReceiverProcessor(Context context, Intent intent) {
        String action = intent.getAction();
        int state;
        if (TextUtils.equals(action, BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
            state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
            if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                mIsScoOn = true;
                if (mHeadsetStateChangeListeners != null) {
                    for (HeadsetStateChangeListener listener : mHeadsetStateChangeListeners) {
                        listener.onBluetoothScoConnected();
                    }
                }
            } else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                if (mHeadsetStateChangeListeners != null) {
                    for (HeadsetStateChangeListener listener : mHeadsetStateChangeListeners) {
                        listener.onBluetoothScoDisconnected();
                    }
                }
                mIsScoOn = false;
            }
        }
    }
    
    private void initProfileListener() {
        mHeadsetProfileListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                mBluetoothHeadset = (BluetoothHeadset) proxy;
                List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
                if (devices.size() > 0) {
                    mConnectedHeadset = devices.get(0);
                    if (isBluetoothSetScoOn()) {
//                        stopScoMode();
                        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    }
                }
            }
            
            @Override
            public void onServiceDisconnected(int profile) {
                mBluetoothHeadset = null;
                mConnectedHeadset = null;
            }
        };
    }
    
    private void registerProxy(){
        if(isSupportBluetooth() && isSupportScoMode()){
            mBluetoothAdapter.getProfileProxy(mContext, mHeadsetProfileListener, BluetoothProfile.HEADSET);
        }
    }
    
    private void unRegisterProxy() {
        mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
    }
    
    
    @Override
    protected void startScoMode() {
        mBluetoothHeadset.startVoiceRecognition(mConnectedHeadset);
        
    }
    
    @Override
    protected void stopScoMode() {
        mBluetoothHeadset.stopVoiceRecognition(mConnectedHeadset);
    }
    
    
    
    @Override
    public void release() {
        super.release();
        if (DEBUG) {
            Log.d(TAG, "releaseBluetooth");
        }
        
        // 1.反注册蓝牙监听
        unRegisterProxy();
    }
    
    @Override
    public boolean isBluetoothSetOn(){
        return super.isBluetoothSetOn() && mConnectedHeadset != null;
    }
    
    @Override
    public boolean isBluetoothSetScoOn(){
        boolean isSco = super.isBluetoothSetScoOn();
        return  isSco
                && mBluetoothHeadset != null
                && mBluetoothHeadset.isAudioConnected(mConnectedHeadset);
    }
}
