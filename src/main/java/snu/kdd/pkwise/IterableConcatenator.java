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
		
		Iterator<T>	curr = null;
		int i = -1;
		
		public IterableListIterator() {
			findNext();
		}

		@Override
		public boolean hasNext() {
			return ( i < n-1 || (curr != null && curr.hasNext()) );
		}

		@Override
		public T next() {
			T item = curr.next();
			findNext();
			return item;
		}
		
		private void findNext() {
			while ( curr == null || !curr.hasNext() ) {
				i += 1;
				if ( i >= n ) break;
				curr = iteratorList.get(i);
			}
		}
	}
}
