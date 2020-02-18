package snu.kdd.etc;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

@RunWith(Parameterized.class)
public class ParameterizedTest {
	
	private int n;
	private int m;
	private static String outputPath = "./tmp/ParameterizedTest";
	private static PrintStream ps = null;
	static {
		try {
			ps = new PrintStream(outputPath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	@Parameters
	public static Collection<ParamInput> provideParams() {
		ObjectList<ParamInput> paramList = new ObjectArrayList<>();
		for ( int i=0; i<10; ++i ) {
			for ( int j=0; j<10; ++j ) {
				paramList.add(new ParamInput(i, j));
			}
		}
		return paramList;
	}

	public ParameterizedTest( ParamInput input ) {
		this.n = input.n;
		this.m = input.m;
	}

	@Test
	public void test1() throws InterruptedException {
		System.out.println( String.format("start test1(%d, %d)", n, m) );
		ps.println( String.format("start test1(%d, %d)", n, m) );
		long ts = System.nanoTime();
		int a = 0;
		while (true) {
			if (System.nanoTime() - ts > 7e9) break;
			else a = a*n + m;
		}
		System.out.println( String.format("finish test1(%d, %d)", n, m) );
		ps.println( String.format("finish test1(%d, %d)", n, m) );
	}

	@Test
	public void test2() throws InterruptedException {
		System.out.println( String.format("start test2(%d, %d)", n, m) );
		ps.println( String.format("start test2(%d, %d)", n, m) );
		long ts = System.nanoTime();
		int a = 0;
		while (true) {
			if (System.nanoTime() - ts > 11e9) break;
			else a = a*n + m;
		}
		System.out.println( String.format("finish test2(%d, %d)", n, m) );
		ps.println( String.format("finish test2(%d, %d)", n, m) );
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
