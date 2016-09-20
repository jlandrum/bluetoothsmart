/**
 * Copyright 2016 James Landrum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jameslandrum.bluetoothsmart.scanner;

import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Debug;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;

import com.jameslandrum.bluetoothsmart.DevicePersistentStorage;
import com.jameslandrum.bluetoothsmart.SmartDevice;
import com.jameslandrum.bluetoothsmart.annotations.Identifier;
import com.jameslandrum.bluetoothsmart.generic.GenericDevice;
import com.jameslandrum.bluetoothsmart.generic.GenericStorage;
import com.jameslandrum.bluetoothsmart.throwable.InvalidSmartDeviceImplementationException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interface class that connects to the proper device scanner for Android
 */
public abstract class DeviceScanner {
	private static final byte[] APPLE_PREFIX =
			new byte[]{0x4C, 0x00};

	@IntDef({SCAN_MODE_LOW_LATENCY, SCAN_MODE_LOW_POWER, SCAN_MODE_NORMAL, SCAN_MODE_PASSIVE})
	@Retention(RetentionPolicy.SOURCE)
	public @interface ScanMode {}
	public static final int SCAN_MODE_PASSIVE =     -1;
	public static final int SCAN_MODE_LOW_POWER =   0;
	public static final int SCAN_MODE_NORMAL =      1;
	public static final int SCAN_MODE_LOW_LATENCY = 2;

	protected static int mScanMode;
	private static DeviceScanner mInstance;
	private static boolean mAllowUnknowns = false;
	protected static final ArrayList<DeviceScannerListener> mListeners = new ArrayList<>();
	protected static final ArrayList<String> mInvalidDevices = new ArrayList<>();
	protected static final ConcurrentHashMap<String, SmartDevice> mDevices = new ConcurrentHashMap<>();
	protected static final ConcurrentHashMap<Method,Class<? extends com.jameslandrum.bluetoothsmart.SmartDevice>> mDeviceIdentifiers = new ConcurrentHashMap<>();
	private DevicePersistentStorage mStorage = new GenericStorage();

	public void forgetDevice(SmartDevice device) {
		mInvalidDevices.remove(device.getAddress());
		mDevices.remove(device.getAddress());
	}

	public void injectDevice(SmartDevice device) {
		mDevices.remove(device.getAddress());
		mInvalidDevices.remove(device.getAddress());
		mDevices.put(device.getAddress(),device);
	}


	/**
	 * Returns an instance of the DeviceScanner
	 * @return The relevant device scanner for this device.
	 */
	public static DeviceScanner getInstance() {
		if (mInstance != null) return mInstance;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			mInstance = new LollipopDeviceScanner();
		} else {
			mInstance = new KitKatDeviceScanner();
		}
		return mInstance;
	}

	DeviceScanner() {
	}

	public abstract void startScan(@ScanMode int scanMode);
	public abstract void stopScan();
	public abstract boolean isScanning();

	public void addDeviceType(Class<? extends com.jameslandrum.bluetoothsmart.SmartDevice> type) {
		try {
			for (Method m : type.getDeclaredMethods()) {
				if (m.getAnnotation(Identifier.class) != null) {
					mDeviceIdentifiers.put(m, type);
				}
			}
		} catch (Exception e) {
			throw new InvalidSmartDeviceImplementationException();
		}
	}

	void processAdvertisement(byte[] data, android.bluetooth.BluetoothDevice device, int rssi) {
  		if (mInvalidDevices.contains(device.getAddress())) return;

		boolean isBeacon = Arrays.equals(Arrays.copyOfRange(data, 5, 7), APPLE_PREFIX);

		if (mDevices.containsKey(device.getAddress())) {
			SmartDevice target = mDevices.get(device.getAddress());
			if (!target.isProcessingEnabled()) return;
			if (isBeacon) {
				target.newBeacon();
				for (DeviceScannerListener listener : mListeners) {
					listener.onDevicePinged(target);
				}
			} else {
				Log.i("Processing Ad", "Processing Advertisement for Device " + target.getAddress());
				target.newAdvertisement(data,rssi);
				for (DeviceScannerListener listener : mListeners) {
					listener.onDeviceUpdated(target);
				}
			}
		} else if (!isBeacon) {
			for (Method identifier : mDeviceIdentifiers.keySet()) {
				try {
					if ((boolean) identifier.invoke(null, new Object[] {data})) {
						SmartDevice target =
								mDeviceIdentifiers
										.get(identifier)
										.getDeclaredConstructor(BluetoothDevice.class)
										.newInstance(device);
						target.newAdvertisement(data,rssi);

						Log.d("MESSAGE", "New known discovered: " + device.getAddress());
						mDevices.put(device.getAddress(), target);
						for (DeviceScannerListener listener : mListeners) {
							listener.onDeviceDiscovered(target);
						}
						mStorage.writeDevice(target);
						return;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (mAllowUnknowns) {
				com.jameslandrum.bluetoothsmart.SmartDevice generic = new GenericDevice(device);
				generic.newAdvertisement(data,rssi);
				mDevices.put(device.getAddress(), generic);
				for (DeviceScannerListener listener : mListeners) {
					listener.onDeviceDiscovered(generic);
				}
				mStorage.writeDevice(generic);
				return;
			}
			Log.d("MESSAGE","New unknown discovered: " + device.getAddress());
			mInvalidDevices.add(device.getAddress());
		}

	}

	/**
	 * Attaches persistent storage to this scanner. This will clear all known devices.
	 * @param storage The storage system to attach.
	 */
	public void attachStorage(DevicePersistentStorage storage) {
		mStorage = storage;
		mDevices.clear();
		for (com.jameslandrum.bluetoothsmart.SmartDevice device : mStorage.getAllDevices()) {
			mDevices.put(device.getAddress(), device);
			for (DeviceScannerListener listener : mListeners) {
				listener.onDeviceDiscovered(device);
			}
		}
	}

	public List<com.jameslandrum.bluetoothsmart.SmartDevice> getAllDevices() {
		return Collections.unmodifiableList(new ArrayList<>(mDevices.values()));
	}

	public static boolean allowsUnknownDevices() {
		return mAllowUnknowns;
	}

	public void addScanListener(@NonNull DeviceScannerListener listener) {
		mListeners.add(listener);
	}

	public void removeScanListener(@NonNull DeviceScannerListener listener) {
		mListeners.remove(listener);
	}

	public static void setUnknownDeviceSupport(boolean enable) {
		mAllowUnknowns = enable;
	}

	public com.jameslandrum.bluetoothsmart.SmartDevice getDeviceByMacAddress(String macAddress) {
		for (com.jameslandrum.bluetoothsmart.SmartDevice device : mDevices.values()) {
			if (device.getAddress().equals(macAddress)) return device;
		}
		return null;
	};

}


