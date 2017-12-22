package edu.ncsu.experiments;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.knowm.xchart.BubbleChart;
import org.knowm.xchart.BubbleChartBuilder;
import org.knowm.xchart.XChartPanel;

import jmetal.core.SolutionSet;

public class BubbleChartTool {
	private static BubbleChart bubbleChart;
	static XChartPanel<BubbleChart> chartPanel;
	static List<String> existSeries;

	static {
		existSeries = new ArrayList<String>();
		bubbleChart = new BubbleChartBuilder().width(500).height(400).build();
		// bubbleChart.addSeries("o0o2", null, new double[] { 0.1, 0.2, 0.3 },
		// new double[] { 0.1, 0.2, 0.3 });

		chartPanel = new XChartPanel<BubbleChart>(bubbleChart);
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Create and set up the window.
				JFrame frame = new JFrame("XChart");
				frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				frame.add(chartPanel);

				// Display the window.
				frame.pack();
				frame.setVisible(true);
			}
		});
	}

	public static void showFrontier(String series, SolutionSet ss) {
		double[] o1 = new double[ss.size()];
		double[] o2 = new double[ss.size()];
		double[] bubble = new double[ss.size()];

		for (int i = 0; i < ss.size(); i++) {
			o1[i] = ss.get(i).getObjective(0);
			o2[i] = ss.get(i).getObjective(1);
			bubble[i] = 3.0;
		}
		try {
			if (existSeries.contains(series)) { // updating
				bubbleChart.updateBubbleSeries(series, o1, o2, bubble);
			} else { // adding
				bubbleChart.addSeries(series, o1, o2, bubble);
				existSeries.add(series);
			}

			chartPanel.revalidate();
			chartPanel.repaint();
		} catch (Exception e) {
			// Do nothing
		}
	}
}
