package snu.kdd.pkwise;

import java.io.IOException;

import snu.kdd.substring_syn.data.Dataset;

public class TestUtils {

	public static WindowDataset getTestRawDataset() throws IOException {
		String datasetName = "WIKI";
		String size = "10000";
		String nr = "1000";
		String qlen = "5";
		WindowDataset dataset = new WindowDataset(datasetName, size, nr, qlen);
		return dataset;
	}

	public static WindowDataset getTestDataset() throws IOException {
		String datasetName = "WIKI";
		String size = "10000";
		String nr = "1000";
		String qlen = "5";
		WindowDataset dataset = Dataset.createWindowInstanceByName(datasetName, size, nr, qlen);
		return dataset;
	}
}
