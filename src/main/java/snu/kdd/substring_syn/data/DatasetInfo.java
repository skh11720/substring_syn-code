package snu.kdd.substring_syn.data;

import java.io.File;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class DatasetInfo {
	
	private static final String prefix;
	private static final Object2ObjectOpenHashMap<String, DataPathInfo> map = new Object2ObjectOpenHashMap<>();;
	
	static {
		String osName = System.getProperty( "os.name" );
		if ( osName.startsWith( "Windows" ) ) prefix = "D:\\ghsong\\data\\synonyms\\";
		else if ( osName.startsWith( "Linux" ) ) prefix = "data_store/";
		else prefix = "";
		
		map.put("SPROT", new DataPathInfo( 
				String.join(File.separator, "sprot", "splitted", "SPROT_two_%s.txt"), 
				String.join(File.separator, "sprot", "splitted", "SPROT_two_%s.txt"), 
				String.join(File.separator, "sprot", "rule.txt")
				));
		map.put("SPROT_long", new DataPathInfo( 
				String.join(File.separator, "sprot_long", "splitted", "SPROT_short_%s.txt"), 
				String.join(File.separator, "sprot_long", "splitted", "SPROT_long_%s.txt"), 
				String.join(File.separator, "sprot_long", "rule.txt")
				));
		map.put("AOL", new DataPathInfo( 
				String.join(File.separator, "aol", "splitted", "aol_%s_data.txt"), 
				String.join(File.separator, "aol", "splitted", "aol_%s_data.txt"), 
				String.join(File.separator, "wordnet", "rules.noun")
				));
		map.put("SYN_test_01", new DataPathInfo( 
				String.join(File.separator, "SYN_test_01", "SYN_test_01_short_%s.txt"), 
				String.join(File.separator, "SYN_test_01", "SYN_test_01_long_%s.txt"), 
				String.join(File.separator, "SYN_test_01", "SYN_test_01_rules.txt")
				));
	}
	
	public static String getSearchedPath( String name, String size ) {
		return String.format(map.get(name).searchedPath, size);
	}
	
	public static String getIndexedPath( String name, String size ) {
		return String.format(map.get(name).indexedPath, size);
	}
	
	public static String getRulePath( String name ) {
		return map.get(name).rulePath;
	}

	private static class DataPathInfo {
		final String searchedPath;
		final String indexedPath;
		final String rulePath;
		
		public DataPathInfo( String searchedPath, String indexedPath, String rulePath ) {
			this.searchedPath = prefix + searchedPath;
			this.indexedPath = prefix + indexedPath;
			this.rulePath = prefix + rulePath;
		}
	}
}
