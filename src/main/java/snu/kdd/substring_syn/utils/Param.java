package snu.kdd.substring_syn.utils;

import java.util.Map.Entry;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Param {

	private ObjectArrayList<String> keyList = new ObjectArrayList<>();
	private Object2ObjectMap<String, String> map = new Object2ObjectArrayMap<>();
	
	public void put( String key, String value ) {
		keyList.add(key);
		map.put(key, value);
	}
	
	public Iterable<Entry<String, String>> getEntries() {
		return map.entrySet();
	}
	
	public String toSummaryString() {
		StringBuilder strbld = new StringBuilder("{");
		for ( String key : keyList ) {
			String value = map.get(key);
			strbld.append(key+":"+value+", ");
		}
		strbld.append("}");
		return strbld.toString();
	}
	
	public String toDisplayString() {
		StringBuilder strbld = new StringBuilder("{\n");
		for ( String key : keyList ) {
			String value = map.get(key);
			strbld.append("\t\t"+key+":"+value+"\n");
		}
		strbld.append("\t}");
		return strbld.toString();
	}
	
	@Override
	public String toString() {
		return toSummaryString();
	}
}
