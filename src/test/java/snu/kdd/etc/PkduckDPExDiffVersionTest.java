package snu.kdd.etc;

import java.io.IOException;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.substring_syn.algorithm.filter.TransSetBoundCalculator3;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.Util;
import vldb18.PkduckDPEx;
import vldb18.PkduckDPEx2;
import vldb18.PkduckDPEx3;

public class PkduckDPExDiffVersionTest {
	
	private interface PkduckDPExInterface {
		public void init( Record query, Record rec, double theta );
		public void compute( int target );
		public boolean isInSigU( int i, int v );
	}
	
	private class PkduckDPExBaseWrapper implements PkduckDPExInterface {
		
		PkduckDPEx pkduckdp;

		@Override
		public void init(Record query, Record rec, double theta) {
			pkduckdp = new PkduckDPEx(query, rec, theta);
		}

		@Override
		public void compute(int target) {
			pkduckdp.compute(target);
			
		}

		@Override
		public boolean isInSigU(int i, int v) {
			return pkduckdp.isInSigU(i, v);
		}
	}
	
	private class PkduckDPExWrapper extends PkduckDPExBaseWrapper {
	}
	
	private class PkduckDPEx2Wrapper extends PkduckDPExBaseWrapper {
		
		TransSetBoundCalculator3 boundCalculator;
		
		@Override
		public void init(Record query, Record rec, double theta) {
			boundCalculator = new TransSetBoundCalculator3(null, rec, theta);
			pkduckdp = new PkduckDPEx2(query, rec, boundCalculator, theta);
		}
	}
	
	private class PkduckDPEx3Wrapper extends PkduckDPEx2Wrapper {
		
		@Override
		public void init(Record query, Record rec, double theta) {
			boundCalculator = new TransSetBoundCalculator3(null, rec, theta);
			pkduckdp = new PkduckDPEx3(query, rec, boundCalculator, theta);
		}
	}

	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};

	@Test
	public void testEfficiency() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", "100");
		TokenOrder order = new TokenOrder(dataset);
		long ts;
		dataset.reindexByOrder(order);
		PkduckDPExInterface[] pkduckdpArr = new PkduckDPExInterface[3];
		pkduckdpArr[0] = new PkduckDPExWrapper();
		pkduckdpArr[1] = new PkduckDPEx2Wrapper();
		pkduckdpArr[2] = new PkduckDPEx3Wrapper();
		long[] tArr = new long[pkduckdpArr.length];

		for ( double theta : thetaList ) {
			for ( Record query : dataset.searchedList ) {
				for ( Record rec :  dataset.indexedList ) {
					System.out.println("rec: "+rec.getID());
					IntOpenHashSet tokenSet = rec.getCandTokenSet();
					for ( int j=0; j<pkduckdpArr.length; ++j ) {
						ts = System.nanoTime();
						pkduckdpArr[j].init(query, rec, theta);
						tArr[j] += System.nanoTime() - ts;
					}

					for ( int target : tokenSet ) {
						for ( int j=0; j<pkduckdpArr.length; ++j ) {
							ts = System.nanoTime();
							pkduckdpArr[j].compute(target);
							tArr[j] += System.nanoTime() - ts;
						}
					}
				}
				break;
			}
		}
		for ( long t : tArr ) System.out.println(t/1e6);
	}

}
