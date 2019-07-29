package snu.kdd.etc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.iterator.SortedRecordSlidingWindowIterator;
import vldb18.PkduckDP;
import vldb18.PkduckDPEx;
import vldb18.old.PkduckDPExOld;

public class PkduckDPExTest {
	
	private interface PkduckDPInterface {
		void init( Record rec, double theta );
		void compute( int target );
		boolean isInSigU( RecordInterface rec, int target, int widx, int w );
	}
	
	private class PkduckDPWrapper implements PkduckDPInterface {
		
		PkduckDP obj;
		double theta;
		
		@Override
		public void init(Record rec, double theta) {
			this.theta = theta;
		}

		@Override
		public void compute(int target) {
		}

		@Override
		public boolean isInSigU( RecordInterface rec, int target, int widx, int w) {
			obj = new PkduckDP(rec, theta);
			return obj.isInSigU(target);
		}
		
	}
	
	private class PkduckDPExOldWrapper implements PkduckDPInterface {
		
		PkduckDPExOld obj;

		@Override
		public void init(Record rec, double theta) {
			obj = new PkduckDPExOld(rec, theta);
		}
		
		@Override
		public void compute(int target) {
			obj.compute(target);
		}

		@Override
		public boolean isInSigU( RecordInterface rec, int target, int widx, int w) {
			return obj.isInSigU(widx, w);
		}
	}
	
	private class PkduckDPExWrapper implements PkduckDPInterface {

		PkduckDPEx obj;

		@Override
		public void init(Record rec, double theta) {
			obj = new PkduckDPEx(rec, rec, theta);
		}
		
		@Override
		public void compute(int target) {
			obj.compute(target);
		}

		@Override
		public boolean isInSigU( RecordInterface rec, int target, int widx, int w) {
			return obj.isInSigU(widx, w);
		}
	}
	
	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};

	@Ignore
	public void test() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT", "1000");
		long ts;
		PkduckDPInterface[] pkduckdpArr = new PkduckDPInterface[3];
		pkduckdpArr[0] = new PkduckDPWrapper();
		pkduckdpArr[1] = new PkduckDPExOldWrapper();
		pkduckdpArr[2] = new PkduckDPExWrapper();
		long[] tArr = new long[pkduckdpArr.length];

		for ( double theta : thetaList ) {
			for ( Record rec :  dataset.indexedList ) {
				System.out.println("rec: "+rec.getID());
				IntOpenHashSet tokenSet = rec.getCandTokenSet();
				for ( int j=0; j<pkduckdpArr.length; ++j ) {
					ts = System.nanoTime();
					pkduckdpArr[j].init(rec, theta);
					tArr[j] += System.nanoTime() - ts;
				}

				for ( int target : tokenSet ) {
					for ( int j=0; j<pkduckdpArr.length; ++j ) {
						ts = System.nanoTime();
						pkduckdpArr[j].compute(target);
						tArr[j] += System.nanoTime() - ts;
					}

					for ( int w=1; w<=rec.size(); ++w ) {
						SortedRecordSlidingWindowIterator window = new SortedRecordSlidingWindowIterator(rec, w, theta);
						for ( int widx=0; window.hasNext(); ++widx ) {
							Subrecord wrec = window.next();
							IntOpenHashSet prefix = Util.getExpandedPrefix(wrec.toRecord(), theta);

							for ( int j=0; j<pkduckdpArr.length; ++j ) {
								ts = System.nanoTime();
								boolean inSig = pkduckdpArr[j].isInSigU(wrec, target, widx, w);
								tArr[j] += System.nanoTime() - ts;

								try {
									assertEquals(inSig, prefix.contains(target));
								}
								catch ( AssertionError e ) {
									System.err.println(j);
									System.err.println(theta);
									System.err.println(wrec);
									System.err.println(prefix);
									System.err.println(target);
									throw e;
								}
							}
						}
					}
				}
			}
		}
		for ( long t : tArr ) System.out.println(t/1e6);
	}
	
	@Test
	public void testPair() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", "100");
		double theta = 0.6;
		Record qrec = dataset.searchedList.get(2);
		Record rec = dataset.indexedList.get(31);
		System.out.println("qrec: "+qrec);
		IntOpenHashSet tokenSet = rec.getCandTokenSet();
		PkduckDPEx pkduckdp = new PkduckDPEx(qrec, rec, theta);
		for ( int target : tokenSet ) {
			pkduckdp.compute(target);
			for ( int w=15; w<=rec.size(); ++w ) {
				SortedRecordSlidingWindowIterator window = new SortedRecordSlidingWindowIterator(rec, w, theta);
				for ( int widx=0; window.hasNext(); ++widx ) {
					Subrecord wrec = window.next();
					IntOpenHashSet prefix = Util.getExpandedPrefix(wrec.toRecord(), theta);
					System.out.println("target: "+target);
					System.out.println("w: "+w+", widx: "+widx);
					System.out.println("window: "+wrec.toString());
					System.out.println("window_prefix: "+prefix);
					System.out.println("pdduck: "+pkduckdp.isInSigU(widx, w));
				}
				break;
			}
		}
	}
}
