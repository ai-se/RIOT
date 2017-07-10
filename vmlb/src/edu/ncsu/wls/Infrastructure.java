package edu.ncsu.wls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;

import edu.ncsu.datasets.Eprotein;
import edu.ncsu.datasets.FMRI;
import edu.ncsu.datasets.PEGASUS;
import edu.ncsu.datasets.PSPLIB;
import edu.ncsu.datasets.Randomset;

public class Infrastructure {
	public static final int VM_ID_SHIFT = 0;
	public static final int CLOUDLET_ID_SHIFT = 0;
	public static final int PEROID_BETWEEN_EVENTS = 1;
	public static final String DATACENTER_NAME = "AWS_EC2";
	// public static final String[] models = new String[] { "fmri", "eprotein",
	// "j30_1", "j30_2", "j60_1", "j60_2",
	// "j90_1", "j90_2", "j120_1", "j120_2" };

	public static final String[] models = new String[] { "sci_Montage_25", "sci_Montage_50", "sci_Montage_100",
			"sci_Montage_1000", "sci_Epigenomics_24", "sci_Epigenomics_46", "sci_Epigenomics_100",
			"sci_Epigenomics_997", "sci_CyberShake_30", "sci_CyberShake_50", "sci_CyberShake_100",
			"sci_CyberShake_1000", "sci_Sipht_30", "sci_Sipht_60", "sci_Sipht_100", "sci_Sipht_1000", "sci_Inspiral_30",
			"sci_Inspiral_50", "sci_Inspiral_100", "sci_Inspiral_1000" };

	// public static final String[] models = new String[] { "sci_Montage_25",
	// "sci_Epigenomics_24", "sci_CyberShake_30",
	// "sci_Sipht_30", "sci_Inspiral_30", "sci_Inspiral_50" };

	/**
	 * Creating Amazon EC2 virtual machines
	 * 
	 * @param userId
	 * @param vms
	 * @param idShift
	 * @return
	 */
	private static List<Vm> createEC2Vms(int userId) {
		// int idShift = VM_ID_SHIFT;
		// long size, bw;
		// int ram, mips, pesNumber;
		// String vmm;
		// // Creates a container to store VMs. This list is passed to the
		// broker
		// // later
		// LinkedList<Vm> list = new LinkedList<Vm>();
		//
		// // create VMs
		// // VM Parameters
		// size = 10000; // image size (MB)
		// ram = 4096; // vm memory (MB)
		// mips = 225;
		// bw = 1000;
		// pesNumber = 2; // number of cpus
		// vmm = "t2.medium"; // VMM name
		// list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size,
		// vmm, new DAGCentralScheduler()));
		//
		// // =========================
		// size = 8000; // image size (MB)
		// ram = 4096; // vm memory (MB)
		// mips = 150;
		// bw = 1000;
		// pesNumber = 1; // number of cpus
		// vmm = "t2.small"; // VMM name
		// list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size,
		// vmm, new DAGCentralScheduler()));
		//
		// // =========================
		// size = 8000; // image size (MB)
		// ram = 4096; // vm memory (MB)
		// mips = 150;
		// bw = 1000;
		// pesNumber = 1; // number of cpus
		// vmm = "t2.small"; // VMM name
		// list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size,
		// vmm, new DAGCentralScheduler()));
		//
		// // =============
		// size = 10000; // image size (MB)
		// ram = 4 * 1024; // vm memory (MB)
		// mips = 200;
		// bw = 1500;
		// pesNumber = 1; // number of cpus
		// vmm = "m3.medium"; // VMM name
		// list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size,
		// vmm, new DAGCentralScheduler()));
		// // list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size,
		// // vmm, new DAGCloudletSchedulerSpaceShared()));
		//
		// return list;

		int idShift = VM_ID_SHIFT;
		long size;
		int ram, pesNumber;

		LinkedList<Vm> list = new LinkedList<Vm>();

		// "irrelevant" parameters
		size = 10000;
		ram = 4096;
		pesNumber = 1;

		double sdm = 100;

		DAGCentralScheduler p = new DAGCentralScheduler();
		list.add(new Vm(idShift++, userId, 3.75 * sdm, pesNumber, ram, 85196800, size, "m3.medium", p));
		list.add(new Vm(idShift++, userId, 7.5 * sdm, pesNumber, ram, 85196800, size, "m3.large", p));
		list.add(new Vm(idShift++, userId, 15 * sdm, pesNumber, ram, 131072000, size, "m3.xlarge", p));
		list.add(new Vm(idShift++, userId, 30 * sdm, pesNumber, ram, 131072000, size, "m3.2xlarge", p));
		list.add(new Vm(idShift++, userId, 7.5 * sdm, pesNumber, ram, 35196800, size, "m4.large", p));
		list.add(new Vm(idShift++, userId, 15 * sdm, pesNumber, ram, 68196800, size, "m4.xlarge", p));
		list.add(new Vm(idShift++, userId, 30 * sdm, pesNumber, ram, 131072000, size, "m4.2xlarge", p));
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
	private static List<Vm> createAWSVms(int userId, int[] ins, int[] ins2type) {
		int idShift = VM_ID_SHIFT;
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

		DAGCentralScheduler p = new DAGCentralScheduler();
		for (int index : ins) {
			if (index == -1 || list.get(index) != null)
				continue;
			switch (ins2type[index]) {
			case 0:
				list.set(index, new Vm(idShift++, userId, 3.75 * sdm, pesNumber, ram, 85196800, size, "m3.medium", p));
				break;
			case 1:
				list.set(index, new Vm(idShift++, userId, 7.5 * sdm, pesNumber, ram, 85196800, size, "m3.large", p));
				break;
			case 2:
				list.set(index, new Vm(idShift++, userId, 15 * sdm, pesNumber, ram, 131072000, size, "m3.xlarge", p));
				break;
			case 3:
				list.set(index, new Vm(idShift++, userId, 30 * sdm, pesNumber, ram, 131072000, size, "m3.2xlarge", p));
				break;
			case 4:
				list.set(index, new Vm(idShift++, userId, 7.5 * sdm, pesNumber, ram, 35196800, size, "m4.large", p));
				break;
			case 5:
				list.set(index, new Vm(idShift++, userId, 15 * sdm, pesNumber, ram, 68196800, size, "m4.xlarge", p));
				break;
			case 6:
				list.set(index, new Vm(idShift++, userId, 30 * sdm, pesNumber, ram, 131072000, size, "m4.2xlarge", p));
				break;
			case 7:
				list.set(index, new Vm(idShift++, userId, 45 * sdm, pesNumber, ram, 18196800, size, "m4.4xlarge", p));
				break;
			}
		}
		return list;
	}

	@SuppressWarnings("unused")
	private static List<Vm> createEqualVms(int userId, int num) {
		int idShift = VM_ID_SHIFT;
		long size = 10000; // image size(MB)
		int ram = 1024; // vm memory (MB)
		int mips = 250;
		long bw = 1000;
		int pesNumber = 2; // number of cpus
		String vmm = "xen"; // VMM name

		LinkedList<Vm> list = new LinkedList<Vm>();
		for (int i = 0; i < num; i++) {
			list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size, vmm, new DAGCentralScheduler()));
		}

