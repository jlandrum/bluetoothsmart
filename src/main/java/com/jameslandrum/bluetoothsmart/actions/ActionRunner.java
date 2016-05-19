package com.jameslandrum.bluetoothsmart.actions;

import android.annotation.SuppressLint;
import android.util.Log;

import com.jameslandrum.bluetoothsmart.SmartDevice;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * ActionRunner - Runs a series of actions on a Bluetooth GATT-enabled device.
 * @author James Landrum
 */

// Suppress API restrictions on ConcurrentLinkedDeque.
@SuppressWarnings("unused")
@SuppressLint("NewApi")
public class ActionRunner extends Thread {
    private int mInterval;
    private ConcurrentLinkedDeque<Action> mActions = new ConcurrentLinkedDeque<>();
    private ArrayList<ErrorHandler> mListeners = new ArrayList<>();
    private SmartDevice mDevice;
    private boolean mContinueOnComplete;
    private long mLastTrigger;
    private Action.ActionError mError;
    private int mAutoTerminate = -1;
    private final Object mHolder = new Object();
    private int mMaxActions = 16;
    private boolean mAutoConnect;
    private int mMode;

    private static final int ASLEEP = 0;
    private static final int AWAKE = 1;

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

	/**
     * Starts the ActionRunner process.
     */
    @Override
    public void run() {
        super.run();
        while (!isInterrupted()) {
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
            if (a.isRepeating()) {
                mActions.addFirst(a);
                Log.d("ActionRunner", "ActionRunner REPEATING ACTION: " + a.toString() + ". There are " + mActions.size() + " more in queue.");
            }
            Log.d("ActionRunner", "ActionRunner is executing action: " + a.toString() + ". There are " + mActions.size() + " more in queue.");
            mError = a.execute(mDevice);
            if (mError != null) {
                Log.d("ActionRunner", "ActionRunner encountered an error and will pause.");
                boolean wasHandled = false;
                for (ErrorHandler listener : mListeners) {
                    wasHandled = listener.onError(mError);
                    if (wasHandled) break;
                }
                mError = null;
                if (!wasHandled) {
                    Log.d("ActionRunner", "ActionRunner failed to resolve error. Will now clear the action queue and restart.");
                    mActions.clear();
                    continue;
                }
                waitForInterrupt();
                continue;
            }
            Log.d("ActionRunner", "Action took " + (System.currentTimeMillis() - mLastTrigger) + "ms to complete.");
            long timeUntilNextTrigger = mInterval - (System.currentTimeMillis() - mLastTrigger);
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
        if (mAutoConnect && !mDevice.isConnected() && !mActions.contains(Connect.CONNECT)) {
            mActions.addFirst(Connect.CONNECT);
            Log.d("ActionRunner", "Added CONNECT automatically as per Auto Connect policy.");
        }
        if (mMaxActions > 0 && mMaxActions <= mActions.size()) {
            Log.d("ActionRunner", "Cannot add action - queue is full!");
            return;
        }
        mActions.add(a);
        Log.d("ActionRunner", "Added action to ActionRunner!");
        if (mError == null && mMode == ASLEEP) {
            Log.d("ActionRunner", "Waking ActionRunner!");
            synchronized (mHolder) {
                mHolder.notify();
                Log.d("ActionRunner", "Waking ActionRunner OK!");
                mMode = AWAKE;
            }
        }
    }

	/**
     * Adds the action to the queue, and moves it to the end of the queue if it has already
     * been placed in the queue.
     * @param a Action to add to the queue.
     * @param pushToLastIfExists If true, the action will be removed and pushed to the end as long
     *                           as the action is not first in line.
     */
    public void addActionToQueue(Action a, boolean pushToLastIfExists) {
        if (pushToLastIfExists && mActions.contains(a) && mActions.peekFirst() != a) {
            mActions.remove(a);
        }
        addActionToQueue(a);
    }

	/**
	 * Checks if a given action is already in the queue.
     * This can be used to avoid flooding the queue with duplicates, however note that this may
     * have adverse effects as it does not consider the order of operations.
     * Consider using addActionToQueue(Action,Boolean).
     * @param a The action to seek.
     * @return True if the action is in the queue, false otherwise.
     */
    public boolean isActionPending(Action a) {
        return mActions.contains(a);
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

	/**
     * Reveals the next action without removing it from the stack.
     * @return
     */
    public Action peekAction() {
        return mActions.peekFirst();
    }

	/**
     * Enables automatic connection support.
     * @param auto true to enable, false to disable.
     */
    public void enableAutoConnect(boolean auto) {
        mAutoConnect = auto;
    }

	/**
	 * Sets the maximum allowed actions.
     * @param max The maximum allowed or <=0 for infinite.
     */
    public void setMaxActions(int max) {
        mMaxActions = max;
    }

	/**
	 * Adds a callback that will have the opportunity to respond to an error.
     * Error handlers are called in a first-in, first-called order.
     * @param errorHandler The handler to add.
     */
    public void addErrorHandler(ErrorHandler errorHandler) {
        mListeners.add(errorHandler);
    }

	/**
     * Removes the given callback from the error handler stack.
     * @param errorHandler The handler to remove.
     */
    public void removeErrorHandler(ErrorHandler errorHandler) {
        mListeners.remove(errorHandler);
    }

    private void pauseThread() {
        mMode = ASLEEP;
        if (mAutoTerminate > 0) {
            long beginSleep = System.currentTimeMillis();
            Log.d("ActionRunner", "ActionRunner is asleep - will auto terminate if not interrupted in " + mAutoTerminate + "ms");
            try { synchronized (mHolder) { mHolder.wait(mAutoTerminate); } } catch (InterruptedException ignored) { }
            if (System.currentTimeMillis() - beginSleep >= mAutoTerminate ) {
                mDevice.disconnect();
                mMode = ASLEEP;
                Log.d("ActionRunner", "ActionRunner is auto-killing connection.");
            } else {
                mMode = AWAKE;
                Log.d("ActionRunner", "ActionRunner received wake signal.");
                return;
            }
        }
        waitForInterrupt();
    }

    private  void waitForInterrupt(long duration) {
        try { synchronized (mHolder) { mHolder.wait(duration); } } catch (InterruptedException ignored) {}
    }

    private synchronized void waitForInterrupt() {
        try { synchronized (mHolder) { mHolder.wait(); } } catch (InterruptedException ignored) { }
    }

    /**
     * Error handler
     */
    public interface ErrorHandler {
        boolean onError(Action.ActionError error);
    }

}
