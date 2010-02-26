package org.jergometer.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Diagram component to draw graphs.
 */
public class Diagram extends JPanel implements ComponentListener {
	public enum Side {
		left, right;

		public static Side fromInt(int i) {
			switch (i) {
				case 0: return left;
				case 1: return right;
				default: return left;
			}
		}

		public int getInt() {
			switch(this) {
				case left: return 0;
				case right: return 1;
				default: return -1;
			}
		}
	}

	// todo: make configurable
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");

	public enum TimeAxisType { minute, date }

	/** Time range. */
	private Range timeRange = new Range(0, 3600);
	/** Time axis type. */
	private TimeAxisType timeAxisType = TimeAxisType.minute;
	/** Value range. */
	private Range[] valueRange = new Range[] { new Range(0,220), new Range(0,100) };
	/** Highlight ranges with colors (only for the left axis). */
	private ArrayList<Range<Color>> highlightRanges = new ArrayList<Range<Color>>();

	/** Graphs. */
	private ArrayList<Graph>[] graphs = new ArrayList[] { new ArrayList<Graph>(), new ArrayList<Graph>() };
	private HashMap<Object,Graph> key2Graph = new HashMap<Object, Graph>();
	private ArrayList<Marker> verticalMarkers = new ArrayList<Marker>();

	/** Background image. */
	private BufferedImage backgroundImage = null;

	/** Layout settings. */
	private static Margin margin = new Margin(15,50,35,15);

	private static int markerSize = 2;
	private static int crossSize = 5;

	private static Stroke normalStroke = new BasicStroke();

	private static Color gridColorEven = new Color(160,160,160);
	private static Color gridColorOdd = new Color(192,192,192);
	private static Stroke gridStrokeEven = new BasicStroke(0.6f);
	private static Stroke gridStrokeOdd = new BasicStroke(0.3f);

	/*
	private static Color gridColorEven = new Color(64,64,64);
	private static Color gridColorOdd = new Color(128,128,128);
	private static Stroke gridStrokeEven = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 1, new float[] {0,4}, 0);
	private static Stroke gridStrokeOdd = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 1, new float[] {0,6}, 0);
	*/

	private static HashMap<RenderingHints.Key,Object> renderingHintsNormal = new HashMap<RenderingHints.Key, Object>();
	static {
		renderingHintsNormal.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		renderingHintsNormal.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		renderingHintsNormal.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		renderingHintsNormal.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		renderingHintsNormal.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		renderingHintsNormal.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		renderingHintsNormal.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		renderingHintsNormal.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
	}

	private static HashMap<RenderingHints.Key,Object> renderingHintsGraph = new HashMap<RenderingHints.Key, Object>(renderingHintsNormal);
	static {
		renderingHintsGraph.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		renderingHintsGraph.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}

	public Diagram() {
		super(true);

		// todo: make configurable
		addHighlightRange(new Range<Color>(125, 145, brighten(0.5f, new Color(192,255,192))));
		addHighlightRange(new Range<Color>(145, 165, brighten(0.5f, new Color(192,230,255))));
		addHighlightRange(new Range<Color>(165, 220, brighten(0.5f, new Color(255,192,192))));

		addComponentListener(this);
		setPreferredSize(new Dimension(640, 480));
	}

	public static Color brighten(double ratio, Color color) {
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		int a = color.getAlpha();

		if (ratio >= 0) {
			r = (int) ((255-r)*ratio + r);
			g = (int) ((255-g)*ratio + g);
			b = (int) ((255-b)*ratio + b);
		} else {
			ratio = 1+ratio;
			r = (int) (r*ratio);
			g = (int) (g*ratio);
			b = (int) (b*ratio);
		}

		return new Color(r, g, b, a);
	}

	public void addHighlightRange(Range<Color> range) {
		highlightRanges.add(range);
	}

	public void clearHighlightRanges() {
		highlightRanges.clear();
	}

	public ArrayList<Range<Color>> getHighlightRanges() {
		return highlightRanges;
	}

	public void addGraph(String key, Graph graph, Side lr) {
		graphs[lr.getInt()].add(graph);
		key2Graph.put(key, graph);

		drawLegend(backgroundImage.createGraphics());

		redrawImage();
	}

	public void clearGraph(String key) {
		key2Graph.get(key).timedValues.clear();
	}

	public void clearGraphs() {
		verticalMarkers.clear();

		for (ArrayList<Graph> graph : graphs) {
			graph.clear();
		}
		key2Graph.clear();

		redrawImage();
	}

	private AlphaComposite makeComposite(float alpha) {
	 int type = AlphaComposite.SRC_OVER;
	 return(AlphaComposite.getInstance(type, alpha));
	}
	
