package uk.ac.starlink.fits;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.TableData;
import nom.tam.fits.TableHDU;
import uk.ac.starlink.table.ColumnHeader;
import uk.ac.starlink.table.StarTable;

/**
 * An implementation of the StarTable interface which uses FITS TABLE
 * or BINTABLE extensions.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsStarTable implements StarTable {

    private final TableHDU thdu;
    private final int nrow;
    private final int ncol;

    /* The following are indexed by column. */
    private final ColumnHeader[] colheads;
    private final double[] scales;
    private final double[] zeros;
    private final boolean[] isScaled;
    private final long[] blanks;
    private final boolean[] hasBlank;

    /* Metadata keys. */
    private final static String TNULL_KEY = "Blank value";
    private final static String TSCAL_KEY = "SCALE";
    private final static String TZERO_KEY = "ZERO";
    private final static String TDISP_KEY = "Display format";
    private final static String TDIM_KEY = "Array arrangement";
    private final static String TBCOL_KEY = "Start column";
    private final static String TFORM_KEY = "Format";
    private final static List metadataKeyList = 
        Collections.unmodifiableList( Arrays.asList( new String[] {
            TNULL_KEY, TSCAL_KEY, TZERO_KEY, TDISP_KEY, TDIM_KEY,
            TBCOL_KEY, TFORM_KEY,
        } ) );

    /**
     * Constructs a FitsStarTable object from a FITS TableHDU object.
     *
     * @param  thdu  a TableHDU object containing data
     */
    public FitsStarTable( TableHDU thdu ) {
        this.thdu = thdu;
        nrow = thdu.getNRows();
        ncol = thdu.getNCols();
        colheads = new ColumnHeader[ ncol ];
        scales = new double[ ncol ];
        Arrays.fill( scales, 1.0 );
        zeros = new double[ ncol ];
        isScaled = new boolean[ ncol ];
        blanks = new long[ ncol ];
        hasBlank = new boolean[ ncol ];

        Header cards = thdu.getHeader();
        for ( int i = 0; i < ncol; i++ ) {
            ColumnHeader colhead = new ColumnHeader( thdu.getColumnName( i ) );
            Map metadata = new HashMap();
            colheads[ i ] = colhead;

            /* Units. */
            String tunit = cards.getStringValue( "TUNIT" + ( i + 1 ) );
            if ( tunit != null ) {
                colhead.setUnitString( tunit );
            }

            /* Format string. */
            String tdisp = cards.getStringValue( "TDISP" + ( i + 1 ) );
            if ( tdisp != null ) {
                metadata.put( TDISP_KEY, tdisp );
            }

            /* Blank value. */
            String tnull = cards.getStringValue( "TNULL" + ( i + 1 ) );
            if ( tnull != null ) {
                blanks[ i ] = Long.parseLong( tnull );
                hasBlank[ i ] = true;
                metadata.put( TNULL_KEY, tnull );
            }
                
            /* Scaling. */
            String tscal = cards.getStringValue( "TSCAL" + ( i + 1 ) );
            String tzero = cards.getStringValue( "TZERO" + ( i + 1 ) );
            if ( tscal != null ) {
                scales[ i ] = Double.parseDouble( tscal );
                metadata.put( TSCAL_KEY, tscal );
            }
            if ( tzero != null ) {
                zeros[ i ] = Double.parseDouble( tzero );
                metadata.put( TZERO_KEY, tzero );
            }
            if ( scales[ i ] != 1.0 || zeros[ i ] != 0.0 ) {
                isScaled[ i ] = true;
            }

            /* Implementation specifics. */
            String tbcol = cards.getStringValue( "TBCOL" + ( i + 1 ) );
            if ( tbcol != null ) {
                metadata.put( TBCOL_KEY, tbcol );
            }
            String tform = cards.getStringValue( "TFORM" + ( i + 1 ) );
            if ( tform != null ) {
                metadata.put( TFORM_KEY, tform );
            }
            String tdim = cards.getStringValue( "TDIM" + ( i + 1 ) );
            if ( tdim != null ) {
                metadata.put( TDIM_KEY, tdim );
            }

            /* Set the column metadata attribute. */
            colhead.setMetadata( metadata );

            /* Class of column. */
            Class cls = Object.class;
            if ( isScaled[ i ] ) {
                colhead.setContentClass( Double.class );
            }
            else if ( nrow > 0 ) {
                Object test = getValueAt( 0, i );
                if ( test != null ) {
                    colhead.setContentClass( test.getClass() );
                }
            }
        }
    }

    public int getRowCount() {
        return nrow;
    }

    public int getColumnCount() {
        return ncol;
    }

    public Object getValueAt( int irow, int icol ) {

        /* Get the cell contents from the fits table. */
        Object base;
        try {
            base = thdu.getElement( irow, icol );
        }
        catch ( FitsException e ) {
            e.printStackTrace();
            return "???";
        }

        /* Data is normally returned as a 1-element array. */
        Class cls = base.getClass().getComponentType();
        if ( cls != null && Array.getLength( base ) == 1 ) {
            boolean hasblank = hasBlank[ icol ];
            long blank = blanks[ icol ]; 
            boolean scaled = isScaled[ icol ];
            double scale = scales[ icol ];
            double zero = zeros[ icol ];

            /* Need to check for blank values and scale for integer types. */
            if ( cls == byte.class ) {
                byte val = ((byte[]) base)[ 0 ];
                return ( hasblank && val == (byte) blank ) 
                       ? null
                       : ( scaled ? (Number) new Double( val * scale + zero )
                                  : (Number) new Byte( val ) );
            }
            else if ( cls == short.class ) {
                short val = ((short[]) base )[ 0 ];
                return ( hasblank && val == (short) blank )
                       ? null
                       : ( scaled ? (Number) new Double( val * scale + zero )
                                  : (Number) new Short( val ) );
            }
            else if ( cls == int.class ) {
                int val = ((int[]) base )[ 0 ];
                return ( hasblank && val == (int) blank )
                       ? null
                       : ( scaled ? (Number) new Double( val * scale + zero )
                                  : (Number) new Integer( val ) );
            }
            else if ( cls == long.class ) {
                long val = ((long[]) base )[ 0 ];
                return ( hasblank && val == (long) blank )
                       ? null
                       : ( scaled ? (Number) new Double( val * scale + zero )
                                  : (Number) new Long( val ) );
            }

            /* Need to scale for floating point types. */
            else if ( cls == float.class ) {
                float val = ((float[]) base )[ 0 ];
                return scaled ? (Number) new Double( val * scale + zero )
                              : (Number) new Float( val );
            }
            else if ( cls == double.class ) {
                double val = ((double[]) base )[ 0 ];
                return scaled ? (Number) new Double( val * scale + zero )
                              : (Number) new Double( val );
            }

            /* Just dereference for string or booleans. */
            else if ( cls == boolean.class ) {
                return new Boolean( ((boolean[]) base)[ 0 ] );
            }
            else if ( cls == String.class ) {
                return new String( ((String[]) base)[ 0 ] );
            }
        }

        /* If it's not a 1-element array, just return the object as 
         * presented. */
        return base;
    }

    public ColumnHeader getHeader( int icol ) {
        return colheads[ icol ];
    }

    public List getColumnMetadataKeys() {
        return metadataKeyList;
    }
}
