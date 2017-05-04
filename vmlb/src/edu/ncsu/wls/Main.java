package edu.ncsu.wls;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import edu.ncsu.datasets.Eprotein;

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
			// vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw,
			// size, vmm, new CloudletSchedulerTimeShared()); // Run the
			// cloudlet simulately inside one VM if possible
			vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm,
					new DAGCloudletSchedulerSpaceShared()); // Only allow one
															// running cloudlet
															// in one VM
			list.add(vm[i]);
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

		vmlist = createVMs(dcBrokerId, 2, 0);
		broker.submitVmList(vmlist);
		
		// Create cloudlets
//		cloudletList = Randomset.createCloudlet(broker.getId(), 10, 0);
//		CloudletPassport workflow = Randomset.getPassport(cloudletList);
		Eprotein eprotein = new Eprotein(broker.getId(), 0);
		cloudletList = eprotein.getCloudletList();
		CloudletPassport workflow = eprotein.getCloudletPassport();
		
		for (Vm vm : vmlist)
			((DAGCloudletSchedulerSpaceShared)(vm.getCloudletScheduler())).setCloudletPassport(workflow);
		Collections.shuffle(cloudletList);
		broker.submitCloudletList(cloudletList);

		// Start the simulation
		CloudSim.startSimulation();
		List<Cloudlet> newList = broker.getCloudletReceivedList();
		CloudSim.stopSimulation();

		MyCloudSimHelper.printCloudletList(newList);
		
		Log.printLine("Total Finish Cloudlets # " + newList.size());
	}
}