import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
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
		
		original = Imgcodecs.imread("tower/10ftcenter2.jpg");
		convertImage(original, converted);
		Imgcodecs.imwrite("images/converted.png", converted);
		
		cancelColors(converted, colorsCanceled);
		Imgcodecs.imwrite("images/colorcancel.png", colorsCanceled);

		List<MatOfPoint> contours = findContours(colorsCanceled);
		//contours = filterContours(contours);
		
		List<MatOfPoint2f> points2f = approxPoly(contours);
		System.out.println(points2f.size());
		System.out.println(points2f.get(0).size());
		
		for(int i = 0; i < points2f.get(0).toList().size(); i++) {
			System.out.println(points2f.get(0).toList().get(i));
		}
		
		List<Rect> rects = findRects(contours);
		System.out.println(rects.size());
		
		for(int i = 0; i < rects.size(); i++) {
			Imgcodecs.imwrite("images/submat" + i + ".png", makeSubmats(original, findRects(contours)).get(i));
		}
	}
	
	public static void convertImage(Mat input, Mat output) {
		Imgproc.blur(input, output, new Size(5,5));
		Imgproc.cvtColor(output, output, Imgproc.COLOR_BGR2HSV);
		
		Imgproc.erode(output, output, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)));
		Imgproc.dilate(output, output, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)));
		
		Imgproc.dilate(output, output, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(50, 50)));
		Imgproc.erode(output, output, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(50, 50)));
		
		Imgproc.blur(output, output, new Size(5,5));
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
	
	public static List<MatOfPoint> filterContours(List<MatOfPoint> contours) {
		for(int i = 0; i < contours.size(); i++) {
			Rect rect = Imgproc.boundingRect(contours.get(i));
			if(rect.width > 200 && rect.height > 100) {
				contours.remove(i);
			}
		}
		return contours;
	}
	
	public static List<MatOfPoint2f> approxPoly(List<MatOfPoint> contours) {
		List<MatOfPoint2f> points = new ArrayList<MatOfPoint2f>();
		for(int i = 0; i < contours.size(); i++) {
			MatOfPoint2f temp = new MatOfPoint2f();
			MatOfPoint2f tempOut = new MatOfPoint2f();
			temp.fromList(contours.get(i).toList());
			Imgproc.approxPolyDP(temp, tempOut, 50.0, true);
			if(tempOut.toList().size() > 3 && tempOut.toList().size() < 10) {
				points.add(tempOut);
			}
		}
		return points;
	}
	
	public static List<Rect> findRects(List<MatOfPoint> contours) {
		List<Rect> rects = new ArrayList<Rect>();
		for(int i = 0; i < contours.size(); i++) {
			Rect rect = Imgproc.boundingRect(contours.get(i));
			if(rect.width > 200 && rect.height > 100) {
				rects.add(rect);
			}
		}
		return rects;
	}
	
	public static void cancelColors(Mat input, Mat output) {
		Core.inRange(input, new Scalar(40, 0, 100), new Scalar(120, 100, 255), output);
	}
	
	public static List<Mat> makeSubmats(Mat input, List<Rect> rects) {
		List<Mat> submats = new ArrayList<Mat>();
		for(int i = 0; i < rects.size(); i++) {
			submats.add(input.submat(rects.get(i)));
		}
		return submats;
	}
	
	public static double findGoalWidth(Point[] points) {
		Point highestY = points[0];
		Point secondHighestY = points[1];
		for(int i = 2; i < points.length; i++) {
			if(points[i].y > highestY.y) {
				highestY = points[i];
			}
			else if(points[i].y > secondHighestY.y) {
				secondHighestY = points[i];
			}
		}
		double width = Math.sqrt((highestY.x - secondHighestY.x)*(highestY.x - secondHighestY.x) + (highestY.x - secondHighestY.y)*(highestY.y - secondHighestY.y));
		return width;
	}
	
	public static double findDistance(double horzFOV, double goalWidth) {
		double realWidth = 20; // inches
		double imageWidth = 800; // pixels
		double distance = (realWidth * ((imageWidth/2)/goalWidth)) / Math.tan(Math.toRadians(horzFOV / 2.0));
		return distance;
	}
}