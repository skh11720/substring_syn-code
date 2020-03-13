package snu.kdd.substring_syn.data.record;

import java.io.IOException;

import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.Ruleset;
import snu.kdd.substring_syn.utils.Log;

public class RecordSerializer {
	
	public static byte[] bbuf = new byte[100];
	public static int[] ibuf = new int[100];
	public static int blen;
	public static int ilen;

	public static final void shallowSerialize(Record rec) throws IOException {
		ilen = 0;
		addToIbuf(rec.getIdx());
		for ( int i=0; i<rec.size(); ++i ) addToIbuf(rec.tokens[i]);
		setBbuf(Snappy.maxCompressedLength(ilen*Integer.BYTES));
		blen = Snappy.rawCompress(ibuf, 0, ilen*Integer.BYTES, bbuf, 0);
	}

	public static final void serialize(Record rec) throws IOException {
		ilen = 0;

		addToIbuf(rec.idx);
		addToIbuf(rec.size());
		for ( int i=0; i<rec.size(); ++i ) addToIbuf(rec.tokens[i]);
		for ( int i=0; i<rec.size(); ++i ) addToIbuf(rec.applicableRules[i].length);
		for ( int i=0; i<rec.size(); ++i ) {
			for ( Rule rule : rec.getApplicableRules(i) ) addToIbuf(rule.getID());
		} 
		for ( int i=0; i<rec.size(); ++i ) addToIbuf(rec.suffixApplicableRules[i].length);
		for ( int i=0; i<rec.size(); ++i ) {
			for ( Rule rule : rec.getSuffixApplicableRules(i) ) addToIbuf(rule.getID());
		}
		for ( int i=0; i<rec.size(); ++i ) addToIbuf(rec.suffixRuleLenPairs[i].length);
		for ( int i=0; i<rec.size(); ++i ) {
			for ( IntPair pair : rec.getSuffixRuleLens(i) ) {
				addToIbuf(pair.i1);
				addToIbuf(pair.i2);
			}
		}
		addToIbuf(rec.getMaxRhsSize());
		setBbuf(Snappy.maxCompressedLength(ilen*Integer.BYTES));
		blen = Snappy.rawCompress(ibuf, 0, ilen*Integer.BYTES, bbuf, 0);
	}

	private static void addToIbuf(int val) {
		if ( ilen >= ibuf.length ) {
			Log.log.trace("RecordSerializer.ibuf is increased: "+ibuf.length+" -> "+(2*ibuf.length));
			int[] newbuf = new int[2*ibuf.length];
			for ( int i=0; i<ibuf.length; ++i ) newbuf[i] = ibuf[i];
			ibuf = newbuf;
		}
		ibuf[ilen] = val;
		ilen += 1;
	}
	
	private static final void setBbuf(int size) {
		while ( size > bbuf.length ) {
			Log.log.trace("RecordSerializer.bbuf is increased: "+bbuf.length+" -> "+(2*bbuf.length));
			bbuf = new byte[2*bbuf.length];
		}
	}
	
	public static final Record deserialize(byte[] buf, int offset, int len, Ruleset ruleset) {
		int numbytes = -1;
		while (true) {
			try {
//			numbytes = Snappy.uncompressedLength(buf, offset, len);
				numbytes = Snappy.rawUncompress(buf, offset, len, ibuf, 0);
				break;
			} catch ( IOException e ) {
				setIbuf(ibuf.length+1);
			}
		}
		IntIterator iter = IntArrayList.wrap(ibuf, numbytes/Integer.BYTES).iterator();
		int id = iter.nextInt();
		int size = iter.nextInt();
		int[] tokens = new int[size];
		for ( int i=0; i<size; ++i ) tokens[i] = iter.nextInt();
		Rule[][] applicableRules = new Rule[size][];
		for ( int i=0; i<size; ++i ) applicableRules[i] = new Rule[iter.next()];
		for ( int i=0; i<size; ++i ) {
			for ( int j=0; j<applicableRules[i].length; ++j ) 
				applicableRules[i][j] = ruleset.getRule(iter.nextInt());
		}
		Rule[][] suffixApplicableRules = new Rule[size][];
		for ( int i=0; i<size; ++i ) suffixApplicableRules[i] = new Rule[iter.next()];
		for ( int i=0; i<size; ++i ) {
			for ( int j=0; j<suffixApplicableRules[i].length; ++j ) 
				suffixApplicableRules[i][j] = ruleset.getRule(iter.nextInt());
		}
		IntPair[][] suffixRuleLenPairs = new IntPair[size][];
		for ( int i=0; i<size; ++i ) suffixRuleLenPairs[i] = new IntPair[iter.next()];
		for ( int i=0; i<size; ++i ) {
			for ( int j=0; j<suffixRuleLenPairs[i].length; ++j ) 
				suffixRuleLenPairs[i][j] = new IntPair(iter.nextInt(), iter.nextInt());
		}
		int maxRhsSize = iter.nextInt();
		Record rec = new Record(id, tokens);
		rec.applicableRules = applicableRules;
		rec.suffixApplicableRules = suffixApplicableRules;
		rec.suffixRuleLenPairs = suffixRuleLenPairs;
		rec.maxRhsSize = maxRhsSize;
		return rec;
	}

	private static final void setIbuf(int size) {
		while ( size > ibuf.length ) {
			Log.log.trace("RecordSerializer.ibuf is increased: "+ibuf.length+" -> "+(2*ibuf.length));
			ibuf = new int[2*ibuf.length];
		}
	}
}
