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

public class Main {

	public static void main(String[] args) {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		Mat original = new Mat();
		Mat converted = new Mat();
		Mat colorsCanceled = new Mat();
		Mat edges = new Mat();
		
		original = Imgcodecs.imread("images/brighttape.png");
		convertImage(original, converted);
		Imgcodecs.imwrite("images/converted.png", converted);
		
		cancelColors(converted, colorsCanceled);
		Imgcodecs.imwrite("images/colorcancel.png", colorsCanceled);
		
		drawEdges(converted, edges);
		Imgcodecs.imwrite("images/edges.png", edges);

		List<MatOfPoint> contours = findContours(edges);
		//printContourPoints(contours);

		System.out.println(findRects(contours).size());
		
		drawContours(original, contours);
		Imgcodecs.imwrite("images/contours.png", original);
		
		Imgcodecs.imwrite("images/submat.png", original.submat(findRects(contours).get(0)));
		
		
	}
	
	public static void convertImage(Mat input, Mat output) {
		Imgproc.blur(input, output, new Size(5,5));
		Imgproc.cvtColor(output, output, Imgproc.COLOR_BGR2HSV);
		/*
		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(100, 50));
		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(40, 40));
		
		Imgproc.erode(output, output, erodeElement);
		Imgproc.erode(output, output, erodeElement);
		
		Imgproc.dilate(output, output, dilateElement);
		Imgproc.dilate(output, output, dilateElement);
		*/
		
		Imgproc.erode(output, output, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)));
		Imgproc.dilate(output, output, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)));
		
		Imgproc.dilate(output, output, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(50, 50)));
		Imgproc.erode(output, output, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(50, 50)));
		
		Imgproc.blur(output, output, new Size(5,5));
	}
	
	public static void printContourPoints(List<MatOfPoint> contours) {
		List<Point> points = new ArrayList<>();
		for(int i = 0; i < contours.size(); i ++) {
			for(int x = 0; x < contours.get(i).toList().size(); x++) {
				points.add(contours.get(i).toList().get(x));
			}
		}		
		for(int i = 0; i < points.size(); i++) {
			System.out.println(points.get(i));
		}
	}
	
	public static void drawEdges(Mat input, Mat output) {
		Imgproc.Canny(input, output, 1, 100);
	}
	
	public static List<MatOfPoint> findContours(Mat image) {
		List<MatOfPoint> contours = new ArrayList<>();
		Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
		return contours;
	}
	
	public static void drawContours(Mat image, List<MatOfPoint> contours) {
		for(int i = 0; i < contours.size(); i++) {
			Imgproc.drawContours(image, contours, i, new Scalar(0,0,255));
		}
	}
	
	public static List<Rect> findRects(List<MatOfPoint> contours) {
		List<Rect> rects = new ArrayList<Rect>();
		for(int i = 0; i < contours.size(); i++) {
			Rect rect = Imgproc.boundingRect(contours.get(i));
			if(rect.width > 50 && rect.height > 100) {
				rects.add(rect);
				System.out.println("x: " + rect.x + "y: " + rect.y + "width: " + rect.width + "height: " + rect.height);
			}
		}
		return rects;
	}
	
	public static void cancelColors(Mat input, Mat output) {
		Core.inRange(input, new Scalar(100, 0, 0), new Scalar(180, 255, 255), output);
	}
	
	/*
	public static boolean isRectWhite(Mat image, Rect rect) {
		
	}
	*/
	
}