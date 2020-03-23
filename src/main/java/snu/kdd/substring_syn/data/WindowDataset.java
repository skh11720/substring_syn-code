package snu.kdd.substring_syn.data;

import java.util.Iterator;

import snu.kdd.pkwise.IterableConcatenator;
import snu.kdd.substring_syn.algorithm.filter.TransLenCalculator;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

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
	
	public Iterable<RecordInterface> getTransWindowList( int qlen, double theta ) {
		return new Iterable<RecordInterface>() {
			
			@Override
			public Iterator<RecordInterface> iterator() {
				return new TransWindowIterator(store.getRecords().iterator(), qlen, theta);
			}
		};
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

	class TransWindowIterator implements Iterator<RecordInterface> {

		final Iterator<TransformableRecordInterface> rIter;
		TransformableRecordInterface rec = null;
		TransLenCalculator transLen;
		int sidx, eidx;
		final int qlen;
		final double theta;
		
		public TransWindowIterator( Iterator<TransformableRecordInterface> rIter, int qlen, double theta ) {
			this.rIter = rIter;
			this.qlen = qlen;
			this.theta = theta;
			sidx = 0;
			eidx = -1;
			findNextWindow();
		}
		
		protected void preprocessRecord() {
			double modifiedTheta = Util.getModifiedTheta(qlen, rec, theta);
			transLen = new TransLenCalculator(null, rec, modifiedTheta);
		}
		
		protected void findNextWindow() {
			if ( rec != null && findNextWindowInRecord() ) return;
			rec = null;
			while ( rIter.hasNext() ) {
				rec = rIter.next();
				sidx = 0;
				eidx = -1;
				preprocessRecord();
				if ( findNextWindowInRecord() ) break;
				else rec = null;
			}
		}
		
		protected boolean findNextWindowInRecord() {
			eidx += 1;
			for ( ; sidx<rec.size(); ++sidx ) {
				for ( ; eidx<rec.size(); ++eidx ) {
//					Log.log.trace("id: "+rec.getID()+"\t"+ "len: "+(eidx-sidx+1) +"\t"+ "range: "+sidx+", "+(eidx+1) +"\t"+ "bound: "+transLen.getLFLB(sidx, eidx)+"\t"+transLen.getLFUB(sidx, eidx) );
					if ( transLen.getLFLB(sidx, eidx) <= qlen && qlen <= transLen.getLFUB(sidx, eidx) ) {
						return true;
					}
				}
				eidx = sidx+1;
			}
			return false;
		}

		@Override
		public boolean hasNext() {
			return rec != null;
		}

		@Override
		public Subrecord next() {
			int sidx0 = sidx;
			int eidx0 = eidx;
			TransformableRecordInterface rec0 = rec;
			findNextWindow();
//			System.out.println(rec0.getID()+"\t"+sidx0+"\t"+eidx0+"\t"+(eidx0-sidx0+1));
			return new Subrecord(rec0, sidx0, eidx0+1);
		}
	}
}
