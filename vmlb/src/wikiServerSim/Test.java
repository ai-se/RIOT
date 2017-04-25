package wikiServerSim;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class Test {
	private static List<Cloudlet> cloudletList;
	private static List<Vm> vmlist;

	private static Datacenter createDatacenter(String name) {
		List<Host> hostList = new ArrayList<Host>();

		// each machine are n-core machine
		List<Pe> peList = new ArrayList<Pe>();
		peList.add(new Pe(0, new PeProvisionerSimple(1000)));
		peList.add(new Pe(1, new PeProvisionerSimple(1200)));
		// peList.add(new Pe(2, new PeProvisionerSimple(1200)));
		// peList.add(new Pe(3, new PeProvisionerSimple(800)));

		// create (same configured) PMs in a datacenter
		int hostId = 0;
		int ram = 2048; // host memory (MB)
		long storage = 1000000; // host storage
		int bw = 10000;

		for (int i = 0; i < 3; ++i) {
			hostList.add(new Host(++hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList,
					new VmSchedulerTimeShared(peList))); // This is our machine
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
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone,
				cost, costPerMem, costPerStorage, costPerBw);

		// Create a PowerDatacenter object.
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new StaticRandomVmAllocationPolicy(hostList),
					storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	private static DatacenterBroker createBroker() {
		DatacenterBroker broker = null;
		try {
			broker = new DatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent + "Time"
				+ indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.print("SUCCESS");

				Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId()
						+ indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent
						+ dft.format(cloudlet.getExecStartTime()) + indent + indent
						+ dft.format(cloudlet.getFinishTime()));
			}
		}
	}

	public static void main(String[] args) {
		// Initial the cloud simulator
		int num_user = 1; // number of cloud users
		Calendar calendar = Calendar.getInstance(); // Calendar whose fields
													// have been initialized
													// with the current date and
													// time.
		boolean trace_flag = false; // trace events
		CloudSim.init(num_user, calendar, trace_flag);

		// create the data center
		Datacenter wiki_dc1 = createDatacenter("wiki_testing");
		System.out.println("the id for wiki_dc is " + wiki_dc1.getId());

		// Create the broker
		DatacenterBroker broker = createBroker();
		int brokerId = broker.getId();

		// Create the virtual machine
		vmlist = new ArrayList<Vm>();

		// VM description
		int vmid = 0;
		int mips = 1000;
		long size = 10000; // image size (MB)
		int ram = 512; // vm memory (MB)
		long bw = 1000;
		int pesNumber = 1; // number of cpus
		String vmm = "Xen"; // VMM name

		// Create 3 VMs
		for (int i = 0; i < 5; ++i) {
			Vm vm = new Vm(vmid++, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			vmlist.add(vm);
		}
		broker.submitVmList(vmlist);

		// Create the workload, or Cloudlets
		cloudletList = new ArrayList<Cloudlet>();
		int id = 0;
		long length = 400000;
		long fileSize = 300;
		long outputSize = 300;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		for (int i = 0; i < 20; ++i) {
			Cloudlet cloudlet = new Cloudlet(id++, length, pesNumber, fileSize, outputSize, utilizationModel,
					utilizationModel, utilizationModel);
			cloudlet.setUserId(brokerId);
//			cloudlet.setVmId(ThreadLocalRandom.current().nextInt(0, 3));
//			cloudlet.setSubmissionTime(i*30000);
//			cloudlet.setExecStartTime(ThreadLocalRandom.current().nextInt(0, 100000));
//			cloudlet.setExecStartTime((30-i)*10000);
			cloudlet.setVmId(1);
			cloudletList.add(cloudlet);
		}

		broker.submitCloudletList(cloudletList);
		// Start the simulation
		CloudSim.startSimulation();
		CloudSim.stopSimulation();

		List<Cloudlet> newList = broker.getCloudletReceivedList();
		printCloudletList(newList);
	}
}
