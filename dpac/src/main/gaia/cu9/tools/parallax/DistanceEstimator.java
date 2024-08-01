package gaia.cu9.tools.parallax;

import java.util.Map;

import java.util.logging.Logger;

import gaia.cu9.tools.parallax.PDF.DistanceEstimationMethod;
import gaia.cu9.tools.parallax.PDF.ExpDecrVolumeDensityDEM;
import gaia.cu9.tools.parallax.PDF.HaywoodExpDecrVolumeDEM;
import gaia.cu9.tools.parallax.PDF.HaywoodUniformDistanceDEM;
import gaia.cu9.tools.parallax.PDF.LosExpDecrVolumeDensityDEM;
import gaia.cu9.tools.parallax.PDF.PDF;
import gaia.cu9.tools.parallax.PDF.UniformDistanceDEM;
import gaia.cu9.tools.parallax.datamodel.DistanceEstimation;
import gaia.cu9.tools.parallax.datamodel.StarVariables;
import gaia.cu9.tools.parallax.util.CdfIntegration;


/**
 * This class calculates the distance estimation for a given star.
 * 
 * In the constructor, the estimator of the PDF of the distance to be used
 * is specified, and can be reused for several stars.
 * 
 * Several parameters controlling how the estimation should be done can be 
 * passed through an optional Map&lt;String, String&gt;
 * 
 * @author eutrilla
 *
 */
public class DistanceEstimator {
	
	public enum EstimationType{
		   HS_UNIFORM,
		   HS_EXP_DECR,
	       UNIFORM_DISTANCE,
	       EXP_DEC_VOL_DENSITY,
	       LOS_EXP_DEC_VOL_DENSITY};

	private EstimationType estimator = null;
 
	// parameters
	private double l = -1;
	private int nPoints = -1;
	private double rMin = -1;
	private double rMax = 100;
	private double minConfVal = -1;
	private double maxConfVal = -1;
	private boolean logAxis = true;
	private boolean normalisePdf = false;
	private boolean integrateToInfinite = true;
	
	private Logger logger;
	private CdfIntegration integrator = null;


	public DistanceEstimator(){
		this(EstimationType.HS_UNIFORM, null);
	}
	
        @SuppressWarnings("this-escape")
	public DistanceEstimator(EstimationType type, Map<String,String> parameters){
		this.logger = Logger.getLogger(this.getClass().getName());
		
		this.estimator=type;
		
		// load Parameters
		this.l = getDouble("L", parameters, 1.35);
		this.nPoints = getInt("nPoints", parameters, 16380);
		this.rMin = getDouble("rMin", parameters, 0.001);
		this.rMax = getDouble("rMax", parameters, 100);
		this.minConfVal = getDouble("minConfVal", parameters, 0.05);
		this.maxConfVal = getDouble("maxConfVal", parameters, 0.95);
		this.logAxis = getBoolean("logAxis", parameters, true);
		this.normalisePdf = getBoolean("normalisePdf", parameters, false);
		this.integrateToInfinite = getBoolean("integrateToInfinite", parameters, true);
		
		this.integrator = new CdfIntegration();
	}
	
	public double[][] getDistanceCdf(StarVariables star){
		DistanceEstimationMethod method = getEstimationMethod(star);
		
		PDF distancePdf = method.getDistancePDF();
		double[][] distanceCdf = integrator.getCdf(distancePdf, logAxis, nPoints, rMin, rMax, normalisePdf, integrateToInfinite);
		return distanceCdf;
	}
	
	public double[][] getDistanceModulusCdf(StarVariables star){
		DistanceEstimationMethod method = getEstimationMethod(star);
		PDF modulusPdf = method.getDistanceModulusPDF();
		double[][] modulusCdf = integrator.getCdf(modulusPdf, true, nPoints, rMin, rMax, normalisePdf, integrateToInfinite);

		for (int i=0;i<modulusCdf[0].length; i++){
			modulusCdf[0][i] = distanceToModulus(modulusCdf[0][i]);
		}
		return modulusCdf;
	}
	
