package org.jergometer.communication;

import java.io.IOException;
import java.io.Closeable;

/**
 * BikeConnector connects to a bike.
 * It is used to receive data from the bike and to control it.
 */
public interface BikeConnector extends Closeable {
	/**
	 * Returns the reader receiving messages from the ergometer.
	 *
	 * @return reader
	 */
	public BikeReader getReader();

	/**
	 * Returns the writer sending messages to the ergometer.
	 *
	 * @return writer
	 */
	public BikeWriter getWriter();

	/**
	 * Closes the connection to the ergometer.
	 */
	public void close() throws IOException;
}
