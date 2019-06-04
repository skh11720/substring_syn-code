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
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;

public abstract class AbstractSearch {

	protected final double theta;
//	protected final Set<IntPair> rslt;
	protected final Set<IntPair> rsltFromQuery;
	protected final Set<IntPair> rsltFromText;
	protected final Logger log; 
	protected StatContainer statContainer;
	
	public AbstractSearch( double theta ) {
		this.theta = theta;
//		this.rslt = new ObjectOpenHashSet<>();
		this.rsltFromQuery = new ObjectOpenHashSet<>();
		this.rsltFromText = new ObjectOpenHashSet<>();
		this.log = LogManager.getFormatterLogger();
	}
	
	public void run( Dataset dataset ) {
		statContainer = new StatContainer(this, dataset);
		statContainer.startWatch(Stat.Time_0_Total);
		for ( Record query : dataset.searchedList ) {
			long ts = System.nanoTime();;
			search(query, dataset.indexedList);
			log.debug("search(query=%d, ...)\t%.3f ms", ()->query.getID(), ()->(System.nanoTime()-ts)/1e6);
		}
		statContainer.stopWatch(Stat.Time_0_Total);
		statContainer.finalize();
		statContainer.print();
		outputResult(dataset);
	}
	
	public void search( Record query, Iterable<Record> records ) {
		for ( Record rec :  records ) {
			searchQuerySide(query, rec);
			searchTextSide(query, rec);
		}
	}

	public void outputResult( Dataset dataset ) {
		try {
			PrintStream ps = new PrintStream(String.format(getOutputPath(dataset), theta));
			getSortedResult(rsltFromQuery).forEach(ip -> ps.println("FROM_QUERY\t"+ip));
			getSortedResult(rsltFromText).forEach(ip -> ps.println("FROM_TEXT\t"+ip));
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
	
	public abstract String getName();

	public abstract String getVersion();
	
	public String getOutputName( Dataset dataset ) {
		return String.join( "_", getName(), getVersion(), String.format("%.2f", theta), dataset.name);
	}

	public String getOutputPath( Dataset dataset ) {
		return "output/"+getOutputName(dataset)+".txt";
	}
}
