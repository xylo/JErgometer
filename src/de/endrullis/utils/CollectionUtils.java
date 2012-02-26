package de.endrullis.utils;

import java.util.Iterator;

/**
 * Collection utils for Java.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CollectionUtils {
	/**
	 * Concatenates the elements from the collection to one string.
	 * If the collection is empty an empty string is returned.
	 *
	 * @param collection collection
	 * @param delimiter delimiter between the elements
	 * @return concatenation
	 */
	public static String join(Iterable collection, String delimiter) {
		return join(collection.iterator(), delimiter);
	}

	/**
	 * Concatenates the elements of the iterator to one string.
	 * If the iterator has no elements an empty string is returned.
	 *
	 * @param iterator iterator
	 * @param delimiter delimiter between the elements
	 * @return concatenation
	 */
	public static String join(Iterator iterator, String delimiter) {
		return join(iterator, "", delimiter, "");
	}

	/**
	 * Concatenates the elements from the collection to one string.
	 * If the collection is empty an empty string is returned.
	 *
	 * @param collection collection
	 * @param start start string
	 * @param delimiter delimiter between the elements
	 * @param end end string
	 * @return concatenation
	 */
	public static String join(Iterable collection, String start, String delimiter, String end) {
		return join(collection.iterator(), start, delimiter, end);
	}

	/**
	 * Concatenates the elements of the iterator to one string.
	 * If the iterator has no elements an empty string is returned.
	 *
	 * @param iterator iterator
	 * @param start start string
	 * @param delimiter delimiter between the elements
	 * @param end end string
	 * @return concatenation
	 */
	public static String join(Iterator iterator, String start, String delimiter, String end) {
		if (iterator.hasNext()) {
			StringBuffer s = new StringBuffer();

			s.append(start);
			s.append(iterator.next());

			while (iterator.hasNext()) {
				s.append(delimiter);
				s.append(iterator.next());
			}

			s.append(end);

			return s.toString();
		} else {
			return "";
		}
	}
}
