package gaia.cu9.tools.parallax.datamodel;

public class DistanceEstimation {
	
	private long sourceId = -1;
	private double bestDistance = -1;
	private double[] distanceInterval= null;
	private double bestModulus = -1;
	private double[] modulusInterval= null;

	
	
	
	public DistanceEstimation(long sourceId, double bestDistance, double[] distanceInterval, double bestModulus, double[] modulusInterval){
		this.sourceId = sourceId;
		this.bestDistance = bestDistance;
		this.distanceInterval = distanceInterval;
		this.bestModulus = bestModulus;
		this.modulusInterval = modulusInterval;
	}

	public long getSourceId() {
		return sourceId;
	}

	public double getBestDistance() {
		return bestDistance;
	}

	public double[] getDistanceInterval() {
		return distanceInterval;
	}

	public double getBestModulus() {
		return bestModulus;
	}

	public double[] getModulusInterval() {
		return modulusInterval;
	}

}
