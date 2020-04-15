package snu.kdd.etc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Test;

import snu.kdd.substring_syn.data.DatasetInfo;

public class buildWiki0Dataset {

	@Test
	public void test() throws IOException {
		int qidx = 23;
		int sidx = 287;
		String name = "WIKI_3";
		String size = "10000";
		
		BufferedReader br;
		PrintStream ps;
		String line;
		
		br = new BufferedReader(new FileReader(DatasetInfo.getSearchedPath(name, size)));
		ps = new PrintStream(DatasetInfo.getSearchedPath("WIKI_0", "0"));
		for ( int i=0; (line = br.readLine()) != null; ++i ) {
			if ( qidx == i ) {
				ps.println(line);
				break;
			}
		}
		br.close();
		ps.close();

		br = new BufferedReader(new FileReader(DatasetInfo.getIndexedPath(name)));
		ps = new PrintStream(DatasetInfo.getIndexedPath("WIKI_0"));
		for ( int i=0; (line = br.readLine()) != null; ++i ) {
			if ( sidx == i ) {
				ps.println(line);
				break;
			}
		}
		br.close();
		ps.close();
	}
}
