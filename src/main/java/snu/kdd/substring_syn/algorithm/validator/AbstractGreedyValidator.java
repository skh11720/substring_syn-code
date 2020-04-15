package snu.kdd.substring_syn.algorithm.validator;

import java.util.Arrays;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public abstract class AbstractGreedyValidator extends AbstractValidator {
	
	protected final StatContainer statContainer;
	public static int optScore = 0;

	public AbstractGreedyValidator(double theta, StatContainer statContainer) {
		super(theta, statContainer);
		this.statContainer = statContainer;
	}

	protected class State {
		ObjectSet<PosRule> candRuleSet;
		Boolean[] bAvailable;
		Int2IntOpenHashMap counter;
		ObjectSet<PosRule> appliedRuleSet;
		
		public State( RecordInterface x, RecordInterface y ) {
			counter = Util.getCounter(y.getTokenArray());
			for ( int token : x.getTokenArray() ) {
				if ( counter.get(token) > 0 )  counter.addTo(token, -1);
			}
			candRuleSet = createPosRuleList(x);
			bAvailable = new Boolean[x.size()];
			Arrays.fill( bAvailable, true );
			appliedRuleSet = new ObjectOpenHashSet<PosRule>();
		}

		private ObjectSet<PosRule> createPosRuleList( RecordInterface rec ) {
			ObjectSet<PosRule> posRuleSet = new ObjectOpenHashSet<PosRule>();
			for (int k=0; k<rec.size(); ++k) {
				for (Rule rule : rec.getApplicableRules(k)) {
//					if ( rule.isSelfRule ) continue;
					for ( int token : rule.getRhs() ) {
						if ( counter.containsKey(token) ) {
							posRuleSet.add( new PosRule(rule, k) );
							break;
						}
					}
				}
			}
			return posRuleSet;
		}
		
		public void findBestTransform() {
			while ( candRuleSet.size() > 0 ) {
				PosRule bestRule = findBestRule();
				if (bestRule == null) break;
				applyBestRule(bestRule);
				removeInvalidRules();
			}
		}
		
		private PosRule findBestRule() {
			double bestScore = 0;
			PosRule bestRule = null;
			for ( PosRule rule : candRuleSet ) {
				final double score;
				switch (optScore) {
				case 0: score = score0(rule.rule); break;
				case 1: score = score1(rule.rule); break;
				case 2: score = score2(rule.rule); break;
				case 3: score = score3(rule.rule); break;
				case 4: score = score4(rule.rule); break;
				case 5: score = score5(rule.rule); break;
				case 6: score = score6(rule.rule); break;
				case 7: score = score7(rule.rule); break;
				case 8: score = score8(rule.rule); break;
				default: throw new RuntimeException("Invalid optScore value: "+optScore);
				}
				if (score > bestScore) {
					bestScore = score;
					bestRule = rule;
				}
//				if (bestScore == 1) break;
			}
			if ( bestScore <= 0) return null;
			else return bestRule;
		}

		private double score0( Rule rule ) {
			double score = 0;
			for (int token : rule.getRhs()) {
				if ( counter.get(token) > 0 ) ++score;
			}
			score /= rule.rhsSize();
			return score;
		}

		private double score1( Rule rule ) {
			double score = 0;
			for (int token : rule.getRhs()) {
				if ( counter.get(token) > 0 ) ++score;
			}
			return score;
		}

		private double score2( Rule rule ) {
			double score = 0;
			for (int token : rule.getRhs()) {
				if ( counter.get(token) > 0 ) ++score;
			}
			score /= rule.lhsSize();
			return score;
		}

		private double score3( Rule rule ) {
			double score = 0;
			for (int token : rule.getRhs()) {
				if ( counter.get(token) > 0 ) ++score;
			}
			for (int token : rule.getLhs()) {
				if ( counter.get(token) > 0 ) --score;
			}
			score /= rule.rhsSize();
			return score;
		}

		private double score4( Rule rule ) {
			double score = 0;
			for (int token : rule.getRhs()) {
				if ( counter.get(token) > 0 ) ++score;
			}
			for (int token : rule.getLhs()) {
				if ( counter.get(token) > 0 ) --score;
			}
			return score;
		}

		private double score5( Rule rule ) {
			double score = 0;
			for (int token : rule.getRhs()) {
				if ( counter.get(token) > 0 ) ++score;
			}
			for (int token : rule.getLhs()) {
				if ( counter.get(token) > 0 ) --score;
			}
			score /= rule.lhsSize();
			return score;
		}

		private double score6( Rule rule ) {
			double score = 0;
			for (int token : rule.getRhs()) {
				if ( counter.get(token) > 0 ) ++score;
			}
			score /= rule.rhsSize();
			for (int token : rule.getLhs()) {
				if ( counter.get(token) > 0 ) --score;
			}
			return score;
		}

		private double score7( Rule rule ) {
			double score = 0;
			for (int token : rule.getRhs()) {
				if ( counter.get(token) > 0 ) ++score;
			}
			score /= rule.rhsSize();
			double penalty = 0;
			for (int token : rule.getLhs()) {
				if ( counter.get(token) > 0 ) ++penalty;
			}
			score -= penalty/rule.lhsSize();
			return score;
		}

		private double score8( Rule rule ) {
			double score = 0;
			for (int token : rule.getRhs()) {
				if ( counter.get(token) > 0 ) ++score;
			}
			double penalty = 0;
			for (int token : rule.getLhs()) {
				if ( counter.get(token) > 0 ) ++penalty;
			}
			score -= penalty/rule.lhsSize();
			return score;
		}
		
		private void applyBestRule( PosRule bestRule ) {
			for (int j=0; j<bestRule.lhsSize(); ++j) bAvailable[bestRule.pos+j] = false;
			candRuleSet.remove( bestRule );
			appliedRuleSet.add( bestRule );
			for (Integer token : bestRule.getRhs()) counter.addTo(token, -1);
		}
		
		private void removeInvalidRules() {
			ObjectSet<PosRule> invalidRuleSet = new ObjectOpenHashSet<>();
			for ( PosRule rule : candRuleSet ) {
				Boolean isValid = true;
				for (int j=0; j<rule.lhsSize(); j++) isValid &= bAvailable[rule.pos+j];
				if (!isValid) invalidRuleSet.add(rule);
			}
			candRuleSet.removeAll(invalidRuleSet);
		}

		public int[] getTransformedString( RecordInterface x ) {
			int transformedSize = (int)(Arrays.stream(bAvailable).filter(b -> b).count());
			for ( PosRule rule : appliedRuleSet ) transformedSize += rule.rhsSize();
			int[] transformedString = new int[transformedSize];
			Iterator<PosRule> ruleIter = appliedRuleSet.stream().sorted().iterator();
			
			for (int i=0, j=0; i<bAvailable.length; ) {
				if ( bAvailable[i] ) transformedString[j++] = x.getToken(i++);
				else {
					PosRule rule = ruleIter.next();
					for ( int token : rule.getRhs() ) transformedString[j++] = token;
					i += rule.lhsSize();
				}
			}
			return transformedString;
		}
	}

	protected class PosRule implements Comparable<PosRule> {
		public Rule rule;
		public int pos;
		
		public PosRule( Rule rule, int pos ) {
			this.rule = rule;
			this.pos = pos;
		}
		
		public int lhsSize() {
			return rule.lhsSize();
		}
		
		public int rhsSize() {
			return rule.rhsSize();
		}
		
		public int[] getRhs() {
			return rule.getRhs();
		}
		
		@Override
		public int hashCode() {
			return rule.hashCode();
		}

		@Override
		public int compareTo(PosRule o) {
			return Integer.compare(this.pos, o.pos);
		}
	}
}
