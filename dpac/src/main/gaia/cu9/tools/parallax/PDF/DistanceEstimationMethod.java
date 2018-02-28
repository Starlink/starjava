package gaia.cu9.tools.parallax.PDF;

public abstract class DistanceEstimationMethod {

	private PDF distancePdf;
	private PDF modulusPdf;
	
	protected abstract PDF createDistancePdf();
	
	protected abstract PDF createModulusPdf();
	
	protected void initPdfs(){
		if (distancePdf == null){
			distancePdf = createDistancePdf();
		}
		
		if (modulusPdf == null){
			modulusPdf = createModulusPdf();
		}
	}
	
	
	public PDF getDistancePDF(){
		initPdfs();
		return distancePdf;
	}
	
	public PDF getDistanceModulusPDF(){
		initPdfs();
		return modulusPdf;
	}
}
