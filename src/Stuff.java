import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

class Stuff {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static void main(String[] args) {
		
		Mat frame = new Mat();
		Mat blurred = new Mat();
		Mat converted = new Mat();
		Mat mask = new Mat();
		Mat gray = new  Mat();
		Mat edges = new Mat();
		
		frame = Imgcodecs.imread("circle.png");
		Rect rect = new Rect(1,1,5,5);
	
		Imgproc.blur(frame, blurred, new Size(10,10));
		Imgcodecs.imwrite("blurred.png", blurred);
		Imgproc.cvtColor(blurred, converted, Imgproc.COLOR_BGR2HLS);
		Imgcodecs.imwrite("converted.png", converted);
	
	    
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
	
		Imgproc.cvtColor(converted,gray,Imgproc.COLOR_BGR2GRAY);
		

		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 23));
		
		Imgproc.erode(gray, gray, erodeElement);
		Imgproc.erode(gray, gray, erodeElement);
		
		Imgproc.dilate(gray, gray, dilateElement);
		Imgproc.dilate(gray, gray, dilateElement);
		
		Imgcodecs.imwrite("gray.png", gray);
		
		Imgproc.Canny(gray, edges, 1, 100);
		
		Imgcodecs.imwrite("edges.png", edges);
		Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
		
		System.out.println(contours.size());
		System.out.println(contours.get(0));
		Point[] points = contours.get(0).toArray();
		System.out.println(points.length);
		
		for(int i = 0; i < points.length; i++) {
			System.out.println(points[i]);
		}
		
		Imgproc.drawContours(converted, contours, 0, new Scalar(0,0,255));
		Imgcodecs.imwrite("contours.png", converted);
		
		
		
		
	}
	
}