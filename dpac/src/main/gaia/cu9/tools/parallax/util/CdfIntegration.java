package gaia.cu9.tools.parallax.util;

import uk.ac.starlink.dpac.math.SplineInterpolator;
import uk.ac.starlink.dpac.math.PolynomialSplineFunction;

import gaia.cu9.tools.parallax.PDF.PDF;

public class CdfIntegration {
	
	// Constants for integration using Boole's law
	private static final double BOOLE_FACTOR = 2.0/45.0;
	private static final double[] BOOLE_COEF = new double[]{7, 32, 12, 32, 7};
	
	// Constants for percentile 
	private static final int JMAX = 100;
	private static final double TOL = 1.e-9;
	
	private double deltaThreshold = 1e-6;
	private int iterationLimit = 10000;

	
	
	/** Calculate the CDF by integrating the PDF in a finite interval.
	 * It is assumed the the PDF below the minimum value is 0. 
	 * The algorithm will continue integrating towards infinity until the increment 
	 * added is lower than a threshold or a limit of iterations is reached so that 
	 * the CDF can be normalized 
	 * To set these thresholds please see {@link #setDeltaThreshold(double)} and 
	 * {@link #setIterationLimit(int)}
	 * @param pdf Probability Distribution Function 
	 * @param logAxis True if the points should be distributed logaritmically, false otherwise
	 * @param nPoints Number of points to be calculated in the range
	 * @param rMin Minimum value of the range, in parsecs
	 * @param rMax Maximum value of the range, in parsecs
	 * @return  CDF
	 */
	public double[][]  getCdf(PDF pdf, boolean logAxis, int nPoints, double rMin, double rMax, boolean normalisePdf, boolean integrateToInfinite){
		
		if(nPoints<=1) throw new IllegalArgumentException("Too few points, at least 2 are needed");
		if(rMax<=rMin) throw new IllegalArgumentException("rMax should be greater than rMin");
		if(pdf==null) throw new IllegalArgumentException("pdf can not be null");
		
		double[] logR = new double[nPoints];
		double[] r = new double[nPoints];
		double[] sampledPdf = new double[nPoints];
		double[] sampledCdf = new double[nPoints];
		
		double step = 0;
		if (logAxis){
			step = (Math.log(rMax)-Math.log(rMin))/(nPoints-1);
		} else {
			step = (rMax-rMin)/(nPoints-1);
		}
		r[0] = rMin;
		logR[0] = Math.log(rMin);
		sampledPdf[0] = pdf.getUnnormalizedProbabilityAt(r[0]);
		sampledCdf[0] = 0;
		
		for (int i=1; i<nPoints; i++){
			if (logAxis){
				logR[i] = logR[i-1] + step;
				r[i] = Math.exp(logR[i]);
			} else {
				r[i] = r[i-1] + step;
				logR[i] = Math.log(r[i]);
			}
			sampledPdf[i] = pdf.getUnnormalizedProbabilityAt(r[i]);
			sampledCdf[i] = sampledCdf[i-1] + integrateBooleRule(pdf, r[i-1], r[i], sampledPdf[i-1], sampledPdf[i]);
		}
		
		double normFactor = sampledCdf[nPoints-1];
	
			if (integrateToInfinite){
				// Keep on integrating to infinite (until PDF below threshold)
				normFactor = normFactor + integrateToInfinitum(pdf, logAxis, r[nPoints-1], step);
			}


			// Normalize the CDF
			if (normFactor!=0){
				for (int i=0; i<nPoints; i++){
					sampledCdf[i] = sampledCdf[i]/normFactor;
				}
			}
			
		if (normalisePdf){	
			
			// Normalise PDF to a proper probability distribution
			if (normFactor!=0){
				for (int i=0; i<nPoints; i++){
					sampledPdf[i] = sampledPdf[i]/normFactor;
				}
			}
			
		} else {
			// Normalize PDF to max = 1
			double maxVal = 0;
		
			for (int i=0; i<nPoints; i++){
				if (sampledPdf[i]>maxVal){
					maxVal = sampledPdf[i];
				}
			}
			
			for (int i=0; i<nPoints; i++){
				sampledPdf[i] = sampledPdf[i]/maxVal;
			}

		}
		

		return new double[][]{r, sampledPdf, sampledCdf};
	}
	
	/** 
	 * Integrate the PDF over a finite interval using Boole's rule
	 * @param pdf probability distribution function to integrate
	 * @param min Minimum value of x (in linear units)
	 * @param max Maximum value of x (in linear units)
	 * @return Value of the integral of the pdf in the interval (min,max)
	 */
	protected double integrateBooleRule(PDF pdf, double min, double max){
		double pdfAtMin = pdf.getUnnormalizedProbabilityAt(min);
		double pdfAtMax = pdf.getUnnormalizedProbabilityAt(max);
		return integrateBooleRule(pdf, min, max, pdfAtMin, pdfAtMax);
	}
	
