package com.jameslandrum.bluetoothsmart.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

import com.jameslandrum.bluetoothsmart.SmartDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Scanner for API 21 and above.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopDeviceScanner extends DeviceScanner {
	public BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
	public BluetoothLeScanner mScanner;

	@Override
	public void startScan(@ScanMode int scanMode) {
		if (DeviceScanner.getConnectedDevices() > 0) return;
		mScanMode = scanMode;
		if (mScanner == null) mScanner = mAdapter.getBluetoothLeScanner();
		if (mAdapter.isEnabled() && mScanner != null) {
			ScanSettings.Builder settings = new ScanSettings.Builder();
			settings.setScanMode(scanMode);
			mScanner.startScan(null, settings.build(), callback);
		}
	}

	@Override
	public void stopScan() {
		if (mScanner != null && mAdapter.isEnabled()) {
			mScanner.stopScan(callback);
		}
	}

	private ScanCallback callback = new ScanCallback() {
		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			if (result.getScanRecord() != null) {
				processAdvertisement(result.getScanRecord().getBytes(), result.getDevice(), result.getRssi());
			}
		}
	};
}
