package uk.ac.starlink.splat.data.ssldm;

/*****************************************
 * IVOA Simple Spectral Line Data Model V1.0
 ********************************************
 */
public class PhysicalQuantity {

    double value = 0;
    double error = 0;
    
    Unit   unit = null;
    String ucd = null;
    String utype = null;
    
    boolean valueSet=false;
    boolean errorSet=false;
    
    public PhysicalQuantity() {
       
    }

    
    public PhysicalQuantity(double value, double error, String unit) {
        setValue(value);
        setError(error);
        this.unit = new Unit(unit);
    }
    
    public PhysicalQuantity(double value, String unit) {
        setValue(value);
        this.unit = new Unit(unit);
    }

    public double getValue() {
        // to do check if value is set?
        return value;
    }

    public void setValue(double value) {
        this.value = value;
        this.valueSet=true;
    }

    public double getError() {
     // to do check if error is set?
    	if (this.errorSet)
    		return error;
        return (Double) null;
    }

    public void setError(double error) {
        this.error = error;
        this.errorSet=true;
    }

    public Unit getUnit() {
        // to do check if unit is set?
        return unit;
    }
    
    public String getUnitExpression() {
        if (unit != null)
            return unit.getExpression();
        else return null;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }
    
    public String getString() {
        String s = "";
        if (valueSet)
            s += value;
        if (this.unit != null)
            s+= " "+this.unit.getExpression();
       
        return s;
    }
    

    public String getUcd() {
        return ucd;
    }


    public void setUcd(String ucd) {
        this.ucd = ucd;
    }


    public String getUtype() {
        return utype;
    }


    public void setUtype(String utype) {
        this.utype = utype;
    }


    public boolean isErrorSet() {        
        return errorSet;
    }
    public boolean isValueSet() {        
        return valueSet;
    }


}
