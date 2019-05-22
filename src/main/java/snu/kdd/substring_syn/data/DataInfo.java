package snu.kdd.substring_syn.data;

import java.io.File;

public class DataInfo {

	final boolean isSynthetic = false;

	public final String searchedPath;
	public final String indexedPath;
	public final String rulePath;
	public final String searchedFileName;
	public final String indexedFileName;
	public final String ruleFileName;
	public final String datasetName;

	public DataInfo( String searchedPath, String indexedPath, String rulePath ) {
		this.searchedPath = searchedPath;
		this.indexedPath = indexedPath;
		this.rulePath = rulePath;
		this.searchedFileName = searchedPath.substring( searchedPath.lastIndexOf(File.separator) + 1 );
		this.indexedFileName = indexedPath.substring( indexedPath.lastIndexOf(File.separator) + 1 );
		this.ruleFileName = rulePath.substring( rulePath.lastIndexOf(File.separator) + 1 );
		this.datasetName = setName();
	}
	
	private String setName() {

		if( isSelfJoin() ) {
			return searchedFileName + "_SelfJoin" + "_wrt_" + ruleFileName;
		}
		else {
			return searchedFileName + "_JoinWith_" + indexedFileName + "_wrt_" + ruleFileName;
		}
	}
	
	private boolean isSelfJoin() {
		return searchedPath.equals( indexedPath );
	}

	public String toJson() {
		StringBuilder bld = new StringBuilder();
		bld.append( "\"Name\": \"" + datasetName + "\"" );
		bld.append( ", \"Data One Path\": \"" + searchedPath + "\"" );
		bld.append( ", \"Data Two Path\": \"" + indexedPath + "\"" );
		bld.append( ", \"Rule Path\": \"" + rulePath + "\"" );
		return bld.toString();
	}
	
	public String getName() {
		return datasetName;
	}
}
