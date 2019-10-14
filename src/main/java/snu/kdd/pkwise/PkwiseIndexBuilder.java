package snu.kdd.pkwise;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.IntTriple;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Util;

public class PkwiseIndexBuilder {

	static Int2ObjectMap<ObjectList<WindowInterval>> map;
	static Int2IntOpenHashMap counter;
	static Int2IntOpenHashMap sidxMap;
	static double theta;

	public static Int2ObjectMap<ObjectList<WindowInterval>> buildTok2WitvMap( PkwiseSearch alg, WindowDataset dataset, int qlen, double theta ) {
		PkwiseIndexBuilder.theta = theta;
		WitvMapBuilder builder = new WitvMapBuilder(dataset, qlen, qlen);
		return builder.build();
	}
	
	public static Int2ObjectMap<ObjectList<WindowInterval>> buildTok2WitvMap( PkwiseSynSearch alg, WindowDataset dataset, int qlen, double theta ) {
		PkwiseIndexBuilder.theta = theta;
		int wMin = alg.getLFLB(qlen);
		int wMax = alg.getLFUB(qlen);
		WitvMapBuilder builder = new WitvMapBuilder(dataset, wMin, wMax);
		return builder.build();
	}
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
	
	public static Int2ObjectMap<ObjectList<WindowInterval>> buildTok2TwitvMap( WindowDataset dataset, int qlen, double theta ) {
		map = new Int2ObjectOpenHashMap<>();
		counter = new Int2IntOpenHashMap();
		sidxMap = new Int2IntOpenHashMap();
		Object2ObjectMap<IntTriple, IntList> tmpMap = new Object2ObjectOpenHashMap<>();
		int rid = -1;
		
		for ( Subrecord window : dataset.getTransWindowList(qlen, theta) ) {
			if ( rid != window.getID() ) {
				rid = window.getID();
				for ( Entry<IntTriple, IntList> e : tmpMap.entrySet() ) {
					IntTriple key = e.getKey();
					int token = key.i1;
					if ( !map.containsKey(token) ) map.put(token, new ObjectArrayList<>());
					int recId = key.i2;
					int w = key.i3;
					IntList list = e.getValue(); 
					int sidx = list.get(0);
					int eidx = sidx+1;
					for ( int i=1; i<list.size(); ++i ) {
						if ( list.get(i) == sidx+1 ) eidx = list.get(i)+1;
						else {
							map.get(token).add(new WindowInterval(recId, w, sidx, eidx));
							sidx = list.get(i);
							eidx = sidx+1;
						}
					}
					map.get(token).add(new WindowInterval(recId, w, sidx, eidx));
					
				}
				tmpMap.clear();
			}
			for ( int token : window.getTokenArray() ) {
				openInterval(token, window);
//				closeInterval(token, window.getID(), window.size(), window.getSidx()+1, false);
				IntTriple key = new IntTriple(token, window.getID(), window.size());
				if ( !tmpMap.containsKey(key) ) tmpMap.put(key, new IntArrayList());
				tmpMap.get(key).add(window.getSidx());
			}
		}
		
		return map;
	}
		
	private static void openInterval( int token, Subrecord window ) {
		if ( counter.get(token) == 0 ) {
//			Log.log.trace("openInterval: "+token+"\t"+Record.tokenIndex.getToken(token));
			sidxMap.put(token, window.getSidx());
		}
		counter.addTo(token, 1);
	}
	
	private static void closeInterval( int token, int rid, int w, int idx, boolean isLast ) {
		if ( counter.get(token) == 1 ) {
			if ( !map.containsKey(token) ) map.put(token, new ObjectArrayList<>());
			int sidx0 = sidxMap.get(token);
			int eidx0 = idx+(isLast?1:0);
			if ( sidx0 >= eidx0 ) {
				System.err.println("isLast="+isLast);
				System.err.println("token="+Record.tokenIndex.getToken(token)+"  ("+token+")");
				System.err.println("rec.id="+rid);
				System.err.println("sidx0="+sidx0);
				System.err.println("eidx0="+eidx0);
				throw new RuntimeException();
			}
//			Log.log.trace("closeInterval: token="+token+", "+Record.tokenIndex.getToken(token)+"\trid="+rid+"\tw="+w+"\tsidx0="+sidx0+"\teidx0="+eidx0);
			map.get(token).add( new WindowInterval( rid, w, sidx0, eidx0 ) );
		}
		counter.addTo(token, -1);
	}
	
	private static boolean isLastWindow( Subrecord window ) {
		if ( window == null ) return false;
		return (window.getSidx()+window.size() == window.getSuperRecord().size());
	}
	
	public static class WitvMapBuilder {
		Iterator<Subrecord> windowList;
		Int2ObjectMap<ObjectList<WindowInterval>> map;
		Int2IntOpenHashMap counter;
		Int2IntOpenHashMap sidxMap;
		Subrecord x;
		int l;
		IntArrayList sig;
		int cov;
		boolean debug = true;
		
		public WitvMapBuilder( WindowDataset dataset, int wMin, int wMax ) {
			windowList = dataset.getWindowList(wMin, wMax).iterator();
			map = new Int2ObjectOpenHashMap<>();
			counter = new Int2IntOpenHashMap();
			sidxMap = new Int2IntOpenHashMap();
			x = null;
			l = 0;
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
				for ( int token : sig ) {
					closeInterval(token, x, true);
				}
				x = null;
				l = 0;
				sig = null;
				return;
			}
			
