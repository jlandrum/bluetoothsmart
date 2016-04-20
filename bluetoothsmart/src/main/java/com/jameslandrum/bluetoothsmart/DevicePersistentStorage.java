package com.jameslandrum.bluetoothsmart;

import java.util.Collection;

public interface DevicePersistentStorage {
	void writeDevice(SmartDevice device);
	SmartDevice readDevice(String macAddress);
	long getDeviceCount();
	Collection<SmartDevice> getAllDevices();
}