	public DistanceEstimation estimate(StarVariables star){
		
		DistanceEstimationMethod method = getEstimationMethod(star);
		
		// distance estimation
		PDF distancePdf = method.getDistancePDF();
		double bestDistance = distancePdf.getBestEstimation();
		
		double[][] distanceCdf = integrator.getCdf(distancePdf, logAxis, nPoints, rMin, rMax, normalisePdf, integrateToInfinite);
		double[] distanceInterval = integrator.getPercentiles(distanceCdf, minConfVal, maxConfVal);

		// distance modulus estimation
		PDF modulusPdf = method.getDistanceModulusPDF();
		double bestModulus = distanceToModulus(modulusPdf.getBestEstimation());
		double[][] modulusCdf = integrator.getCdf(modulusPdf, true, nPoints, rMin, rMax, normalisePdf, integrateToInfinite);
		double[] modulusInterval = integrator.getPercentiles(distanceCdf, minConfVal, maxConfVal);
		
		for (int i=0;i<modulusInterval.length; i++){
			modulusInterval[i] = distanceToModulus(modulusInterval[i]);
		}

		return new DistanceEstimation(star.getSourceId(), bestDistance, distanceInterval, bestModulus, modulusInterval);
	}
	
	protected double distanceToModulus(double r){
		return 5*(Math.log10(r*1000) - 5);
	}

	
	protected double getDouble(String key, Map<String,String> parameters, double defaultValue){
		double result = defaultValue;
		if(parameters!=null){
			String value = parameters.get(key);
			if (value!=null){
				try{
					result = Double.valueOf(value);
					logger.config("Found value for parameter '" + key + "', using: " + result);
				} catch (Exception e){
					throw new IllegalArgumentException("Invalid value for parameter '" + key +"' (" + value + ")");
				}
			} else {
				logger.config("No value for parameter '" + key +"' found, using default value: " + defaultValue);
			}
		}
		return result;
	}
	
	protected int getInt(String key, Map<String,String> parameters, int defaultValue){
		int result = defaultValue;
		if(parameters!=null){
			String value = parameters.get(key);
			if (value!=null){
				try{
					result = Integer.valueOf(value);
					logger.config("Found value for parameter '" + key + "', using: " + result);
				} catch (Exception e){
					throw new IllegalArgumentException("Invalid value for parameter '" + key +"' (" + value + ")");
				}
			} else {
				logger.config("No value for parameter '" + key +"' found, using default value: " + defaultValue);
			}
		}
		return result;
	}
	
	protected boolean getBoolean(String key, Map<String,String> parameters, boolean defaultValue){
		boolean result = defaultValue;
		if(parameters!=null){
			String value = parameters.get(key);
			if (value!=null){
				try{
					result = Boolean.valueOf(value);
					logger.config("Found value for parameter '" + key + "', using: " + result);
				} catch (Exception e){
					throw new IllegalArgumentException("Invalid value for parameter '" + key +"' (" + value + ")");
				}
			} else {
				logger.config("No value for parameter '" + key +"' found, using default value: " + defaultValue);
			}
		}
		return result;
	}

	protected DistanceEstimationMethod getEstimationMethod(StarVariables star) {

		DistanceEstimationMethod method = null;
		switch (estimator) {
		case HS_UNIFORM:
			method = new HaywoodUniformDistanceDEM(star, rMax);
			break;
		case HS_EXP_DECR:
			method = new HaywoodExpDecrVolumeDEM(star, l);
			break;
		case UNIFORM_DISTANCE:
			method = new UniformDistanceDEM(star, rMax);
			break;
		case EXP_DEC_VOL_DENSITY:
			method = new ExpDecrVolumeDensityDEM(star, l);
			break;
		case LOS_EXP_DEC_VOL_DENSITY:
			method = new LosExpDecrVolumeDensityDEM(star);
			break;
		}

		return method;
	}
}
