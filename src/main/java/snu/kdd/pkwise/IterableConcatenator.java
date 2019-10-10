package snu.kdd.pkwise;

import java.util.Iterator;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class IterableConcatenator<T> {
	
	private final List<Iterator<T>> iteratorList;
	private int n;
	
	public IterableConcatenator() {
		this.iteratorList = new ObjectArrayList<Iterator<T>>();
		n = 0;
	}

	public IterableConcatenator( List<Iterable<T>> iterableList ) {
		this();
		for ( Iterable<T> iterable : iterableList ) addIterable(iterable);
	}
	
	public void addIterable( Iterable<T> iterable ) {
		iteratorList.add(iterable.iterator());
		n += 1;
	}
	
	public Iterable<T> iterable() {
		return new Iterable<T>() {

			@Override
			public Iterator<T> iterator() {
				return new IterableListIterator();
			}
		};
	}
	
	
	class IterableListIterator implements Iterator<T> {
		
		Iterator<T>	curr;
		int i = 0;
		
		public IterableListIterator() {
			curr = iteratorList.get(0);
		}

		@Override
		public boolean hasNext() {
			return ( i < n-1 || curr.hasNext() );
		}

		@Override
		public T next() {
			if ( !curr.hasNext() ) {
				i += 1;
				curr = iteratorList.get(i);
			}
			return curr.next();
		}
	}
}
