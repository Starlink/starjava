package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.votable.DataFormat;

/**
 * Parameter which represents one of the VOTable encoding formats.
 *
 * @author   Mark Taylor (Starlink)
 * @since    15 Aug 2005
 */
public class VotFormatParameter extends Parameter {

    private static DataFormat[] VOT_FORMATS = new DataFormat[] {
        DataFormat.TABLEDATA,
        DataFormat.BINARY,
        DataFormat.FITS,
    };

    private DataFormat dataFormat_;

    public VotFormatParameter( String name ) {
        super( name );
        StringBuffer usage = new StringBuffer();
        for ( int i = 0; i < VOT_FORMATS.length; i++ ) {
            usage.append( VOT_FORMATS[ i ].toString().toLowerCase() )
                 .append( '|' );
        }
        usage.append( "empty" );
        setUsage( usage.toString() );
        setDefault( DataFormat.TABLEDATA.toString().toLowerCase() );
    }

    /**
     * Returns the value of this parameter as a DataFormat object.
     *
     * @param  env  execution environment
     * @return  data format - may be null to indicate empty
     */
    public DataFormat formatValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return dataFormat_;
    }

    public void setValueFromString( Environment env, String sval ) 
            throws TaskException {
        for ( int i = 0; i < VOT_FORMATS.length; i++ ) {
            DataFormat format = VOT_FORMATS[ i ];
            if ( format.toString().equalsIgnoreCase( sval ) ) {
                dataFormat_ = format;
                super.setValueFromString( env, sval );
                return;
            }
        }
        if ( sval == null || sval.equalsIgnoreCase( "empty" ) ) {
            dataFormat_ = null;
            super.setValueFromString( env, sval );
            return;
        }
        throw new ParameterValueException( this, "Unknown format " + sval );
    }

}
