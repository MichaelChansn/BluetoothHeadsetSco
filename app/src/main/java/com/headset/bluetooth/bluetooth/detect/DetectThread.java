package com.headset.bluetooth.bluetooth.detect;

/**
 * <p>
 *
 * @author kangsen
 * @version v9.0
 * @since 17/12/19
 */

public class DetectThread {
    
    private MyThread mThread;
    private Runnable mRunnable;
    
    public DetectThread(Runnable runnable){
        mRunnable = runnable;
        mThread = new MyThread(mRunnable);
    }
    
    public void start() {
        mThread.start();
    }
    
    public void stop() {
        mThread.stopRun();
    }
    
    private class MyThread extends Thread {
        Runnable mRunnable;
        boolean mIsRunning;
        private static final int SLEEP_TIME = 2000;
        
        private MyThread(Runnable runnable){
            this.mRunnable = runnable;
            this.mIsRunning = true;
        }
        
        @Override
        public void run() {
            while (mIsRunning && !this.isInterrupted()) {
                mRunnable.run();
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    mIsRunning = false;
                    break;
                }
            }
        }
        
        private void stopRun(){
            mIsRunning = false;
            this.interrupt();
        }
    }
}
