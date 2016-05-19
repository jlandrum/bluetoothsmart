package com.jameslandrum.bluetoothsmart.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

/**
 * Scanner for API 19
 */
public class KitKatDeviceScanner extends DeviceScanner implements BluetoothAdapter.LeScanCallback {
	public BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

	KitKatDeviceScanner() {
		super();
	}

	@Override
	public void startScan(@ScanMode int scanMode) {
		if (mAdapter.isEnabled()) mAdapter.startLeScan(this);
	}

	@Override
	public void stopScan() {
		if (mAdapter.isEnabled()) mAdapter.stopLeScan(this);
	}

	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		processAdvertisement(scanRecord, device);
	}
}
