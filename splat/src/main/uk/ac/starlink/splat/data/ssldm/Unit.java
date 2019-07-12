package uk.ac.starlink.splat.data.ssldm;
/*
 ********************************************
 * IVOA Simple Spectral Line Data Model V1.0
 ********************************************
 */
public class Unit {
    /* 
     * Class representing the unit a physical quantity is expressed with
     * @author Margarida Castro Neves
     * 08.2016
     */
    String expression;
    double scaleSI;
    String dimEquation;
    public Unit() {
        
    }
    
    public Unit(String unitexp) {
        this.expression = unitexp;
    }
    
    public String getExpression() {
        return expression;
    }
    public void setExpression(String expression) {
        this.expression = expression;
    }
    public double getScaleSI() {
        return scaleSI;
    }
    public void setScaleSI(double scaleSI) {
        this.scaleSI = scaleSI;
    }
    public String getDimEquation() {
        return dimEquation;
    }
    public void setDimEquation(String dimEquation) {
        this.dimEquation = dimEquation;
    }

    
}
