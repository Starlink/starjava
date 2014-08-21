package uk.ac.starlink.ndtools;

import java.util.Iterator;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.TaskException;

/**
 * A Parameter representing one of the primitive numeric types known by
 * the NDArray package.
 * 
 * @see uk.ac.starlink.array.Type
 */
class TypeParameter extends ChoiceParameter<Type> {

    public TypeParameter( String name ) {
        super( name, Type.class,
               (Type[]) Type.allTypes().toArray( new Type[ 0 ] ) );
    }

    /**
     * Returns the numeric primitive Type represented by this parameter.
     */
    public Type typeValue( Environment env ) throws TaskException {
        return objectValue( env );
    }
}
