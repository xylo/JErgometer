package org.jergometer.communication;

/**
 * Unconfigured COM port exception.
 */
public class UnconfiguredComPortException extends Exception {
	public UnconfiguredComPortException() {
		super("The COM port has not been configured yet.");
	}
}
