package edu.ncsu.model;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

	public static void forcePrintCloudList(List<Task> list) {
		Log.enable();
		printCloudletList(list);
	}

	public static void printCloudletList(List<Task> list) {
		int size = list.size();
		Collections.sort(list, Comparator.comparing(Task::getExecStartTime));
		Task cloudlet;
		String indent = "    ";
		// Log.enable();
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("CloudletID\tStatus\tVM ID\t    Time\t Start Time\t Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(str("(#" + cloudlet.getCloudletId() + ")  " + cloudlet, 15));

			if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
				Log.printLine("SUCCESS" + indent + str(cloudlet.getVmId(), 10)
						+ str(dft.format(cloudlet.getActualCPUTime()), 10) + indent
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
	public static void printCloudletList2(List<Task> list) {
		String indent = "   ";
		// Log.enable();
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("CloudletID\tStatus\tVM ID\t    Time\t Start Time\t Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");

		List<Integer> vmids = new ArrayList<Integer>();
		for (Task cloudlet : list) {
			if (!vmids.contains(cloudlet.getVmId()))
				vmids.add(cloudlet.getVmId());
		}

		Collections.sort(vmids);

		for (int v : vmids) {
			for (Task cloudlet : list) {
				if (cloudlet.getVmId() != v)
					continue;
				Log.print(str("(#" + cloudlet.getCloudletId() + ")  " + cloudlet, 15));

				if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
					Log.printLine("SUCCESS" + indent + indent + indent + str(cloudlet.getVmId(), 10)
							+ str(dft.format(cloudlet.getActualCPUTime()), 10) + indent
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
