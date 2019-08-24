package snu.kdd.substring_syn.algorithm.index.inmem;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
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
	public abstract ObjectSet<Record> querySideFilter( Record query );
	public abstract ObjectSet<Record> textSideFilter( Record query );

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
}
