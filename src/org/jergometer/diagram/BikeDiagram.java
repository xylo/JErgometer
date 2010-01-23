package org.jergometer.diagram;

import org.jergometer.gui.Diagram;

import java.awt.*;

/**
 * @author Stefan Endrullis
 */
public class BikeDiagram {
	public static void createLegend(Diagram diagram, boolean dest, boolean bright) {
		String suffix = "";
		Stroke s = dest ? new BasicStroke(2f) : new BasicStroke(0.5f);
		int a = bright ? 64 : 255;
		double b = -0.5; //bright ? 0 : -0.5;
		if (dest) {
			suffix = "-dest";
		}
		diagram.addGraph("pulse" + suffix, new Diagram.Graph("Pulse", Diagram.brighten(b, new Color(255,0,0, a)), s, bright), Diagram.Side.left);
		diagram.addGraph("pedalRPM" + suffix, new Diagram.Graph("Pedal RPM", Diagram.brighten(b, new Color(0,255,0, a)), s, bright), Diagram.Side.left);
		diagram.addGraph("power" + suffix, new Diagram.Graph("Power", Diagram.brighten(b, new Color(0,0,255, a)), s, bright), Diagram.Side.left);
		diagram.addGraph("performance" + suffix, new Diagram.Graph("Performance", Diagram.brighten(b, new Color(0,0,0, a)), s, bright), Diagram.Side.left);
	}
}
