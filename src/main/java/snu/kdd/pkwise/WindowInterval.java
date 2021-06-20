package snu.kdd.pkwise;

public class WindowInterval {

	public final int rid;
	public final int w;
	public final int sidx;
	public final int eidx; 	
	public WindowInterval( int rid, int w, int sidx, int eidx ) {
		this.rid = rid;
		this.w = w;
		this.sidx = sidx;
		this.eidx = eidx;
	}
	
	@Override
	public String toString() {
		return String.format("<%d, %d, [%d, %d)>", rid, w, sidx, eidx);
	}
}
