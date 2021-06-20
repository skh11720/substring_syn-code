package snu.kdd.substring_syn.data;

import java.util.ArrayList;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.substring_syn.data.record.ReusableRecord;

/***
 * 
 * An AC automata implementation for rules
 *
 */

public class ACAutomataR {
	private class State {
		ArrayList<Rule> output;
		Int2ObjectOpenHashMap<State> split;

		State func;
		State parent;

		int token;

		State() {
			func = this;
		}

		State( int token, State parent ) {
			this.token = token;
			this.parent = parent;
		}
	}

	private final State root;

	public ACAutomataR( Iterable<Rule> rules ) {
		root = new State();

		for( final Rule rule : rules ) {
			State curr = root;
			for( final int str : rule.getLhs() ) {
				State next;
				if( curr.split != null && ( next = curr.split.get( str ) ) != null ) {
					curr = next;
				}
				else {
					next = new State( str, curr );
					if( curr.split == null ) {
						curr.split = new Int2ObjectOpenHashMap<>();
					}
					curr.split.put( str, next );
					curr = next;
				}
			}
			if( curr.output == null ) {
				curr.output = new ArrayList<>( 3 );
			}
			curr.output.add( rule );
		}

		ArrayList<State> currdepth = new ArrayList<>();
		ArrayList<State> nextdepth = new ArrayList<>();

		for( final Entry<Integer, State> depth_1_entries : root.split.entrySet() ) {
			final State state = depth_1_entries.getValue();
			state.func = root;
			if( state.split != null ) {
				for( final Entry<Integer, State> depth_2_entries : state.split.entrySet() ) {
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
						curr.output = new ArrayList<>();
					}
					curr.output.addAll( curr.func.output );
				}

				if( curr.split != null ) {
					for( final Entry<Integer, State> child : curr.split.entrySet() ) {
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

	public Rule[][] applicableRules( int[] tokens ) {
		@SuppressWarnings( "unchecked" )
		final ObjectOpenHashSet<Rule>[] tmprslt = new ObjectOpenHashSet[ tokens.length ];
		for( int i = 0; i < tokens.length; ++i ) {
			tmprslt[ i ] = new ObjectOpenHashSet<>();
		}

		State curr = root;
		int i = 0;
		while( i < tokens.length ) {
			State next;
			if( curr.split != null && ( next = curr.split.get( tokens[ i ] ) ) != null ) {
				curr = next;
				++i;

				if( next.output != null ) {
					for( final Rule rule : next.output ) {
						tmprslt[ i - rule.getLhs().length ].add( rule );
					}
				}
			}
			else if( curr == root ) {
				++i;
			}
			else {
				curr = curr.func;
			}
		}

		final Rule[][] result = new Rule[ tokens.length ][];
		for( i = 0; i < tokens.length; ++i ) {
			result[ i ] = tmprslt[ i ].toArray( new Rule[ 0 ] );
		}
		return result;
	}
	
	public void computeApplicableRules(ReusableRecord rec) {
		State curr = root;
		int i = 0;
		while ( i < rec.size() ) {
			State next;
			if ( curr.split != null && ( next = curr.split.get(rec.getToken(i))) != null ) {
				curr = next;
				i += 1;
				if ( next.output != null ) {
					for ( final Rule rule : next.output ) {
						rec.addApplicableRule(i-rule.getLhs().length, rule);
					}
				}
			}
			else if ( curr == root ) i += 1;
			else curr = curr.func;
		}
	}
}
