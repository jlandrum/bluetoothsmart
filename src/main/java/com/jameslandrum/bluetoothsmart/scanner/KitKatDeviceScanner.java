package com.jameslandrum.bluetoothsmart.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

/**
 * Scanner for API 19
 */
public class KitKatDeviceScanner extends DeviceScanner implements BluetoothAdapter.LeScanCallback {
	private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
	private boolean mIsScanning;

	KitKatDeviceScanner() {
		super();
	}

	@Override
	public void startScan(@ScanMode int scanMode) {
		if (!mAdapter.isEnabled()) return;
		mAdapter.startLeScan(this);
		mIsScanning = true;
	}

	@Override
	public void stopScan() {
		if (!mAdapter.isEnabled()) return;
		mAdapter.stopLeScan(this);
		mIsScanning = false;
	}

	@Override
	public boolean isScanning() {
		return mAdapter.isEnabled() && mIsScanning;
	}

	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		processAdvertisement(scanRecord, device, rssi);
	}
}
