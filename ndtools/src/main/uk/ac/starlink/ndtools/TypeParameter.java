package uk.ac.starlink.ndtools;

import java.util.Iterator;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.task.AbortException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;

/**
 * A Parameter representing one of the primitive numeric types known by
 * the NDArray package.
 * 
 * @see uk.ac.starlink.array.Type
 */
class TypeParameter extends Parameter {

    private Type typeval;

    public TypeParameter( String name ) {
        super( name );
    }

    /**
     * Returns the numeric primitive Type represented by this parameter.
     */
    public Type typeValue() throws ParameterValueException, AbortException {
        checkGotValue();
        return typeval;
    }

    public void setValueFromString( String stringval )
            throws ParameterValueException {
        String typename = stringval;
        for ( Iterator it = Type.allTypes().iterator(); it.hasNext(); ) {
            Type type = (Type) it.next();
            if ( typename.equalsIgnoreCase( type.toString() ) ) {
                typeval = type;
                super.setValueFromString( stringval );
            }
        }

        /* Didn't find one. */
        StringBuffer sb = new StringBuffer( "Known types are:" );
        for ( Iterator it = Type.allTypes().iterator(); it.hasNext(); ) {
            sb.append( " " ).append( it.next().toString() );
        }
        throw new ParameterValueException( this, sb.toString() );
    }
}
