package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.Util;
import vldb18.NaivePkduckValidator;
import vldb18.PkduckDPEx;

public class PrefixSearch extends AbstractSearch {

	final NaivePkduckValidator validator;

	
	public PrefixSearch( double theta ) {
		super(theta);
		validator = new NaivePkduckValidator();
	}
	
	@Override
	protected void searchRecordFromQuery( Record query, Record rec ) {
	}
	
	@Override
	protected void searchRecordFromText( Record query, Record rec ) {
		IntList candTokenList = getCandTokenList(query, rec);
		log.debug("PrefixSearch.getCandTokenList(%d, %d)  candTokenSet.size=%d", query.getID(), rec.getID(), candTokenList.size());
		PkduckDPEx pkduckdp = new PkduckDPEx(rec, theta, query.size());
		for ( int target : candTokenList ) {
			long ts0 = System.nanoTime();
			pkduckdp.compute(target);
			log.debug("PkduckDPEx.compute(%d) rec.id=%d  %.3f ms", target, rec.getID(), (System.nanoTime()-ts0)/1e6);
			ts0 = System.nanoTime();
			boolean isSimilar = applyPrefixFiltering(query, rec, pkduckdp, target);
			if ( isSimilar ) break;
			log.debug("PrefixSearch.applyPrefixFiltering(...)  %4d/%4d  %.3f ms", 0, rec.size()*(rec.size()+1)/2, (System.nanoTime()-ts0)/1e6 );
		} // end for
		
	}
	
	protected IntList getCandTokenList( Record query, Record rec ) {
		IntSet tokenSet = rec.getCandTokenSet();
		tokenSet.retainAll(Util.getPrefix(query, theta));
		return new IntArrayList( tokenSet.stream().sorted().iterator() );
	}

	protected boolean applyPrefixFiltering( Record query, Record rec, PkduckDPEx pkduckdp, int target ) {
		int nWindow = 0;
		for ( int widx=0; widx<rec.size(); ++widx ) {
			log.trace("widx: %d  maxWindowSize: %d", widx, pkduckdp.getMaxWindowSize(widx));
			for ( int w=1; w<=pkduckdp.getMaxWindowSize(widx); ++w ) {
				++nWindow;
				boolean isInSigU = pkduckdp.isInSigU(widx, w);
				log.trace("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, target=%d, ...)  widx=%d/%d  w=%d/%d  isInSigU=%s", query.getID(), rec.getID(), target, widx, rec.size()-1, w, pkduckdp.getMaxWindowSize(widx), isInSigU );
				if ( isInSigU ) {
					double sim = validator.simx2y((new Subrecord(rec, widx, widx+w)).toRecord(), query);
					log.trace("sim: %.3f", sim);
					if ( sim >= theta ) {
						rsltFromText.add(new IntPair(query.getID(), rec.getID()));
						log.debug("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, target=%d, ...)  isInSigU=true", query.getID(), rec.getID(), target);
						return true;
					}
				}
			}
		}
		log.debug("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, target=%d, ...)  isInSigU=false", query.getID(), rec.getID(), target);
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
