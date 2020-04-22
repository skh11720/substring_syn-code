package snu.kdd.substring_syn.algorithm.search;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.filter.TransLenLazyCalculator;
import snu.kdd.substring_syn.algorithm.validator.GreedyValidator;
import snu.kdd.substring_syn.algorithm.validator.ImprovedGreedyValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.IntRange;
import snu.kdd.substring_syn.utils.ReturnStatus;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.SortedWindowExpander;
import vldb18.PkduckDP;

public class PrefixSearch extends AbstractIndexBasedSearch {

	protected IntSet queryCandTokenSet;
	protected IntSet expandedPrefix;
	protected final boolean bLF, bPF;
	private final GreedyValidator validator;
	protected TransLenLazyCalculator transLenCalculator = null;
	protected double modifiedTheta;

	
	public PrefixSearch( double theta, boolean bLF, boolean bPF, IndexChoice indexChoice ) {
		super(theta, indexChoice);
		this.bLF = bLF;
		this.bPF = bPF;
		param.put("bLF", Boolean.toString(bLF));
		param.put("bPF", Boolean.toString(bPF));
		param.put("index_impl", indexChoice.toString());
		validator = new ImprovedGreedyValidator(theta, statContainer);
	}
	
	@Override
	protected final void prepareSearchGivenQuery(Record query) {
		super.prepareSearchGivenQuery(query);
		queryCandTokenSet = query.getCandTokenSet();
		expandedPrefix = getExpandedPrefix(query);
	}
	
	@Override
	protected void searchRecordQuerySide( Record query, RecordInterface rec ) {
//		Log.log.trace("searchRecordQuerySide(%d, %d)", ()->query.getID(), ()->rec.getID());
		IntRange wRange = getWindowSizeRangeQuerySide(query, rec);
//		Log.log.trace("wRange=(%d,%d)", ()->wRange.min, ()->wRange.max);
		for ( int widx=0; widx<rec.size(); ++widx ) {
			SortedWindowExpander witer = new SortedWindowExpander(rec, widx, theta);
			while ( witer.hasNext() ) {
				Subrecord window = witer.next();
//				Log.log.trace("window=[%d,%d]", ()->window.sidx, ()->window.eidx);
				IntCollection wprefix = witer.getPrefix();
				ReturnStatus status = searchWindowQuerySide(query, window, wRange, wprefix);
				if (status == ReturnStatus.Continue ) continue;
				else if (status == ReturnStatus.Break ) break;
				else if (status == ReturnStatus.Terminate ) return;
			}
		}
	}

	protected final IntSet getExpandedPrefix( Record query ) {
		IntSet expandedPrefix = new IntOpenHashSet();
		PkduckDP pkduckdp = new PkduckDP(query, theta);
		for ( int target : queryCandTokenSet ) {
			statContainer.startWatch("Time_QS_Pkduck");
			boolean isInSigU = pkduckdp.isInSigU(target);
			statContainer.stopWatch("Time_QS_Pkduck");
			if (isInSigU) expandedPrefix.add(target);
		}
		return expandedPrefix;
	}
	
	protected final IntRange getWindowSizeRangeQuerySide( Record query, RecordInterface rec ) {
		int min = (int)Math.max(1, Math.ceil(theta*query.getMinTransLength()));
		int max = (int)Math.min(1.0*query.getMaxTransLength()/theta, rec.size());
		return new IntRange(min, max);
	}
	
	protected final ReturnStatus searchWindowQuerySide( Record query, Subrecord window, IntRange wRange, IntCollection wprefix ) {
		int w = window.size();
		
		if ( bLF ) {
			ReturnStatus lfOutput = applyLengthFilterQuerySide(w, wRange);
			if ( lfOutput != ReturnStatus.None ) return lfOutput;
			statContainer.addCount(Stat.Len_QS_LF, w);
		}
		
		if ( bPF ) {
			if ( isFilteredByPrefixFilteringQuerySide(wprefix, expandedPrefix) ) return ReturnStatus.Continue;
			statContainer.addCount(Stat.Len_QS_PF, w); 
		}

		statContainer.startWatch(Stat.Time_QS_Validation);
		boolean isSim = verifyQuerySide(query, window);
		statContainer.stopWatch(Stat.Time_QS_Validation);
		if ( isSim ) {
			addResultQuerySide(query, window);
//					Log.log.trace("rsltFromQuery.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->window.size(), ()->window.sidx);
//					Log.log.trace("rsltFromQueryMatch\t%s ||| %s", ()->query.toOriginalString(), ()->window.toOriginalString());
			return ReturnStatus.Terminate;
		}
		return ReturnStatus.None;
	}
	
