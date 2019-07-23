package snu.kdd.substring_syn.algorithm.index;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.RecordInterface;
import snu.kdd.substring_syn.utils.StatContainer;

public abstract class AbstractIndexBasedFilter {

	protected final double theta;
	protected final StatContainer statContainer;
	protected final boolean useCountFilter = true;

	public AbstractIndexBasedFilter( double theta, StatContainer statContainer ) {
		this.theta = theta;
		this.statContainer = statContainer;
	}

	public abstract ObjectSet<RecordInterface> querySideFilter( Record query );
	public abstract ObjectSet<Record> textSideFilter( Record query );
}
