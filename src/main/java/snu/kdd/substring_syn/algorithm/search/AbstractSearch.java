package snu.kdd.substring_syn.algorithm.search;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.FileAppender;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.utils.Param;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;

public abstract class AbstractSearch {

	protected final String id;
	protected final Param param;
	protected final double theta;
	protected final Set<IntPair> rsltQuerySide;
	protected final Set<IntPair> rsltTextSide;
	protected final Logger log; 
	protected StatContainer statContainer;
	
	protected enum Phase {
		QuerySide,
		TextSide,
	}
	
	public AbstractSearch( double theta ) {
		this.log = LogManager.getFormatterLogger(this);
		Appender appender = ((org.apache.logging.log4j.core.Logger)log).getAppenders().get("File");
		String logpath = ((FileAppender)appender).getFileName();
		id = FilenameUtils.getBaseName(logpath);

		param = new Param();
		this.theta = theta;
		param.put("theta", Double.toString(theta));

		this.rsltQuerySide = new ObjectOpenHashSet<>();
		this.rsltTextSide = new ObjectOpenHashSet<>();
	}
	
	public void run( Dataset dataset ) {
		statContainer = new StatContainer(this, dataset);
		statContainer.startWatch(Stat.Time_0_Total);
		prepareSearch(dataset);
		searchBody(dataset);
		statContainer.stopWatch(Stat.Time_0_Total);
		putResultIntoStat();
		statContainer.finalizeAndOutput();
		outputResult(dataset);
	}
	
	protected void prepareSearch( Dataset dataset ) {
	}
	
	protected void searchBody( Dataset dataset ) {
		for ( Record query : dataset.searchedList ) {
			long ts = System.nanoTime();
			searchQuery(query, dataset);
			log.debug("search(query=%d, ...)\t%.3f ms", ()->query.getID(), ()->(System.nanoTime()-ts)/1e6);
		}
	}
	
	protected void searchQuery( Record query, Dataset dataset ) {
		search(query, dataset.indexedList);
	}
	
	protected void search( Record query, Iterable<Record> records ) {
		for ( Record rec :  records ) {
			statContainer.startWatch(Stat.Time_1_QSTotal);
			searchQuerySide(query, rec);
			statContainer.stopWatch(Stat.Time_1_QSTotal);

			statContainer.startWatch(Stat.Time_2_TSTotal);
			searchTextSide(query, rec);
			statContainer.stopWatch(Stat.Time_2_TSTotal);
		}
	}
	
	protected void putResultIntoStat() {
		statContainer.addCount(Stat.Num_Result, countResult());
		statContainer.addCount(Stat.Num_QS_Result, rsltQuerySide.size());
		statContainer.addCount(Stat.Num_TS_Result, rsltTextSide.size());
	}
	
	protected int countResult() {
		Set<IntPair> rslt = new ObjectOpenHashSet<>(rsltQuerySide);
		rslt.addAll(rsltTextSide);
		return rslt.size();
	}

	protected void outputResult( Dataset dataset ) {
		try {
			PrintStream ps = new PrintStream(String.format(getOutputPath(dataset), theta));
			getSortedResult(rsltQuerySide).forEach(ip -> ps.println("FROM_QUERY\t"+ip));
			getSortedResult(rsltTextSide).forEach(ip -> ps.println("FROM_TEXT\t"+ip));
			ps.close();
		}
		catch ( FileNotFoundException e ) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	protected List<IntPair> getSortedResult( Set<IntPair> rslt ) {
		return rslt.stream().sorted().collect(Collectors.toList());
	}
	
	protected abstract void searchQuerySide( Record query, Record rec );
	
	protected abstract void searchTextSide( Record query, Record rec ); 

	public String getID() { return id; }
	
	public abstract String getName();

	public abstract String getVersion();
	
	public StatContainer getStatContainer() {
		return statContainer;
	}
	
	public String getOutputName( Dataset dataset ) {
		return String.join( "_", getName(), getVersion(), String.format("%.2f", theta), dataset.name);
	}

	public String getOutputPath( Dataset dataset ) {
		return "output/"+getOutputName(dataset)+".txt";
	}
	
	public Param getParam() {
		return param;
	}
}
