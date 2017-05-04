package edu.ncsu.wls;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;

public class OnlineDatacenterBroker extends DatacenterBroker {
	private int vmIndex = 0; // rotating the vm to put cloudlets into
	// private HashMap<Cloudlet, List<Cloudlet>> requiring = new
	// HashMap<Cloudlet, List<Cloudlet>>();

	public OnlineDatacenterBroker(String name) throws Exception {
		super(name);
	}

	/**
	 * Processes events available for this Broker.
	 * 
	 * @param ev
	 *            a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		// Resource characteristics request
		case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
			processResourceCharacteristicsRequest(ev);
			break;
		// Resource characteristics answer
		case CloudSimTags.RESOURCE_CHARACTERISTICS:
			processResourceCharacteristics(ev);
			break;
		// VM Creation answer
		case CloudSimTags.VM_CREATE_ACK:
			processVmCreate(ev);
			break;
		// A finished cloudlet returned
		case CloudSimTags.CLOUDLET_RETURN:
			processCloudletReturn(ev);
			break;
		// if the simulation finishes
		case CloudSimTags.END_OF_SIMULATION:
			shutdownEntity();
			break;
		// Cloudlet creation
		case CloudSimTags.CLOUDLET_SUBMIT:
			submitCloudlet();
			break;
		case CloudSimTags.VM_MIGRATE:
			// processVmMigrate(ev);
			System.out.println("debuging message... vm migration...");
			break;
		// other unknown tags are processed by this method
		default:
			processOtherEvent(ev);
			break;
		}
	}
	//
	// protected void processVmMigrate(SimEvent ev) {
	// send(this.getId(), CloudSimTags.VM_MIGRATE)
	// }

	/**
	 * Process the ack received due to a request for VM creation.
	 * 
	 * @param ev
	 *            a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	@Override
	protected void processVmCreate(SimEvent ev) {
		int[] data = (int[]) ev.getData();
		int datacenterId = data[0];
		int vmId = data[1];
		int result = data[2];

		if (result == CloudSimTags.TRUE) {
			getVmsToDatacentersMap().put(vmId, datacenterId);
			getVmsCreatedList().add(VmList.getById(getVmList(), vmId));
			Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vmId + " has been created in Datacenter #"
					+ datacenterId + ", Host #" + VmList.getById(getVmsCreatedList(), vmId).getHost().getId());
		} else {
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Creation of VM #" + vmId + " failed in Datacenter #"
					+ datacenterId);
		}

		incrementVmsAcks();

		// all the requested VMs have been created
		if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) {
			submitCloudlets();
			createCloudlets();
		} else {
			// all the acks received, but some VMs were not created
			if (getVmsRequested() == getVmsAcks()) {
				// find id of the next datacenter that has not been tried
				for (int nextDatacenterId : getDatacenterIdsList()) {
					if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
						createVmsInDatacenter(nextDatacenterId);
						return;
					}
				}

				// all datacenters already queried
				if (getVmsCreatedList().size() > 0) { // if some vm were created
					submitCloudlets();
					createCloudlets();
				} else { // no vms created. abort
					Log.printLine(CloudSim.clock() + ": " + getName()
							+ ": none of the required VMs could be created. Aborting");
					finishExecution();
				}
			}
		}
	}

	private void createCloudlets() {
		for (Cloudlet cloudlet : getCloudletList()) {
			MyCloudlet myCloudlet = (MyCloudlet) cloudlet;
			send(getId(), myCloudlet.getStartTime(), CloudSimTags.CLOUDLET_SUBMIT, 1);
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Create cloudlet " + cloudlet.getCloudletId()
					+ " at : " + myCloudlet.getStartTime());
		}
	}

	private void submitCloudlet() {
		Cloudlet cloudlet = getCloudletList().get(0);
		Vm vm;
		vm = getVmsCreatedList().get(vmIndex);
		Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet " + cloudlet.getCloudletId()
				+ " to VM #" + vm.getId());

		if (cloudlet.getVmId() < 0) // using the rotation strategy only when VM
									// id not assigned
			cloudlet.setVmId(vmIndex);
		sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
		vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
		// remove submitted cloudlet from waiting list
		getCloudletList().remove(cloudlet);
	}

	@Override
	protected void submitCloudlets() {
		int vmIndex = 0;
		Vm vm;
		for (Cloudlet cloudlet : getCloudletList()) {
			MyCloudlet cloudlet1 = (MyCloudlet) cloudlet;
			if (cloudlet1.getStartTime() == 0) {
				// if user didn't bind this cloudlet and it has not been
				// executed yet
				if (cloudlet.getVmId() == -1) {
					vm = getVmsCreatedList().get(vmIndex);
				} else { // submit to the specific vm
					vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
					if (vm == null) { // vm was not created
						Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
								+ cloudlet.getCloudletId() + ": bount VM not available");
						continue;
					}
				}

				Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet " + cloudlet.getCloudletId()
						+ " to VM #" + vm.getId());
				cloudlet.setVmId(vm.getId());
				sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
				cloudletsSubmitted++;
				vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
				getCloudletSubmittedList().add(cloudlet);
			} else {
				continue;
			}
		}

		// remove submitted cloudlets from waiting list
		for (Cloudlet cloudlet : getCloudletSubmittedList()) {
			getCloudletList().remove(cloudlet);
		}
	}

	@Override
	protected void processCloudletReturn(SimEvent ev) {
		Cloudlet cloudlet = (Cloudlet) ev.getData();
		getCloudletReceivedList().add(cloudlet);
		Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId() + " received");
		cloudletsSubmitted--;
		if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // all
																		// cloudlets
																		// executed
			Log.printLine(CloudSim.clock() + ": " + getName() + ": All Cloudlets executed. Finishing...");
			clearDatacenters();
			finishExecution();
		} else { // some cloudlets haven't finished yet
			if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {
				// all the cloudlets sent finished. It means that some bount
				// cloudlet is waiting its VM be created
				// clearDatacenters();
				// createVmsInDatacenter(0);
			}

		}
	}
}