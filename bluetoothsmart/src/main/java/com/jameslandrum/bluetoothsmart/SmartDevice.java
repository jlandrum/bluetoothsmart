package com.jameslandrum.bluetoothsmart;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Build;
import android.os.Debug;
import android.support.annotation.CallSuper;
import android.util.Log;
import android.util.Pair;

import com.jameslandrum.bluetoothsmart.actions.Action;
import com.jameslandrum.bluetoothsmart.actions.ActionRunner;
import com.jameslandrum.bluetoothsmart.annotations.AdEvaluator;
import com.jameslandrum.bluetoothsmart.annotations.CharDef;
import com.jameslandrum.bluetoothsmart.annotations.SmartDeviceDef;
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

	private HashMap<CharacteristicPair,Characteristic> mCharacteristics = new HashMap<>();
	private ArrayList<UpdateListener<T>> mUpdateListeners = new ArrayList<>();
	private ArrayList<GattListener> mGattListeners = new ArrayList<>();
	private ActionRunner mActionRunner;
	private SmartDeviceDef mDeclaration;

	public SmartDevice(BluetoothDevice device) {
		mDevice = device;
		mName = mDevice.getName();
		mDeclaration = getClass().getAnnotation(SmartDeviceDef.class);
		if (mDeclaration == null) throw new RuntimeException("Device must have SmartDeviceDef.");

		if (mName == null || mName.isEmpty()) mName = device.getAddress();

		loadFieldProcessors();
		loadCharacteristics();
	}

	private void loadCharacteristics() {
		try {
			for (Field f : this.getClass().getDeclaredFields()) {
				Annotation a = f.getAnnotation(CharDef.class);
				if (a!=null) {
					CharDef charDef = (CharDef) a;
					Log.e("OK","CHAR " + charDef.service());
					f.set(this,getCharacteristic(charDef.service(), charDef.id()));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void loadFieldProcessors() {
		try {
			for (Field f : this.getClass().getDeclaredFields()) {
				for (Annotation a : f.getAnnotations()) {
					if (a.annotationType() == null) continue;
					AdEvaluator evaluator = a.annotationType().getAnnotation(AdEvaluator.class);
					if (evaluator != null && evaluator.annotationType() == AdEvaluator.class) {
						Constructor[] c = evaluator.evaluator().getConstructors();
						Log.e("OK","ADPARSE " + c[0].getName());
						if (c.length > 1) throw new RuntimeException("Evaluator can only have one constructor");
						////noinspection unchecked
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

	public void prepareActionRunner(Context c, ActionRunner customActionRunner) {
		mAppContext = c;
		mActionRunner = customActionRunner;
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
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
				onConnect();
			}
		}
		Log.i("BluetoothGatt", "Device " + getName() + " is " + (mConnected?"":"not ") + "connected. (" + status + "," +newState + ")");
	}

	public void onConnect() {
		for (UpdateListener listener : mUpdateListeners) {
			listener.onConnect();
		}
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

	public Characteristic getCharacteristic(String serviceId, String characteristicId) {
		Characteristic c = mCharacteristics.get(new CharacteristicPair(serviceId,characteristicId));
		if (c == null) {
			c = new Characteristic(serviceId,characteristicId);
			updateCharacteristic(c);
			mCharacteristics.put(new CharacteristicPair(serviceId,characteristicId), c);
		}
		return c;
	}

	public boolean updateCharacteristic(Characteristic characteristic) {
		try {
			characteristic.setCharacteristic(
					mGatt.getService(characteristic.getServiceId())
							.getCharacteristic(characteristic.getCharacteristicId()));
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		super.onCharacteristicChanged(gatt, characteristic);
		Characteristic c = 	getCharacteristic(characteristic.getService().getUuid().toString(),
				characteristic.getUuid().toString());
		if (c != null){
			c.setCharacteristicValue(characteristic.getValue(), true);
		} else {
			throw new RuntimeException("Characteristic notified but not known!");
		}
	}

	@Override
	public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
		for (GattListener listener : mGattListeners) {
			listener.onDescriptorWrite(descriptor, status);
		}
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
		void onDescriptorWrite(BluetoothGattDescriptor descriptor, int status);
	}

	public static class NotConnectedError implements Action.ActionError {
	}

	public class CharacteristicPair extends android.support.v4.util.Pair<String,String> {
		/**
		 * Constructor for a Pair.
		 *
		 * @param first  the first object in the Pair
		 * @param second the second object in the pair
		 */
		public CharacteristicPair(String first, String second) {
			super(first.toLowerCase(), second.toLowerCase());
		}
	}
}
