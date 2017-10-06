package com.sf.zoe;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;

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
import com.github.sarxos.webcam.WebcamPanel;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

class CaptureThread implements Runnable {
	private BlockingQueue<ImageVo> queue;
	private volatile boolean stopFlag = false;
	static final String OUT_FILE_NAME = "out.mpg";
	private Webcam webcam;

	public CaptureThread(BlockingQueue<ImageVo> queue, int interval, Webcam webcam) {
		this.queue = queue;
		this.webcam = webcam;

	}

	public void setStopFlag(boolean stopFlag) {
		this.stopFlag = stopFlag;
	}

	@Override
	public void run() {
		try {

			File file = new File(OUT_FILE_NAME);

			IMediaWriter writer = ToolFactory.makeWriter(file.getName());
			Dimension size = new Dimension(640, 480);// WebcamResolution.QVGA.getSize();

			writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG1VIDEO, size.width, size.height);
			// writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, size.width,
			// size.height);

			long start = System.currentTimeMillis();
			long current = start;
			for (int i = 0; !stopFlag; i++) {
				// System.out.println("Capture frame " + i);
				BufferedImage image = ConverterFactory.convertToType(webcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);
				if ((System.currentTimeMillis() - current) > 1000) {
					this.queue.put(new ImageVo(image, System.currentTimeMillis() - start));
					current = System.currentTimeMillis();
				}
				IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);
				IVideoPicture frame = converter.toPicture(image, (System.currentTimeMillis() - start) * 1000);
				frame.setKeyFrame(i == 0);
				frame.setQuality(0);

				writer.encodeVideo(0, frame);

				// 10 FPS
				Thread.sleep(50);
			}
			System.out.println(System.currentTimeMillis() - start);
			writer.close();
			System.out.println("Video recorded in file: " + file.getAbsolutePath());
		} catch (InterruptedException e) {
			// TODO need to add proper logging
			e.printStackTrace();
		}
	}
}

class ProcessThread implements Runnable {
	private BlockingQueue<ImageVo> queue;

	static final String ENDPOINT_URL = "https://westus.api.cognitive.microsoft.com/emotion/v1.0/recognize";
	static final String[] API_KEYS = { "13ae61eaa7064561994fbb26e45798b6", "470b9dbd4fb74dff9ed9545dd40218e9",
			"a204c19086d84ae2b1c65c39834ae78e", "542ce8daf19e4c03bb5214311793a7f3", "46cf72f2590c4dc59ad7823b048c754b",
			"eb51057442a448feb9660ab5da69f266",
			// "abf7e6d8a2624c9eacc33eaad5669eae", "965e8669a92844f0af8c7e9e10aa850a",
			"546f5f12c021414c9328fc531fb698fc", "2ea94f49c940452d8428f8a2eab4f2a9" };
	private URIBuilder uriBuilder;
	private CloseableHttpClient httpclient;
	private volatile boolean stopFlag = false;
	private int interval;

	public void setStopFlag(boolean stopFlag) {
		this.stopFlag = stopFlag;
	}

	public ProcessThread(BlockingQueue<ImageVo> queue, int interval) {
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
			ImageVo bImage;
			String fileName = "";
			// consuming BufferedImages until exit BufferedImage is received
			int requestCount = 1;

			// Creating chart
			Linechart chart = new Linechart("");
			chart.pack();
			RefineryUtilities.centerFrameOnScreen(chart);
			chart.setVisible(true);
			while ((bImage = queue.take()) != null) {
				fileName = String.valueOf(bImage.getTime());
				try {
					// ImageIO.write(bImage.getBi(), "JPG", new File(fileName));
					int timeIndex = (int) (bImage.getTime());
					JSONArray json = processImage(bImage.getBi(), API_KEYS[requestCount % API_KEYS.length]);
					System.out.println(json);
					if (json != null && json.length() > 0) {
						chart.addDataPoint(JsonUtil.getScores(json), timeIndex / 1000);
					}
					requestCount++;
					System.out.println("queue.size(): " + queue.size());
					if (queue.size() == 0 && stopFlag) {
						// break;
					}

				} catch (IOException | URISyntaxException e) {
					System.out.println("Error in saving file in producer");
					e.printStackTrace();
				}
				Thread.sleep(interval);
			}
			chart.dispose();
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
		long start = System.currentTimeMillis();
		HttpResponse response = httpclient.execute(httppost);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			String str = EntityUtils.toString(entity);
			try {
				System.out.println(System.currentTimeMillis() - start);
				// System.exit(0);
				return new JSONArray(str);
			} catch (Exception e) {
				System.out.println("Received Wrong error message: " + str + "\n" + e + " Key: " + key);
			}
		}
		return null;
	}
}

/**
 * 
 * @author cmuttoju
 *
 */
public class ZOEUtil {
	private static final int INTERVAL = 1 * 1000; // in seconds

	public static void main(String[] args) {
		// Creating BlockingQueue of size 10
		JFrame window = new JFrame("Capture Panel");
		BlockingQueue<ImageVo> queue = new ArrayBlockingQueue<>(10);

		Webcam webcam = Webcam.getDefault();
		webcam.setViewSize(new Dimension(640, 480));
		webcam.open(true);

		WebcamPanel panel = new WebcamPanel(webcam);
		panel.setFPSDisplayed(true);
		panel.setDisplayDebugInfo(true);
		panel.setImageSizeDisplayed(true);
		panel.setMirrored(true);
		window.add(panel);
		CaptureThread producer = new CaptureThread(queue, INTERVAL, webcam);
		ProcessThread consumer = new ProcessThread(queue, INTERVAL);

		window.setSize(640, 600);
		window.setLayout(new FlowLayout());
		JButton start = new JButton("START");

		start.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				start.setEnabled(false);
				new Thread(producer).start();
				System.out.println("CaptureThread started!!");
				new Thread(consumer).start();
				System.out.println("ProcessThread started!!");

			}
		});
		JButton stop = new JButton("STOP");
		stop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				producer.setStopFlag(true);
				consumer.setStopFlag(true);
				// window.dispose();

			}
		});
		window.add(start);
		window.add(stop);
		window.setResizable(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.pack();
		window.setVisible(true);

	}

}
