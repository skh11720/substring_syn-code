package snu.kdd.pkwise;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Test;

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
	
	@Test
	public void textWitvMapCorrectness() throws IOException {
		double theta = 0.6;
		int qlen = 5;
		int kmax = 3;
		WindowDataset dataset = TestUtils.getTestDataset();
		PkwiseSearch alg = new PkwiseSearch(theta, qlen, kmax);
		PkwiseIndex index = new PkwiseIndex(alg, dataset, qlen, theta);
		
		for ( Entry<Integer, ObjectList<WindowInterval>> e : index.getWitvMap().entrySet() ) {
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
	public void testWitvIterator() throws IOException {
		double theta = 0.6;
		int qlen = 5;
		int kmax = 3;
		WindowDataset dataset = TestUtils.getTestDataset();
		PkwiseSearch alg = new PkwiseSearch(theta, qlen, kmax);
		PkwiseIndex index = new PkwiseIndex(alg, dataset, qlen, theta);
		
		for ( int token=0; token<20; ++token ) {
			System.out.println(token+"\t"+Record.tokenIndex.getToken(token));
			System.out.println(index.getWitvMap().get(token));
			Iterator<Subrecord> iter = index.getWitvIterator(token);
			while ( iter.hasNext() ) {
				Subrecord window = iter.next();
				System.out.println(window.getID()+"\t"+window.getSidx()+"\t"+window.size());
			}
			System.out.println();
		}
	}

	@Test
	public void testTwitvIterator() throws IOException {
		double theta = 0.6;
		int qlen = 5;
		int kmax = 3;
		WindowDataset dataset = TestUtils.getTestDataset();
		PkwiseSearch alg = new PkwiseSearch(theta, qlen, kmax);
		PkwiseIndex index = new PkwiseIndex(alg, dataset, qlen, theta);
		
		for ( int token=0; token<20; ++token ) {
			System.out.println(token+"\t"+Record.tokenIndex.getToken(token));
			System.out.println(index.getTwitvMap().get(token));
			Iterator<Subrecord> iter = index.getTwitvIterator(token);
			while ( iter.hasNext() ) {
				Subrecord window = iter.next();
				System.out.println(window.getID()+"\t"+window.getSidx()+"\t"+window.size());
			}
			System.out.println();
		}
	}
}
