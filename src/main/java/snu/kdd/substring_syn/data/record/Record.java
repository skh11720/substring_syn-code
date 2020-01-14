package snu.kdd.substring_syn.data.record;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.Ruleset;
import snu.kdd.substring_syn.data.TokenIndex;
import snu.kdd.substring_syn.utils.Util;

public class Record implements RecordInterface, Comparable<Record> {
	
	public static final Record EMPTY_RECORD = new Record(new int[0]);
	public static TokenIndex tokenIndex = null;

	protected int id;
	protected int[] tokens;
	protected final int hash;

	Rule[][] applicableRules = null;
	Rule[][] suffixApplicableRules = null;
	int[][] transformLengths = null;
//	long[] estTrans;
	IntPair[][] suffixRuleLenPairs = null;

	int maxRhsSize = 0;
	
	public Record( int id, String str ) {
		this.id = id;
		String[] pstr = Records.tokenize(str);
		tokens = new int[ pstr.length ];
		for( int i = 0; i < pstr.length; ++i ) {
			tokens[ i ] = tokenIndex.getIDOrAdd( pstr[ i ] );
		}
		
		hash = getHash();
	}
	
	public Record( int id, int[] tokens ) {
		this.id = id;
		this.tokens = tokens;
		hash = getHash();
	}

	public Record( int[] tokens ) { // for transformed strings
		this(-1, tokens);
	}

