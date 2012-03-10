package org.jergometer.communication;

import java.util.HashMap;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class BikeConnectors {
	public static final BikeConnector[] allBikeConnectors = new BikeConnector[]{
		new KettlerBikeConnector(),
		new SocatKettlerBikeConnector(),
		new BikeConnectorSimulatorRecord(),
		new BikeConnectorSimulatorReplay(),
	};

	public static HashMap<String, BikeConnector> name2bikeConnector = new HashMap<String, BikeConnector>() {{
		for (BikeConnector bikeConnector : allBikeConnectors) {
			put(bikeConnector.getName(), bikeConnector);
		}
	}};
}
