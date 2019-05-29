package snu.kdd.substring_syn.algorithm.search;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;

public abstract class AbstractSearch {

	protected final double theta;
//	protected final Set<IntPair> rslt;
	protected final Set<IntPair> rsltFromQuery;
	protected final Set<IntPair> rsltFromText;
	protected final Logger log; 
	
	public AbstractSearch( double theta ) {
		this.theta = theta;
//		this.rslt = new ObjectOpenHashSet<>();
		this.rsltFromQuery = new ObjectOpenHashSet<>();
		this.rsltFromText = new ObjectOpenHashSet<>();

		log = LogManager.getFormatterLogger();
	}
	
	public void run( Dataset dataset ) {
		for ( Record qrec : dataset.searchedList ) {
			long ts = System.nanoTime();;
			search(qrec, dataset.indexedList);
			log.debug("search(qrec=%d, ...)\t%.3f ms", ()->qrec.getID(), ()->(System.nanoTime()-ts)/1e6);
		}
		outputResult();
	}
	
	public void search( Record qrec, Iterable<Record> records ) {
		for ( Record rec :  records ) {
			searchRecordFromText(qrec, rec);
		}
	}
	
	public List<IntPair> getResultFromText() {
		return rsltFromText.stream().sorted().collect(Collectors.toList());
	}

	public void outputResult() {
		try {
			PrintStream ps = new PrintStream(String.format("output/"+getOutputName()+".txt", theta));
			getResultFromText().forEach(ip -> ps.println("FROM_TEXT\t"+ip));
			ps.close();
		}
		catch ( FileNotFoundException e ) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	protected abstract void searchRecordFromQuery( Record qrec, Record rec );
	
	protected abstract void searchRecordFromText( Record qrec, Record rec ); 
	
	public abstract String getName();

	public abstract String getVersion();
	
	public String getOutputName() {
		return getName() +"_" + getVersion() + String.format("_%.2f", theta);
	}
}
