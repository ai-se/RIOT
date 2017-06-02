package edu.ncsu.datasets;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;

import edu.ncsu.wls.CloudletPassport;
import edu.ncsu.wls.MyCloudlet;

/**
 * Simulating the workflow in PSPLIB Reference
 * http://www.om-db.wi.tum.de/psplib/files/Kolisch-Sprecher-96.pdf auto parsing
 * the data in edu.ncsu.datasets.psplib
 * 
 * @author jianfeng
 *
 */

public class PSPLIB implements Dataset {
	private int idshift;
	private int userid; // TODO assuming single user at this time
	private double starttime = 0.1; // TODO Set start time?
	private String version;
	private int taskNum;

	private Map<String, MyCloudlet> tasks;
	private CloudletPassport workflow;

	public PSPLIB(int userid, int idshift, String version, int taskNum) {
		tasks = new HashMap<String, MyCloudlet>();
		workflow = new CloudletPassport();
		this.idshift = idshift;
		this.userid = userid;
		this.version = version;
		this.taskNum = taskNum;

		this.createTasksAndWorkFlows();
	}

	public List<MyCloudlet> getCloudletList() {
		Log.printLine("RUNNING at " + this.version + " getcloudlist...");
		List<MyCloudlet> cloudletList = new ArrayList<MyCloudlet>();

		for (String name : this.tasks.keySet())
			cloudletList.add(tasks.get(name));
		return cloudletList;
	}

	public CloudletPassport getCloudletPassport() {
		return workflow;
	}

	private MyCloudlet createCloudlet(double workloadInSecs) {
		// cloudlet parameters
		long length = (long) (1000.0 * workloadInSecs);
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		MyCloudlet cloudlet = new MyCloudlet(idshift++, length, pesNumber, fileSize, outputSize, utilizationModel,
				utilizationModel, utilizationModel, starttime);
		cloudlet.setUserId(userid);

		return cloudlet;
	}

	/**
	 * Scan the psplib file, parse it and create cloudlets as well as the
	 * workflow.
	 */
	private void createTasksAndWorkFlows() {
		InputStream modelStream = PSPLIB.class.getResourceAsStream("/edu/ncsu/datasets/psplib/" + this.version + ".sm");
		Scanner scan = new Scanner(modelStream);
		String[] taskFlowInfo = new String[this.taskNum];
		String[] taskLoadInfo = new String[this.taskNum];

		while (scan.hasNextLine())
			if (scan.nextLine().contains("successors"))
				break;

		scan.nextLine(); // ignore the start task
		for (int i = 0; i < this.taskNum; i++)
			taskFlowInfo[i] = scan.nextLine();

		while (scan.hasNextLine())
			if (scan.nextLine().contains("duration"))
				break;
		scan.nextLine();
		scan.nextLine(); // ignore the start task

		for (int i = 0; i < this.taskNum; i++)
			taskLoadInfo[i] = scan.nextLine();
		scan.close();

		// Create cloudlets
		for (String str : taskLoadInfo) {
			String[] s = str.split("\\s+");
			tasks.put(s[s.length-7], createCloudlet(Integer.parseInt(s[s.length-5]) * 10));
		}
		
		
		// Create workFlows
		for (String str : taskFlowInfo) {
			String[] s = str.split("\\s+");
			int successors = Integer.parseInt(s[3]);
			for (int i = 4; i < 4 + successors; i++) {
				if (Integer.parseInt(s[i]) > this.taskNum + 1)
					continue;
				// System.out.println(s[1] + "- > " + s[i]);
				set(s[1], s[i]);
			}
		}
	}

	/**
	 * just to simplify for createWorkFlows
	 * 
	 * @param from
	 * @param to
	 */
	private void set(String from, String to) {
		this.workflow.addCloudWorkflow(tasks.get(from), tasks.get(to));
	}

	public static void main(String[] args) {
		PSPLIB x = new PSPLIB(0, 0, "j1201_1", 120);
		System.out.println(x.getCloudletList().size());
	}
}
