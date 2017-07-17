package edu.ncsu.datasets;

import java.util.List;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;

import edu.ncsu.wls.DAG;
import edu.ncsu.wls.Task;

public class Randomset implements Dataset {
	private List<Task> cloudletList;
	private DAG workflow;

	public Randomset(int userid, int idshift, int cloudlets) {
		// cloudlet parameters
		long length = 40000;
		// long fileSize = 300;
		// long outputSize = 300;
		// int pesNumber = 1;
		// UtilizationModel utilizationModel = new UtilizationModelFull();

		Task[] cloudlet = new Task[cloudlets];

		for (int i = 0; i < cloudlets; i++) {
			// cloudlet[i] = new Task(idshift + i, length, pesNumber, fileSize,
			// outputSize, utilizationModel,
			// utilizationModel, utilizationModel);
			cloudlet[i] = new Task(idshift + i, length);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userid);
			this.cloudletList.add(cloudlet[i]);
		}

		workflow = new DAG();
		workflow.addCloudWorkflow(cloudletList.get(3), cloudletList.get(2));
		workflow.addCloudWorkflow(cloudletList.get(4), cloudletList.get(2));
		workflow.addCloudWorkflow(cloudletList.get(2), cloudletList.get(1));

	}

	@Override
	public List<Task> getCloudletList() {
		return this.cloudletList;
	}

	@Override
	public DAG getCloudletPassport() {
		return workflow;
	}
}
