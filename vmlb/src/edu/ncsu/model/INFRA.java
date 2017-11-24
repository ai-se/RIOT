package edu.ncsu.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;

import edu.ncsu.datasets.PEGASUS;

public class INFRA {
	public static final int PEROID_BETWEEN_EVENTS = 1;
	public static final String DATACENTER_NAME = "AWS_EC2";
	public static double bandwidthFluctuation = 0.3;
	public static double cpuFluctuation = 0.24;
	public static double workflodError = 0.3;
	public static ImmutableMap<String, Double> unit_price;
	public static final String[] models, smallmodels;
	static {
		unit_price = ImmutableMap.<String, Double> builder().put("m1.small", 0.06) //
				.put("m3.medium", 0.067) //
				.put("m3.large", 0.133) //
				.put("m3.xlarge", 0.266) //
				.put("m3.2xlarge", 0.532) //
				.put("m4.large", 0.1) //
				.put("m4.xlarge", 0.2) //
				.put("m4.2xlarge", 0.4) //
				.put("m4.4xlarge", 0.8) //
				.build();

		models = new String[] { //
				"sci_Montage_25", "sci_Montage_50", "sci_Montage_100", "sci_Montage_1000", //
				"sci_Epigenomics_24", "sci_Epigenomics_46", "sci_Epigenomics_100", "sci_Epigenomics_997", //
				"sci_CyberShake_30", "sci_CyberShake_50", "sci_CyberShake_100", "sci_CyberShake_1000", //
				"sci_Sipht_30", "sci_Sipht_60", "sci_Sipht_100", "sci_Sipht_1000", //
				"sci_Inspiral_30", "sci_Inspiral_50", "sci_Inspiral_100", "sci_Inspiral_1000" };

		smallmodels = new String[] { "sci_Montage_25", "sci_Montage_50", "sci_Montage_100", "sci_Epigenomics_24",
				"sci_Epigenomics_46", "sci_Epigenomics_100", "sci_CyberShake_30", "sci_CyberShake_50",
				"sci_CyberShake_100", "sci_Sipht_30", "sci_Sipht_60", "sci_Inspiral_30", "sci_Inspiral_50" };
	}

	/**
	 * Creating Amazon EC2 virtual machines
	 * 
	 * @param userId
	 * @param vms
	 * @param idShift
	 * @return
	 */
	private static List<Vm> createEC2Vms() {
		int userId = 0;
		int idShift = 0;
		long size;
		int ram, pesNumber;

		LinkedList<Vm> list = new LinkedList<Vm>();

		// "irrelevant" parameters
		size = 10000;
		ram = 4096;
		pesNumber = 1;

		double sdm = 100;

		CloudletScheduler p = null;
		list.add(new Vm(idShift++, userId, 3.75 * sdm, pesNumber, ram, 85196800, size, "m3.medium", p));
		list.add(new Vm(idShift++, userId, 7.5 * sdm, pesNumber, ram, 35196800, size, "m4.large", p));
		list.add(new Vm(idShift++, userId, 7.5 * sdm, pesNumber, ram, 85196800, size, "m3.large", p));
		list.add(new Vm(idShift++, userId, 15 * sdm, pesNumber, ram, 68196800, size, "m4.xlarge", p));
		list.add(new Vm(idShift++, userId, 15 * sdm, pesNumber, ram, 131072000, size, "m3.xlarge", p));
		list.add(new Vm(idShift++, userId, 30 * sdm, pesNumber, ram, 131072000, size, "m3.2xlarge", p));
		list.add(new Vm(idShift++, userId, 40 * sdm, pesNumber, ram, 131072000, size, "m4.2xlarge", p));
		list.add(new Vm(idShift++, userId, 45 * sdm, pesNumber, ram, 18196800, size, "m4.4xlarge", p));
		return list;
	}

