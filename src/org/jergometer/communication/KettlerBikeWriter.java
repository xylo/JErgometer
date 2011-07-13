package org.jergometer.communication;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * It writes messages to the bike.
 */
public class KettlerBikeWriter implements BikeWriter {

// static

	/** Bike commands. */
	public static final String CMD_UNKNOWN1 = "CD";
	public static final String CMD_HELLO    = "CM";
	public static final String CMD_GET_ID   = "ID";
	public static final String CMD_RESET    = "RS";
	public static final String CMD_GET_DATA = "PW ";
	private static final byte[] ln = {13,10};

// dynamic

	/** Output stream. */
	private DataOutputStream out;

	/**
	 * Creates the writer for the outgoing messages to the XUP board.
	 *
	 * @param out output stream
	 */
	public KettlerBikeWriter(OutputStream out) {
		this.out = new DataOutputStream(out);
	}

// commands

	/**
	 * Sends the hello command to the bike.
	 *
	 * @throws IOException thrown if io problems occurred
	 */
	public void sendHello() throws IOException {
		writeRawBytes(CMD_HELLO.getBytes());
	}

	/**
	 * Sends the hello command to the bike.
	 *
	 * @throws IOException thrown if io problems occurred
	 */
	public void sendReset() throws IOException {
		writeRawBytes(CMD_RESET.getBytes());
		try {
			// wait 5s after bike reset (avoids problems with X3)
			Thread.sleep(5000);
		} catch (InterruptedException ignored) {}
	}

	/**
	 * Sends the getId command to the bike.
	 *
	 * @throws IOException thrown if io problems occurred
	 */
	public void sendGetId() throws IOException {
		writeRawBytes(CMD_GET_ID.getBytes());
	}

	/**
	 * Sends the getData command to the bike.
	 *
	 * @throws IOException thrown if io problems occurred
	 */
	public void sendGetData(int power) throws IOException {
		writeRawBytes((CMD_GET_DATA + power).getBytes());
	}

	public void writeRawBytes(byte[] bytes) throws IOException {
		out.write(bytes);
//		out.flush();
		out.write(ln);
		out.flush();
	}
}
