package edu.ncsu.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Vm;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * 
 * @author jianfeng
 *
 *         CloudletPassport is used for controlling global working flow of
 *         cloudlets Used as a member in any CloudletScheduler
 * 
 */

public class DAG {
	private HashMap<Task, List<Task>> requiring = new HashMap<Task, List<Task>>();
	private HashMap<Task, List<Task>> contributeTo = new HashMap<Task, List<Task>>();
	private Table<Task, Task, Long> files = HashBasedTable.create(); // from,to,file-size
	public HashMap<Task, Double> fileTransferTime = new HashMap<Task, Double>();
	private int defultTotalCloudletNum;
	public int totalCloudletNum = 0;

	// ready negative-not ready 0-ready
	private int[] ready = new int[1200]; // TODO assume 1200 cloudlets at max
	private int[] totalNeed = new int[1200];

	public void rmCache() {
		totalCloudletNum = defultTotalCloudletNum;
		System.arraycopy(totalNeed, 0, ready, 0, defultTotalCloudletNum);

	}

	public List<Task> randTopo(List<Task> tasks, Random rand) {
		// temporary saving ready list
		int[] rSave = new int[defultTotalCloudletNum];
		System.arraycopy(ready, 0, rSave, 0, defultTotalCloudletNum);
		System.arraycopy(totalNeed, 0, ready, 0, defultTotalCloudletNum);

		List<Task> res = new ArrayList<Task>();
		while (res.size() < tasks.size()) {
			List<Task> nexts = new ArrayList<Task>();
			for (Task t : tasks)
				if (isCloudletPrepared(t) && !res.contains(t))
					nexts.add(t);
			Collections.shuffle(nexts, rand);
			res.add(nexts.get(0));
			afterOneCloudletSuccess(nexts.get(0));
		}

		// recovery ready to outside environment
		System.arraycopy(rSave, 0, ready, 0, defultTotalCloudletNum);
		return res;
	}

	public void addCloudWorkflow(Task from, Task to) {
		if (!this.requiring.containsKey(to))
			this.requiring.put(to, new ArrayList<Task>());
		this.requiring.get(to).add(from);

		if (!this.contributeTo.containsKey(from))
			this.contributeTo.put(from, new ArrayList<Task>());
		this.contributeTo.get(from).add(to);

		int index = to.getCloudletId();
		ready[index] += 1;
		totalNeed[index] += 1;
	}

	public boolean isEdge(Task from, Task to) {
		if (requiring.containsKey(to))
			return requiring.get(to).contains(from);

		return false;
	}

	public void setFilesBetween(Task from, Task to, long fileSize) {
		files.put(from, to, fileSize);
	}

	public synchronized boolean isCloudletPrepared(Task cloudlet) {
		int index = cloudlet.getCloudletId();
		return ready[index] == 0;
	}

	public synchronized void afterOneCloudletSuccess(Task cloudlet) {
		if (contributeTo.containsKey(cloudlet))
			for (Task p : contributeTo.get(cloudlet)) {
				int pindex = p.getCloudletId();
				ready[pindex] -= 1;
			}
	}

	public void setCloudletNum(int totalCloudletNum) {
		this.totalCloudletNum = totalCloudletNum;
		defultTotalCloudletNum = totalCloudletNum;
	}

	public void clearFileTransferTimes(List<Task> cList) {
		this.fileTransferTime.clear();
		for (Task c : cList)
			this.fileTransferTime.put(c, 0.0);
	}

	/**
	 * Calculating/ pre-calc the required file-transfer times for each cloudlet
	 * Only when all files finished transfer, is the cloudlet be done.
	 * 
	 * @param task2ins
	 * @param ins2type
	 * @param vmlist
	 */
	public void calcFileTransferTimes(int[] task2ins, List<Vm> vmlist, List<Task> cList) {
		this.clearFileTransferTimes(cList);

		for (Task target : requiring.keySet()) {
			for (Task src : requiring.get(target)) {
				int s = cList.indexOf(src);
				int t = cList.indexOf(target);
				if (task2ins[s] != task2ins[t] && files.contains(src, target)) {
					long bw = Long.min(vmlist.get(task2ins[s]).getBw(), vmlist.get(task2ins[t]).getBw());
					double time = fileTransferTime.get(src) + files.get(src, target) / bw;
					fileTransferTime.put(src, time);
				} // if type not equal and have file to transfer
			} // for target
		} // for src
	}

	public boolean hasPred(Task x) {
		if (requiring.containsKey(x) && requiring.get(x).size() > 0)
			return true;
		return false;
	}

	public boolean hasSucc(Task x) {
		if (contributeTo.containsKey(x) && contributeTo.get(x).size() > 0)
			return true;
		return false;
	}

	public HashMap<Task, List<Task>> getRequiring() {
		return requiring;
	}

	public List<Task> meRequires(Task me) {
		if (!requiring.containsKey(me))
			return new ArrayList<Task>();

		return requiring.get(me);
	}

	public List<Task> meContributeTo(Task me) {
		if (!contributeTo.containsKey(me))
			return new ArrayList<Task>();

		return contributeTo.get(me);
	}

	public String toString() {
		String r = "FLOW\n";
		for (Task to : this.requiring.keySet()) {
			for (Task from : this.requiring.get(to))
				r += (from) + "|";
			r += "-> " + to + "\n";
		}

		return r;
	}
}
