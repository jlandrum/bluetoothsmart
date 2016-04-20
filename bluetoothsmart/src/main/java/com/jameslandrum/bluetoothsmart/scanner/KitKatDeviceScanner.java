package com.jameslandrum.bluetoothsmart.scanner;

import android.bluetooth.BluetoothAdapter;

/**
 * Scanner for API 19
 */
public class KitKatDeviceScanner extends DeviceScanner {
	public BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

	KitKatDeviceScanner() {
		super();
	}

	@Override
	public void startScan(@ScanMode int scanMode) {

	}

	@Override
	public void stopScan(@ScanMode int scanMode) {

	}
}
