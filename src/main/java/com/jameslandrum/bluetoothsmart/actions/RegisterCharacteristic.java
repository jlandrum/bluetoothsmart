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
		mListener = mListener;
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
	public void onDisconnect() {
		mDevice.removeGattListener(this);
		mDevice.removeOnUpdateListener(this);
		mListener = null;
	}

	public interface CharacteristicNotifyListener {
		void onCharacteristicNotify(Characteristic c);
	}

	/* unused */
	@Override public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {}
	@Override public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status) {}
	@Override public void onUpdate(Object device) {}
	@Override public void onConnect() {}
}

