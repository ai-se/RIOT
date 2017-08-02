package edu.ncsu.wls;

import org.cloudbus.cloudsim.Cloudlet;

/**
 * This file is a simplified version of cloudbus's Cloudlet
 * 
 * @author jianfeng
 *
 */
public class Task {
	public final int id;
	private int userId = 0;
	private double execStartTime;
	private double finishTime;
	private long cloudletLength;
	public long defCloudletL;
	private int status;
	protected int vmId;
	private final Resource res;

	public Task(int cloudletId, long cloudletLength) {
		this.id = cloudletId;
		this.cloudletLength = cloudletLength;
		this.res = new Resource();
		this.defCloudletL = cloudletLength;
	}

	/**
	 * Internal class that keeps track of Cloudlet's movement in different
	 * CloudResources. Each time a cloudlet is run on a given VM, the cloudlet's
	 * execution history on each VM is registered
	 * 
	 * @todo THIS IS FOR MULTIPLE_CORE ENVIRONMENT EXTENSION
	 */
	private static class Resource {

		/**
		 * Cloudlet's submission (arrival) time to a CloudResource.
		 */
		public double submissionTime = 0.0;

		/**
		 * The time this Cloudlet resides in a CloudResource (from arrival time
		 * until departure time, that may include waiting time).
		 */
		public double wallClockTime = 0.0;

		/**
		 * The total time the Cloudlet spent being executed in a CloudResource.
		 */
		public double actualCPUTime = 0.0;

		/**
		 * Cloudlet's length finished so far.
		 */
		public long finishedSoFar = 0;

	}

	public boolean setCloudletLength(final long cloudletLength) {
		if (cloudletLength <= 0) {
			return false;
		}

		this.cloudletLength = cloudletLength;
		return true;
	}

	public double getWaitingTime() {
		final double subTime = res.submissionTime;
		return execStartTime - subTime;
	}

	public long getCloudletFinishedSoFar() {
		final long finish = res.finishedSoFar;
		if (finish > cloudletLength) {
			return cloudletLength;
		}

		return finish;
	}

	public long getRemainingCloudletLength() {
		return Long.max(this.cloudletLength - res.finishedSoFar, 0);
	}

	public boolean isFinished() {
		return res.finishedSoFar >= this.cloudletLength;
	}

	public void setCloudletFinishedSoFar(final long length) {
		if (length >= 0.0)
			res.finishedSoFar = length;
	}

	public void setUserId(final int id) {
		userId = id;
	}

	public int getUserId() {
		return userId;
	}

	public void setSubmissionTime(final double clockTime) {
		if (clockTime < 0.0)
			return;
		res.submissionTime = clockTime;
	}

	public double getSubmissionTime() {
		return res.submissionTime;
	}

	public void setExecStartTime(final double clockTime) {
		execStartTime = clockTime;
	}

	public double getExecStartTime() {
		return execStartTime;
	}

	/**
	 * Sets the Cloudlet's execution parameters. These parameters are set by the
	 * CloudResource before departure or sending back to the original Cloudlet's
	 * owner.
	 *
	 * @param wallTime
	 *            the time of this Cloudlet resides in a CloudResource (from
	 *            arrival time until departure time).
	 * @param actualTime
	 *            the total execution time of this Cloudlet in a CloudResource.
	 *
	 * @see Resource#wallClockTime
	 * @see Resource#actualCPUTime
	 *
	 * @pre wallTime >= 0.0
	 * @pre actualTime >= 0.0
	 * @post $none
	 */
	public void setExecParam(final double wallTime, final double actualTime) {
		if (wallTime < 0.0 || actualTime < 0.0) {
			return;
		}

		res.wallClockTime = wallTime;
		res.actualCPUTime = actualTime;
	}

	/**
	 * if newStatus is Cloudlet.SUCCESS then please provide currentTime
	 * 
	 * @param newStatus
	 */
	public void setCloudletStatus(final int newStatus, double currentClock) {
		// if the new status is same as current one, then ignore the rest
		if (status == newStatus) {
			return;
		}

		if (newStatus == Cloudlet.SUCCESS) {
			finishTime = currentClock;
		}

		status = newStatus;
	}

	public String getCloudletStatusString() {
		return Cloudlet.getStatusString(status);
	}

	public double getFinishTime() {
		return finishTime;
	}

	public int getStatus() {
		return status;
	}

	public int getCloudletId() {
		return id;
	}

	public int getVmId() {
		return vmId;
	}

	public void setVmId(final int vmId) {
		this.vmId = vmId;
	}

	public double getActualCPUTime() {
		return getFinishTime() - getExecStartTime();
	}

	public long getCloudletLength() {
		// return this.defCloudletL;
		return this.cloudletLength;
	}

	public String toString() {
		return "task@" + this.id;
	}
}