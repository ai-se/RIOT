package edu.ncsu.wls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Consts;
import org.cloudbus.cloudsim.ResCloudlet;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

/**
 * DAGCloudletSchedulerSpaceShared implements a policy of scheduling performed
 * by a virtual machine to run its {@link Cloudlet Cloudlets}. It considers
 * there will be only one cloudlet per VM. Other cloudlets will be in a waiting
 * list. We consider that file transfer from cloudlets waiting happens before
 * cloudlet execution. I.e., even though cloudlets must wait for CPU, data
 * transfer happens as soon as cloudlets are submitted.
 * 
 * DAG - the cloudlet is organized as the DAG. If one cloudlet is not ready, it
 * will process the next available cloudlet
 * 
 * The DAG is creating through method addCloudWorkflow
 * 
 * Dynamic adding cloudlet is NOT supported at this time.
 * 
 * @author Jianfeng Chen
 */

class SingleVmInfo {
	public Vm v;
	public double mips;
	public List<ResCloudlet> waitingList;
	public List<ResCloudlet> finishList;
	public ResCloudlet executing;

	public SingleVmInfo(Vm v) {
		this.v = v;
		this.mips = v.getMips();
		waitingList = new ArrayList<ResCloudlet>();
		finishList = new ArrayList<ResCloudlet>();
	}

	public void appendWL(ResCloudlet rcl) {
		waitingList.add(rcl);
	}

	public boolean hasWaitings() {
		return !this.waitingList.isEmpty();
	}
}

public class DAGCentralScheduler extends CloudletScheduler {
	private CloudletDAG dag;
	private List<Vm> vmlist;
	private TreeSet<Double> clock;
	private double lstUpdateClock;
	private Map<Vm, SingleVmInfo> vmInfos;

	private int usedPes; // not used
	private int currentCpus; // not used
	private int submittedCloudletNum;

	/**
	 * ® Creates a new CloudletSchedulerSpaceShared object. This method must be
	 * invoked before starting the actual simulation.
	 * 
	 * @pre $none
	 * @post $none
	 */
	public DAGCentralScheduler() { // this will exec at every evaluation.
		super();
		clock = new TreeSet<Double>();
		lstUpdateClock = -1.0;
		vmInfos = new HashMap<Vm, SingleVmInfo>();
		submittedCloudletNum = 0;
	}

	public void setCloudletPassport(CloudletDAG cp) {
		this.dag = cp;
	}

	public void setVmList(List<Vm> list) {
		this.vmlist = list;
		for (Vm v : vmlist)
			vmInfos.put(v, new SingleVmInfo(v));
	}

	private Vm getVmById(int id) {
		for (Vm v : vmlist)
			if (v.getId() == id)
				return v;
		return null;
	}

	@Override
	public double updateVmProcessing(double currentTime, List<Double> mipsShare) {
		// case 0 if submission not done. waiting
		if (submittedCloudletNum != dag.totalCloudletNum)
			return currentTime;

		// case -1 such currentTime clock has been check in another processing
		// call
		if (currentTime <= lstUpdateClock) {
			Double k = clock.higher(currentTime);
			if (k != null)
				return clock.higher(currentTime);
			else
				return 0.0;
		}

		lstUpdateClock = currentTime;

		// case 1 checking whether all finished
		int done = 0;
		for (Vm v : vmlist) {
			SingleVmInfo vmInfo = this.vmInfos.get(v);
			done += vmInfo.finishList.size();
		} // for v
		if (done == dag.totalCloudletNum)
			return 0.0;

		// case 2 processing
		// step 2-1 updating executing
		double timespan = currentTime - getPreviousTime();
		for (Vm v : vmlist) {
			SingleVmInfo vinfo = vmInfos.get(v);

			// Updating executing cloudlet
			if (vinfo.executing != null) {
				vinfo.executing.updateCloudletFinishedSoFar((long) (vinfo.mips * timespan * Consts.MILLION));
				if (vinfo.executing.getRemainingCloudletLength() == 0) { // finish
																			// exec
																			// current
																			// cloudlet
					cloudletFinish(vinfo.executing);
					vinfo.finishList.add(vinfo.executing);
					vinfo.executing = null;
				} // if execing done
			} // if has exec cloudlet
		}

		// step 2-2 add new cloudlet to exec if free
		for (Vm v : vmlist) {
			SingleVmInfo vinfo = vmInfos.get(v);

			if (vinfo.executing != null) // not free :(
				continue;

			// No waiting cloudlets
			if (!vinfo.hasWaitings())
				continue;

			// try to fetch a new cloudlet
			for (ResCloudlet rcl : vinfo.waitingList) {
				if (dag.isCloudletPrepared(rcl.getCloudlet())) {
					vinfo.executing = rcl;
					rcl.setCloudletStatus(Cloudlet.INEXEC);
					break;
				} // if rcl ready
			} // for rcl in WL

			// find a new cloudlet to exec. rm it from WL
			if (vinfo.executing != null) {
				vinfo.waitingList.remove(vinfo.executing);
				clock.add(currentTime + vinfo.executing.getCloudletLength() / vinfo.mips);
			}
		} // for v

		setPreviousTime(currentTime);

		Double k = clock.higher(currentTime);
		if (k != null)
			return k;
		else {
			return currentTime + CloudSim.getMinTimeBetweenEvents();
		}
	}

