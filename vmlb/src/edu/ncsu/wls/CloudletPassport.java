package edu.ncsu.wls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

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

public class CloudletPassport {
	private HashMap<Cloudlet, List<Cloudlet>> requiring = new HashMap<Cloudlet, List<Cloudlet>>();
	private List<Integer> receivedCloudletIds = new ArrayList<Integer>();
	private HashMap<Cloudlet, HashMap<Cloudlet, Long>> files = new HashMap<Cloudlet, HashMap<Cloudlet, Long>>();
	private HashMap<Cloudlet, Double> fileTransferTime = new HashMap<Cloudlet, Double>();

	public CloudletPassport() {
	}

	public void addCloudWorkflow(Cloudlet from, Cloudlet to) {
		if (!this.requiring.containsKey(to))
			this.requiring.put(to, new ArrayList<Cloudlet>());
		this.requiring.get(to).add(from);
	}

	public void setFilesBetween(Cloudlet from, Cloudlet to, long fileSize) {
		if (!files.containsKey(from))
			files.put(from, new HashMap<Cloudlet, Long>());
		files.get(from).put(to, fileSize);
	}

	public boolean isCloudletPrepared(Cloudlet cloudlet) {
		// Log.printLine("checking passing? at" + cloudlet.getCloudletId());
		if (!this.requiring.containsKey(cloudlet) || this.requiring.get(cloudlet).size() == 0) {
			return true;
		}

		for (Cloudlet requirement : this.requiring.get(cloudlet))
			if (!this.receivedCloudletIds.contains(requirement.getCloudletId())) {
				// Log.printLine("We're checking cloudlet and found fail at ***
				// " + cloudlet.getCloudletId());
				return false;
			}
		return true;
	}

	public void afterOneCloudletSuccess(Cloudlet cloudlet) {
		this.receivedCloudletIds.add(cloudlet.getCloudletId());
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
					long bw = Long.min(vmlist.get(task2ins[s]).getBw(),
							vmlist.get(task2ins[t]).getBw());
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
		for (Cloudlet c : requiring.keySet())
			if (requiring.get(c).contains(x))
				return true;
		return false;
	}

	public HashMap<Cloudlet, List<Cloudlet>> getRequiring() {
		return requiring;
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