	/**
	 * 
	 * @param userId
	 * @param ins
	 * @param ins2type
	 * @return
	 */
	private static List<Vm> createAWSVms(int[] ins, int[] ins2type) {
		int userId = 0;
		int idShift = 0;
		long size;
		int ram, pesNumber;

		LinkedList<Vm> list = new LinkedList<Vm>();

		// "irrelevant" parameters
		size = 10000;
		ram = 4096;
		pesNumber = 1;

		double sdm = 100;

		for (int i = 0; i <= Ints.max(ins); i++)
			list.add(null);

		CloudletScheduler p = null;
		for (int index : ins) {
			if (index == -1 || list.get(index) != null)
				continue;
			switch (ins2type[index]) {
			case 0:
				list.set(index, new Vm(idShift++, userId, 3.75 * sdm, pesNumber, ram, 85196800, size, "m3.medium", p));
				break;
			case 1:
				list.set(index, new Vm(idShift++, userId, 7.5 * sdm, pesNumber, ram, 35196800, size, "m4.large", p));
				break;
			case 2:
				list.set(index, new Vm(idShift++, userId, 7.5 * sdm, pesNumber, ram, 85196800, size, "m3.large", p));
				break;
			case 3:
				list.set(index, new Vm(idShift++, userId, 15 * sdm, pesNumber, ram, 68196800, size, "m4.xlarge", p));
				break;
			case 4:
				list.set(index, new Vm(idShift++, userId, 15 * sdm, pesNumber, ram, 131072000, size, "m3.xlarge", p));
				break;
			case 5:
				list.set(index, new Vm(idShift++, userId, 40 * sdm, pesNumber, ram, 131072000, size, "m3.2xlarge", p));
				break;
			case 6:
				list.set(index, new Vm(idShift++, userId, 30 * sdm, pesNumber, ram, 131072000, size, "m4.2xlarge", p));
				break;
			case 7:
				list.set(index, new Vm(idShift++, userId, 45 * sdm, pesNumber, ram, 181968000, size, "m4.4xlarge", p));
				break;
			}
		}
		return list;
	}

	public static List<Vm> createVms() {
		return createEC2Vms();
	}

	public static List<Vm> createVms(int[] ins, int[] ins2type) {
		return createAWSVms(ins, ins2type);
	}

	/**
	 * In this pricing model, we assume that the vm is booted as the workflow
	 * starts, and terminates after workflow terminates. TODO 1. vm provision
	 * time? 2. boot only when needed?
	 * 
	 * https://aws.amazon.com/ec2/instance-types/
	 * 
	 * @return
	 */
	public static double getUnitPrice(List<Vm> vmlist) {
		double res = 0;
		for (Vm v : vmlist) {
			res += INFRA.getUnitPrice(v);
		}
		return res;

	}

	public static double getUnitPrice(Vm v) {
		return INFRA.unit_price.get(v.getVmm());
	}

	public static int getAvalVmTypeNum() {
		return 8; // in aws EC2
	}

	/**
	 * Generating the pre-defined study cases Using memo decorator pattern
	 * 
	 * @param dataset
	 * @param brokerId
	 * @param cloudleIdShift
	 * @param preferCloudLetNum4Random
	 *            useful ONLY when dataset="random"
	 * @return List<MyCloudlet> cloudletList + CloudletPassport workflow
	 */
	private static HashMap<Integer, Object[]> archieveCloudLets = new HashMap<Integer, Object[]>();

	public static Object[] getCaseCloudlets(String dataset, int brokerId) {
		int ID = dataset.hashCode() + brokerId;

		if (!archieveCloudLets.containsKey(ID)) {
			List<Task> cloudletList = null;
			DAG workflow = null;

			switch (dataset) {
			default:
				if (dataset.startsWith("sci_")) { // case Scientific Workflow
					PEGASUS sciF = new PEGASUS(brokerId, dataset.substring(4));
					cloudletList = sciF.getCloudletList();
					workflow = sciF.getDAG();
				} else {
					System.err.println("Check the dataset name");
					System.exit(-1);
				}
			}

			workflow.setCloudletNum(cloudletList.size());
			Object[] res = new Object[] { cloudletList, workflow };
			archieveCloudLets.put(ID, res);
		}
		return archieveCloudLets.get(ID);
	}

}
