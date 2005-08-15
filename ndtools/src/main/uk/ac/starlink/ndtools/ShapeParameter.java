package uk.ac.starlink.ndtools;

import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter representing an N-dimensional shape.
 */
class ShapeParameter extends Parameter {

    private NDShape shape;

    public ShapeParameter( String name ) {
        super( name );
    }

    public void setValueFromString( Environment env, String stringval )
            throws TaskException {
        try {
            shape = NDShape.fromString( stringval );
        }
        catch ( IllegalArgumentException e ) {
            throw new ParameterValueException( this, e.getMessage() );
        }
        super.setValueFromString( env, stringval );
    }


    /**
     * Gets the value of this parameter as an NDShape object.
     */
    public NDShape shapeValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return shape;
    }
}
