package com.jameslandrum.bluetoothsmart.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.jameslandrum.bluetoothsmart.Characteristic;
import com.jameslandrum.bluetoothsmart.SmartDevice;
import com.jameslandrum.bluetoothsmart.actions.errors.CharacteristicWriteError;

import org.apache.commons.codec.binary.Hex;

public class SetCharacteristic extends CharacteristicAction implements SmartDevice.GattListener {
	private Characteristic mCharacteristic;
	private byte[] mData;
	private final Object mHolder = new Object();
	private ActionError mError;

	public SetCharacteristic(Characteristic characteristic, byte[] data) {
		super(characteristic);
		mCharacteristic = characteristic;
		mData = data;
	}

	public void refresh(byte[] newValue) {
		mData = newValue;
		super.refresh();
	}

	@Override
	public ActionError execute(SmartDevice smartDevice) {
		mError = super.execute(smartDevice);
		if (mError != null) return mError;

		BluetoothGattCharacteristic characteristic = mCharacteristic.getCharacteristic();
		characteristic.setValue(mData);

		smartDevice.addGattListener(this);
		mGatt.writeCharacteristic(characteristic);
		try { synchronized (mHolder) { mHolder.wait(300); } } catch (InterruptedException e) {
			mError = new CharacteristicWriteError();
		}
		smartDevice.removeGattListener(this);

		if (mCompleteListener != null)
			mCompleteListener.onActionCompleted(this, mError==null);

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

	public void enableRepeat(Boolean enable) {
		setRepeating(enable);
	}

	@Override
	public String toString() {
		return "Setting Characteristic " + mCharacteristic.getCharacteristicLabel() + " to " + new String(Hex.encodeHex(mData));
	}

	/* Unused */
	@Override public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status) {}
	@Override public void onDescriptorWrite(BluetoothGattDescriptor descriptor, int status) {}
	@Override public void onCharacteristicNotify(Characteristic characteristic) {}

}
