/**
 * Copyright (C) 2018 Jacob Scott <jascottytechie@gmail.com> Description:
 * Provides methods for parsing and encoding json
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package me.jascotty2.libv3.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtils {

	public final static double JAVA_VERSION = getVersion();

	private static double getVersion() {
		String version = System.getProperty("java.version");
		int i = version.indexOf('.');
		if (i != -1 && (i = version.indexOf('.', i + 1)) != -1) {
			return Double.parseDouble(version.substring(0, i));
		}
		return Double.NaN;
	}

	public static String getStackStr(Exception err) {
		if (err == null) {// || err.getCause() == null) {
			return "";
		}
		ByteArrayOutputStream stackoutstream = new ByteArrayOutputStream();
		PrintWriter stackstream = new PrintWriter(stackoutstream);
		err.printStackTrace(stackstream);
		stackstream.flush();
		stackstream.close();
		return stackoutstream.toString();
	}

	public static String getStackStr(Throwable err) {
		if (err == null) {
			return "";
		}
		ByteArrayOutputStream stackoutstream = new ByteArrayOutputStream();
		PrintWriter stackstream = new PrintWriter(stackoutstream);
		err.printStackTrace(stackstream);
		stackstream.flush();
		stackstream.close();
		return stackoutstream.toString();
	}

	// does not work in JRE 8+
	private static Method getStackTraceElementMethod;
	private static Method getStackTraceDepthMethod;

	static {
		try {
			getStackTraceElementMethod = Throwable.class.getDeclaredMethod("getStackTraceElement", int.class);
			getStackTraceElementMethod.setAccessible(true);
			getStackTraceDepthMethod = Throwable.class.getDeclaredMethod("getStackTraceDepth");
			getStackTraceDepthMethod.setAccessible(true);
		} catch (Exception ex) {
			getStackTraceElementMethod = getStackTraceDepthMethod = null;
		}
	}

	/**
	 * If you only need one stack trace element this is faster than
	 * Throwable.getStackTrace()[element], it doesn't generate the full stack
	 * trace.
	 */
	public static StackTraceElement getStackTraceElement(int index) {
		try {
			Throwable dummy = new Throwable();

			if (JAVA_VERSION >= 8 && JAVA_VERSION < 9) {
				return sun.misc.SharedSecrets.getJavaLangAccess().getStackTraceElement(dummy, index);
//			} else if (JAVA_VERSION >= 9) {
//				return StackWalker.getInstance(Collections.emptySet(), index + 1)
//				.walk(s -> s.skip(index).findFirst())
//				.orElse(null);
			} else if (getStackTraceElementMethod == null) {
				// better than nothing, right? :/
				return (new Throwable()).getStackTrace()[index];
			} else {
				if (index < (Integer) getStackTraceDepthMethod.invoke(dummy)) {
					return (StackTraceElement) getStackTraceElementMethod.invoke(new Throwable(), index);
				} else {
					return null;
				}
			}
		} catch (Throwable t) {
		}
		return null;
	}

	public static void setPrivateField(Class<?> c, Object handle, String fieldName, Object value) throws Exception {
		Field f = c.getDeclaredField(fieldName);
		f.setAccessible(true);
		f.set(handle, value);
	}

	public static Object getPrivateField(Class<?> c, Object handle, String fieldName) throws Exception {
		Field field = c.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(handle);
	}
//
//	public static Object invokePrivateMethod(Class<?> c, String methodName, Object handle, Object... parameters) throws Exception {
//		Class[] types = new Class[parameters.length];
//		for (int i = 0; i < types.length; ++i) {
//			types[i] = parameters[i].getClass();
//		}
//		Method m = c.getDeclaredMethod(methodName, types);
//		m.setAccessible(true);
//		return m.invoke(handle, parameters);
//	}

	public static Object invokePrivateMethod(Class<?> c, String methodName, Object handle, Class[] types, Object[] parameters) throws Exception {
		Method m = c.getDeclaredMethod(methodName, types);
		m.setAccessible(true);
		return m.invoke(handle, parameters);
	}

}
