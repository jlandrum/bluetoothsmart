package com.jameslandrum.bluetoothsmart.scanner;

import com.jameslandrum.bluetoothsmart.SmartDevice;

/**
 * Callback for Device Scanning
 */
public interface DeviceScannerListener {
	void onDeviceDiscovered(SmartDevice smartDevice);
	void onDeviceUpdated(SmartDevice smartDevice);
	void onDevicePinged(SmartDevice target);
}
