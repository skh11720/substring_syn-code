package snu.kdd.etc;

import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;

import snu.kdd.substring_syn.algorithm.PrefixSearchFilterPowerTest;

@SuppressWarnings("rawtypes")
public class ParallelTest {
	

	@Test
	public void runParameterizedTest() {
		Class[] cls = {ParameterizedTest.class};
		JUnitCore.runClasses(new ParallelComputer(true, true), cls);
	}

	@Test
	public void runPrefixSearchFilterPowerTest() {
		Class[] cls = {PrefixSearchFilterPowerTest.class};
		JUnitCore.runClasses(new ParallelComputer(true, true), cls);
	}
}
