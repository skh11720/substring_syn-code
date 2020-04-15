package snu.kdd.substring_syn.parallel;

import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;

import snu.kdd.substring_syn.algorithm.NaiveSearchTest;

public class ParallelNaiveSearchTest {

	@SuppressWarnings("rawtypes")
	@Test
	public void test() {
		Class[] cls = {NaiveSearchTest.class};
		JUnitCore.runClasses(new ParallelComputer(true, true), cls);
	}

}
