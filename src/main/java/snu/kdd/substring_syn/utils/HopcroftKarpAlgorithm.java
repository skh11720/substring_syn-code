package snu.kdd.substring_syn.utils;

import java.util.Arrays;
import java.util.LinkedList;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.IntPair;

public class HopcroftKarpAlgorithm {
	
	private static final int inf = Integer.MAX_VALUE;
	final BipartiteGraph G;
	final int[] pair;
	final int[] d;
	
	public HopcroftKarpAlgorithm(BipartiteGraph G) {
		this.G = G;
		pair = new int[G.nV];
		d = new int[G.nV+1];
		Arrays.fill(pair, G.nV);
	}
	
	public ObjectList<IntPair> run() {
		while (bfs()) {
			for ( int u : G.L ) {
				if ( pair[u] == G.nV ) {
					if (dfs(u)) {
					}
				}
			}
		}
		ObjectList<IntPair> output = new ObjectArrayList<IntPair>();
		for ( int u : G.L ) {
			if (pair[u] != G.nV) output.add(new IntPair(u, pair[u]));
		}
		return output;
	}
	
	private boolean bfs() {
		LinkedList<Integer> Q = new LinkedList<>();
		for ( int u : G.L ) {
			if ( pair[u] == G.nV ) {
				d[u] = 0;
				Q.add(u);
			}
			else d[u] = inf;
		}
		d[G.nV] = inf;
		while (!Q.isEmpty()) {
			int u = Q.pollFirst();
			if ( d[u] < d[G.nV] ) {
				for ( int v : G.adjList.get(u) ) {
					if ( d[pair[v]] == inf ) {
						d[pair[v]] = d[u]+1;
						Q.add(pair[v]);
					}
				}
			}
		}
		return d[G.nV] != inf;
	}
	
	private boolean dfs(int u) {
		if (u != G.nV) {
			for ( int v : G.adjList.get(u) ) {
				if ( d[pair[v]] == d[u]+1 ) {
					if ( dfs(pair[v]) ) {
						pair[v] = u;
						pair[u] = v;
						return true;
					}
				}
			}
			d[u] = inf;
			return false;
		}
		return true;
	}
}