	protected final ReturnStatus applyLengthFilterQuerySide( int w, IntRange wRange ) {
		if ( w > wRange.max ) return ReturnStatus.Break;
		if ( w < wRange.min ) return ReturnStatus.Continue;
		return ReturnStatus.None;
	}
	
	protected final boolean isFilteredByPrefixFilteringQuerySide( IntCollection wprefix, IntSet expandedPrefix ) {
		return !Util.hasIntersection(wprefix, expandedPrefix);
	}
	
	protected boolean verifyQuerySide( Record query, Subrecord window ) {
		double sim = validator.simQuerySide(query, window);
//		if ( sim >= theta ) Log.log.trace("verifyQuerySide(%d, %d): sim=%.3f", ()->query.getID(), ()->window.getSuperRecord().getID(), ()->sim);
		return sim >= theta;
	}
	
	@Override
	protected void searchRecordTextSide( Record query, TransformableRecordInterface rec ) {
//		Log.log.trace("searchRecordFromText(%d, %d)", ()->query.getIdx(), ()->rec.getIdx());
		modifiedTheta = Util.getModifiedTheta(query, rec, theta);
		
//		if (bLF || bPF) {
//			statContainer.startWatch("Time_TS_searchRecord.transLen");
//			transLenCalculator = new TransLenLazyCalculator(statContainer, rec, 0, rec.size(), modifiedTheta);
//			statContainer.stopWatch("Time_TS_searchRecord.transLen");
//		}
		
		if (bPF) searchRecordTextSideWithPrefixFilter(query, rec);
		else searchRecordTextSideWithoutPrefixFilter(query, rec);
	}
	
	protected void searchRecordTextSideWithPrefixFilter( Record query, TransformableRecordInterface rec ) {
		IntList candTokenList = getCandTokenList(query, rec, modifiedTheta);
		PkduckDPExIncremental pkduckdp = new PkduckDPExIncrementalOpt(query, rec, modifiedTheta);
//		Log.log.trace("searchRecordTextSideWithPF(%d, %d)\tcandTokenList=%s", ()->query.getIdx(), ()->rec.getIdx(), ()->candTokenList);
		ObjectSet<IntPair> verifiedWindowSet = new ObjectOpenHashSet<>();
		
		for ( int target : candTokenList ) {
			pkduckdp.setTarget(target);
			for ( int widx=0; widx<rec.size(); ++widx ) {
				transLenCalculator = new TransLenLazyCalculator(statContainer, rec, widx, rec.size()-widx, modifiedTheta);
				pkduckdp.init();
				for ( int w=1; w<=rec.size()-widx; ++w ) {
//					Log.log.trace("target=%s (%d), widx=%d, w=%d", Record.tokenIndex.getToken(target), target, widx, w);
					if ( bLF && applyLengthFilterTextSide(query, widx, w) == ReturnStatus.Break ) break;
					if ( bPF && applyPrefixFilterTextSide(pkduckdp, widx, w, verifiedWindowSet) == ReturnStatus.Continue ) continue;
					if ( verifyTextSideWrapper(query, rec, widx, w) == ReturnStatus.Terminate ) return;
				}
			}
		}
	}
	
	protected void searchRecordTextSideWithoutPrefixFilter( Record query, TransformableRecordInterface rec ) {
		for ( int widx=0; widx<rec.size(); ++widx ) {
			transLenCalculator = new TransLenLazyCalculator(statContainer, rec, widx, rec.size()-widx, modifiedTheta);
			for ( int w=1; w<=rec.size()-widx; ++w ) {
				if ( bLF && applyLengthFilterTextSide(query, widx, w) == ReturnStatus.Break ) break;
				if ( verifyTextSideWrapper(query, rec, widx, w) == ReturnStatus.Terminate ) return;
			}
		}
	}

