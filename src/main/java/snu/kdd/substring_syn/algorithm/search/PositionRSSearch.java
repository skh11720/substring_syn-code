package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.filter.TransLenLazyCalculator;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.RecordWithEndpoints;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.IntRange;
import snu.kdd.substring_syn.utils.ReturnStatus;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class PositionRSSearch extends RSSearch {
	
	public PositionRSSearch( double theta, boolean bLF, boolean bPF, IndexChoice indexChoice ) {
		super(theta, bLF, bPF, indexChoice);
	}

	@Override
	protected void searchRecordQuerySide( Record query, RecordInterface rec ) {
		IntRange wRange = getWindowSizeRangeQuerySide(query, rec);
		int sidx = ((RecordWithEndpoints)rec).getStartPoint();
		IntList epList = ((RecordWithEndpoints)rec).getEndpoints();

		for ( int eidx : epList ) {
			Subrecord window = new Subrecord(rec, sidx, eidx);
			IntCollection wprefix = Util.getPrefix(window, theta);
			ReturnStatus status = searchWindowQuerySide(query, window, wRange, wprefix);
			if (status == ReturnStatus.Continue ) continue;
			else if (status == ReturnStatus.Break ) break;
			else if (status == ReturnStatus.Terminate ) return;
		}
	}
	
	@Override
	protected void searchRecordTextSideWithPrefixFilter( Record query, TransformableRecordInterface rec ) {
		statContainer.startWatch("Time_TS_searchRecordPF.getCandTokenList");
		IntList candTokenList = getCandTokenList(query, rec, modifiedTheta);
		statContainer.stopWatch("Time_TS_searchRecordPF.getCandTokenList");
		int widx = ((RecordWithEndpoints)rec).getStartPoint();
		IntList eidxList = ((RecordWithEndpoints)rec).getEndpoints();
		transLenCalculator = new TransLenLazyCalculator(statContainer, rec, widx, eidxList.getInt(eidxList.size()-1), modifiedTheta);
		PkduckDPExIncremental pkduckdp = new PkduckDPExIncrementalOpt(query, rec, modifiedTheta);
		ObjectSet<IntPair> verifiedWindowSet = new ObjectOpenHashSet<>();
		
		for ( int target : candTokenList ) {
			statContainer.startWatch("Time_TS_searchRecordPF.setTarget");
			pkduckdp.setTarget(target);
			statContainer.stopWatch("Time_TS_searchRecordPF.setTarget");
			statContainer.startWatch("Time_TS_searchRecordPF.initPkduck");
			pkduckdp.init();
			statContainer.stopWatch("Time_TS_searchRecordPF.initPkduck");
			int j = 0;
			while ( j < eidxList.size() && eidxList.get(j) <= widx ) ++j;
			for ( int w=1; w<=rec.size()-widx; ++w ) {
				if ( bLF && applyLengthFilterTextSide(query, widx, w) == ReturnStatus.Break ) break;
				statContainer.startWatch("Time_TS_searchRecordPF.pkduck");
				pkduckdp.compute(widx+1, w);
				statContainer.stopWatch("Time_TS_searchRecordPF.pkduck");

				if ( j >= eidxList.size() ) break;
				if ( eidxList.get(j) != widx+w ) continue;
				++j;
				
				if ( verifiedWindowSet.contains(new IntPair(widx, w)) ) continue;
				if ( !pkduckdp.isInSigU(widx, w) ) continue;
				verifiedWindowSet.add(new IntPair(widx, w));
				statContainer.addCount(Stat.Len_TS_PF, w);
				if ( verifyTextSideWrapper(query, rec, widx, w) == ReturnStatus.Terminate ) return;
			}
		}
	}
	
	@Override
	protected void searchRecordTextSideWithoutPrefixFilter( Record query, TransformableRecordInterface rec ) {
		int widx = ((RecordWithEndpoints)rec).getStartPoint();
		IntList eidxList = ((RecordWithEndpoints)rec).getEndpoints();
		transLenCalculator = new TransLenLazyCalculator(statContainer, rec, widx, eidxList.getInt(eidxList.size()-1), modifiedTheta);
		int j = 0;
		while ( j < eidxList.size() && eidxList.get(j) <= widx ) ++j;
		for ( int w=1; w<=rec.size()-widx; ++w ) {
			if ( j >= eidxList.size() ) break;
			if ( eidxList.get(j) != widx+w ) continue;
			++j;
			if ( bLF && applyLengthFilterTextSide(query, widx, w) == ReturnStatus.Break ) break;
			if ( verifyTextSideWrapper(query, rec, widx, w) == ReturnStatus.Terminate ) return;
		}
	}
}
