package com.jameslandrum.bluetoothsmart;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.util.Log;

import com.jameslandrum.bluetoothsmart.actions.Action;
import com.jameslandrum.bluetoothsmart.actions.ActionRunner;
import com.jameslandrum.bluetoothsmart.annotations.AdEvaluator;
import com.jameslandrum.bluetoothsmart.annotations.SmartDeviceDeclaration;
import com.jameslandrum.bluetoothsmart.processors.adfield.BaseAdEvaluator;
import com.jameslandrum.bluetoothsmart.throwable.InvalidStateException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A generic SmartDevice - acts as a very basic SmartDevice
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SmartDevice<T> extends BluetoothGattCallback {
	public static final String SMARTDEVICE_ID = "MacAddress";

	private Context mAppContext;
	protected final BluetoothDevice mDevice;
	protected BluetoothGatt mGatt;
	private boolean mConnected;
	private String mName;
	private long mLastAd;
	private HashMap<Field, BaseAdEvaluator<Boolean>> mFieldProcessors = new HashMap<>();

	private ArrayList<UpdateListener<T>> mUpdateListeners = new ArrayList<>();
	private ArrayList<GattListener> mGattListeners = new ArrayList<>();
	private ActionRunner mActionRunner;
	private SmartDeviceDeclaration mDeclaration;

	public SmartDevice(BluetoothDevice device) {
		mDevice = device;
		mName = mDevice.getName();
		mDeclaration = getClass().getAnnotation(SmartDeviceDeclaration.class);
		if (mDeclaration == null) throw new RuntimeException("Device must have SmartDeviceDeclaration.");

		if (mName == null || mName.isEmpty()) mName = device.getAddress();

		// TODO: Move to own method for clarity
		try {
			for (Field f : this.getClass().getDeclaredFields()) {
				for (Annotation a : f.getAnnotations()) {
					AdEvaluator evaluator = a.annotationType().getAnnotation(AdEvaluator.class);
					if (evaluator != null) {
						Constructor[] c = evaluator.evaluator().getConstructors();
						if (c.length > 1) throw new RuntimeException("Evaluator can only have one constructor");
						//noinspection unchecked
						mFieldProcessors.put(f, (BaseAdEvaluator<Boolean>) c[0].newInstance(a));
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@CallSuper
	public void newAdvertisement(byte[] data) {
		for (Field f : mFieldProcessors.keySet()) {
			try {
				f.setAccessible(true);
				f.set(this, mFieldProcessors.get(f).evaluate(data));
			} catch (IllegalAccessException e) {
				// TODO: Look at more graceful alternatives.
				throw new RuntimeException(e);
			}
		}

		mLastAd = System.currentTimeMillis();

		for (UpdateListener l : mUpdateListeners) //noinspection unchecked
			l.onUpdate(this);
	}

	public void newBeacon() {};

	public void prepareActionRunner(Context c, int interval, boolean continueOnComplete) {
		mAppContext = c;
		mActionRunner = new ActionRunner(this, interval, continueOnComplete);
	}

	public void connect() {
		if (mGatt != null) {
			if (!mConnected) mGatt.connect();
		} else if (mAppContext != null) {
			mGatt = mDevice.connectGatt(mAppContext, true, this);
		} else {
			throw new InvalidStateException("Must call prepareActionRunner before calling connect.");
		}
	}

	public void disconnect() {
		if (mGatt != null) {
			if (mConnected) mGatt.disconnect();
		}
	}


	public boolean isConnected() {
		return mConnected;
	}

	public boolean isVisible() { return System.currentTimeMillis() - mLastAd < mDeclaration.adFrequency(); }

	public String getName() {
		return mName;
	}

	public void bond() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			mDevice.createBond();
		}
	}

	public String getId() {
		return mDevice.getAddress();
	}

	public String getAddress() {
		return mDevice.getAddress();
	}

	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
		super.onConnectionStateChange(gatt, status, newState);
		mConnected = newState == BluetoothGatt.STATE_CONNECTED;
		if (mConnected) {
			if (mGatt.getServices().size() == 0) {
				mGatt.discoverServices();
			} else {
				for (UpdateListener listener : mUpdateListeners) {
					listener.onConnect();
				}
			}
		}
		Log.i("BluetoothGatt", "Device " + getName() + " is " + (mConnected?"":"not ") + "connected. (" + status + "," +newState + ")");
	}

	@Override
	public void onServicesDiscovered(BluetoothGatt gatt, int status) {
		super.onServicesDiscovered(gatt, status);
		for (UpdateListener listener : mUpdateListeners) {
			listener.onConnect();
		}
	}

	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		for (GattListener listener : mGattListeners) {
			listener.onCharacteristicWrite(characteristic, status);
		}
	}

	@Override
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		for (GattListener listener : mGattListeners) {
			listener.onCharacteristicRead(characteristic, status);
		}
	}

	@SuppressWarnings("unchecked")
	public void addOnUpdateListener(UpdateListener listener) {
		mUpdateListeners.add(listener);
	}

	public void removeOnUpdateListener(UpdateListener listener) {
		mUpdateListeners.remove(listener);
	}

	public void addGattListener(GattListener listener) {
		mGattListeners.add(listener);
	}

	public void removeGattListener(GattListener listener) {
		mGattListeners.remove(listener);
	}


	public BluetoothGatt getGatt() {
		return mGatt;
	}

	public ActionRunner getActionRunner() {
		return mActionRunner;
	}

	public interface UpdateListener<T> {
		void onUpdate(T device);
		void onConnect();
		void onDisconnect();
	}

	public interface GattListener {
		void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status);
		void onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status);
	}

	public static class NotConnectedError implements Action.ActionError {
	}
}
