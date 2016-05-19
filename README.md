# BluetoothSmart
BluetoothSmart is a library that allows a more objective approach to communicating with Bluetooth Smart devices.

Currently, this project is configured to push to a local Maven repository configured at /Library/Maven but can also be included as a module project instead.

# Use
In order to begin, a device must be created that extends the SmartDevice object:
```java
@SmartDeviceDef
public class MyDevice extends SmartDevice {
}
```

In order to filter the device out, the class must also provide an identifier:
```java
@Identifier
public static boolean identify(byte[] data) {
	return Arrays.equals(Arrays.copyOfRange(data, 9, 11), MY_MANUFACTURER_ID);
}
```

To scan for devices, use the DeviceScanner instance:

```java
DeviceScanner scanner = DeviceScanner.getInstance();
scanner.addDeviceType(MyDevice.class);
if (BluetoothAdapter.getDefaultAdapter.isEnabled())
	scanner.startScan(DeviceScanner.SCAN_MODE_LOW_LATENCY);
```

SmartDevice objects can now contain specially annotated variables that can infer data from advertisements:
```java
@AdInteger(start=20) private int mBatteryLevel;
@CharDef(service = SERVICE_UUID, id = CHARACTERISTIC_UUID) public Characteristic BATTERY_LEVEL;
```

JavaDoc will soon be provided as well as some samples to work with common BLE devices.

