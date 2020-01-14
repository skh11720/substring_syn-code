package snu.kdd.pkwise;

import java.io.IOException;

import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;

public class TestUtils {

	static DatasetParam defaultParam = new DatasetParam("WIKI", "10000", "1000", "5", "1.0");

	public static WindowDataset getTestRawDataset() throws IOException {
		String datasetName = "WIKI";
		String size = "10000";
		String nr = "1000";
		String qlen = "5";
		String lenRatio = "1.0";
		DatasetParam param = new DatasetParam(datasetName, size, nr, qlen, lenRatio);
		WindowDataset dataset = DatasetFactory.createWindowInstanceByName(param);
		return dataset;
	}

	public static WindowDataset getTestWindowDataset() throws IOException {
		return DatasetFactory.createWindowInstanceByName(defaultParam);
	}
	
	public static WindowDataset getTestTransWindowDataset() throws IOException {
		return DatasetFactory.createTransWindowInstanceByName(defaultParam, "0.6");
	}

	public static WindowDataset getTestWindowDataset( String datasetName, String size, String nr, String qlen, String lenRatio ) throws IOException {
		DatasetParam param = new DatasetParam(datasetName, size, nr, qlen, lenRatio);
		WindowDataset dataset = DatasetFactory.createWindowInstanceByName(param);
		return dataset;
	}

	public static TransWindowDataset getTestTransWindowDataset( String datasetName, String size, String nr, String qlen, String lenRatio, String theta ) throws IOException {
		DatasetParam param = new DatasetParam(datasetName, size, nr, qlen, lenRatio);
		TransWindowDataset dataset = DatasetFactory.createTransWindowInstanceByName(param, theta);
		return dataset;
	}
}
