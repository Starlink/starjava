package uk.ac.starlink.splat.data.ssldm;

import org.vamdc.xsams.schema.DataType;

/*
 ********************************************
 * IVOA Simple Spectral Line Data Model V1.0
 ********************************************
 */
public class SpectralLine {

    
    /*
     * Class representing a Spectroscopic line
     * 
     * @Author Margarida Castro Neves
     * 08.2016
     */
    String title = null;
    String elementName = null;
 
    Level initialLevel = null;
    Level finalLevel = null;
   
    Species initialElement ; 
    Species finalElement ;
    PhysicalQuantity wavelength ; // Wavelength in vacuum of the transition originating the line
    PhysicalQuantity frequency ;  
    PhysicalQuantity wavenumber ;
    PhysicalQuantity airWavelength ;   
    PhysicalQuantity einsteinA ;
    PhysicalQuantity oscillatorStrength ;
    PhysicalQuantity weightedOscillatorStrength ;
//    PhysicalQuantity intensity ;
    PhysicalQuantity observedFlux ;
    PhysicalQuantity observedFluxWaveMin ;
    PhysicalQuantity observedFluxWaveMax ;
    PhysicalQuantity significanceOfDetection ;
    PhysicalQuantity transitionType ;
    PhysicalQuantity strength ;
    PhysicalQuantity observedBroadeningCoefficient ;
    PhysicalQuantity observedShiftingCoefficient ;
    String ucd = "em.line";
    
    
    public SpectralLine() {
        initialElement = new Species();
        finalElement = new Species();
        wavelength =  new PhysicalQuantity(); // Wavelength in vacuum of the transition originating the line
        frequency = new PhysicalQuantity();   
        wavenumber =  new PhysicalQuantity();
        airWavelength =  new PhysicalQuantity();   
        einsteinA = new PhysicalQuantity(); 
        oscillatorStrength =  new PhysicalQuantity();
        weightedOscillatorStrength =  new PhysicalQuantity();
//        intensity =  new PhysicalQuantity();
        observedFlux =  new PhysicalQuantity();
        observedFluxWaveMin =  new PhysicalQuantity();
        observedFluxWaveMax =  new PhysicalQuantity();
        significanceOfDetection =  new PhysicalQuantity();
        transitionType =  new PhysicalQuantity();
        strength =  new PhysicalQuantity();
        observedBroadeningCoefficient =  new PhysicalQuantity();
        observedShiftingCoefficient =  new PhysicalQuantity();
    }
    
