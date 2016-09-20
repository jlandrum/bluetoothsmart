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

import com.jameslandrum.bluetoothsmart.Characteristic;
import com.jameslandrum.bluetoothsmart.SmartDevice;
import com.jameslandrum.bluetoothsmart.actions.errors.NotImplementedError;

public class CharacteristicAction extends Action {
	protected Characteristic mCharacteristic;
	protected BluetoothGatt mGatt;
	private final Object mHolder = new Object();
	private ActionError mError;

	public CharacteristicAction(Characteristic characteristic) {
		super();
		mCharacteristic = characteristic;
	}

	@Override
	public ActionError execute(SmartDevice smartDevice) {
		super.execute(smartDevice);
		if (smartDevice.getGatt() == null || !smartDevice.isConnected()) {
			return new SmartDevice.NotConnectedError();
		}

		mGatt = smartDevice.getGatt();

		if (!mCharacteristic.ready()) {
			if (!smartDevice.updateCharacteristic(mCharacteristic))
			return new Characteristic.CharacteristicNotFoundError();
		}

		return null;
	}

	public Characteristic getCharacteristic() {
		return mCharacteristic;
	}
}
