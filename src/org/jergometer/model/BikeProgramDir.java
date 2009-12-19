package org.jergometer.model;

import java.io.File;

/**
 * BikeProgramDir.
 */
public class BikeProgramDir extends HoldsFile {
	private String name;
	private String path;

	public BikeProgramDir(File file, String name, String path) {
		super(file);
		this.name = name;
		this.path = path;
	}

	// getters and setters

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public String toString() {
		return name;
	}
}