		return list;
	}

	// TODO entrance to modify VM configurations
	public static List<Vm> createVms(int userId) {
		// return createAWSVms(userId);
		return createEC2Vms(userId);
		// return createEqualVms(userId, 4);
	}

	public static List<Vm> createVms(int userId, int[] ins, int[] ins2type) {
		return createAWSVms(userId, ins, ins2type);
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
		// TODO add more prices
		ImmutableMap<String, Double> unit_price = ImmutableMap.<String, Double> builder().put("m1.small", 0.06) //
				.put("m3.medium", 0.067) //
				.put("m3.large", 0.133) //
				.put("m3.xlarge", 0.266) //
				.put("m3.2xlarge", 0.532) //
				.put("m4.large", 0.1) //
				.put("m4.xlarge", 0.2) //
				.put("m4.2xlarge", 0.4) //
				.put("m4.4xlarge", 0.8) //
				.build();

		double res = 0;
		for (Vm v : vmlist) {
			res += unit_price.get(v.getVmm());
			// System.out.println(v.getVmm());
		}
		// System.out.println(res);
		return res;

	}

	public static double getUnitPrice(Vm v) {
		ImmutableMap<String, Double> unit_price = ImmutableMap.<String, Double> builder().put("m1.small", 0.06) //
				.put("m3.medium", 0.067) //
				.put("m3.large", 0.133) //
				.put("m3.xlarge", 0.266) //
				.put("m3.2xlarge", 0.532) //
				.put("m4.large", 0.1) //
				.put("m4.xlarge", 0.2) //
				.put("m4.2xlarge", 0.4) //
				.put("m4.4xlarge", 0.8) //
				.build();

		return unit_price.get(v.getVmm());
	}

	public static int getAvailableVmTypes() {
		return 8; // in aws EC2
	}

	/**
	 * Creating data centers
	 * 
	 * @return
	 */
	public static Datacenter createDatacenter() {
		return Infrastructure.createDatacenter(10);
	}

	// TODO entrance to modify Data center configurations
	public static Datacenter createDatacenter(int reqVmNum) {
		String name = DATACENTER_NAME;
		List<Host> hostList = new ArrayList<Host>();

		// each machine are n-core machine
		List<Pe> peList = new ArrayList<Pe>();
		peList.add(new Pe(0, new PeProvisionerSimple(Integer.MAX_VALUE)));

		// create (same configured) PMs in a datacenter
		int hostId = 0;
		int ram = Integer.MAX_VALUE; // host memory (MB)
		long storage = Integer.MAX_VALUE; // host storage
		int bw = Integer.MAX_VALUE;

		for (int i = 0; i < reqVmNum; i++) {
			hostList.add(new Host(++hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList,
					new VmSchedulerSpaceShared(peList))); // This is our
															// machine
		}

		// data center characteristics
		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 1.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone,
				cost, costPerMem, costPerStorage, costPerBw);

		// Create a PowerDatacenter object.
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new StaticRandomVmAllocationPolicy(hostList),
					storageList, 1);
			// datacenter = new Datacenter(name, characteristics, new
			// StaticRandomVmAllocationPolicy(hostList),
			// storageList);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
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
			List<MyCloudlet> cloudletList = null;
			CloudletDAG workflow = null;

			switch (dataset) {
			case "fmri":
				FMRI fmri = new FMRI(brokerId, CLOUDLET_ID_SHIFT);
				cloudletList = fmri.getCloudletList();
				workflow = fmri.getCloudletPassport();
				break;
			case "eprotein":
				Eprotein eprotein = new Eprotein(brokerId, CLOUDLET_ID_SHIFT);
				cloudletList = eprotein.getCloudletList();
				workflow = eprotein.getCloudletPassport();
				break;
			case "random":
				Randomset rset = new Randomset(brokerId, CLOUDLET_ID_SHIFT, 10);
				cloudletList = rset.getCloudletList();
				workflow = rset.getCloudletPassport();
				break;
			case "j30_1":
			case "j30":
				PSPLIB psp1 = new PSPLIB(brokerId, CLOUDLET_ID_SHIFT, "j301_1", 30);
				cloudletList = psp1.getCloudletList();
				workflow = psp1.getCloudletPassport();
				break;
			case "j30_2":
				PSPLIB psp11 = new PSPLIB(brokerId, CLOUDLET_ID_SHIFT, "j301_2", 30);
				cloudletList = psp11.getCloudletList();
				workflow = psp11.getCloudletPassport();
				break;
			case "j60_1":
			case "j60":
				PSPLIB psp2 = new PSPLIB(brokerId, CLOUDLET_ID_SHIFT, "j601_1", 60);
				cloudletList = psp2.getCloudletList();
				workflow = psp2.getCloudletPassport();
				break;
			case "j60_2":
				PSPLIB psp22 = new PSPLIB(brokerId, CLOUDLET_ID_SHIFT, "j601_2", 60);
				cloudletList = psp22.getCloudletList();
				workflow = psp22.getCloudletPassport();
				break;
			case "j90_1":
			case "j90":
				PSPLIB psp3 = new PSPLIB(brokerId, CLOUDLET_ID_SHIFT, "j901_1", 90);
				cloudletList = psp3.getCloudletList();
				workflow = psp3.getCloudletPassport();
				break;
			case "j90_2":
				PSPLIB psp32 = new PSPLIB(brokerId, CLOUDLET_ID_SHIFT, "j901_2", 90);
				cloudletList = psp32.getCloudletList();
				workflow = psp32.getCloudletPassport();
				break;
			case "j120_1":
				PSPLIB psp4 = new PSPLIB(brokerId, CLOUDLET_ID_SHIFT, "j1201_1", 120);
				cloudletList = psp4.getCloudletList();
				workflow = psp4.getCloudletPassport();
				break;
			case "j120_2":
				PSPLIB psp42 = new PSPLIB(brokerId, CLOUDLET_ID_SHIFT, "j1201_2", 120);
				cloudletList = psp42.getCloudletList();
				workflow = psp42.getCloudletPassport();
				break;
			default:
				if (dataset.startsWith("sci_")) { // case Scientific Workflow
					PEGASUS sciF = new PEGASUS(brokerId, CLOUDLET_ID_SHIFT, dataset.substring(4));
					cloudletList = sciF.getCloudletList();
					workflow = sciF.getCloudletPassport();
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
