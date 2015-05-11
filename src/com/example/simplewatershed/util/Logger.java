package com.example.simplewatershed.util;

import android.util.Log;

public class Logger {

	public static void d(Class<?> c, String message) {
		String className = parserClassName(c);
		if (Util.LOG) {
			Log.d("StickMaker" + ":" + className, message);
		}
	}

	public static void d(String tag, Class<?> c, String message) {
		String className = parserClassName(c);
		Log.d(tag + ":" + className, message);
	}

	public static void e(String tag, Class<?> c, String message) {
		String className = parserClassName(c);
		Log.e(tag + ":" + className, message);
	}

	public static void i(String tag, Class<?> c, String message) {
		String className = parserClassName(c);
		Log.i(tag + ":" + className, message);
	}

	public static void v(String tag, Class<?> c, String message) {
		String className = parserClassName(c);
		Log.v(tag + ":" + className, message);
	}

	public static void w(String tag, Class<?> c, String message) {
		String className = parserClassName(c);
		Log.w(tag + ":" + className, message);
	}

	private static String parserClassName(Class<?> c) {
		int dotLastIndex = c.getName().lastIndexOf('.');
		int moneyIndex = c.getName().lastIndexOf('$');
		if(moneyIndex != -1) {
			return c.getName().substring(++dotLastIndex, moneyIndex--);
		} else {
			return c.getName().substring(++dotLastIndex);
		}
	}
}
