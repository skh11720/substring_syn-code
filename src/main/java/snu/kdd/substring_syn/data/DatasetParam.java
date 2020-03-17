package snu.kdd.substring_syn.data;

import snu.kdd.substring_syn.utils.InputArgument;
import snu.kdd.substring_syn.utils.Log;

public class DatasetParam {
	public final String name;
	public final String size;
	public final String nr;
	public final String qlen;
	public final String lenRatio;
	public final String nar;

	public DatasetParam(InputArgument arg) {
		name = arg.getOptionValue("data");
		size = arg.getOptionValue("nt");
		nr = arg.getOptionValue("nr");
		qlen = arg.getOptionValue("ql");
		lenRatio = arg.getOptionValue("lr");
		nar = arg.getOptionValue("nar");
	}
	
	public DatasetParam( String name, String size, String nr, String qlen, String lenRatio, String nar) {
		this.name = checkAndAssign(name, "name");
		this.size = checkAndAssign(size, "size");
		this.nr = checkAndAssign(nr, "nr");
		this.qlen = checkAndAssign(qlen, "qlen");
		this.lenRatio = checkAndAssign(lenRatio, "lenRatio");
		this.nar = checkAndAssign(nar, "nar");
		Log.log.info("DatasetParam: "+getDatasetName());
	}

	public DatasetParam( String name, String size, String nr, String qlen, String lenRatio) {
		this(name, size, nr, qlen, lenRatio, "-1");
	}
	
	public final String getDatasetName() {
		StringBuilder strbld = new StringBuilder(name);
		if ( size != null ) strbld.append("_n"+size);
		if ( nr != null ) strbld.append("_r"+nr);
		if ( qlen != null ) strbld.append("_q"+qlen);
		if ( lenRatio != null ) strbld.append("_l"+lenRatio);
		if ( nar != null && Integer.parseInt(nar) >= 0 ) strbld.append("_a"+nar);
		return strbld.toString();
	}
	
	private final String checkAndAssign(String val, String name) {
		checkValue(val, name);
		return val;
	}
	
	private final void checkValue(String val, String name) {
		if ( val == null ) {
			NullPointerException e = new NullPointerException(name+" cannot be null");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	@Override
	public String toString() {
		return String.format("DatasetParam(%s, %s, %s, %s, %s, %s)", name ,size, nr, qlen ,lenRatio, nar);
	}
}
