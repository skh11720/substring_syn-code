package snu.kdd.substring_syn.algorithm.index.disk;

public interface DiskBasedPositionalIndexInterface {

	class InvListEntry {
		public final int ridx;
		public final int pos;
		
		public InvListEntry( int ridx, int pos ) {
			this.ridx = ridx;
			this.pos = pos;
		}
		
		@Override
		public String toString() {
			return String.format("(%d, %d)", ridx, pos);
		}
	}
	
	class TransInvListEntry {
		public final int ridx;
		public final int left;
		public final int right;
		
		public TransInvListEntry( int ridx, int left, int right ) {
			this.ridx = ridx; 
			this.left = left;
			this.right = right;
		}
		
		@Override
		public String toString() {
			return String.format("(%d, %d, %d)", ridx, left, right);
		}
	}
}
