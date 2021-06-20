package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.faerie.FaerieSynSearch;
import snu.kdd.pkwise.PkwiseNaiveSearch;
import snu.kdd.pkwise.PkwiseSearch;
import snu.kdd.pkwise.PkwiseSynSearch;
import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.utils.InputArgument;
import snu.kdd.substring_syn.utils.Log;

public class AlgorithmFactory {

	public enum AlgorithmName {
		ExactNaiveSearch,
		GreedyNaiveSearch,
		RSSearch,
		SimWRSSearch,
		ExactRSSearch,
		ExactSimWRSSearch,
		PkwiseNaiveSearch,
		PkwiseSearch,
		PkwiseSynSearch,
		FaerieSynNaiveSearch,
		FaerieSynSearch,

		ZeroRSSearch,
		
		ExactNaiveContainmentSearch,
		NaiveContainmentSearch,
		ContainmentRSSearch,
		FaerieSynContainmentSearch,
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
		Fopt_CR,
		Fopt_PR,
		Fopt_LR, 
		Fopt_CPL,
		Fopt_CPR,
		Fopt_CLR,
		Fopt_PLR,
		Fopt_CPLR,
	}
	
	public enum GoalOption {
		None,
		Exact,
		Zero,
		Contain,
		SimW,
		ExactSimW,
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
			case Fopt_CR: bLF = false; bPF = true; indexChoice = IndexChoice.Count; break;
			case Fopt_PR: bLF = false; bPF = true; indexChoice = IndexChoice.Position; break;
			case Fopt_LR: bLF = true; bPF = true; indexChoice = IndexChoice.None; break;
			case Fopt_CPL: bLF = true; indexChoice = IndexChoice.CountPosition; break;
			case Fopt_CPR: bPF = true; indexChoice = IndexChoice.CountPosition; break;
			case Fopt_CLR: bLF = true; bPF = true; indexChoice = IndexChoice.Count; break;
			case Fopt_PLR: bLF = true; bPF = true; indexChoice = IndexChoice.Position; break;
			case Fopt_CPLR: bLF = true; bPF = true; indexChoice = IndexChoice.CountPosition; break;
			default: throw new RuntimeException("Unexpected error");
			}
			this.bLF = bLF;
			this.bPF = bPF;
			this.indexChoice = indexChoice;
		}
	}
	
	public static AbstractSearch createInstance( InputArgument arg ) {
		Log.log.trace("AlgorithmFactory.createInstance()");
		AlgorithmName algName = AlgorithmName.valueOf( arg.getOptionValue("alg") );
		String paramStr = arg.getOptionValue("param");
		DictParam param = new DictParam(paramStr);
		switch ( algName ) {
		case ExactNaiveSearch: return createExactNaiveSearch(param);
		case RSSearch: return createRSSearch(param, GoalOption.None);
		case SimWRSSearch: return createRSSearch(param, GoalOption.SimW);
		case ExactRSSearch: return createRSSearch(param, GoalOption.Exact);
		case ExactSimWRSSearch: return createRSSearch(param, GoalOption.ExactSimW);
		case PkwiseNaiveSearch: return createPkwiseNaiveSearch(param);
		case PkwiseSearch: return createPkwiseSearch(param);
		case PkwiseSynSearch: return createPkwiseSynSearch(param, arg);
		case FaerieSynSearch: return createFaerieSynSearch(param);
		case ZeroRSSearch: return createRSSearch(param, GoalOption.Zero);
		default: throw new RuntimeException("Unexpected error");
		}
	}
	
	private static ExactNaiveSearch createExactNaiveSearch( DictParam param ) {
		double theta = Double.parseDouble(param.get("theta"));
		return new ExactNaiveSearch(theta);
	}
	
	private static AbstractSearch createRSSearch( DictParam param, GoalOption goal ) {
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
			return new PositionRSSearch(theta, bLF, bPF, indexChoice);
		}
		else {
			return new RSSearch(theta, bLF, bPF, indexChoice);
		}
	}
	
	private static PkwiseNaiveSearch createPkwiseNaiveSearch( DictParam param ) {
		double theta = Double.parseDouble(param.get("theta"));
		int qlen = Integer.parseInt(param.get("qlen"));
		return new PkwiseNaiveSearch(theta, qlen);
	}
	
	private static PkwiseSearch createPkwiseSearch( DictParam param ) {
		double theta = Double.parseDouble(param.get("theta"));
		int qlen = Integer.parseInt(param.get("qlen"));
		String kmax = param.get("kmax");
		return new PkwiseSearch(theta, qlen, kmax);
	}
	
	private static PkwiseSynSearch createPkwiseSynSearch( DictParam param, InputArgument arg ) {
		double theta = Double.parseDouble(param.get("theta"));
		int qlen = Integer.parseInt(arg.getOptionValue("ql"));
		String kmax = param.get("kmax");
		return new PkwiseSynSearch(theta, qlen, kmax);
	}

	private static FaerieSynSearch createFaerieSynSearch( DictParam param ) {
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
