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
import snu.kdd.substring_syn.algorithm.filter.TransLenCalculator;
import snu.kdd.substring_syn.algorithm.validator.GreedyValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.IntRange;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.SortedWindowExpander;
import vldb18.PkduckDP;

public class PrefixSearch extends AbstractIndexBasedSearch {

	protected IntSet queryCandTokenSet;
	protected IntSet expandedPrefix;
	protected final boolean bLF, bPF;
	protected final GreedyValidator validator;
	protected TransLenCalculator transLenCalculator = null;

	
	public PrefixSearch( double theta, boolean bLF, boolean bPF, IndexChoice indexChoice ) {
		super(theta, indexChoice);
		this.bLF = bLF;
		this.bPF = bPF;
		param.put("bLF", Boolean.toString(bLF));
		param.put("bPF", Boolean.toString(bPF));
		param.put("index_impl", indexChoice.toString());
		validator = new GreedyValidator(theta, statContainer);
	}
	
	@Override
	protected void prepareSearchGivenQuery(Record query) {
		super.prepareSearchGivenQuery(query);
		queryCandTokenSet = query.getCandTokenSet();
		expandedPrefix = getExpandedPrefix(query);
	}
	
	@Override
	protected void searchRecordQuerySide( Record query, Record rec ) {
		Log.log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		IntRange wRange = getWindowSizeRangeQuerySide(query, rec);
		Log.log.debug("wRange=(%d,%d)", ()->wRange.min, ()->wRange.max);
		for ( int widx=0; widx<rec.size(); ++widx ) {
			SortedWindowExpander witer = new SortedWindowExpander(rec, widx, theta);
			while ( witer.hasNext() ) {
				Subrecord window = witer.next();
				int w = window.size();
				
				if ( bLF ) {
					switch ( applyLengthFilterQuerySide(w, wRange) ) {
					case filtered_ignore: continue;
					case filtered_stop: break;
					default:
					}
					statContainer.addCount(Stat.Len_QS_LF, w);
				}
				
				if ( bPF && isFilteredByPrefixFilteringQuerySide(witer, expandedPrefix)) continue;

				statContainer.addCount(Stat.Len_QS_PF, w); 
				statContainer.startWatch(Stat.Time_Validation);
				boolean isSim = verifyQuerySide(query, window);
				statContainer.stopWatch(Stat.Time_Validation);
				if ( isSim ) {
					rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
					Log.log.debug("rsltFromQuery.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->window.size(), ()->window.sidx);
					Log.log.debug("rsltFromQueryMatch\t%s ||| %s", ()->query.toOriginalString(), ()->window.toOriginalString());
					return;
				}
			}
		}
	}

	protected IntSet getExpandedPrefix( Record query ) {
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
	
	protected IntRange getWindowSizeRangeQuerySide( Record query, Record rec ) {
		int min = (int)Math.max(1, Math.ceil(theta*query.getMinTransLength()));
		int max = (int)Math.min(1.0*query.getMaxTransLength()/theta, rec.size());
		return new IntRange(min, max);
	}
	
	protected LFOutput applyLengthFilterQuerySide( int w, IntRange wRange ) {
		if ( w > wRange.max ) return LFOutput.filtered_stop;
		if ( w < wRange.min ) return LFOutput.filtered_ignore;
		return LFOutput.not_filtered;
	}
	
	protected boolean isFilteredByPrefixFilteringQuerySide( SortedWindowExpander witer, IntSet expandedPrefix ) {
		IntCollection wprefix = witer.getPrefix();
		return !Util.hasIntersection(wprefix, expandedPrefix);
	}
	
	protected boolean verifyQuerySide( Record query, Subrecord window ) {
		double sim = validator.simQuerySide(query, window);
		if ( sim >= theta ) Log.log.debug("verifyQuerySide(%d, %d): sim=%.3f", ()->query.getID(), ()->window.getSuperRecord().getID(), ()->sim);
		return sim >= theta;
	}
	
	@Override
	protected void searchRecordTextSide( Record query, Record rec ) {
		Log.log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		double modifiedTheta = Util.getModifiedTheta(query, rec, theta);
		
		if (bLF) {
			statContainer.startWatch("Time_TS_searchRecord.transLen");
			transLenCalculator = new TransLenCalculator(statContainer, rec, modifiedTheta);
			statContainer.stopWatch("Time_TS_searchRecord.transLen");
		}
		
		if (bPF) searchRecordTextSideWithPrefixFilter(query, rec);
		else searchRecordTextSideWithoutPrefixFilter(query, rec);;
	}
	
	protected IntList getCandTokenList( Record query, Record rec, double theta ) {
		IntSet recTokenSet = rec.getCandTokenSet();
		IntSet tokenSet = new IntOpenHashSet();
		for ( int token : Util.getPrefix(query, theta) ) {
			if ( recTokenSet.contains(token) ) tokenSet.add(token);
		}
		return new IntArrayList( tokenSet.stream().sorted().iterator() );
	}
	
	protected void searchRecordTextSideWithPrefixFilter( Record query, Record rec ) {
		double modifiedTheta = Util.getModifiedTheta(query, rec, theta);
		IntList candTokenList = getCandTokenList(query, rec, modifiedTheta);
		PkduckDPExIncremental pkduckdp = new PkduckDPExIncremental(query, rec, modifiedTheta);
		Log.log.trace("searchRecordTextSideWithPF(%d, %d)\tcandTokenList=%s", ()->query.getID(), ()->rec.getID(), ()->candTokenList);
		
		for ( int target : candTokenList ) {
			pkduckdp.setTarget(target);
			for ( int widx=0; widx<rec.size(); ++widx ) {
				pkduckdp.init();
				for ( int w=1; w<=rec.size()-widx; ++w ) {
//					Log.log.trace("target=%s (%d), widx=%d, w=%d", Record.tokenIndex.getToken(target), target, widx, w);
					if ( bLF ) {
//						Log.log.trace("lb=%d, query.size=%d", transLenCalculator.getLFLB(widx, widx+w-1), query.size());
						if ( transLenCalculator.getLFLB(widx, widx+w-1) > query.size() ) break;
						statContainer.addCount(Stat.Len_TS_LF, w);
					}
					statContainer.startWatch("Time_TS_searchRecordPF.pkduck");
					pkduckdp.compute(widx+1, w);
					statContainer.stopWatch("Time_TS_searchRecordPF.pkduck");
//					Log.log.trace("isInSigU=%s", pkduckdp.isInSigU(widx, w));
					
					if ( pkduckdp.isInSigU(widx, w) ) {
						statContainer.addCount(Stat.Len_TS_PF, w);
						Subrecord window = new Subrecord(rec, widx, widx+w);
						boolean isSim = verifyTextSideWrapper(query, window);
						if ( isSim ) {
							rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
							Log.log.debug("rsltFromText.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->window.size(), ()->window.sidx);
							Log.log.debug("rsltFromTextMatch\t%s ||| %s", ()->query.toOriginalString(), ()->window.toOriginalString());
							return;
						}
					}
				}
			}
		}
	}
	
	protected void searchRecordTextSideWithoutPrefixFilter( Record query, Record rec ) {
		for ( int widx=0; widx<rec.size(); ++widx ) {
			for ( int w=1; w<=rec.size()-widx; ++w ) {
				if ( bLF ) {
					if ( transLenCalculator.getLFLB(widx, widx+w-1) > query.size() ) break;
					statContainer.addCount(Stat.Len_TS_LF, w);
				}
				Subrecord window = new Subrecord(rec, widx, widx+w);
				boolean isSim = verifyTextSideWrapper(query, window);
				if ( isSim ) {
					rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
					Log.log.debug("rsltFromText.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->window.size(), ()->window.sidx);
					Log.log.debug("rsltFromTextMatch\t%s ||| %s", ()->query.toOriginalString(), ()->window.toOriginalString());
					return;
				}
			}
		}
	}
	
	protected boolean verifyTextSideWrapper( Record query, Subrecord window ) {
		statContainer.startWatch(Stat.Time_Validation);
		boolean isSim = verifyTextSide(query, window);
		statContainer.stopWatch(Stat.Time_Validation);
		return isSim;
	}

	protected boolean verifyTextSide( Record query, Subrecord window ) {
		double sim = validator.simTextSide(query, window);
		if ( sim >= theta ) Log.log.debug("verifyTextSide(%d, %d): sim=%.3f", ()->query.getID(), ()->window.getSuperRecord().getID(), ()->sim);
		return sim >= theta;
	}

	protected enum LFOutput {
		filtered_ignore,
		filtered_stop,
		not_filtered
	}
	
	protected LFOutput applyLengthFiltering( Record query, int widx, int w ) {
		int ub = transLenCalculator.getLFUB(widx, widx+w-1);
		int lb = transLenCalculator.getLFLB(widx, widx+w-1);
		if ( query.size() > ub ) {
			statContainer.increment("Num_TS_LFByUB");
			return LFOutput.filtered_ignore;
		}
		if ( query.size() < lb ) {
			statContainer.increment("Num_TS_LFByLB");
			return LFOutput.filtered_ignore;
		}
		else return LFOutput.not_filtered;
	}


	class PkduckDPExIncremental {
		
		protected final int maxTransLen;
		protected final Record query;
		protected final Record rec;
		protected final double theta;
		protected final int[][][] g;
		protected final boolean[][] b;
		protected int target;
		
		
		public PkduckDPExIncremental( Record query, Record rec, double theta ) {
			this.query = query;
			this.rec = rec;
			this.theta = theta;
			this.maxTransLen = rec.getMaxTransLength();
			this.g = new int[2][rec.size()+1][maxTransLen+1];
			this.b = new boolean[rec.size()+1][rec.size()+1];
			for (boolean[] bArr : b) Arrays.fill(bArr, false);
		}
		
		public void compute( int i, int v ) {
			for ( int l=1; l<=transLenCalculator.getUB(i-1, i+v-2); ++l ) {
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

		protected void init() {
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

		protected int getPrefixLen( int len ) {
			return len - (int)(Math.ceil(theta*len)) + 1;
		}
		
		protected boolean isInSigU( int i, int v ) {
			return b[i+1][v];
		}
		
		public String toStringEntries(int flag) {
			StringBuilder strbld = new StringBuilder();
			for ( int i=0; i< g[flag].length; ++i ) strbld.append("\n"+Arrays.toString(g[flag][i]));
			return strbld.toString();
		}
	}

	class PkduckDPExIncrementalOpt extends PkduckDPExIncremental {
		
		ObjectList<Object2IntMap<IntPair>> numSmallestThanTarget0 = null;
		ObjectList<Object2IntMap<IntPair>> numSmallestThanTarget1 = null;

		public PkduckDPExIncrementalOpt(Record query, Record rec, double theta) {
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
			for ( int l=1; l<=transLenCalculator.getUB(i-1, i+v-2); ++l ) {
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
		 */
		return "6.08";
	}
}
