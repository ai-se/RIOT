package edu.ncsu.experiments;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import edu.ncsu.datasets.Eprotein;
import edu.ncsu.datasets.FMRI;
import edu.ncsu.datasets.Randomset;
import edu.ncsu.wls.CloudletPassport;
import edu.ncsu.wls.DAGCloudletSchedulerSpaceShared;
import edu.ncsu.wls.Infrastructure;
import edu.ncsu.wls.MyCloudSimHelper;
import edu.ncsu.wls.MyCloudlet;
import edu.ncsu.wls.OnlineDatacenterBroker;

public class ExpRandomSchedule {
	static List<MyCloudlet> cloudletList;
	static CloudletPassport workflow;

	public static void main(String[] args) {
		// Initial the cloud simulator
		int num_user = 1; // number of cloud users
		Calendar calendar = Calendar.getInstance();
		boolean trace_flag = false; // trace events
		CloudSim.init(num_user, calendar, trace_flag);

		// create the data center
		Infrastructure.createDatacenter("ncsu");

		// Create the broker
		OnlineDatacenterBroker broker = null;
		try {
			broker = new OnlineDatacenterBroker("dc_broker");
		} catch (Exception e) {
			e.printStackTrace();
		}

		int dcBrokerId = broker.getId();
		// Create the virtual machine
		List<Vm> vmlist = new ArrayList<Vm>();

		vmlist = Infrastructure.createEqualVms(dcBrokerId, 2, 0);
		broker.submitVmList(vmlist);

		if (args.length > 0) {
			switch (args[0]) {
			case "fmri":
				FMRI fmri = new FMRI(broker.getId(), 0);
				cloudletList = fmri.getCloudletList();
				workflow = fmri.getCloudletPassport();
				break;
			case "eprotein":
				Eprotein eprotein = new Eprotein(broker.getId(), 0);
				cloudletList = eprotein.getCloudletList();
				workflow = eprotein.getCloudletPassport();
				break;
			case "random":
				cloudletList = Randomset.createCloudlet(broker.getId(), 10, 0);
				workflow = new CloudletPassport();
				break;
			default:
				System.out.println("Check the dataset name");
				return;
			}
		} else {
			cloudletList = Randomset.createCloudlet(broker.getId(), 10, 0);
			workflow = new CloudletPassport();
		}

		for (Vm vm : vmlist)
			((DAGCloudletSchedulerSpaceShared) (vm.getCloudletScheduler())).setCloudletPassport(workflow);
		Collections.shuffle(cloudletList);
		broker.submitCloudletList(cloudletList);

		// Log.disable();
		CloudSim.startSimulation();
		List<Cloudlet> newList = broker.getCloudletReceivedList();
		CloudSim.stopSimulation();
		MyCloudSimHelper.printCloudletList(newList);

		Log.printLine("Total Finish Cloudlets # " + newList.size());

	}
}
