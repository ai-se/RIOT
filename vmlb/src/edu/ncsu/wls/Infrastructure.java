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

public class Infrastructure {
	/**
	 * Creating Amazon EC2 virtual machines
	 * 
	 * @param userId
	 * @param vms
	 * @param idShift
	 * @return
	 */
	public static List<Vm> createEC2Vms(int userId, int idShift) {
		long size, bw;
		int ram, mips, pesNumber;
		// Creates a container to store VMs. This list is passed to the broker
		// later
		LinkedList<Vm> list = new LinkedList<Vm>();

		// create VMs
		// VM Parameters
		size = 10000; // image size (MB)
		ram = 4096; // vm memory (MB)
		mips = 250;
		bw = 1000;
		pesNumber = 2; // number of cpus
		String vmm = "t2.medium"; // VMM name
		list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size, vmm, new DAGCloudletSchedulerSpaceShared()));

		// =============
		size = 10000; // image size (MB)
		ram = 4 * 1024; // vm memory (MB)
		mips = 200;
		bw = 1500;
		pesNumber = 1; // number of cpus
		vmm = "m3.medium"; // VMM name
		list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size, vmm, new DAGCloudletSchedulerSpaceShared()));
//		list.add(new Vm(idShift++, userId, mips, pesNumber, ram, bw, size, vmm, new DAGCloudletSchedulerSpaceShared()));

		return list;
	}

	public static List<Vm> createEqualVms(int userId, int num, int idShift) {
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

	public static Datacenter createDatacenter(String name) {
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
}
