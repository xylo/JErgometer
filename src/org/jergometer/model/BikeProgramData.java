package org.jergometer.model;

import de.endrullis.xml.XMLElement;

import java.util.ArrayList;

/**
 * Bike program data used by BikeProgram.
 *
 * @see org.jergometer.control.BikeProgram
 */
public class BikeProgramData {
	private String name;
	private int duration;
	private ArrayList<TimeEvent> events = new ArrayList<TimeEvent>();

	public BikeProgramData(XMLElement rootElement) {
		fromXML(rootElement);
	}

	public void fromXML(XMLElement rootElement) {
		name = rootElement.getAttribute("name");
		duration = getTime(rootElement.getAttribute("duration"));

		XMLElement timeEventsElement = rootElement.getChildElement("timeevents");
		if(timeEventsElement != null) {
			for (XMLElement timeEventElement : timeEventsElement.getChildElements()) {
				if (timeEventElement.getName().equals("timeevent")) {
					events.add(new TimeEvent(timeEventElement));
				}
			}
		}
	}

	public XMLElement toXML() {
		return null; // TODO
	}


	// getters and setters
	public String getName() {
		return name;
	}

	public int getDuration() {
		return duration;
	}

	public ArrayList<TimeEvent> getEvents() {
		return events;
	}

	public static int getTime(String timeString) {
		timeString = timeString.replaceAll(",", ".");
		String h = null, m = null, s = null;
		if (timeString.indexOf(':') >= 0) {
			String[] parts = timeString.split(":");
			switch (parts.length) {
				case 3:
					h = parts[0];
					m = parts[1];
					s = parts[2];
					break;
					case 2:
						m = parts[0];
						s = parts[1];
						break;
					case 1:
						s = parts[0];
			}
		} else {
			if (timeString.indexOf('h') >= 0) {
				String[] parts = timeString.split("h");
				h = parts[0];
					timeString = parts.length == 2 ? parts[1] : "0";
			}
			if (timeString.indexOf('m') >= 0) {
				String[] parts = timeString.split("m");
				m = parts[0];
				timeString = parts.length == 2 ? parts[1] : "0";
			}
			if (timeString.indexOf('\'') >= 0) {
				String[] parts = timeString.split("'");
				m = parts[0];
				timeString = parts.length == 2 ? parts[1] : "0";
			}
			s = timeString.replace("s", "");
		}

		int time = 0;
		if (h != null) {
			time += (int) (3600 * Double.parseDouble(h));
		}
		if (m != null) {
			time += (int) (60 * Double.parseDouble(m));
		}
		if (s != null) {
			time += Integer.parseInt(s);
		}

		return time;
	}


	// inner classes
	public static class TimeEvent {
		protected int time;
		protected ArrayList<Action> actions = new ArrayList<Action>();

		public TimeEvent(XMLElement element) {
			fromXML(element);
		}

		public void fromXML(XMLElement element) {
			String timeString = element.getAttribute("time");
			time = BikeProgramData.getTime(timeString);

			for (XMLElement actionElement : element.getChildElements()) {
				if(actionElement.getName().equals("action")) {
					actions.add(new Action(actionElement));
				}
			}
		}

		// getters
		public int getTime() {
			return time;
		}

		public ArrayList<Action> getActions() {
			return actions;
		}
	}


	public static class Action {
		public enum Type {power, pulse}

		private Type type;
		private int value;

		public Action(int value) {
			this.value = value;
		}

		public Action(XMLElement element) {
			fromXML(element);
		}

		public void fromXML(XMLElement element) {
			type = Type.valueOf(element.getAttribute("type"));
			value = Integer.parseInt(element.getAttribute("value"));
		}

		// getters
		public Type getType() {
			return type;
		}

		public int getValue() {
			return value;
		}
	}
}
