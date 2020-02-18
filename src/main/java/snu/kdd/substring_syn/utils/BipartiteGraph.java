package snu.kdd.substring_syn.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class BipartiteGraph {
	public final int nV;
	public final IntList L;
	public final IntList R;
	public final Int2ObjectMap<IntList> adjList;

	public BipartiteGraph(int[] L, int[] R, Int2ObjectMap<IntList> adjList) {
		this(IntArrayList.wrap(L), IntArrayList.wrap(R), adjList);
	}
	
	public BipartiteGraph(IntList L, IntList R, Int2ObjectMap<IntList> adjList) {
		this.L = L;
		this.R = R;
		this.nV = L.size() + R.size();
		this.adjList = adjList;
	}
	
	@Override
	public String toString() {
		StringBuilder strbld = new StringBuilder("{\n  L="+L+"\n  R="+R+"\n  adjList={\n");
		for ( int key=0; key<nV; ++key ) strbld.append("\t"+key+"=>"+adjList.get(key)+",\n");
		strbld.append("\t}\n}");
		return strbld.toString();
	}
}