	protected final IntList getCandTokenList( Record query, TransformableRecordInterface rec, double theta ) {
		IntSet recTokenSet = rec.getCandTokenSet();
		IntSet tokenSet = new IntOpenHashSet();
		for ( int token : Util.getPrefix(query, theta) ) {
			if ( recTokenSet.contains(token) ) tokenSet.add(token);
		}
		return new IntArrayList( tokenSet.stream().sorted().iterator() );
	}
	
	protected final ReturnStatus applyLengthFilterTextSide( Record query, int widx, int w ) {
		statContainer.addCount(Stat.Len_TS_LF, w);
		if ( transLenCalculator.getLFLB(widx+w-1) > query.size() ) return ReturnStatus.Break;
		return ReturnStatus.None;
	}
	
	protected ReturnStatus applyPrefixFilterTextSide( PkduckDPExIncremental pkduckdp, int widx, int w, ObjectSet<IntPair> verifiedWindowSet ) {
		statContainer.startWatch("Time_TS_searchRecordPF.pkduck");
		pkduckdp.compute(widx+1, w);
		statContainer.stopWatch("Time_TS_searchRecordPF.pkduck");
//		Log.log.trace("applyPrefixFilterTextSide: rec.idx=%d, widx=%d, w=%d, isInSigU=%s", pkduckdp.rec.getIdx(), widx, w, pkduckdp.isInSigU(widx, w));
		if ( verifiedWindowSet.contains(new IntPair(widx, w)) ) return ReturnStatus.Continue;
		if ( !pkduckdp.isInSigU(widx, w) ) return ReturnStatus.Continue;
		verifiedWindowSet.add(new IntPair(widx, w));
		statContainer.addCount(Stat.Len_TS_PF, w);
		return ReturnStatus.None;
	}
	
	protected final ReturnStatus verifyTextSideWrapper( Record query, TransformableRecordInterface rec, int widx, int w ) {
//		Log.log.trace("verifyTextSideWrapper: query.idx=%d, rec.idx=%d, widx=%d, w=%d", ()->query.getIdx(), ()->rec.getIdx(), ()->widx, ()->w);
		Subrecord window = new Subrecord(rec, widx, widx+w);
		statContainer.startWatch(Stat.Time_TS_Validation);
		boolean isSim = verifyTextSide(query, window);
		statContainer.stopWatch(Stat.Time_TS_Validation);
		if ( isSim ) {
			addResultTextSide(query, rec);
//			Log.log.trace("rsltFromText.add(%d, %d), w=%d, widx=%d", ()->query.getIdx(), ()->rec.getIdx(), ()->window.size(), ()->window.sidx);
//			Log.log.trace("rsltFromTextMatch\t%s ||| %s", ()->query.toOriginalString(), ()->window.toOriginalString());
			return ReturnStatus.Terminate;
		}
		return ReturnStatus.None;
	}

	protected boolean verifyTextSide( Record query, Subrecord window ) {
		double sim = validator.simTextSide(query, window);
//		if ( sim >= theta ) Log.log.trace("verifyTextSide(%d, %d): sim=%.3f", ()->query.getID(), ()->window.getSuperRecord().getID(), ()->sim);
		return sim >= theta;
	}



	protected class PkduckDPExIncremental {
		
		protected final int maxTransLen;
		protected final Record query;
		protected final TransformableRecordInterface rec;
		protected final double theta;
		protected final int[][][] g;
		protected final boolean[][] b;
		protected int target;
		
		
		public PkduckDPExIncremental( Record query, TransformableRecordInterface rec, double theta ) {
			this.query = query;
			this.rec = rec;
			this.theta = theta;
			this.maxTransLen = rec.getMaxTransLength();
			this.g = new int[2][rec.size()+1][maxTransLen+1];
			this.b = new boolean[rec.size()+1][rec.size()+1];
			for (boolean[] bArr : b) Arrays.fill(bArr, false);
		}
		
