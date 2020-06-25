// Copyright (c) 2006 - 2016, Markus Strauch.
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
// * Redistributions of source code must retain the above copyright notice, 
// this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright notice, 
// this list of conditions and the following disclaimer in the documentation 
// and/or other materials provided with the distribution.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
// THE POSSIBILITY OF SUCH DAMAGE.

package net.sf.sdedit.util;

import java.awt.Color;
import java.awt.Component;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class Utilities {

	/*
	 * Maps the Class representations of the primitive classes onto their wrapper
	 * classes.
	 */
	private static final Map<Class<?>, Class<?>> wrapperClasses;

	private static CL cl;

	private static final Map<String, Class<?>> primitiveClasses;

	static {
		final HashMap<Class<?>, Class<?>> wrappers = new HashMap<Class<?>, Class<?>>();
		wrappers.put(Integer.TYPE, Integer.class);
		wrappers.put(Boolean.TYPE, Boolean.class);
		wrappers.put(Character.TYPE, Character.class);
		wrappers.put(Byte.TYPE, Byte.class);
		wrappers.put(Short.TYPE, Short.class);
		wrappers.put(Long.TYPE, Long.class);
		wrappers.put(Float.TYPE, Float.class);
		wrappers.put(Double.TYPE, Double.class);
		wrapperClasses = Collections.unmodifiableMap(wrappers);
		final HashMap<String, Class<?>> primClasses = new HashMap<String, Class<?>>(8, 1.0F);
		primClasses.put("boolean", boolean.class);
		primClasses.put("byte", byte.class);
		primClasses.put("char", char.class);
		primClasses.put("short", short.class);
		primClasses.put("int", int.class);
		primClasses.put("long", long.class);
		primClasses.put("float", float.class);
		primClasses.put("double", double.class);
		primClasses.put("void", void.class);
		primitiveClasses = Collections.unmodifiableMap(primClasses);

	}

	private static final String CR = "\r";

	private static final String LF = "\n";

	private static final String CRLF = CR + LF;

	private static final Map<String, String> keys = new HashMap<String, String>();

	private Utilities() {

	}

	private static CL cl() {
		if (cl == null) {
			cl = new CL();
		}
		return cl;
	}

	public static PrintWriter createPrintWriter() {
		return PWriter.create();
	}

	public static PrintWriter createPrintWriter(File file, String encoding) throws IOException {
		OutputStream outputStream = new FileOutputStream(file);
		try {
			OutputStreamWriter osw = new OutputStreamWriter(outputStream, encoding);
			PrintWriter pw = new PrintWriter(osw);
			return pw;
		} catch (IOException e) {
			outputStream.close();
			throw e;
		}
	}

	protected static class CL extends ClassLoader {

		private HashMap<String, byte[]> classes;

		CL() {
			super(CL.class.getClassLoader());
			classes = new HashMap<String, byte[]>();
		}

		public void addClass(String name, byte[] code) {
			classes.put(name, code);
		}

		protected Class<?> findClass(String name) throws ClassNotFoundException {
			try {
				byte[] code = classes.remove(name);
				Class<?> clazz = defineClass(name, code, 0, code.length);
				return clazz;
			} catch (RuntimeException re) {
				throw re;
			} catch (Throwable t) {
				t.printStackTrace();
				throw new ClassNotFoundException(t.getMessage());
			}
		}
	}

	public static String toString(PrintWriter pw) {
		pw.flush();
		return pw.toString();
	}

	public static void erase(File file, boolean recursive) {
		if (recursive) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File _file : files) {
					erase(_file, true);
				}
			}
		}
		file.delete();
	}

	private static int cast(byte b) {
		if (b >= 0) {
			return b;
		}
		return 256 + b;
	}

	public static byte[] toByteArray(String hexString) {
		byte[] bytes = new byte[hexString.length() / 2];
		for (int i = 0; i < hexString.length(); i += 2) {
			String hex = hexString.substring(i, i + 2);
			bytes[i / 2] = (byte) Integer.parseInt(hex, 16);
		}
		return bytes;
	}

	public static String toHexString(byte[] bytes) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			String h = Integer.toHexString(cast(bytes[i]));
			if (h.length() == 1) {
				h = "0" + h;
			}
			sb.append(h);
		}
		return sb.toString();
	}

	public static String cutToSize(String string, Charset charset, int length) {
		if (string == null) {
			return null;
		}
		if (length < 1) {
			return "";
		}
		CharsetDecoder decoder = charset.newDecoder();
		CharsetEncoder encoder = charset.newEncoder();
		CharBuffer cb = CharBuffer.wrap(string.toCharArray());
		ByteBuffer bb = ByteBuffer.allocate(length);
		encoder.encode(cb, bb, false);
		bb.position(0);
		cb.clear();
		decoder.decode(bb, cb, false);
		int p = cb.position();
		char[] chars = new char[p];
		cb.position(0);
		cb.get(chars, 0, p);
		if (p == 1 && chars[0] == 0 && string.length() > 1) {
			return "";
		}
		return new String(chars);
	}

	public static String toString(Date date) {
		return toString(date, null);
	}

	public static Class<?> loadClass(String name, byte[] code) throws ClassNotFoundException {
		cl().addClass(name, code);
		return cl().loadClass(name);
	}

	public static byte[] zip(InputStream is) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(bos);
		ZipEntry ze = new ZipEntry("X");
		zos.putNextEntry(ze);
		pipe(is, zos);
		zos.closeEntry();
		zos.flush();
		zos.close();
		return bos.toByteArray();
	}

	public static byte[] unzip(byte[] bytes) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			ZipInputStream zis = new ZipInputStream(bis);
			zis.getNextEntry();
			return toByteArray(zis, -1);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Date toDate(String string, String format) {
		DateFormat dateFormat = new SimpleDateFormat(format);
		dateFormat.setLenient(true);
		Date d;
		try {
			d = dateFormat.parse(string);
		} catch (ParseException pe) {
			throw new IllegalArgumentException(pe);
		}
		return d;
	}

	public static String toString(Date date, String format) {
		DateFormat dateFormat;
		if (format != null) {
			dateFormat = new SimpleDateFormat(format);
		} else {
			dateFormat = DateFormat.getInstance();
		}
		return dateFormat.format(date);
	}

	private static <T> T getFirst(Collection<T> set, boolean remove) {
		Iterator<T> iterator = set.iterator();
		if (!iterator.hasNext()) {
			throw new IllegalStateException("The set is empty");
		}
		T first = iterator.next();
		if (remove) {
			iterator.remove();
		}
		return first;
	}

	public static <T> T pollFirst(Collection<T> set) {
		return getFirst(set, true);
	}

	public static <T> T peekFirst(Collection<T> set) {
		return getFirst(set, false);
	}

	public static <T, S extends T, U extends T> T nvl(S obj, U nullObject) {
		if (obj != null) {
			return obj;
		}
		return nullObject;
	}

	public static String lpad(String str, char pad, int len) {
		while (str.length() < len) {
			str = pad + "" + str;
		}
		return str;
	}

	public static int[] makeInts(int from, int to) {
		if (to < from) {
			throw new IllegalArgumentException("from=" + from + ", to=" + to);
		}
		int[] ints = new int[to - from + 1];
		for (int i = from; i <= to; i++) {
			ints[i] = from + i;
		}
		return ints;
	}

	public static Object nvl2(Object obj, Object obj1, Object obj2) {
		return obj == null ? obj2 : obj1;
	}

	public static <T> LinkedList<T> singletonList(T element) {
		LinkedList<T> list = new LinkedList<T>();
		list.add(element);
		return list;
	}

	public static String pad(char c, int length) {
		char[] characters = new char[length];
		Arrays.fill(characters, c);
		return new String(characters);
	}

	public static String getSimpleName(File file) {
		String name = file.getName();
		int i = name.lastIndexOf('.');
		if (i > 0) {
			name = name.substring(0, i);
		}
		return name;
	}

	public static <T> T peek(Collection<T> set) {
		Iterator<T> iterator = set.iterator();
		if (!iterator.hasNext()) {
			throw new IllegalStateException("The set is empty");
		}
		T first = iterator.next();
		return first;
	}

	public static String substring(String string, int pos, int length) {
		if (string.length() - pos <= length) {
			return string.substring(pos);
		}
		return string.substring(pos, pos + length);
	}

	public static String join(String token, Object[] objects) {
		if (objects != null) {
			String[] strings = new String[objects.length];
			for (int i = 0; i < objects.length; i++) {
				strings[i] = objects[i].toString();
			}
			return join(token, strings);
		}
		return "";
	}

	public static String join(String token, Collection<String> strings) {
		return join(token, strings.toArray(new String[strings.size()]));
	}

	public static String join(String token, String[] strings) {
		if (strings == null || strings.length == 0) {
			return "";
		}
		StringBuffer sb = new StringBuffer();

		for (int x = 0; x < strings.length - 1; x++) {
			sb.append(strings[x]);
			sb.append(token);
		}
		sb.append(strings[strings.length - 1]);

		return (sb.toString());
	}

	public static String findMainClass(File jarFile) throws IOException {
		JarFile jf = new JarFile(jarFile);
		try {
			Manifest manifest = jf.getManifest();
			if (manifest == null) {
				return null;
			}
			String mainClass = manifest.getMainAttributes().getValue("Main-Class");
			return mainClass;
		} finally {
			jf.close();
		}
	}

	public static String findUniqueName(String name, List<String> names) {
		Set<String> nameSet = new HashSet<String>(names);
		if (!nameSet.contains(name)) {
			return name;
		}
		int i = 1;
		while (true) {
			String trial = name + "-" + i;
			if (!names.contains(trial)) {
				return trial;
			}
			i++;
		}
	}

	public static <T> boolean contains(T[] array, T elem) {
		return indexOf(array, elem) >= 0;
	}

	public static <T> int indexOf(T[] array, T elem) {
		int i = 0;
		for (T t : array) {
			if (t.equals(elem)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	public static URL getResource(String name) {
		URL res = Utilities.class.getResource("/resource/" + name);
		return res;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] reverse(T[] array) {
		Class<?> elemClass = array.getClass().getComponentType();
		int l = array.length;
		T[] reverse = (T[]) Array.newInstance(elemClass, l);
		for (int i = 0; i < array.length; i++) {
			reverse[l - i - 1] = array[i];
		}
		return reverse;
	}

	public static File toFile(URL url) {
		File file;
		try {
			file = new File(url.toURI());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
		return file;
	}

	public static <T, C extends Collection<T>> Collection<T> toCollection(Class<C> cls, T[] array) {
		try {
			Collection<T> collection = cls.getConstructor().newInstance();
			for (T t : array) {
				collection.add(t);
			}
			return collection;
		} catch (RuntimeException re) {
			throw re;
		} catch (Throwable t) {
			throw new IllegalArgumentException(t);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T, S extends T, U extends T> T[] joinArrays(S[] array0, U[] array1, Class<T> elementClass) {
		int l0 = Array.getLength(array0);
		int l1 = Array.getLength(array1);
		T[] array = (T[]) Array.newInstance(elementClass, l0 + l1);
		System.arraycopy(array0, 0, array, 0, l0);
		System.arraycopy(array1, 0, array, l0, l1);
		return array;
	}

	public static <T, C extends Collection<T>> Collection<T> flatten(Class<C> cls,
			Collection<? extends Collection<T>> collections) {
		try {
			Collection<T> flatCollection = cls.getConstructor().newInstance();
			for (Collection<T> collection : collections) {
				flatCollection.addAll(collection);
			}
			return flatCollection;
		} catch (RuntimeException re) {
			throw re;
		} catch (Throwable t) {
			throw new IllegalArgumentException(t);
		}
	}

	/**
	 * Converts (a portion) of the given stream to a byte array.
	 * 
	 * @param inputStream the stream from which to read bytes
	 * @param size        the number of bytes to be read, or -1 if the whole of the
	 *                    (remaining) bytes should be read
	 * @return a byte array of length <tt>size</tt> containing bytes read from the
	 *         given stream, starting at the current position
	 * @throws IOException
	 */
	public static byte[] toByteArray(InputStream inputStream, final int size) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		long length = size == -1 ? Long.MAX_VALUE : size;
		pipe(inputStream, out, length);
		return out.toByteArray();
	}

	public static byte[] load(File file) throws IOException {
		InputStream in = new FileInputStream(file);
		try {
			in = new BufferedInputStream(in);
			return toByteArray(in, -1);
		} finally {
			in.close();
		}
	}

	/**
	 * Saves a byte array to a file.
	 * 
	 * @param file  the file
	 * @param bytes the byte array
	 * @throws IOException
	 */
	public static void save(File file, byte[] bytes) throws IOException {
		OutputStream os = new FileOutputStream(file);
		try {
			os = new BufferedOutputStream(os);
			for (int i = 0; i < bytes.length; i += 2048) {
				int len = Math.min(bytes.length - i, 2048);
				os.write(bytes, i, len);
			}
		} finally {
			os.close();
		}
	}

	/**
	 * Returns a string containing the (simple) names of the objects in the array,
	 * separated by ';', enclosed by square brackets.
	 * 
	 * @param objects the object array
	 * @param simple  flag denoting if simple class names should be used
	 * @return a string containing the names of the objects' classes
	 */
	public static String classesString(Object[] objects, boolean simple) {
		String[] strings = new String[objects.length];
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] == null) {
				strings[i] = "null";
			} else if (simple) {
				strings[i] = objects[i].getClass().getSimpleName();
			} else {
				strings[i] = objects[i].getClass().getName();
			}
		}
		return "[" + join(",", strings) + "]";
	}

	/**
	 * The same as {@linkplain #classesString(Object[], boolean)} for collections.
	 * 
	 * @param objects
	 * @param simple
	 * @return
	 */
	public static String classesString(Collection<?> objects, boolean simple) {
		return classesString(objects.toArray(), simple);
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] castArray(Object[] array, Class<T> componentType) {
		T[] result = (T[]) Array.newInstance(componentType, array.length);
		for (int i = 0; i < array.length; i++) {
			result[i] = componentType.cast(array[i]);
		}
		return result;
	}

	public static <T> Iterable<T> castIterable(final Object iterable, final Class<T> itemClass) {
		if (iterable == null) {
			return null;
		}
		if (iterable.getClass().isArray()) {
			return new Iterable<T>() {

				public Iterator<T> iterator() {
					return new Iterator<T>() {

						int i = 0;

						public boolean hasNext() {
							return i < Array.getLength(iterable);
						}

						public T next() {
							return itemClass.cast(Array.get(iterable, i++));
						}

						public void remove() {
							throw new UnsupportedOperationException();

						}

					};
				}

			};
		} else if (Iterable.class.isAssignableFrom(iterable.getClass())) {
			return new Iterable<T>() {

				public Iterator<T> iterator() {
					return new Iterator<T>() {

						Iterator<?> iter = ((Iterable<?>) iterable).iterator();

						public boolean hasNext() {
							return iter.hasNext();
						}

						public T next() {
							return itemClass.cast(iter.next());
						}

						public void remove() {
							iter.remove();
						}

					};
				}

			};
		}
		throw new IllegalArgumentException(iterable.getClass().getName() + " is not iterable");

	}

	public static <T> Iterable<T> wrap(final Iterator<?> iterator, final Class<T> elemClass) {
		return new Iterable<T>() {

			public Iterator<T> iterator() {
				return new Iterator<T>() {

					public boolean hasNext() {
						return iterator.hasNext();
					}

					public T next() {
						return elemClass.cast(iterator.next());
					}

					public void remove() {
						iterator.remove();
					}

				};
			}

		};
	}

	public static String toString(URL url, Charset charset) throws IOException {
		InputStream stream = url.openStream();
		try {
			PWriter pw = PWriter.create();
			for (String line : readLines(stream, charset)) {
				pw.println(line);
			}
			pw.flush();
			return pw.toString();
		} finally {
			stream.close();
		}
	}

	/**
	 * Reads bytes from the <tt>from</tt> input stream and writes them to the
	 * <tt>to</tt> output stream.
	 * 
	 * @param from
	 * @param to
	 * @throws IOException
	 */
	public static void pipe(InputStream from, OutputStream to) throws IOException {
		pipe(from, to, Long.MAX_VALUE);
	}

	public static void pipe(InputStream from, OutputStream to, long size) throws IOException {
		final int EOF = -1;
		final int BUFFER_SIZE = 1024;
		byte[] buffer = new byte[BUFFER_SIZE];
		long total = 0;
		for (;;) {
			if (Thread.interrupted()) {
				Thread.currentThread().interrupt();
				return;
			}
			long diff = size - total;
			int length;
			if (diff >= BUFFER_SIZE) {
				length = BUFFER_SIZE;
			} else {
				length = (int) diff;
			}
			if (length <= 0) {
				return;
			}
			int n = from.read(buffer, 0, length);
			if (n == EOF) {
				return;
			}
			to.write(buffer, 0, n);
			total += n;
		}
	}

	public static void concatenate(File target, Collection<File> source) throws IOException {
		boolean append = false;
		for (File file : source) {
			long size = file.length();
			long transferred = 0;
			FileOutputStream outputStream = new FileOutputStream(target, append);
			append = true;
			try {
				FileChannel to = outputStream.getChannel();
				FileInputStream inputStream = new FileInputStream(file);
				try {
					FileChannel from = inputStream.getChannel();
					while (transferred < size) {
						transferred += to.transferFrom(from, transferred, size - transferred);
					}
				} finally {
					inputStream.close();
				}
			} finally {
				outputStream.close();
			}
		}
	}

	public static void concatenate(File target, File... source) throws IOException {
		concatenate(target, Arrays.asList(source));
	}

	public static Iterable<String> readLines(String command, Ref<InputStream> streamRef, Charset charset)
			throws IOException {
		Process proc = Runtime.getRuntime().exec(command);
		InputStream stream = proc.getInputStream();
		if (streamRef != null) {
			streamRef.t = stream;
		}
		return readLines(stream, charset);
	}

	public static Iterable<String> readLines(String command, Charset charset) throws IOException {
		return readLines(command, null, charset);
	}

	public static Iterable<String> readLines(File file, Ref<InputStream> streamRef, Charset charset)
			throws IOException {
		InputStream stream = new FileInputStream(file);
		if (streamRef != null) {
			streamRef.t = stream;
		}
		return readLines(stream, charset);
	}

	public static Iterable<String> readLines(File file, Charset charset) throws IOException {
		return readLines(file, null, charset);
	}

	public static Iterable<String> readLines(URL url, Charset charset) throws IOException {
		return readLines(url.openStream(), charset);
	}

	public static Iterable<String> readLines(final InputStream stream, Charset charset) throws IOException {

		final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset));
		return new Iterable<String>() {

			public Iterator<String> iterator() {
				return new Iterator<String>() {

					private String currentLine;

					public boolean hasNext() {
						try {
							currentLine = reader.readLine();
							if (currentLine == null) {
								stream.close();
							}
						} catch (IOException e) {
							try {
								stream.close();
							} catch (IOException ignored) {
								/* empty */
							}
							throw new IllegalStateException("Cannot read file", e);
						}
						return currentLine != null;
					}

					public String next() {
						return currentLine;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	public static Class<?> getWrapperClass(Class<?> primitiveClass) {
		return wrapperClasses.get(primitiveClass);
	}

	public static boolean isPrimitiveClass(Class<?> cls) {
		return getWrapperClass(cls) != null;
	}

	public static String getDuration(long milliseconds, String format) {
		long ms = milliseconds - 3600000;
		return toString(new Date(ms), format);
	}

	public static <S, T> Pair<S, T> pair(S arg1, T arg2) {
		return new Pair<S, T>(arg1, arg2);
	}

	public static Object getField(Object obj, String name) {
		Field field;
		Class<?> klass;
		if (obj instanceof String) {
			try {
				klass = Class.forName((String) obj);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("class not found: " + obj);
			}
			obj = null;
		} else {
			klass = obj.getClass();
		}
		try {
			field = klass.getDeclaredField(name);
		} catch (Exception e) {
			try {
				field = klass.getField(name);
			} catch (Exception e1) {
				throw new IllegalArgumentException("Field not available: " + klass.getName() + "." + name);
			}

		}
		// TODO Java>=9 deprecates isAccessible, but canAccess(obj) is not available as
		// of Java<9
		// Substitute this by !field.canAccess(obj) when compiling against Java 11
		if (!field.isAccessible()) {
			field.setAccessible(true);
		}
		Object value;
		try {
			value = field.get(obj);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Field cannot be accessed: " + klass.getName() + "." + name);
		}
		return value;
	}

	/**
	 * Invokes an instance method of the given object, or a class method, if the
	 * object is a String (containing the class name) resp. an instance of
	 * java.lang.Class.
	 * <p>
	 * The method must have the given name and its formal parameters must match the
	 * given arguments. If there is more than one matching method, it is undefined
	 * which one of them will be chosen.
	 * 
	 * 
	 * @param methodName a method name
	 * @param object     one of the following
	 *                   <ul>
	 *                   <li>a String, representing a class name</li>
	 *                   <li>a Class object</li>
	 *                   <li>any other object</li>
	 *                   </ul>
	 * @param args       the arguments to be passed to the method
	 * @return
	 */
	public static Object invoke(String methodName, Object object, Object... args) {
		return invokeIfAnnotated(methodName, null, object, args);
	}

	public static Object invokeIfAnnotated(String methodName, Class<?> annotationClass, Object object, Object... args) {
		Method method;
		Class<?> theClass;
		Object target = null;
		if (object instanceof String) {
			try {
				theClass = Class.forName((String) object);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(e);
			}
		} else if (object instanceof Class<?>) {
			theClass = (Class<?>) object;
		} else {
			theClass = object.getClass();
			target = object;
		}
		method = resolveMethod(theClass, methodName, args, true);

		if (method != null && annotationClass != null) {
			boolean annotationFound = false;
			for (Annotation ann : method.getDeclaredAnnotations()) {
				if (annotationClass.isInstance(ann)) {
					annotationFound = true;
					break;
				}
			}
			if (!annotationFound) {
				throw new IllegalArgumentException(
						"No matching method annotated by " + annotationClass.getName() + " found");
			}
		}

		if (method == null) {
			throw new IllegalArgumentException(
					"No matching method named " + methodName + " found in class " + theClass.getName());
		}

		// TODO Java>=9 deprecates isAccessible, but canAccess(obj) is not available as
		// of Java<9
		// Substitute this by !method.canAccess(obj) when compiling against Java 11
		if (!method.isAccessible()) {
			method.setAccessible(true);
		}
		Object result;
		try {
			result = method.invoke(target, args);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(
					"cannot access method " + methodName + " of " + object.getClass().getName());
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			}
			throw new IllegalStateException(e.getCause());
		}
		return result;

	}

	@SuppressWarnings("unchecked")
	public static <T> T clone(T t) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeObject(t);
			out.flush();

			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(in);
			T copy = (T) ois.readObject();
			in.close();
			out.close();
			return copy;
		} catch (RuntimeException re) {
			throw re;
		} catch (Throwable th) {
			throw new IllegalStateException(th);
		}
	}

	/**
	 * Convenience method for finding a method that fits a name and the types of the
	 * objects inside the parameter array.
	 * 
	 * @param clazz a class that declares the method that is looked for
	 * @param name  the name of the method
	 * @param args  array of arguments with which the method should be called
	 * @return a method that has the given names and can be called using the
	 *         arguments given, <code>null</code> if no such method exists
	 */
	public static Method resolveMethod(Class<?> clazz, String name, Object[] args) {
		return resolveMethod(clazz, name, args, false);
	}
	
	@SuppressWarnings("unchecked")
	public static Method resolveMethod(Class<?> clazz, String name, Object[] args, boolean slack) {
		Method[] methods = Utilities.joinArrays(clazz.getMethods(), clazz.getDeclaredMethods(), Method.class);

		for (int i = 0; i < methods.length; i++) {
			Method m = methods[i];
			if (m.getName().equals(name)) {
				Class<?>[] classes = m.getParameterTypes();
				if (args == null) {
					if (classes.length == 0) {
						return m;
					}
				} else {
					boolean match = args.length == classes.length;
					for (int j = 0; match && j < args.length; j++) {
						match = args[j] == null || classes[j].isAssignableFrom(args[j].getClass())
								|| wrapperClasses.get(classes[j]) == args[j].getClass();
					}
					if (match) {
						return m;
					}
				}
			}
		}
		Method method = null;
		if (slack) {
			for (Method _method : Utilities.joinArrays(clazz.getMethods(), clazz.getDeclaredMethods(), Method.class)) {
				if (_method.getName().equals(name) && args.length == _method.getParameterTypes().length) {
					method = _method;
					break;
				}
			}
			if (method != null) {
				Class<?>[] types = method.getParameterTypes();
				for (int i = 0; i < types.length; i++) {
					Class<?> type = types[i];
					if (args[i] != null && !types[i].isInstance(args[i])
							&& wrapperClasses.get(type) != args[i].getClass()) {
						if (args[i] instanceof Map<?,?>) {
							args[i] = Utilities.fromMap((Map<String,Object>) args[i], type);
						} else {
							args[i] = ObjectFactory.createFromString(type, args[i].toString());	
						}
						
					}
				}
			}			
		}
		return method;
	}
	
	

	@SuppressWarnings("unchecked")
	public static <T> Constructor<T> resolveConstructor(Class<T> clazz, Object... args) {
		Constructor<?> c = null;
		for (Constructor<?> constructor : clazz.getConstructors()) {
			Class<?>[] classes = constructor.getParameterTypes();
			if (args.length == 0) {
				if (classes.length == 0) {
					c = constructor;
					break;
				}
			} else {
				boolean match = args.length == classes.length;
				for (int j = 0; match && j < args.length; j++) {
					match = classes[j].isAssignableFrom(args[j].getClass())
							|| wrapperClasses.get(classes[j]) == args[j].getClass();
				}
				if (match) {
					c = constructor;
					break;
				}
			}
		}
		return (Constructor<T>) c;
	}

	public static <T> boolean in(T element, T... set) {
		for (T s : set) {
			if (element.equals(s)) {
				return true;
			}
		}
		return false;
	}

	public static Method findMethod(Class<?> clazz, String name, boolean declared) {
		for (Method method : declared ? clazz.getDeclaredMethods() : clazz.getMethods()) {
			if (method.getName().equals(name)) {
				return method;
			}
		}
		return null;
	}

	/**
	 * 
	 * @param          <T>
	 * @param set0
	 * @param set1
	 * @param set0Only
	 * @param set1Only
	 * @return true iff both sets are equal
	 */
	@SuppressWarnings("unchecked")
	public static <T> boolean computeDifference(Set<T> set0, Set<T> set1, Ref<Set<T>> set0Only, Ref<Set<T>> set1Only) {
		try {
			set0Only.t = set0.getClass().getConstructor().newInstance();
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot instantiate another " + set0.getClass().getName(), e);

		}
		try {
			set1Only.t = set1.getClass().getConstructor().newInstance();
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot instantiate another " + set1.getClass().getName(), e);

		}

		set0Only.t.addAll(set0);
		set1Only.t.addAll(set1);

		set0Only.t.removeAll(set1);
		set1Only.t.removeAll(set0);

		return set0Only.t.isEmpty() && set1Only.t.isEmpty();

	}

	public static Number round(Number number, Number unit) {
		double n = number.doubleValue();
		double u = unit.doubleValue();

		double r = Math.IEEEremainder(n, u);

		if (r < n / 2) {
			n -= r;
		} else {
			n = n + u - r;
		}

		return Double.valueOf(n);

	}

	@Deprecated
	public static InputStream filter(String command, InputStream input, File workingDir) throws IOException {

		if (workingDir == null) {
			workingDir = new File(".");
		}

		Process process = Runtime.getRuntime().exec(command, new String[0], workingDir);
		try {
			process.waitFor();
		} catch (InterruptedException ie) {

		}

		if (input != null) {
			pipe(input, process.getOutputStream());
		}

		return process.getInputStream();
	}

	public static int iIn(Object object, Object... objects) {
		return indexOf(objects, object);
	}

	private static abstract class MyFileFilter extends FileFilter {
		String suffix;
	}

	public static File[] chooseFiles(JFileChooser fileChooser, Component component, boolean open, boolean multiple,
			String message, String file, String... filter) {

		fileChooser.setMultiSelectionEnabled(multiple);

		for (FileFilter fileFilter : fileChooser.getChoosableFileFilters()) {
			if (fileFilter instanceof MyFileFilter) {
				fileChooser.removeChoosableFileFilter(fileFilter);
			}
		}

		if (filter.length > 0) {
			for (int i = 0; i < filter.length; i += 2) {
				final String description = filter[i];
				final String suffix = filter[i + 1].toLowerCase();
				MyFileFilter fileFilter = new MyFileFilter() {

					@Override
					public boolean accept(File f) {
						if (f.isDirectory()) {
							return true;
						}
						String name = f.getName().toLowerCase();
						return name.endsWith(suffix);
					}

					@Override
					public String getDescription() {
						return description;
					}
				};
				fileFilter.suffix = suffix;
				fileChooser.addChoosableFileFilter(fileFilter);
			}

			if (file != null) {
				int dot = file.lastIndexOf('.');
				if (dot >= 0) {
					String type = file.substring(dot + 1);
					for (FileFilter _filter : fileChooser.getChoosableFileFilters()) {
						if (_filter instanceof MyFileFilter) {
							if (((MyFileFilter) _filter).suffix.equals(type)) {
								fileChooser.setFileFilter(_filter);
								break;
							}

						}
					}
				}
			}
		}
		fileChooser.setDialogTitle(message);
		int ret;
		if (open) {
			ret = fileChooser.showOpenDialog(component);
		} else {
			ret = fileChooser.showSaveDialog(component);
		}
		File[] files;
		if (ret == JFileChooser.APPROVE_OPTION) {
			if (multiple) {
				if (fileChooser.getSelectedFiles() == null || fileChooser.getSelectedFiles().length == 0) {
					files = null;
				} else {
					files = fileChooser.getSelectedFiles();
				}
			} else {
				File selectedFile = fileChooser.getSelectedFile();
				if (!open) {
					FileFilter selectedFilter = fileChooser.getFileFilter();
					if (selectedFilter instanceof MyFileFilter) {
						String type = ((MyFileFilter) selectedFilter).suffix;
						selectedFile = UIUtilities.affixType(selectedFile, type);
					}
				}
				files = new File[] { selectedFile };
			}
		} else {
			files = null;
		}
		return files;
	}

	public static String generateKey() {
		KeyGenerator keygen;
		try {
			keygen = KeyGenerator.getInstance("DESede");
			return toHexString(keygen.generateKey().getEncoded());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public static byte[] encrypt(byte[] bytes, String hexKey)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		SecretKey key = new SecretKeySpec(toByteArray(hexKey), "DESede");
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("DESede");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		} catch (NoSuchPaddingException e) {
			throw new IllegalStateException(e);
		}
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(bytes);
	}

	public static List<Integer> getHashCodes(Collection<? extends Object> objects) {
		List<Integer> hashCodes = new LinkedList<Integer>();
		for (Object obj : objects) {
			hashCodes.add(System.identityHashCode(obj));
		}
		return hashCodes;
	}

	public static byte[] decrypt(byte[] bytes, String hexKey)
			throws NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		SecretKey key = new SecretKeySpec(toByteArray(hexKey), "DESede");
		try {
			Cipher cipher = Cipher.getInstance("DESede");
			cipher.init(Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(bytes);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static File[] listFiles (File directory, String... patterns) {
		if (!directory.canRead()) {
			throw new IllegalArgumentException("not readable: " + directory.getAbsolutePath());
		}
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("not a directory: " + directory.getAbsolutePath());
		}
		FilenameFilter fnf = new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if (patterns.length == 0) {
					return true;
				}
				for (String pattern : patterns) {
					if (Pattern.matches(pattern, name)) {
						return true;
					}
				}
				return false;
			}
		};
		return directory.listFiles(fnf);
	}

	public static PropertyDescriptor[] getProperties(Class<?> cls) {
		BeanInfo beanInfo;
		try {
			beanInfo = Introspector.getBeanInfo(cls);
			return beanInfo.getPropertyDescriptors();
		} catch (IntrospectionException e) {
			throw new IllegalArgumentException("Unable to introspect " + cls.getName(), e);
		}
	}

	private static <T> T newInstance(Class<?> clazz, Class<T> interfaceType, Object... args) {
		Constructor<?> constructor = resolveConstructor(clazz, args);
		if (constructor == null) {
			throw new IllegalArgumentException(
					"constructor not found: " + clazz.getName() + "#" + classesString(args, false));
		}
		Object obj;
		try {
			obj = constructor.newInstance(args);
		} catch (InstantiationException e) {
			throw new IllegalArgumentException("cannot instantiate class: " + clazz.getName(), e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot access constructor of class: " + clazz.getName(), e);
		} catch (InvocationTargetException ite) {
			if (ite.getTargetException() instanceof RuntimeException) {
				throw (RuntimeException) ite.getTargetException();
			}
			throw new IllegalArgumentException(ite);
		}
		T t;
		try {
			t = interfaceType.cast(obj);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(
					"cannot cast new instance of " + clazz.getName() + " to interface type " + interfaceType.getName());
		}

		return t;
	}

	public static <T> T newInstance(String cls, Class<T> interfaceType, Object... args) {
		Class<?> clazz;
		try {
			clazz = Class.forName(cls);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("class not found: " + cls, e);
		}
		return newInstance(clazz, interfaceType, args);
	}

	public static <T> T newInstance(Class<T> clazz, Object... args) {
		return newInstance(clazz, clazz, args);
	}

	/**
	 * If <tt>string</tt> contains <tt>oldString</tt> as a substring, returns
	 * <tt>string</tt> with the first occurrence of <tt>oldString</tt> replaced by
	 * <tt>newString</tt>. Otherwise returns <tt>string</tt>.
	 * <p>
	 * Note that in contrast to the <tt>replaceFirst</tt> method of
	 * <tt>java.lang.String</tt>, <tt>oldString</tt> is interpreted literally, not
	 * as a regular expression.
	 * 
	 * @param string
	 * @param oldString
	 * @param newString
	 * @return
	 */
	public static String replaceFirst(String string, String oldString, String newString) {
		String result = string;
		if (string != null) {
			int i = string.indexOf(oldString);
			if (i >= 0) {
				result = string.substring(0, i) + newString;
				if (i + oldString.length() < string.length() - 1) {
					result += string.substring(i + oldString.length());
				}
			}
		}
		return result;

	}

	public static URL asURL(Class<?> clazz) {
		int pkg = clazz.getPackage() == null ? 0 : clazz.getPackage().getName().length() + 1;
		String className = clazz.getName().substring(pkg);
		String resourceName = className.replace('.', '$') + ".class";
		URL url = clazz.getResource(resourceName);
		return url;
	}

	public static byte[] serialize(Object object) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(bos);
			os.writeObject(object);
			os.flush();
			os.close();
			byte[] bytes = bos.toByteArray();
			return bytes;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot replicate object of type " + object.getClass().getName(), e);
		}
	}

	public static <T> T deserialize(byte[] bytes, final ClassLoader classloader, Class<T> cls) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

			ObjectInputStream is = new ObjectInputStream(bis) {
				protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
					String name = desc.getName();
					try {
						return Class.forName(name, false, classloader);
					} catch (ClassNotFoundException ex) {
						@SuppressWarnings("rawtypes")
						Class cl = primitiveClasses.get(name);
						if (cl != null) {
							return cl;
						}
						throw ex;
					}
				}
			};
			return cls.cast(is.readObject());
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot replicate object of type " + cls.getName(), e);
		}
	}

	public static <T> T replicate(T object, ClassLoader classloader) {
		if (object == null) {
			return null;
		}
		byte[] bytes = serialize(object);
		@SuppressWarnings("unchecked")
		Class<T> cls = (Class<T>) object.getClass();
		return deserialize(bytes, classloader, cls);
	}

	private static String changeNewlines(String string, String newline) {
		string = string.replaceAll(CR, "");
		string = string.replaceAll(LF, newline);
		return string;
	}

	public static String unixEncode(String string) {
		return changeNewlines(string, LF);
	}

	public static String dosEncode(String string) {
		return changeNewlines(string, CRLF);
	}

	public static String platformEncode(String string) {
		return changeNewlines(string, System.getProperty("line.separator"));
	}

	public static String toString(byte[] bytes, String encoding) {
		String string;
		try {
			string = new String(bytes, encoding);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("Illegal encoding: " + encoding);
		}
		return string;
	}

	public static byte[] getBytes(String string, String encoding) {
		byte[] bytes;
		try {
			bytes = string.getBytes(encoding);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("Illegal encoding: " + encoding);
		}
		return bytes;
	}

	// see http://ridiculousfish.com/blog/posts/colors.html
	private static double getHue(int idx, double initialAngle) {
		int ridx = 0;
		for (int i = 0; i < 31; i++, ridx = (ridx << 1) | (idx & 1), idx >>>= 1)
			;
		return initialAngle + (1D * ridx) / (1 << 31);
	}

	public static Color getColor(int i, float initialAngle, float saturation, float brightness) {
		double hue = getHue(i, initialAngle);
		Color color = Color.getHSBColor((float) hue, saturation, brightness);
		return color;
	}

	public static String getRGBString(int i, float initialAngle, float saturation, float brightness) {
		Color color = getColor(i, initialAngle, saturation, brightness);
		return String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
	}

	public static byte[] decodeBase64(String string) {
		Object decoder;
		String method;
		try {
			decoder = invoke("getDecoder", "java.util.Base64");
			method = "decode";
		} catch (IllegalArgumentException e) {
			decoder = "javax.xml.bind.DatatypeConverter";
			method = "parseBase64Binary";
		}
		return (byte[]) invoke(method, decoder, string);
	}

	public static synchronized String getKey(String className, String methodName, String b) {
		String key = keys.get(b);
		if (key == null) {
			try {
				CL cl = new CL();
				ByteArrayInputStream stream = new ByteArrayInputStream(decodeBase64(b));
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				pipe(stream, out);
				out.flush();
				out.close();
				byte[] code = out.toByteArray();
				cl.addClass(className, code);
				Class<?> klass = cl.loadClass(className);
				Method method = klass.getMethod(methodName);
				key = (String) method.invoke(null, (Object[]) null);
				keys.put(b, key);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new IllegalArgumentException("cannot read the key", e);
			}
		}
		return key;
	}

	public static int max(int... nums) {
		if (nums.length == 0) {
			throw new IllegalArgumentException("no numbers provided to compute maximum of");
		}
		int max = Integer.MIN_VALUE;
		for (int n : nums) {
			if (n > max) {
				max = n;
			}
		}
		return max;
	}

	public static int min(int... nums) {
		if (nums.length == 0) {
			throw new IllegalArgumentException("no numbers provided to compute minimum of");
		}
		int min = Integer.MAX_VALUE;
		for (int n : nums) {
			if (n < min) {
				min = n;
			}
		}
		return min;
	}

	public static <T> T fromMap(Map<String, Object> map, Class<T> cls, Object... constructorArgs) {
		T t = newInstance(cls, constructorArgs);
		for (PropertyDescriptor prop : getProperties(cls)) {
			Object value = map.get(prop.getName());
			if (value != null) {
				if (!prop.getPropertyType().isInstance(value)) {
					value = ObjectFactory.createFromString(prop.getPropertyType(), value.toString());
				}
				try {
					prop.getWriteMethod().invoke(t, value);
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new IllegalStateException(
							"cannot set property " + prop.getName() + " of class " + cls.getName(), e);
				}
			}
		}
		return t;
	}

	public static Map<String, Object> toMap(Object bean) {
		Map<String, Object> map = new TreeMap<String, Object>();
		for (PropertyDescriptor prop : getProperties(bean.getClass())) {
			if (!"class".equals(prop.getName()) && prop.getReadMethod() != null) {
				Object value;
				try {
					value = prop.getReadMethod().invoke(bean);
					map.put(prop.getName(), value);
				} catch (Throwable t) {
					throw new IllegalStateException(t);
				}
			}
		}
		return map;
	}

	/*
	 * From Apache Commons Lang WordUtils
	 */
	public static String wrap(final String str, int wrapLength, String newLineStr, final boolean wrapLongWords) {
		if (str == null) {
			return null;
		}
		if (newLineStr == null) {
			newLineStr = System.getProperty("line.separator");
		}
		if (wrapLength < 1) {
			wrapLength = 1;
		}
		final int inputLineLength = str.length();
		int offset = 0;
		final StringBuilder wrappedLine = new StringBuilder(inputLineLength + 32);

		while (inputLineLength - offset > wrapLength) {
			if (str.charAt(offset) == ' ') {
				offset++;
				continue;
			}
			int spaceToWrapAt = str.lastIndexOf(' ', wrapLength + offset);

			if (spaceToWrapAt >= offset) {
				// normal case
				wrappedLine.append(str.substring(offset, spaceToWrapAt));
				wrappedLine.append(newLineStr);
				offset = spaceToWrapAt + 1;

			} else {
				// really long word or URL
				if (wrapLongWords) {
					// wrap really long word one line at a time
					wrappedLine.append(str.substring(offset, wrapLength + offset));
					wrappedLine.append(newLineStr);
					offset += wrapLength;
				} else {
					// do not wrap really long word, just extend beyond limit
					spaceToWrapAt = str.indexOf(' ', wrapLength + offset);
					if (spaceToWrapAt >= 0) {
						wrappedLine.append(str.substring(offset, spaceToWrapAt));
						wrappedLine.append(newLineStr);
						offset = spaceToWrapAt + 1;
					} else {
						wrappedLine.append(str.substring(offset));
						offset = inputLineLength;
					}
				}
			}
		}

		// Whatever is left in line is short enough to just pass through
		wrappedLine.append(str.substring(offset));

		return wrappedLine.toString();
	}

	public static long hash64(String string) {
		long h = 0;
		for (int i = 0; i < string.length(); i++) {
			h = 63 * h + string.charAt(i);
		}
		return h;
	}

	public static Color decodeColor(String colorString) {
		Color color = null;
		if (colorString != null) {
			try {
				color = Color.decode(colorString);
			} catch (NumberFormatException e) {
				try {
					Field field = Color.class.getField(colorString.toLowerCase());
					if (field != null) {
						return (Color) field.get(null);
					}
				} catch (Exception e1) {

				}
			}
		}
		return color;
	}

	public static <T1, T2> Iterable<Pair<T1, T2>> pairs(final Iterable<T1> i1, final Iterable<T2> i2) {
		return new Iterable<Pair<T1, T2>>() {

			@Override
			public Iterator<Pair<T1, T2>> iterator() {
				return new Iterator<Pair<T1, T2>>() {

					private Iterator<T1> ii1 = i1.iterator();

					private Iterator<T2> ii2 = i2.iterator();

					@Override
					public boolean hasNext() {
						return ii1.hasNext() & ii2.hasNext();
					}

					@Override
					public Pair<T1, T2> next() {
						return pair(ii1.next(), ii2.next());
					}

					@Override
					public void remove() {
						ii1.remove();
						ii2.remove();
					}
				};
			}

		};
	}
	
}
