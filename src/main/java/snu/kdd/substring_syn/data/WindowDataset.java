package snu.kdd.substring_syn.data;

import java.util.Iterator;

import snu.kdd.pkwise.IterableConcatenator;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.StatContainer;

public class WindowDataset extends DiskBasedDataset {

	protected final int qlen;

	public WindowDataset(StatContainer statContainer, DatasetParam param, Ruleset ruleset, RecordStore store) {
		super(statContainer, param, ruleset, store);
		qlen = Integer.parseInt(param.qlen);
	}

	@Override
	public void addStat() {
		super.addStat();
		statContainer.setStat("Size_Recordstore", store.diskSpaceUsage().toString());
	}

	
	public Iterable<Subrecord> getWindowList( int w ) {
		return new Iterable<Subrecord>() {
			
			@Override
			public Iterator<Subrecord> iterator() {
				return new WindowIterator(store.getRecords().iterator(), w);
			}
		};
	}

	public Iterable<Subrecord> getWindowList( int wMin, int wMax ) {
		IterableConcatenator<Subrecord> iconcat = new IterableConcatenator<>();
		for ( int w=wMin; w<=wMax; ++w ) iconcat.addIterable(getWindowList(w));
		return iconcat.iterable();
	}
	
	public static class WindowIterator implements Iterator<Subrecord> {

		final Iterator<TransformableRecordInterface> rIter;
		TransformableRecordInterface rec = null;
		TransformableRecordInterface recNext = null;
		int widx = -1;
		final int w;
		
		public WindowIterator( Iterator<TransformableRecordInterface> rIter, int w ) {
			this.rIter = rIter;
			this.w = w;
			while ( rIter.hasNext() ) {
				rec = rIter.next();
				if ( rec.size() >= w ) break;
				else rec = null;
			}
			while ( rIter.hasNext() ) {
				recNext = rIter.next();
				if ( recNext.size() >= w ) break;
				else recNext = null;
			}
		}

		@Override
		public boolean hasNext() {
			return (recNext != null || widx +w < rec.size());
		}

		@Override
		public Subrecord next() {
			widx += 1;
			if ( widx+w > rec.size() ) {
				rec = recNext;
				recNext = null;
				while ( rIter.hasNext() ) {
					recNext = rIter.next();
					if ( recNext.size() >= w ) break;
					else recNext = null;
				}
				widx = 0;
			}
//			if ( rec.getID() == 7324 && w == 2 ) Log.log.trace("window: "+(new Subrecord(rec, widx, widx+w))+"\t"+(new Subrecord(rec, widx, widx+w)).toOriginalString());
			return new Subrecord(rec, widx, widx+w);
		}
	}
}
