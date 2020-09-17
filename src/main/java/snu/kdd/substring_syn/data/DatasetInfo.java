package snu.kdd.substring_syn.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.yaml.snakeyaml.Yaml;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class DatasetInfo {
	
	private static final String prefix;
	private static final Object2ObjectOpenHashMap<String, DatasetInfo> map = new Object2ObjectOpenHashMap<>();;
	
	static {
		String osName = System.getProperty( "os.name" );
		if ( osName.startsWith( "Windows" ) ) prefix = "D:\\ghsong\\data\\synonyms\\";
		else if ( osName.startsWith( "Linux" ) ) prefix = "data/";
		else prefix = "";

		Yaml yaml = new Yaml();
		InputStream inputStream;
		try {
			inputStream = new FileInputStream("data_info.yml");
            LinkedHashMap<String, Map<String,?>> yamlMap = yaml.load(inputStream);
            for ( Entry<String, Map<String,?>>entry : yamlMap.entrySet() ) {
                DatasetInfo info = new DatasetInfo(entry.getValue());
                map.put(entry.getKey(), info);
            }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static String getSearchedPath( String name, String qlen ) {
		String path = map.get(name).searchedPath;
		if ( qlen == null ) return path;
		else return String.format(path, qlen);
	}
	
	public static String getIndexedPath( String name ) {
		String path = map.get(name).indexedPath;
		return path;
	}
	
	public static String getRulePath( String name, String nr ) {
		String path = map.get(name).rulePath;
		if ( nr == null ) return path;
		else return String.format(path, nr);
	}
	
	public static String getMaxSize(String name) {
		return map.get(name).maxSize;
	}
	
	private final String searchedPath;
	private final String indexedPath;
	private final String rulePath;
	private final String maxSize;
	
	public DatasetInfo( Map<String, ?> map ) {
		searchedPath = String.join(File.separator, prefix, (String)map.get("searchedPath"));
		indexedPath = String.join(File.separator, prefix, (String)map.get("indexedPath"));
		rulePath = String.join(File.separator, prefix, (String)map.get("rulePath"));
		@SuppressWarnings("unchecked")
		ArrayList<String> sizeList = (ArrayList<String>)map.get("nt");
		maxSize = sizeList.get(sizeList.size()-1);
	}
	
	@Override
	public String toString() {
		StringBuilder strbld = new StringBuilder("DatasetInfo {\n");
		strbld.append("\tsearchedPath: "+searchedPath+"\n");
		strbld.append("\tindexedPath: "+indexedPath+"\n");
		strbld.append("\trulePath: "+rulePath+"\n");
		strbld.append("}");
		return strbld.toString();
	}
}
