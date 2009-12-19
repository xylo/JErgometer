package org.jergometer.diagram;

import org.jergometer.gui.Diagram;
import org.jergometer.model.BikeSession;
import org.jergometer.model.MiniDataRecord;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;

/**
 */
public class ProgressionVisualizer implements DiagramVisualizer {
	private Diagram diagram;
	private boolean stopped = false;

	public ProgressionVisualizer(Diagram diagram) {
		this.diagram = diagram;
	}

	public void stopVisualization() {
	}

	public void visualize(ArrayList<BikeSession> bikeSessions) {
		if (bikeSessions.size() < 2) return;

		Date startTime = bikeSessions.get(0).getStartTime();
		Date endTime = bikeSessions.get(bikeSessions.size() - 1).getStartTime();

		long start = startTime.getTime();
		long end = endTime.getTime();

		synchronized(diagram) {
			diagram.setTimeRange(new Diagram.Range(start,end));
			diagram.setTimeAxisType(Diagram.TimeAxisType.date);

			diagram.clearGraphs();
			diagram.addGraph("pulse", "Pulse", new Color(255,0,0), Diagram.Side.left);
			diagram.addGraph("pedalRPM", "Pedal RPM", new Color(0,255,0), Diagram.Side.left);
			diagram.addGraph("power", "Power", new Color(0,0,255), Diagram.Side.left);
			diagram.addGraph("performance", "Performance", new Color(0,0,0), Diagram.Side.left);

			for (BikeSession bikeSession : bikeSessions) {
				if (stopped) return;

				long time = bikeSession.getStartTime().getTime() - start;
				int programDuration = bikeSession.getProgramDuration();
				MiniDataRecord sum = bikeSession.getSum();

				if (bikeSession.getPulseCount() != 0) {
					diagram.addValue("pulse", time, sum.getPulse()/bikeSession.getPulseCount());
				}
				if (programDuration != 0) {
					diagram.addValue("pedalRPM", time, sum.getPedalRpm()/programDuration);
					diagram.addValue("power", time, sum.getPower()/programDuration);
					double performance = ((double) sum.getPower()/programDuration * sum.getPedalRpm()/programDuration / 60);
					diagram.addValue("performance", time, (int) performance);
				}
			}
		}
	}
}
