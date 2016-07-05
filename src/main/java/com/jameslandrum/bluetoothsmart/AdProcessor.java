package com.jameslandrum.bluetoothsmart;

import com.jameslandrum.bluetoothsmart.annotations.AdValue;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Processes advertisements based on their specifications.
 */
public class AdProcessor {
	private Field mField;
	private Processor mProcessor;
	private int mShift = 0;
	private int mStart = 0;
	private int mEnd = 0;
	private int mMask = 0xFF;
	private int mLength = 0;

	public AdProcessor(Field f, AdValue value) {
		mField = f;
		mProcessor = Processor.forType(f.getType());
		switch (value.type()) {
			case BIT:
				mStart = (int) Math.floor(value.start()/8.0f);
				mShift = value.start()%8;
				mEnd = (int) Math.ceil(value.end()/8.0f);
				int clip = (value.end() - value.start()) % 8;
				mMask = filter(clip);
				mLength = (int) Math.ceil((value.end() - value.start()) / 8.0f);
				break;
			case BYTE:
				mStart = value.start();
				mEnd = value.end();
				mLength = value.end() - value.start();
				break;
		}
	}

	byte[] data;
	public void process(Object o, byte[] advertisement) {
		if (mProcessor == null) return;
		data = advertisement;
		byte[] chunk = Arrays.copyOfRange(advertisement, mStart, mEnd);
		BigInteger big = new BigInteger(chunk);
		big = big.shiftLeft(mShift);
		int values = Math.max(1,(int) Math.ceil((float)big.bitLength()/8.0f));
		byte[] shifted = big.toByteArray();
		if (shifted.length > values) shifted = Arrays.copyOfRange(shifted, 1, shifted.length);
		shifted[shifted.length-1] &= mMask;
		big = new BigInteger(Arrays.copyOfRange(shifted,0,mLength));
		try {
			mField.setAccessible(true);
			mProcessor.process(o,mField,big);
			mField.setAccessible(false);
		} catch (Exception ignored) {}
	}

	private int filter(int len) {
		switch (len) {
			case 1: return 0b10000000;
			case 2: return 0b11000000;
			case 3: return 0b11100000;
			case 4: return 0b11110000;
			case 5: return 0b11111000;
			case 6: return 0b11111100;
			case 7: return 0b11111110;
			default: return 0b00000000;
		}
	}
	private enum Processor {
		Integer(int.class) {
			@Override
			void process(Object o, Field f, BigInteger data) throws IllegalAccessException {
				f.setInt(o, data.intValue());
			}
		},
		String(String.class) {
			@Override
			void process(Object o, Field f, BigInteger data) throws IllegalAccessException {
				f.set(o, new String(data.toByteArray()));
			}
		},
		Boolean(boolean.class) {
			@Override
			void process(Object o, Field f, BigInteger data) throws IllegalAccessException {
				f.setBoolean(o, !(data.signum() == 0));
			}
		}
		;

		private Class<?> mType;

		Processor(Class<?> type) {
			mType = type;
		}

		public static Processor forType(Class<?> type) {
			for (Processor p : values()) {
				if (p.mType == type) return p;
			}
			return null;
		}

		abstract void process(Object o, Field f, BigInteger data) throws IllegalAccessException;
	}
}
