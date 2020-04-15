package snu.kdd.substring_syn.algorithm.index.inmem;

import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

public interface PositionalIndexInterface {

	class InvListEntry {
		final TransformableRecordInterface rec;
		final int pos;
		
		public InvListEntry( TransformableRecordInterface rec, int pos ) {
			this.rec = rec;
			this.pos = pos;
		}
		
		@Override
		public String toString() {
			return String.format("(%d, %d)", rec.getIdx(), pos);
		}
	}
	
	class TransInvListEntry {
		final TransformableRecordInterface rec;
		final int left;
		final int right;
		
		public TransInvListEntry( TransformableRecordInterface rec, int left, int right ) {
			this.rec = rec;
			this.left = left;
			this.right = right;
		}
		
		@Override
		public String toString() {
			return String.format("(%d, %d, %d)", rec.getIdx(), left, right);
		}
	}
}
