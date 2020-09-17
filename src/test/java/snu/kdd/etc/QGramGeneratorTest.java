package snu.kdd.etc;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.QGram;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.QGramGenerator;

public class QGramGeneratorTest {

	@Test
	public void visualize() throws IOException {
		String name = "WIKI";
		String size = "10000";
		String nr = "107836";
		String ql = "5";
		int q = 1;
		DatasetParam param = new DatasetParam(name, size, nr, ql, null);
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		
		for ( int i=0; i<10; ++i ) {
			TransformableRecordInterface rec = dataset.getRecord(i);
			rec.preprocessApplicableRules();
			rec.preprocessSuffixApplicableRules();
			System.out.println(rec.toStringDetails());
			
			QGramGenerator generator = new QGramGenerator(rec, q);
			ObjectSet<QGram> qgramSet = generator.gen();
			for ( QGram qgram : qgramSet ) {
				System.out.println("\t"+qgram);
			}
		}
	}
	
	@Test
	public void correctnessTest() throws IOException {
		String name = "WIKI";
		String size = "100000";
		String nr = "1000";
		String ql = "5";
		int q = 3;
		DatasetParam param = new DatasetParam(name, size, nr, ql, null);
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		
		for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
			rec.preprocessApplicableRules();
			rec.preprocessSuffixApplicableRules();
			QGramGenerator generator = new QGramGenerator(rec, q);

			ObjectSet<QGram> qgramSet0 = new ObjectOpenHashSet<>();
			for ( Record exp : Records.expandAll(rec) ) {
				for ( int i=0; i<exp.size()-q+1; ++i ) {
					qgramSet0.add(new QGram(exp.getTokenList().subList(i, i+q).toIntArray()));
				}
			}

			ObjectSet<QGram> qgramSet1 = generator.gen();

			for ( QGram qgram : qgramSet0 ) {
				try {
					assertTrue( qgramSet1.contains(qgram) );
				}
				catch ( AssertionError e ) {
					System.err.println(rec.toStringDetails());
					System.err.println(qgram);
					System.exit(1);
				}
			}

			for ( QGram qgram : qgramSet1 ) {
				try {
					assertTrue( qgramSet0.contains(qgram) );
				}
				catch ( AssertionError e ) {
					System.err.println(rec.toStringDetails());
					System.err.println(qgram);
					System.exit(1);
				}
			}
		}
	}
}