	public int getID() {
		return id;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals( Object o ) {
		if( o != null ) {
			Record orec = (Record) o;
			if( this == orec ) return true;
			if( id == orec.id || id == -1 || orec.id == -1 ) {
				return Records.compare( tokens, orec.tokens ) == 0;
			}
			return false;
		}
		else return false;
	}

	public Collection<Integer> getTokens() {
		return IntArrayList.wrap(tokens);
	}

	public IntSet getDistinctTokens() {
		return new IntOpenHashSet(tokens);
	}

	public int[] getTokenArray() {
		return tokens;
	}

	public IntList getTokenList() {
		return IntArrayList.wrap(tokens);
	}

	public int getToken( int i ) {
		return tokens[i];
	}
	
	@Override
	public int getSidx() {
		return 0;
	}
	
	@Override
	public int compareTo( Record o ) {
		if( tokens.length != o.tokens.length ) {
			return tokens.length - o.tokens.length;
		}

		int idx = 0;
		while( idx < tokens.length ) {
			int cmp = Integer.compare( tokens[ idx ], o.tokens[ idx ] );
			if( cmp != 0 ) {
				return cmp;
			}
			++idx;
		}
		return 0;
	}

	public int size() {
		return tokens.length;
	}
	
	public Record getSuperRecord() {
		return this;
	}
	
	public int getNumApplicableRules() {
		int count = 0;
		for( int i = 0; i < applicableRules.length; ++i ) {
			for( Rule rule : applicableRules[ i ] ) {
				if( rule.isSelfRule ) {
					continue;
				}
				++count;
			}
		}
		return count;
	}

	public Iterable<Rule> getApplicableRuleIterable() {
		return new Iterable<Rule>() {
			@Override
			public Iterator<Rule> iterator() {
				return new RuleIterator();
			}
		};
	}

	public Rule[][] getApplicableRules() {
		return applicableRules;
	}
	
	public Iterable<Rule> getApplicableRules( int k ) {
		if( applicableRules == null ) {
			return null;
		}
		else if( k < applicableRules.length ) {
			return Arrays.asList(applicableRules[k]);
		}
		else {
			return Arrays.asList(Rule.EMPTY_RULE);
		}
	}

	public Iterable<Rule> getSuffixApplicableRules( int k ) {
		if( suffixApplicableRules == null ) {
			return null;
		}
		else if( k < suffixApplicableRules.length ) {
			return Arrays.asList(suffixApplicableRules[k]);
		}
		else {
			return Arrays.asList(Rule.EMPTY_RULE);
		}
	}
	
	public IntPair[] getSuffixRuleLens( int k ) {
		if ( suffixRuleLenPairs == null ) {
			return null;
		}
		else if ( k < suffixRuleLenPairs.length ) {
			return suffixRuleLenPairs[k];
		}
		else return null;
	}

	public int getMaxTransLength() {
		return transformLengths[ tokens.length - 1 ][ 1 ];
	}

	public int getMinTransLength() {
		return transformLengths[ tokens.length - 1 ][ 0 ];
	}

	@Override
	public int getMaxRhsSize() {
		if ( maxRhsSize == 0 ) {
			maxRhsSize = 1;
			for ( Rule rule : getApplicableRuleIterable() ) {
				maxRhsSize = Math.max(maxRhsSize, rule.rhsSize());
			}
		}
		return maxRhsSize;
	}
	
	public IntOpenHashSet getCandTokenSet() {
		IntOpenHashSet tokenSet = new IntOpenHashSet();
		for ( Rule r : getApplicableRuleIterable() ) {
			for ( int token : r.getRhs() ) tokenSet.add(token);
		}
		return tokenSet;
	}
	
	public Record getPartialRecord( int sidx, int eidx ) {
		Record newrec = new Record(getTokenList().subList(sidx, eidx).toIntArray());
		newrec.id = getID();
		newrec.applicableRules = new Rule[eidx-sidx][];
		for ( int i=sidx; i<eidx; ++i ) newrec.applicableRules[i-sidx] = this.applicableRules[i];
		newrec.suffixApplicableRules = new Rule[eidx-sidx][];
		for ( int i=sidx; i<eidx; ++i ) newrec.suffixApplicableRules[i-sidx] = this.suffixApplicableRules[i];
		newrec.transformLengths = null;
		newrec.suffixRuleLenPairs = new IntPair[eidx-sidx][];
		for ( int i=sidx; i<eidx; ++i ) newrec.suffixRuleLenPairs[i-sidx] = this.suffixRuleLenPairs[i];
		return newrec;
	}

	public void preprocessAll() {
		preprocessApplicableRules();
		preprocessSuffixApplicableRules();
		preprocessTransformLength();
	}
	
	public void preprocessApplicableRules() {
		if ( applicableRules != null ) return;
		applicableRules = Rule.automata.applicableRules(tokens);
	}

	public void preprocessSuffixApplicableRules() {
		if ( suffixApplicableRules != null ) return;
		ObjectList<ObjectList<Rule>> tmplist = new ObjectArrayList<ObjectList<Rule>>();
		ObjectList<ObjectSet<IntPair>> pairList = new ObjectArrayList<>();

		for( int i = 0; i < tokens.length; ++i ) {
			tmplist.add( new ObjectArrayList<Rule>() );
			pairList.add( new ObjectOpenHashSet<>() );
		}

		for( int i = tokens.length - 1; i >= 0; --i ) {
			for( Rule rule : applicableRules[ i ] ) {
				int suffixidx = i + rule.getLhs().length - 1;
				tmplist.get( suffixidx ).add( rule );
				pairList.get( suffixidx ).add( new IntPair(rule.lhsSize(), rule.rhsSize()) );
			}
		}

		suffixApplicableRules = new Rule[ tokens.length ][];
		suffixRuleLenPairs = new IntPair[ tokens.length ][];
		for( int i = 0; i < tokens.length; ++i ) {
			suffixApplicableRules[ i ] = tmplist.get( i ).toArray( new Rule[ 0 ] );
			suffixRuleLenPairs[i] = pairList.get(i).toArray( new IntPair[0] );
		}
	}

	protected void preprocessTransformLength() {
		if ( transformLengths != null ) return;
		transformLengths = new int[ tokens.length ][ 2 ];
		for( int i = 0; i < tokens.length; ++i )
			transformLengths[ i ][ 0 ] = transformLengths[ i ][ 1 ] = i + 1;

		for( Rule rule : applicableRules[ 0 ] ) {
			int fromSize = rule.lhsSize();
			int toSize = rule.rhsSize();
			if( fromSize > toSize ) {
				transformLengths[ fromSize - 1 ][ 0 ] = Math.min( transformLengths[ fromSize - 1 ][ 0 ], toSize );
			}
			else if( fromSize < toSize ) {
				transformLengths[ fromSize - 1 ][ 1 ] = Math.max( transformLengths[ fromSize - 1 ][ 1 ], toSize );
			}
		}
		for( int i = 1; i < tokens.length; ++i ) {
			transformLengths[ i ][ 0 ] = Math.min( transformLengths[ i ][ 0 ], transformLengths[ i - 1 ][ 0 ] + 1 );
			transformLengths[ i ][ 1 ] = Math.max( transformLengths[ i ][ 1 ], transformLengths[ i - 1 ][ 1 ] + 1 );
			for( Rule rule : applicableRules[ i ] ) {
				int fromSize = rule.lhsSize();
				int toSize = rule.rhsSize();
				if( fromSize > toSize ) {
					transformLengths[ i + fromSize - 1 ][ 0 ] = Math.min( transformLengths[ i + fromSize - 1 ][ 0 ],
							transformLengths[ i - 1 ][ 0 ] + toSize );
				}
				else if( fromSize < toSize ) {
					transformLengths[ i + fromSize - 1 ][ 1 ] = Math.max( transformLengths[ i + fromSize - 1 ][ 1 ],
							transformLengths[ i - 1 ][ 1 ] + toSize );
				}

			}
		}
	}
	
	public String toString() {
		StringBuilder rslt = new StringBuilder();
		for( int id : tokens ) {
			if( rslt.length() != 0 ) {
				rslt.append(" ");
			}
			rslt.append(id);
		}
		return rslt.toString();
	}

	public String toOriginalString() {
		StringBuilder rslt = new StringBuilder();
		for( int id : tokens ) {
			rslt.append(tokenIndex.getToken( id ) + " ");
		}
		return rslt.toString();
	}
	
	public String toStringDetails() {
		StringBuilder rslt = new StringBuilder();
		rslt.append("ID: "+id+"\n");
		rslt.append("rec: "+toOriginalString()+"\n");
		rslt.append("tokens: "+toString()+"\n");
		rslt.append("nRules: "+getNumApplicableRules()+"\n");
		for ( Rule rule : getApplicableRuleIterable() ) {
			if ( rule.isSelfRule ) continue;
			rslt.append("\t"+rule.toString()+"\t"+rule.toOriginalString()+"\n");
		}
		return rslt.toString();
	}

	private int getHash() {
		// djb2-like
		int hash = Util.bigprime + id;
		for( int token : tokens ) {
			hash = ( hash << 5 ) + Util.bigprime + token;
//                tmp = 0x1f1f1f1f ^ tmp + token;
//			hash = hash % Util.bigprime;
		}
		return (int) ( hash % Integer.MAX_VALUE );
	}
	
	public final byte[] serialize() throws IOException {
		IntArrayList list = new IntArrayList();
		list.add(id);
		list.add(size());
		for ( int i=0; i<size(); ++i ) list.add(tokens[i]);
		for ( int i=0; i<size(); ++i ) list.add(applicableRules[i].length);
		for ( int i=0; i<size(); ++i ) {
			for ( Rule rule : getApplicableRules(i) ) list.add(rule.getID());
		} 
		for ( int i=0; i<size(); ++i ) list.add(suffixApplicableRules[i].length);
		for ( int i=0; i<size(); ++i ) {
			for ( Rule rule : getSuffixApplicableRules(i) ) list.add(rule.getID());
		}
		for ( int i=0; i<size(); ++i ) list.add(suffixRuleLenPairs[i].length);
		for ( int i=0; i<size(); ++i ) {
			for ( IntPair pair : getSuffixRuleLens(i) ) {
				list.add(pair.i1);
				list.add(pair.i2);
			}
		}
		list.add(maxRhsSize);
		return Snappy.compress(list.toIntArray());
	}
	
	public static Record deserialize(byte[] buf, int len, Ruleset ruleset) throws IOException {
		int[] arr = Snappy.uncompressIntArray(buf, 0, len);
		IntIterator iter = IntArrayList.wrap(arr).iterator();
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

	
	
	class RuleIterator implements Iterator<Rule> {
		int k = 0;
		int i = 0;

		@Override
		public boolean hasNext() {
			return (k < applicableRules.length);
		}

		@Override
		public Rule next() {
			Rule rule = applicableRules[k][i++];
			if ( i >= applicableRules[k].length ) {
				++k;
				i = 0;
			}
			return rule;
		}
	}
}
