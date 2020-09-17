package snu.kdd.etc;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class FileSizeTest {

	@Test
	public void test() {
		File f = new File("src/test/java/snu/kdd/etc/FileSizeTest.java");
		System.out.println(f.exists());
		System.out.println(FileUtils.sizeOfAsBigInteger(f));
	}
}
