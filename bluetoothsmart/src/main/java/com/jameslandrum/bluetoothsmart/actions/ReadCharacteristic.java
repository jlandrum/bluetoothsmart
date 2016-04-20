package com.jameslandrum.bluetoothsmart.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.jameslandrum.bluetoothsmart.Characteristic;
import com.jameslandrum.bluetoothsmart.SmartDevice;

public class ReadCharacteristic extends Action implements SmartDevice.GattListener {
	private Characteristic mCharacteristic;
	private ActionError mHolder;

	public ReadCharacteristic(Characteristic characteristic, int id, int group) {
		super(id, group);
		mCharacteristic = characteristic;
	}

	@Override
	public ActionError execute(SmartDevice smartDevice) {
		super.execute(smartDevice);
		if (smartDevice.getGatt() == null || !smartDevice.isConnected()) {
			return new SmartDevice.NotConnectedError();
		}

		BluetoothGatt mGatt = smartDevice.getGatt();

		if (!mCharacteristic.ready()) {
			try {
				mCharacteristic.setCharacteristic(
				mGatt.getService(mCharacteristic.getServiceId())
						.getCharacteristic(mCharacteristic.getCharacteristicId()));
			} catch (Exception e) {
				e.printStackTrace();
				return new Characteristic.CharacteristicNotFoundError();
			}
		}

		BluetoothGattCharacteristic characteristic = mCharacteristic.getCharacteristic();

		smartDevice.addGattListener(this);
		mGatt.readCharacteristic(characteristic);
		try { mHolder.wait(); } catch (InterruptedException e) { e.printStackTrace(); }
		smartDevice.removeGattListener(this);

		return null;
	}

	public Characteristic getCharacteristic() {
		return mCharacteristic;
	}

	@Override
	public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {}

	@Override
	public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status) {
		if (status != BluetoothGatt.GATT_FAILURE) {
			mHolder = new CharacteristicReadError();
		}
		mHolder.notify();
	}

	private class CharacteristicReadError implements ActionError {}
}
