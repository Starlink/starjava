package uk.ac.starlink.splat.data.ssldm;
/*
 ********************************************
 * IVOA Simple Spectral Line Data Model V1.0
 ********************************************
 */
public class QuantumNumber {

    /* 
     * Class representing the quantum numbers describing each quantum state
     * @author Margarida Castro Neves
     * 08.2016
     */
    
    String label ;//  Human readable string representing the name of the quantum number. It is a string like “F”, “J”, “I1”, etc.,
    String type ; // String representing the quantum number, recommended within this model (c.f. Chapter 4)
    PhysicalQuantity numeratorValue ;// The numerator of the quantum number value. For non half-integers, this number would just represent the value of the quantum number
    PhysicalQuantity denominatorValue; // The denominator of the quantum number value. It defaults to 1 (one) if not explicitly input (as would be the case for non half-integer numbers)
    String description; // A human readable string, describing the nature of the quantum number
    
    public QuantumNumber() {
        // TODO Auto-generated constructor stub
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PhysicalQuantity getNumeratorValue() {
        return numeratorValue;
    }

    public void setNumeratorValue(PhysicalQuantity numeratorValue) {
        this.numeratorValue = numeratorValue;
    }

    public PhysicalQuantity getDenominatorValue() {
        return denominatorValue;
    }

    public void setDenominatorValue(PhysicalQuantity denominatorValue) {
        this.denominatorValue = denominatorValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
