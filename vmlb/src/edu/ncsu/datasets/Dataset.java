package edu.ncsu.datasets;

import java.util.List;

import edu.ncsu.wls.DAG;
import edu.ncsu.wls.Task;

public interface Dataset {
	public List<Task> getCloudletList();
	public DAG getDAG();
}