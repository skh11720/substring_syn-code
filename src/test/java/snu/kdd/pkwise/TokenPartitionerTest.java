package snu.kdd.pkwise;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.substring_syn.data.record.Record;

public class TokenPartitionerTest {

	@SuppressWarnings("unused")
	@Test
	public void test() throws IOException {
		double theta = 0.6;
		int qlen = 5;
		int kmax = 5;
		WindowDataset dataset = TestUtils.getTestWindowDataset();
		TokenPartitioner partitioner = new TokenPartitioner(kmax);
		System.out.println(partitioner);
		
		for ( int i=0; i<Record.tokenIndex.getMaxID(); i+=1000 ) {
			System.out.println(i+"\t"+partitioner.getTokenClass(i));
		}
	}

}
