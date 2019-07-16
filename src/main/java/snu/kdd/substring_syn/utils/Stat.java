package snu.kdd.substring_syn.utils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class Stat {
	
	public static final String Alg_ID = "Alg_ID";
	public static final String Alg_Name = "Alg_Name";
	public static final String Alg_Version = "Alg_Version";
	public static final String Param = "Param";
//	public static final String Dataset_searchedPath = "Dataset_searchedPath";
//	public static final String Dataset_indexedPath = "Dataset_indexedPath";
//	public static final String Dataset_rulePath = "Dataset_rulePath";
	public static final String Dataset_Name = "Dataset_Name";
	public static final String Dataset_numSearched = "Dataset_numSearched";
	public static final String Dataset_numIndexed= "Dataset_numIndexed";
	public static final String Dataset_numRule= "Dataset_numRule";
	
	public static final String Time_0_Total = "Time_0_Total";
	public static final String Time_1_QSTotal = "Time_1_QSTotal";
	public static final String Time_2_TSTotal = "Time_2_TSTotal";
	public static final String Time_3_Validation = "Time_3_Validation";
	public static final String Time_4_BuildIndex = "Time_4_BuildIndex";
	public static final String Time_5_IndexFilter = "Time_5_IndexFilter";

//	public static final String Mem_,
	public static final String Num_Result = "Num_Result";
	public static final String Num_QS_Result = "Num_QS_Result";
	public static final String Num_TS_Result = "Num_TS_Result";
	public static final String Num_QS_Verified = "Num_QS_Verified";
	public static final String Num_TS_Verified = "Num_TS_Verified";

	public static final String Num_QS_WindowSizeLF = "Num_QS_WindowSizeLF";
	public static final String Num_TS_WindowSizeLF = "Num_TS_WindowSizeLF";
	public static final String Num_QS_WindowSizeVerified = "Num_QS_WindowSizeVerified";
	public static final String Num_TS_WindowSizeVerified = "Num_TS_WindowSizeVerified";
	public static final String Num_QS_WindowSizeAll = "Num_QS_WindowSizeAll";
	public static final String Num_TS_WindowSizeAll = "Num_TS_WindowSizeAll";
	
	public static final String Num_QS_IndexFiltered = "Num_QS_IndexFiltered";
	public static final String Num_TS_IndexFiltered = "Num_TS_IndexFiltered";
	
	public static List<String> getList() {
		Field[] fieldList = Stat.class.getDeclaredFields();
		return Arrays.stream(fieldList).map(f->f.getName()).collect(Collectors.toList());
	}
	
	public static Set<String> getSet() {
		return new ObjectOpenHashSet<String>( getList() );
	}
}
