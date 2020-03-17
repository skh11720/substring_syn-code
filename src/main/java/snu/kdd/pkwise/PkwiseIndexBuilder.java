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
import snu.kdd.substring_syn.data.record.RecordInterface;
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
	
//	public static Int2ObjectMap<ObjectList<WindowInterval>> buildTok2WitvMap( PkwiseSynSearch alg, WindowDataset dataset, int wMin, int wMax ) {
//		PkwiseIndexBuilder.theta = theta;
//		int wMin = alg.getLFLB(qlen);
//		int wMax = alg.getLFUB(qlen);
//		WitvMapBuilder builder = new WitvMapBuilder(dataset, alg.getSiggen(), wMin, wMax, true);
//		return builder.build();
//	}

//	public static Int2ObjectMap<ObjectList<WindowInterval>> buildTok2TwitvMap( TransWindowDataset dataset, int qlen, double theta ) {
//		return null;
//	}
//		int wMin = alg.getLFLB(qlen);
//		int wMax = alg.getLFUB(qlen);
//
//		map = new Int2ObjectOpenHashMap<>();
//		counter = new Int2IntOpenHashMap();
//		sidxMap = new Int2IntOpenHashMap();
//		int head = -1;
//		int tail = -1;
//		for ( Subrecord window : dataset.getWindowList(wMin, wMax)) {
//			if ( head == -1 ) { // first window
//				for ( int token : window.getTokenArray() ) {
//					openInterval(token, window);
//				}
//			}
//			else {
//				tail = window.getToken(window.size()-1);
//				if ( head != tail ) {
//					openInterval(tail, window);
//					closeInterval(head, window.getID(), window.size(), window.getSidx(), false);
//				}
//			}
//			head = window.getToken(0);
//			
//			if ( isLastWindow(window) ) {
//				for ( int token : window.getTokenArray() ) {
//					closeInterval(token, window.getID(), window.size(), window.getSidx(), true);
//				}
//				head = -1;
//			}
//		}
//		return map;
//	}
	
//	public static Int2ObjectMap<ObjectList<WindowInterval>> buildTok2TwitvMap( WindowDataset dataset, int qlen, double theta ) {
//		map = new Int2ObjectOpenHashMap<>();
//		counter = new Int2IntOpenHashMap();
//		sidxMap = new Int2IntOpenHashMap();
//		Object2ObjectMap<IntTriple, IntList> tmpMap = new Object2ObjectOpenHashMap<>();
//		int rid = -1;
//		
//		for ( RecordInterface window : dataset.getTransWindowList(qlen, theta) ) {
//			if ( rid != window.getID() ) {
//				rid = window.getID();
//				for ( Entry<IntTriple, IntList> e : tmpMap.entrySet() ) {
//					IntTriple key = e.getKey();
//					int token = key.i1;
//					if ( !map.containsKey(token) ) map.put(token, new ObjectArrayList<>());
//					int recId = key.i2;
//					int w = key.i3;
//					IntList list = e.getValue(); 
//					int sidx = list.get(0);
//					int eidx = sidx+1;
//					for ( int i=1; i<list.size(); ++i ) {
//						if ( list.get(i) == sidx+1 ) eidx = list.get(i)+1;
//						else {
//							map.get(token).add(new WindowInterval(recId, w, sidx, eidx));
//							sidx = list.get(i);
//							eidx = sidx+1;
//						}
//					}
//					map.get(token).add(new WindowInterval(recId, w, sidx, eidx));
//					
//				}
//				tmpMap.clear();
//			}
//			for ( int token : window.getTokenArray() ) {
//				openInterval(token, window);
////				closeInterval(token, window.getID(), window.size(), window.getSidx()+1, false);
//				IntTriple key = new IntTriple(token, window.getID(), window.size());
//				if ( !tmpMap.containsKey(key) ) tmpMap.put(key, new IntArrayList());
//				tmpMap.get(key).add(window.getSidx());
//			}
//		}
//		
//		return map;
//	}
		
