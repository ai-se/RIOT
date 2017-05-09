package edu.ncsu.strategies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import edu.ncsu.wls.CloudletPassport;
import edu.ncsu.wls.DAGCloudletSchedulerSpaceShared;
import edu.ncsu.wls.Infrastructure;
import edu.ncsu.wls.MyCloudlet;
import edu.ncsu.wls.OnlineDatacenterBroker;

public class MinMax {
	/**
	 * For each cloudlet in the dataset, calculate estimated executing time in
	 * every virtual machines
	 * 
	 * WARNING, returns are SHIFTs, not real IDs.
	 * 
	 * @param dataset
	 * @param vmNum
	 * @return res.get(vmid).get(cloudletId) -> get the estimated exec time
	 */
	@SuppressWarnings("unchecked")
	public static Map<Integer, Map<Integer, Double>> getEstTimeMatrix(String dataset) {
		Log.disable();
		Map<Integer, Map<Integer, Double>> res = new HashMap<Integer, Map<Integer, Double>>();
		int vmNum = 1;

		for (int i = 0; i < vmNum; i++) {
			Map<Integer, Double> resi = new HashMap<Integer, Double>();

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
			List<MyCloudlet> cloudletList;

			cloudletList = (List<MyCloudlet>) Infrastructure.getCaseCloudlets(dataset, broker.getId())[0];

			// Create vm list
			List<Vm> vmlist = new ArrayList<Vm>();
			vmlist = Infrastructure.createVms(dcBrokerId);
			vmNum = vmlist.size();

			CloudletPassport workflow = new CloudletPassport(); // in MinMin or
																// MinMax, we
																// ignore
																// the DAG
			for (Vm vm : vmlist)
				((DAGCloudletSchedulerSpaceShared) (vm.getCloudletScheduler())).setCloudletPassport(workflow);

			broker.submitCloudletList(cloudletList);
			broker.submitVmList(vmlist);

			for (Cloudlet c : cloudletList) {
				c.setVmId(vmlist.get(i).getId());
			}

			CloudSim.startSimulation();
			CloudSim.stopSimulation();

			List<Cloudlet> newList = broker.getCloudletReceivedList();
			// MyCloudSimHelper.printCloudletList(newList);

			if (newList.size() != cloudletList.size()) {
				System.err.println("can not simulating all cloudlets!");
				System.exit(-1);
			}

			for (Cloudlet c : newList) {
				resi.put(c.getCloudletId(), c.getActualCPUTime());
			}
			res.put(vmlist.get(i).getId(), resi);
		}
		Log.enable();
		return res;
	}

	/**
	 * calc argmin(list)
	 * 
	 * @param lst
	 * @return
	 */
	private static int argmin(double[] lst) {
		double base = lst[0];
		int res = 0;
		for (int i = 1; i < lst.length; i++) {
			if (lst[i] < base) {
				res = i;
				base = lst[i];
			}
		}
		return res;
	}

	private static int argmax(double[] lst) {
		double base = lst[0];
		int res = 0;
		for (int i = 0; i < lst.length; i++) {
			if (lst[i] > base) {
				res = i;
				base = lst[i];
			}
		}
		return res;
	}

