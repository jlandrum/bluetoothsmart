package com.jameslandrum.bluetoothsmart;

import com.jameslandrum.bluetoothsmart.annotations.AdCompatible;
import com.jameslandrum.bluetoothsmart.annotations.AdValue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Processes advertisements based on their specifications.
 */
public class AdProcessor {
	private AdValue mSource;
	private Field mField;
	private Processor mProcessor;

	private int mStart = 0;
	private int mEnd = 0;
	private int mByteStart = 0;
	private int mByteEnd = 0;
	private int mByteLength = 0;
	private int mShift = 0;
	private int mClip = 0;

	public AdProcessor(Field f, AdValue value) {
		mSource = value;
		mField = f;
		mProcessor = Processor.forType(f.getType());
		switch (value.type()) {
			case BIT:
				mStart = value.start();
				mEnd = value.end();
				mByteStart = (int) Math.floor(mStart/8.0f);
				mByteEnd =  (int) Math.ceil(mEnd/8.0f);
				mByteLength = (int) Math.ceil((mEnd - mStart) / 8.0f);
				mShift = (8 - mEnd % 8) % 8;
				mClip = (mStart % 8 + mShift) % 8;
				break;
			case BYTE:
				mStart = value.start() * 8;
				mEnd = value.end() * 8;
				mByteStart = value.start();
				mByteEnd = value.end();
				mByteLength = mByteEnd - mByteStart;
  				break;
		}
	}

	void process(Object o, byte[] advertisement) {
		if (mProcessor == null) return;

		BigInteger bytes = new BigInteger(Arrays.copyOfRange(advertisement, mByteStart, mByteEnd));
		bytes = bytes.shiftRight(mShift);
		byte[] data = bytes.toByteArray();

		if (data.length < mByteLength) {
			data = Arrays.copyOf(data, mByteLength);
		}

		data = Arrays.copyOfRange(data, data.length-mByteLength, data.length);
		if (mClip > 0) data[0] &= 0b11111111 >> mClip;

		if (mSource.flip()) {
			for (int i = 0; i < data.length / 2; i++) {
				byte t = data[i];
				data[i] = data[data.length-i-1];
				data[data.length-i-1] = t;
			}
		}

		try {
			mField.setAccessible(true);
			mProcessor.process(o,this,data);
			mField.setAccessible(false);
		} catch (Exception ignored) {
			ignored.printStackTrace();
		}
	}

	private enum Processor {
		Integer(int.class) {
			@Override
			void process(Object o, AdProcessor f, byte[] data) throws IllegalAccessException {
				int val = new BigInteger(data).intValue();
				if (!f.mSource.signed() && val < 0) val = (int) ((long)val & 0xFFL);
				f.mField.setInt(o, val);
			}
		},
		String(String.class) {
			@Override
			void process(Object o, AdProcessor f, byte[] data) throws IllegalAccessException {
				f.mField.set(o, new String(data));
			}
		},
		Boolean(boolean.class) {
			@Override
			void process(Object o, AdProcessor f, byte[] data) throws IllegalAccessException {
				f.mField.setBoolean(o, new BigInteger(data).intValue() > 0);
			}
		},
		Compatible(AdCompatible.class) {
			@Override
			void process(Object o, AdProcessor f, byte[] data) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
				AdCompatible.class.getMethod("set", byte[].class).invoke(f.mField.get(o), data);
			}
		}
		;

		private Class<?> mType;

		Processor(Class<?> type) {
			mType = type;
		}

		public static Processor forType(Class<?> type) {
			for (Processor p : values()) {
				if (p.mType.isAssignableFrom(type)) return p;
			}
			return null;
		}

		abstract void process(Object o, AdProcessor p, byte[] data) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException;
	}
}
