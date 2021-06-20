package snu.kdd.pkwise;

import java.util.Collections;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.WindowDataset;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Util;

public class PkwiseIndexBuilder {

	static Int2ObjectMap<ObjectList<WindowInterval>> map;
	static Int2IntOpenHashMap counter;
	static Int2IntOpenHashMap sidxMap;
	static double theta;

	public static Int2ObjectMap<ObjectList<WindowInterval>> buildTok2WitvMap( PkwiseSearch alg, WindowDataset dataset, int wMin, int wMax, double theta ) {
		PkwiseIndexBuilder.theta = theta;
		WitvMapBuilder builder = new WitvMapBuilder(dataset, alg.getSiggen(), wMin, wMax, true);
		return builder.build();
	}
	

	
		
	
	
	private static boolean isLastWindow( Subrecord window ) {
		if ( window == null ) return false;
		return (window.getSidx()+window.size() == window.getSuperRecord().size());
	}
	
	public static class WitvMapBuilder {
		final Iterator<Subrecord> windowList;
		final PkwiseSignatureGenerator siggen;
		final Int2ObjectMap<ObjectList<WindowInterval>> map;
		final Int2IntOpenHashMap counter;
		final Int2IntOpenHashMap sidxMap;
		Subrecord x;
		int l;
		IntArrayList prefix;
		IntArrayList sig;
		int cov;
		final boolean indexing;
		boolean debug = true;
		
		public WitvMapBuilder( WindowDataset dataset, PkwiseSignatureGenerator siggen, int wMin, int wMax, boolean indexing ) {
			windowList = dataset.getWindowList(wMin, wMax).iterator();
			this.siggen = siggen;
			map = new Int2ObjectOpenHashMap<>();
			counter = new Int2IntOpenHashMap();
			sidxMap = new Int2IntOpenHashMap();
			this.indexing = indexing;
			init();
		}
		
		private void init() {
			x = null;
			l = 0;
			prefix = null;
			sig = null;
		}
		
		public Int2ObjectMap<ObjectList<WindowInterval>> build() {
			while ( windowList.hasNext() ) {
				maintainPrefix();
			}
			maintainPrefix();
			return map;
		}
		
		public void maintainPrefix() {
			if ( isLastWindow(x) ) {
				sig = siggen.genSignature(prefix, indexing);
				for ( int token : sig ) {
					closeInterval(token, x, true);
				}
				init();
				return;
			}
			
			Subrecord x1 = windowList.next();
			int maxDiff = Util.getPrefixLength(x1, theta);
			IntArrayList prefix1;
			IntArrayList sig1 = null;
			
			if ( x == null ) {
				int l = siggen.getPrefixLength(x1, maxDiff);
				prefix1 = new IntArrayList( x1.getTokenList().stream().sorted().limit(l).iterator() );
				sig1 = siggen.genSignature(prefix1, indexing);
				for ( int token : sig1 ) {
					openInterval(token, x1);
				}
			}

			else {
				int t1 = x.getToken(0);
				int t2 = x1.getToken(x1.size()-1);
				prefix1 = new IntArrayList(prefix);
				removeFromSig(prefix1, t1);
				if ( prefix1.size() >0 && t2 < prefix1.getInt(prefix1.size()-1) ) addToPrefix(prefix1, t2);
				PrefixWrapper wprefix1 = siggen.wrapPrefix(prefix1);
				int cov1 = wprefix1.cov;

				
				if ( siggen.getCov(prefix, t1) < maxDiff ) {
					if ( cov1 == maxDiff ) {
						siggen.removeTrailingNonCoveringTokens(wprefix1);
						sig1 = siggen.genSignature(wprefix1.prefix, indexing);
						if ( t1 != t2 ) {
							openAndCloseIntervals(sig, sig1, x1);
						}
					}
					else {
						IntArrayList diffPrefix = siggen.expandPrefix(wprefix1, x1);
						sig1 = siggen.genSignature(wprefix1.prefix, indexing);
						if ( diffPrefix.size() != 1 || diffPrefix.getInt(0) != t1 ) {
							openAndCloseIntervals(sig, sig1, x1);
						}
					}
					
				}
				else {
					if ( cov1 > maxDiff ) {
						IntArrayList diffPrefix = siggen.shrinkPrefix(wprefix1);
						siggen.removeTrailingNonCoveringTokens(wprefix1);
						sig1 = siggen.genSignature(wprefix1.prefix, indexing);
						if ( diffPrefix.size() != 1 || diffPrefix.getInt(0) != t2 ) {
							openAndCloseIntervals(sig, sig1, x1);
						}
					}
				}
			}
			
			x = x1;
			prefix = prefix1;
			if ( sig1 != null ) sig = sig1;
		}
		
		private void openInterval( int token, Subrecord window ) {
			if ( counter.get(token) == 0 ) {
				sidxMap.put(token, window.getSidx());
			}
			counter.addTo(token, 1);
		}

		private void closeInterval( int token, Subrecord window, boolean isLast ) {
			
			if ( counter.get(token) == 1 ) {
				if ( !map.containsKey(token) ) map.put(token, new ObjectArrayList<>());
				int sidx0 = sidxMap.get(token);
				int eidx0 = window.getSidx()+(isLast?1:0);
				map.get(token).add( new WindowInterval( window.getIdx(), window.size(), sidx0, eidx0 ) );
			}
			counter.addTo(token, -1);
		}
		
		private void removeFromSig( IntArrayList sig, int token ) {
			int pos = Collections.binarySearch(sig, token);
			if ( pos >= 0 ) {
				sig.removeInt(pos);
			}
		}
		
		private void addToPrefix( IntArrayList sig, int token ) {
			int pos = Collections.binarySearch(sig, token);
			if ( pos >= 0 ) sig.add(pos, token);
			else {
				pos = -pos-1;
				if ( pos >= sig.size() ) sig.add(token);
				else sig.add(pos, token);
			}
		}

		private void openAndCloseIntervals( IntArrayList sig, IntArrayList sig1, Subrecord window ) {
			int i=0, i1 = 0;
			while ( i < sig.size() && i1 < sig1.size() ) {
				if ( sig.getInt(i) == sig1.getInt(i1) ) {
					i += 1;
					i1 += 1;
				}
				else if ( sig.getInt(i) < sig1.getInt(i1) ) {
					closeInterval(sig.getInt(i), window, false);
					i += 1;
				}
				else {
					openInterval(sig1.getInt(i1), window);
					i1 += 1;
				}
			}
			
			while ( i < sig.size() ) {
				closeInterval(sig.getInt(i), window, false);
				i += 1;
			}
			
			while ( i1 < sig1.size() ) {
				openInterval(sig1.getInt(i1), window);
				i1 += 1;
			}
		}
	}
}
