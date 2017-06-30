package edu.ncsu.algorithms;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import edu.ncsu.wls.CloudletDAG;
import edu.ncsu.wls.DAGCentralScheduler;
import edu.ncsu.wls.Infrastructure;
import edu.ncsu.wls.MyCloudSimHelper;
import edu.ncsu.wls.MyCloudlet;
import edu.ncsu.wls.OnlineDatacenterBroker;

/**
 * Experiment 1: for all dataset, repeat 30 times. Randomly assign the task, return the makespan into lst.csv
 * 
 * @author jianfeng
 *
 */
public class RandomSchedule {
	static List<MyCloudlet> cloudletList;
	static CloudletDAG workflow;

	@SuppressWarnings("unchecked")
	public static Map<String, Object> core(String[] args) {
		int num_user = 1; // number of cloud users
		Calendar calendar = Calendar.getInstance();
		boolean trace_flag = false; // trace events
		CloudSim.init(num_user, calendar, trace_flag);
		Infrastructure.createDatacenter();

		// Create the broker
		OnlineDatacenterBroker broker = null;
		try {
			broker = new OnlineDatacenterBroker("dc_broker");
		} catch (Exception e) {
			e.printStackTrace();
		}

		int dcBrokerId = broker.getId();

		List<Vm> vmlist = new ArrayList<Vm>();
		vmlist = Infrastructure.createVms(dcBrokerId); 
		broker.submitVmList(vmlist);

		// Generate pre defined cloudletList and workflow
		Object[] info;
		if (args.length > 0) {
			info = Infrastructure.getCaseCloudlets(args[0], broker.getId());

		} else {
			info = Infrastructure.getCaseCloudlets("random", broker.getId());
		}
		cloudletList = (List<MyCloudlet>) info[0];
		workflow = (CloudletDAG) info[1];

		for (Vm vm : vmlist)
			((DAGCentralScheduler) (vm.getCloudletScheduler())).setCloudletPassport(workflow);
		Collections.shuffle(cloudletList);

		// Random Assign cloudlet to vms
		for (MyCloudlet c : cloudletList) {
			c.setVmId(vmlist.get(ThreadLocalRandom.current().nextInt(0, vmlist.size())).getId());
		}

		broker.submitCloudletList(cloudletList);
		// Log.disable();
		CloudSim.startSimulation();
		CloudSim.stopSimulation();
		List<Cloudlet> newList = broker.getCloudletReceivedList();
		Log.enable();
		MyCloudSimHelper.printCloudletList(newList);

		// CHECK ERRORS!!
		if (newList.size() != cloudletList.size()) {
			boolean[] tmp = new boolean[30];
			for (Cloudlet c : newList) {
				tmp[c.getCloudletId()] = true;
			}
			for (int i = 0; i < 30; i++)
				if (!tmp[i])
					System.err.println(i);
			System.err.println("Check errors");
			return null;
		}

		// writing the record
		int[] vmmap = new int[cloudletList.size()];
		for (Cloudlet c : newList)
			vmmap[c.getCloudletId()] = c.getVmId();
		Map<String, Object> record = new HashMap<String, Object>();
		record.put("makespan", (int) newList.get(newList.size() - 1).getFinishTime());
		record.put("dataset", args.length > 0 ? args[0] : "random");
		record.put("vmid", vmmap);
		return record;
	}

	public static void main(String[] args) throws IOException {
		File file = new File("lst.csv");
		// file.createNewFile();
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		String[] mockargs = new String[1];
		String[] models = new String[] { "fmri", "eprotein", "j30", "j60", "j90" };
		for (String s : models) {
			mockargs[0] = s;
			int repeat = 30;
			for (int i = 1; i <= repeat; i++) {
				Map<String, Object> res = core(mockargs);

				out.write(res.get("dataset").toString());
				out.write(",");
				out.write(res.get("makespan").toString());
				out.write(",");
				for (Integer tmp : (int[]) res.get("vmid"))
					out.write(tmp.toString() + "|");
				out.write("\n");
			}
			out.flush();
		}
		out.close();
	}

}
