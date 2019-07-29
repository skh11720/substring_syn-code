package snu.kdd.etc;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ParameterizedTest {
	
	private int n;
	private int m;
	
	@Parameters
	public static Collection<ParamInput> provideParams() {
		return Arrays.asList( new ParamInput[] {
				new ParamInput(2, 3), new ParamInput(-1, 5), new ParamInput(-3, -3),
				new ParamInput(2, 3), new ParamInput(-1, 5), new ParamInput(-3, -3),
				new ParamInput(2, 3), new ParamInput(-1, 5), new ParamInput(-3, -3),
				new ParamInput(2, 3), new ParamInput(-1, 5), new ParamInput(-3, -3),
				} );
	}

	public ParameterizedTest( ParamInput input ) {
		this.n = input.n;
		this.m = input.m;
	}

	@Test
	public void test1() throws InterruptedException {
		System.out.println("before1"+n*m);
		System.out.println(n*m);
		Thread.sleep(10000);
		System.out.println("after1"+n*m);

	}

	@Test
	public void test2() throws InterruptedException {
		System.out.println("before2"+n*m);
		System.out.println(n*m);
		Thread.sleep(10000);
		System.out.println("after2"+n*m);
	}
	
	
	static class ParamInput {
		int n;
		int m;
		
		public ParamInput( int n, int m ) {
			this.n = n;
			this.m = m;
		}
	}
}
