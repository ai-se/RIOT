package edu.ncsu.wls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Consts;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

/**
 * Dynamic adding cloudlet is NOT supported at this time.
 * 
 * @author jianfeng
 *
 */

class SingleVmInfo {
	public Vm v;
	public double mips;
	public List<Task> waitingList;
	public List<Task> finishList;
	public Task executing;

	public SingleVmInfo(Vm v) {
		this.v = v;
		this.mips = v.getMips();
		waitingList = new ArrayList<Task>();
		finishList = new ArrayList<Task>();
	}

	public void appendWL(Task rcl) {
		waitingList.add(rcl);
	}

	public boolean hasWaitings() {
		return !this.waitingList.isEmpty();
	}
}

public class DAGCentralSimulator {
	private DAG dag;
	private List<Vm> vmlist;
	private TreeSet<Double> clock;
	private Map<Vm, SingleVmInfo> vmInfos;
	private int submittedNum;
	private int finishedNum;
	private double currentTime;

	public DAGCentralSimulator() {
		clock = new TreeSet<Double>();
		vmInfos = new HashMap<Vm, SingleVmInfo>();
		submittedNum = 0;
		finishedNum = 0;
	}

	public void setCloudletPassport(DAG cp) {
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

	/**=
	 * We are now supporting SUBMITTED-IN-ORDER ~~
	 * 
	 * @param cloudlet
	 * @param fileTransferTime
	 * @return
	 */
	public void taskSubmit(Task cloudlet, double fileTransferTime) {
		Vm myVm = getVmById(cloudlet.getVmId());

		// updating the file transfer time
		fileTransferTime = dag.getFileTransferTime(cloudlet);
		double extraSize = myVm.getMips() * fileTransferTime;
		long length = cloudlet.getCloudletLength();
		length += extraSize;
		cloudlet.setCloudletLength(length);

		this.vmInfos.get(myVm).appendWL(cloudlet);
		Log.printLine(String.format("[----] SUBMIT %s to VM # %d", cloudlet, cloudlet.getVmId()));
		cloudlet.setCloudletStatus(Cloudlet.READY, -1); // -1 not used
		
		submittedNum += 1;
	}

	private void taskFinish(Task task) {
		double nowClock = currentTime;
		Log.printLine(String.format("[%.2f] FINISH %s", nowClock, task));
		task.setCloudletStatus(Cloudlet.SUCCESS, nowClock);
		this.dag.afterOneCloudletSuccess(task);
		finishedNum += 1;
	}

	private void taskStarts(Task task, double nowClock) {
		Log.printLine(String.format("[%.2f] START %s running at VM # %d", nowClock, task, task.getVmId()));
		task.setExecStartTime(nowClock);
	}

	public boolean boot() {
		if (this.submittedNum != dag.totalCloudletNum)
			return false;

		currentTime = CloudSim.getMinTimeBetweenEvents();
		double previousTime = 0;
		while (finishedNum < submittedNum) {
			// step 1-1 updating executing
			double timespan = currentTime - previousTime;
			for (Vm v : vmlist) {
				SingleVmInfo vinfo = vmInfos.get(v);

				// Updating executing cloudlet
				if (vinfo.executing != null) {
					vinfo.executing.setCloudletFinishedSoFar((long) (vinfo.mips * timespan * Consts.MILLION));
					if (vinfo.executing.getRemainingCloudletLength() == 0) { // finish
						taskFinish(vinfo.executing);
						vinfo.finishList.add(vinfo.executing);
						vinfo.executing = null;
					} // if execing done
				} // if has exec cloudlet
			}

			// step 1-2 add new cloudlet to exec if free
			for (Vm v : vmlist) {
				SingleVmInfo vinfo = vmInfos.get(v);

				if (vinfo.executing != null) // not free :(
					continue;

				// No waiting cloudlets
				if (!vinfo.hasWaitings())
					continue;

				// try to fetch a new cloudlet
				for (Task task : vinfo.waitingList) {
					
					if (dag.isCloudletPrepared(task)) {
						vinfo.executing = task;
						taskStarts(task, currentTime);
						break;
					} // if rcl ready
				} // for rcl in WL

				// find a new cloudlet to exec. rm it from WL
				if (vinfo.executing != null) {
					vinfo.waitingList.remove(vinfo.executing);
					clock.add(currentTime + vinfo.executing.getCloudletLength() / vinfo.mips);
				}
			} // for v

			previousTime = currentTime;
			Double k = clock.higher(currentTime);

			if (k != null)
				currentTime = k;
			else{
				currentTime = currentTime + CloudSim.getMinTimeBetweenEvents();
			}
		}

		return true;
	}

}
