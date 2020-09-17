package snu.kdd.etc;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.utils.BipartiteGraph;
import snu.kdd.substring_syn.utils.HopcroftKarpAlgorithm;

public class BipartiteMatchingTest {
	
	static Random rn = new Random(0);
	
	class Edge {
		/* undirected */
		final int u, v;
		
		public Edge(int u, int v) {
			this.u = u;
			this.v = v;
		}
		
		@Override
		public int hashCode() {
			return u+v;
		}
		
		@Override
		public boolean equals(Object obj) {
			if ( obj == null ) return false;
			Edge o = (Edge)obj;
			return u == o.u && v == o.v || u == o.v && v == o.u;
		}
		
		@Override
		public String toString() {
			return "("+u+","+v+")";
		}
	}


	

	
	class Matching {
		final ObjectSet<Edge> edges = new ObjectOpenHashSet<Edge>();
		final Int2IntMap matches = new Int2IntOpenHashMap();
		
		public Matching() {}
		
		public Matching(ObjectSet<Edge> edges) {
			for ( Edge e : edges ) add(e);
		}
		
		public void add(Edge e) {
			if ( !isVertexDisjoint(e) ) {
				Exception exc = new Exception("Edge "+e+" is not disjoint to this Matching.");
				exc.printStackTrace();
				System.exit(1);
			}
			edges.add(e);
			matches.put(e.u, e.v);
			matches.put(e.v, e.u);
		}
		
		public boolean containsVertex(int u) {
			return matches.containsKey(u);
		}
		
		public boolean isVertexDisjoint(Edge e) {
			return !(matches.containsKey(e.u) || matches.containsKey(e.v));
		}
		
		public int size() { return edges.size(); }
		
		@Override
		public String toString() {
			return edges.toString();
		}
	}
	
	class Path {
		final ObjectList<Edge> list = new ObjectArrayList<Edge>();
		final IntSet V = new IntOpenHashSet();
		
		public Path(Edge... edges) {
			for ( Edge e : edges ) add(e); 
		}
		
		@Override
		protected Path clone() {
			Path path = new Path();
			for (Edge e : list) path.add(e);
			return path;
		}

		public int size() { return list.size(); }
		
		public int getLastNode() {
			if (size() == 0) {
				Exception e = new Exception("Path is empty.");
				e.printStackTrace();
				System.exit(1);
			}
			return list.get(size()-1).v;
		}
		
		public void add(Edge e) { 
			if ( size() > 0 && getLastNode() != e.u ) {
				Exception exc = new Exception("The edge "+e+" cannot be appended to the Path "+this+".");
				exc.printStackTrace();
				System.exit(1);
			}
			list.add(e);
			V.add(e.u);
			V.add(e.v);
		}
		
		@Override
		public String toString() {
			return list.toString();
		}
	}

	
	public BipartiteGraph generateBipartiteGraph(int nV, int degMax) {
		IntList L = new IntArrayList();
		IntList R = new IntArrayList();
		L.add(0);
		R.add(1);
		for ( int i=2; i<nV; ++i ) {
			if ( rn.nextDouble() >= 0.5 ) L.add(i);
			else R.add(i);
		}
		Int2ObjectMap<IntList> adjList = new Int2ObjectOpenHashMap<IntList>();
		for ( int u : L ) adjList.put(u, new IntArrayList());

		for ( int u : L ) {
			int deg = rn.nextInt(degMax);
			for ( int i=0; i<deg; ++i ) {
				int v = R.getInt(rn.nextInt(R.size()));
				adjList.get(u).add(v);
			}
		}
		return new BipartiteGraph(L, R, adjList);
	}

	private static <T> ObjectSet<T> symdiff(ObjectSet<T> x, ObjectSet<T> y) {
		ObjectSet<T> cap = new ObjectOpenHashSet<>(x);
		cap.retainAll(y);
		ObjectSet<T> z = new ObjectOpenHashSet<>();
		for ( T o : x ) if ( !cap.contains(o) ) z.add(o);
		for ( T o : y ) if ( !cap.contains(o) ) z.add(o);
		return z;
	}
	
	
	class NaiveMaximumMatchingAlgorithm {
		final ObjectList<Edge> edgeList;
		final boolean[] state;
		final IntSet V;
		Matching M;
		
