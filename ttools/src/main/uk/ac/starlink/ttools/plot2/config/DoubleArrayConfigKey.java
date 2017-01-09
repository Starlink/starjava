package uk.ac.starlink.ttools.plot2.config;

/**
 * Config key for a list of floating point values.
 *
 * @author   Mark Taylor
 * @since    9 Jan 2017
 */
public class DoubleArrayConfigKey extends ConfigKey<double[]> {

    /**
     * Constructor.
     *
     * @param   meta  metadata
     * @param   dflt  default value
     */
    public DoubleArrayConfigKey( ConfigMeta meta, double[] dflt ) {
        super( meta, double[].class, dflt == null ? null : dflt.clone() );
        if ( meta.getStringUsage() == null ) {
            meta.setStringUsage( "<n>,<n>,..." );
        }
    }

    public String valueToString( double[] dvals ) {
        StringBuffer sbuf = new StringBuffer();
        if ( dvals != null ) { 
            for ( int i = 0; i < dvals.length; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( "," );
                }
                sbuf.append( DoubleConfigKey.doubleToString( dvals[ i ] ) );
            }
        }
        return sbuf.toString();
    }

    public double[] stringToValue( String txt ) throws ConfigException {
        if ( txt == null || txt.trim().length() == 0 ) {
            return new double[ 0 ];
        }
        else {
            String[] txts = txt.split( ",", -1 );
            int n = txts.length;
            double[] dvals = new double[ n ];
            for ( int i = 0; i < n; i++ ) {
                dvals[ i ] = DoubleConfigKey.stringToDouble( txts[ i ], this );
            }
            return dvals;
        }
    }

    public Specifier<double[]> createSpecifier() {
        return new TextFieldSpecifier( this, new double[ 0 ] );
    }
}
