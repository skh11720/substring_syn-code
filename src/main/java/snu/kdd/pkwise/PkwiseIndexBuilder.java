package snu.kdd.pkwise;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Log;

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
		int head = -1;
		int tail = -1;
		Subrecord windowPrev = null;
		
		for ( Subrecord window : dataset.getTransWindowList(qlen, theta) ) {
			for ( int token : window.getTokenArray() ) {
				openInterval(token, window);
				closeInterval(token, window.getID(), window.size(), window.getSidx()+1, false);
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
