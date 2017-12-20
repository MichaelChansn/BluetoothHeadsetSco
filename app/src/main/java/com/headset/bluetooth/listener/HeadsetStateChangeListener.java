/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.headset.bluetooth.listener;

/**
 * 当耳机状态发生改变的时候进行调用
 * <p>
 *
 * @author kangsen
 * @version v1.0
 * @since 17/12/7
 */

public interface HeadsetStateChangeListener {
    
    /**
     * 有线耳机插入
     */
    public void onWiredHeadsetConnected();
    
    /**
     * 有线耳机拔出
     */
    public void onWiredHeadsetDisconnected();
    
    /**
     * 蓝牙耳机插入
     */
    public void onBluetoothHeadsetConnected();
    
    /**
     * 蓝牙耳机拔出
     */
    public void onBluetoothHeadsetDisconnected();
    
    /**
     * 蓝牙耳机切换至sco模式
     */
    public void onBluetoothScoDisconnected();
    
    /**
     * 蓝牙耳机切换出sco模式
     */
    public void onBluetoothScoConnected();
    
    /**
     * 由于拔掉耳机的回调，不区分是蓝牙还是有线
     */
    public void onAudioBecomeNoisy();
}
