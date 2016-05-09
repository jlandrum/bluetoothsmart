package com.jameslandrum.bluetoothsmart.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.jameslandrum.bluetoothsmart.Characteristic;
import com.jameslandrum.bluetoothsmart.SmartDevice;

public class ReadCharacteristic extends CharacteristicAction implements SmartDevice.GattListener {
	private Characteristic mCharacteristic;
	private ActionError mHolder;
	private ActionError mError;

	public ReadCharacteristic(Characteristic characteristic) {
		super(characteristic);
		mCharacteristic = characteristic;
	}

	@Override
	public ActionError execute(SmartDevice smartDevice) {
		mError = super.execute(smartDevice);
		if (mError != null) return mError;

		smartDevice.addGattListener(this);
		mGatt.readCharacteristic(mCharacteristic.getCharacteristic());
		try { mHolder.wait(); } catch (InterruptedException e) { e.printStackTrace(); }
		smartDevice.removeGattListener(this);

		return mError;
	}

	public Characteristic getCharacteristic() {
		return mCharacteristic;
	}


	@Override
	public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status) {
		if (status != BluetoothGatt.GATT_FAILURE) {
			mHolder = new CharacteristicReadError();
		}
		mHolder.notify();
	}

	/* Unused */
	@Override public void onDescriptorWrite(BluetoothGattDescriptor descriptor, int status) {}
	@Override public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {}
	private class CharacteristicReadError implements ActionError {}
}
