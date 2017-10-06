package com.sf.zoe;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.json.JSONObject;

/**
 * @author cmuttoju
 */
public class Linechart extends ApplicationFrame {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	JFreeChart chart;
	XYSeriesCollection dataSet;
	static final int TIME_INTERVAL = 1;
	int currentYimeInterval = 10;

	public Linechart(final String title) {

		super(title);
		dataSet = new XYSeriesCollection();
		dataSet = getDataSet();
		chart = ChartFactory.createXYLineChart("Emotion Index", "Time(in Sec)", "Emotion", dataSet);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		setContentPane(chartPanel);

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.getRenderer().setSeriesPaint(0, new Color(255,69,0));
		plot.getRenderer().setSeriesPaint(1, new Color(0, 0, 255));
		plot.getRenderer().setSeriesPaint(2, new Color(255,0,255));
		plot.getRenderer().setSeriesPaint(3, new Color(0, 255, 0));
		plot.getRenderer().setSeriesPaint(4, new Color(0, 255, 255));
		plot.getRenderer().setSeriesPaint(5, new Color(161, 107, 0));
		plot.getRenderer().setSeriesPaint(6, new Color(119,136,153));

		plot.setBackgroundPaint(new Color(255, 255, 255));
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

		// renderer.setSeriesPaint(0, new Color(225, 225, 225));

		ValueAxis domainAxis = (ValueAxis) plot.getDomainAxis();
		domainAxis.setLowerBound(0.0);
		domainAxis.setUpperBound(currentYimeInterval);
		plot.setDomainAxis(domainAxis);

		ValueAxis rangeAxis = plot.getRangeAxis();
		rangeAxis.setLowerBound(0.0);
		rangeAxis.setUpperBound(1.1);
		plot.setRangeAxis(rangeAxis);
		// addDataPoint(null, 0);
	}

	public static void main(final String[] args) {
		final Linechart demo = new Linechart("Multiple Dataset Demo 1");
		demo.pack();
		RefineryUtilities.centerFrameOnScreen(demo);
		demo.setVisible(true);
	}

	public void addDataPoint(JSONObject obj, int time) {
		if (time > currentYimeInterval) {
			currentYimeInterval = currentYimeInterval + TIME_INTERVAL;
			XYPlot plot = (XYPlot) chart.getPlot();
			ValueAxis domainAxis = (ValueAxis) plot.getDomainAxis();
			domainAxis.setLowerBound(0.0);
			domainAxis.setUpperBound(currentYimeInterval);
			plot.setDomainAxis(domainAxis);

		}
		List<XYSeries> sl = dataSet.getSeries();
		for (XYSeries series : sl) {
			series.add(time, obj.getDouble((String) series.getKey()));
		}
	}

	@Override
	public void dispose() {
		System.out.println("Called Dispose");
		BufferedImage img = chart.createBufferedImage(600, 400);
		File f = new File("./output.png");
		try {
			ImageIO.write(img, "png", f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.dispose();
	}

	static XYSeriesCollection getDataSet() {
		XYSeries happiness = new XYSeries("happiness");
		happiness.add(0, 0);
		/*
		 * happiness.add(2, 0.4); happiness.add(3, 0.5); happiness.add(4, 0.5);
		 * happiness.add(5, 1);
		 */

		XYSeries neutral = new XYSeries("neutral");
		neutral.add(0, 0);
		XYSeries contempt = new XYSeries("contempt");
		contempt.add(0, 0);
		/*
		 * neutral.add(2, 0.4); neutral.add(3, 0.5); neutral.add(4, 0.5); neutral.add(5,
		 * 1);
		 */

		XYSeries sadness = new XYSeries("sadness");
		sadness.add(0, 0);
		/*
		 * sadness.add(2, 0.4); sadness.add(3, 0.5); sadness.add(4, 0.5); sadness.add(5,
		 * 1);
		 */

		XYSeries surprise = new XYSeries("surprise");
		surprise.add(0, 0);

		/*
		 * surprise.add(2, 0.4); surprise.add(3, 0.5); surprise.add(4, 0.5);
		 * surprise.add(5, 1);
		 */

		XYSeries anger = new XYSeries("anger");
		anger.add(0, 0);
		/*
		 * anger.add(20, 0.2); anger.add(30, 0.2); anger.add(40, 0.5); anger.add(50, 1);
		 */

		XYSeries disgust = new XYSeries("disgust");
		disgust.add(0, 0);
		/*
		 * disgust.add(22, 0.4); disgust.add(13, 0.5); disgust.add(14, 0.5);
		 * disgust.add(15, 1);
		 */

		/* Add all XYSeries to XYSeriesCollection */
		// XYSeriesCollection implements XYDataset
		XYSeriesCollection emotionSeries = new XYSeriesCollection();
		// add series using addSeries method
		emotionSeries.addSeries(happiness);
		emotionSeries.addSeries(neutral);
		emotionSeries.addSeries(sadness);
		emotionSeries.addSeries(surprise);
		emotionSeries.addSeries(anger);
		emotionSeries.addSeries(disgust);
		emotionSeries.addSeries(contempt);
		return emotionSeries;
	}

}
