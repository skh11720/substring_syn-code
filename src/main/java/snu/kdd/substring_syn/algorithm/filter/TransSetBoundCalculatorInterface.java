package snu.kdd.substring_syn.algorithm.filter;

public interface TransSetBoundCalculatorInterface {
	
	public int getLB( int i, int j );
	public int getLBMono( int i, int j );
	public int getUB( int i, int j );
	public int getLFLB( int i, int j );
	public int getLFLBMono( int i, int j );
	public int getLFUB( int i, int j );
}
