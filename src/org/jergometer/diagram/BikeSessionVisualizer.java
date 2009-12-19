package org.jergometer.diagram;

import org.jergometer.gui.Diagram;
import org.jergometer.model.BikeSession;
import org.jergometer.model.MiniDataRecord;

import java.awt.*;
import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Used to visualize a bike session in the diagram.
 */
public class BikeSessionVisualizer implements DiagramVisualizer {
	private final Diagram diagram;
	private boolean stopped = false;

	public BikeSessionVisualizer(Diagram diagram) {
		this.diagram = diagram;
	}

	public void visualize(BikeSession bikeSession, boolean clearBefore) throws IOException {
		synchronized(diagram) {
			ArrayList<MiniDataRecord> miniDataRecords = bikeSession.getData();

			if (clearBefore) {
				diagram.setTimeRange(new Diagram.Range(0,bikeSession.getProgramDuration()));
				diagram.setTimeAxisType(Diagram.TimeAxisType.minute);
				diagram.clearGraphs();
			}
			diagram.addGraph("pulse", "Pulse", new Color(128,0,0), Diagram.Side.left);
			diagram.addGraph("pedalRPM", "Pedal RPM", new Color(0,128,0), Diagram.Side.left);
			diagram.addGraph("power", "Power", new Color(0,0,128), Diagram.Side.left);

			int time = 0;
			for (MiniDataRecord miniDataRecord : miniDataRecords) {
				if (stopped) return;
				if (time == bikeSession.getProgramDuration()) break;

				diagram.addValue("pulse", time, miniDataRecord.getPulse());
				diagram.addValue("pedalRPM", time, miniDataRecord.getPedalRpm());
				diagram.addValue("power", time, miniDataRecord.getPower());

				time++;
			}
		}
	}

// DiagramVisualizer
	public void stopVisualization() {
		stopped = true;
	}
}