		public NaiveMaximumMatchingAlgorithm(BipartiteGraph G) {
			edgeList = new ObjectArrayList<>();
			for ( int u : G.L ) {
				for ( int v : G.adjList.get(u) ) {
					edgeList.add(new Edge(u, v));
				}
			}
			state = new boolean[edgeList.size()];
			V = new IntOpenHashSet();
			M = new Matching();
		}
		
		public Matching run() {
			branch(0);
			return M;
		}
		
		private void branch(int idx) {
			if (idx == state.length) {
//				System.out.println(state2str());
				Matching Mtmp = new Matching();
				for (int i=0; i<state.length; ++i) {
					if ( state[i] ) {
						Edge e = edgeList.get(i);
						Mtmp.add(e);
					}
				}
				if (Mtmp.size() > M.size()) M = Mtmp;
			}
			else {
				Edge e = edgeList.get(idx);
				if ( !V.contains(e.u) && !V.contains(e.v) ) {
					V.add(e.u);
					V.add(e.v);
					state[idx] = true;
					branch(idx+1);
				}
				if (state[idx]) {
					V.remove(e.u);
					V.remove(e.v);
					state[idx] = false;
				}
				branch(idx+1);
			}
		}
		
		@SuppressWarnings("unused")
		private String state2str() {
			StringBuilder strbld = new StringBuilder();
			for ( int i=0; i<state.length; ++i ) {
				if (state[i]) strbld.append("1");
				else strbld.append("0");
			}
			return strbld.toString();
		}
	}
	
	/*
	 * Too slow... not used
	 */
	class HopcroftKarpAlgorithmVERYSLOW {
		
		final BipartiteGraph G;
		IntSet candV;
		Matching M;
		
		public HopcroftKarpAlgorithmVERYSLOW(BipartiteGraph G) {
			this.G = G;
		}
		
		public Matching run() {
			M = new Matching();
			updateMatching();
			return M;
		}

		public void updateMatching() {
			while (true) {
				ObjectList<Path> paths = findAugmentingPaths();
//				System.out.println("paths="+paths);
				if (paths.size() == 0 ) break;
				ObjectSet<Edge> edgesInPath = unionPaths(paths);
				ObjectSet<Edge> edges = symdiff(M.edges, edgesInPath);
				M = new Matching(edges);
//				System.out.println("M="+M);
			}
		}
		
		public ObjectList<Path> findAugmentingPaths() {
			ObjectList<Path> pathList = new ObjectArrayList<Path>();
			candV = getVertices();
			while ( !candV.isEmpty() ) {
				Path path = findShortestAugmentingPath();
//				System.out.println("Augpath="+path);
				if (path == null) break;
				pathList.add(path);
				updateCandVertices(path);
//				System.out.println("candV="+candV);
			}
			return pathList;
		}
		
		public IntSet getVertices() {
			IntSet V = new IntOpenHashSet();
			for ( int i=0; i<G.nV; ++i ) V.add(i);
			return V;
		}
		
		public Path findShortestAugmentingPath() {
			LinkedList<Path> Q = new LinkedList<>();
			for (Edge e : getStartingEdges()) {
//				System.out.println("StartingEdge="+e);
				Path path = new Path(e);
				int u = path.getLastNode();
				if ( !M.containsVertex(u) ) return path;
				Q.add(path);
			}
			while ( !Q.isEmpty() ) {
				Path path = Q.pollFirst();
//				System.out.println("Path="+path);
				int u = path.getLastNode();
				int v = M.matches.get(u);
				path.add(new Edge(u, v));
//				System.out.println("MatchingEdge="+(new Edge(u, v)));
				for (Edge e : getPossibleNextEdges(path) ) {
//					System.out.println("NextEdge="+e);
					Path newpath = path.clone();
					newpath.add(e);
					int w = newpath.getLastNode();
					if ( !M.containsVertex(w) ) return path;
					else Q.add(newpath);
//					if (Q.size() % 1000 == 0 ) System.out.println("Q.size="+Q.size());
				}
			}
			return null;
		}
		
