package gaia.cu9.tools.parallax.datamodel;


public class StarVariables {
	protected long sourceId;
	protected double alpha; // equatorial coordinates in radian
	protected double delta; // equatorial coordinates in radian
	protected double Ldeg; // Galactic coordinates L in degrees
	protected double Bdeg; // Galactic coordinates B in degrees
	protected double varpi;
	protected double errVarpi;
	
	protected double realR; // used only in tests to evaluate estimator performance
	

	
	public StarVariables(long sourceId, double alpha, double delta, double lGal, double bGal, double varpi, double sigmaVarpi){
		this.sourceId = sourceId;
		this.alpha = alpha;
		this.delta = delta;
		this.Ldeg  = lGal;
		this.Bdeg  = bGal;
		this.varpi = varpi;
		this.errVarpi = sigmaVarpi;
	}

	public long getSourceId() {
		return sourceId;
	}

	public void setSourceId(long sourceId) {
		this.sourceId = sourceId;
	}

	public double getAlpha() {
		return alpha;
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public double getDelta() {
		return delta;
	}

	public void setDelta(double delta) {
		this.delta = delta;
	}

	public double getLdeg() {
		return Ldeg;
	}

	public void setLdeg(double ldeg) {
		Ldeg = ldeg;
	}

	public double getBdeg() {
		return Bdeg;
	}

	public void setBdeg(double bdeg) {
		Bdeg = bdeg;
	}

	public double getVarpi() {
		return varpi;
	}

	public void setVarpi(double varpi) {
		this.varpi = varpi;
	}

	public double getErrVarpi() {
		return errVarpi;
	}

	public void setErrVarpi(double errVarpi) {
		this.errVarpi = errVarpi;
	}

	public double getRealR() {
		return realR;
	}

	public void setRealR(double realR) {
		this.realR = realR;
	}
	
}
