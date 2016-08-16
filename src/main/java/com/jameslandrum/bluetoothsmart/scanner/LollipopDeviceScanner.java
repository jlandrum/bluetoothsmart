package com.jameslandrum.bluetoothsmart.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Scanner for API 21 and above.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopDeviceScanner extends DeviceScanner {
	public BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
	public BluetoothLeScanner mScanner;
	private Thread mProcessorThread;
	private final Object mLock = new Object();
	private ConcurrentSkipListMap<String, ScanResult> mAdsToProcess = new ConcurrentSkipListMap<>();

	@Override
	public void startScan(@ScanMode int scanMode) {
		mScanMode = scanMode;
		if (mScanner == null) mScanner = mAdapter.getBluetoothLeScanner();
		if (mAdapter.isEnabled() && mScanner != null) {
			ScanSettings.Builder settings = new ScanSettings.Builder();
			Log.d("LollipopDeviceScanner", "Setting scan mode to " + scanMode);
			settings.setScanMode(scanMode);
			mScanner.startScan(null, settings.build(), callback);
		}
		mProcessorThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!mProcessorThread.isInterrupted()) {
					if (mAdsToProcess.size() == 0) try {
						synchronized (mLock) {
							Log.i("Processing Ad", "Waiting for Ad");
							mLock.wait();
						}
					} catch (InterruptedException ignored) {
						break;
					}

					try {
						ScanResult result = mAdsToProcess.firstEntry().getValue();
						mAdsToProcess.remove(mAdsToProcess.firstEntry().getKey());
						if (result.getScanRecord() != null) {
							Log.i("Processing Ad", "Processing Advertisement for Device " + result.getDevice().getAddress());
							processAdvertisement(result.getScanRecord().getBytes(), result.getDevice(), result.getRssi());
						}
					} catch (Exception ignored) {}
				}
			}
		});
		mProcessorThread.start();
	}

	@Override
	public void stopScan() {
		if (mScanner != null && mAdapter.isEnabled()) {
			mScanner.stopScan(callback);
			mProcessorThread.interrupt();
		}
	}

	private ScanCallback callback = new ScanCallback() {
		@Override
		public void onScanResult(int callbackType, final ScanResult result) {
			if (result.getScanRecord() != null) {
				if (mAdsToProcess.containsKey(result.getDevice().getAddress())) {
					mAdsToProcess.remove(result.getDevice().getAddress());
				}
				mAdsToProcess.put(result.getDevice().getAddress(), result);
				synchronized (mLock) {
					mLock.notify();
				}
			}
		}
	};
}
