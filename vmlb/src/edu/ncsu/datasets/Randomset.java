package edu.ncsu.datasets;

import java.util.List;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;

import edu.ncsu.wls.CloudletPassport;
import edu.ncsu.wls.MyCloudlet;

public class Randomset implements Dataset {
	private double starttime = 0.1; // TODO Set start time?
	private List<MyCloudlet> cloudletList;
	private CloudletPassport workflow;

	public Randomset(int userid, int idshift, int cloudlets) {
		// cloudlet parameters
		long length = 40000;
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		MyCloudlet[] cloudlet = new MyCloudlet[cloudlets];

		for (int i = 0; i < cloudlets; i++) {
			cloudlet[i] = new MyCloudlet(idshift + i, length, pesNumber, fileSize, outputSize, utilizationModel,
					utilizationModel, utilizationModel, starttime);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userid);
			this.cloudletList.add(cloudlet[i]);
		}

		workflow = new CloudletPassport();
		workflow.addCloudWorkflow(cloudletList.get(3), cloudletList.get(2));
		workflow.addCloudWorkflow(cloudletList.get(4), cloudletList.get(2));
		workflow.addCloudWorkflow(cloudletList.get(2), cloudletList.get(1));

	}

	@Override
	public List<MyCloudlet> getCloudletList() {
		return this.cloudletList;
	}

	@Override
	public CloudletPassport getCloudletPassport() {
		return workflow;
	}
}
