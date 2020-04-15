package snu.kdd.substring_syn.utils;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.data.QGram;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;

public class QGramGenerator {

	final Record rec;
	final int q;
	ObjectSet<QGram> qgramSet;
	State state;
	
	public QGramGenerator( Record rec, int q ) {
		this.rec = rec;
		this.q = q;
	}
	
	public ObjectSet<QGram> gen() {
		qgramSet = new ObjectOpenHashSet<>();
		state = new State();
		for ( int i=0; i<rec.size(); ++i ) {
			gen(i);
		}
		return qgramSet;
	}
	
	private void gen( int i ) {
		for ( Rule r : rec.getApplicableRules(i) ) {
			state.addRule(r);
			if ( i == rec.size()-1 || state.isFull() ) {
				state.genQGram();
			}
			else if ( i+r.lhsSize() < rec.size() ) gen(i+r.lhsSize());
			else if ( i+r.lhsSize() == rec.size() ) state.genQGram();
			state.removeRule();
		}
	}
	
	
	
	private class State {
		Rule[] ruleList;
		int nRule;
		int rhsSize;
		
		public State() {
			ruleList = new Rule[q];
			nRule = 0;
			rhsSize = 0;
		}
		
		public void addRule( Rule r ) {
			ruleList[nRule] = r;
			nRule += 1;
			rhsSize += r.rhsSize();
		}
		
		public boolean isFull() {
			return (nRule > 0 && ruleList[0].rhsSize()+q-1 <= rhsSize);
		}
		
		public void genQGram() {
			IntArrayList tokens = new IntArrayList();
			for ( int j=0; j<nRule; ++j ) {
				for ( int token : ruleList[j].getRhs() ) {
					tokens.add(token);
				}
			}
			for ( int i=0; i<Math.min(ruleList[0].rhsSize(), tokens.size()-q+1); ++i ) qgramSet.add(new QGram(tokens.subList(i, i+q).toIntArray()));
		}
		
		public void removeRule() {
			nRule -= 1;
			rhsSize -= ruleList[nRule].rhsSize();
		}
	}
}
