package com.jameslandrum.bluetoothsmart.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.jameslandrum.bluetoothsmart.Characteristic;
import com.jameslandrum.bluetoothsmart.SmartDevice;
import com.jameslandrum.bluetoothsmart.actions.errors.CharacteristicWriteError;

import org.apache.commons.codec.binary.Hex;

import java.io.InputStream;
import java.util.Arrays;

public class LargeSetCharacteristic extends CharacteristicAction implements SmartDevice.GattListener {
	private Characteristic mCharacteristic;
	private final Object mHolder = new Object();
	private ActionError mError;
	private byte[] mData;
	private int mSize;

	public LargeSetCharacteristic(Characteristic characteristic, byte[] data, int size) {
		super(characteristic);
		mCharacteristic = characteristic;
		mData = data;
		mSize = size;
	}

	@Override
	public ActionError execute(SmartDevice smartDevice) {
		mError = super.execute(smartDevice);
		if (mError != null) return mError;
		int index = 0;

		BluetoothGattCharacteristic characteristic = mCharacteristic.getCharacteristic();
		characteristic.setValue(mData);

		smartDevice.addGattListener(this);
		try {
			while (index < mData.length) {
				if (!mDevice.isConnected()) {
					mError = new CharacteristicWriteError();
					return mError;
				}
				int end = Math.min(index+mSize, mData.length);
				characteristic.setValue(Arrays.copyOfRange(mData,index,end));
				mGatt.writeCharacteristic(characteristic);
				Log.d("ActionRunner","Setting " + (end-index) + " bytes.");
				synchronized (mHolder) {
					mHolder.wait(300);
				}
				index+=mSize;
			}
		} catch (Exception e) {
			mError = new CharacteristicWriteError();
		}
		smartDevice.removeGattListener(this);

		if (mCompleteListener != null)
			mCompleteListener.onActionCompleted(this, mError!=null);

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
		return "Writing to Characteristic " + mCharacteristic.getCharacteristicLabel();
	}

	/* Unused */
	@Override public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status) {}
	@Override public void onDescriptorWrite(BluetoothGattDescriptor descriptor, int status) {}
	@Override public void onCharacteristicNotify(Characteristic characteristic) {}

}