	@Override
	public void cloudletFinish(ResCloudlet rcl) {
		rcl.setCloudletStatus(Cloudlet.SUCCESS);
		rcl.finalizeCloudlet();
		getCloudletFinishedList().add(rcl);
		this.dag.afterOneCloudletSuccess(rcl.getCloudlet());
		// System.out.println("Done " + rcl.getCloudlet());
	}

	@Override
	// NOTE: parameter fileTransferTime will be ignored here
	public double cloudletSubmit(Cloudlet cloudlet, double fileTransferTime) {
		Vm myVm = getVmById(cloudlet.getVmId());

		// updating the file transfer time
		fileTransferTime = dag.getFileTransferTime(cloudlet);
		double extraSize = myVm.getMips() * fileTransferTime;
		long length = cloudlet.getCloudletLength();
		length += extraSize;
		cloudlet.setCloudletLength(length);

		// for convenience, always put to waiting list
		ResCloudlet rcl = new ResCloudlet(cloudlet);
		this.vmInfos.get(myVm).appendWL(rcl);
		// rcl.setCloudletStatus(Cloudlet.QUEUED);
		// getCloudletWaitingList().add(rcl);

		submittedCloudletNum += 1;
		// System.out.println("submitting " + cloudlet);
		return 0.0;
	}

	@Override
	public double cloudletSubmit(Cloudlet cloudlet) {
		return cloudletSubmit(cloudlet, 0.0);
	}

	@Override
	public int getCloudletStatus(int cloudletId) {
		for (ResCloudlet rcl : getCloudletExecList()) {
			if (rcl.getCloudletId() == cloudletId) {
				return rcl.getCloudletStatus();
			}
		}

		for (ResCloudlet rcl : getCloudletPausedList()) {
			if (rcl.getCloudletId() == cloudletId) {
				return rcl.getCloudletStatus();
			}
		}

		for (ResCloudlet rcl : getCloudletWaitingList()) {
			if (rcl.getCloudletId() == cloudletId) {
				return rcl.getCloudletStatus();
			}
		}

		for (ResCloudlet rcl : getCloudletFinishedList()) {
			if (rcl.getCloudletId() == cloudletId)
				return rcl.getCloudletStatus();
		}

		return -1;
	}

	@Override
	public double getTotalUtilizationOfCpu(double time) {
		double totalUtilization = 0;
		for (ResCloudlet gl : getCloudletExecList()) {
			totalUtilization += gl.getCloudlet().getUtilizationOfCpu(time);
		}
		return totalUtilization;
	}

	@Override
	public boolean isFinishedCloudlets() {
		return getCloudletFinishedList().size() > 0;
	}

	@Override
	public Cloudlet getNextFinishedCloudlet() {
		if (getCloudletFinishedList().size() > 0) {
			return getCloudletFinishedList().remove(0).getCloudlet();
		}
		return null;
	}

	@Override
	public int runningCloudlets() {
		return getCloudletExecList().size();
	}

	/**
	 * Returns the first cloudlet to migrate to another VM.
	 * 
	 * @return the first running cloudlet
	 * @pre $none
	 * @post $none
	 * 
	 * @todo it doesn't check if the list is empty
	 */
	@Override
	public Cloudlet migrateCloudlet() {
		ResCloudlet rcl = getCloudletExecList().remove(0);
		rcl.finalizeCloudlet();
		Cloudlet cl = rcl.getCloudlet();
		usedPes -= cl.getNumberOfPes();
		return cl;
	}

	@Override
	public List<Double> getCurrentRequestedMips() {
		List<Double> mipsShare = new ArrayList<Double>();
		if (getCurrentMipsShare() != null) {
			for (Double mips : getCurrentMipsShare()) {
				mipsShare.add(mips);
			}
		}
		return mipsShare;
	}

	@Override
	public double getTotalCurrentAvailableMipsForCloudlet(ResCloudlet rcl, List<Double> mipsShare) {
		/* @todo The param rcl is not being used. */
		double capacity = 0.0;
		int cpus = 0;
		for (Double mips : mipsShare) { // count the cpus available to the vmm
			capacity += mips;
			if (mips > 0) {
				cpus++;
			}
		}
		currentCpus = cpus;
		capacity /= cpus; // average capacity of each cpu
		return capacity;
	}

	@Override
	public double getTotalCurrentAllocatedMipsForCloudlet(ResCloudlet rcl, double time) {
		// @todo the method isn't in fact implemented
		// TODO Auto-generated method stub
		return 0.0;
	}

