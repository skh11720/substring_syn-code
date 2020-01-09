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
	public static final String Dataset_nt= "Dataset_nt";
	public static final String Dataset_nr= "Dataset_nr";
	public static final String Dataset_qlen = "Dataset_qlen";
	public static final String Dataset_lr = "Dataset_lr";
	public static final String Dataset_numSearched = "Dataset_numSearched";
	public static final String Dataset_numIndexed= "Dataset_numIndexed";
	public static final String Dataset_numRule= "Dataset_numRule";
	
	public static final String Time_Total = "Time_Total";
	public static final String Time_QS_Total = "Time_QS_Total";
	public static final String Time_TS_Total = "Time_TS_Total";
	public static final String Time_Prepare_Data = "Time_Prepare_Data";
	public static final String Time_Preprocess= "Time_Preprocess";
	public static final String Time_QS_Validation = "Time_QS_Validation";
	public static final String Time_TS_Validation = "Time_TS_Validation";
	public static final String Time_BuildIndex = "Time_BuildIndex";
	public static final String Time_QS_IndexFilter = "Time_QS_IndexFilter";
	public static final String Time_TS_IndexFilter = "Time_TS_IndexFilter";

	/*
	 * Searched: searched substrings
	 * LF: substrings remaining after the length filtering
	 * PF: substrings remaining after the prefix filtering
	 * Verified: verified substrings
	 */

	public static final String Num_Result = "Num_Result";
	public static final String Num_QS_Result = "Num_QS_Result";
	public static final String Num_TS_Result = "Num_TS_Result";
	public static final String Num_QS_Retrieved = "Num_QS_Retrieved";
	public static final String Num_TS_Retrieved = "Num_TS_Retrieved";
	public static final String Num_QS_Verified = "Num_QS_Verified";
	public static final String Num_TS_Verified = "Num_TS_Verified";
	
	public static final String Len_SearchedAll= "Len_SearchedAll";
	public static final String Len_IndexedAll= "Len_IndexedAll";
	public static final String Size_Index_InvList = "Size_Index_InvList";
	public static final String Size_Index_TransInvList = "Size_Index_TransInvList";
	public static final String Len_QS_Retrieved = "Len_QS_Retrieved";
	public static final String Len_TS_Retrieved = "Len_TS_Retrieved";
	public static final String Len_QS_LF = "Len_QS_LF";
	public static final String Len_TS_LF = "Len_TS_LF";
	public static final String Len_QS_PF = "Len_QS_PF";
	public static final String Len_TS_PF = "Len_TS_PF";
	public static final String Len_QS_Verified = "Len_QS_Verified";
	public static final String Len_TS_Verified = "Len_TS_Verified";
	
	public static final String Mem_Before_Index = "Mem_Before_Index";
	public static final String Mem_After_Index = "Mem_After_Index";
	public static final String Space_Index = "Space_Index";
	
	public static List<String> getList() {
		Field[] fieldList = Stat.class.getDeclaredFields();
		return Arrays.stream(fieldList).map(f->f.getName()).collect(Collectors.toList());
	}
	
	public static Set<String> getSet() {
		return new ObjectOpenHashSet<String>( getList() );
	}
}
