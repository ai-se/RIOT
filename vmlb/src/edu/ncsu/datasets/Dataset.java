package edu.ncsu.datasets;

import java.util.List;

import edu.ncsu.model.DAG;
import edu.ncsu.model.Task;

public interface Dataset {
	public List<Task> getCloudletList();
	public DAG getDAG();
}