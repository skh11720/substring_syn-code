package snu.kdd.substring_syn.data;

import org.apache.commons.cli.CommandLine;

import snu.kdd.substring_syn.utils.Log;

public class DatasetParam {
	public final String name;
	public final String size;
	public final String nr;
	public final String qlen;
	public final String lenRatio;

	public DatasetParam(CommandLine cmd) {
		name = Dataset.getOptionValue(cmd, "data");
		size = Dataset.getOptionValue(cmd, "nt");
		nr = Dataset.getOptionValue(cmd, "nr");
		qlen = Dataset.getOptionValue(cmd, "ql");
		lenRatio = Dataset.getOptionValue(cmd, "lr");
	}
	
	public DatasetParam( String name, String size, String nr, String qlen, String lenRatio) {
		checkValue(name, "name");
		checkValue(size, "size");
		checkValue(nr, "nr");
		checkValue(qlen, "qlen");
		checkValue(lenRatio, "lenRatio");
		this.name = name;
		this.size = size;
		this.nr = nr;
		this.qlen = qlen;
		this.lenRatio = lenRatio;
		Log.log.info("DatasetParam: "+getDatasetName());
	}
	
	public final String getDatasetName() {
		StringBuilder strbld = new StringBuilder(name);
		if ( size != null ) strbld.append("_n"+size);
		if ( nr != null ) strbld.append("_r"+nr);
		if ( qlen != null ) strbld.append("_q"+qlen);
		if ( lenRatio != null ) strbld.append("_l"+lenRatio);
		return strbld.toString();
	}
	
	private final void checkValue(String val, String name) {
		if ( val == null ) {
			NullPointerException e = new NullPointerException(name+" cannot be null");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
