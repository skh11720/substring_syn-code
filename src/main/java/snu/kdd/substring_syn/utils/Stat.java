package snu.kdd.substring_syn.utils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Stat {
	
	public static final String Alg_ID = "Alg_ID";
	public static final String Alg_Name = "Alg_Name";
	public static final String Alg_Version = "Alg_Version";
	public static final String Alg_Param = "Alg_Param";
//	public static final String Dataset_searchedPath = "Dataset_searchedPath";
//	public static final String Dataset_indexedPath = "Dataset_indexedPath";
//	public static final String Dataset_rulePath = "Dataset_rulePath";
	public static final String Dataset_Name = "Dataset_Name";
	public static final String Dataset_numSearched = "Dataset_numSearched";
	public static final String Dataset_numIndexed= "Dataset_numIndexed";
	public static final String Dataset_numRule= "Dataset_numRule";
	
	public static final String Time_0_Total = "Time_0_Total";
	public static final String Time_1_Validation = "Time_1_Validation";

//	public static final String Mem_,
	public static final String Num_Result = "Num_Result";
	public static final String Num_ResultQuerySide = "Num_ResultQuerySide";
	public static final String Num_ResultTextSide = "Num_ResultTextSide";
	public static final String Num_VerifyQuerySide = "Num_VerifyQuerySide";
	public static final String Num_VerifyTextSide = "Num_VerifyTextSide";

	public static final String Num_VerifiedWindowSizeQuerySide = "Num_VerifiedWindowSizeQuerySide";
	public static final String Num_VerifiedWindowSizeTextSide = "Num_VerifiedWindowSizeTextSide";
	public static final String Num_WindowSizeAllQuerySide = "Num_WindowSizeAllQuerySide";
	public static final String Num_WindowSizeAllTextSide = "Num_WindowSizeAllTextSide";
	
	public static List<String> getList() {
		Field[] fieldList = Stat.class.getDeclaredFields();
		return Arrays.stream(fieldList).map(f->f.getName()).collect(Collectors.toList());
	}
}
