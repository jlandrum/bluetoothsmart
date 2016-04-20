package com.jameslandrum.bluetoothsmart.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

/**
 * Scanner for API 21 and above.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopDeviceScanner extends DeviceScanner {
	public BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
	public BluetoothLeScanner mScanner = mAdapter.getBluetoothLeScanner();

	@Override
	public void startScan(@ScanMode int scanMode) {
		ScanSettings.Builder settings = new ScanSettings.Builder();
		settings.setScanMode(scanMode);
		mScanner.startScan(null, settings.build(), callback);
	}

	@Override
	public void stopScan(@ScanMode int scanMode) {
		mScanner.stopScan(callback);
	}

	private ScanCallback callback = new ScanCallback() {
		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			if (result.getScanRecord() != null) {
				processAdvertisement(result.getScanRecord().getBytes(), result.getDevice());
			}
		}
	};
}
