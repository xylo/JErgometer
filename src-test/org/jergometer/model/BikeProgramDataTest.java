package org.jergometer.model;

import junit.framework.TestCase;

/**
 * Some tests for BikeProgramData.
 */
public class BikeProgramDataTest extends TestCase {
	public void testTimeDefinitions() {
		assertEquals(60, BikeProgramData.GetTime.inst.apply("60"));
		assertEquals(60, BikeProgramData.GetTime.inst.apply("60s"));
		assertEquals(60, BikeProgramData.GetTime.inst.apply("1m"));
		assertEquals(60, BikeProgramData.GetTime.inst.apply("1'"));
		assertEquals(60, BikeProgramData.GetTime.inst.apply("1:0"));
		assertEquals(60, BikeProgramData.GetTime.inst.apply("1:00"));
		assertEquals(60, BikeProgramData.GetTime.inst.apply("1.0m"));
		assertEquals(60, BikeProgramData.GetTime.inst.apply("1,0m"));
		assertEquals(90, BikeProgramData.GetTime.inst.apply("1,5m"));
		assertEquals(90, BikeProgramData.GetTime.inst.apply("1m30"));
		assertEquals(90, BikeProgramData.GetTime.inst.apply("1m30s"));
		assertEquals(90, BikeProgramData.GetTime.inst.apply("01:30"));
		assertEquals(90, BikeProgramData.GetTime.inst.apply("0:01:30"));
		assertEquals(90, BikeProgramData.GetTime.inst.apply("0h01m30"));
		assertEquals(3661, BikeProgramData.GetTime.inst.apply("1:1:1"));
		assertEquals(3661, BikeProgramData.GetTime.inst.apply("1h1m1"));
	}
}
