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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ReadCharacteristic extends CharacteristicAction implements SmartDevice.GattListener {
	private Characteristic mCharacteristic;
	private final Object mHolder = new Object();
	private ActionError mError;
	private HashSet<OnReadCharacteristic> mListeners = new HashSet<>();

	public ReadCharacteristic(Characteristic characteristic) {
		super(characteristic);
		mCharacteristic = characteristic;
	}

	@Override
	public ActionError execute(SmartDevice smartDevice) {
		mError = super.execute(smartDevice);
		if (mError != null) return mError;
		if (mCharacteristic.getCharacteristic() == null) {
			mError = new CharacteristicReadError();
		} else {
			smartDevice.addGattListener(this);
			mGatt.readCharacteristic(mCharacteristic.getCharacteristic());
			try {
				synchronized (mHolder) {
					mHolder.wait(5000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			smartDevice.removeGattListener(this);
		}

		if (mCompleteListener != null)
			mCompleteListener.onActionCompleted(this, mError==null);
		return mError;
	}

	public Characteristic getCharacteristic() {
		return mCharacteristic;
	}

	@Override
	public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status) {

		if (status == BluetoothGatt.GATT_FAILURE) {
			mError = new CharacteristicReadError();
		} else {
			mCharacteristic.setCharacteristic(characteristic);
			for (OnReadCharacteristic r : mListeners) {
				r.onCharacteristicRead(mCharacteristic);
			}
		}
		synchronized (mHolder) {
			mHolder.notify();
		}
	}

	@Override
	public String toString() {
		return "Reading Characteristic " + mCharacteristic.getCharacteristicLabel();
	}

	public void addChangeListener(OnReadCharacteristic listener) {
		mListeners.add(listener);
	}

	public void removeChangeListener(OnReadCharacteristic listener) {
		mListeners.remove(listener);
	}

	public interface OnReadCharacteristic {
		void onCharacteristicRead(Characteristic c);
	}

	/* Unused */
	@Override public void onDescriptorWrite(BluetoothGattDescriptor descriptor, int status) {}
	@Override public void onCharacteristicNotify(Characteristic characteristic) {}
	@Override public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {}
	private class CharacteristicReadError implements ActionError {}
}