			Subrecord x1 = windowList.next();
//			Log.log.trace("window: rid="+x1.getID()+"\tsidx="+x1.getSidx()+"\tsize="+x1.size()+"\twindow="+x1.getTokenList());
			int maxDiff = Util.getPrefixLength(x1, theta);
			IntArrayList sig1;
			
			if ( x == null ) { // x1 is the first window
//				Log.log.trace("First window");
				sig1 = getSig(x1);
				for ( int token : sig1 ) {
					openInterval(token, x1);
				}
			}

			else {
//				Log.log.trace("Intermediate window");
				int t1 = x.getToken(0);
				int t2 = x1.getToken(x1.size()-1);
//				Log.log.trace("t1="+t1+"\tt2="+t2);
				sig1 = new IntArrayList(sig);
				if ( sig.contains(t1) ) removeFromSig(sig1, t1);
				if ( t2 < sig1.getInt(sig1.size()-1) ) addToSig(sig1, t2);
				int cov = getCov(sig, -1);
				int cov1 = getCov(sig1, -1);

//				System.out.println("rid="+x1.getID());
//				System.out.println("t1="+t1+"\tt2="+t2);
//				System.out.println("sig="+sig);
//				System.out.println("sig1="+sig1);
//				System.out.println("cov1="+cov1);
//				System.out.println("sig1.size="+sig1.size()+"\tcov="+cov+"\tmaxDiff="+maxDiff);
				
				if ( getCov(sig, t1) < maxDiff ) {
//					Log.log.trace("case1");
					if ( cov1 == maxDiff ) {
//						Log.log.trace("case1.1");
//						while ( sig1.getInt(sig1.size()-1) ) {
//						}
						// while tail(P') are ...
						if ( t1 != t2 ) {
//							Log.log.trace("case1.1.1");
							openInterval(t2, x1);
							closeInterval(t1, x1, false);
						}
					}
					else {
//						Log.log.trace("case1.2");
//						throw new RuntimeException("NOT AVAILABLE NOW1");
						Iterator<Integer> iter = x1.getTokenList().stream().sorted().iterator();
						for ( int i=0; i<sig1.size(); ++i ) iter.next();
						int dl = 0;
						int cov2 = 0;
						while ( cov2 < 1 ) {
							dl += 1;
							cov2 += 1;
						}
						assert (dl == 1);
						t2 = iter.next();
//						Log.log.trace("t2="+t2);
						addToSig(sig1, t2);
						if ( t1 != t2 ) {
//							Log.log.trace("case1.2.1");
							openInterval(t2, x1);
							closeInterval(t1, x1, false);
						}
					}
					
				}
				else {
//					Log.log.trace("case2");
//					throw new RuntimeException("NOT AVAILABLE NOW2");
					if ( cov1 > maxDiff ) {
//						Log.log.trace("case2.1");
						Iterator<Integer> iter = sig1.stream().sorted((x,y)->Integer.compare(y, x)).iterator();
						int dl = 0;
						int cov2 = 0;
						while ( cov2 < 1 ) {
							dl += 1;
							cov2 += 1;
						}
						int t3 = iter.next();
//						Log.log.trace("t3="+t3);
						removeFromSig(sig1, t3);
//						Log.log.trace("sig1="+sig1);
						// while tail(P') are ...
						if ( t3 != t2 ) {
//							Log.log.trace("case2.1.1");
							openInterval(t2, x1);
							closeInterval(t3, x1, false);
						}
					}
				}
			}
//			Log.log.trace("sig1="+sig1);
			
			x = x1;
			sig = sig1;
		}
		
		private void openInterval( int token, Subrecord window ) {
//			Log.log.trace(String.format("openInterval: token=%d\trid=%d\tsidx=%d\tsize=%d\tcount=%d", token, window.getID(), window.getSidx(), window.size(), counter.get(token)));
			if ( counter.get(token) == 0 ) {
				sidxMap.put(token, window.getSidx());
			}
			counter.addTo(token, 1);
		}

		private void closeInterval( int token, Subrecord window, boolean isLast ) {
//			Log.log.trace(String.format("closeInterval: token=%d\trid=%d\tsidx=%d\tsize=%d\tcount=%d", token, window.getID(), window.getSidx(), window.size(), counter.get(token)));
			
			if ( counter.get(token) == 1 ) {
				if ( !map.containsKey(token) ) map.put(token, new ObjectArrayList<>());
				int sidx0 = sidxMap.get(token);
				int eidx0 = window.getSidx()+(isLast?1:0);
				map.get(token).add( new WindowInterval( window.getID(), window.size(), sidx0, eidx0 ) );
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
		
		private void addToSig( IntArrayList sig, int token ) {
			int pos = Collections.binarySearch(sig, token);
			if ( pos >= 0 ) sig.add(pos, token);
			else {
				pos = -pos-1;
				if ( pos >= sig.size() ) sig.add(token);
				else sig.add(pos, token);
			}
		}
		
		private IntArrayList getSig( Subrecord window ) {
			IntArrayList sig = new IntArrayList( window.getTokenList().stream().sorted().limit(Util.getPrefixLength(window, theta)).iterator() );
			return sig;
		}
		
		private int getCov( IntArrayList sig, int ignored ) {
			int cov = 0;
			for ( int token : sig ) {
				if ( token == ignored ) continue;
				cov += 1;
			}
			return cov;
		}
	} // end class WitvMapBuilder
}
