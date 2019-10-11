package snu.kdd.pkwise;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Subrecord;

public class PkwiseIndexBuilder {

	final Int2ObjectMap<ObjectList<WindowInterval>> map;
	final Int2IntOpenHashMap counter;
	final Int2IntOpenHashMap sidxMap;
	
	public static Int2ObjectMap<ObjectList<WindowInterval>> buildTok2WitvMap( PkwiseSearch alg, WindowDataset dataset, int qlen ) {
		int wMin = alg.getLFLB(qlen);
		int wMax = alg.getLFUB(qlen);
		PkwiseIndexBuilder builder = new PkwiseIndexBuilder(dataset.getWindowList(wMin, wMax));
		return builder.map;
	}

	public static Int2ObjectMap<ObjectList<WindowInterval>> buildTok2TwitvMap( WindowDataset dataset, int qlen, double theta ) {
		PkwiseIndexBuilder builder = new PkwiseIndexBuilder(dataset.getTransWindowList(qlen, theta));
		return builder.map;
	}
		
	private PkwiseIndexBuilder( Iterable<Subrecord> windowList ) {
		map = new Int2ObjectOpenHashMap<>();
		counter = new Int2IntOpenHashMap();
		sidxMap = new Int2IntOpenHashMap();
		int head = -1;
		int tail = -1;
		for ( Subrecord window : windowList ) {
			if ( head == -1 ) { // first window
				for ( int token : window.getTokenArray() ) {
					openInterval(token, window);
				}
			}
			else {
				tail = window.getToken(window.size()-1);
				if ( head != tail ) {
					openInterval(tail, window);
					closeInterval(head, window, false);
				}
			}
			head = window.getToken(0);
			
			if ( isLastWindow(window) ) {
				for ( int token : window.getTokenArray() ) {
					closeInterval(token, window, true);
				}
				head = -1;
			}
		}
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
			map.get(token).add( new WindowInterval( window.getID(), window.size(), sidxMap.get(token), window.getSidx()+(isLast?1:0)) );
		}
		counter.addTo(token, -1);
	}
	
	private boolean isLastWindow( Subrecord window ) {
		return (window.getSidx()+window.size() == window.getSuperRecord().size());
	}
}
