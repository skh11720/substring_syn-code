package snu.kdd.etc;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.record.Record;
import vldb18.PkduckDPEx;

@RunWith(Parameterized.class)
public class PkduckDPExTest {
	
	static Random rn;
	static final int nSample = 100;
	
	Param param;
	final String outputName;
	
	static class Param {
		double theta;
		String size;
		String name = "SPROT_long";
		
		public Param( double theta, String size ) {
			this.theta = theta;
			this.size = size;
		}
	}
	
	@Parameters
	public static Collection<Param> provideParams() {
		rn = new Random(0);
		ObjectList<Param> paramList = new ObjectArrayList<>();
		double[] thetaList = {1.0, 0.9, 0.8, 0.7, 0.6};
		String[] sizeList = {"100"};
		for ( double theta : thetaList ) {
			for ( String size : sizeList ) {
				paramList.add( new Param(theta, size) );
			}
		}
		return paramList;
	}
	
	public PkduckDPExTest( Param param ) {
		this.param = param;
		outputName = String.format("tmp/PkduckDPExTest_%.2f_%s.txt", param.theta, param.size);
	}

	@Test
	public void testCorrectness() throws IOException {
		Dataset dataset = DatasetFactory.createInstanceByName("SPROT_long", param.size);
		ObjectList<Record> searchedList = new ObjectArrayList<>(dataset.getSearchedList().iterator());
		ObjectList<Record> indexedList = new ObjectArrayList<>(dataset.getIndexedList().iterator());
		ObjectList<String> outputList = new ObjectArrayList<>();
		long ts = System.nanoTime();
		for ( int repeat=0; repeat<nSample; ++repeat ) {
			Record query = searchedList.get( rn.nextInt(searchedList.size()) );
			Record rec = indexedList.get( rn.nextInt(indexedList.size()) );
			IntSet candTokenSet = new IntOpenHashSet(query.getTokens());
			PkduckDPEx pkduckdp = new PkduckDPEx(query, rec, param.theta);
			for ( int token : candTokenSet ) {
				pkduckdp.compute(token);
				for ( int i=0; i<rec.size(); ++i ) {
					for ( int v=1; v<=rec.size()-i; ++v ) {
						boolean isInSigU = pkduckdp.isInSigU(i, v);
						outputList.add(String.format("%d %d %d %d %d %d", query.getIdx(), rec.getIdx(), token, i, v, isInSigU?1:0));
					}
				}
			}
		}
		
		if ( !(new File(outputName)).exists() ) saveOutput(outputList);
		ObjectList<String> answerList = loadOutput();
		for ( int i=0; i<answerList.size(); ++i ) {
			assertTrue( answerList.get(i).equals(outputList.get(i)));
		}

		double t = (System.nanoTime()-ts)/1e6;
		System.err.println(outputName+"\t"+t+" ms");
		PrintWriter pw = new PrintWriter( new FileOutputStream("tmp/PkduckDPExTest-TimeEfficiency.txt", true) );
		LocalDate date = LocalDate.now();
		pw.println(date.toString()+"\t"+outputName+"\t"+t);
		pw.close();
	}
	
	public void saveOutput( ObjectList<String> outputList ) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(outputName);
		for ( String output : outputList ) {
			pw.println(output);
		}
		pw.close();
	}

	public ObjectList<String> loadOutput() throws IOException {
		ObjectList<String> outputList = new ObjectArrayList<>();
		BufferedReader br = new BufferedReader( new FileReader(outputName) );
		Iterator<String> iter = br.lines().iterator();
		while ( iter.hasNext() ) {
			outputList.add(iter.next().trim());
		}
		br.close();
		return outputList;
	}
}
