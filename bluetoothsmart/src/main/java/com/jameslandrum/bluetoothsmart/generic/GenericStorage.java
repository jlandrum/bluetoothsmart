package com.jameslandrum.bluetoothsmart.generic;

import com.jameslandrum.bluetoothsmart.DevicePersistentStorage;
import com.jameslandrum.bluetoothsmart.SmartDevice;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A storage engine that does nothing.
 */
public class GenericStorage implements DevicePersistentStorage {
	@Override
	public void writeDevice(SmartDevice device) {}

	@Override
	public SmartDevice readDevice(String macAddress) {
		return null;
	}

	@Override
	public long getDeviceCount() { return 0; }

	@Override
	public Collection<SmartDevice> getAllDevices() {
		return new ArrayList<>();
	}
}
