package org.jergometer.control;

import org.jergometer.model.*;

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
	private int idleCounter = 0;

	public BikeProgram(File file, String programName, BikeProgramData programData) {
		super(file);
		this.programName = programName.replaceAll("\\\\", "/");
		this.programData = programData;
		eventIterator = programData.getEvents().iterator();
		nextEvent = eventIterator.next();
		doActions(nextEvent.getActions());
		nextEvent = eventIterator.hasNext() ? eventIterator.next() : null;
	}

	/**
	 * Adds the new data record to the session and returns true if user is not cycling anymore.
	 *
	 * @param dataRecord new data record
	 * @return true if user does not cycle anymore
	 */
	public UpdateStatus update(DataRecord dataRecord) {
		if(session.update(dataRecord)) {
			if(nextEvent != null && nextEvent.getTime() == session.getDuration()) {
				doActions(nextEvent.getActions());
				nextEvent = eventIterator.hasNext() ? eventIterator.next() : null;
			} else {
				subProgram.update(dataRecord);
			}
			idleCounter = 0;
			return UpdateStatus.cycle;
		} else {
			if (++idleCounter >= 4) {
				return UpdateStatus.pulse;
			} else {
				return UpdateStatus.idle;
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
