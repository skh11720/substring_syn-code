package snu.kdd.faerie;

import java.io.Serializable;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class FaerieIndexEntry implements Serializable {
	
	final Int2ObjectMap<IntList> tok2posListMap;
	
	public FaerieIndexEntry( int[] arr ) {
		tok2posListMap = new Int2ObjectOpenHashMap<IntList>();
		for ( int i=0; i<arr.length; ++i ) {
			int token = arr[i];
			if ( !tok2posListMap.containsKey(token) ) tok2posListMap.put(token, new IntArrayList());
			tok2posListMap.get(token).add(i);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder strbld = new StringBuilder("FaerieIndexEntry {\n");
		for ( Int2ObjectMap.Entry<IntList> entry : tok2posListMap.int2ObjectEntrySet() ) {
			strbld.append("\t"+entry.getIntKey()+":"+entry.getValue()+"\n");
		}
		strbld.append("}");
		return strbld.toString();
	}

	private static final long serialVersionUID = 34239854729843L;
	
	
//	public static void main(String[] args) throws IOException, ClassNotFoundException {
//		int[] arr = {3, 2, 5, 3, 7, 3, 2, 3, 1};
//		FaerieIndexEntry entry = new FaerieIndexEntry(arr);
//
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		ObjectOutputStream oos = new ObjectOutputStream(bos);
//		oos.writeObject(entry);
//		oos.flush();
//		byte[] buf = bos.toByteArray();
//		System.out.println(Arrays.toString(buf));
//		
//		ByteArrayInputStream bis = new ByteArrayInputStream(buf);
//		ObjectInputStream ois = new ObjectInputStream(bis);
//		FaerieIndexEntry entry2 = (FaerieIndexEntry)ois.readObject();
//		System.out.println(entry2);
//	}
}
