package edu.ncsu.strategies;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import edu.ncsu.wls.CloudletPassport;
import edu.ncsu.wls.DAGCloudletSchedulerSpaceShared;
import edu.ncsu.wls.Infrastructure;
import edu.ncsu.wls.MyCloudlet;
import edu.ncsu.wls.OnlineDatacenterBroker;

/**
 * Contains one static evaluation function for SA, GA, PSO, etc.
 *
 * @author jianfeng
 *
 */
public class CoreEval {
	/**
	 * 
	 * @param dataset
	 *            Name of dataset
	 * @param mapping
	 *            The VM provision array. Ranked by clouldetId-shift. All values
	 *            should be vm ID.
	 */
	@SuppressWarnings("unchecked")
	public static void eval(String dataset, Chromosome config) {
		// Create Cloudsim server
		int num_user = 1; // number of cloud users
		Calendar calendar = Calendar.getInstance();
		boolean trace_flag = false; // trace events
		CloudSim.init(num_user, calendar, trace_flag);
		Infrastructure.createDatacenter();
		OnlineDatacenterBroker broker = null;
		try {
			broker = new OnlineDatacenterBroker("dc_broker");
		} catch (Exception e) {
			e.printStackTrace();
		}

		int dcBrokerId = broker.getId();

		// Get dataset
		Object[] info = Infrastructure.getCaseCloudlets(dataset, broker.getId());
		List<MyCloudlet> cloudletList = (List<MyCloudlet>) info[0];
		CloudletPassport workflow = (CloudletPassport) info[1];

		// Create vm list
		List<Vm> vmlist = new ArrayList<Vm>();
		vmlist = Infrastructure.createVms(dcBrokerId);

		for (Vm vm : vmlist)
			((DAGCloudletSchedulerSpaceShared) (vm.getCloudletScheduler())).setCloudletPassport(workflow);

		broker.submitCloudletList(cloudletList);
		broker.submitVmList(vmlist);

		int[] mapping = config.getMapping();
		for (Cloudlet c : cloudletList) {
			c.setVmId(mapping[c.getCloudletId() - Infrastructure.CLOUDLET_ID_SHIFT]);
		}

		CloudSim.startSimulation();
		CloudSim.stopSimulation();

		List<Cloudlet> newList = broker.getCloudletReceivedList();
		// MyCloudSimHelper.printCloudletList(newList);

		if (newList.size() != cloudletList.size()) {
			System.err.println("can not simulating all cloudlets!");
			System.exit(-1);
		}

		double makespan = 0;
		for (Cloudlet c : newList) {
			makespan = Math.max(makespan, c.getFinishTime());
		}

		config.setMakespan(makespan);
	}
}
