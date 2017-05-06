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
import java.util.concurrent.ThreadLocalRandom;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import edu.ncsu.datasets.Eprotein;
import edu.ncsu.datasets.FMRI;
import edu.ncsu.datasets.PSPLIB;
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

	public static Map<String, Object> core(String[] args) {
		int num_user = 1; // number of cloud users
		Calendar calendar = Calendar.getInstance();
		boolean trace_flag = false; // trace events
		CloudSim.init(num_user, calendar, trace_flag);
		Infrastructure.createDatacenter("ncsu");

		// Create the broker
		OnlineDatacenterBroker broker = null;
		try {
			broker = new OnlineDatacenterBroker("dc_broker");
		} catch (Exception e) {
			e.printStackTrace();
		}

		int dcBrokerId = broker.getId();

		List<Vm> vmlist = new ArrayList<Vm>();
		vmlist = Infrastructure.createEC2Vms(dcBrokerId, 0);

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
				workflow = Randomset.getPassport(cloudletList);
				break;
			case "j30":
				PSPLIB psp1 = new PSPLIB(broker.getId(), 0, "j301_1", 30);
				cloudletList = psp1.getCloudletList();
				workflow = psp1.getCloudletPassport();
				break;
			case "j60":
				PSPLIB psp2 = new PSPLIB(broker.getId(), 0, "j601_1", 60);
				cloudletList = psp2.getCloudletList();
				workflow = psp2.getCloudletPassport();
				break;
			case "j90":
				PSPLIB psp3 = new PSPLIB(broker.getId(), 0, "j901_1", 90);
				cloudletList = psp3.getCloudletList();
				workflow = psp3.getCloudletPassport();
				break;
			default:
				System.out.println("Check the dataset name");
				return null;
			}
		} else {
			cloudletList = Randomset.createCloudlet(broker.getId(), 10, 0);
			workflow = new CloudletPassport();
		}

		for (Vm vm : vmlist)
			((DAGCloudletSchedulerSpaceShared) (vm.getCloudletScheduler())).setCloudletPassport(workflow);
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
