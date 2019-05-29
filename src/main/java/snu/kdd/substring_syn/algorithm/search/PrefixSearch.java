package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Subrecord;
import vldb18.NaivePkduckValidator;
import vldb18.PkduckDPEx;

public class PrefixSearch extends AbstractSearch {

	final NaivePkduckValidator validator;

	
	public PrefixSearch( double theta ) {
		super(theta);
		validator = new NaivePkduckValidator();
	}
	
	@Override
	protected void prepareSearchGivenQueryRecord( Record qrec ) {
	}

	@Override
	protected void searchRecordFromQuery( Record qrec, Record rec ) {
	}
	
	@Override
	protected void searchRecordFromText( Record qrec, Record rec ) {
		IntList candTokenList = getCandTokenList(qrec, rec);
		log.debug("PrefixSearch.getCandTokenList(%d, %d)  candTokenSet.size=%d", qrec.getID(), rec.getID(), candTokenList.size());
		PkduckDPEx pkduckdp = new PkduckDPEx(rec, theta, qrec.size());
		for ( int target : candTokenList ) {
			long ts0 = System.nanoTime();
			pkduckdp.compute(target);
			log.debug("PkduckDPEx.compute(%d) rec.id=%d  %.3f ms", target, rec.getID(), (System.nanoTime()-ts0)/1e6);
			ts0 = System.nanoTime();
			int nWindow = applyPrefixFiltering(qrec, rec, pkduckdp, target);
			log.debug("PrefixSearch.applyPrefixFiltering(...)  %4d/%4d  %.3f ms", nWindow, rec.size()*(rec.size()+1)/2, (System.nanoTime()-ts0)/1e6 );
		} // end for
		
	}
	
	protected IntList getCandTokenList( Record qrec, Record rec ) {
		IntSet tokenSet = rec.getCandTokenSet();
		tokenSet.retainAll(qrec.getTokens());
		return new IntArrayList( tokenSet.stream().sorted().iterator() );
	}

	protected int applyPrefixFiltering( Record qrec, Record rec, PkduckDPEx pkduckdp, int target ) {
		int nWindow = 0;
		for ( int widx=0; widx<rec.size(); ++widx ) {
			for ( int w=1; w<=pkduckdp.getMaxWindowSize(widx); ++w ) {
				++nWindow;
				boolean isInSigU = pkduckdp.isInSigU(widx, w);
				if ( isInSigU ) {
					double sim = validator.sim(qrec, (new Subrecord(rec, widx, widx+w)).toRecord());
					if ( sim >= theta ) {
						rsltFromText.add(new IntPair(qrec.getID(), rec.getID()));
						log.debug("PrefixSearch.applyPrefixFiltering(qrec.id=%d, rec.id=%d, target=%d, ...)  isInSigU=true", qrec.getID(), rec.getID(), target);
						return nWindow;
					}
				}
			}
		}
		log.debug("PrefixSearch.applyPrefixFiltering(qrec.id=%d, rec.id=%d, target=%d, ...)  isInSigU=false", qrec.getID(), rec.getID(), target);
		return nWindow;
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
