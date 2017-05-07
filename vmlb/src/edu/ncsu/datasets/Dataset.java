package edu.ncsu.datasets;

import java.util.List;

import edu.ncsu.wls.CloudletPassport;
import edu.ncsu.wls.MyCloudlet;

public interface Dataset {
	public List<MyCloudlet> getCloudletList();
	public CloudletPassport getCloudletPassport();
}
