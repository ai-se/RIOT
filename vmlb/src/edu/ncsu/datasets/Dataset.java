package edu.ncsu.datasets;

import java.util.List;

import edu.ncsu.wls.CloudletDAG;
import edu.ncsu.wls.MyCloudlet;

public interface Dataset {
	public List<MyCloudlet> getCloudletList();
	public CloudletDAG getCloudletPassport();
}
