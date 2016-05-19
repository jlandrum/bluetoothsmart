package com.jameslandrum.bluetoothsmart.generic;

import android.bluetooth.BluetoothDevice;

import com.jameslandrum.bluetoothsmart.SmartDevice;

/**
 * A generic device. It does not process advertisements but can be talked to.
 */
public class GenericDevice extends SmartDevice {
	public GenericDevice(BluetoothDevice device) {
		super(device);
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
}
