package snu.kdd.pkwise;

import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import snu.kdd.substring_syn.data.QGram;
import snu.kdd.substring_syn.data.RecordStore;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.QGramGenerator;

public class TransWindowDataset extends WindowDataset {
	
	protected final RecordStore qgramStore = null;
	protected final Int2IntMap qid2ridMap = null;
	final int qlen;
	final double theta;

	public TransWindowDataset(String datasetName, String size, String nr, String qlen, String theta ) {
		super(datasetName, size, nr, qlen);
		this.qlen = Integer.parseInt(qlen);
		this.theta = Double.parseDouble(theta);
	}
	
	public Iterator<QGram> getIterator() {
		return new QGramIterator(qlen);
	}
	
	class QGramIterator implements Iterator<QGram> {

		final Iterator<Record> riter;
		final int q;
		Iterator<QGram> qiter = null;
		Record rec;
		QGramGenerator qgen;
		
		public QGramIterator( int q ) {
			riter = recordStore.getRecords().iterator();
			this.q = q;
			findNext();
		}

		@Override
		public boolean hasNext() {
			return (riter.hasNext() || qiter.hasNext());
		}

		@Override
		public QGram next() {
			QGram qgram = qiter.next();
			findNext();
			return qgram;
		}
		
		private void findNext() {
			while ( qiter == null || !qiter.hasNext() ) {
				if ( !riter.hasNext() ) return;
				rec = riter.next();
				rec.preprocessApplicableRules();
				qgen = new QGramGenerator(rec, q);
				qiter = qgen.gen().iterator();
			}
		}
	}
}