	public synchronized void addValue(String key, long time, int value) {
		Graph graph = key2Graph.get(key);
		graph.timedValues.add(new Point(time, value));

		int n = graph.timedValues.size();
		if(n < 2) return;

		Point p1 = graph.timedValues.get(n-2);
		Point p2 = graph.timedValues.get(n-1);

		Graphics2D g = backgroundImage.createGraphics();
		//g.setComposite(makeComposite(0.4f));
		g.setPaint(graph.color);
		g.setStroke(graph.stroke);
		g.setRenderingHints(renderingHintsGraph);

		drawGraphLine(g, p1, p2, Side.left);

		g.setRenderingHints(renderingHintsNormal);
		g.setStroke(new BasicStroke());
		g.setColor(Color.BLACK);

		int x = (int) getX(p1.x);
		int width = (int) getX(p2.x) - x + 2;
		repaint(x, 0, width, backgroundImage.getHeight());
	}

	public synchronized void addVerticalMarker(Marker marker) {
		verticalMarkers.add(marker);
		drawVerticalMarker(marker);
	}

	public synchronized void removeVerticalMarker(Marker marker) {
		verticalMarkers.remove(marker);
	}

	public void paint(Graphics g) {
		super.paint(g);
		g.drawImage(backgroundImage,0,0,null);
	}

	public void redrawImage() {
		Graphics2D g = backgroundImage.createGraphics();
		int width = backgroundImage.getWidth();
		int height = backgroundImage.getHeight();

		g.setRenderingHints(renderingHintsNormal);
		g.setStroke(normalStroke);

		g.setBackground(Color.WHITE);
		g.setColor(Color.WHITE);
		g.fillRect(0,0,width,height);

		g.setColor(Color.BLACK);

		// draw highlights
		drawHighlights(g);

		// draw marker lines
		drawVerticalMarkers(g);

		// draw graph
		for(Side lr : new Side[] {Side.left, Side.right}) {
			for(Graph graph : graphs[lr.getInt()]) drawGraph(g, graph, lr);
		}
		drawLegend(g);

		// axis
		g.drawLine(margin.left, margin.top, margin.left, height - margin.bottom + crossSize);
		g.drawLine(width - margin.right, margin.top, width - margin.right, height - margin.bottom + crossSize);
		g.drawLine(margin.left - crossSize, height - margin.bottom, width - margin.right + crossSize, height - margin.bottom);

		// left axis markers
		drawVerticalAxisMarkers(g, Side.left, true);
		drawTimeAxisMarkers(g, true);

		repaint();
	}

	private void drawVerticalMarkers(Graphics2D g) {
		int height = backgroundImage.getHeight();

		Color oldColor = g.getColor();
		Stroke oldStroke = g.getStroke();
		for (Marker verticalMarker : verticalMarkers) {
			if (verticalMarker.color != null) g.setColor(verticalMarker.color);
			if (verticalMarker.stroke != null) g.setStroke(verticalMarker.stroke);
			long x = verticalMarker.x;
			g.draw(new Line2D.Float(getX(x), margin.top, getX(x), height - margin.bottom + crossSize));
		}
		g.setColor(oldColor);
		g.setStroke(oldStroke);
	}

	private void drawVerticalMarker(Marker verticalMarker) {
		int height = backgroundImage.getHeight();

		Graphics2D g = backgroundImage.createGraphics();

		Color oldColor = g.getColor();
		Stroke oldStroke = g.getStroke();
		if (verticalMarker.color != null) g.setColor(verticalMarker.color);
		if (verticalMarker.stroke != null) g.setStroke(verticalMarker.stroke);
		long x = verticalMarker.x;
		Line2D.Float line = new Line2D.Float(getX(x), margin.top, getX(x), height - margin.bottom + crossSize);
		g.draw(line);
		repaint(line.getBounds());
		g.setColor(oldColor);
		g.setStroke(oldStroke);
	}

	private float getDiagramX(long value) {
		int width = backgroundImage.getWidth();
		int diagramWidth = width - margin.left - margin.right;
		return diagramWidth * value / (float) (timeRange.max - timeRange.min);
	}

	public float getX(long value) {
		return margin.left + getDiagramX(value);
	}

	private float getDiagramY(long value, Side lr) {
		int height = backgroundImage.getHeight();
		int diagramHeight = height - margin.top - margin.bottom;
		return diagramHeight * value / (float) (valueRange[lr.getInt()].max - valueRange[lr.getInt()].min);
	}