//	private static void openInterval( int token, RecordInterface window ) {
//		if ( counter.get(token) == 0 ) {
////			Log.log.trace("openInterval: "+token+"\t"+Record.tokenIndex.getToken(token));
//			sidxMap.put(token, window.getSidx());
//		}
//		counter.addTo(token, 1);
//	}
	
//	private static void closeInterval( int token, int rid, int w, int idx, boolean isLast ) {
//		if ( counter.get(token) == 1 ) {
//			if ( !map.containsKey(token) ) map.put(token, new ObjectArrayList<>());
//			int sidx0 = sidxMap.get(token);
//			int eidx0 = idx+(isLast?1:0);
//			if ( sidx0 >= eidx0 ) {
//				System.err.println("isLast="+isLast);
//				System.err.println("token="+Record.tokenIndex.getToken(token)+"  ("+token+")");
//				System.err.println("rec.id="+rid);
//				System.err.println("sidx0="+sidx0);
//				System.err.println("eidx0="+eidx0);
//				throw new RuntimeException();
//			}
////			Log.log.trace("closeInterval: token="+token+", "+Record.tokenIndex.getToken(token)+"\trid="+rid+"\tw="+w+"\tsidx0="+sidx0+"\teidx0="+eidx0);
//			map.get(token).add( new WindowInterval( rid, w, sidx0, eidx0 ) );
//		}
//		counter.addTo(token, -1);
//	}
	
	private static boolean isLastWindow( RecordInterface window ) {
		if ( window == null ) return false;
		return (window.getSidx()+window.size() == window.getSuperRecord().size());
	}
	
	public static class WitvMapBuilder {
		final Iterator<RecordInterface> windowList;
		final PkwiseSignatureGenerator siggen;
		final Int2ObjectMap<ObjectList<WindowInterval>> map;
		final Int2IntOpenHashMap counter;
		final Int2IntOpenHashMap sidxMap;
		RecordInterface x;
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
//				Log.log.trace("Last window");
				sig = siggen.genSignature(prefix, indexing);
				for ( int token : sig ) {
					closeInterval(token, x, true);
				}
				init();
				return;
			}
			
			RecordInterface x1 = windowList.next();
