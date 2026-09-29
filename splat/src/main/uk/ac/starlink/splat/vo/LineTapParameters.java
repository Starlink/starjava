package uk.ac.starlink.splat.vo;

import java.io.IOException;
import java.util.ArrayList;

public class LineTapParameters {
	
	
	//LineTap Parameters
	private String title = "";
	private double vacuumWavelength;
	private double vacuumWavelengthError;
	private String method;	
	private int ion_charge;
	private int mass_number; 
	private double	upper_energy;
	private double lower_energy;
	private String  inchi;
	private String inchikey;
	private double einstein_a;
	private String xsams_uri;
	private String line_reference;
	

	// Parameters for query
	private ArrayList<double []> vacuumWavelengthRanges;
	private ArrayList<int []> upperEnergyRange; // int or double?
	private ArrayList<int []> lowerEenergyRange;
	
	// units
	private String WavelengthUnit;

	private String EnergyUnit;


	public LineTapParameters() {
		
	}
	
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public double getVacuumWavelengthRange() {
		return vacuumWavelength;
	}

	public void setVacuumWavelength(double vacuumWavelength) {
		this.vacuumWavelength = vacuumWavelength;
	}

	public double getVacuumWavelengthError() {
		return vacuumWavelengthError;
	}

	public void setVacuumWavelengthError(double vacuumWavelengthError) {
		this.vacuumWavelengthError = vacuumWavelengthError;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public int getIon_charge() {
		return ion_charge;
	}

	public void setIon_charge(int ion_charge) {
		this.ion_charge = ion_charge;
	}

	public int getMass_number() {
		return mass_number;
	}

	public void setMass_number(int mass_number) {
		this.mass_number = mass_number;
	}

	public double getUpper_energy() {
		return upper_energy;
	}

	public void setUpper_energy(double upper_energy) {
		this.upper_energy = upper_energy;
	}

	public double getLower_energy() {
		return lower_energy;
	}

	public void setLower_energy(double lower_energy) {
		this.lower_energy = lower_energy;
	}

	public String getInchi() {
		return inchi;
	}

	public void setInchi(String inchi) {
		this.inchi = inchi;
	}

	public String getInchikey() {
		return inchikey;
	}

	public void setInchikey(String inchikey) {
		this.inchikey = inchikey;
	}

	public double getEinstein_a() {
		return einstein_a;
	}

	public void setEinstein_a(double einstein_a) {
		this.einstein_a = einstein_a;
	}

	public String getXsams_uri() {
		return xsams_uri;
	}

	public void setXsams_uri(String xsams_uri) {
		this.xsams_uri = xsams_uri;
	}

	public String getLine_reference() {
		return line_reference;
	}

	public void setLine_reference(String line_reference) {
		this.line_reference = line_reference;
	}
	
	public ArrayList<double[]> getVacuumWavelengthRanges() {
		return vacuumWavelengthRanges;
	}

	public void setVacuumWavelengthRanges(ArrayList<double[]> vacuumWavelengthRanges) {
		this.vacuumWavelengthRanges = vacuumWavelengthRanges;
	}

	public ArrayList<int[]> getUpperEnergyRange() {
		return upperEnergyRange;
	}

	public void setUpperEnergyRange(ArrayList<int[]> upperEnergyRange) {
		this.upperEnergyRange = upperEnergyRange;
	}

	public ArrayList<int[]> getLowerEenergyRange() {
		return lowerEenergyRange;
	}

	public void setLowerEenergyRange(ArrayList<int[]> lowerEenergyRange) {
		this.lowerEenergyRange = lowerEenergyRange;
	}
	
	public String getWavelengthUnit() {
		return WavelengthUnit;
	}


	public void setWavelengthUnit(String wavelengthUnit) {
		WavelengthUnit = wavelengthUnit;
	}


	public String getEnergyUnit() {
		return EnergyUnit;
	}


	public void setEnergyUnit(String energyUnit) {
		EnergyUnit = energyUnit;
	}
	

	public void setRanges(double[] ranges) {  // check about unit!
		vacuumWavelengthRanges=new ArrayList<double[]>();
		for (int i=0; i<ranges.length-1; i+=2) {
			double[] ri = new double[2];
			ri[0] = ranges[i];
			ri[1]=ranges[i+1];
			vacuumWavelengthRanges.add(ri); 
		}
		
	}
/*	
	public String [] getInChiName(String species) {
		
		if (! species.isEmpty()) {
			try {
				return RSCSpecies.getInchIKey(species);
				
			} catch (IOException e) {
				
				return "";
			}
		}
		else return "";
	}
   public String [] getInChiByFormula(String formula) {
		
		if (! formula.isEmpty()) {
			try {
				return RSCSpecies.getInchIKeyByFormula(formula);
				
			} catch (IOException e) {
				
				return "";
			}
		}
		else return "";
	}
	
 	private void setInChiByFormula(String formula, int charge) {
		String ic = getInChiByFormula(formula);
		if (! ic.isEmpty()) {
			//inchi.setText(inchi);
			//inchi.setToolTipText(inchikey);
		}
		else {
			ic="error";
			//inChiLabel.setToolTipText("InChIKey could not be retrieved");
		}
		this.inchi = ic;
	}

*/




}
