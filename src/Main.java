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

	final static double VERT_FOV = Math.toRadians(44.44);
	final static double REAL_TAPE_HEIGHT = 14; // inches of real tape height
	final static double IMG_HEIGHT = 480; // pixels of image resolution
	final static double IMG_WIDTH = 640; // pixels of image resolution
	
	public static void main(String[] args) {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		VideoCapture camera = new VideoCapture();
		camera.open(1);
		Mat frame = new Mat();
		camera.read(frame);
		findGoal(camera);
		
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
	
	public static MatOfPoint filterContours(List<MatOfPoint> contours) {
		List<Rect> rects = new ArrayList<Rect>();
		int lrgstRectIndx = 0;
		for(int i = 0; i < contours.size(); i++) {
			Rect rect = Imgproc.boundingRect(contours.get(i));
			if(rect.width > 90 && rect.width < 200 
					&& rect.height > 30 && rect.height < 100
					&& rect.y < 400) {
				rects.add(rect);
				if(rects.get(i).width > rects.get(lrgstRectIndx).width) {
					lrgstRectIndx = i;
				}
			}
		}
		return contours.get(lrgstRectIndx);
	}
	
	//find approximate point vertices of contoured goal tape
	public static MatOfPoint2f approxPoly(MatOfPoint contour) {
		MatOfPoint2f point2f = new MatOfPoint2f();
		List<Point> points = contour.toList();
		point2f.fromList(points);
		Imgproc.approxPolyDP(point2f, point2f, 7.0, false); //third parameter: smaller->more points
		return point2f;
	} 
	
	public static List<Rect> findRect(List<MatOfPoint> contours) {
		List<Rect> rects = new ArrayList<Rect>();
		for(int i = 0; i < contours.size(); i++) {
			Rect rect = Imgproc.boundingRect(contours.get(i));
			rects.add(rect);
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
	
	public static double pointDist(Point[] points) {
		double dist = Math.sqrt((points[0].x - points[1].x)*(points[0].x - points[1].x) + (points[0].x - points[1].y)*(points[0].y - points[1].y));
		return dist;
	}
	
	public static double findDistToGoal(double tapeHeight, double camAngle) {
		camAngle = Math.toRadians(camAngle);
		
		double distance = (REAL_TAPE_HEIGHT * ((IMG_HEIGHT/2)/(tapeHeight))) / Math.tan(VERT_FOV/2.0); //Dylan's first dist formula
		//double distance = (REAL_TAPE_HEIGHT * ((((VERT_FOV/2 + camAngle)/VERT_FOV) * IMG_HEIGHT)/tapeHeight)) / Math.tan(VERT_FOV/2 + camAngle); //our dist formula
		double distError = Math.log10(distance/61.223) / Math.log10(1.0056); //power function of error of first distance formula
		return distError;
	}
	
	//finds real height based off of observed width
	public static double findRealHeight(double width) {
		return width * 0.7; //ratio from height to width is 14/20 (0.7)
	}
	
	//finds angle at which camera is facing the goal
	public static double findLateralAngle(Point[] points) {
		double angle = Math.toDegrees(Math.asin(Math.abs((points[0].y - points[1].y)/pointDist(points))));
		return angle;
	}
	
	//if true then robot turns right to face goal straight on
	public static boolean isFacingLeft(Point[] points) {
		if(points[0].y > points[1].y) {
			return points[0].x < points[1].x;
		}
		return points[0].x > points[1].x;
	}
	
	//if true then robot is on right of midline
	public static boolean isOnRight(Rect rect) {
		return (rect.x + rect.width/2) < IMG_WIDTH/2;
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
			MatOfPoint contour = filterContours(contours);			
			Rect goal = Imgproc.boundingRect(contour);
			MatOfPoint2f points2f = approxPoly(contour);
			
			Imgcodecs.imwrite("submat" + ".png", frame.submat(goal));
			Point[] bottomY = findBottomY(points2f.toArray());
			System.out.println("dist: " + findDistToGoal(findRealHeight(goal.width), 31));
			System.out.println("lat angle: " + findLateralAngle(bottomY));
			System.out.println("isOnRight : " + isFacingLeft(bottomY));
			
		}
	}
}