//			Log.log.trace("window: rid="+x1.getID()+"\tsidx="+x1.getSidx()+"\tsize="+x1.size()+"\twindow="+x1.getTokenList());
			int maxDiff = Util.getPrefixLength(x1, theta);
			IntArrayList prefix1;
			IntArrayList sig1 = null;
			
			if ( x == null ) { // x1 is the first window
//				Log.log.trace("First window");
				int l = siggen.getPrefixLength(x1, maxDiff);
				prefix1 = new IntArrayList( x1.getTokenList().stream().sorted().limit(l).iterator() );
				sig1 = siggen.genSignature(prefix1, indexing);
//				if ( x1.getID() == 7324 && x1.size() == 2 ) {
//					Log.log.trace("First window");
//					Log.log.trace("prefix1=%s", prefix1);
//					Log.log.trace("sig1=%s", sig1);
//				}
				for ( int token : sig1 ) {
					openInterval(token, x1);
				}
			}

			else {
//				Log.log.trace("Intermediate window");
				int t1 = x.getToken(0);
				int t2 = x1.getToken(x1.size()-1);
//				Log.log.trace("t1="+t1+"\tt2="+t2);
				prefix1 = new IntArrayList(prefix);
				removeFromSig(prefix1, t1);
				if ( prefix1.size() >0 && t2 < prefix1.getInt(prefix1.size()-1) ) addToPrefix(prefix1, t2);
				PrefixWrapper wprefix1 = siggen.wrapPrefix(prefix1);
				int cov1 = wprefix1.cov;

//				System.out.println("rid="+x1.getID());
//				System.out.println("t1="+t1+"\tt2="+t2);
//				System.out.println("sig="+sig);
//				System.out.println("sig1="+sig1);
//				System.out.println("cov1="+cov1);
//				System.out.println("sig1.size="+sig1.size()+"\tcov="+cov+"\tmaxDiff="+maxDiff);
//				if ( x1.getID() == 7324 && x1.size() == 2 ) {
//					Log.log.trace("Intermediate window");
//					Log.log.trace("prefix1=%s", prefix1);
//					Log.log.trace("sig1=%s", sig1);
//				}
				
				if ( siggen.getCov(prefix, t1) < maxDiff ) {
//					Log.log.trace("case1");
					if ( cov1 == maxDiff ) {
//						Log.log.trace("case1.1");
						siggen.removeTrailingNonCoveringTokens(wprefix1);
						sig1 = siggen.genSignature(wprefix1.prefix, indexing);
						if ( t1 != t2 ) {
//							Log.log.trace("case1.1.1");
							openAndCloseIntervals(sig, sig1, x1);
//							if ( x1.getID() == 7324 && x1.size() == 2 ) {
//								Log.log.trace("case1.1.1");
//								Log.log.trace("prefix1=%s", prefix1);
//								Log.log.trace("sig1=%s", sig1);
//							}
						}
					}
					else {
//						Log.log.trace("case1.2");
						IntArrayList diffPrefix = siggen.expandPrefix(wprefix1, x1);
						sig1 = siggen.genSignature(wprefix1.prefix, indexing);
						if ( diffPrefix.size() != 1 || diffPrefix.getInt(0) != t1 ) {
//							Log.log.trace("case1.2.1");
							openAndCloseIntervals(sig, sig1, x1);
//							if ( x1.getID() == 7324 && x1.size() == 2 ) {
//								Log.log.trace("case1.2.1");
//								Log.log.trace("prefix1=%s", prefix1);
//								Log.log.trace("sig1=%s", sig1);
//							}
						}
					}
					
				}
				else {
//					Log.log.trace("case2");
					if ( cov1 > maxDiff ) {
//						Log.log.trace("case2.1");
						IntArrayList diffPrefix = siggen.shrinkPrefix(wprefix1);
						siggen.removeTrailingNonCoveringTokens(wprefix1);
						sig1 = siggen.genSignature(wprefix1.prefix, indexing);
						if ( diffPrefix.size() != 1 || diffPrefix.getInt(0) != t2 ) {
//							Log.log.trace("case2.1.1");
							openAndCloseIntervals(sig, sig1, x1);
//							if ( x1.getID() == 7324 && x1.size() == 2 ) {
//								Log.log.trace("case2.1.1");
//								Log.log.trace("prefix1=%s", prefix1);
//								Log.log.trace("sig1=%s", sig1);
//							}
						}
					}
				}
			}
//			Log.log.trace("sig1="+sig1);
			
			x = x1;
			prefix = prefix1;
			if ( sig1 != null ) sig = sig1;
		}
		
		private void openInterval( int token, RecordInterface window ) {
//			Log.log.trace(String.format("openInterval: token=%d\trid=%d\tsidx=%d\tsize=%d\tcount=%d", token, window.getID(), window.getSidx(), window.size(), counter.get(token)));
			if ( counter.get(token) == 0 ) {
				sidxMap.put(token, window.getSidx());
			}
			counter.addTo(token, 1);
		}

		private void closeInterval( int token, RecordInterface window, boolean isLast ) {
//			Log.log.trace(String.format("closeInterval: token=%d\trid=%d\tsidx=%d\tsize=%d\tcount=%d", token, window.getID(), window.getSidx(), window.size(), counter.get(token)));
			
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
//			Log.log.trace("sig.elements="+Arrays.toString(sig.elements()));
//			Log.log.trace("pos="+pos);
			if ( pos >= 0 ) {
				sig.removeInt(pos);
			}
//			Log.log.trace("sig="+sig);
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

		private void openAndCloseIntervals( IntArrayList sig, IntArrayList sig1, RecordInterface window ) {
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
	} // end class WitvMapBuilder
}
