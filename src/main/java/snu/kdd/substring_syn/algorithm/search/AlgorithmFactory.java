package snu.kdd.substring_syn.algorithm.search;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.faerie.FaerieSynNaiveSearch;
import snu.kdd.faerie.FaerieSynSearch;
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
		FaerieSynNaiveSearch,
		FaerieSynSearch,

		ZeroPrefixSearch,
	}
	
	public enum FilterOptionLabel {
		Fopt_None,
		Fopt_Index,
		Fopt_C, // count
		Fopt_P, // position
		Fopt_L, // length
		Fopt_R, // prefix
		Fopt_IL, // index+length
		Fopt_IR, // index+prefix
		Fopt_CP,
		Fopt_CL,
		Fopt_PL,
		Fopt_CPL,
		Fopt_CPLR,
	}

	public static class FilterOption {
		public final boolean bLF;
		public final boolean bPF;
		public final IndexChoice indexChoice;
		
		public FilterOption( FilterOptionLabel label ) {
			boolean bLF = false, bPF = false;
			IndexChoice indexChoice = IndexChoice.None;
			switch (label) {
			case Fopt_None: break;
			case Fopt_Index: indexChoice = IndexChoice.Naive; break;
			case Fopt_C: indexChoice = IndexChoice.Count; break;
			case Fopt_P: indexChoice = IndexChoice.Position; break;
			case Fopt_L: bLF = true; break;
			case Fopt_R: bPF = true; break;
			case Fopt_IL: bLF = true; indexChoice = IndexChoice.Naive; break;
			case Fopt_IR: bPF = true; indexChoice = IndexChoice.Naive; break;
			case Fopt_CP: indexChoice = IndexChoice.CountPosition; break;
			case Fopt_CL: bLF = true; indexChoice = IndexChoice.Count; break;
			case Fopt_PL: bLF = true; indexChoice = IndexChoice.Position; break;
			case Fopt_CPL: bLF = true; indexChoice = IndexChoice.CountPosition; break;
			case Fopt_CPLR: bLF = true; bPF = true; indexChoice = IndexChoice.CountPosition; break;
			default: throw new RuntimeException("Unexpected error");
			}
			this.bLF = bLF;
			this.bPF = bPF;
			this.indexChoice = indexChoice;
		}
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
		case FaerieSynNaiveSearch: return createFaerieSynNaiveSearch(param, cmd);
		case FaerieSynSearch: return createFaerieSynSearch(param, cmd);
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
			FilterOptionLabel label = FilterOptionLabel.valueOf(param.get("filter"));
			FilterOption fopt = new FilterOption(label);
			bLF = fopt.bLF;
			bPF = fopt.bPF;
			indexChoice = fopt.indexChoice;
		}
		else {
			bLF = Boolean.parseBoolean(param.get("bLF"));
			bPF = Boolean.parseBoolean(param.get("bPF"));
			indexChoice = IndexChoice.valueOf(param.get("index_impl"));
		}
		if ( indexChoice == IndexChoice.CountPosition || indexChoice == IndexChoice.Position ) {
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

	private static FaerieSynNaiveSearch createFaerieSynNaiveSearch( DictParam param, CommandLine cmd ) {
		double theta = Double.parseDouble(param.get("theta"));
		return new FaerieSynNaiveSearch(theta);
	}
	
	private static FaerieSynSearch createFaerieSynSearch( DictParam param, CommandLine cmd ) {
		double theta = Double.parseDouble(param.get("theta"));
		boolean isDiskBased = Boolean.parseBoolean(param.get("isDiskBased"));
		return new FaerieSynSearch(theta, isDiskBased);
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