	/** 
	 * Integrate the PDF over a finite interval using Boole's rule
	 * 
	 * This implementation uses the value of the PDF at the boundaries that 
	 * has already been calculated externally, thus avoiding unnecesary calls
	 * to {@link PDF#getUnnormalizedProbabilityAt(double)}
	 * 
	 * @param pdf probability distribution function to integrate
	 * @param min Minimum value of x (in linear units)
	 * @param max Maximum value of x (in linear units)
	 * @param pdfAtMin Value of the PDF at min
	 * @param pdfAtMax Value of the PDF at max
	 * @return Value of the integral of the pdf in the interval (min,max)
	 */
	protected double integrateBooleRule(PDF pdf, double min, double max, double pdfAtMin, double pdfAtMax){
		
		double h = (max-min)/(BOOLE_COEF.length-1);
		
		// 1st term
		double accum = BOOLE_COEF[0] * pdfAtMin;
		
		// 2nd-4th terms
		for(int i=1;i<BOOLE_COEF.length-1;i++){
			double x = min + i*h;
			double y = pdf.getUnnormalizedProbabilityAt(x);
			accum = accum + BOOLE_COEF[i] * y;
		}
		
		// 5th term
		accum = accum + BOOLE_COEF[BOOLE_COEF.length-1] * pdfAtMax;
		
		double result = BOOLE_FACTOR * h * accum;
		
		return result;
	}
	
	protected double integrateToInfinitum(PDF pdf, boolean logAxis, double minR, double step){
		
		double factor = 1;
		if (logAxis){
			factor = Math.exp(step);
		}
		double currentMinX = minR;
		double currentMaxX = 0;
		int nIterations = 0;
		
		double accum = 0;
		double delta = 0;
		double diff = 0;
		double pdfAtMin = pdf.getUnnormalizedProbabilityAt(currentMinX);
		double pdfAtMax = 0;

		do {
			if (nIterations >= iterationLimit){
				throw new RuntimeException("Integration did not converge after " + nIterations + " steps. " +
										   "Last delta (threshold)"  + diff + " (" + deltaThreshold + ")");
			}
			
			if (logAxis){
				currentMaxX = currentMinX * factor;		
			} else {
				currentMaxX = currentMinX + step;
			}
			
			pdfAtMax = pdf.getUnnormalizedProbabilityAt(currentMaxX);
			
			delta = integrateBooleRule(pdf, currentMinX, currentMaxX, pdfAtMin, pdfAtMax);
			accum = accum + delta;
			
			// prepare next iteration
			diff = pdfAtMax/ accum;
			currentMinX = currentMaxX;
			pdfAtMin = pdfAtMax;
			nIterations++;
	
		} while(diff>deltaThreshold);
		
		return accum;
		
	}
	
	public double[] getPercentiles(double[][] cdf, double... percentiles ){
		
		double[] result = new double[percentiles.length];
		
		SplineInterpolator interpolator = new SplineInterpolator();
		PolynomialSplineFunction spline  = interpolator.interpolate(cdf[0], cdf[2]);
		
		double[] r = cdf[0];
		double minX = r[0];
		double maxX = r[r.length-1];
		
		for (int i=0; i<percentiles.length; i++){
			result[i] = findPercentile(spline, minX, maxX, percentiles[i]);
		}
		
		return result;
	}
	
	
	/**
	 * The method find the distance that satisfy the given confidence
	 * interval. The root finding used is the bisection method.
	 * @param confidence
	 */
	private double findPercentile(PolynomialSplineFunction spline, double minX, double maxX, double percentile){
			
		double x1 = minX;
		double x2 = maxX;
		
		double xm = 0.5*(x1 + x2);
		double f1, fm;
		for (int j=0;j<JMAX; j++){
			xm = 0.5*(x1 + x2);
            fm = spline.value(xm) - percentile;
            if (Math.abs(fm) < TOL){
            	break;
            }
            f1 = spline.value(x1) - percentile;
            if (f1*fm < 0){
            	x2 = xm;
            } else {
            	x1 = xm;
            }
            // nIter++;
		}
		// System.out.println(nIter);
		return xm;
	}
	

	public double getDeltaThreshold() {
		return deltaThreshold;
	}

	public void setDeltaThreshold(double deltaThreshold) {
		this.deltaThreshold = deltaThreshold;
	}

	public int getIterationLimit() {
		return iterationLimit;
	}

	public void setIterationLimit(int iterationLimit) {
		this.iterationLimit = iterationLimit;
	}
	
	
	
	
	
	

}
