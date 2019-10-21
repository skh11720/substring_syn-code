package snu.kdd.substring_syn.algorithm.search;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.pkwise.PkwiseNaiveSearch;
import snu.kdd.pkwise.PkwiseSearch;
import snu.kdd.pkwise.PkwiseSynSearch;
import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;

public class AlgorithmFactory {

	public enum AlgorithmName {
		ExactNaiveSearch,
		GreedyNaiveSearch,
		PrefixSearch,
		ExactPrefixSearch,
		PkwiseNaiveSearch,
		PkwiseSearch,
		PkwiseSynSearch,
		ZeroPrefixSearch,
	}
	
	private enum FilterOption {
		NoFilter,
		IF, // IF
		ICF, // ICF (C)
		IPF, // IPF ( C + P )
		IPF_PR, // IPF + PR
		LF, // IPF+LF
		PF, // IPF+LF+PF
		NoIndex, // LF+PF
		NaivePF, // IF+LF+PF

		IPFOnly_L, // IPF + LF
		IPFOnly_L_PR, // IPF + LF + PR
		IPFOnly_PR, // IPF + PF
		ICF_L, // ICF + LF
		ICF_L_PR, // ICF + LF + PR
		ICF_PR, // ICF + PF
		IPFOnly, // IPF Only
		LFOnly, // LF
		PFOnly, // PF
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
		case PkwiseNaiveSearch: return createPkwiseNaiveSearch(param);
		case PkwiseSearch: return createPkwiseSearch(param);
		case PkwiseSynSearch: return createPkwiseSynSearch(param, cmd);
		case ZeroPrefixSearch: return createZeroPrefixSearch(param);
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

	private static AbstractSearch createPrefixSearch( DictParam param, boolean isExact ) {
		return createPrefixSearch(param, isExact, false);
	}
	
	private static AbstractSearch createPrefixSearch( DictParam param, boolean isExact, boolean isZero ) {
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

			case IPFOnly_L: bLF = true; bPF = false; indexChoice = IndexChoice.PositionOnly; break;
			case ICF_L: bLF = true; bPF = false; indexChoice = IndexChoice.Count; break;

			case IPFOnly: bLF = bPF = false; indexChoice = IndexChoice.PositionOnly; break;
			case LFOnly: bLF = true; bPF = false; indexChoice = IndexChoice.Naive; break;
			case PFOnly: bLF = false; bPF = true; indexChoice = IndexChoice.Naive; break;

			case ICF_PR: bLF = false; bPF = true; indexChoice = IndexChoice.Count; break;
			case IPFOnly_PR: bLF = false; bPF = true; indexChoice = IndexChoice.PositionOnly; break;
			
			case ICF_L_PR: bLF = true; bPF = true; indexChoice = IndexChoice.Count; break;
			case IPF_PR: bLF = false; bPF = true; indexChoice = IndexChoice.Position; break;
			case IPFOnly_L_PR: bLF = true; bPF = true; indexChoice = IndexChoice.PositionOnly; break;
			default: throw new RuntimeException("Unexpected error");
			}

//		NoFilter,
//		IF, // IF
//		ICF, // ICF (C)
//		IPF, // IPF ( C + P )
//		IPF_PR, // IPF + PR
//		LF, // IPF+LF
//		PF, // IPF+LF+PF
//		NoIndex, // LF+PF
//		NaivePF, // IF+LF+PF
//
//		IPFOnly_L, // IPF + LF
//		IPFOnly_L_PR, // IPF + LF + PR
//		IPFOnly_PR, // IPF + PF
//		ICF_L, // ICF + LF
//		ICF_L_PR, // ICF + LF + PR
//		ICF_PR, // ICF + PF
//		IPFOnly, // IPF Only
//		LFOnly, // LF
//		PFOnly, // PF
		}
		else {
			bLF = Boolean.parseBoolean(param.get("bLF"));
			bPF = Boolean.parseBoolean(param.get("bPF"));
			indexChoice = IndexChoice.valueOf(param.get("index_impl"));
		}
		if ( indexChoice == IndexChoice.Position ) {
			if ( isZero ) return new ZeroPositionPrefixSearch(theta, bLF, bPF, indexChoice);
			if ( isExact ) return new ExactPositionPrefixSearch(theta, bLF, bPF, indexChoice);
			else return new PositionPrefixSearch(theta, bLF, bPF, indexChoice);
		}
		else {
			if ( isZero ) return new ZeroPrefixSearch(theta, bLF, bPF, indexChoice);
			if ( isExact ) return new ExactPrefixSearch(theta, bLF, bPF, indexChoice);
			else return new PrefixSearch(theta, bLF, bPF, indexChoice);
		}
	}
	
	private static AbstractSearch createZeroPrefixSearch( DictParam param ) {
		return createPrefixSearch(param, false, true);
	}

	private static PkwiseNaiveSearch createPkwiseNaiveSearch( DictParam param ) {
		double theta = Double.parseDouble(param.get("theta"));
		int qlen = Integer.parseInt(param.get("qlen"));
		int kmax = Integer.parseInt(param.get("kmax"));
		return new PkwiseNaiveSearch(theta, qlen, kmax);
	}
	
	private static PkwiseSearch createPkwiseSearch( DictParam param ) {
		double theta = Double.parseDouble(param.get("theta"));
		int qlen = Integer.parseInt(param.get("qlen"));
		int kmax = Integer.parseInt(param.get("kmax"));
		return new PkwiseSearch(theta, qlen, kmax);
	}
	
	private static PkwiseSynSearch createPkwiseSynSearch( DictParam param, CommandLine cmd ) {
		double theta = Double.parseDouble(param.get("theta"));
		int qlen = Integer.parseInt(cmd.getOptionValue("ql"));
		int kmax = Integer.parseInt(param.get("kmax"));
		return new PkwiseSynSearch(theta, qlen, kmax);
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
