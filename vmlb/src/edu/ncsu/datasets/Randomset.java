package edu.ncsu.datasets;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;

import edu.ncsu.wls.CloudletPassport;
import edu.ncsu.wls.MyCloudlet;

public class Randomset {
	public static List<MyCloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
		// Creates a container to store Cloudlets
		LinkedList<MyCloudlet> list = new LinkedList<MyCloudlet>();

		// cloudlet parameters
		long length = 40000;
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		MyCloudlet[] cloudlet = new MyCloudlet[cloudlets];

		for (int i = 0; i < cloudlets; i++) {
			cloudlet[i] = new MyCloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel,
					utilizationModel, utilizationModel, ThreadLocalRandom.current().nextInt(0, 1));
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			// cloudlet[i].setVmId(ThreadLocalRandom.current().nextInt(0,100));
			cloudlet[i].setVmId(i % 3);
			list.add(cloudlet[i]);
		}

		return list;
	}

	public static CloudletPassport getPassport(List<MyCloudlet> cloudletList) {
		CloudletPassport testingWorkFlow = new CloudletPassport();
		testingWorkFlow.addCloudWorkflow(cloudletList.get(3), cloudletList.get(2));
		testingWorkFlow.addCloudWorkflow(cloudletList.get(4), cloudletList.get(2));
		testingWorkFlow.addCloudWorkflow(cloudletList.get(2), cloudletList.get(1));

		return testingWorkFlow;
	}
}
