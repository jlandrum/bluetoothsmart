package com.jameslandrum.bluetoothsmart;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.jameslandrum.bluetoothsmart.actions.Action;

import java.util.ArrayList;
import java.util.UUID;

public class Characteristic {
	protected BluetoothGattCharacteristic mCharacteristic;
	protected UUID mCharId;
	protected UUID mServiceId;
	private String mLabel;
	private ArrayList<CharacteristicChangeListener> mListeners = new ArrayList<>();

	protected Characteristic(String s, String c) {
		mCharId = UUIDfromString(c);
		mServiceId = UUIDfromString(s);
		mLabel = s+" : "+c;
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

	public void setCharacteristicValue(byte[] value) {
		setCharacteristicValue(value,false);
	}

	public void setCharacteristicValue(byte[] value, boolean notify) {
		mCharacteristic.setValue(value);
		if (notify) {
			for (CharacteristicChangeListener listener : mListeners) {
				listener.onCharacteristicChanged(this);
			}
		}
	}

	public byte[] getValue() {
		return mCharacteristic.getValue();
	}

	public void setCharacteristicLabel(String label) {
		if (label != null) mLabel = label;
	}

	public String getCharacteristicLabel() {
		return mLabel;
	}

	public BluetoothGattCharacteristic getCharacteristic() {
		return mCharacteristic;
	}

	public void addCharacteristicChangeListener(CharacteristicChangeListener listener) {
		mListeners.add(listener);
	}

	public void removeCharacteristicChangeListener(CharacteristicChangeListener listener) {
		mListeners.remove(listener);
	}

	public static class CharacteristicNotFoundError implements Action.ActionError {
	}

	public static interface CharacteristicChangeListener {
		public void onCharacteristicChanged(Characteristic characteristic);
	}
}
