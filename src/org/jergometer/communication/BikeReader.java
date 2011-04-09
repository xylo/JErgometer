package org.jergometer.communication;

/**
 * Interface for ergometer readers.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public interface BikeReader {
	public void close();
	public void addBikeReaderListener(BikeReaderListener listener);
	public void removeBikeReaderListener(BikeReaderListener listener);
	public void start();
}
