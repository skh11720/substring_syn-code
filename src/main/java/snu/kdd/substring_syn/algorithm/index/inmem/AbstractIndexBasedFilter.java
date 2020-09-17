package snu.kdd.substring_syn.algorithm.index.inmem;

import java.math.BigInteger;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.StatContainer;

public abstract class AbstractIndexBasedFilter {

	protected final Dataset dataset;
	protected final double theta;
	protected final StatContainer statContainer;

	public AbstractIndexBasedFilter( Dataset dataset, double theta, StatContainer statContainer ) {
		this.dataset = dataset;
		this.theta = theta;
		this.statContainer = statContainer;
	}

	public abstract long invListSize();
	public abstract long transInvListSize();
	public abstract int getNumInvFault();
	public abstract int getNumTinvFault();
	public abstract BigInteger diskSpaceUsage();

	protected abstract IntIterable querySideFilter( Record query );
	protected abstract IntIterable textSideFilter( Record query );

	public Iterable<TransformableRecordInterface> getCandRecordsQuerySide( Record query ) {
		IntIterable intIter = querySideFilter(query);
		return getCandRecords(intIter);
	}

	public Iterable<TransformableRecordInterface> getCandRecordsTextSide( Record query ) {
		IntIterable intIter = textSideFilter(query);
		return getCandRecords(intIter);
	}

	protected String visualizeCandRecord( Record rec, IntList idxList ) {
		StringBuilder strbld = new StringBuilder();
		for ( int i=0, j=0; i<rec.size(); ++i ) {
			if ( j < idxList.size() && i == idxList.get(j) ) {
				strbld.append("O");
				++j;
			}
			else strbld.append('-');
		}
		return idxList.size()+"\t"+strbld.toString();
	}

	protected String visualizeCandRecord( IntSet candTokenSet, Record rec ) {
		StringBuilder strbld = new StringBuilder();
		int count = 0;
		for ( int token : rec.getTokens()) {
			if ( candTokenSet.contains(token) ) {
				strbld.append("O");
				++count;
			}
			else strbld.append('-');
		}
		return count+"\t"+strbld.toString();
	}

	private Iterable<TransformableRecordInterface> getCandRecords( IntIterable intIter ) {
		return new Iterable<TransformableRecordInterface>() {
			
			@Override
			public Iterator<TransformableRecordInterface> iterator() {
				return getRecordIterator(intIter);
			}
		};
	}
	
	private Iterator<TransformableRecordInterface> getRecordIterator( IntIterable intIter ) {
		return new Iterator<TransformableRecordInterface>() {
			
			IntIterator iter = intIter.iterator();

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public TransformableRecordInterface next() {
				return dataset.getRecord(iter.nextInt());
			}
		};
	}
}
