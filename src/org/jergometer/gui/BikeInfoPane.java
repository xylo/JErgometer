package org.jergometer.gui;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;

/**
 * Template based info pane showing current training statistics.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class BikeInfoPane extends JEditorPane implements HyperlinkListener {
	private MainWindow mainWindow;
	private Template template;

	private Dimension maxMinimumSize = new Dimension(0, 0);

	public BikeInfoPane(MainWindow mainWindow, Template template) {
		this.mainWindow = mainWindow;
		this.template = template;
		setEditable(false);
		setDoubleBuffered(true);
		addHyperlinkListener(this);

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent componentEvent) {
				System.out.println(1);
				setDoubleBuffered(true);
			}
		});
	}

	public void setTemplate(Template template) {
		this.template = template;
	}

	@Override
	public void hyperlinkUpdate(HyperlinkEvent event) {
		if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			try {
				String action = event.getDescription();
				mainWindow.bikeInfoPaneAction(action);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void setContext(VelocityContext context) {
		final Writer writer = new StringWriter();
		context.put("bgColor", Integer.toHexString(getBackground().getRGB() & 0x00ffffff));
		template.merge(context, writer);
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					setText(writer.toString());
				}
			});
		} catch (Exception ignored) {
		}
	}

	public void resetValues() {
		VelocityContext context = new VelocityContext();
		context.put("programAction", "-");
		context.put("timeString", "-");
		context.put("destPowerString", "-");
		context.put("actPowerString", "-");
		context.put("pulseString", "-");
		context.put("pedalRpmString", "-");
		context.put("speedString", "-");
		context.put("distanceString", "-");
		context.put("energyString", "-");
		setContext(context);
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension minimumSize = super.getMinimumSize();

		maxMinimumSize.height = Math.max(maxMinimumSize.height, minimumSize.height);
		maxMinimumSize.width = Math.max(maxMinimumSize.width, minimumSize.width);

		return maxMinimumSize;
	}
}
