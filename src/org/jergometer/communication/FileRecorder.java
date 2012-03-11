package org.jergometer.communication;

import org.jergometer.model.DataRecord;

import java.io.*;

/**
 * Records file sessions.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class FileRecorder implements BikeListener {
	private DataOutputStream out = null;

	public FileRecorder(String simulatorSession) throws FileNotFoundException {
		out = new DataOutputStream(new FileOutputStream(simulatorSession));
	}

	@Override
	public void bikeAck() {
	}

	@Override
	public void bikeData(DataRecord data) {
		try {
			data.toStream(out);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void bikeError() {
	}

	@Override
	public void bikeDestPowerChanged(int change) {
	}

	public void close() {
		try {
			out.close();
		} catch (IOException ignored) {
		}
	}
}
