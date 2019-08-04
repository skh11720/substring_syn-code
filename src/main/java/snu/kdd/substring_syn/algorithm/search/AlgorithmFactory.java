package snu.kdd.substring_syn.algorithm.search;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch.IndexChoice;

public class AlgorithmFactory {

	private enum AlgorithmName {
		ExactNaiveSearch,
		PrefixSearch,
	}
	
	public static AbstractSearch createInstance( CommandLine cmd ) {
		AlgorithmName algName = AlgorithmName.valueOf( cmd.getOptionValue("alg") );
		String paramStr = cmd.getOptionValue("param");
		DictParam param = new DictParam(paramStr);
		switch ( algName ) {
		case ExactNaiveSearch: return createExactNaiveSearch(param);
		case PrefixSearch: return createPrefixSearch(param);
		default: throw new RuntimeException("Unexpected error");
		}
	}
	
	private static ExactNaiveSearch createExactNaiveSearch( DictParam param ) {
		double theta = Double.parseDouble(param.get("theta"));
		return new ExactNaiveSearch(theta);
	}
	
	private static PrefixSearch createPrefixSearch( DictParam param ) {
		double theta = Double.parseDouble(param.get("theta"));
		boolean idxFilter_query = Boolean.parseBoolean(param.get("bIFQ"));
		boolean idxFilter_text = Boolean.parseBoolean(param.get("bIFT"));
		boolean lf_query = Boolean.parseBoolean(param.get("bLFQ"));
		boolean lf_text = Boolean.parseBoolean(param.get("bLFT"));
		IndexChoice indexChoice = IndexChoice.valueOf(param.get("index_impl"));
		return new PrefixSearch(theta, idxFilter_query, idxFilter_text, lf_query, lf_text, indexChoice);
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
	}
}
