package org.jergometer.communication;

/**
 * Unconfigured serial port exception.
 */
public class UnconfiguredSerialPortException extends Exception {
	public UnconfiguredSerialPortException() {
		super("The serial port has not been configured yet.");
	}
}
