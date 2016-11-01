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

package com.jameslandrum.bluetoothsmart.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.jameslandrum.bluetoothsmart.Characteristic;
import com.jameslandrum.bluetoothsmart.SmartDevice;
import com.jameslandrum.bluetoothsmart.actions.errors.RegisterForNotificationError;

import java.util.UUID;

public class RegisterCharacteristic extends CharacteristicAction implements SmartDevice.GattListener, SmartDevice.UpdateListener {
	private final Object mHolder = new Object();
	private ActionError mError;
	private boolean mEnable;
	private CharacteristicNotifyListener mListener;

	protected static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID =
			UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	public RegisterCharacteristic(Characteristic characteristic, CharacteristicNotifyListener listener, boolean enable) {
		super(characteristic);
		mEnable = enable;
		mCharacteristic = characteristic;
		mListener = listener;
	}

	@Override
	public ActionError execute(SmartDevice smartDevice) {
		mError = super.execute(smartDevice);
		if (mError != null) return mError;

		BluetoothGattDescriptor descriptor = mCharacteristic.getCharacteristic()
				.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
		if (descriptor == null) return new RegisterForNotificationError();

		mError = mGatt.setCharacteristicNotification(mCharacteristic.getCharacteristic(),mEnable) ?
				null : new RegisterForNotificationError();
		if (mError != null) return mError;

		smartDevice.addGattListener(this);
		smartDevice.addOnUpdateListener(this);
		descriptor.setValue(mEnable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
				BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
		mGatt.writeDescriptor(descriptor);
		try { synchronized (mHolder) { mHolder.wait(500); } } catch (InterruptedException e) { e.printStackTrace(); }

		if (mError!=null) {
			smartDevice.removeGattListener(this);
			smartDevice.removeOnUpdateListener(this);
			mListener = null;
		}

		mCompleteListener = null;
		return mError;
	}

	public Characteristic getCharacteristic() {
		return mCharacteristic;
	}

	@Override
	public void onDescriptorWrite(BluetoothGattDescriptor descriptor, int status) {
		if (status == BluetoothGatt.GATT_FAILURE) {
			mError = new RegisterForNotificationError();
		}
		synchronized (mHolder) {
			mHolder.notify();
		}
	}

	@Override
	public String toString() {
		return "Registering to Characteristic " + mCharacteristic.getCharacteristicLabel();
	}

	@Override public void onCharacteristicNotify(Characteristic characteristic) {
		if (mListener!=null) mListener.onCharacteristicNotify(characteristic);
	}

	@Override
	public void onEvent(SmartDevice.UpdateEvent event, Object device) {
		if (event == SmartDevice.UpdateEvent.DISCONNECT) {
			mDevice.removeGattListener(this);
			mDevice.removeOnUpdateListener(this);
			mListener = null;
		}
	}

	public interface CharacteristicNotifyListener {
		void onCharacteristicNotify(Characteristic c);
	}

	/* unused */
	@Override public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {}
	@Override public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status) {}
}

