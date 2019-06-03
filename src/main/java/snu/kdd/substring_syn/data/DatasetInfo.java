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
				String.join(File.separator, "sprot", "splitted", "SPROT_two_%d.txt"), 
				String.join(File.separator, "sprot", "splitted", "SPROT_two_%d.txt"), 
				String.join(File.separator, "sprot", "rule.txt")
				));
		map.put("SPROT_long", new DataPathInfo( 
				String.join(File.separator, "sprot_long", "splitted", "SPROT_short_%d.txt"), 
				String.join(File.separator, "sprot_long", "splitted", "SPROT_long_%d.txt"), 
				String.join(File.separator, "sprot_long", "rule.txt")
				));
	}
	
	public static DataPathInfo getDataPaths( String name ) {
		return map.get(name);
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
