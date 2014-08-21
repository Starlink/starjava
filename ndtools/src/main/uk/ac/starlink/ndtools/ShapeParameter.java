package uk.ac.starlink.ndtools;

import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter representing an N-dimensional shape.
 */
class ShapeParameter extends Parameter<NDShape> {

    public ShapeParameter( String name ) {
        super( name, NDShape.class, false );
    }

    public NDShape stringToObject( Environment env, String stringval ) {
        return NDShape.fromString( stringval );
    }


    /**
     * Gets the value of this parameter as an NDShape object.
     */
    public NDShape shapeValue( Environment env ) throws TaskException {
        return objectValue( env );
    }
}
