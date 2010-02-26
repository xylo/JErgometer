package org.jergometer.diagram;

import org.jergometer.gui.Diagram;
import org.jergometer.translation.I18n;

import java.awt.*;

/**
 * Utility class for adding the legend to a diagram.
 *
 * @author Stefan Endrullis
 */
public class BikeDiagram {
	public static void createLegend(Diagram diagram, boolean dest, boolean bright, long programLength) {
		diagram.addVerticalMarker(new Diagram.Marker(programLength, Color.BLACK, new BasicStroke(), "End of program"));

		createLegend(diagram, dest, bright);
	}

	public static void createLegend(Diagram diagram, boolean dest, boolean bright) {
		String suffix = "";
		Stroke s = dest ? new BasicStroke(2f) : new BasicStroke(0.5f);
		int a = bright ? 64 : 255;
		double b = -0.5; //bright ? 0 : -0.5;
		if (dest) {
			suffix = "-dest";
		} else {
			addGraph(diagram, "pulse-end", Diagram.brighten(0.5, new Color(255,0,0, a)), s, Diagram.Side.left, true);
		}
		addGraph(diagram, "pulse"       + suffix, Diagram.brighten(b, new Color(255,0,0, a)), s, Diagram.Side.left, bright);
		addGraph(diagram, "pedalRPM"    + suffix, Diagram.brighten(b, new Color(0,255,0, a)), s, Diagram.Side.left, bright);
		addGraph(diagram, "power"       + suffix, Diagram.brighten(b, new Color(0,0,255, a)), s, Diagram.Side.left, bright);
		addGraph(diagram, "performance" + suffix, Diagram.brighten(b, new Color(0,0,0, a)),   s, Diagram.Side.left, bright);
	}

	public static void addGraph(Diagram diagram, String name, Color color, Stroke s, Diagram.Side side, boolean hideInLegend) {
		diagram.addGraph(name, new Diagram.Graph(hideInLegend ? "" : I18n.getString("legend." + name), color, s, hideInLegend), side);
	}
}
