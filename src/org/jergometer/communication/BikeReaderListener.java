package org.jergometer.communication;

import org.jergometer.model.DataRecord;

/**
 * Listener of the bike.
 */
public interface BikeReaderListener {
	/**
	 * Called if the bike has sent the ACK getString.
	 */
	public void bikeAck();

	/**
	 * Called if the bike has sent the data.
	 *
	 * @param data date from the bike
	 */
	public void bikeData(DataRecord data);

	/**
	 * Called if an error occurred.
	 */
	public void bikeError();
}
