package org.jergometer.communication;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * It writes messages to the bike.
 */
public class KettlerBikeWriter {

// static

	/** Bike commands. */
	public static final String CMD_UNKNOWN1   = "CD";
	public static final String CMD_HELLO      = "CM";
	public static final String CMD_GET_ID     = "ID";
	public static final String CMD_RESET      = "RS";
	public static final String CMD_GET_STATUS = "ST ";
	public static final String CMD_SET_POWER  = "PW ";

// dynamic

	/** Newline. */
	private byte[] ln = {'\n'};
	/** Output stream. */
	private DataOutputStream out;

	/**
	 * Creates the writer for the outgoing messages to the XUP board.
	 *
	 * @param out output stream
	 */
	public KettlerBikeWriter(boolean cr, OutputStream out) {
		if (cr) {
			this.ln = new byte[]{'\r', '\n'};
		}
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
	 * Sends the getStatus command to the bike.
	 *
	 * @throws IOException thrown if io problems occurred
	 */
	public void sendGetData() throws IOException {
		writeRawBytes((CMD_GET_STATUS).getBytes());
	}

	/**
	 * Sends the setPower command to the bike.
	 *
	 * @throws IOException thrown if io problems occurred
	 */
	public void sendSetPower(int power) throws IOException {
		writeRawBytes((CMD_SET_POWER + power).getBytes());
	}

	public void writeRawBytes(byte[] bytes) throws IOException {
		out.write(bytes);
		out.write(ln);
		out.flush();
	}
}
