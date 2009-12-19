package org.jergometer.control;

import org.jergometer.model.BikeProgramData;
import org.jergometer.model.BikeSession;
import org.jergometer.model.DataRecord;
import org.jergometer.model.HoldsFile;

import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;

/**
 * Controls the power of the ergometer. It may use any input
 * data of the ergometer (e.g. pulse).
 */
public class BikeProgram extends HoldsFile {
	private int power = 0;
	private String programName;
	private BikeProgramData programData;
	private BikeSession session;
	private SubProgram subProgram;
	private Iterator<BikeProgramData.TimeEvent> eventIterator;
	private BikeProgramData.TimeEvent nextEvent;

	public BikeProgram(File file, String programName, BikeProgramData programData) {
		super(file);
		this.programName = programName.replaceAll("\\\\", "/");
		this.programData = programData;
		eventIterator = programData.getEvents().iterator();
		nextEvent = eventIterator.next();
		doActions(nextEvent.getActions());
		nextEvent = eventIterator.hasNext() ? eventIterator.next() : null;
	}

	public void update(DataRecord dataRecord) {
		if(session.update(dataRecord)) {
			if(nextEvent != null && nextEvent.getTime() == session.getDuration()) {
				doActions(nextEvent.getActions());
				nextEvent = eventIterator.hasNext() ? eventIterator.next() : null;
			} else {
				subProgram.update(dataRecord);
			}
		}
	}

	private void doActions(ArrayList<BikeProgramData.Action> actions) {
		BikeProgramData.Action currentAction;

		for (BikeProgramData.Action action : actions) {
			doAction(action);

			currentAction = action;
		}
	}

	private void doAction(BikeProgramData.Action action) {
		if(action.getType() == BikeProgramData.Action.Type.power) {
			power = action.getValue();
			subProgram = new SubProgram.Power(session, power);
		}
		else if(action.getType() == BikeProgramData.Action.Type.pulse) {
			int pulse = action.getValue();
			subProgram = new SubProgram.Pulse(session, power, pulse);
		}
	}

	public int getPower() {
		power = subProgram.getPower();
		return power;
	}

	public String getProgramName() {
		return programName;
	}

	public BikeProgramData getProgramData() {
		return programData;
	}

	public BikeSession newSession() {
		return session = new BikeSession(programName, programData.getDuration());
	}

	public BikeSession getSession() {
		return session;
	}

	public String toString() {
		return programData.getName();
	}
}
