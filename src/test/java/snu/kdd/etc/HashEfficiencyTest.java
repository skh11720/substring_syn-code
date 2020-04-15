//package snu.kdd.etc;
//
//import java.io.IOException;
//
//import org.junit.Test;
//
//import snu.kdd.substring_syn.data.DatasetFactory;
//import snu.kdd.substring_syn.data.DatasetParam;
//import snu.kdd.substring_syn.data.record.Record;
//import snu.kdd.substring_syn.utils.StatContainer;
//
//public class HashEfficiencyTest {
//
//	@Test
//	public void test() throws IOException {
//		/*
//		 7299.7554
//		 */
//		long ts = System.nanoTime();
//		DatasetFactory.param = new DatasetParam("WIKI", "300000", "107836", "5", "1.0");
//		DatasetFactory.statContainer = new StatContainer();
//		DatasetFactory.initCreationProcess(DatasetFactory.param);
//		Record.tokenIndex = DatasetFactory.buildTokenIndex();
//		System.out.println((System.nanoTime()-ts)/1e6);
//	}
//}
