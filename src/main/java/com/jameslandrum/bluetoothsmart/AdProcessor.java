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

	public AdProcessor(Field f, AdValue value) {
		mField = f;
		mProcessor = Processor.forType(f.getType());
		mStart = (int) Math.floor(value.startBit()/8.0f);
		mShift = value.startBit()%8;
		mEnd = (int) Math.ceil(value.endBit()/8.0f);
		int clip = (value.endBit() - value.startBit()) % 8;
		mMask ^= (clip == 0 ? 0 : (int)Math.pow(2,clip));
	}

	public void process(Object o, byte[] advertisement) {
		byte[] filtered = Arrays.copyOfRange(advertisement, mStart, mEnd);
		filtered[filtered.length-1] &= mMask;
		BigInteger big = new BigInteger(filtered);
		big = big.shiftLeft(mShift);
		try {
			mField.setAccessible(true);
			mProcessor.process(o,mField,big);
			mField.setAccessible(false);
		} catch (Exception ignored) {}
	}

	private enum Processor {
		Integer(Integer.class) {
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
		Boolean(Boolean.class) {
			@Override
			void process(Object o, Field f, BigInteger data) throws IllegalAccessException {
				f.setBoolean(o, !(data.signum() == 0));
			}
		},

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
