package edu.ncsu.wls;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class Main {
	private static List<MyCloudlet> cloudletList;
	private static List<Vm> vmlist;

	private static Datacenter createDatacenter(String name) {
		List<Host> hostList = new ArrayList<Host>();

		// each machine are n-core machine
		List<Pe> peList = new ArrayList<Pe>();
		peList.add(new Pe(0, new PeProvisionerSimple(1000)));
		// peList.add(new Pe(1, new PeProvisionerSimple(1200)));

		// create (same configured) PMs in a datacenter
		int hostId = 0;
		int ram = 2048; // host memory (MB)
		long storage = 1000000; // host storage
		int bw = 10000;

		for (int i = 0; i < 100; ++i) {
			// hostList.add(new PowerHost(hostId++, new
			// RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage,
			// peList, new VmSchedulerTimeShared(peList), new
			// PowerModelCubic(1000, 1)));

			hostList.add(new Host(++hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList,
					new VmSchedulerTimeShared(peList))); // This is our
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

	private static List<Vm> createVMs(int userId, int vms, int idShift) {
		// Creates a container to store VMs. This list is passed to the broker
		// later
		LinkedList<Vm> list = new LinkedList<Vm>();

		// VM Parameters
		long size = 10000; // image size (MB)
		int ram = 512; // vm memory (MB)
		int mips = 250;
		long bw = 1000;
		int pesNumber = 1; // number of cpus
		String vmm = "Xen"; // VMM name

		// create VMs
		Vm[] vm = new Vm[vms];

		for (int i = 0; i < vms; i++) {
			vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			list.add(vm[i]);
		}

		return list;
	}

	private static List<MyCloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
		// Creates a container to store Cloudlets
		LinkedList<MyCloudlet> list = new LinkedList<MyCloudlet>();

		// cloudlet parameters
		long length = 40000;
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		MyCloudlet[] cloudlet = new MyCloudlet[cloudlets];

		for (int i = 0; i < cloudlets; i++) {
			cloudlet[i] = new MyCloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel,
					utilizationModel, utilizationModel, ThreadLocalRandom.current().nextInt(0, 1));
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			cloudlet[i].setVmId(ThreadLocalRandom.current().nextInt(0,100));
			list.add(cloudlet[i]);
		}

		return list;
	}

	public static void main(String[] args) {
		// Initial the cloud simulator
		int num_user = 1; // number of cloud users
		Calendar calendar = Calendar.getInstance();
		boolean trace_flag = false; // trace events
		CloudSim.init(num_user, calendar, trace_flag);

		// create the data center
		createDatacenter("WIKI"); 

		// Create the broker
		OnlineDatacenterBroker broker = null;
		try {
			broker = new OnlineDatacenterBroker("dc_broker");
		} catch (Exception e) {
			e.printStackTrace();
		}

		int dcBrokerId = broker.getId();
		// Create the virtual machine
		vmlist = new ArrayList<Vm>();

		vmlist = createVMs(dcBrokerId, 100, 0);
		broker.submitVmList(vmlist);

		// Create cloudlets
		cloudletList = createCloudlet(broker.getId(), 100, 0);

		broker.submitCloudletList(cloudletList);

		// this is a testing
		// HashMap<String, Object> migrationData = new HashMap<String,
		// Object>();
		// // sendNow(broker.getId(), CloudSimTags.VM_MIGRATE, migrationData);
		// PowerDatacenter x = null;

		// Start the simulation
		CloudSim.startSimulation();
		List<Cloudlet> newList = broker.getCloudletReceivedList();
		CloudSim.stopSimulation();

		MyCloudSimHelper.printCloudletList(newList.subList(newList.size()-3, (newList.size()-1)));
	}
}