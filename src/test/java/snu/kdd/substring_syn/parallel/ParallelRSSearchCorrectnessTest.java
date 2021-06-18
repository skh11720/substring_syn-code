package snu.kdd.substring_syn.parallel;

import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;

import snu.kdd.substring_syn.algorithm.RSSearchCorrectnessTest;

public class ParallelRSSearchCorrectnessTest {

	@SuppressWarnings("rawtypes")
	@Test
	public void test() {
		Class[] cls = {RSSearchCorrectnessTest.class};
		JUnitCore.runClasses(new ParallelComputer(true, true), cls);
	}
}
