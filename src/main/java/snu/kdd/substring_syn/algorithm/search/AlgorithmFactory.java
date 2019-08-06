package snu.kdd.substring_syn.algorithm.search;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch.IndexChoice;

public class AlgorithmFactory {

	private enum AlgorithmName {
		ExactNaiveSearch,
		GreedyNaiveSearch,
		PrefixSearch,
	}
	
	private enum FilterOption {
		NoFilter,
		IF, // IF
		ICF, // ICF
		IPF, // IPF
		LF, // IPF+LF
		PF, // IPF+LF+PF
		NoIndex, // LF+PF
	}
	
	public static AbstractSearch createInstance( CommandLine cmd ) {
		AlgorithmName algName = AlgorithmName.valueOf( cmd.getOptionValue("alg") );
		String paramStr = cmd.getOptionValue("param");
		DictParam param = new DictParam(paramStr);
		switch ( algName ) {
		case ExactNaiveSearch: return createExactNaiveSearch(param);
		case GreedyNaiveSearch: return createGreedyNaiveSearch(param);
		case PrefixSearch: return createPrefixSearch(param);
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
	
	private static PrefixSearch createPrefixSearch( DictParam param ) {
		double theta = Double.parseDouble(param.get("theta"));
		boolean bIF = false, bICF = false, bLF = false, bPF = false;
		IndexChoice indexChoice;
	
		if ( param.containsKey("filter")) {
			indexChoice = IndexChoice.Naive;
			bIF = bLF = bPF = false;
			switch (FilterOption.valueOf(param.get("filter"))) {
			case PF: bPF = true;
			case LF: bLF = true;
			case IPF: indexChoice = IndexChoice.Position;
			case ICF: bICF = true;
			case IF: bIF = true;
			case NoFilter: break;
			case NoIndex: bLF = bPF = true; break;
			default: throw new RuntimeException("Unexpected error");
			}
		}
		else {
			bIF = Boolean.parseBoolean(param.get("bIF"));
			bICF = Boolean.parseBoolean(param.get("bICF"));
			bLF = Boolean.parseBoolean(param.get("bLF"));
			bPF = Boolean.parseBoolean(param.get("bPF"));
			indexChoice = IndexChoice.valueOf(param.get("index_impl"));
		}
		return new PrefixSearch(theta, bIF, bICF, bLF, bPF, indexChoice);
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
