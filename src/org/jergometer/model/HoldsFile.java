package org.jergometer.model;

import java.io.File;

/**
 * Container that holds a file.
 */
public class HoldsFile {
	private File file;

	public HoldsFile(File file) {
		this.file = file;
	}

	// getters and setters

	public File getFile() {
		return file;
	}
}
