package com.jameslandrum.bluetoothsmart.throwable;

public class DeviceIdentifierMissingError extends Error {
	public DeviceIdentifierMissingError() {
		super("Device class must override getDeviceIdentifier");
	}
}
