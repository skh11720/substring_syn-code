package snu.kdd.substring_syn.algorithm.index.disk.objects;

public interface IterativePostingListInterface {

	int getIdx();
	void init();
	boolean hasNext();
	void next();
}
