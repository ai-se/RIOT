package edu.ncsu.algorithms;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import edu.ncsu.datasets.Utils;
import edu.ncsu.wls.CloudletPassport;
import edu.ncsu.wls.DAGCloudletSchedulerSpaceShared;
import edu.ncsu.wls.Infrastructure;
import edu.ncsu.wls.MyCloudlet;
import edu.ncsu.wls.OnlineDatacenterBroker;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionType;
import jmetal.core.Variable;
import jmetal.util.JMException;

/**
 * Storing element type of chromosome for VM scheduling problem Used by
 * VMScheduleSoltionType
 * 
 * @author jianfeng
 *
 */
class VMLoc extends Variable {
	private static final long serialVersionUID = -875003925545159630L;
	private int value_; // store VM ID~

	public VMLoc(int value) {
		this.value_ = value;
	}

	public double getValue() {
		return value_;
	}

	public void setValue(int value) {
		this.value_ = value;
	}

	@Override
	public String toString() {
		return "" + value_;
	}

	@Override
	public Variable deepCopy() {
		return new VMLoc(value_);
	}
}

/**
 * Solution type of VMS problem. including a function-- randomly assigning vm
 * scheduling
 * 
 * @author jianfeng
 *
 */
class VMScheduleSolutionType extends SolutionType {
	Random rand;
	VmsProblem problem_;

	public VMScheduleSolutionType(VmsProblem problem, Random rand) {
		super(problem);
		problem_ = problem;
		this.rand = rand;
	}

	@Override
	public Variable[] createVariables() {
		Variable[] variables = new Variable[problem_.getNumberOfVariables()];
		for (int var = 0; var < problem_.getNumberOfVariables(); var++)
			variables[var] = new VMLoc(Utils.sampleOne(problem_.getVmids(), rand));
		return variables;
	}
}

/**
 * Definition of VM scheduling problem
 * 
 * @author jianfeng
 *
 */
public class VmsProblem extends Problem {
	private static final long serialVersionUID = 6371615104008697832L;
	public Random rand;
	private int[] vmid_;
	private int cloudletNum;
	private List<MyCloudlet> cloudletList;
	private CloudletPassport workflow;

	private String name;

	@SuppressWarnings("unchecked")
	public VmsProblem(String dataset, Random rand) {
		this.rand = rand;
		this.name = dataset;

		List<Vm> vms = Infrastructure.createVms(-1);
		vmid_ = new int[vms.size()];
		for (int i = 0; i < vms.size(); i++)
			vmid_[i] = vms.get(i).getId();

		Object[] tmpInfo = Infrastructure.getCaseCloudlets(this.name, -1);
		cloudletList = (List<MyCloudlet>) tmpInfo[0];
		cloudletNum = cloudletList.size();
		workflow = (CloudletPassport) (tmpInfo[1]);

		this.numberOfVariables_ = this.cloudletNum;
		this.numberOfObjectives_ = 1; // TODO change this if needed

		this.solutionType_ = new VMScheduleSolutionType(this, rand);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void evaluate(Solution solution) throws JMException {
		int[] mapping = new int[cloudletNum];
		for (int var = 0; var < this.numberOfVariables_; var++)
			mapping[var] = (int) solution.getDecisionVariables()[var].getValue();

		// ****** starting cloudsim simulation
		Log.disable();
		// Create Cloudsim server
		int num_user = 1; // number of cloud users
		Calendar calendar = Calendar.getInstance();
		boolean trace_flag = false; // trace events
		CloudSim.init(num_user, calendar, trace_flag);
		Infrastructure.createDatacenter();
		OnlineDatacenterBroker broker = null;
		try {
			broker = new OnlineDatacenterBroker("dc_broker");
		} catch (Exception e) {
			e.printStackTrace();
		}

		int dcBrokerId = broker.getId();

		// Get dataset
		Object[] info = Infrastructure.getCaseCloudlets(this.name, broker.getId());
		List<MyCloudlet> cloudletList = (List<MyCloudlet>) info[0];
		CloudletPassport workflow = (CloudletPassport) info[1];

		// Create vm list
		List<Vm> vmlist = new ArrayList<Vm>();
		vmlist = Infrastructure.createVms(dcBrokerId);

		for (Vm vm : vmlist)
			((DAGCloudletSchedulerSpaceShared) (vm.getCloudletScheduler())).setCloudletPassport(workflow);

		broker.submitCloudletList(cloudletList);
		broker.submitVmList(vmlist);

		for (Cloudlet c : cloudletList) {
			c.setVmId(mapping[c.getCloudletId() - Infrastructure.CLOUDLET_ID_SHIFT]);
		}

		CloudSim.startSimulation();
		CloudSim.stopSimulation();

		List<Cloudlet> newList = broker.getCloudletReceivedList();
		// MyCloudSimHelper.printCloudletList(newList);

		if (newList.size() != cloudletList.size()) {
			System.err.println("can not simulating all cloudlets!");
			System.exit(-1);
		}

		double makespan = 0;
		for (Cloudlet c : newList) {
			makespan = Math.max(makespan, c.getFinishTime());
		}

		// ***** end of cloudsim simulation
		solution.setObjective(0, makespan);
	}

	public int[] getVmids() {
		return this.vmid_;
	}

	public int randInt(int bound) {
		return this.rand.nextInt(bound);
	}

	public CloudletPassport getWorkflow() {
		return workflow;
	}

	// /**
	// * Return back the DAG for workflow of current VmsProblem
	// *
	// * @return Key(int)- id of cloudlet vale(List) - containing list which the
	// * key connect to.
	// */
	// @SuppressWarnings({ "rawtypes", "unchecked" })
	// public Map<Integer, List> parseOutDAG() {
	// Map<Integer, List> res = new HashMap<Integer, List>();
	//
	// HashMap<Cloudlet, List<Cloudlet>> graph = this.workflow.getRequiring();
	//
	// for (Cloudlet from : graph.keySet()) {
	// int fromi = from.getCloudletId();
	// res.put(fromi, new ArrayList<Integer>());
	// for (Cloudlet to : graph.get(from))
	// res.get(fromi).add(to.getCloudletId());
	// }
	// return res;
	// }
}
