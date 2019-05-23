package snu.kdd.etc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.substring_syn.data.Query;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.RecordSortedSlidingWindow;
import vldb18.PkduckDP;
import vldb18.PkduckDPEx;

public class PkduckDPExTest {
	
	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};

	@Test
	public void test() throws IOException {
		Query query = Util.getQueryWithPreprocessing("SPROT", 1000);
		TokenOrder order = new TokenOrder(query);
		long ts;
		query.reindexByOrder(order);
		long[] tArr = new long[2];
		for ( double theta : thetaList ) {
			for ( Record rec :  query.indexedSet ) {
				System.out.println("rec: "+rec.getID());
				IntOpenHashSet tokenSet = getCandTokenSet(rec);
				ts = System.nanoTime();
				PkduckDPEx pkduckDP1 = new PkduckDPEx(rec, theta);
				tArr[1] += System.nanoTime() - ts;
				for ( int target : tokenSet ) {
					ts = System.nanoTime();
					pkduckDP1.compute(target);
					tArr[1] += System.nanoTime() - ts;

					for ( int w=1; w<=rec.size(); ++w ) {
						RecordSortedSlidingWindow window = new RecordSortedSlidingWindow(rec, w, theta);
						for ( int widx=0; window.hasNext(); ++widx ) {
							Subrecord wrec = window.next();
							IntOpenHashSet prefix = Util.getPrefix(wrec.toRecord(query.ruleSet.automata), theta);

							ts = System.nanoTime();
							PkduckDP pkduckDP0 = new PkduckDP(wrec, theta);
							boolean inSig0 = pkduckDP0.isInSigU(target);
							tArr[0] += System.nanoTime() - ts;
							ts = System.nanoTime();
							boolean inSig1 = pkduckDP1.isInSigU(widx, w);
							tArr[1] += System.nanoTime() - ts;
							try {
								assertEquals(prefix.contains(target), inSig0);
								assertEquals(prefix.contains(target), inSig1);
							}
							catch ( AssertionError e ) {
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
		for ( long t : tArr ) System.out.println(t/1e6);
	}

	private IntOpenHashSet getCandTokenSet( Record rec ) {
		IntOpenHashSet tokenSet = new IntOpenHashSet();
		for ( Rule r : rec.getApplicableRuleIterable() ) {
			tokenSet.addAll(IntArrayList.wrap(r.getRhs()));
		}
		return tokenSet;
	}
}
