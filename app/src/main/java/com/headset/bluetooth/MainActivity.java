package com.headset.bluetooth;

import java.io.FileDescriptor;
import java.io.IOException;

import com.headset.bluetooth.bluetooth.AbsBluetoothHeadset;
import com.headset.bluetooth.bluetooth.v1.BluetoothHeadsetV1;
import com.headset.bluetooth.bluetooth.v2.BluetoothHeadsetV2;
import com.headset.bluetooth.other.BluetoothHelper;

import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    
    private static final String MUSIC_PATH = "jintou.mp3";
    
    private Button mButtonPlay;
    private Button mButtonBluetooth;
    private TextView mTextViewShow;
    private TextView mTextViewBlue;
    private MediaPlayer mMediaPlayer;
    
    private AbsBluetoothHeadset mBluetoothHeadset;
    private BluetoothHelper mBluetoothHelper;
    private FileDescriptor mAssetFileDescriptor;
    
    private int mCurrentPos;
    
    private boolean mIsSco;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }
    
    private void init(){
        mButtonPlay = findViewById(R.id.button_play);
        mButtonBluetooth = findViewById(R.id.button_bluetooth);
        mTextViewShow = findViewById(R.id.text_show);
        mTextViewBlue = findViewById(R.id.text_blue);
        mMediaPlayer = new MediaPlayer();
        AssetManager assetManager = getResources().getAssets();
        try {
            mAssetFileDescriptor = assetManager.openFd(MUSIC_PATH).getFileDescriptor();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        mButtonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            playMusic();
            }
        });
    
        
        mButtonBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchSco();
            }
        });
    }
    
    private void switchSco() {
        if (!mIsSco) {
//            mBluetoothHeadset = new BluetoothHeadsetV1(this);
            mBluetoothHeadset = new BluetoothHeadsetV2(this);
            mBluetoothHeadset.startBluetooth();
//            mBluetoothHelper = new BluetoothHelper(this);
//            mBluetoothHelper.start();
            mIsSco = true;
            mButtonBluetooth.setText("关闭蓝牙SCO");
            mTextViewBlue.setText("蓝牙SCO模式已经开启");
    
        } else {
            mBluetoothHeadset.release();
//            mBluetoothHelper.stop();
            mIsSco = false;
            mButtonBluetooth.setText("开启蓝牙SCO");
            mTextViewBlue.setText("蓝牙SCO模式已经关闭");
    
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }
    
    private void playMusic() {
        if (!mMediaPlayer.isPlaying()) {
            play(mCurrentPos);
        } else {
            pause();
        }
    }
    
    private void pause(){
        if (mMediaPlayer.isPlaying()) {
            mCurrentPos = mMediaPlayer.getCurrentPosition();
            mTextViewShow.setText("音乐停止播放...");
            mButtonPlay.setText("播放音乐");
            mMediaPlayer.stop();
        }
    }
    
    private void play(int playPosition) {
        try {
            mMediaPlayer.reset();// 把各项参数恢复到初始状态
            mMediaPlayer.setDataSource(mAssetFileDescriptor);
            mMediaPlayer.prepare();// 进行缓冲
            mMediaPlayer.setOnPreparedListener(new MyPreparedListener(playPosition));
            mTextViewShow.setText("音乐正在播放...");
            mButtonPlay.setText("停止音乐");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private final class MyPreparedListener implements
            android.media.MediaPlayer.OnPreparedListener {
        private int playPosition;
        
        private MyPreparedListener(int playPosition) {
            this.playPosition = playPosition;
        }
        
        @Override
        public void onPrepared(MediaPlayer mp) {
            mMediaPlayer.start();// 开始播放
            if (playPosition > 0) {
                mMediaPlayer.seekTo(playPosition);
            }
        }
        
    }
}
