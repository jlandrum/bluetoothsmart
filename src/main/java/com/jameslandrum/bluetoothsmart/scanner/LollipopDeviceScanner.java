/**
 * Copyright 2016 James Landrum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
	private boolean mIsScanning;

	@Override
	public void startScan(@ScanMode int scanMode) {
		mScanMode = scanMode;
		if (mScanner == null) mScanner = mAdapter.getBluetoothLeScanner();
		if (mAdapter.isEnabled() && mScanner != null) {
			ScanSettings.Builder settings = new ScanSettings.Builder();
			Log.d("LollipopDeviceScanner", "Setting scan mode to " + scanMode);
			settings.setScanMode(scanMode);
			mScanner.startScan(null, settings.build(), callback);
			mIsScanning = true;
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
			mIsScanning = false;
			mProcessorThread.interrupt();
		}
	}

	@Override
	public boolean isScanning() {
		return mIsScanning;
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
