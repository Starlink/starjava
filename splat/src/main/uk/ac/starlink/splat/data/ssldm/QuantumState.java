package uk.ac.starlink.splat.data.ssldm;
/*
 ********************************************
 * IVOA Simple Spectral Line Data Model V1.0
 ********************************************
 */
public class QuantumState {

   /* 
    * Class representing the Quantum State of the corresponding level in the transition    
    * @author Margarida Castro Neves
    * 08.2016
    */
    PhysicalQuantity mixingCoefficient ;// A positive or negative number giving the squared  or the signed linear coefficient corresponding to the associated component in the expansion of the eigenstate
    QuantumNumber quantumNumber ;// Quantum number(s) describing the state
    String termSymbol ;// The term (symbol) to which this quantum state belongs, if applicable
    
    public QuantumState() {
        // TODO Auto-generated constructor stub
    }

    public PhysicalQuantity getMixingCoefficient() {
        return mixingCoefficient;
    }

    public void setMixingCoefficient(PhysicalQuantity mixingCoefficient) {
        this.mixingCoefficient = mixingCoefficient;
    }

    public QuantumNumber getQuantumNumber() {
        return quantumNumber;
    }

    public void setQuantumNumber(QuantumNumber quantumNumber) {
        this.quantumNumber = quantumNumber;
    }

    public String getTermSymbol() {
        return termSymbol;
    }

    public void setTermSymbol(String termSymbol) {
        this.termSymbol = termSymbol;
    }

}