	public float getY(long value, Side lr) {
		int height = backgroundImage.getHeight();
		return height - margin.bottom - getDiagramY(value, lr);
	}

	private long getMarkerValueDistance(Range range, int multiplesOf, int pxLength, int pxMinDistance) {
		double valueMinDistance = ((double) (range.max - range.min)) * pxMinDistance / pxLength;
		double factorf = valueMinDistance / multiplesOf;
		long factori = (long) factorf;
		return factori == factorf ? factori * multiplesOf : (factori+1) * multiplesOf;
	}

	private void drawHighlights(Graphics2D g) {
		int width = backgroundImage.getWidth();

		// horizontal lines
		for(Range<Color> range : highlightRanges) {
			int minY = (int) getY(range.max, Side.left);
			int maxY = (int) getY(range.min, Side.left);

			g.setColor(range.attribute);
			g.fillRect(margin.left, minY, width - margin.left - margin.right, maxY - minY);
		}
		g.setColor(Color.BLACK);
	}

	private void drawVerticalAxisMarkers(Graphics2D g, Side lr, boolean lines) {
		int width = backgroundImage.getWidth();
		int height = backgroundImage.getHeight();

		int x = lr == Side.left ? margin.left : width - margin.right;
		Range range = valueRange[lr.getInt()];
		int minY = margin.top;
		int maxY = height - margin.bottom;

		int markerValueDistance = (int) getMarkerValueDistance(range, 10, maxY - minY, 10);
		float markerPixelDistance = (maxY - minY) * markerValueDistance / (float) (range.max - range.min);

		// horizontal lines
		for(int markerNr = 1; lines && markerNr * markerPixelDistance <= maxY - minY; markerNr++) {
			int y = maxY - (int) (markerNr * markerPixelDistance);

			g.setColor(markerNr % 2 == 0 ? gridColorEven : gridColorOdd);
			g.setStroke(markerNr % 2 == 0 ? gridStrokeEven : gridStrokeOdd);
			g.drawLine(margin.left + 1, y, width - margin.right - 1, y);
		}
		g.setColor(Color.BLACK);
		g.setStroke(normalStroke);

		// markers and font
		FontMetrics fm = g.getFontMetrics();
		for(int markerNr = 1; markerNr * markerPixelDistance <= maxY - minY; markerNr ++) {
			int y = maxY - (int) (markerNr * markerPixelDistance);
			g.drawLine(x - markerSize, y, x + markerSize, y);

			String value = (range.min + markerNr * markerValueDistance) + "";
			Rectangle2D bounds = fm.getStringBounds(value, g);
			if(markerNr % 2 == 0) g.drawString(value, x - (int) bounds.getWidth() - markerSize - 3, y + (int) bounds.getHeight() / 2 - 1);
		}
	}

	private void drawTimeAxisMarkers(Graphics2D g, boolean lines) {
		int width = backgroundImage.getWidth();
		int height = backgroundImage.getHeight();

		int y = height - margin.bottom;
		Range range = timeRange;
		int minX = margin.left;
		int maxX = width - margin.right;

		long markerValueDistance;
		if (timeAxisType == TimeAxisType.minute) {
			markerValueDistance = getMarkerValueDistance(range, 60, maxX - minX, 10);
		} else {
			markerValueDistance = getMarkerValueDistance(range, 24*60*60*1000, maxX - minX, 40);
		}
		float markerPixelDistance = (long) (maxX - minX) * markerValueDistance / (float) (range.max - range.min);

		// vertical lines
		for(int markerNr = 1; lines && markerNr * markerPixelDistance <= maxX - minX; markerNr++) {
			int x = minX + (int) (markerNr * markerPixelDistance);

			g.setColor(markerNr % 2 == 0 ? gridColorEven : gridColorOdd);
			g.setStroke(markerNr % 2 == 0 ? gridStrokeEven : gridStrokeOdd);
			g.drawLine(x, margin.top + 1, x, height - margin.bottom - 1);
		}
		g.setColor(Color.BLACK);
		g.setStroke(normalStroke);

		// markers and font
		FontMetrics fm = g.getFontMetrics();
		for(int markerNr = 1; lines && markerNr * markerPixelDistance <= maxX - minX; markerNr++) {
			int x = minX + (int) (markerNr * markerPixelDistance);
			g.drawLine(x, y - markerSize, x, y + markerSize);

			String value;
			if (timeAxisType == TimeAxisType.minute) {
				value = (range.min + markerNr * markerValueDistance) / 60 + "";
			} else {
				Date date = new Date(range.min + (long) markerNr * markerValueDistance);
				value = dateFormat.format(date);
			}
			Rectangle2D bounds = fm.getStringBounds(value, g);
			if(markerNr % 2 == 0) g.drawString(value, x - (int) bounds.getWidth() / 2, y + (int) bounds.getHeight() + markerSize + 1);
		}
	}

