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
		duration = getValue(GetTime.inst, rootElement.getAttribute("duration"), 0);

		LastState lastState = new LastState();

		XMLElement timeEventsElement = rootElement.getChildElement("timeevents");
		if(timeEventsElement != null) {
			for (XMLElement timeEventElement : timeEventsElement.getChildElements()) {
				if (timeEventElement.getName().equals("timeevent")) {
					events.add(new TimeEvent(timeEventElement, lastState));
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



	private static int getValue(IntFunction1<String> f, String string, int lastValue) {
		int sign = 0;

		if (string.startsWith("+")) {
			sign = 1;
			string = string.substring(1);
		} else
		if (string.startsWith("-")) {
			sign = -1;
			string = string.substring(1);
		}

		if (sign == 0) {
			return f.apply(string);
		} else {
			return lastValue + sign * f.apply(string);
		}
	}
	


	public static interface IntFunction1<T> {
		public int apply(String s);
	}

	public static class GetTime implements IntFunction1<String> {
		public static final GetTime inst = new GetTime();

		public int apply(String timeString) {
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
	}
	public static class GetInt implements IntFunction1<String> {
		public static final GetInt inst = new GetInt();

		public int apply(String s) {
			return Integer.parseInt(s);
		}
	}



	// inner classes
	private class LastState {
		int time = 0;
		int power = 0;
		int pulse = 0;

		public int getLast(Action.Type type) {
			switch (type) {
				case power:
					return power;
				case pulse:
					return pulse;
			}
			return -1;
		}

		public void setLast(Action.Type type, int value) {
			switch (type) {
				case power:
					power = value;
					return;
				case pulse:
					pulse = value;
			}
		}
	}

	public static class TimeEvent {
		protected int time;
		protected ArrayList<Action> actions = new ArrayList<Action>();

		public TimeEvent(XMLElement element, LastState lastState) {
			fromXML(element, lastState);
		}

		public void fromXML(XMLElement element, LastState lastState) {
			time = BikeProgramData.getValue(GetTime.inst, element.getAttribute("time"), lastState.time);
			lastState.time = time;

			for (XMLElement actionElement : element.getChildElements()) {
				if(actionElement.getName().equals("action")) {
					actions.add(new Action(actionElement, lastState));
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

		public Action(XMLElement element, LastState lastState) {
			fromXML(element, lastState);
		}

		public void fromXML(XMLElement element, LastState lastState) {
			type = Type.valueOf(element.getAttribute("type"));
			value = BikeProgramData.getValue(GetInt.inst, element.getAttribute("value"), lastState.getLast(type));
			lastState.setLast(type, value);
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
