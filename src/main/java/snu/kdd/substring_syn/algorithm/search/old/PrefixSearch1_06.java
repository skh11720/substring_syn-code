package snu.kdd.substring_syn.algorithm.search.old;

import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.RecordInterface;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.IntRange;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.SortedWindowExpander;

public class PrefixSearch1_06 extends PrefixSearch1_05 {
	
	/*
	 * Use length filtering in the text-side transformation.
	 * Use TransSetBoundCalculator5. 
	 * Use PkduckDPEx3.
	 * Use lbmono in the length filtering.
	 */

	public PrefixSearch1_06( double theta ) {
		super(theta);
	}

	@Override
	protected void searchRecordQuerySide( Record query, RecordInterface rec ) {
		Log.log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Num_QS_WindowSizeAll, Util.sumWindowSize(rec));
		IntSet expandedPrefix = getExpandedPrefix(query);
		IntRange wRange = getWindowSizeRangeQuerySide(query, rec);
		Log.log.debug("wRange=(%d,%d)", wRange.min, wRange.max);
		for ( int widx=0; widx<rec.size(); ++widx ) {
			SortedWindowExpander witer = new SortedWindowExpander(rec, widx, theta);
			while ( witer.hasNext() ) {
				Subrecord window = witer.next();
				if ( witer.getSetSize() < wRange.min || witer.getSetSize() > wRange.max ) continue;
				int w = window.size();
				statContainer.addCount(Stat.Num_QS_WindowSizeLF, w);
				IntSet wprefix = witer.getPrefix();
				if (Util.hasIntersection(wprefix, expandedPrefix)) {
					statContainer.addCount(Stat.Num_QS_WindowSizeVerified, w);
					statContainer.startWatch(Stat.Time_3_Validation);
					boolean isSim = validator.isSimx2yOverThreahold(query, window.toRecord(), theta);
					statContainer.stopWatch(Stat.Time_3_Validation);
					statContainer.increment(Stat.Num_QS_Verified);
					if (isSim) {
						rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
						Log.log.debug("rsltFromQuery.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->window.size(), ()->window.sidx);
						return;
					}
				}
			}
		}
	}

	@Override
	public String getVersion() {
		return "1.06";
	}
}
