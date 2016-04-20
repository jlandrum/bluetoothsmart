package com.jameslandrum.bluetoothsmart.actions;

import android.annotation.SuppressLint;
import android.util.Log;

import com.jameslandrum.bluetoothsmart.SmartDevice;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;

// Suppress API restrictions on ConcurrentLinkedDeque.
@SuppressLint("NewApi")
public class ActionRunner extends Thread {
    private int mInterval;
    private ConcurrentLinkedDeque<Action> mActions = new ConcurrentLinkedDeque<>();
    private ArrayList<RunnerStateListener> mListeners = new ArrayList<>();
    private SmartDevice mDevice;
    private boolean mContinueOnComplete;
    private long mLastTrigger;
    private long mBeginSleep;
    private Action.ActionError mError;
    private int mAutoTerminate = -1;
    private boolean mWaitAutoTerm;
    private final Object mHolder = new Object();
    private int mMaxActions = 10;
    private boolean mDebug = true;

    /**
     * Creates a thread that will execute actions on a timely manner.
     * @param device The smart device to associate with this action runner.
     * @param interval The minimum duration between action execution.
     * @param continueOnComplete True to base interval on action complete.
     *                           False to base on action start.
     */
    public ActionRunner(SmartDevice device, int interval, boolean continueOnComplete) {
        mDevice = device;
        mInterval = interval;
        mContinueOnComplete = continueOnComplete;
        start();
    }

    @Override
    public void run() {
        super.run();
        while (true) {
            Log.d("ActionRunner", "ActionRunner is beginning a cycle.");
            if (!mContinueOnComplete) mLastTrigger = System.currentTimeMillis();
            if (mActions.size() == 0) {
                Log.d("ActionRunner", "ActionRunner has no tasks. Sleeping!");
                pauseThread();
                Log.d("ActionRunner", "ActionRunner has woken up!");
                continue;
            }
            if (mContinueOnComplete) mLastTrigger = System.currentTimeMillis();
            Action a = mActions.removeFirst();
            Log.d("ActionRunner", "ActionRunner is executing action: " + a.toString());
            mError = a.execute(mDevice);
            if (mError != null) {
                for (RunnerStateListener listener : mListeners) {
                    listener.onPaused(mError);
                }
                Log.d("ActionRunner", "ActionRunner encountered an error and will pause.");
                if (mDebug) {
                    Log.d("ActionRunner", "ActionRunner will now clear the action queue and restart.");
                    mActions.clear();
                    continue;
                }
                waitForInterrupt();
                continue;
            }
            long timeUntilNextTrigger = mInterval - System.currentTimeMillis() - mLastTrigger;
            if (timeUntilNextTrigger > 0 && !a.noDelay()) {
                Log.d("ActionRunner", "ActionRunner delaying for " + timeUntilNextTrigger + "ms");
                waitForInterrupt(timeUntilNextTrigger);
            } else {
                Log.d("ActionRunner", "ActionRunner is not delaying.");
            }
        }
    }

    /**
     * Adds a new action to the queue.
     * If the thread has paused, it will wake the thread.
     * @param a The action to add to the queue.
     */
    public void addActionToQueue(Action a) {
        if (a == null) return;
        if (mMaxActions <= mActions.size()) {
            Log.d("ActionRunner", "Cannot add action - queue is full!");
            return;
        }
        mActions.add(a);
        Log.d("ActionRunner", "Added action to ActionRunner!");
        Log.d("ActionRunner", "Error: " + mError + ", State: " + getState());
        if (mError == null && (getState() == State.WAITING || (getState() == State.TIMED_WAITING && mWaitAutoTerm)) ) {
            Log.d("ActionRunner", "Waking ActionRunner!");
            synchronized (mHolder) {
                mHolder.notify();
                Log.d("ActionRunner", "Waking ActionRunner OK!");
            }
        }
    }

    /**
     * Sets the duration
     * @param i How long the runner should sit until calling a disconnect on the devices' GATT.
     *          Set to -1 to disable auto disconnect.
     * @return Self
     */
    public ActionRunner setAutoTerminate(int i) {
        mAutoTerminate = i;
        return this;
    }

    public Action peekAction() {
        return mActions.peekFirst();
    }

    public interface RunnerStateListener {
        void onPaused(Action.ActionError error);
    }

    private void pauseThread() {
        if (mAutoTerminate > 0) {
            mBeginSleep = System.currentTimeMillis();
            mWaitAutoTerm = true;
            Log.d("ActionRunner", "ActionRunner is asleep - will auto terminate if not interrupted in " + mAutoTerminate + "ms");
            try { synchronized (mHolder) { mHolder.wait(mAutoTerminate); } } catch (InterruptedException ignored) { }
            if (System.currentTimeMillis() - mBeginSleep >= mAutoTerminate ) {
                mDevice.disconnect();
                Log.d("ActionRunner", "ActionRunner is auto-killing connection.");
            } else {
                Log.d("ActionRunner", "ActionRunner received wake signal.");
                return;
            }
        }
        mWaitAutoTerm = false;
        waitForInterrupt();
    }

    private  void waitForInterrupt(long duration) {
        try { synchronized (mHolder) { mHolder.wait(duration); } } catch (InterruptedException ignored) {}
    }

    private synchronized void waitForInterrupt() {
        try { synchronized (mHolder) { mHolder.wait(); } } catch (InterruptedException ignored) { }
    }
}
