package edu.ncsu.datasets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.ncsu.wls.CloudletPassport;
import edu.ncsu.wls.MyCloudlet;

/**
 * Parser and interpreter of Pegasus Workflow dataset, widely used in Scientific
 * Workflow research
 * 
 * @author jianfeng
 *
 */
public class PEGASUS implements Dataset {
	private int idshift;
	private int userid;
	private String problemName;
	private double starttime = 0.1; // TODO Set start time?

	private Map<String, MyCloudlet> tasks;
	private CloudletPassport workflow;

	public PEGASUS(int userid, int idshift, String problemName) {
		tasks = new HashMap<String, MyCloudlet>();
		workflow = new CloudletPassport();
		this.userid = userid;
		this.idshift = idshift;
		this.problemName = problemName;
		this.createTasksAndWorkFlows();
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
	 * Scan the xml file, parse it.
	 */
	private void createTasksAndWorkFlows() {
		File fXmlFile = new File("src/edu/ncsu/datasets/pegasus/" + this.problemName + ".xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		Document doc;
		NodeList jobNodes = null;
		NodeList flowNodes = null;
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize(); // optional, but recommended
			jobNodes = doc.getElementsByTagName("job");
			flowNodes = doc.getElementsByTagName("child");
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}

		// System.out.println(jobNodes.getLength());
		// step 1 create cloudlets
		for (int i = 0; i < jobNodes.getLength(); i++) {
			Element jobInfo = (Element) jobNodes.item(i);
			double runtime = Double.parseDouble(jobInfo.getAttribute("runtime"));
			String id = jobInfo.getAttribute("id");
			tasks.put(id, createCloudlet(runtime));
		}
		tasks.put("entry", createCloudlet(1));
		tasks.put("exit", createCloudlet(1));

		// step 2 create workflows
		for (int i = 0; i < flowNodes.getLength(); i++) {
			Element flowInfo = (Element) flowNodes.item(i);
			String child = flowInfo.getAttribute("ref");
			for (int j = 0; j < flowInfo.getElementsByTagName("parent").getLength(); j++) {
				Element parente = (Element) flowInfo.getElementsByTagName("parent").item(j);
				String parent = parente.getAttribute("ref");
				this.workflow.addCloudWorkflow(tasks.get(parent), tasks.get(child));
			}
		}
		// linking entry->..
		for (MyCloudlet myc : this.getCloudletList()) {
			if (myc == tasks.get("entry"))
				continue;

			if (!this.workflow.hasPred(myc))
				this.workflow.addCloudWorkflow(tasks.get("entry"), myc);
		}
		// linking ..->exit
		for (MyCloudlet myc : this.getCloudletList()) {
			if (myc == tasks.get("exit"))
				continue;

			if (!this.workflow.hasSucc(myc))
				this.workflow.addCloudWorkflow(myc, tasks.get("exit"));
		}

		// step 3 updating the files transferred between tasks
		HashMap<String, String> useAsInput = new HashMap<String, String>(); // fileName-taskName
		HashMap<String, String> outputTo = new HashMap<String, String>();
		HashMap<String, Integer> fileSize = new HashMap<String, Integer>(); // fileName-task

		for (int i = 0; i < jobNodes.getLength(); i++) {
			Element jobInfo = (Element) jobNodes.item(i);
			String myid = jobInfo.getAttribute("id");

			for (int j = 0; j < jobInfo.getElementsByTagName("uses").getLength(); j++) {
				Element fileInfo = (Element) jobInfo.getElementsByTagName("uses").item(j);

				if (fileInfo.getAttribute("link").equals("input"))
					useAsInput.put(fileInfo.getAttribute("file"), myid);

				if (fileInfo.getAttribute("link").equals("output"))
					outputTo.put(fileInfo.getAttribute("file"), myid);

				fileSize.put(fileInfo.getAttribute("file"), Integer.parseInt(fileInfo.getAttribute("size")));
			}
		}

		for (String file : useAsInput.keySet()) {
			String toid = useAsInput.get(file);
			String fromid = outputTo.get(file);

			if (fromid == null)
				fromid = "entry";
			this.workflow.setFilesBetween(tasks.get(fromid), tasks.get(toid), fileSize.get(file));
		}

		for (String file : outputTo.keySet()) {
			if (useAsInput.get(file) == null) {
				String fromid = outputTo.get(file);
				this.workflow.setFilesBetween(tasks.get(fromid), tasks.get("exit"), fileSize.get(file));
			}
		}
	}

	@Override
	public List<MyCloudlet> getCloudletList() {
		// Log.printLine("RUNNING at PEGASUS " + this.problemName + "
		// getcloudlist...");
		List<MyCloudlet> cloudletList = new ArrayList<MyCloudlet>();
		for (String name : this.tasks.keySet())
			cloudletList.add(tasks.get(name));
		return cloudletList;
	}

	@Override
	public CloudletPassport getCloudletPassport() {
		return this.workflow;
	}

	public static void main(String[] args) {
		PEGASUS test = new PEGASUS(0, 0, "Montage_25");
		 System.out.println(test.getCloudletList());
		 System.out.println(test.getCloudletPassport());
	}
}