package snu.kdd.substring_syn.algorithm.validator;

import java.util.Arrays;
import java.util.stream.IntStream;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.BipartiteGraph;
import snu.kdd.substring_syn.utils.HopcroftKarpAlgorithm;
import snu.kdd.substring_syn.utils.StatContainer;

public class ImprovedGreedyValidator extends GreedyValidator {

	public ImprovedGreedyValidator(double theta, StatContainer statContainer) {
		super(theta, statContainer);
	}

	@Override
	public int[] getTransform(RecordInterface trans, RecordInterface target) {
		if ( isSingleTokenTransform(trans) ) return getTransformForSingleTokenTransform(trans, target);
		else return super.getTransform(trans, target);
	}
	
	protected static final boolean isSingleTokenTransform(RecordInterface rec) {
		for ( Rule rule : rec.getApplicableRuleIterable() ) {
			if ( rule.lhsSize() > 1 || rule.rhsSize() > 1 ) return false;
		}
		return true;
	}

	protected static final int[] getTransformForSingleTokenTransform(RecordInterface trans, RecordInterface target) {
		BipartiteGraph G = buildGraph(trans, target);
		HopcroftKarpAlgorithm alg = new HopcroftKarpAlgorithm(G);
		ObjectList<IntPair> pairList = alg.run();
		int[] transformed = Arrays.copyOf(trans.getTokenArray(), trans.size());
		for ( IntPair pair : pairList ) transformed[pair.i1] = target.getToken(pair.i2-trans.size());
		return transformed;
	}

	protected static final BipartiteGraph buildGraph(RecordInterface trans, RecordInterface target) {
		Int2IntMap map = getTok2posMap(target);
		int[] L = IntStream.range(0, trans.size()).toArray();
		int[] R = IntStream.range(trans.size(), trans.size()+target.size()).toArray();
		Int2ObjectMap<IntList> adjList = new Int2ObjectOpenHashMap<>();
		for ( int i=0; i<trans.size(); ++i ) {
			adjList.put(i, new IntArrayList());
			for ( Rule rule : trans.getApplicableRules(i) ) {
				int token = rule.getRhs()[0];
				if ( map.containsKey(token) ) {
					int pos = map.get(token);
					adjList.get(i).add(trans.size()+pos);
				}
			}
		}
		return new BipartiteGraph(L, R, adjList);
	}
	
	protected static final Int2IntMap getTok2posMap(RecordInterface rec) {
		Int2IntMap tok2posMap = new Int2IntOpenHashMap();
		for ( int i=0; i<rec.size(); ++i ) tok2posMap.put(rec.getToken(i), i);
		return tok2posMap;
	}
}
