package snu.kdd.substring_syn.object.indexstore;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import snu.kdd.substring_syn.data.IntPair;

public abstract class IndexStore<E extends Serializable> {
	
	protected final EntryStore<E> store;
	protected final InvertedIndex index;

	public IndexStore(EntryStore<E> store) {
		this.store = store;
		Iterable<E> entries = store.getEntries();
		Iterable<IntPair> kvList = getKvList(entries);
		index = new InvertedIndex(kvList);
	}
	
	protected abstract Iterable<IntPair> getKvList(Iterable<E> entries);
	
	public final Iterable<E> getEntries(int key) {
        IntList invList = index.getInvList(key);
        if ( invList == null ) return Collections.emptyList();
        else return new Iterable<E>() {

			@Override
			public Iterator<E> iterator() {
				return new IndexEntryIterator(invList);
			}
		};
	}
	
	public final IntList getInvList(int key) {
		IntList invList = index.getInvList(key);
		if ( invList == null ) return IntLists.EMPTY_LIST;
		else return invList;
	}
	
	public final BigInteger diskSpaceUsage() {
		return store.diskSpaceUsage().add(index.invListAccessor.diskSpaceUsage);
	}
	
	final class IndexEntryIterator implements Iterator<E> {
		
		final IntList invList;
		int i = 0;
		
		public IndexEntryIterator(IntList invList) {
			this.invList = invList;
		}

		@Override
		public boolean hasNext() {
			return invList != null && i < invList.size();
		}

		@Override
		public E next() {
			int id = invList.get(i);
			E entry = store.getEntry(id);
			i += 1;
			return entry;
		}
		
	}
}
