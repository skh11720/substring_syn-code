package snu.kdd.pkwise;

import java.io.IOException;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetParam;

public class TestUtils {

	public static WindowDataset getTestRawDataset() throws IOException {
		String datasetName = "WIKI";
		String size = "10000";
		String nr = "1000";
		String qlen = "5";
		DatasetParam param = new DatasetParam(datasetName, size, nr, qlen, null);
		WindowDataset dataset = new WindowDataset(param);
		return dataset;
	}

	public static WindowDataset getTestDataset() throws IOException {
		String datasetName = "WIKI";
		String size = "10000";
		String nr = "1000";
		String qlen = "5";
		DatasetParam param = new DatasetParam(datasetName, size, nr, qlen, null);
		WindowDataset dataset = Dataset.createWindowInstanceByName(param);
		return dataset;
	}

	public static WindowDataset getTestDataset( String datasetName, String size, String nr, String qlen ) throws IOException {
		DatasetParam param = new DatasetParam(datasetName, size, nr, qlen, null);
		WindowDataset dataset = Dataset.createWindowInstanceByName(param);
		return dataset;
	}

	public static TransWindowDataset getTestDataset( String datasetName, String size, String nr, String qlen, String theta ) throws IOException {
		DatasetParam param = new DatasetParam(datasetName, size, nr, qlen, null);
		TransWindowDataset dataset = Dataset.createTransWindowInstanceByName(param, theta);
		return dataset;
	}
}
