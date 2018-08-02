package uk.ac.starlink.splat.data.ssldm;
/*
 ********************************************
 * IVOA Simple Spectral Line Data Model V1.0
 ********************************************
 */
public class Environment {

   /* 
    * Class representing the physical properties of the ambient gas, plasma, dust
    * or stellar atmosphere where the line is generated
    * @author Margarida Castro Neves
    * 08.2016
    */ 
    
    PhysicalQuantity temperature ; //  The temperature in the lineproducing  plasma
    PhysicalQuantity opticalDepth ; // The optical depth of the line producing plasma
    PhysicalQuantity particleDensity ; //  The particle density in the line producing plasma
    PhysicalQuantity massdensity ; // The mass density in the lineproducing plasma
    PhysicalQuantity pressure ; // The pressure in the line producing plasma
    PhysicalQuantity entropy ; // The entropy of the line producing plasma
    PhysicalQuantity mass ; // The total mass of the lineproducing gas/dust cloud or star
    PhysicalQuantity metallicity ; // The logarithmic ratio between the element and the Hydrogen abundance, normalized to the solar value
    PhysicalQuantity extinctionCoefficient ; // The suppression of the emission line intensity due to  the presence of optically thick matter along the lineof-sight
   // PhysicalModel model ; // Placeholder for future  detailed theoretical models  of the environment plasma  where the line appears.
 
    public Environment() {
        // TODO Auto-generated constructor stub
    }

    public PhysicalQuantity getTemperature() {
        return temperature;
    }

    public void setTemperature(PhysicalQuantity temperature) {
        this.temperature = temperature;
    }

    public PhysicalQuantity getOpticalDepth() {
        return opticalDepth;
    }

    public void setOpticalDepth(PhysicalQuantity opticalDepth) {
        this.opticalDepth = opticalDepth;
    }

    public PhysicalQuantity getParticleDensity() {
        return particleDensity;
    }

    public void setParticleDensity(PhysicalQuantity particleDensity) {
        this.particleDensity = particleDensity;
    }

    public PhysicalQuantity getMassdensity() {
        return massdensity;
    }

    public void setMassdensity(PhysicalQuantity massdensity) {
        this.massdensity = massdensity;
    }

    public PhysicalQuantity getPressure() {
        return pressure;
    }

    public void setPressure(PhysicalQuantity pressure) {
        this.pressure = pressure;
    }

    public PhysicalQuantity getEntropy() {
        return entropy;
    }

    public void setEntropy(PhysicalQuantity entropy) {
        this.entropy = entropy;
    }

    public PhysicalQuantity getMass() {
        return mass;
    }

    public void setMass(PhysicalQuantity mass) {
        this.mass = mass;
    }

    public PhysicalQuantity getMetallicity() {
        return metallicity;
    }

    public void setMetallicity(PhysicalQuantity metallicity) {
        this.metallicity = metallicity;
    }

    public PhysicalQuantity getExtinctionCoefficient() {
        return extinctionCoefficient;
    }

    public void setExtinctionCoefficient(PhysicalQuantity extinctionCoefficient) {
        this.extinctionCoefficient = extinctionCoefficient;
    }

}
