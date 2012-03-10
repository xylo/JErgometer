package org.jergometer.communication;

import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class BikeConnectorSimulatorRecord extends KettlerBikeConnector {

	private FileRecorder fileRecorder;

	@Override
	public void connect(String serialName, BikeListener listener) throws BikeException, UnsupportedCommOperationException, IOException {
		super.connect(serialName, listener);
		fileRecorder = new FileRecorder(BikeConnectorSimulatorReplay.SIMULATOR_SESSION);
		reader.addBikeReaderListener(fileRecorder);
	}

	@Override
	public void close() throws IOException {
		super.close();
		fileRecorder.close();
	}

	@Override
	public String getName() {
		return "simulator-record";
	}

	@Override
	public String toString() {
		return getName();
	}
}
