package com.jameslandrum.bluetoothsmart;

import android.bluetooth.BluetoothGattCharacteristic;

import com.jameslandrum.bluetoothsmart.actions.Action;

import java.util.UUID;

public class Characteristic {
	protected BluetoothGattCharacteristic mCharacteristic;
	protected UUID mCharId;
	protected UUID mServiceId;

	public Characteristic(String s, String c) {
		mCharId = UUIDfromString(c);
		mServiceId = UUIDfromString(s);
	}

	private UUID UUIDfromString(String s) {
		if (s.length() == 4) {
			return UUID.fromString("0000" + s + "-0000-1000-8000-00805F9B34FB");
		} else return UUID.fromString(s);
	}

	public boolean ready() {
		return mCharacteristic != null;
	}

	public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
		mCharacteristic = characteristic;
	}

	public UUID getServiceId() {
		return mServiceId;
	}

	public UUID getCharacteristicId() {
		return mCharId;
	}

	public BluetoothGattCharacteristic getCharacteristic() {
		return mCharacteristic;
	}

	public static class CharacteristicNotFoundError implements Action.ActionError {
	}
}
