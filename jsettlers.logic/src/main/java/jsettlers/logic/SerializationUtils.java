package jsettlers.logic;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class SerializationUtils {

	/**
	 * pucgenie: Don't use writeUnshared here.
	 * @param <T>
	 * @param oos
	 * @param data
	 * @throws IOException
	 */
	public static <T> void writeSparseArray(ObjectOutputStream oos, T[] data) throws IOException {
		oos.writeInt(data.length);

		for (int index = 0; index < data.length; index++) {
			T object = data[index];
			if (object != null) {
				oos.writeInt(index);
				oos.writeObject(object);
			}
		}
		oos.writeInt(-1);
		oos.flush();
	}

	/**
	 * Uses writeUnshared for the map, but the elements inside the map are shared (Java Serialization rules).
	 * @param <K>
	 * @param <V>
	 * @param oos
	 * @param map
	 * @throws IOException
	 */
	public static <K extends Comparable<K>, V> void writeHashMap(ObjectOutputStream oos, Map<K, V> map) throws IOException {
		oos.writeUnshared(new TreeMap<>(map));
	}

	@SuppressWarnings("unchecked")
	public static <K extends Comparable<K>, V> HashMap<K, V> readHashMap(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		return new HashMap<>((TreeMap<K, V>) ois.readUnshared());
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] readSparseArray(ObjectInputStream ois, Class<T> arrayElementType) throws IOException, ClassNotFoundException {
		int length = ois.readInt();
		T[] data = (T[]) Array.newInstance(arrayElementType, length);

		int index = ois.readInt();
		while (index >= 0) {
			data[index] = (T) ois.readObject();
			index = ois.readInt();
		}
		return data;
	}
}