	/**
	 * 
	 * @param estTimeMatrix
	 * @return a map key:cloudlet id, value:best Vm id
	 */
	public static Map<Integer, Integer> minmin(Map<Integer, Map<Integer, Double>> estTimeMatrix) {
		Object[] vmid_tmp = estTimeMatrix.keySet().toArray();
		int[] vid = Arrays.stream(vmid_tmp).mapToInt(o -> (int) o).toArray();

		Object[] cloudletid_tmp = estTimeMatrix.get(vid[0]).keySet().toArray();
		int[] cid = Arrays.stream(cloudletid_tmp).mapToInt(o -> (int) o).toArray();

		int vmNum = vid.length;
		int cloudletNum = cid.length;

		Map<Integer, Integer> assignedTo = new HashMap<Integer, Integer>(); // cloudid
																			// as
																			// key
		Map<Integer, Double> currentVmLoad = new HashMap<Integer, Double>(); // vm
																				// id
																				// as
																				// key
		for (int i : vid)
			currentVmLoad.put(i, 0.0);

		int finished = 0;
		while (finished < cloudletNum) {
			double[] z = new double[cloudletNum]; // marking the best load for
													// each cloudlet
			int[] correspondingAssign = new int[cloudletNum];
			Arrays.fill(z, Double.MAX_VALUE);
			Arrays.fill(correspondingAssign, -1);

			for (int c = 0; c < cloudletNum; c++) { // for each cloudlet
				if (assignedTo.containsKey(cid[c]))
					continue; // has been assigned

				// find the best vm for c-th cloudlet under current vm workload
				int bestVm = -1;
				double bestLoad = Double.MAX_VALUE;
				for (int v = 0; v < vmNum; v++) { // for each machine
					if (currentVmLoad.get(vid[v]) + estTimeMatrix.get(vid[v]).get(cid[c]) < bestLoad) {
						bestVm = vid[v];
						bestLoad = currentVmLoad.get(vid[v]) + estTimeMatrix.get(vid[v]).get(cid[c]);
					}
				}
				z[c] = bestLoad;
				correspondingAssign[c] = bestVm;
			}
			int assignTask = argmin(z);
			int selected_vm = correspondingAssign[assignTask];
			assignedTo.put(cid[assignTask], selected_vm);
			currentVmLoad.put(selected_vm,
					currentVmLoad.get(selected_vm) + estTimeMatrix.get(selected_vm).get(cid[assignTask]));
			finished++;
		}

		return assignedTo;
	}

	public static Map<Integer, Integer> minmax(Map<Integer, Map<Integer, Double>> estTimeMatrix) {
		Object[] vmid_tmp = estTimeMatrix.keySet().toArray();
		int[] vid = Arrays.stream(vmid_tmp).mapToInt(o -> (int) o).toArray();

		Object[] cloudletid_tmp = estTimeMatrix.get(vid[0]).keySet().toArray();
		int[] cid = Arrays.stream(cloudletid_tmp).mapToInt(o -> (int) o).toArray();

		int vmNum = vid.length;
		int cloudletNum = cid.length;

		Map<Integer, Integer> assignedTo = new HashMap<Integer, Integer>(); // cloudid
																			// as
																			// key
		Map<Integer, Double> currentVmLoad = new HashMap<Integer, Double>(); // vm
																				// id
																				// as
																				// key
		for (int i : vid)
			currentVmLoad.put(i, 0.0);

		int finished = 0;
		while (finished < cloudletNum) {
			double[] z = new double[cloudletNum]; // marking the best load for
													// each cloudlet
			int[] correspondingAssign = new int[cloudletNum];
			Arrays.fill(z, -Double.MAX_VALUE);
			Arrays.fill(correspondingAssign, -1);

			for (int c = 0; c < cloudletNum; c++) { // for each cloudlet
				if (assignedTo.containsKey(cid[c]))
					continue; // has been assigned

				// find the best vm for c-th cloudlet under current vm workload
				int bestVm = -1;
				double bestLoad = Double.MAX_VALUE;
				for (int v = 0; v < vmNum; v++) { // for each machine
					if (currentVmLoad.get(vid[v]) + estTimeMatrix.get(vid[v]).get(cid[c]) < bestLoad) {
						bestVm = vid[v];
						bestLoad = currentVmLoad.get(vid[v]) + estTimeMatrix.get(vid[v]).get(cid[c]);
					}
				}
				z[c] = bestLoad;
				correspondingAssign[c] = bestVm;
			}

			int assignTask = argmax(z);
			int selected_vm = correspondingAssign[assignTask];
			assignedTo.put(cid[assignTask], selected_vm);
			currentVmLoad.put(selected_vm,
					currentVmLoad.get(selected_vm) + estTimeMatrix.get(selected_vm).get(cid[assignTask]));
			finished++;
		}

		return assignedTo;
	}

	public static void main(String[] args) {
		Map<Integer, Map<Integer, Double>> estTimeMatrix = getEstTimeMatrix("j90");
		Map<Integer, Integer> res = minmin(estTimeMatrix);
		System.out.println(res);
	}

}
