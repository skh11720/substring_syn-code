package snu.kdd.etc;

import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;

public class ParallelTest {
	

	@SuppressWarnings("rawtypes")
	@Test
	public void test() {
		Class[] cls = {ParameterizedTest.class};
		JUnitCore.runClasses(new ParallelComputer(true, true), cls);
	}

}
