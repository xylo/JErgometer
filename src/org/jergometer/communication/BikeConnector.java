package org.jergometer.communication;

import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.Closeable;

/**
 * BikeConnector connects to a bike.
 * It is used to receive data from the bike and to control it.
 */
public interface BikeConnector extends Closeable {
	public void sendHello() throws IOException;
	public void sendReset() throws IOException;
	public void sendGetId() throws IOException;
	public void sendGetData() throws IOException;
	public void sendSetPower(int power) throws IOException;

	public void connect(String serialName, BikeListener listener) throws BikeException, UnsupportedCommOperationException, IOException;

	/**
	 * Closes the connection to the ergometer.
	 */
	public void close() throws IOException;

	public String getName();
}
