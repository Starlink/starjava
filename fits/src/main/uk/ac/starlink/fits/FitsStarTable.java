package uk.ac.starlink.fits;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.TableData;
import nom.tam.fits.TableHDU;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RandomStarTable;
import uk.ac.starlink.table.ValueInfo;

/**
 * An implementation of the StarTable interface which uses FITS TABLE
 * or BINTABLE extensions.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsStarTable extends RandomStarTable {

    private final TableHDU thdu;
    private final int nrow;
    private final int ncol;

    /* The following are indexed by column. */
    private final ColumnInfo[] colinfos;
    private final double[] scales;
    private final double[] zeros;
    private final boolean[] isScaled;
    private final long[] blanks;
    private final boolean[] hasBlank;

    /* Auxiliary metadata for columns. */
    private final static ValueInfo tnullInfo = new DefaultValueInfo(
        "Blank",
        Long.class,
        "Bad value indicator (TNULLn card)" );
    private final static ValueInfo tscalInfo = new DefaultValueInfo(
        "Scale",
        Double.class,
        "Multiplier for values (TSCALn card)" );
    private final static ValueInfo tzeroInfo = new DefaultValueInfo(
        "Zero",
        Double.class,
        "Offset for values (TZEROn card)" );
    private final static ValueInfo tdispInfo = new DefaultValueInfo(
        "Format",
        String.class, 
        "Display format in FORTRAN notation (TDISPn card)" );
    private final static ValueInfo tbcolInfo = new DefaultValueInfo(
        "Start column",
        Integer.class,
        "Start column for data (TBCOLn card)" );
    private final static ValueInfo tformInfo = new DefaultValueInfo(
        "Format code",
        String.class,
        "Data type code (TFORMn card)" );
    private final static List auxDataInfos = Arrays.asList( new ValueInfo[] {
        tnullInfo, tscalInfo, tzeroInfo, tdispInfo, tbcolInfo, tformInfo,
    } );

    /**
     * Constructs a FitsStarTable object from a FITS TableHDU object.
     *
     * @param  thdu  a TableHDU object containing data
     */
    public FitsStarTable( TableHDU thdu ) throws IOException {
        this.thdu = thdu;
        nrow = thdu.getNRows();
        ncol = thdu.getNCols();
        colinfos = new ColumnInfo[ ncol ];
        scales = new double[ ncol ];
        Arrays.fill( scales, 1.0 );
        zeros = new double[ ncol ];
        isScaled = new boolean[ ncol ];
        blanks = new long[ ncol ];
        hasBlank = new boolean[ ncol ];

        Header cards = thdu.getHeader();
        for ( int icol = 0; icol < ncol; icol++ ) {
            int jcol = icol + 1;
            ColumnInfo cinfo = new ColumnInfo( thdu.getColumnName( icol ) );
            List auxdata = cinfo.getAuxData();
            colinfos[ icol ] = cinfo;

            /* Units. */
            String tunit = cards.getStringValue( "TUNIT" + jcol );
            if ( tunit != null ) {
                cinfo.setUnitString( tunit );
            }

            /* Format string. */
            String tdisp = cards.getStringValue( "TDISP" + jcol );
            if ( tdisp != null ) {
                auxdata.add( new DescribedValue( tdispInfo, tdisp ) );
            }

            /* Blank value. */
            String tnull = cards.getStringValue( "TNULL" + jcol );
            if ( tnull != null ) {
                long nullval = Long.parseLong( tnull );
                blanks[ icol ] = nullval;
                hasBlank[ icol ] = true;
                auxdata.add( new DescribedValue( tnullInfo, 
                                                 new Long( nullval ) ) );
            }
            else {
                cinfo.setNullable( false );
            }
                
            /* Shape. */
            String tdim = cards.getStringValue( "TDIM" + jcol );
            if ( tdim != null ) {
                tdim = tdim.trim();
                if ( tdim.charAt( 0 ) == '(' && 
                     tdim.charAt( tdim.length() - 1 ) == ')' ) {
                    tdim = tdim.substring( 1, tdim.length() - 1 ).trim();
                    String[] sdims = tdim.split( "," );
                    if ( sdims.length > 0 ) {
                        try {
                            int[] dims = new int[ sdims.length ];
                            for ( int i = 0; i < sdims.length; i++ ) {
                                dims[ i ] = Integer.parseInt( sdims[ i ] );
                            }
                            cinfo.setShape( dims );
                        }
                        catch ( NumberFormatException e ) {
                            // can't set shape
                        }
                    }
                }
            }

            /* Scaling. */
            String tscal = cards.getStringValue( "TSCAL" + jcol );
            String tzero = cards.getStringValue( "TZERO" + jcol );
            if ( tscal != null ) {
                double scalval = Double.parseDouble( tscal );
                scales[ icol ] = scalval;
                auxdata.add( new DescribedValue( tscalInfo,
                                                 new Double( scalval ) ) );
            }
            if ( tzero != null ) {
                double zeroval = Double.parseDouble( tzero );
                zeros[ icol ] = zeroval;
                auxdata.add( new DescribedValue( tzeroInfo,
                                                 new Double( zeroval ) ) );
            }
            if ( scales[ icol ] != 1.0 || zeros[ icol ] != 0.0 ) {
                isScaled[ icol ] = true;
            }

            /* Implementation specifics. */
            String tbcol = cards.getStringValue( "TBCOL" + jcol );
            if ( tbcol != null ) {
                int bcolval = Integer.parseInt( tbcol );
                auxdata.add( new DescribedValue( tbcolInfo, 
                                                 new Integer( bcolval ) ) );
            }
            String tform = cards.getStringValue( "TFORM" + jcol );
            if ( tform != null ) {
                auxdata.add( new DescribedValue( tformInfo, tform ) );
            }

            /* Class of column. */
            Class cls = Object.class;
            if ( isScaled[ icol ] ) {
                cinfo.setContentClass( Double.class );
            }
            else if ( nrow > 0 ) {
                Object test = getCell( 0, icol );
                if ( test != null ) {
                    cinfo.setContentClass( test.getClass() );
                }
            }
        }
    }

    public long getRowCount() {
        return (long) nrow;
    }

    public int getColumnCount() {
        return ncol;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colinfos[ icol ];
    }

    public List getColumnAuxDataInfos() {
        return auxDataInfos;
    }

    public Object getCell( long lrow, int icol ) throws IOException {
        int irow = checkedLongToInt( lrow );
        try {
            return packageValue( thdu.getElement( irow, icol ), icol );
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException().initCause( e );
        }
    }

    public Object[] getRow( long lrow ) throws IOException {
        int irow = checkedLongToInt( lrow );
        Object[] row;
        try {
            row = thdu.getRow( irow );
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException().initCause( e );
        }
        for ( int icol = 0; icol < ncol; icol++ ) {
            row[ icol ] = packageValue( row[ icol ], icol );
        }
        return row;
    }

    /**
     * Turns the object got from the fits getElement call into the
     * object we want to return from this table.  That includes
     * scaling it if necessary, spotting blank values, and turning 
     * it from a 1-element array to a Number object.
     *
     * @param   base  the object got from the fits table
     * @param   icol  the column from which <tt>base</tt> comes
     * @return  the object representing the value of the cell
     */
    private Object packageValue( Object base, int icol ) {
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
}
