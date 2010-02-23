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

	public void visualize(BikeSession bikeSession, boolean clearBefore, boolean fullSessionLength) throws IOException {
		synchronized(diagram) {
			ArrayList<MiniDataRecord> miniDataRecords = bikeSession.getData();

			int duration = fullSessionLength ? bikeSession.getStatsTotal().getDuration() : bikeSession.getProgramDuration();

			if (clearBefore) {
				diagram.setTimeRange(new Diagram.Range(0, duration));
				diagram.setTimeAxisType(Diagram.TimeAxisType.minute);
				diagram.clearGraphs();
			}
			BikeDiagram.createLegend(diagram, false, false, bikeSession.getProgramDuration());

			int time = 0;
			for (MiniDataRecord miniDataRecord : miniDataRecords) {
				if (stopped) return;
				if (time == duration) break;

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
