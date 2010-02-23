package org.jergometer.diagram;

import org.jergometer.gui.Diagram;
import org.jergometer.model.BikeSession;
import org.jergometer.model.MiniDataRecord;
import org.jergometer.model.StatsRecord;

import java.util.ArrayList;
import java.util.Date;

/**
 */
public class ProgressionVisualizer implements DiagramVisualizer {
	private final Diagram diagram;
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
			BikeDiagram.createLegend(diagram, false, false);

			for (BikeSession bikeSession : bikeSessions) {
				if (stopped) return;

				long time = bikeSession.getStartTime().getTime() - start;
				int programDuration = bikeSession.getProgramDuration();
				StatsRecord sum = bikeSession.getStatsRegular();

				if (sum.getPulseCount() != 0) {
					diagram.addValue("pulse", time, (int) (sum.getAveragePulse() + 0.5));
				}
				if (programDuration != 0) {
					diagram.addValue("pedalRPM", time, (int) (sum.getAveragePedalRPM() + 0.5));
					diagram.addValue("power", time, (int) (sum.getAveragePower() + 0.5));
					double performance = (sum.getAveragePower() * sum.getAveragePedalRPM() / 60);
					diagram.addValue("performance", time, (int) (performance + 0.5));
				}
			}
		}
	}
}
