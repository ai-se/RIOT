package edu.ncsu.wls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

/**
 * 
 * @author jianfeng
 *
 *         CloudletPassport is used for controlling global workingflow of
 *         cloudlets Used as a member in any CloudletScheduler
 * 
 */

public class CloudletDAG {
	private HashMap<Cloudlet, List<Cloudlet>> requiring = new HashMap<Cloudlet, List<Cloudlet>>();
	private HashMap<Cloudlet, List<Cloudlet>> contributeTo = new HashMap<Cloudlet, List<Cloudlet>>();
	private HashMap<Cloudlet, HashMap<Cloudlet, Long>> files = new HashMap<Cloudlet, HashMap<Cloudlet, Long>>();
	private HashMap<Cloudlet, Double> fileTransferTime = new HashMap<Cloudlet, Double>();
	public int totalCloudletNum = 0;

	// ready 0-notCalc 1-notReady 2-Ready -1-Always ready(no requires)
	private int[] readyed = new int[5000]; // TODO assume 5k cloudlets at max

	public CloudletDAG() {
	}

	public void rmCache() {
		if (IntStream.of(readyed).sum() != 0) // not the first time rmCache
			for (int i = 0; i < totalCloudletNum; i++)
				readyed[i] = readyed[i] == -1 ? -1 : 1;
	}

	public void addCloudWorkflow(Cloudlet from, Cloudlet to) {
		if (!this.requiring.containsKey(to))
			this.requiring.put(to, new ArrayList<Cloudlet>());
		this.requiring.get(to).add(from);

		if (!this.contributeTo.containsKey(from))
			this.contributeTo.put(from, new ArrayList<Cloudlet>());
		this.contributeTo.get(from).add(to);
	}

	public void setFilesBetween(Cloudlet from, Cloudlet to, long fileSize) {
		if (!files.containsKey(from))
			files.put(from, new HashMap<Cloudlet, Long>());
		files.get(from).put(to, fileSize);
	}

	public synchronized boolean isCloudletPrepared(Cloudlet cloudlet) {
		int index = cloudlet.getCloudletId() - Infrastructure.CLOUDLET_ID_SHIFT;

		switch (readyed[index]) {
		case 1:
			return false;
		case 2:
			return true;
		case -1:
			return true;
		case 0:
			if (!this.hasPred(cloudlet)) {
				readyed[index] = -1;
				return true;
			} else {
				readyed[index] = 1;
				return false;
			}

		}
		return true;
	}

	public synchronized void afterOneCloudletSuccess(Cloudlet cloudlet) {
		if (contributeTo.containsKey(cloudlet))
			for (Cloudlet p : contributeTo.get(cloudlet)) {
				int pindex = p.getCloudletId() - Infrastructure.CLOUDLET_ID_SHIFT;
				if (readyed[pindex] != -1)
					readyed[pindex] = 2;
			}
	}

	public void setCloudletNum(int totalCloudletNum) {
		this.totalCloudletNum = totalCloudletNum;
	}

	/**
	 * Calculating/ pre-calc the required file-transfer times for each cloudlet
	 * Only when all files finished transfer, is the cloudlet be done.
	 * 
	 * @param task2ins
	 * @param ins2type
	 * @param vmlist
	 */
	public void calcFileTransferTimes(int[] task2ins, List<Vm> vmlist, List<MyCloudlet> cList) {
		this.fileTransferTime.clear();
		for (Cloudlet c : cList)
			fileTransferTime.put(c, 0.0);

		for (Cloudlet target : requiring.keySet()) {
			for (Cloudlet src : requiring.get(target)) {
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

	private long getFileTransferSize(Cloudlet from, Cloudlet to) {
		if (!this.files.containsKey(from))
			return new Random(123456L).nextInt(448484515); // Early version
		// return 0;
		if (!this.files.get(from).containsKey(to))
			return 0; // no files to transferring

		return this.files.get(from).get(to);
	}

	public double getFileTransferTime(Cloudlet c) {
		return fileTransferTime.get(c);
	}

	public boolean hasPred(Cloudlet x) {
		if (requiring.containsKey(x) && requiring.get(x).size() > 0)
			return true;
		return false;
	}

	public boolean hasSucc(Cloudlet x) {
		if (contributeTo.containsKey(x) && contributeTo.get(x).size() > 0)
			return true;
		return false;
	}

	public HashMap<Cloudlet, List<Cloudlet>> getRequiring() {
		return requiring;
	}

	public List<Cloudlet> meRequires(Cloudlet me) {
		if (!requiring.containsKey(me))
			return new ArrayList<Cloudlet>();

		return requiring.get(me);
	}

	public List<Cloudlet> meContributeTo(Cloudlet me) {
		if (!contributeTo.containsKey(me))
			return new ArrayList<Cloudlet>();

		return contributeTo.get(me);
	}

	public String toString() {
		String r = "FLOW\n";
		for (Cloudlet to : this.requiring.keySet()) {
			for (Cloudlet from : this.requiring.get(to))
				r += (from) + "|";
			r += "-> " + to + "\n";
		}

		return r;
	}
}
