package org.jergometer.model;

import junit.framework.TestCase;

/**
 * Some tests for BikeProgramData.
 */
public class BikeProgramDataTest extends TestCase {
	public void testTimeDefinitions() {
		assertEquals(60, BikeProgramData.getTime("60"));
		assertEquals(60, BikeProgramData.getTime("60s"));
		assertEquals(60, BikeProgramData.getTime("1m"));
		assertEquals(60, BikeProgramData.getTime("1'"));
		assertEquals(60, BikeProgramData.getTime("1:0"));
		assertEquals(60, BikeProgramData.getTime("1:00"));
		assertEquals(60, BikeProgramData.getTime("1.0m"));
		assertEquals(60, BikeProgramData.getTime("1,0m"));
		assertEquals(90, BikeProgramData.getTime("1,5m"));
		assertEquals(90, BikeProgramData.getTime("1m30"));
		assertEquals(90, BikeProgramData.getTime("1m30s"));
		assertEquals(90, BikeProgramData.getTime("01:30"));
		assertEquals(90, BikeProgramData.getTime("0:01:30"));
		assertEquals(90, BikeProgramData.getTime("0h01m30"));
		assertEquals(3661, BikeProgramData.getTime("1:1:1"));
		assertEquals(3661, BikeProgramData.getTime("1h1m1"));
	}
}