		public Iterable<Edge> getStartingEdges() {
			return new Iterable<Edge>() {
				
				@Override
				public Iterator<Edge> iterator() {
					return new Iterator<Edge>() {
						
						IntIterator uIter = candV.iterator();
						int u = uIter.nextInt();
						int il = 0;
						IntList list = new IntArrayList(G.adjList.get(u).iterator());
						Edge e = findNext();
						
						@Override
						public Edge next() {
							Edge o = e;
							e = findNext();
							return o;
						}
						
						@Override
						public boolean hasNext() {
							return e != null;
						}
						
						private Edge findNext() {
							while (uIter.hasNext()) {
								if ( M.containsVertex(u) ) {
									u = uIter.nextInt();
									list = null;
								}
								else {
									if (list == null) list = new IntArrayList(G.adjList.get(u).iterator());
									while (il < list.size()) {
										int v = list.getInt(il);
										il += 1;
										if ( candV.contains(v) ) return new Edge(u, v);
									}
									u = uIter.nextInt();
									il = 0;
									list = null;
								}
							}
							return null;
						}
					};
				}
			};
		}
		
		public Iterable<Edge> getPossibleNextEdges(Path path) {
			return new Iterable<Edge>() {
				
				@Override
				public Iterator<Edge> iterator() {
					return new Iterator<Edge>() {

						final int u = path.getLastNode();
						IntList list = new IntArrayList(G.adjList.get(u));
						int il = 0;
						Edge e = findNext();
						
						@Override
						public Edge next() {
							Edge o = e;
							e = findNext();
//							System.out.println("getPossibleNextEdge, o="+o);
							return o;
						}
						
						@Override
						public boolean hasNext() {
							return e != null;
						}
						
						private Edge findNext() {
							while (il < list.size()) {
								int v = list.getInt(il);
//								System.out.println("getPossibleNextEdge, v="+v+"\t"+candV.contains(v)+"\t"+path.V.contains(v));
								il += 1;
								if ( candV.contains(v) && !path.V.contains(v) ) return new Edge(u, v);
							}
							return null;
						}
					};
				}
			};
		}
		
		private void updateCandVertices(Path path) {
			candV.removeAll(path.V);
		}

		public ObjectSet<Edge> unionPaths(ObjectList<Path> paths) {
			ObjectSet<Edge> edges = new ObjectOpenHashSet<>();
			for ( Path path : paths ) edges.addAll(path.list);
//			System.out.println("unionPath="+edges);
			return edges;
		}
	}

	private Matching intPairs2Matching(ObjectList<IntPair> pairList) {
		Matching M = new Matching();
		for ( IntPair pair : pairList ) M.add(new Edge(pair.i1, pair.i2));
		return M;
	}

	@Test
	public void testCorrectness() {
		int nV = 10;
		int tries = 100000;
		BipartiteGraph[] Garr = new BipartiteGraph[tries];
		for ( int i=0; i<tries; ++i ) Garr[i] = generateBipartiteGraph(nV, 5);
		for ( int i=0; i<tries; ++i ) {
//			if (i != 32) continue;
			System.out.println(i);
			BipartiteGraph G = Garr[i];
			NaiveMaximumMatchingAlgorithm alg0 = new NaiveMaximumMatchingAlgorithm(G);
			Matching M0 = alg0.run();
			HopcroftKarpAlgorithm alg1 = new HopcroftKarpAlgorithm(G);
			Matching M1 = intPairs2Matching(alg1.run());
//			System.out.println("G="+G);
//			System.out.println("M0="+M0);
//			System.out.println("M1="+M1);
			assertEquals(M0.size(), M1.size());
		}
	}
	
	@Test
	public void testEfficiency() {
		/*
		1000	1.009867 ms
		2000	0.7439960000000001 ms
		3000	0.934038 ms
		4000	1.420319 ms
		5000	1.876271 ms
		6000	2.341676 ms
		7000	2.595784 ms
		8000	3.1515980000000003 ms
		9000	3.750607 ms
		10000	3.755422 ms
		 */
		int tries = 100;
		int[] nVArr = IntStream.range(1, 11).map(x->x*1000).toArray();
		long ts;
		long[] tArr = new long[nVArr.length];
		
		for ( int i=0; i<nVArr.length; ++i ) {
			int nV = nVArr[i];
			for ( int j=0; j<tries; ++j ) {
				BipartiteGraph G = generateBipartiteGraph(nV, 5);
//				System.out.println(G);
				HopcroftKarpAlgorithm alg = new HopcroftKarpAlgorithm(G);
				ts = System.nanoTime();
				@SuppressWarnings("unused") 
				Matching M1 = intPairs2Matching(alg.run());
				long t = (System.nanoTime() - ts);
				tArr[i] += t;
//				System.out.println(t/1e6);
			}
			System.out.println(nVArr[i]+"\t"+(tArr[i]/1e6/tries)+" ms");
		}
	}
}
