package snu.kdd.pkwise;

import java.io.IOException;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.data.WindowDataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Util;

public class PkwiseSignatureGeneratorTest {

	@Test
	public void visualize() throws IOException {
		double theta = 0.6;
		int kmax = 3;
		WindowDataset dataset = TestUtils.getTestWindowDataset();
		KwiseSignatureMap sigMap = new KwiseSignatureMap();
		TokenPartitioner partitioner = new TokenPartitioner(kmax);
		PkwiseSignatureGenerator pksiggen = new PkwiseSignatureGenerator(partitioner, sigMap, kmax);
		
		for ( int i=0; i<10; ++i ) {
			Record rec = dataset.getRecord(i);
			int maxDiff = Util.getPrefixLength(rec, theta);
			int l = pksiggen.getPrefixLength(rec, maxDiff);
			IntArrayList sig = pksiggen.genSignature(rec, maxDiff, true);
			System.out.println("rec="+rec);
			System.out.println("rec.size="+rec.size()+"\tprefixLen="+l);
			System.out.println("maxDiff="+maxDiff+"\tsig="+sig);
		}
		
		System.out.println(sigMap);
	}
}
