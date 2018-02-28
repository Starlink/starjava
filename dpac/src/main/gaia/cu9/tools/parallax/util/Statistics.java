package gaia.cu9.tools.parallax.util;

public class Statistics {
	
	public static double mean(double[] x){
		double acc = 0.0;
		for (double val: x){
			acc += val;
		}
		return acc/x.length;
	}
	
	public static double rootMean(double[] x){
		double acc = 0.0;
		for (double val: x){
			acc += val*val;
		}
		return Math.sqrt(acc/x.length);
	}
	
	public static double stdDev(double[] x){
		return stdDev(x, mean(x));
	}
	
	public static double stdDev(double[] x, double mean){
		double acc = 0.0;
		for (double val: x){
			acc += (val-mean)*(val-mean);
		}
		return Math.sqrt(acc/(x.length-1));
	}
	
	public static double max(double[] x){
		double max = Math.abs(x[0]);
		for (int j=1; j<x.length; j++){
			double tmp = Math.abs(x[j]);
			if (tmp>max){
				max = tmp;
			}
		}
		return max;
	}

}
