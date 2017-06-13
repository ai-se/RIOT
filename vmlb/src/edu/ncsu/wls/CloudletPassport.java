package edu.ncsu.wls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;

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
	private HashMap<Cloudlet, HashMap<Cloudlet, Integer>> files = new HashMap<Cloudlet, HashMap<Cloudlet, Integer>>();

	public CloudletPassport() {
	}

	public void addCloudWorkflow(Cloudlet from, Cloudlet to) {
		if (!this.requiring.containsKey(to))
			this.requiring.put(to, new ArrayList<Cloudlet>());
		this.requiring.get(to).add(from);
	}

	public void setFilesBetween(Cloudlet from, Cloudlet to, int fileSize) {
		if (!files.containsKey(from))
			files.put(from, new HashMap<Cloudlet, Integer>());
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

	public int getFileTransferSize(Cloudlet from, Cloudlet to) {
		if (!this.files.containsKey(from))
			return 1000; // Early version, no bandwidth or file transferring
							// considered
		if (!this.files.get(from).containsKey(to))
			return 0; // no files to transferring

		return this.files.get(from).get(to);
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
