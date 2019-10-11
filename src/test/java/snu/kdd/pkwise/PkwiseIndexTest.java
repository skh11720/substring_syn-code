package snu.kdd.pkwise;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Ignore;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;

public class PkwiseIndexTest {

	@Test
	public void visualizeWitvMap() throws IOException {
		double theta = 0.6;
		int qlen = 5;
		int kmax = 3;
		WindowDataset dataset = TestUtils.getTestDataset();
		PkwiseSearch alg = new PkwiseSearch(theta, qlen, kmax);
		PkwiseIndex index = new PkwiseIndex(alg, dataset, qlen, theta);
		for ( Entry<Integer, ObjectList<WindowInterval>> e : index.getWitvMap().entrySet() ) {
			int tokenIdx = e.getKey();
			String token = Record.tokenIndex.getToken(tokenIdx);
			ObjectList<WindowInterval> list = e.getValue();
			if ( token.equals("as") ) {
				System.out.println(token);
				for ( WindowInterval witv : list ) {
					System.out.println(witv);
				}
			}
		}
	}

	@Ignore
	public void testBuildIndex() throws IOException {
		double theta = 0.6;
		int qlen = 5;
		int kmax = 3;
		WindowDataset dataset = TestUtils.getTestDataset();
		PkwiseSearch alg = new PkwiseSearch(theta, qlen, kmax);
		PkwiseIndex index = new PkwiseIndex(alg, dataset, qlen, theta);
		index.writeToFile();
	}
	
	@Test
	public void testWitvMapCorrectness() throws IOException {
		double theta = 0.6;
		int qlen = 5;
		int kmax = 3;
		WindowDataset dataset = TestUtils.getTestDataset();
		PkwiseSearch alg = new PkwiseSearch(theta, qlen, kmax);
		Int2ObjectMap<ObjectList<WindowInterval>> map = PkwiseIndexBuilder.buildTok2WitvMap(alg, dataset, qlen);
		
		for ( Entry<Integer, ObjectList<WindowInterval>> e : map.entrySet() ) {
			int token = e.getKey();
			ObjectList<WindowInterval> list = e.getValue();
			for ( WindowInterval witv : list ) {
				int rid = witv.rid;
				int w = witv.w;
				int sidx = witv.sidx;
				int eidx = witv.eidx;
				
				Record rec = dataset.getRecord(rid);
				for ( int idx=sidx; idx<eidx; ++idx ) {
					Subrecord window = new Subrecord(rec, idx, idx+w);
					assertTrue(window.getTokenList().contains(token));
				}
			}
		}
	}

	@Test
	public void testTwitvMapCorrectness() throws IOException {
		double theta = 0.6;
		int qlen = 5;
		int kmax = 3;
		WindowDataset dataset = TestUtils.getTestDataset();
		Int2ObjectMap<ObjectList<WindowInterval>> map = PkwiseIndexBuilder.buildTok2TwitvMap(dataset, qlen, theta);
		
		for ( Entry<Integer, ObjectList<WindowInterval>> e : map.entrySet() ) {
			int token = e.getKey();
			ObjectList<WindowInterval> list = e.getValue();
			for ( WindowInterval twitv : list ) {
				int rid = twitv.rid;
				int w = twitv.w;
				int sidx = twitv.sidx;
				int eidx = twitv.eidx;

//				System.out.println(Record.tokenIndex.getToken(token)+"\t"+token+"\t"+rid+"\t"+sidx+"\t"+eidx+"\t"+w);
				Record rec = dataset.getRecord(rid);
				for ( int idx=sidx; idx<eidx; ++idx ) {
					Subrecord window = new Subrecord(rec, idx, idx+w);
					assertTrue(window.getTokenList().contains(token));
				}
			}
		}
	}
}