    public SpectralLine(String linetitle ) {
        setTitle(linetitle);
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle( String newtitle ) {
        title = newtitle;
    }
    
  //  public String getElementName() {
  //      return elementName;
  //  }
    
 //   public void setElementName( String element ) {
 //       elementName = element;
 //   }
    
    public Level getInitialLevel() {
        return initialLevel;
    }

    public void setInitialLevel(Level initialLevel) {
        this.initialLevel = initialLevel;
        this.initialLevel.setUcd("phys.atmol.initial");
    }

    public Level getFinalLevel() {
        return finalLevel;
    }

    public void setFinalLevel(Level finalLevel) {
        this.finalLevel = finalLevel;
        this.finalLevel.setUcd("phys.atmol.final");

    }

    public Species getInitialElement() {      
        return initialElement;
    }
    
   

    public void setInitialElement(String initialElement, int ionStage) {
        this.initialElement.setName( initialElement, ionStage );
        this.initialElement.setElementName( initialElement );
        this.initialElement.setUcd("phys.atmol.initial;phys.atmol.element");
        this.initialElement.setIonizationStage(ionStage);
        setTitle(this.initialElement.getName());
    }

    public Species getFinalElement() {
        return finalElement;
    }

    public void setFinalElement(String finalElement) {
        this.finalElement.setName( finalElement );
        this.finalElement.setElementName(finalElement);
        this.finalElement.setUcd("phys.atmol.final;phys.atmol.element");
    }

    public PhysicalQuantity getWavelength() {
        return wavelength;
    }

    public void setWavelength(PhysicalQuantity wavelength) {
        this.wavelength = wavelength;
    }
    
    public void setWavelength(double value, String unit) {
        this.wavelength = new PhysicalQuantity(value, unit );              
    }
    public void setWavelength(double value, double error, String unit) {
        this.wavelength = new PhysicalQuantity(value, error, unit );              
    }


    public PhysicalQuantity getFrequency() {
        return frequency;
    }

    public void setFrequency(PhysicalQuantity frequency) {
        this.frequency = frequency;
    }
    
    public void setFrequency(double value, String unit) {
        this.frequency = new PhysicalQuantity(value, unit );
    }


    public PhysicalQuantity getWavenumber() {
        return wavenumber;
    }

    public void setWavenumber(PhysicalQuantity wavenumber) {
        this.wavenumber = wavenumber;
    }
    

    public PhysicalQuantity getAirWavelength() {
        return airWavelength;
    }

    public void setAirWavelength(PhysicalQuantity airWavelength) {
        this.airWavelength = airWavelength;
    }
    
    public void setAirWavelength(double value, String unit) {
        this.airWavelength = new PhysicalQuantity(value, unit );              
    }

    public PhysicalQuantity getEinsteinA() {
        return  einsteinA;
    }

    public void setEinsteinA(PhysicalQuantity  einsteinA) {
        this.einsteinA =  einsteinA;
    }
    
    public void setEinsteinA(double value, String unit) {
        this.einsteinA = new PhysicalQuantity(value, unit );    
        this.einsteinA.setUcd("phys.atmol.transProb");
    }
    
    public PhysicalQuantity getOscillatorStrength() {
        return oscillatorStrength;
    }

    public void setOscillatorStrength(PhysicalQuantity oscillatorStrength) {
        this.oscillatorStrength = oscillatorStrength;
    }
    
    public void setOscillatorStrength(double value, String unit) {
        this.oscillatorStrength = new PhysicalQuantity(value, unit );       
    }


    public PhysicalQuantity getWeightedOscillatorStrength() {
        return weightedOscillatorStrength;
    }

    public void setWeightedOscillatorStrength(PhysicalQuantity weightedOscillatorStrength) {
        this.weightedOscillatorStrength = weightedOscillatorStrength;
    }
    
    public void setWeightedOscillatorStrength(double value, String unit) {
        this.weightedOscillatorStrength = new PhysicalQuantity(value, unit );
    }

/*    public PhysicalQuantity getIntensity() {
        return intensity;
    }

    public void setIntensity(PhysicalQuantity intensity) {
        this.intensity = intensity;
    }
    */

    public PhysicalQuantity getObservedFlux() {
        return observedFlux;
    }

    public void setObservedFlux(PhysicalQuantity observedFlux) {
        this.observedFlux = observedFlux;
    }

    public PhysicalQuantity getObservedFluxWaveMin() {
        return observedFluxWaveMin;
    }

    public void setObservedFluxWaveMin(PhysicalQuantity observedFluxWaveMin) {
        this.observedFluxWaveMin = observedFluxWaveMin;
    }

    public PhysicalQuantity getObservedFluxWaveMax() {
        return observedFluxWaveMax;
    }

    public void setObservedFluxWaveMax(PhysicalQuantity observedFluxWaveMax) {
        this.observedFluxWaveMax = observedFluxWaveMax;
    }

    public PhysicalQuantity getSignificanceOfDetection() {
        return significanceOfDetection;
    }

    public void setSignificanceOfDetection(PhysicalQuantity significanceOfDetection) {
        this.significanceOfDetection = significanceOfDetection;
    }

    public PhysicalQuantity getTransitionType() {
        return transitionType;
    }

    public void setTransitionType(PhysicalQuantity transitionType) {
        this.transitionType = transitionType;
    }

    public PhysicalQuantity getStrength() {
        return strength;
    }

    public void setStrength(PhysicalQuantity strength) {
        this.strength = strength;
    }

    public PhysicalQuantity getObservedBroadeningCoefficient() {
        return observedBroadeningCoefficient;
    }

    public void setObservedBroadeningCoefficient(PhysicalQuantity observedBroadeningCoefficient) {
        this.observedBroadeningCoefficient = observedBroadeningCoefficient;
    }

    public PhysicalQuantity getObservedShiftingCoefficient() {
        return observedShiftingCoefficient;
    }

    public void setObservedShiftingCoefficient(PhysicalQuantity observedShiftingCoefficient) {
        this.observedShiftingCoefficient = observedShiftingCoefficient;
    }

    public void setOscillatorStrength(double value, Object object, String units) {
        // TODO Auto-generated method stub
        
    }

	public void setStrength(double value, String unit) {
		this.strength = new PhysicalQuantity(value, unit );  
			
	}
/*
	public void setIntensity(double value, String unit) {
		this.intensity = new PhysicalQuantity(value, unit);  
			
	}
	
	*/


   

}
