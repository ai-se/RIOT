package edu.ncsu.datasets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;

import edu.ncsu.wls.DAG;
import edu.ncsu.wls.Task;

/**
 * Simulating the workflow of paper Mapping of Scientific Workflow within the
 * e-Protein project to Distributed Resources
 * 
 * @author jianfeng
 *
 *         TODO Considering memory/bw/IO?
 */

public class Eprotein implements Dataset {
	private int idshift;
	private int userid; // TODO assuming single user at this time
	private Map<String, Task> tasks;
	private DAG workflow;

	public Eprotein(int userid, int idshift) {
		tasks = new HashMap<String, Task>();
		workflow = new DAG();
		this.idshift = idshift;
		this.userid = userid;

		this.createTasks();
		this.createWorkFlows();
	}

	public List<Task> getCloudletList() {
		Log.printLine("RUNNING at Eprotein getcloudlist...");
		List<Task> cloudletList = new ArrayList<Task>();

		for (String name : this.tasks.keySet())
			cloudletList.add(tasks.get(name));

		return cloudletList;
	}

	public DAG getCloudletPassport() {
		return workflow;
	}

	private Task createCloudlet(double workloadInSecs) {
		// cloudlet parameters
		long length = (long) (1000.0 * workloadInSecs);
		// long fileSize = 300;
		// long outputSize = 300;
		// int pesNumber = 1;
		// UtilizationModel utilizationModel = new UtilizationModelFull();

		// Task cloudlet = new Task(idshift++, length, pesNumber, fileSize,
		// outputSize, utilizationModel,
		// utilizationModel, utilizationModel);
		Task cloudlet = new Task(idshift++, length);
		cloudlet.setUserId(userid);

		return cloudlet;
	}

	private void createTasks() {
		tasks.put("PROSITE", createCloudlet(0.5)); // 0
		tasks.put("SEG", createCloudlet(0.3)); // 1
		tasks.put("COILS2", createCloudlet(0.4)); // 2
		tasks.put("SingalP", createCloudlet(67.0)); // 3
		tasks.put("TMHMM", createCloudlet(0.3)); // 4
		tasks.put("HMMer", createCloudlet(150.0)); // 5
		tasks.put("Prospero", createCloudlet(7)); // 6
		tasks.put("IMPALA", createCloudlet(86)); // 7
		tasks.put("BLAST", createCloudlet(75)); // 8
		tasks.put("PSI_BLAST", createCloudlet(320)); // 9
		tasks.put("PSI_PRED", createCloudlet(0.7)); // 10
		tasks.put("Summary", createCloudlet(5)); // 11
		tasks.put("3D_PSSM", createCloudlet(670.0)); // 12
		tasks.put("Genome_Summary", createCloudlet(472)); // 13
		tasks.put("SCOP", createCloudlet(270)); // 14
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

	private void createWorkFlows() {
		set("PROSITE", "Summary");
		set("SEG", "HMMer");
		set("SEG", "Prospero");
		set("COILS2", "Prospero");
		set("SingalP", "TMHMM");
		set("TMHMM", "Prospero");
		set("HMMer", "Summary");
		set("Prospero", "IMPALA");
		set("Prospero", "BLAST");
		set("Prospero", "PSI_BLAST");
		set("PSI_BLAST", "PSI_PRED");
		set("IMPALA", "Summary");
		set("BLAST", "Summary");
		set("PSI_BLAST", "Summary");
		set("PSI_PRED", "3D_PSSM");
		set("Summary", "Genome_Summary");
		set("3D_PSSM", "Genome_Summary");
		set("Genome_Summary", "SCOP");
	}

	// public static void main(String[] args) {
	// Eprotein x = new Eprotein(0, 0);
	// }
}
