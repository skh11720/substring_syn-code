package snu.kdd.substring_syn.data;

import java.util.ArrayList;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

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

		String word;

		State() {
			func = this;
		}

		State( String word, State parent ) {
			this.word = word;
			this.parent = parent;
		}
	}

	private final State root;
	ObjectList<String> ruleStrList;

	// creates an automata with left hand sides of rules
	public ACAutomataS( Iterable<String> ruleStrs ) {
		ObjectSet<String> ruleStrSet = new ObjectOpenHashSet<>();
		for ( String ruleStr : ruleStrs ) {
			String[] rstr = ruleStr.toLowerCase().split("\\|\\|\\|");
			if ( !rstr[0].equals(rstr[1]) ) ruleStrSet.add(ruleStr.toLowerCase());
		}
		ruleStrList = new ObjectArrayList<>(ruleStrSet);

		// 1. Create a root state
		root = new State();

		// 2. Build Trie for rules
		int ridx = 0;
		for( final String ruleStr : ruleStrList ) {
			State curr = root;
			for( final String word : Ruleset.getLhs(ruleStr) ) {
				State next;
				if( curr.split != null && ( next = curr.split.get( word ) ) != null ) {
					curr = next;
				}
				else {
					next = new State( word, curr );
					if( curr.split == null ) {
						curr.split = new Object2ObjectOpenHashMap<>();
					}
					curr.split.put( word, next );
					curr = next;
				}
			}
			if( curr.output == null ) {
				curr.output = new IntArrayList( 3 );
			}
			curr.output.add( ridx );
			ridx += 1;
		}

		// 3. Calculate the failure function
		// Use BFS
		ArrayList<State> currdepth = new ArrayList<>();
		ArrayList<State> nextdepth = new ArrayList<>();

		// Calculate depth-1 states
		for( final Entry<String, State> depth_1_entries : root.split.entrySet() ) {
			final State state = depth_1_entries.getValue();
			state.func = root;
			// Add depth-2 states
			if( state.split != null ) {
				for( final Entry<String, State> depth_2_entries : state.split.entrySet() ) {
					currdepth.add( depth_2_entries.getValue() );
				}
			}
		}

		// Calculate depth-x states
		while( !currdepth.isEmpty() ) {
			for( final State curr : currdepth ) {
				State r = curr.parent.func;
				while( true ) {
					if( r == root || r.split != null && r.split.containsKey( curr.word ) ) {
						break;
					}
					else {
						r = r.func;
					}
				}
				if( r.split.containsKey( curr.word ) ) {
					curr.func = r.split.get( curr.word );
				}
				else {
					curr.func = root;
				}

				// Compute output function
				if( curr.func.output != null ) {
					if( curr.output == null ) {
						curr.output = new IntArrayList();
					}
					curr.output.addAll( curr.func.output );
				}

				// Add next states
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
	
	public int getNumApplicableRules( String[] rec ) {
		int nar = 0;
		State curr = root;
		for ( int i=0; i< rec.length; ) {
			State next;
//			System.out.println(curr.split.keySet());
			if ( curr.split != null && ( next = curr.split.get( rec[i] ) ) != null ) {
				curr = next;
				++i;

//				System.out.println("next.output: "+next.output);
				if ( next.output != null ) {
					nar += next.output.size();
//					for ( int ridx : next.output ) System.out.println(ruleStrList.get(ridx));
				}
			}
			else if ( curr == root ) {
				++i;
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
//			System.out.println(curr.split.keySet());
			if ( curr.split != null && ( next = curr.split.get( rec[i] ) ) != null ) {
				curr = next;
				++i;

//				System.out.println("next.output: "+next.output);
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