	@Override
	public double getTotalCurrentRequestedMipsForCloudlet(ResCloudlet rcl, double time) {
		// @todo the method isn't in fact implemented
		// TODO Auto-generated method stub
		return 0.0;
	}

	@Override
	public double getCurrentRequestedUtilizationOfRam() {
		// @todo the method isn't in fact implemented
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getCurrentRequestedUtilizationOfBw() {
		// @todo the method isn't in fact implemented
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Cloudlet cloudletCancel(int cloudletId) {
		System.out.println("canceling C" + cloudletId);
		// First, looks in the finished queue
		for (ResCloudlet rcl : getCloudletFinishedList()) {
			if (rcl.getCloudletId() == cloudletId) {
				getCloudletFinishedList().remove(rcl);
				return rcl.getCloudlet();
			}
		}

		// Then searches in the exec list
		for (ResCloudlet rcl : getCloudletExecList()) {
			if (rcl.getCloudletId() == cloudletId) {
				getCloudletExecList().remove(rcl);
				if (rcl.getRemainingCloudletLength() == 0) {
					cloudletFinish(rcl);
				} else {
					rcl.setCloudletStatus(Cloudlet.CANCELED);
				}
				return rcl.getCloudlet();
			}
		}

		// Now, looks in the paused queue
		for (ResCloudlet rcl : getCloudletPausedList()) {
			if (rcl.getCloudletId() == cloudletId) {
				getCloudletPausedList().remove(rcl);
				return rcl.getCloudlet();
			}
		}

		// Finally, looks in the waiting list
		for (ResCloudlet rcl : getCloudletWaitingList()) {
			if (rcl.getCloudletId() == cloudletId) {
				rcl.setCloudletStatus(Cloudlet.CANCELED);
				getCloudletWaitingList().remove(rcl);
				return rcl.getCloudlet();
			}
		}

		return null;

	}

	@Override
	public boolean cloudletPause(int cloudletId) {
		System.out.println("Pause");
		boolean found = false;
		int position = 0;

		// first, looks for the cloudlet in the exec list
		for (ResCloudlet rcl : getCloudletExecList()) {
			if (rcl.getCloudletId() == cloudletId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			// moves to the paused list
			ResCloudlet rgl = getCloudletExecList().remove(position);
			if (rgl.getRemainingCloudletLength() == 0) {
				cloudletFinish(rgl);
			} else {
				rgl.setCloudletStatus(Cloudlet.PAUSED);
				getCloudletPausedList().add(rgl);
			}
			return true;

		}

		// now, look for the cloudlet in the waiting list
		position = 0;
		found = false;
		for (ResCloudlet rcl : getCloudletWaitingList()) {
			if (rcl.getCloudletId() == cloudletId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			// moves to the paused list
			ResCloudlet rgl = getCloudletWaitingList().remove(position);
			if (rgl.getRemainingCloudletLength() == 0) {
				cloudletFinish(rgl);
			} else {
				rgl.setCloudletStatus(Cloudlet.PAUSED);
				getCloudletPausedList().add(rgl);
			}
			return true;

		}

		return false;
	}

	@Override
	public double cloudletResume(int cloudletId) {
		boolean found = false;
		int position = 0;
		// look for the cloudlet in the paused list
		for (ResCloudlet rcl : getCloudletPausedList()) {
			if (rcl.getCloudletId() == cloudletId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			ResCloudlet rcl = getCloudletPausedList().remove(position);

			// it can go to the exec list
			if ((currentCpus - usedPes) >= rcl.getNumberOfPes()) {
				rcl.setCloudletStatus(Cloudlet.INEXEC);
				for (int i = 0; i < rcl.getNumberOfPes(); i++) {
					rcl.setMachineAndPeId(0, i);
				}

				long size = rcl.getRemainingCloudletLength();
				size *= rcl.getNumberOfPes();
				rcl.getCloudlet().setCloudletLength(size);

				getCloudletExecList().add(rcl);
				usedPes += rcl.getNumberOfPes();

				// calculate the expected time for cloudlet completion
				double capacity = 0.0;
				int cpus = 0;
				for (Double mips : getCurrentMipsShare()) {
					capacity += mips;
					if (mips > 0) {
						cpus++;
					}
				}
				currentCpus = cpus;
				capacity /= cpus;

				long remainingLength = rcl.getRemainingCloudletLength();
				double estimatedFinishTime = CloudSim.clock() + (remainingLength / (capacity * rcl.getNumberOfPes()));

				return estimatedFinishTime;
			} else {// no enough free PEs: go to the waiting queue
				rcl.setCloudletStatus(Cloudlet.QUEUED);

				long size = rcl.getRemainingCloudletLength();
				size *= rcl.getNumberOfPes();
				rcl.getCloudlet().setCloudletLength(size);

				getCloudletWaitingList().add(rcl);
				return 0.0;
			}

		}

		// not found in the paused list: either it is in in the queue, executing
		// or not exist
		return 0.0;

	}

}
