package org.jergometer.communication;

import java.io.IOException;

/**
 * Interface for ergometer writers.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public interface BikeWriter {
	public void sendHello() throws IOException;
	public void sendReset() throws IOException;
	public void sendGetId() throws IOException;
	public void sendGetData(int power) throws IOException;
}
