package com.htmlhifive.pitalium.explorer.image;

import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.awt.Color;
import java.lang.Math;

/**
 * Utility class to calculate similarity.
 */
public class SimilarityUtils {

	/**
	 * Extract the feature matrix of size FeatureRow by FeatureCol from image.
	 * @param FeatureRow the row size of feature matrix
	 * @param FeatureCol the column size of feature matrix
	 */
	private static int FeatureRow = 5;
	private static int FeatureCol = 5;
	
	
	/*
	 * Set the way to calculate norm.
	 * if true, use the average of norm,
	 * otherwise, use the norm of color distances.
	 */
	public static boolean averageNorm = false;

	/**
	 * Constructor
	 */
	public SimilarityUtils(){} 

	/**
	 * calculate similarity of given rectangle area and offset
	 * @param expectedImage
	 * @param actualImage
	 * @param rectangle
	 * @param similarRectangle
	 * @param offset
	 * @return similarity using norm calculation pixel by pixel  
	 */
	public static double calcSimilarity(BufferedImage expectedImage,BufferedImage actualImage, Rectangle rectangle, ComparedRectangle similarRectangle, Offset offset) {
		
		Offset featureOffset = offset;
		double similarityPixelByPixel, similarityFeatureMatrix=-1;
		SimilarityUnit similarityUnit = new SimilarityUnit();
		similarityPixelByPixel = calcSimilarityPixelByPixel(expectedImage, actualImage, rectangle, similarityUnit, offset);


		/* calculate similarity using feature matrix. */
		int comparedRectangleWidth = (int)rectangle.getWidth(), comparedRectangleHeight = (int)rectangle.getHeight();

		// execute this only when comparedRectangleWidth >= FeatureCol && comparedRectangleHeight >= FeatureRow
		if (SimilarityUtils.checkFeatureSize(comparedRectangleWidth, comparedRectangleHeight)) {
			similarityFeatureMatrix = calcSimilarityByFeatureMatrix(expectedImage, actualImage, rectangle, featureOffset);
		}
		similarityUnit.setSimilarityFeatureMatrix(similarityFeatureMatrix);

		String category = categorize(similarityUnit);	
		
		// check strict scaling
		/* TODO
		if (category == "SCALING") {
			boolean scale = ShiftUtils.checkScaling(expectedImage, actualImage, rectangle);
		}
		*/
		
		similarRectangle.setType(category);	
		similarRectangle.setSimilarityUnit(similarityUnit);

		return similarityPixelByPixel;
	}

	private static String categorize(SimilarityUnit similarityUnit) {
		
		/* Check scaling */

		double similarityThresDiff, similarityTotalDiff, similarityFeatureMatrix;
		double scalingDiffCriterion = ComparisonParameters.getScalingDiffCriterion();
		double scalingFeatureCriterion = ComparisonParameters.getScalingFeatureCriterion();
				
		similarityThresDiff = similarityUnit.getSimilarityThresDiff();
		similarityTotalDiff = similarityUnit.getSimilarityTotalDiff();
		similarityFeatureMatrix = similarityUnit.getSimilarityFeatureMatrix();
		if (similarityThresDiff - similarityTotalDiff >= scalingDiffCriterion && similarityFeatureMatrix >= scalingFeatureCriterion) {
			return "SCALING";
		}

		return "SIMILAR";
	}

	/**
	 * Calculate the distance of two feature matrices.
	 * @param expectedFeature The feature matrix of expected sub image
	 * @param actualFeature The feature matrix of actual sub image
	 * @return the norm of distance between two matrices
	 */
	public static double calcFeatureDistance(Color[][] expectedFeature, Color[][] actualFeature) {
		// the difference of Red, Green, and Blue.
		int r, g, b;
		double dist=0;
		for (int row=0; row<FeatureRow; row++) {
			for (int col=0; col<FeatureCol; col++) {
				r = expectedFeature[row][col].getRed() - actualFeature[row][col].getRed();			
				g = expectedFeature[row][col].getGreen() - actualFeature[row][col].getGreen();			
				b = expectedFeature[row][col].getBlue() - actualFeature[row][col].getBlue();			
				dist += r*r + g*g + b*b;
			}
		}

		// normalize and return.
		return Math.sqrt(dist)/(Math.sqrt(3*FeatureRow*FeatureCol)*255);
	}

	/** 
	 * Check if the size of rectangle is large enough to use feature method.
	 * @param width the width of compared rectangle
	 * @param heigth the height of compared rectangle
	 * @return if the rectangle is large enough, return true.
	 */
	public static boolean checkFeatureSize (int width, int height) {
		return width >= FeatureCol && height >= FeatureRow ;
	}

