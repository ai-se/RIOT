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

	public CloudletPassport() {
	}

	public void addCloudWorkflow(Cloudlet from, Cloudlet to) {
		if (!this.requiring.containsKey(to))
			this.requiring.put(to, new ArrayList<Cloudlet>());
		this.requiring.get(to).add(from);
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

	public HashMap<Cloudlet, List<Cloudlet>> getRequiring() {
		return requiring;
	}
}
