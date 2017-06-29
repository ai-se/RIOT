package edu.ncsu.wls;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;

public class MyCloudSimHelper {
	@SuppressWarnings("deprecation")

	private static String str(Object o, int len) {
		String org = o.toString();
		if (org.length() < len) {
			int appends = len - org.length();
			for (int i = 0; i < appends; i++)
				org += " ";
		}
		return org;
	}

	public static void forcePrintCloudList(List<Cloudlet> list) {
		Log.enable();
		printCloudletList(list);
	}

	public static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;
		String indent = "    ";
		// Log.enable();
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("CloudletID\tStatus\tDataCenter\tVM ID\t    Time\t Start Time\t Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(str("(#" + cloudlet.getCloudletId() + ")  " + cloudlet, 15));

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.printLine("SUCCESS" + indent + cloudlet.getAllResourceName()[0] + indent + indent + indent
						+ str(cloudlet.getVmId(), 10) + str(dft.format(cloudlet.getActualCPUTime()), 10) + indent
						+ str(dft.format(cloudlet.getExecStartTime()), 15)
						+ str(dft.format(cloudlet.getFinishTime()), 6));

			} else {
				Log.printLine("FAIL");
			}
		}
	}

	/**
	 * Print the cloudlet let list information Group by vm Sorted by finished
	 * time
	 * 
	 * @param list
	 */
	public static void printCloudletList2(List<Cloudlet> list) {
		String indent = "   ";
		// Log.enable();
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("CloudletID\tStatus\tDataCenter\tVM ID\t    Time\t Start Time\t Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");

		List<Integer> vmids = new ArrayList<Integer>();
		for (Cloudlet cloudlet : list) {
			if (!vmids.contains(cloudlet.getVmId()))
				vmids.add(cloudlet.getVmId());
		}

		Collections.sort(vmids);

		for (int v : vmids) {
			for (Cloudlet cloudlet : list) {
				if (cloudlet.getVmId() != v)
					continue;
				Log.print(str("(#" + cloudlet.getCloudletId() + ")  " + cloudlet, 15));

				if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
					Log.printLine("SUCCESS" + indent + cloudlet.getAllResourceName()[0] + indent + indent + indent
							+ str(cloudlet.getVmId(), 10) + str(dft.format(cloudlet.getActualCPUTime()), 10) + indent
							+ str(dft.format(cloudlet.getExecStartTime()), 15)
							+ str(dft.format(cloudlet.getFinishTime()), 6));

				} else {
					Log.printLine("FAIL");
				} // if status
			} // for cloudlet
			Log.printLine();

		} // for v
	}
}
