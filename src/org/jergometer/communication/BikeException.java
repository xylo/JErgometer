package org.jergometer.communication;

import java.io.IOException;

/**
 * Special IO exception related to the serial port connection to the ergometer.
 */
public class BikeException extends Exception {
	public BikeException(String message) {
		super(message);
	}

	public BikeException(String message, Throwable cause) {
		super(message, cause);
	}

	public BikeException(Throwable cause) {
		super(cause);
	}
}
