package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.RecordSortedSlidingWindowIterator;
import vldb18.NaivePkduckValidator;
import vldb18.PkduckDP;
import vldb18.PkduckDPEx;

public class PrefixSearch extends AbstractSearch {

	final NaivePkduckValidator validator;

	
	public PrefixSearch( double theta ) {
		super(theta);
		validator = new NaivePkduckValidator();
	}

	@Override
	protected void searchQuerySide( Record query, Record rec ) {
		log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Num_WindowSizeAll, Util.sumWindowSize(rec));
		IntSet expandedPrefix = getExpandedPrefix(query);
		int wMax = getMaxWindowSize(query, rec);
		for ( int w=1; w<=wMax; ++w ) {
			RecordSortedSlidingWindowIterator witer = new RecordSortedSlidingWindowIterator(rec, w, theta);
			while ( witer.hasNext() ) {
				statContainer.increment(Stat.Num_VerifiedWindowSize);
				Subrecord window = witer.next();
				IntSet wprefix = witer.getPrefix();
				if (Util.hasIntersection(wprefix, expandedPrefix)) {
					statContainer.startWatch(Stat.Time_1_Validation);
					boolean isSim = validator.isSimx2yOverThreahold(query, window.toRecord(), theta);
					statContainer.stopWatch(Stat.Time_1_Validation);
					statContainer.increment(Stat.Num_VerifyQuerySide);
					if (isSim) {
						rsltFromQuery.add(new IntPair(query.getID(), rec.getID()));
						log.debug("rsltFromQuery.add(%d, %d)", ()->query.getID(), ()->rec.getID());
						return;
					}
				}
			}
		}
	}

	protected IntSet getExpandedPrefix( Record query ) {
		IntSet candTokenSet = query.getCandTokenSet();
		IntSet expandedPrefix = new IntOpenHashSet();
		PkduckDP pkduckdp = new PkduckDP(query, theta);
		for ( int target : candTokenSet ) {
			if ( pkduckdp.isInSigU(target) ) expandedPrefix.add(target);
		}
		return expandedPrefix;
	}
	
	protected int getMaxWindowSize( Record query, Record rec ) {
		int maxSize = (int)Math.floor(query.getMaxTransLength()/theta);
		maxSize = Math.min(maxSize, rec.size());
		return maxSize;
	}
	
	@Override
	protected void searchTextSide( Record query, Record rec ) {
		log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Num_WindowSizeAll, Util.sumWindowSize(rec));
		IntList candTokenList = getCandTokenList(query, rec);
		PkduckDPEx pkduckdp = new PkduckDPEx(rec, theta, query.size());
		for ( int target : candTokenList ) {
			long ts0 = System.nanoTime();
			pkduckdp.compute(target);
			log.trace("PkduckDPEx.compute(%d) rec.id=%d  %.3f ms", target, rec.getID(), (System.nanoTime()-ts0)/1e6);
			ts0 = System.nanoTime();
			boolean isSimilar = applyPrefixFiltering(query, rec, pkduckdp, target);
			if ( isSimilar ) break;
			log.trace("PrefixSearch.applyPrefixFiltering(...)  %4d/%4d  %.3f ms", 0, rec.size()*(rec.size()+1)/2, (System.nanoTime()-ts0)/1e6 );
		}
	}
	
	protected IntList getCandTokenList( Record query, Record rec ) {
		IntSet tokenSet = rec.getCandTokenSet();
		tokenSet.retainAll(Util.getPrefix(query, theta));
		return new IntArrayList( tokenSet.stream().sorted().iterator() );
	}

	protected boolean applyPrefixFiltering( Record query, Record rec, PkduckDPEx pkduckdp, int target ) {
		for ( int widx=0; widx<rec.size(); ++widx ) {
			log.trace("widx: %d  maxWindowSize: %d", widx, pkduckdp.getMaxWindowSize(widx));
			for ( int w=1; w<=pkduckdp.getMaxWindowSize(widx); ++w ) {
				statContainer.increment(Stat.Num_VerifiedWindowSize);
				boolean isInSigU = pkduckdp.isInSigU(widx, w);
				log.trace("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, target=%d, ...)  widx=%d/%d  w=%d/%d  isInSigU=%s", query.getID(), rec.getID(), target, widx, rec.size()-1, w, pkduckdp.getMaxWindowSize(widx), isInSigU );
				if ( isInSigU ) {
					Subrecord window = new Subrecord(rec, widx, widx+w);
					statContainer.startWatch(Stat.Time_1_Validation);
					boolean isSim = validator.isSimx2yOverThreahold(window.toRecord(), query, theta);
					statContainer.stopWatch(Stat.Time_1_Validation);
					statContainer.increment(Stat.Num_VerifyTextSide);
					if (isSim) {
						rsltFromText.add(new IntPair(query.getID(), rec.getID()));
						log.debug("rsltFromText.add(%d, %d)", ()->query.getID(), ()->rec.getID());
						return true;
					}
				}
			}
		}
		log.trace("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, target=%d, ...)  isInSigU=false", query.getID(), rec.getID(), target);
		return false;
	}

	@Override
	public String getName() {
		return "PrefixSearch";
	}

	@Override
	public String getVersion() {
		return "1.00";
	}
}