	private void drawLegend(Graphics2D g) {
		int width = backgroundImage.getWidth();
		int height = backgroundImage.getHeight();

		int y = height - margin.bottom + 22;
		Range range = timeRange;
		int minX = margin.left;
		int maxX = width - margin.right;

		// font metrics
		FontMetrics fm = g.getFontMetrics();

		int spacing = 30;

		// determine total with of the legend
		int totalWidth = -10;
		for (ArrayList<Graph> graphs2 : graphs) {
			for (Graph graph : graphs2) {
				if (!graph.hideInLegend) {
					Rectangle2D bounds = fm.getStringBounds(graph.name, g);
					totalWidth += bounds.getWidth() + spacing;
				}
			}
		}

		// draw legend
		int x = minX + ((maxX-minX) - totalWidth)/2;
		for (ArrayList<Graph> graphs2 : graphs) {
			for (Graph graph : graphs2) {
				if (!graph.hideInLegend) {
					Rectangle2D bounds = fm.getStringBounds(graph.name, g);
					g.setColor(graph.color);
					g.setStroke(normalStroke);
					g.drawString(graph.name, x, y + (int) bounds.getHeight() + markerSize + 1);
					g.setColor(Color.BLACK);
					g.setStroke(normalStroke);

					x += bounds.getWidth() + spacing;
				}
			}
		}
	}

	private void drawGraph(Graphics2D g, Graph graph, Side lr) {
		g.setPaint(graph.color);
		g.setStroke(graph.stroke);
		g.setRenderingHints(renderingHintsGraph);

		Point p1 = null;
		for(Point p2 : graph.timedValues) {
			if(p1 != null) drawGraphLine(g, p1, p2, lr);
			p1 = p2;
		}

		g.setRenderingHints(renderingHintsNormal);
		g.setStroke(new BasicStroke());
		g.setColor(Color.BLACK);
	}

	private void drawGraphLine(Graphics2D g, Point p1, Point p2, Side lr) {
		g.draw(new Line2D.Float(getX(p1.x), getY(p1.y, lr), getX(p2.x), getY(p2.y, lr)));
	}

	public void componentResized(ComponentEvent e) {
		componentShown(e);
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
		backgroundImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		redrawImage();
	}

	public Range getTimeRange() {
		return timeRange;
	}

	public void setTimeRange(Range timeRange) {
		this.timeRange = timeRange;
	}

	public TimeAxisType getTimeAxisType() {
		return timeAxisType;
	}

	public void setTimeAxisType(TimeAxisType timeAxisType) {
		this.timeAxisType = timeAxisType;
	}

	public Range getValueRange(Side lr) {
		return valueRange[lr.getInt()];
	}

	public void setValueRange(Side lr, Range valueRange) {
		this.valueRange[lr.getInt()] = valueRange;
	}

	public void componentHidden(ComponentEvent e) {
	}

	/** Range with minimal and maximal value (and attribute). */
	public static class Range<T> {
		/** Minimal value of the range. */
		public long min;
		/** Maximal values of the range. */
		public long max;
		/** Attribute. */
		public T attribute = null;

		public Range(long min, long max) {
			this.min = min;
			this.max = max;
		}

		public Range(long min, long max, T attribute) {
			this(min,max);
			this.attribute = attribute;
		}
	}

	/** Margin with top, bottom, left, right. */
	public static class Margin {
		public int top, bottom, left, right;

		public Margin(int top, int bottom, int left, int right) {
			this.top = top;
			this.bottom = bottom;
			this.left = left;
			this.right = right;
		}
	}

	/** Graph consisting of point in time. */
	public static class Graph {
		public String name;
		public Color color;
		public Stroke stroke;
		public boolean hideInLegend;
		public ArrayList<Point> timedValues;

		public Graph(String name, Color color, Stroke stroke, boolean hideInLegend) {
			this.name = name;
			this.color = color;
			this.stroke = stroke;
			this.hideInLegend = hideInLegend;
			timedValues = new ArrayList<Point>();
		}
	}

	/** Point with long value for x and int value for y. */
	public class Point {
		private long x;
		private int y;

		public Point(long x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public static class Marker {
		private long x;
		public Color color;
		public Stroke stroke;
		private String msg;

		public Marker(long x, Color color, Stroke stroke, String msg) {
			this.x = x;
			this.color = color;
			this.stroke = stroke;
			this.msg = msg;
		}
	}
}
