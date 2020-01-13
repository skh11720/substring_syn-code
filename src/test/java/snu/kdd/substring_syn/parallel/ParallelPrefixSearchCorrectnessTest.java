package snu.kdd.substring_syn.parallel;

import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;

import snu.kdd.substring_syn.algorithm.PrefixSearchCorrectnessTest;

public class ParallelPrefixSearchCorrectnessTest {

	@SuppressWarnings("rawtypes")
	@Test
	public void test() {
		Class[] cls = {PrefixSearchCorrectnessTest.class};
		JUnitCore.runClasses(new ParallelComputer(true, true), cls);
	}
}
