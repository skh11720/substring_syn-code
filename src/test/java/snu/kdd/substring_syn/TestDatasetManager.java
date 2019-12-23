package snu.kdd.substring_syn;

import java.io.IOException;
import java.util.Iterator;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.pkwise.TransWindowDataset;
import snu.kdd.pkwise.WindowDataset;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.utils.Util;

public class TestDatasetManager {

	private static TestDatasetManager manager = new TestDatasetManager();
	private Object2ObjectMap<DatasetKey, Dataset> map = new Object2ObjectOpenHashMap<>();
	
	public static final Iterable<Dataset> getAllDatasets() {
		String nr = "10000";
		String size = "10000";
		String[] datasetNameList = {"WIKI", "PUBMED", "AMAZON"};
		String[] qlenList = {"1", "3", "5"};
		return new Iterable<Dataset>() {

			@Override
			public Iterator<Dataset> iterator() {
				return new Iterator<Dataset>() {
					
					int didx = 0;
					int qidx = 0;
					
					@Override
					public Dataset next() {
						DatasetKey key = new DatasetKey(0, datasetNameList[didx], size, nr, qlenList[qidx], "0");
						didx += 1;
						if ( didx >= datasetNameList.length ) {
							didx = 0;
							qidx += 1;
						}
						return manager.getDataset(key);
					}
					
					@Override
					public boolean hasNext() {
						return qidx < qlenList.length;
					}
				};
			}
		};
	}
	
	public static final Dataset getDataset( String datasetName, String size, String nr, String qlen ) {
		DatasetKey key = new DatasetKey(0, datasetName, size, nr, qlen, "0");
		return manager.getDataset(key);
	}

	public static final WindowDataset getWindowDataset( String datasetName, String size, String nr, String qlen ) {
		DatasetKey key = new DatasetKey(1, datasetName, size, nr, qlen, "0");
		return (WindowDataset) manager.getDataset(key);
	}

	public static final TransWindowDataset getTransWindowDataset( String datasetName, String size, String nr, String qlen, String theta ) {
		DatasetKey key = new DatasetKey(2, datasetName, size, nr, qlen, theta);
		return (TransWindowDataset) manager.getDataset(key);
	}
	
	private final Dataset getDataset( DatasetKey key ) {
		createDatasetIfNotExists(key);
		return map.get(key);
	}
	
	private final void createDatasetIfNotExists( DatasetKey key ) {
		if ( !map.containsKey(key) ) {
			Dataset dataset = null;
			try {
				switch (key.type) {
				case 0: dataset = createPlainDataset(key); break;
				case 1: dataset = createWindowDataset(key); break;
				case 2: dataset = createTransWindowDataset(key); break;
				}
			} catch ( IOException e ) {
				e.printStackTrace();
				System.exit(1);
			}
			map.put(key, dataset);
		}
	}
	
	private final Dataset createPlainDataset( DatasetKey key ) throws IOException {
		return Dataset.createInstanceByName(key.getName(), key.getSize(), key.getNr(), key.getQlen());
	}
	
	private final WindowDataset createWindowDataset( DatasetKey key ) throws IOException {
		return Dataset.createWindowInstanceByName(key.getName(), key.getSize(), key.getNr(), key.getQlen());
	}
	
	private final TransWindowDataset createTransWindowDataset( DatasetKey key ) throws IOException {
		return Dataset.createTransWindowInstanceByName(key.getName(), key.getSize(), key.getNr(), key.getQlen(), key.getTheta());
	}
	
	
	
	static final class DatasetKey {
		
		final int type;
		final String[] fields;
		
		public DatasetKey( int type, String datasetName, String size, String nr, String qlen, String theta ) {
			this.type = type;
			fields = new String[5];
			fields[0] = datasetName;
			fields[1] = size;
			fields[2] = nr;
			fields[3] = qlen;
			fields[4] = theta;
		}
		
		public final String getName() { return fields[0]; }
		public final String getSize() { return fields[1]; }
		public final String getNr() { return fields[2]; }
		public final String getQlen() { return fields[3]; }
		public final String getTheta() { return fields[4]; }
		
		@Override
		public final int hashCode() {
			int hash = 0;
			for ( int i=0; i<fields.length; ++i ) hash = (hash << 5) + fields[i].hashCode() % Util.bigprime;
			return type*100 + hash%100;
		}
		
		@Override
		public final boolean equals(Object obj) {
			if ( obj == null ) return false;
			DatasetKey o = (DatasetKey) obj;
			for ( int i=0; i<fields.length; ++i ) {
				if ( !fields[i].equals(o.fields[i]) ) return false;
			}
			return true;
		}
	}
}
