package snu.kdd.etc;

import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;

import snu.kdd.substring_syn.algorithm.RSSearchFilterPowerTest;

@SuppressWarnings("rawtypes")
public class ParallelTest {
	

	@Test
	public void runParameterizedTest() {
		Class[] cls = {ParameterizedTest.class};
		JUnitCore.runClasses(new ParallelComputer(true, true), cls);
	}

	@Test
	public void runRSSearchFilterPowerTest() {
		Class[] cls = {RSSearchFilterPowerTest.class};
		JUnitCore.runClasses(new ParallelComputer(true, true), cls);
	}
}
