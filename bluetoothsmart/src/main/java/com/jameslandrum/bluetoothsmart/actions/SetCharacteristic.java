package com.jameslandrum.bluetoothsmart.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.jameslandrum.bluetoothsmart.Characteristic;
import com.jameslandrum.bluetoothsmart.SmartDevice;

public class SetCharacteristic extends Action implements SmartDevice.GattListener {
	private Characteristic mCharacteristic;
	private byte[] mData;
	private final Object mHolder = new Object();
	private ActionError mError;

	public SetCharacteristic(Characteristic characteristic, byte[] data, int id, int group) {
		super(id, group);
		mCharacteristic = characteristic;
		mData = data;
	}

	public void refresh(byte[] newValue) {
		mData = newValue;
		super.refresh();
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
		characteristic.setValue(mData);

		smartDevice.addGattListener(this);
		mGatt.writeCharacteristic(characteristic);
		try { synchronized (mHolder) { mHolder.wait(3000); } } catch (InterruptedException e) { e.printStackTrace(); }
		smartDevice.removeGattListener(this);

		return mError;
	}

	public Characteristic getCharacteristic() {
		return mCharacteristic;
	}

	@Override
	public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {
		if (status == BluetoothGatt.GATT_FAILURE) {
			mError = new CharacteristicWriteError();
		}
		synchronized (mHolder) {
			mHolder.notify();
		}
	}

	@Override
	public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status) {

	}

	private class CharacteristicWriteError implements ActionError {}
}
