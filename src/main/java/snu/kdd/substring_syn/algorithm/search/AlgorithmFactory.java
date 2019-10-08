package snu.kdd.substring_syn.algorithm.search;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.pkwise.PkwiseSearch;
import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;

public class AlgorithmFactory {

	private enum AlgorithmName {
		ExactNaiveSearch,
		GreedyNaiveSearch,
		PrefixSearch,
		ExactPrefixSearch,
		PkwiseSearch,
	}
	
	private enum FilterOption {
		NoFilter,
		IF, // IF
		ICF, // ICF
		IPF, // IPF
		LF, // IPF+LF
		PF, // IPF+LF+PF
		NoIndex, // LF+PF
		NaivePF, // IF+LF+PF
	}
	
	public static AbstractSearch createInstance( CommandLine cmd ) {
		AlgorithmName algName = AlgorithmName.valueOf( cmd.getOptionValue("alg") );
		String paramStr = cmd.getOptionValue("param");
		DictParam param = new DictParam(paramStr);
		switch ( algName ) {
		case ExactNaiveSearch: return createExactNaiveSearch(param);
		case GreedyNaiveSearch: return createGreedyNaiveSearch(param);
		case PrefixSearch: return createPrefixSearch(param, false);
		case ExactPrefixSearch: return createPrefixSearch(param, true);
		case PkwiseSearch: return createPkwiseSearch(param);
		default: throw new RuntimeException("Unexpected error");
		}
	}
	
	private static ExactNaiveSearch createExactNaiveSearch( DictParam param ) {
		double theta = Double.parseDouble(param.get("theta"));
		return new ExactNaiveSearch(theta);
	}
	
	private static GreedyNaiveSearch createGreedyNaiveSearch( DictParam param ) {
		double theta = Double.parseDouble(param.get("theta"));
		return new GreedyNaiveSearch(theta);
	}
	
	private static PrefixSearch createPrefixSearch( DictParam param, boolean isExact ) {
		double theta = Double.parseDouble(param.get("theta"));
		boolean bLF = false, bPF = false;
		IndexChoice indexChoice;
	
		if ( param.containsKey("filter")) {
			indexChoice = IndexChoice.None;
			bLF = bPF = false;
			switch (FilterOption.valueOf(param.get("filter"))) {
			case PF: bPF = true;
			case LF: bLF = true;
			case IPF: indexChoice = IndexChoice.Position; break;
			case ICF: indexChoice = IndexChoice.Count; break;
			case IF: indexChoice = IndexChoice.Naive; break;
			case NoFilter: break;
			case NaivePF: indexChoice = IndexChoice.Naive;
			case NoIndex: bLF = bPF = true; break;
			default: throw new RuntimeException("Unexpected error");
			}
		}
		else {
			bLF = Boolean.parseBoolean(param.get("bLF"));
			bPF = Boolean.parseBoolean(param.get("bPF"));
			indexChoice = IndexChoice.valueOf(param.get("index_impl"));
		}
		if ( indexChoice == IndexChoice.Position ) {
			if ( isExact ) return new ExactPositionPrefixSearch(theta, bLF, bPF, indexChoice);
			else return new PositionPrefixSearch(theta, bLF, bPF, indexChoice);
		}
		else {
			if ( isExact ) return new ExactPrefixSearch(theta, bLF, bPF, indexChoice);
			else return new PrefixSearch(theta, bLF, bPF, indexChoice);
		}
	}
	
	private static PkwiseSearch createPkwiseSearch( DictParam param ) {
		double theta = Double.parseDouble(param.get("theta"));
		int qlen = Integer.parseInt(param.get("qlen"));
		int kmax = Integer.parseInt(param.get("kmax"));
		return new PkwiseSearch(theta, qlen, kmax);
	}
	
	
	
	private static class DictParam {
		Object2ObjectOpenHashMap<String, String> map = new Object2ObjectOpenHashMap<>();
		
		public DictParam( String paramStr ) {
			for ( String param : paramStr.split(",") ) {
				String[] pair = param.split(":");
				map.put(pair[0], pair[1]);
			}
		}
		
		public String get( String key ) {
			if ( map.containsKey(key) ) return map.get(key);
			else throw new RuntimeException("No such key in DictParam: "+key);
		}
		
		public boolean containsKey( String key ) {
			return map.containsKey(key);
		}
	}
}
