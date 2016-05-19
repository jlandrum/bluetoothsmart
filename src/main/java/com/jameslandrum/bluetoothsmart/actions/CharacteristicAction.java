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
