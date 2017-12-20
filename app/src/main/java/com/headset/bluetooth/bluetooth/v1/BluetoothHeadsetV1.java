package com.headset.bluetooth.bluetooth.v1;

import com.headset.bluetooth.bluetooth.AbsBluetoothHeadset;
import com.headset.bluetooth.listener.HeadsetStateChangeListener;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.text.TextUtils;

/**
 * <p>打开蓝牙SCO模式的第一种方案：
 * <p>1.此方案使用API1就有的api，直接操作AudioManager。
 * <p>2.startBluetoothSco()和stopBluetoothSco()配对使用。
 * <p>3.如果极端情况下无法调用stopBluetoothSco()，如杀进程时，进程结束之后，<strong>系统会自动复原退出SCO模式，不会搞乱蓝牙模式引起系统蓝牙无声</strong>。
 * <p>4.为了防止在SCO切换之后，被其他应用如微信之类的语音打断，采用了一种保活机制，每隔2秒检测一次状态。
 * <p>5.使用此方案，耳机在切换到SCO和退出SCO时 会有 提示音。
 * <p>6.此方案切换到SCO的速度 较快
 * <p>
 *
 * @author kangsen
 * @version v1.0
 * @since 17/12/20
 */

public class BluetoothHeadsetV1 extends AbsBluetoothHeadset{
    
    public BluetoothHeadsetV1(Context context) {
        super(context);
    }
    
    @Override
    protected void registerBluetoothHeadsetReceiver(IntentFilter headsetFilter) {
        // 注册使用startBluetoothSco开启SCO后的广播
        headsetFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
    }
    
    @Override
    protected void registerBluetoothHeadsetReceiverProcessor(Context context, Intent intent) {
        String action = intent.getAction();
        int state;
        // SCO切换广播
        if (TextUtils.equals(action, AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
            state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                mIsScoOn = true;
                if (mHeadsetStateChangeListeners != null) {
                    for (HeadsetStateChangeListener listener : mHeadsetStateChangeListeners) {
                        listener.onBluetoothScoConnected();
                    }
                }
            } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED || state == -1) {
                if (mHeadsetStateChangeListeners != null) {
                    for (HeadsetStateChangeListener listener : mHeadsetStateChangeListeners) {
                        listener.onBluetoothScoDisconnected();
                    }
                }
                mIsScoOn = false;
            }
        }
    }
    
    @Override
    protected void startScoMode() {
        mAudioManager.startBluetoothSco();
    }
    
    @Override
    protected void stopScoMode() {
        mAudioManager.stopBluetoothSco();
    }
    
}
