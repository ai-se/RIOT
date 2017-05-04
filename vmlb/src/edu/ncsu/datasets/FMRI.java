package edu.ncsu.datasets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;

import edu.ncsu.wls.CloudletPassport;
import edu.ncsu.wls.MyCloudlet;

/**
 * Simulating the workflow of paper Mapping of Scientific Workflow within the
 * Grid Middleware Services for Virtual Data Discovery, Composition, and
 * Integration
 * 
 * workload was referred in https://github.com/pegasus-isi/WorkflowGenerator/blob/master/juve/yu/fmri.py
 * @author jianfeng
 *
 *         TODO Considering memory/bw/IO?
 */

public class FMRI {
	private int idshift;
	private int userid; // TODO assuming single user at this time
	private double starttime = 0.1; // TODO Set start time?
	private Map<String, MyCloudlet> tasks;
	private CloudletPassport workflow;

	public FMRI(int userid, int idshift) {
		tasks = new HashMap<String, MyCloudlet>();
		workflow = new CloudletPassport();
		this.idshift = idshift;
		this.userid = userid;

		this.createTasks();
		this.createWorkFlows();
	}

	public List<MyCloudlet> getCloudletList() {
		Log.printLine("RUNNING at f-MRI getcloudlist...");
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

	private void createTasks() {
		tasks.put("align_warp/1", createCloudlet(38.0)); // 0
		tasks.put("align_warp/3", createCloudlet(37.0)); // 1
		tasks.put("align_warp/5", createCloudlet(31.6)); // 2
		tasks.put("align_warp/7", createCloudlet(39.6)); // 3
		tasks.put("reslice/2", createCloudlet(67.0)); // 4
		tasks.put("reslice/4", createCloudlet(66.9)); // 5
		tasks.put("reslice/6", createCloudlet(67.3)); // 6
		tasks.put("reslice/8", createCloudlet(60.9)); // 7
		tasks.put("softmean/9", createCloudlet(36.6)); // 8
		tasks.put("slicer/10", createCloudlet(37.8)); // 9
		tasks.put("slicer/12", createCloudlet(30.97)); // 10
		tasks.put("slicer/14", createCloudlet(34.15)); // 11
		tasks.put("convert/11", createCloudlet(66.0)); // 12
		tasks.put("convert/13", createCloudlet(65.7)); // 13
		tasks.put("convert/15", createCloudlet(64.0)); // 14
	}

	/**
	 * just to simplify for createWorkFlows
	 * 
	 * @param from
	 * @param to
	 */
	private void set(String from, String to) {
//		if (!this.tasks.keySet().contains(from) || !this.tasks.keySet().contains(to)){
//			System.out.println(from + " " + to);
//		}
		this.workflow.addCloudWorkflow(tasks.get(from), tasks.get(to));
	}

	private void createWorkFlows() {
		set("align_warp/1", "reslice/2");
		set("align_warp/3", "reslice/4");
		set("align_warp/5", "reslice/6");
		set("align_warp/7", "reslice/8");
		set("reslice/2", "softmean/9");
		set("reslice/4", "softmean/9");
		set("reslice/6", "softmean/9");
		set("reslice/8", "softmean/9");
		set("softmean/9", "slicer/10");
		set("softmean/9", "slicer/12");
		set("softmean/9", "slicer/14");
		set("slicer/10", "convert/11");
		set("slicer/12", "convert/13");
		set("slicer/14", "convert/15");
	}

	public static void main(String[] args) {
		FMRI x = new FMRI(0, 0);
	}
}
