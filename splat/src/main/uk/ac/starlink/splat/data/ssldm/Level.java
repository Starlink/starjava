package uk.ac.starlink.splat.data.ssldm;
/*
 ********************************************
 * IVOA Simple Spectral Line Data Model V1.0
 ********************************************
 */
public class Level {

    
   /* 
    * Class representing Initial and Final levels between which the transition happens
    * @author Margarida Castro Neves
    * 08.2016
    */
    
    PhysicalQuantity totalStatWeight ; // An integer representing the total number of terms pertaining to a given level
    PhysicalQuantity nuclearStatWeight ; // The same as Level.totalStatWeight for nuclear spin states only
    PhysicalQuantity landeFactor  ;// A dimensionless factor g that accounts for the splitting of normal energy levels into uniformly spaced sublevels in the presence of a magnetic field
    PhysicalQuantity lifeTime ; // Intrinsic lifetime of a level due to its radiative decay
    PhysicalQuantity energy  ; //The binding energy of an electron belonging to the level
  
    String energyOrigin ; // Human readable string indicating the nature of the energy origin, e.g., “Ionization energy limit”, “Ground state energy” of an atom, “Dissociation limit” for  a molecule, etc
    QuantumState quantumState ; // A representation of the level quantum state through its set of quantum numbers
    String nuclearSpinSymmetryType ;//  A string indicating the type of nuclear spin symmetry. Possible values are: “para”,“ortho”, “meta”
    PhysicalQuantity parity ;//  Eigenvalue of the parityoperator. Values (+1,-1)
    String configuration ;// Human readable string representing the corresponding level configuration 
    
    String ucd = "phys.atmol.level";
   
    
    public Level() {
        // TODO Auto-generated constructor stub
    }

    public PhysicalQuantity getTotalStatWeight() {
        return totalStatWeight;
    }

    public void setTotalStatWeight(PhysicalQuantity totalStatWeight) {
        this.totalStatWeight = totalStatWeight;
    }
    
    public void setTotalStatWeight(double doubleValue, double err, String unit) {
       this.totalStatWeight = new PhysicalQuantity( doubleValue, err, unit);        
    }
    
    public void setTotalStatWeight(double doubleValue, String unit) {
        this.totalStatWeight = new PhysicalQuantity( doubleValue, unit);        
    }

    public PhysicalQuantity getNuclearStatWeight() {
        return nuclearStatWeight;
    }

    public void setNuclearStatWeight(PhysicalQuantity nuclearStatWeight) {
        this.nuclearStatWeight = nuclearStatWeight;
    }

    public PhysicalQuantity getLandeFactor() {
        return landeFactor;
    }

    public void setLandeFactor(PhysicalQuantity landeFactor) {
        this.landeFactor = landeFactor;
    }
    
    public void setLandeFactor(double value, double error, String units) {
        this.landeFactor = new PhysicalQuantity(value, error, units);       
    }
    public void setLandeFactor(double value, String units) {
        this.landeFactor = new PhysicalQuantity(value, units);       
    }


    public PhysicalQuantity getLifeTime() {
        return lifeTime;
    }

    public void setLifeTime(PhysicalQuantity lifeTime) {
        this.lifeTime = lifeTime;
    }

    public PhysicalQuantity getEnergy() {
        return energy;
    }
   

    public void setEnergy(PhysicalQuantity energy) {
        this.energy = energy;
    }
    
    public void setEnergy(double value, double error , String units) {
        this.energy = new PhysicalQuantity(value, error, units);  
        energy.setUcd("phys.energy;"+this.ucd);
    }
    
    public void setEnergy(double value, String units) {
        this.energy = new PhysicalQuantity(value, units);           
    }

    public String getEnergyOrigin() {
        return energyOrigin;
    }

    public void setEnergyOrigin(String energyOrigin) {
        this.energyOrigin = energyOrigin;
    }

    public QuantumState getQuantumState() {
        return quantumState;
    }

    public void setQuantumState(QuantumState quantumState) {
        this.quantumState = quantumState;
    }

    public String getNuclearSpinSymmetryType() {
        return nuclearSpinSymmetryType;
    }

    public void setNuclearSpinSymmetryType(String nuclearSpinSymmetryType) {
        this.nuclearSpinSymmetryType = nuclearSpinSymmetryType;
    }

    public PhysicalQuantity getParity() {
        return parity;
    }

    public void setParity(PhysicalQuantity parity) {
        this.parity = parity;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getUcd() {
        return ucd;
    }

    public void setUcd(String ucd) {
        this.ucd = ucd+";"+this.ucd;
    }

   

  
   
   
    

   
}
