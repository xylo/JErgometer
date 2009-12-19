package org.jergometer.model;

import org.jergometer.control.BikeProgram;

/**
 * Filter for bike sessions.
 */
public class BikeSessionFilter {
	public enum Type { off, program, programDir }

	private Type type = Type.off;
	private BikeProgram bikeProgram;
	private BikeProgramDir programDir;
	private boolean onlyCompletedSessions;

	public void setProgramFilter(BikeProgram bikeProgram) {
		this.bikeProgram = bikeProgram;
		type = Type.program;
	}

	public void setProgramDirFilter(BikeProgramDir programDir) {
		this.programDir = programDir;
		type = Type.programDir;
	}

	public void deactivateFilter() {
		type = Type.off;
	}

	public boolean match(BikeSession bikeSession) {
		if (!onlyCompletedSessions || bikeSession.isCompleted()) {
			switch (type) {
				case off:
					return true;
				case program:
					return bikeSession.getProgramName().equals(bikeProgram.getProgramName());
				case programDir:
					return bikeSession.getProgramName().startsWith(programDir.getPath().replaceAll("\\\\", "/") + '/');
				default:
					return false;
			}
		} else {
			return false;
		}
	}


	// getters and setters

	public Type getType() {
		return type;
	}

	public BikeProgram getBikeProgram() {
		return bikeProgram;
	}

	public BikeProgramDir getProgramDir() {
		return programDir;
	}

	public boolean isOnlyCompletedSessions() {
		return onlyCompletedSessions;
	}

	public void setOnlyCompletedSessions(boolean onlyCompletedSessions) {
		this.onlyCompletedSessions = onlyCompletedSessions;
	}
}
