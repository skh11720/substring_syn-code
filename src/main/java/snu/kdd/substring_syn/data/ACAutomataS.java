package snu.kdd.substring_syn.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.utils.StringSplitIterator;

/***
 * 
 * An AC automata implementation for rules in the String-level
 *
 */

public class ACAutomataS {
	private class State {
		IntArrayList output;
		Object2ObjectOpenHashMap<String, State> split;

		State func;
		State parent;

		String token;

		State() {
			func = this;
		}

		State( Substring token, State parent ) {
			this.token = token.toString();
			this.parent = parent;
		}
	}

	private final State root;
	ObjectList<String> ruleStrList;

	public ACAutomataS( Iterable<String> ruleStrs ) {
		ObjectSet<String> ruleStrSet = new ObjectOpenHashSet<>();
		for ( String ruleStr : ruleStrs ) {
			String[] rstr = ruleStr.toLowerCase().split("\\|\\|\\|");
			if ( !rstr[0].equals(rstr[1]) ) ruleStrSet.add(ruleStr.toLowerCase());
		}
		ruleStrList = new ObjectArrayList<>(ruleStrSet);

		root = new State();

		int ridx = 0;
		for( final String ruleStr : ruleStrList ) {
			State curr = root;
			StringSplitIterator wordIter = new StringSplitIterator(Ruleset.getLhs(ruleStr));
			while ( wordIter.hasNext() ) {
				Substring token = wordIter.next();
				State next;
				if( curr.split != null && ( next = curr.split.get( token ) ) != null ) {
					curr = next;
				}
				else {
					next = new State( token, curr );
					if( curr.split == null ) {
						curr.split = new Object2ObjectOpenHashMap<>();
					}
					curr.split.put( token.toString(), next );
					curr = next;
				}
			}
			if( curr.output == null ) {
				curr.output = new IntArrayList( 3 );
			}
			curr.output.add( ridx );
			ridx += 1;
		}

		ArrayList<State> currdepth = new ArrayList<>();
		ArrayList<State> nextdepth = new ArrayList<>();

		for( final Entry<String, State> depth_1_entries : root.split.entrySet() ) {
			final State state = depth_1_entries.getValue();
			state.func = root;
			if( state.split != null ) {
				for( final Entry<String, State> depth_2_entries : state.split.entrySet() ) {
					currdepth.add( depth_2_entries.getValue() );
				}
			}
		}

		while( !currdepth.isEmpty() ) {
			for( final State curr : currdepth ) {
				State r = curr.parent.func;
				while( true ) {
					if( r == root || r.split != null && r.split.containsKey( curr.token ) ) {
						break;
					}
					else {
						r = r.func;
					}
				}
				if( r.split.containsKey( curr.token ) ) {
					curr.func = r.split.get( curr.token );
				}
				else {
					curr.func = root;
				}

				if( curr.func.output != null ) {
					if( curr.output == null ) {
						curr.output = new IntArrayList();
					}
					curr.output.addAll( curr.func.output );
				}

				if( curr.split != null ) {
					for( final Entry<String, State> child : curr.split.entrySet() ) {
						nextdepth.add( child.getValue() );
					}
				}
			}

			final ArrayList<State> tmp = currdepth;
			currdepth = nextdepth;
			nextdepth = tmp;
			nextdepth.clear();
		}
	}
	
	public int getNumApplicableRules( Iterator<Substring> tokenIter ) {
		int nar = 0;
		State curr = root;
		Substring token = tokenIter.next();
		while (true) {
			State next;
			if ( curr.split != null && ( next = curr.split.get(token) ) != null ) {
				curr = next;
				if ( tokenIter.hasNext() ) token = tokenIter.next();
				else break;

				if ( next.output != null ) {
					nar += next.output.size();
				}
			}
			else if ( curr == root ) {
				if ( tokenIter.hasNext() ) token = tokenIter.next();
				else break;
			}
			else {
				curr = curr.func;
			}
		}
		return nar;
	}

	public void printApplicableRules( String[] rec ) {
		State curr = root;
		for ( int i=0; i< rec.length; ) {
			State next;
			if ( curr.split != null && ( next = curr.split.get( rec[i] ) ) != null ) {
				curr = next;
				++i;

				if ( next.output != null ) {
					for ( int ridx : next.output ) System.err.println(ruleStrList.get(ridx));
				}
			}
			else if ( curr == root ) {
				++i;
			}
			else {
				curr = curr.func;
			}
		}
	}
}
