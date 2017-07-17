package edu.ncsu.wls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

//import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

/**
 * 
 * @author jianfeng
 *
 *         CloudletPassport is used for controlling global workingflow of
 *         cloudlets Used as a member in any CloudletScheduler
 * 
 */

public class DAG {
	private HashMap<Task, List<Task>> requiring = new HashMap<Task, List<Task>>();
	private HashMap<Task, List<Task>> contributeTo = new HashMap<Task, List<Task>>();
	private HashMap<Task, HashMap<Task, Long>> files = new HashMap<Task, HashMap<Task, Long>>();
	private HashMap<Task, Double> fileTransferTime = new HashMap<Task, Double>();
	public boolean ignoreDAGmode = false;
	private int defultTotalCloudletNum;
	public int totalCloudletNum = 0;

	// ready negative-not ready 0-ready
	private int[] readyed = new int[5000]; // TODO assume 5k cloudlets at max
	private int[] totalNeed = new int[5000];

	public DAG() {
	}

	public void rmCache() {
		if (ignoreDAGmode) {
			for (int i = 0; i < totalCloudletNum; i++)
				readyed[i] = 0;
		}

		totalCloudletNum = defultTotalCloudletNum;
		// for (int i = 0; i < totalCloudletNum; i++)
		// readyed[i] = totalNeed[i];
		System.arraycopy(totalNeed, 0, readyed, 0, defultTotalCloudletNum);

		this.ignoreDAGmode = false;
	}

	public void turnOnIgnoreDAGMode() {
		this.ignoreDAGmode = true;
	}

	public int[] randTopo(List<Task> tasks, Random rand) {
		// temporary saving readyed list
		int[] rSave = new int[defultTotalCloudletNum];
		System.arraycopy(readyed, 0, rSave, 0, defultTotalCloudletNum);
		System.arraycopy(totalNeed, 0, readyed, 0, defultTotalCloudletNum);

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

		// recover readyed to outside environment
		System.arraycopy(rSave, 0, readyed, 0, defultTotalCloudletNum);

		int[] resi = new int[tasks.size()];
		for (int i = 0; i < tasks.size(); i++)
			resi[i] = tasks.indexOf(res.get(i));
		return resi;
	}

	public void addCloudWorkflow(Task from, Task to) {
		if (!this.requiring.containsKey(to))
			this.requiring.put(to, new ArrayList<Task>());
		this.requiring.get(to).add(from);

		if (!this.contributeTo.containsKey(from))
			this.contributeTo.put(from, new ArrayList<Task>());
		this.contributeTo.get(from).add(to);

		int index = to.getCloudletId() - Infrastructure.CLOUDLET_ID_SHIFT;
		readyed[index] += 1;
		totalNeed[index] += 1;
	}

	public void setFilesBetween(Task from, Task to, long fileSize) {
		if (!files.containsKey(from))
			files.put(from, new HashMap<Task, Long>());
		files.get(from).put(to, fileSize);
	}

	public synchronized boolean isCloudletPrepared(Task cloudlet) {
		if (ignoreDAGmode)
			return true;

		int index = cloudlet.getCloudletId() - Infrastructure.CLOUDLET_ID_SHIFT;
		return readyed[index] == 0;
	}

	public synchronized void afterOneCloudletSuccess(Task cloudlet) {
		if (contributeTo.containsKey(cloudlet))
			for (Task p : contributeTo.get(cloudlet)) {
				int pindex = p.getCloudletId() - Infrastructure.CLOUDLET_ID_SHIFT;
				readyed[pindex] -= 1;
			}
	}

	public void setCloudletNum(int totalCloudletNum) {
		this.totalCloudletNum = totalCloudletNum;
		defultTotalCloudletNum = totalCloudletNum;
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
		this.fileTransferTime.clear();
		for (Task c : cList)
			fileTransferTime.put(c, 0.0);

		if (ignoreDAGmode)
			return;

		for (Task target : requiring.keySet()) {
			for (Task src : requiring.get(target)) {
				int s = cList.indexOf(src);
				int t = cList.indexOf(target);
				if (task2ins[s] != task2ins[t]) {
					long bw = Long.min(vmlist.get(task2ins[s]).getBw(), vmlist.get(task2ins[t]).getBw());
					double time = fileTransferTime.get(src) + this.getFileTransferSize(src, target) / bw;
					fileTransferTime.put(src, time);
				} // if type not equal
			} // for target
		} // for src
	}

	private long getFileTransferSize(Task from, Task to) {
		if (!this.files.containsKey(from))
			return new Random(123456L).nextInt(448484515); // Early version
		// return 0;
		if (!this.files.get(from).containsKey(to))
			return 0; // no files to transferring

		return this.files.get(from).get(to);
	}

	public double getFileTransferTime(Task c) {
		return fileTransferTime.get(c);
	}

	public boolean hasPred(Task x) {
		if (ignoreDAGmode)
			return false;

		if (requiring.containsKey(x) && requiring.get(x).size() > 0)
			return true;
		return false;
	}

	public boolean hasSucc(Task x) {
		if (ignoreDAGmode)
			return false;

		if (contributeTo.containsKey(x) && contributeTo.get(x).size() > 0)
			return true;
		return false;
	}

	public HashMap<Task, List<Task>> getRequiring() {
		return requiring;
	}

	public List<Task> meRequires(Task me) {
		if (ignoreDAGmode || !requiring.containsKey(me))
			return new ArrayList<Task>();

		return requiring.get(me);
	}

	public List<Task> meContributeTo(Task me) {
		if (ignoreDAGmode || !contributeTo.containsKey(me))
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
