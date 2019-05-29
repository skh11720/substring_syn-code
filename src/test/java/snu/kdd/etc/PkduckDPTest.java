package snu.kdd.etc;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.Util;
import vldb18.PkduckDP;

public class PkduckDPTest {
	
	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};

	@Test
	public void test() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT", 10000);
		TokenOrder order = new TokenOrder(dataset);
		long ts;
		dataset.reindexByOrder(order);
		long[] tArr = new long[1];
		for ( double theta : thetaList ) {
			for ( Record rec : dataset.searchedList ) {
				PkduckDP pkduckDP0 = new PkduckDP(rec, theta);
				IntOpenHashSet tokenSet = getCandTokenSet(rec);
				IntOpenHashSet prefix = Util.getPrefix(rec, theta);
				
				for ( int token : tokenSet ) {
					ts = System.nanoTime();
					boolean inSig0 = pkduckDP0.isInSigU(token);
					tArr[0] += System.nanoTime()-ts;
					assertEquals(prefix.contains(token), inSig0);
				}
			}
		}
		for ( long t : tArr ) System.out.println(t/1e6);
	}

	public void outputAnswer( Dataset dataset, double theta ) throws FileNotFoundException {
		String path = String.format("tmp/PkduckDPTest_Answer_%s_%.2f.txt", dataset.dataInfo.datasetName, theta);
		PrintStream ps = new PrintStream(path);
		for ( Record rec : dataset.searchedList ) {
			PkduckDP pkduckDP = new PkduckDP(rec, theta);
			IntOpenHashSet tokenSet = getCandTokenSet(rec);
			
			for ( int token : tokenSet ) {
				int rid = rec.getID();
				boolean inSig = pkduckDP.isInSigU(token);
				ps.println(rid+"\t"+token+"\t"+(inSig?1:0));
			}
		}
		ps.close();
	}
	
	private IntOpenHashSet getCandTokenSet( Record rec ) {
		IntOpenHashSet tokenSet = new IntOpenHashSet();
		for ( Rule r : rec.getApplicableRuleIterable() ) {
			tokenSet.addAll(IntArrayList.wrap(r.getRhs()));
		}
		return tokenSet;
	}
	
	public ObjectArrayList<int[]> loadAnswer( Dataset dataset, double theta ) throws IOException {
		String path = String.format("tmp/PkduckDPTest_Answer_%s_%.2f.txt", dataset.dataInfo.datasetName, theta);
		BufferedReader br = new BufferedReader( new FileReader(path) );
		ObjectArrayList<int[]> answerList = new ObjectArrayList<>();
		br.lines().forEach(s -> {
			String[] token = s.split("\t");
			int[] arr = Arrays.stream(token).mapToInt(Integer::parseInt).toArray();
			answerList.add(arr);
		});
		br.close();
		return answerList;
	}
}
