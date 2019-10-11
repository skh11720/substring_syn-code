package snu.kdd.pkwise;

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

public class PkwiseIndexBuilder {

	static Int2ObjectMap<ObjectList<WindowInterval>> map;
	static Int2IntOpenHashMap counter;
	static Int2IntOpenHashMap sidxMap;
	
	public static Int2ObjectMap<ObjectList<WindowInterval>> buildTok2WitvMap( PkwiseSearch alg, WindowDataset dataset, int qlen ) {
		int wMin = alg.getLFLB(qlen);
		int wMax = alg.getLFUB(qlen);

		map = new Int2ObjectOpenHashMap<>();
		counter = new Int2IntOpenHashMap();
		sidxMap = new Int2IntOpenHashMap();
		int head = -1;
		int tail = -1;
		for ( Subrecord window : dataset.getWindowList(wMin, wMax)) {
			if ( head == -1 ) { // first window
				for ( int token : window.getTokenArray() ) {
					openInterval(token, window);
				}
			}
			else {
				tail = window.getToken(window.size()-1);
				if ( head != tail ) {
					openInterval(tail, window);
					closeInterval(head, window.getID(), window.size(), window.getSidx(), false);
				}
			}
			head = window.getToken(0);
			
			if ( isLastWindow(window) ) {
				for ( int token : window.getTokenArray() ) {
					closeInterval(token, window.getID(), window.size(), window.getSidx(), true);
				}
				head = -1;
			}
		}
		return map;
	}

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
		return (window.getSidx()+window.size() == window.getSuperRecord().size());
	}
}
