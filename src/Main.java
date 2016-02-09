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
import org.opencv.videoio.VideoCapture;

public class Main {

	public static void main(String[] args) {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		VideoCapture camera = new VideoCapture();
		camera.open(1);
		Mat frame = new Mat();
		camera.read(frame);
		findGoal(camera);
		
		/*
		Mat original = new Mat();
		Mat converted = new Mat();
		Mat colorsCanceled = new Mat();
		
		original = Imgcodecs.imread("tower/8ftcenter.jpg");
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
		System.out.println(rects.get(0).width);
		System.out.println(findDistance(57.2,rects.get(0).width,6,20));
		
		for(int i = 0; i < rects.size(); i++) {
			Imgcodecs.imwrite("images/submat" + i + ".png", makeSubmats(original, findRects(contours)).get(i));
		}
		*/
	}
	
	public static void convertImage(Mat input, Mat output) {
		Imgproc.blur(input, output, new Size(5,5));
		Imgproc.cvtColor(output, output, Imgproc.COLOR_BGR2HSV);
		
		Imgproc.erode(output, output, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)));
		Imgproc.dilate(output, output, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)));
		
		Imgproc.dilate(output, output, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)));
		Imgproc.erode(output, output, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)));
		
		Imgproc.blur(output, output, new Size(5,5));
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
		List<MatOfPoint> newContours = new ArrayList<MatOfPoint>();
		for(int i = 0; i < contours.size(); i++) {
			Rect rect = Imgproc.boundingRect(contours.get(i));
			if(rect.width > 90 && rect.width < 200 
					&& rect.height > 30 && rect.height < 100
					&& rect.y < 400) {
				newContours.add(contours.get(i));
			}
		}
		return newContours;
	}
	
	//find approximate point vertices of contoured goal tape
	public static List<MatOfPoint2f> approxPoly(List<MatOfPoint> contours) {
		List<MatOfPoint2f> points = new ArrayList<MatOfPoint2f>();
		for(int i = 0; i < contours.size(); i++) {
			MatOfPoint2f temp = new MatOfPoint2f();
			MatOfPoint2f tempOut = new MatOfPoint2f();
			temp.fromList(contours.get(i).toList());
			Imgproc.approxPolyDP(temp, tempOut, 7.0, false); //third parameter: smaller->more points
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
			if(rect.width > 90 && rect.width < 200 
				&& rect.height > 30 && rect.height < 100
				&& rect.y < 400) {
				rects.add(rect);
			}
		}
		return rects;
	}
	
	// scalar params: HSV
	public static void cancelColors(Mat input, Mat output) {
		Core.inRange(input, new Scalar(25, 0, 220), new Scalar(130, 80, 255), output);
	}
	
	public static List<Mat> makeSubmats(Mat input, List<Rect> rects) {
		List<Mat> submats = new ArrayList<Mat>();
		for(int i = 0; i < rects.size(); i++) {
			submats.add(input.submat(rects.get(i)));
		}
		return submats;
	}
	
	//finds bottom vertices of goal tape
	public static Point[] findBottomY(Point[] points) {
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
		Point[] highestYCoords = {highestY, secondHighestY};
		return highestYCoords;
	}
	
	public static double dist(Point[] coords) {
		double dist = Math.sqrt((coords[0].x - coords[1].x)*(coords[0].x - coords[1].x) + (coords[0].x - coords[1].y)*(coords[0].y - coords[1].y));
		return dist;
	}
	
	//for Dylan's second dist formula
	public static double pointYAvg(Point[] points) {
		double avg = (points[0].y + points[1].y)/2;
		return avg;
	}
	
	//params: tapeHeight is height of tape in pixels, tapeBottomY is average bottom y coord of tape, camAngle is upward angle of camera 
	public static double findDistance(double vertFOV, double tapeHeight, double tapeBottomY, double camAngle) {
		vertFOV = Math.toRadians(vertFOV);
		camAngle = Math.toRadians(camAngle);
		double realHeight = 14; // inches
		double imageHeight = 480; // pixels
		
		double distance = (realHeight * ((imageHeight/2)/(tapeHeight))) / Math.tan(vertFOV/2.0); //Dylan's first dist formula
		//double distance = (realHeight * ((((vertFOV/2 + camAngle)/vertFOV) * imageHeight)/tapeHeight)) / Math.tan(vertFOV/2 + camAngle); //our dist formula
		//double distance = (realHeight * ((imageHeight/2)/(tapeHeight))) / Math.sin(vertFOV/2) * Math.sin(90 - vertFOV/2); //Justin's dist formula (law of sines)
		
		double distError = Math.log10(distance/61.223) / Math.log10(1.0056); //power function of error of first distance formula
		
		//Dylan's second dist formula
//		double topAvg = horizonY - (tapeBottomY - tapeHeight); //distance from horizon to top of goal
//		double bottomAvg = horizonY - tapeBottomY; //distance from horizon to bottom of goal
//		double imageTop = (imageHeight - horizonY) / Math.cos((imageHeight-(imageHeight-horizonY) * (imageHeight)/vertFOV));
//		double imageBottom = horizonY;
//		double goalTop = topAvg / Math.cos(horizonY-topAvg * (imageHeight)/vertFOV);
//		double goalBottom = bottomAvg / Math.cos(horizonY-bottomAvg * (imageHeight)/vertFOV);
//		double distance = realHeight * (((imageTop - imageBottom)/(goalTop - goalBottom))/(Math.tan(vertFOV - camAngle)));
	
		return distError;
	}
	
	//finds real height based off of observed width
	public static double findRealHeight(double width) {
		return width * 0.7; //ratio from height to width is 14/20 (0.7)
	}
	
	//finds angle at which camera is facing the goal
	public static double findLateralAngle(Point[] points) {
		double angle = Math.toDegrees(Math.asin(Math.abs((points[0].y - points[1].y)/dist(points))));
		return angle;
	}
	
	//finds position of camera off midline of goal
	public static boolean isOnRight(Point[] points) {
		if(points[0].y > points[1].y) {
			return points[0].x < points[1].x;
		}
		return points[0].x > points[1].x;
	}
	
	public static void findGoal(VideoCapture camera) {
		while(true) {
			Mat frame = new Mat();
			Mat output = new Mat();
			camera.read(frame);
			Imgcodecs.imwrite("original.png", frame);
			convertImage(frame, output);
			Imgcodecs.imwrite("converted.png", output);
			cancelColors(output, output);
			Imgcodecs.imwrite("colors.png", output);
			
			List<MatOfPoint> contours = findContours(output);
			List<Rect> rects = findRects(contours);
			
			contours = filterContours(contours);			
			List<MatOfPoint2f> points2f = approxPoly(contours);
			
			if(points2f.size() >= rects.size()){
				for(int i = 0; i < rects.size(); i++) {
					Imgcodecs.imwrite("submat" + i + ".png", makeSubmats(frame, rects).get(i));
					Point[] bottomY = findBottomY(points2f.get(i).toArray());
					System.out.println("dist: " + findDistance(44.44, findRealHeight(rects.get(i).width), rects.get(i).y + rects.get(i).height, 31));
					System.out.println("lat angle: " + findLateralAngle(bottomY));
					System.out.println("isOnRight : " + isOnRight(bottomY));
				}
			}
		}
	}
}