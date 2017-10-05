package com.sf.zoe;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jfree.ui.RefineryUtilities;
import org.json.JSONArray;

import com.github.sarxos.webcam.Webcam;

class CaptureThread implements Runnable {
	final Webcam webcam = Webcam.getDefault();
	private BlockingQueue<BufferedImage> queue;

	private int interval;

	public CaptureThread(BlockingQueue<BufferedImage> queue, int interval) {
		this.queue = queue;
		this.interval = interval;

	}

	@Override
	public void run() {
		try {
			webcam.setViewSize(new Dimension(640, 480));
			System.out.println(webcam.getDevice().getName());
			webcam.open();
			while (true) {
				// ImageIO.write(webcam.getImage(), "JPG", new File(name));
				this.queue.put(webcam.getImage());
				System.out.println("Image added to queue at " + System.currentTimeMillis());
				Thread.sleep(interval);
			}

		} catch (InterruptedException e) {
			// TODO need to add proper logging
			e.printStackTrace();
		}
	}

}

class ProcessThread implements Runnable {
	private BlockingQueue<BufferedImage> queue;
	private int interval;

	static final String ENDPOINT_URL = "https://westus.api.cognitive.microsoft.com/emotion/v1.0/recognize";
	static final String[] API_KEYS = { "13ae61eaa7064561994fbb26e45798b6", "470b9dbd4fb74dff9ed9545dd40218e9",
			"a204c19086d84ae2b1c65c39834ae78e", "542ce8daf19e4c03bb5214311793a7f3" };
	private URIBuilder uriBuilder;
	private CloseableHttpClient httpclient;

	public ProcessThread(BlockingQueue<BufferedImage> queue, int interval) {
		this.queue = queue;
		this.interval = interval;
		try {
			this.uriBuilder = new URIBuilder(ENDPOINT_URL);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		httpclient = HttpClients.createDefault();
	}

	@Override
	public void run() {
		try {
			BufferedImage bImage;
			String fileName = "";
			// consuming BufferedImages until exit BufferedImage is received
			int requestCount = 1;

			// Creating chart
			Linechart chart = new Linechart("Multiple Dataset Demo 1");
			chart.pack();
			RefineryUtilities.centerFrameOnScreen(chart);
			chart.setVisible(true);
			long currTime = System.currentTimeMillis();
			while ((bImage = queue.take()) != null) {
				fileName = String.valueOf(System.currentTimeMillis());
				try {
					ImageIO.write(bImage, "JPG", new File(fileName));
					System.out.println("********" + requestCount % API_KEYS.length);
					int timeIndex = (int) (System.currentTimeMillis() - currTime);
					JSONArray json = processImage(bImage, API_KEYS[requestCount % API_KEYS.length]);
					System.out.println(json);
					if (json.length() > 0) {
						chart.addDataPoint(JsonUtil.getScores(json), timeIndex / 1000);
					}
					requestCount++;
				} catch (IOException | URISyntaxException e) {
					System.out.println("Error in saving file in producer");
					e.printStackTrace();
				}
				Thread.sleep(interval);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private JSONArray processImage(BufferedImage bufferedImage, String key) throws URISyntaxException, IOException {

		URI uri = uriBuilder.build();
		HttpPost httppost = new HttpPost(uri);
		// File file = new File("/Users/cmuttoju/Documents/Untitled.png");
		// FileBody bin = new FileBody(new
		// File("/Users/cmuttoju/Documents/Untitled.png"));
		httppost.setHeader("Content-Type", "application/octet-stream");
		httppost.setHeader("Ocp-Apim-Subscription-Key", key);

		// String encodedfile = encodeFileToBase64Binary(new
		// File("/Users/cmuttoju/Documents/Untitled.png"));

		// BufferedImage bufferedImage = ImageIO.read(file);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(bufferedImage, "jpg", baos);
		} catch (IOException ex) {
		}
		byte[] img = baos.toByteArray();
		ByteArrayEntity reqEntity = new ByteArrayEntity(img);

		httppost.setEntity(reqEntity);

		HttpResponse response = httpclient.execute(httppost);
		HttpEntity entity = response.getEntity();
		if (entity != null) {

			String str = EntityUtils.toString(entity);
			try {
				return new JSONArray(str);
			} catch (Exception e) {
				System.out.println("Received Wrong error message: " + str + "\n" + e);
			}
		}
		return null;
	}
}

public class ZOEUtil {
	private static final int INTERVAL = 1 * 1000; // in seconds

	public static void main(String[] args) {
		// Creating BlockingQueue of size 10
		BlockingQueue<BufferedImage> queue = new ArrayBlockingQueue<>(10);
		CaptureThread producer = new CaptureThread(queue, INTERVAL);
		ProcessThread consumer = new ProcessThread(queue, INTERVAL);
		new Thread(producer).start();
		System.out.println("CaptureThread started!!");
		new Thread(consumer).start();
		System.out.println("ProcessThreadß started!!");
	}

}