	/**
	 * Calculate the similarity using feature matrix and find the best match where it has the highest similarity
	 * This method should be implemented only when the size of actualSubImage is greater than or equal to FeatureCol by FeatureRow.
	 * @param expectedSubImage the sub-image of given rectangle area of expected image
	 * @param actualSubImage the sub-image of given 'template' rectangle area of actual image. it is smaller than expectedSubImage. 
	 * @param rectangle The rectangle area where to compare.
	 * @return the 'feature' similarity of given area between two images.
	 */
	public static double calcSimilarityByFeatureMatrix(BufferedImage expectedImage, BufferedImage actualImage, Rectangle rectangle, Offset offset) {


		// set range to be checked
		int minWidth  = Math.min(expectedImage.getWidth(),  actualImage.getWidth()),
			minHeight = Math.min(expectedImage.getHeight(), actualImage.getHeight());
		int actualX = (int)rectangle.getX(), 		 actualY = (int)rectangle.getY(),
			actualWidth = (int)rectangle.getWidth(), actualHeight = (int) rectangle.getHeight();
		int maxMove;
		if (offset == null) {	
			maxMove = ComparisonParameters.getMaxMove();
			offset = new Offset(0,0);
		} else {
			maxMove = 0;
		}
		int leftMove = Math.min(maxMove, actualX-1),
			rightMove = Math.min(maxMove, minWidth-(actualX+actualWidth)),
			topMove = Math.min(maxMove, actualY-1),
			downMove = Math.min(maxMove, minHeight-(actualY+actualHeight));
		int expectedX = actualX-leftMove-offset.getX(), expectedY = actualY-topMove-offset.getY(),
			expectedWidth = actualWidth+leftMove+rightMove, expectedHeight = actualHeight+topMove+downMove;
		
		// initialize sub-image.
		Rectangle entireFrame = new Rectangle(expectedX, expectedY, expectedWidth, expectedHeight);
		BufferedImage expectedSubImage = ImageUtils2.getSubImage(expectedImage, entireFrame);
		BufferedImage actualSubImage = ImageUtils2.getSubImage(actualImage, rectangle);
		
		// initialize the color array.
		int[] expectedColors = new int[expectedWidth * expectedHeight];
		int[] actualColors = new int[actualWidth * actualHeight];

		expectedSubImage.getRGB(0, 0, expectedWidth, expectedHeight, expectedColors, 0, expectedWidth);
		actualSubImage.getRGB(0, 0, actualWidth, actualHeight, actualColors, 0, actualWidth);

		int[] expectedRed = new int[expectedColors.length];
		int[] expectedGreen = new int[expectedColors.length];
		int[] expectedBlue = new int[expectedColors.length];
		int[] actualRed = new int[actualColors.length];
		int[] actualGreen = new int[actualColors.length];
		int[] actualBlue = new int[actualColors.length];

		for (int i = 0; i < expectedColors.length; i++) {
			Color expectedColor = new Color(expectedColors[i]);
			expectedRed[i] = expectedColor.getRed();
			expectedGreen[i] = expectedColor.getGreen();
			expectedBlue[i] = expectedColor.getBlue();
		}

		for (int i = 0; i < actualColors.length; i++) {
			Color actualColor = new Color(actualColors[i]);
			actualRed[i] = actualColor.getRed();
			actualGreen[i] = actualColor.getGreen();
			actualBlue[i] = actualColor.getBlue();
		}


		/* Calculate the feature matrix of actual sub-image. */

		// the size of grid.
		int GridWidth = actualWidth/FeatureCol, GridHeight = actualHeight/FeatureRow;
		int GridArea = GridWidth * GridHeight;

		Color[][] actualFeature = new Color[FeatureRow][FeatureCol];
		for (int row=0; row<FeatureRow; row++) {
			for (int col=0; col<FeatureCol; col++) {

				// Sum of Red, Green, and Blue.
				int rSum=0, gSum=0, bSum=0;

				// Calculate the feature value actualFeature[row][col].
				for (int i=0; i<GridHeight; i++) {
					for (int j=0; j<GridWidth; j++) {
						rSum += actualRed[actualWidth*(GridHeight*row + i) + (GridWidth*col + j)];
						gSum += actualGreen[actualWidth*(GridHeight*row + i) + (GridWidth*col + j)];
						bSum += actualBlue[actualWidth*(GridHeight*row + i) + (GridWidth*col + j)];
					}
				}

				actualFeature[row][col] = new Color((int)(rSum/GridArea), (int)(gSum/GridArea), (int)(bSum/GridArea));
			}
		}

		/* Calculate the feature matrix of expected subimage.*/

		Color[][] expectedFeature = new Color[FeatureRow][FeatureCol];


		int bestX=0, bestY=0;
		double dist=0, min=-1;

		// Find the best match moving sub-image.
		for (int y = 0; y <= topMove + downMove; y++) {
			for (int x = 0; x <= leftMove + rightMove; x++) {

				// Calculate the distance between the expected feature matrix and the actual feature matrix shifhted (x, y).
				for (int row=0; row<FeatureRow; row++) {
					for (int col=0; col<FeatureCol; col++) {

						// Sum of Red, Green, and Blue.
						int rSum=0, gSum=0, bSum=0;

						// Calculate the feature value expectedFeature[row][col].
						for (int i=0; i<GridHeight; i++) {
							for (int j=0; j<GridWidth; j++) {
								rSum += expectedRed[expectedWidth*(GridHeight*row + (y+i)) + (GridWidth*col + (x+j))];
								gSum += expectedGreen[expectedWidth*(GridHeight*row + (y+i)) + (GridWidth*col + (x+j))];
								bSum += expectedBlue[expectedWidth*(GridHeight*row + (y+i)) + (GridWidth*col + (x+j))];
							}
						}
						expectedFeature[row][col] = new Color((int)(rSum/GridArea), (int)(gSum/GridArea), (int)(bSum/GridArea));
					}
				}

				// Calculate the feature distance at each shift (x, y).
				dist = calcFeatureDistance(expectedFeature, actualFeature);

				// Find the best match.
				if (dist < min || min==-1)
				{
					min = dist;
					// offset (from expected to actual) of best match
					bestX = leftMove -x ;
					bestY = topMove - y;
				}
			}
		}

		if (maxMove != 0) {
			offset.setX(bestX);
			offset.setY(bestY);
		}
		double similarity = 1-min;
		
		// round similarity to 2 decimal places.
		similarity = (double)Math.round(similarity*100)/100;
		
		return similarity;
	}

