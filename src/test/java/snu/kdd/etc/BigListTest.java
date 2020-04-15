package snu.kdd.etc;

import java.math.BigInteger;

import org.junit.Test;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

public class BigListTest {

	@Test
	public void test() {
		ObjectList<BigInteger> list = new ObjectArrayList<>();
		BigInteger n = new BigInteger("1");
		for ( int i=0; i<40; ++i ) {
			list.add(n);
			n = n.add(n);
		}
		list.stream().forEach(x->System.out.println(x));
	}
}
