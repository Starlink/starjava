package uk.ac.starlink.ndtools;

import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.task.AbortException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;

/**
 * Parameter representing an N-dimensional shape.
 */
class ShapeParameter extends Parameter {

    private NDShape shape;

    public ShapeParameter( String name ) {
        super( name );
    }

    public void setValueFromString( String stringval )
            throws ParameterValueException {
        try {
            shape = NDShape.fromString( stringval );
        }
        catch ( IllegalArgumentException e ) {
            throw new ParameterValueException( this, e.getMessage() );
        }
        super.setValueFromString( stringval );
    }


    /**
     * Gets the value of this parameter as an NDShape object.
     */
    public NDShape shapeValue() throws ParameterValueException, AbortException {
        checkGotValue();
        return shape;
    }
}
