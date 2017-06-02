package edu.ncsu.wls;

import java.util.ArrayList;
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

import edu.ncsu.datasets.Eprotein;
import edu.ncsu.datasets.FMRI;
import edu.ncsu.datasets.PSPLIB;
import edu.ncsu.datasets.Randomset;

public class Infrastructure {
	public static final int VM_ID_SHIFT = 0;
	public static final int CLOUDLET_ID_SHIFT = 0;
	public static final String DATACENTER_NAME = "ncsu";
	public static final String[] models = new String[] { "fmri", "eprotein", "j30_1", "j30_2", "j60_1", "j60_2",
			"j90_1", "j90_2", "j120_1", "j120_2" };

	/**
	 * Creating Amazon EC2 virtual machines
	 * 
	 * @param userId
	 * @param vms
	 * @param idShift
	 * @return
	 */
	private static List<Vm> createEC2Vms(int userId) {
		int idShift = VM_ID_SHIFT;
		long size, bw;
		int ram, mips, pesNumber;
		String vmm;
		// Creates a container to store VMs. This list is passed to the broker
		// later
		LinkedList<Vm> list = new LinkedList<Vm>();

		// create VMs
		// VM Parameters
		size = 10000; // image size (MB)
		ram = 4096; // vm memory (MB)
		mips = 225;
		bw = 1000;
		pesNumber = 2; // number of cpus
		vmm = "t2.medium"; // VMM name
		list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size, vmm, new DAGCloudletSchedulerSpaceShared()));

		// =========================
		size = 8000; // image size (MB)
		ram = 4096; // vm memory (MB)
		mips = 150;
		bw = 1000;
		pesNumber = 1; // number of cpus
		vmm = "t2.small"; // VMM name
		list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size, vmm, new DAGCloudletSchedulerSpaceShared()));

		// =========================
		size = 8000; // image size (MB)
		ram = 4096; // vm memory (MB)
		mips = 150;
		bw = 1000;
		pesNumber = 1; // number of cpus
		vmm = "t2.small"; // VMM name
		list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size, vmm, new DAGCloudletSchedulerSpaceShared()));

		// =============
		size = 10000; // image size (MB)
		ram = 4 * 1024; // vm memory (MB)
		mips = 200;
		bw = 1500;
		pesNumber = 1; // number of cpus
		vmm = "m3.medium"; // VMM name
		list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size, vmm, new DAGCloudletSchedulerSpaceShared()));
		// list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size,
		// vmm, new DAGCloudletSchedulerSpaceShared()));

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
			list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size, vmm,
					new DAGCloudletSchedulerSpaceShared()));
		}

		return list;
	}

	// TODO entrance to modify VM configurations
	public static List<Vm> createVms(int userId) {
		return createEC2Vms(userId);
		// return createEqualVms(userId, 4);
	}

	// TODO entrance to modify Data center configurations
	public static Datacenter createDatacenter() {
		String name = DATACENTER_NAME;
		List<Host> hostList = new ArrayList<Host>();

		// each machine are n-core machine
		List<Pe> peList = new ArrayList<Pe>();
		peList.add(new Pe(0, new PeProvisionerSimple(500)));
		peList.add(new Pe(0, new PeProvisionerSimple(500)));

		// create (same configured) PMs in a datacenter
		int hostId = 0;
		int ram = 100000; // host memory (MB)
		long storage = 1000000; // host storage
		int bw = 10000;

		for (int i = 0; i < 5; ++i) {
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
	 * Generating the pre-defined study cases
	 * 
	 * @param dataset
	 * @param brokerId
	 * @param cloudleIdShift
	 * @param preferCloudLetNum4Random
	 *            useful ONLY when dataset="random"
	 * @return List<MyCloudlet> cloudletList + CloudletPassport workflow
	 */
	public static Object[] getCaseCloudlets(String dataset, int brokerId) {
		List<MyCloudlet> cloudletList = null;
		CloudletPassport workflow = null;
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
			System.err.println("Check the dataset name");
			System.exit(-1);
		}

		return new Object[] { cloudletList, workflow };
	}

}
