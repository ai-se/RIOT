package edu.ncsu.experiments;

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

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import edu.ncsu.algorithms.MinMax;
import edu.ncsu.wls.CloudletPassport;
import edu.ncsu.wls.DAGCloudletSchedulerSpaceShared;
import edu.ncsu.wls.Infrastructure;
import edu.ncsu.wls.MyCloudSimHelper;
import edu.ncsu.wls.MyCloudlet;
import edu.ncsu.wls.OnlineDatacenterBroker;

/**
 * For each dataset, run MIN_MAX scheduling strategy (no need to repeat for this
 * algorithm) report the results into lst.csv
 * 
 * @author jianfeng
 *
 */
public class ExpMinmaxSchedule {
	static List<MyCloudlet> cloudletList;
	static CloudletPassport workflow;

	@SuppressWarnings("unchecked")
	public static Map<String, Object> core(String dataset, Map<Integer, Integer> mapping) {
		Log.disable();
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

		// Generate pre defined cloudletList and workflow
		Object[] info;
		info = Infrastructure.getCaseCloudlets(dataset, broker.getId());
		cloudletList = (List<MyCloudlet>) info[0];
		workflow = (CloudletPassport) info[1];

		List<Vm> vmlist = new ArrayList<Vm>();
		vmlist = Infrastructure.createVms(dcBrokerId);
		for (Vm vm : vmlist)
			((DAGCloudletSchedulerSpaceShared) (vm.getCloudletScheduler())).setCloudletPassport(workflow);
		Collections.shuffle(cloudletList);

		// **** VM Assignment
		for (MyCloudlet c : cloudletList) {
			c.setVmId(mapping.get(c.getCloudletId()));
		}

		broker.submitVmList(vmlist);
		broker.submitCloudletList(cloudletList);

		CloudSim.startSimulation();
		CloudSim.stopSimulation();
		List<Cloudlet> newList = broker.getCloudletReceivedList();
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
			vmmap[c.getCloudletId() - Infrastructure.CLOUDLET_ID_SHIFT] = c.getVmId();

		Map<String, Object> record = new HashMap<String, Object>();
		record.put("makespan", (int) newList.get(newList.size() - 1).getFinishTime());
		record.put("dataset", dataset);
		record.put("vmid", vmmap);
		return record;
	}

	public static void main(String[] args) throws IOException {
		File file = new File("minmax.csv");
		// file.createNewFile();
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		String[] models = Infrastructure.models;
		for (int repeat = 0; repeat < 30; repeat++) {
			for (String s : models) {
				Map<Integer, Integer> mapping = MinMax.minmax(MinMax.getEstTimeMatrix(s));
				Map<String, Object> res = core(s, mapping);

				out.write(res.get("dataset").toString());
				out.write(",");
				out.write(res.get("makespan").toString());
				out.write(",");
				for (Integer tmp : (int[]) res.get("vmid"))
					out.write(tmp.toString() + "|");
				out.write("\n");
				out.flush();
			} // for s
		} // for repeat
		out.close();
	}

}