		public void compute( int i, int v ) {
			for ( int l=1; l<=transLenCalculator.getUB(i+v-2); ++l ) {
				for (Rule rule : rec.getSuffixApplicableRules( i+v-2 )) {
					if ( l - rule.rhsSize() < 0 ) continue;
					int num_smaller = 0;
					Boolean isValid = true;
					for ( int tokenInRhs : rule.getRhs() ) {
						isValid &= (tokenInRhs != target);
						num_smaller += (tokenInRhs < target)?1:0;
					}
					if ( isValid && v-rule.lhsSize() >= 0 ) {
						g[0][v][l] = Math.min( g[0][v][l], g[0][v-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
					}
				}
				
				for (Rule rule : rec.getSuffixApplicableRules( i+v-2 )) {
					if ( l - rule.rhsSize() < 0 ) continue;
					int num_smaller = 0;
					Boolean isValid = false;
					for ( int tokenInRhs : rule.getRhs() ) {
						isValid |= (tokenInRhs == target);
						num_smaller += (tokenInRhs < target)?1:0;
					}
					if ( v-rule.lhsSize() >= 0 ) {
						g[1][v][l] = Math.min( g[1][v][l], g[1][v-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
						if (isValid) g[1][v][l] = Math.min( g[1][v][l], g[0][v-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
					}
				}
				
				if ( g[1][v][l] <= getPrefixLen(l)-1 ) {
					b[i][v] = true;
					return;
				}
			}
		}

		protected final void init() {
			for ( int o=0; o<2; ++o ) {
				for ( int v=0; v<=rec.size(); ++v ) {
					Arrays.fill( g[o][v], maxTransLen+1 );
				}
			}
			g[0][0][0] = 0;
			for ( int v=0; v<=rec.size(); ++v ) Arrays.fill(b[v], false);
		}
		
		protected void setTarget(int target ) {
			this.target = target;
		}

		protected final int getPrefixLen( int len ) {
			return len - (int)(Math.ceil(theta*len)) + 1;
		}
		
		protected final boolean isInSigU( int i, int v ) {
			return b[i+1][v];
		}
		
		public final String toStringEntries(int flag) {
			StringBuilder strbld = new StringBuilder();
			for ( int i=0; i< g[flag].length; ++i ) strbld.append("\n"+Arrays.toString(g[flag][i]));
			return strbld.toString();
		}
	}

	protected class PkduckDPExIncrementalOpt extends PkduckDPExIncremental {
		
		ObjectList<Object2IntMap<IntPair>> numSmallestThanTarget0 = null;
		ObjectList<Object2IntMap<IntPair>> numSmallestThanTarget1 = null;

		public PkduckDPExIncrementalOpt(Record query, TransformableRecordInterface rec, double theta) {
			super(query, rec, theta);
		}
		
		@Override
		protected void setTarget(int target) {
			super.setTarget(target);
			numSmallestThanTarget0 = new ObjectArrayList<>();
			numSmallestThanTarget1 = new ObjectArrayList<>();
			for ( int k=0; k<rec.size(); ++k ) {
				Object2IntMap<IntPair> map0 = new Object2IntOpenHashMap<>();
				Object2IntMap<IntPair> map1 = new Object2IntOpenHashMap<>();
				for ( Rule rule : rec.getSuffixApplicableRules(k) ) {
					IntPair key = new IntPair(rule.lhsSize(), rule.rhsSize());
					boolean containsTarget = false;
					int numSmaller = 0;
					for ( int tokenInRhs : rule.getRhs() ) {
						containsTarget |= (tokenInRhs == target);
						numSmaller += (tokenInRhs < target)?1:0;
					}
					if ( containsTarget ) {
						if ( !map1.containsKey(key) || numSmaller < map1.get(key) ) map1.put(key, numSmaller);
					}
					else {
						if ( !map0.containsKey(key) || numSmaller < map0.get(key) ) map0.put(key, numSmaller);
					}
				}
				numSmallestThanTarget0.add(map0);
				numSmallestThanTarget1.add(map1);
			}
		}
		
		@Override
		public void compute(int i, int v) {
			for ( int l=1; l<=transLenCalculator.getUB(i+v-2); ++l ) {
				for ( Object2IntMap.Entry<IntPair> e : numSmallestThanTarget0.get(i+v-2).object2IntEntrySet() ) {
					int lhsSize = e.getKey().i1;
					int rhsSize = e.getKey().i2;
					int num_smaller = e.getIntValue();
					if ( v-lhsSize >= 0 && l-rhsSize >= 0 ) {
						g[0][v][l] = Math.min( g[0][v][l], g[0][v-lhsSize][l-rhsSize]+num_smaller );
					}
				}

				for ( Object2IntMap.Entry<IntPair> e : numSmallestThanTarget0.get(i+v-2).object2IntEntrySet() ) {
					int lhsSize = e.getKey().i1;
					int rhsSize = e.getKey().i2;
					int num_smaller = e.getIntValue();
					if ( v-lhsSize >= 0 && l-rhsSize >= 0 ) {
						g[1][v][l] = Math.min( g[1][v][l], g[1][v-lhsSize][l-rhsSize]+num_smaller );
					}
				}

				for ( Object2IntMap.Entry<IntPair> e : numSmallestThanTarget1.get(i+v-2).object2IntEntrySet() ) {
					int lhsSize = e.getKey().i1;
					int rhsSize = e.getKey().i2;
					int num_smaller = e.getIntValue();
					if ( v-lhsSize >= 0 && l-rhsSize >= 0 ) {
						g[1][v][l] = Math.min( g[1][v][l], g[1][v-lhsSize][l-rhsSize]+num_smaller );
						g[1][v][l] = Math.min( g[1][v][l], g[0][v-lhsSize][l-rhsSize]+num_smaller );
					}
				}
				
				if ( g[1][v][l] <= getPrefixLen(l)-1 ) {
					b[i][v] = true;
					return;
				}
			}
		}
	}

	@Override
	public String getOutputName( Dataset dataset ) {
		return String.join( "_", getName(), getVersion(), indexChoice.toString(), bLF? "L":"", bPF? "P":"", String.format("%.2f", theta), dataset.name);
	}
	
	@Override
	public String getName() {
		return "PrefixSearch";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 2.00: length filtering
		 * 3.00: index filtering for query-side
		 * 3.01: takes O(n^2) time to split records
		 * 3.02: position filter text-side
		 * 3.03: multiset
		 * 4.00: multiset
		 * 4.01: refactor
		 * 4.02: filter option
		 * 4.03: filter option
		 * 4.04: fit stat bug
		 * 4.05: skip text-side if a pair is an answer
		 * 4.06: fix bug in position filter
		 * 4.07: fix bug in pkduckdp text-side
		 * 4.08: fix bug
		 * 4.09: PositionPrefixSearch
		 * 4.10: use self rules in verification
		 * 4.11: fix bug in position filter
		 * 4.12: fix bug in position filter, segment-wise count filter in text-side
		 * 5.00: refactor and use disk-based dataset
		 * 5.01: prune substrings by using prefix and suffix list
		 * 5.02: reduce redundant computation in TransLenCalculator
		 * 5.03: reduce redundant computation by Subrecord.toRecord()
		 * 5.04: ignore rules with score zero in GreedyValidator
		 * 6.00: use DiskBasedNaiveInvertedIndex
		 * 6.01: use DiskBasedNaiveInvertedIndex in CountFilter
		 * 6.02: use DiskBasedPositionalInvertedIndex in PositionFilter
		 * 6.03: do not use OjbectSet to improve speed
		 * 6.04: eliminate duplicated record preprocessing
		 * 6.05: modify dataset instantiation
		 * 6.06: fix a bug in transLen calculator
		 * 6.07: fix a bug in position filter
		 * 6.08: optimization
		 * 6.09: improve position filter
		 * 6.10: fix a bug
		 * 6.11: update score function in position filter
		 * 6.12: fix bugs in position filter (ERROR)
		 * 6.13: fix bugs in count and position filter
		 * 6.14: fix bugs in IndexNaiveFilter
		 * 6.15: update score functions
		 * 6.16: fix bug in score function
		 * 6.17: modify verification
		 * 6.18: improve index filter memory efficiency
		 * 6.19: prevent repeated verification in PF 
		 * 6.20: improve pos filter, without split
		 * 6.21: fix bug in length filter
		 * 6.22: fix bug in PF and LF
		 * 6.23: speedup filters and use maximum matching
		 * 6.24: use RecordPool
		 * 6.25: remodel RecordStore
		 * 6.26: improve count filter
		 * 6.27: not use RecordPool
		 * 6.28: fix bug in modified count filter, speedup preprocessing
		 * 6.29: use RecordPool
		 * 6.30: Reduce pool size=1e4
		 * 6.31: improve memory efficiency
		 * 6.32: use java.util.PriorityQueue
		 */
		return "6.32";
	}
}