	/**
	 * Calculate the similarity by comparing two images pixel by pixel,
	 * and find the best match where it has the highest similarity.
	 * In this method, we count the number of different pixels as well.
	 * @param expectedSubImage the sub-image of given rectangle area of expected image
	 * @param actualSubImage the sub-image of given 'template' rectangle area of actual image. it is smaller than expectedSubImage. 
	 * @param rectangle The rectangle area where to compare.
	 * @param similarityUnit
	 * @param offset best match offset. If default offset is given, don't find the best match.
	 * @return the 'pixel by pixel' similarity of given area between two images.
	 */
	public static double calcSimilarityPixelByPixel(BufferedImage expectedImage,BufferedImage actualImage, Rectangle rectangle,	SimilarityUnit similarityUnit, Offset offset) {

		// set range to be checked
		int minWidth  = Math.min(expectedImage.getWidth(),  actualImage.getWidth()),
			minHeight = Math.min(expectedImage.getHeight(), actualImage.getHeight());
		int actualX = (int)rectangle.getX(), 		 actualY = (int)rectangle.getY(),
			actualWidth = (int)rectangle.getWidth(), actualHeight = (int) rectangle.getHeight();
		int maxMove;
		if (offset == null) {	
			maxMove = ComparisonParameters.getMaxMove();
			offset = new Offset(0,0);
		} else {
			maxMove = 0;
		}
		int leftMove = Math.min(maxMove, actualX-1),
			rightMove = Math.min(maxMove, minWidth-(actualX+actualWidth)),
			topMove = Math.min(maxMove, actualY-1),
			downMove = Math.min(maxMove, minHeight-(actualY+actualHeight));
		int expectedX = actualX-leftMove-offset.getX(), expectedY = actualY-topMove-offset.getY(),
			expectedWidth = actualWidth+leftMove+rightMove, expectedHeight = actualHeight+topMove+downMove;
		
		// initialize sub-image.
		Rectangle entireFrame = new Rectangle(expectedX, expectedY, expectedWidth, expectedHeight);
		BufferedImage expectedSubImage = ImageUtils2.getSubImage(expectedImage, entireFrame);
		BufferedImage actualSubImage = ImageUtils2.getSubImage(actualImage, rectangle);
	
		// initialize the color array.
		int[] expectedColors = new int[expectedWidth * expectedHeight];
		int[] actualColors = new int[actualWidth * actualHeight];

		expectedSubImage.getRGB(0, 0, expectedWidth, expectedHeight, expectedColors, 0, expectedWidth);
		actualSubImage.getRGB(0, 0, actualWidth, actualHeight, actualColors, 0, actualWidth);

		int[] expectedRed = new int[expectedColors.length];
		int[] expectedGreen = new int[expectedColors.length];
		int[] expectedBlue = new int[expectedColors.length];
		int[] actualRed = new int[actualColors.length];
		int[] actualGreen = new int[actualColors.length];
		int[] actualBlue = new int[actualColors.length];

		for (int i = 0; i < expectedColors.length; i++) {
			Color expectedColor = new Color(expectedColors[i]);
			expectedRed[i] = expectedColor.getRed();
			expectedGreen[i] = expectedColor.getGreen();
			expectedBlue[i] = expectedColor.getBlue();
		}

		for (int i = 0; i < actualColors.length; i++) {
			Color actualColor = new Color(actualColors[i]);
			actualRed[i] = actualColor.getRed();
			actualGreen[i] = actualColor.getGreen();
			actualBlue[i] = actualColor.getBlue();
		}

		// the difference of Red, Green, and Blue, respectively.
		int r, g, b, bestX=0, bestY=0;

		// to count the number of different pixels.
		int thresDiffCount, thresDiffMin = -1;	// difference from diffThreshold
		int totalDiffCount, totalDiffMin = -1;	// difference from 0
		double similarityThresDiff, similarityTotalDiff;
		double norm=0, min=-1;
		double diffThreshold = ComparisonParameters.getDiffThreshold();
		
		// Find the best match moving sub-image.
		for (int y = 0; y <= topMove + downMove; y++) {
			for (int x = 0; x <= leftMove + rightMove; x++) {

				// Calculate the similarity on the (x, y)-shifted sub-image of expectedImage.
				thresDiffCount = 0;	
				totalDiffCount = 0;
				norm = 0;
				for (int i = 0; i < actualHeight; i++) {
					for (int j = 0; j < actualWidth; j++) {
						r = expectedRed[expectedWidth*(i+y)+(j+x)] - actualRed[actualWidth*i+j];
						g = expectedGreen[expectedWidth*(i+y)+(j+x)] - actualGreen[actualWidth*i+j];
						b = expectedBlue[expectedWidth*(i+y)+(j+x)] - actualBlue[actualWidth*i+j];
						if (averageNorm) {
							norm += Math.sqrt(r*r + g*g + b*b);
						}	else {
							norm += r*r + g*g + b*b;
						}
						if (r*r+g*g+b*b > 3*255*255*diffThreshold*diffThreshold)
							thresDiffCount++;
						if (r*r+g*g+b*b > 0)
							totalDiffCount++;
					}
				}

				// Find the minimal difference.
				if (norm < min || min == -1)
				{
					min = norm;
					// offset (from expected to actual) of best match
					bestX = leftMove - x;
					bestY = topMove - y;
				}

				// Find the minimal number of total different pixels.
				if (totalDiffCount < totalDiffMin || totalDiffMin == -1)
				{
					totalDiffMin = totalDiffCount;
				}

				// Find the minimal number of threshold different pixels.
				if (thresDiffCount < thresDiffMin || thresDiffMin == -1)
				{
					thresDiffMin = thresDiffCount;
				}
			}
		}
		double similarity;

		// normalize and calculate average.
		if (averageNorm) {
			similarity = 1-min/(Math.sqrt(3)*255*actualWidth*actualHeight);
		}	else {
			similarity = 1-Math.sqrt(min/(actualWidth*actualHeight))/(Math.sqrt(3)*255);
		}

		// normalize the number of different pixels.
		similarityThresDiff = 1 - (double)thresDiffMin/(actualWidth*actualHeight);
		similarityTotalDiff = 1 - (double)totalDiffMin/(actualWidth*actualHeight);
		
		// round similarities to 2 decimal place.
		similarity = (double)Math.round(similarity*100)/100;
		similarityThresDiff = (double)Math.round(similarityThresDiff*100)/100;
		similarityTotalDiff = (double)Math.round(similarityTotalDiff*100)/100;
		
		if (maxMove != 0) {
			offset.setX(bestX);
			offset.setY(bestY);
		}
		similarityUnit.setXSimilar(offset.getX());
		similarityUnit.setYSimilar(offset.getY());
		similarityUnit.setSimilarityPixelByPixel(similarity);
		similarityUnit.setSimilarityThresDiff(similarityThresDiff);
		similarityUnit.setSimilarityTotalDiff(similarityTotalDiff);

		return similarity;
	}
}


