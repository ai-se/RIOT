package wikiServerSim;

import java.text.DecimalFormat;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;

public class MyCloudSimHelper {
	@SuppressWarnings("deprecation")
	public static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("CloudletID\tStatus\tDataCenter\tVM ID\tTime\t Start Time\t Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(cloudlet.getCloudletId() + "\t");

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.printLine("SUCCESS \t" + cloudlet.getAllResourceName()[0] + "\t" + cloudlet.getVmId() + "\t"
//						+ CloudSim.getEntity(cloudlet.getVmId()).toString()
						+ dft.format(cloudlet.getActualCPUTime()) + "\t" + dft.format(cloudlet.getExecStartTime())
						+ "\t" + dft.format(cloudlet.getFinishTime()));

			} else {
				Log.printLine("FAIL");
			}
		}
	}
}
