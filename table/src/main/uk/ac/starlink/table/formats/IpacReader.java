package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.TimeMapper;
import uk.ac.starlink.table.ValueInfo;

/**
 * Reads and parses IPAC-format tables.
 * The data format is defined at
 * <a href="http://irsa.ipac.caltech.edu/applications/DDGEN/Doc/ipac_tbl.html"
 *     >http://irsa.ipac.caltech.edu/applications/DDGEN/Doc/ipac_tbl.html</a>.
 *
 * @author   Mark Taylor
 * @since    7 Feb 2006
 */
class IpacReader implements RowSequence {

    private final InputStream in_;
    private final int[] ends_;
    private final String[] tokens_;
    private final ColumnReader[] colReaders_;
    private final DescribedValue[] params_;
    private final LineSequence lseq_;
    private String dataLine_;
    private String[] dataTokens_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.formats" );
    private static final boolean WORKAROUND_TRUNCATION = true;

    /**
     * Constructor.
     *
     * @param  in  input stream supplying the IPAC table
     */
    public IpacReader( InputStream in ) throws IOException {

        /* Set up a reader to supply lines one at a time. */
        in_ = in;
        lseq_ = new LineSequence( in );

        /* Read the table parameters (leading lines starting with a
         * backslash). */
        params_ = readParameters( lseq_ );

        /* Work out what the ending character column is for each data
         * column in the table. */
        ends_ = readEnds( lseq_ );
        int ncol = ends_.length;
        tokens_ = new String[ ncol ];

        /* Read and parse the lines giving column metadata. */
        String[] hlines = readHeaderLines( lseq_ );
        if ( hlines.length < 2 ) {
            throw new TableFormatException( "Not enough header lines" );
        }
        String[] colNames = readHeaderTokens( hlines[ 0 ] );
        String[] colTypes = readHeaderTokens( hlines[ 1 ] );
        String[] colUnits = hlines.length >= 3 ? readHeaderTokens( hlines[ 2 ] )
                                               : new String[ ncol ];
        String[] colNulls = hlines.length >= 4 ? readHeaderTokens( hlines[ 3 ] )
                                               : new String[ ncol ];

        /* Construct column reader objects for each column. */
        colReaders_ = new ColumnReader[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnReader cr =
                createColumnReader( colNames[ icol ], colTypes[ icol ],
                                    colUnits[ icol ], colNulls[ icol ] );
            ColumnInfo info = cr.getInfo();
            if ( info.getContentClass().equals( String.class ) ) {
                info.setElementSize( ends_[ icol ] - 
                                     ( icol == 0 ? 0 : ends_[ icol - 1 ] ) );
            }
            colReaders_[ icol ] = cr;
        }
    }

    /**
     * Returns the number of columns in this IPAC table.
     *
     * @return   column count
     */
    public int getColumnCount() {
        return colReaders_.length;
    }

    /**
     * Returns the metadata for a given column in this IPAC table.
     *
     * @param  icol  column index
     * @return  column metadata
     */
    public ColumnInfo getColumnInfo( int icol ) {
        return colReaders_[ icol ].getInfo();
    }

    /**
     * Returns an array of the parameter metadata objects 
     * associated with this IPAC table.
     *
     * @return  table parameters
     */
    public DescribedValue[] getParameters() {
        return params_;
    }

    public boolean next() throws IOException {
        dataLine_ = lseq_.nextLine();
        dataTokens_ = null;
        return dataLine_ != null;
    }

    public Object getCell( int icol ) throws IOException {
        return colReaders_[ icol ].readValue( getDataTokens()[ icol ] );
    }

    public Object[] getRow() throws IOException {
        int ncol = colReaders_.length;
        String[] tokens = getDataTokens();
        Object[] row = new Object[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            row[ icol ] = colReaders_[ icol ].readValue( tokens[ icol ] );
        }
        return row;
    }

    public void close() throws IOException {
        in_.close();
    }

    /**
     * Returns the array of per-column data tokens for the current data row,
     * if necessary first reading it from the current row text.
     *
     * @return   data token array
     */
    private String[] getDataTokens() throws IOException {
        if ( dataLine_ == null ) {
            throw new IllegalStateException( "next() not yet called" );
        }
        if ( dataTokens_ == null ) {
            dataTokens_ = readDataTokens( dataLine_ );
        }
        return dataTokens_;
    }

    /**
     * Parses a header line and returns the tokens.
     *
     * @return   array of trimmed strings giving the per-column contents of
     *           a header line; '|' characters are omitted
     */
    private String[] readHeaderTokens( String line ) {
        int ipos = 1;
        String[] tokens = new String[ ends_.length ];
        for ( int icol = 0; icol < tokens.length; icol++ ) {
            String token = line.substring( ipos, ends_[ icol ] - 1 );
            token = token.replace( '-', ' ' ).trim();
            tokens[ icol ] = token;
            ipos = ends_[ icol ];
        }
        return tokens;
    }

    /**
     * Parses a data line and returns the tokens.
     *
     * @return  array of trimmed strings giving the per-column contents of
     *          a data line
     */
    private String[] readDataTokens( String line ) {
        int ipos = 0;
        String[] tokens = tokens_;
        int leng = line.length();
        for ( int icol = 0; icol < tokens.length; icol++ ) {
            tokens[ icol ] = 
                line.substring( ipos, Math.min( ends_[ icol ], leng ) ).trim();
            ipos = ends_[ icol ];
        }
        return tokens;
    }

    /**
     * Reads all the header lines next up in a line sequence.
     * Header lines are ones that start with a '|' character.
     *
     * @param  lseq  line sequence supplying input, positioned at the start
     *         of header lines
     * @return  array of header lines
     */
    private static String[] readHeaderLines( LineSequence lseq )
            throws IOException {
        List<String> lines = new ArrayList<String>();
        boolean done = false;
        for ( String line; ! done && ( line = lseq.nextLine() ) != null; ) {
            if ( line.length() > 0 ) {
                if ( line.charAt( 0 ) == '|' ) {
                    lines.add( line );
                }
                else {
                    lseq.replaceLine( line );
                    done = true;
                }
            }
        }
        return lines.toArray( new String[ 0 ] );
    }

    /**
     * Examines a line from the line sequence and determines the end 
     * positions of each data column.  The result is an array in which
     * the Nth element contains the (0-based) index of the final
     * character column forming the Nth data column.
     * 
     * @param  lseq  line sequence positioned at the start of the header lines
     * @return  array of column indices
     */
    private static int[] readEnds( LineSequence lseq ) throws IOException {
        boolean done = false;
        for ( String line; ! done && ( line = lseq.nextLine() ) != null; ) {
            line = line.trim();
            if ( line.length() > 1 ) {
                if ( line.charAt( 0 ) != '|' ) {
                    throw new TableFormatException( "Bad header line, "
                                                  + "should start with '|': \""
                                                  + line + "\"" );
                }
                else {
                    lseq.replaceLine( line );
                    return readEnds( line );
                }
            }
        }
        throw new TableFormatException( "No header lines found" );
    }

    /**
     * Examines the text of a line and determines the end positions of each
     * data column.  The result is an array in which the Nth element 
     * contains the (0-based) index of the final character column forming
     * the Nth data column.
     *
     * @param  line   text of a header line
     * @return  array of column indices
     */
    private static int[] readEnds( String line ) {
        int[] ends1 = new int[ line.length() ];
        int icol = 0;
        for ( int ipos = 1; ipos < line.length(); ipos++ ) {
            if ( line.charAt( ipos ) == '|' ) {
                ends1[ icol++ ] = ipos + 1;
            }
        }
        int[] ends = new int[ icol ];
        System.arraycopy( ends1, 0, ends, 0, icol );
        return ends;
    }

    /**
     * Returns an array of the parameters appearing in a line sequence.
     * Parameters are of the form
     * <pre>
     *    \ comment
     * </pre>
     * or
     * <pre>
     *    \name = value
     * </pre>
     *
     * @param  lseq  line sequence positioned at start of parameters
     * @return  array of parameter metadata objects
     */
    private static DescribedValue[] readParameters( LineSequence lseq )
            throws IOException {
        List<String> comments = new ArrayList<String>();
        List<DescribedValue> params = new ArrayList<DescribedValue>();
        boolean done = false;
        for ( String line; ! done && ( line = lseq.nextLine() ) != null; ) {
            line = line.trim();
            if ( line.length() > 1 ) {
                if ( line.charAt( 0 ) == '\\' ) {
                    if ( line.charAt( 1 ) == ' ' ) {
                        comments.add( line.substring( 2 ) );
                    }
                    else {
                        params.add( parseParameter( line.substring( 1 ) ) );
                    }
                }
                else {
                    lseq.replaceLine( line );
                    done = true;
                }
            }
        }
        if ( comments.size() > 0 ) {
            StringBuffer comBuf = new StringBuffer();
            for ( Iterator<String> it = comments.iterator(); it.hasNext(); ) {
                comBuf.append( it.next() );
                if ( it.hasNext() ) {
                    comBuf.append( '\n' );
                }
            }
            params.add( new DescribedValue( IpacTableBuilder.COMMENT_INFO,
                                            comBuf.toString() ) );
        }
        return params.toArray( new DescribedValue[ 0 ] );
    }

    /**
     * Turns a line from an IPAC table representing a name=value parameter
     * setting into a DescribedValue.
     *
     * @param   line   line of text
     * @return  parameter metadata object
     */
    private static DescribedValue parseParameter( String line )
            throws TableFormatException {
        int ieq = line.indexOf( '=' );
        if ( ieq < 0 ) {
            throw new TableFormatException( "Bad \\name=value line \"" + line 
                                          + "\"" );
        }
        String name = line.substring( 0, ieq ).trim();
        String sval = line.substring( ieq + 1 ).trim();
        int sleng = sval.length();
        if ( sval.length() > 1 &&
             ( sval.charAt( 0 ) == '\'' && sval.charAt( sleng - 1 ) == '\'' ||
               sval.charAt( 0 ) == '"' && sval.charAt( sleng - 1 ) == '"' ) ) {
            return createParameter( name, sval.substring( 1, sleng - 1 ) );
        }
        else if ( sval.length() == 0 ) {
            return createParameter( name, "" );
        }
        else if ( sval.equals( "T" ) ) {
            return createParameter( name, Boolean.TRUE );
        }
        else if ( sval.equals( "F" ) ) {
            return createParameter( name, Boolean.FALSE );
        }
        else {
            try {
                return createParameter( name, Integer.valueOf( sval ) );
            }
            catch ( NumberFormatException e ) {
            }
            try {
                return createParameter( name, Long.valueOf( sval ) );
            }
            catch ( NumberFormatException e ) {
            }
            try {
                return createParameter( name, Double.valueOf( sval ) );
            }
            catch ( NumberFormatException e ) {
            }
            return createParameter( name, sval );
        }
    }

    /**
     * Creates a new parameter metadata object given its name and value.
     * The content class is just the class of the given value.
     *
     * @param  name  parameter name
     * @param  value  parameter value
     */
    private static DescribedValue createParameter( String name, Object value ) {
        Class<?> clazz = value == null ? String.class : value.getClass();
        return new DescribedValue( new DefaultValueInfo( name, clazz ), value );
    }

    /**
     * Constructs a ColumnReader object suitable for a column in an IPAC table.
     *
     * @param   name  column name
     * @param   type  IPAC column type string
     * @param   unit  column unit string
     * @param   blank null value representation string, if any
     */
    private static ColumnReader createColumnReader( String name, String type,
                                                    String unit, String blank ) 
            throws TableFormatException {
        DefaultValueInfo info = new DefaultValueInfo( name );
        if ( unit != null && unit.trim().length() > 0 ) {
            info.setUnitString( unit );
        }
        final String blankVal = ( blank == null || blank.trim().length() == 0 )
                              ? null
                             : blank.trim();
        final boolean hasBlank = blankVal != null;
        if ( typeMatch( type, "int" ) || type.equalsIgnoreCase( "i" ) ) {
            info.setContentClass( Integer.class );
            info.setNullable( hasBlank );
            return new ColumnReader( info ) {
                Object readValue( String token ) {
                    if ( hasBlank && blankVal.equals( token ) ) {
                        return null;
                    }
                    else {
                        try {
                            return Integer.valueOf( token );
                        }
                        catch ( NumberFormatException e ) {
                            return null;
                        }
                    }
                }
            };
        }
        else if ( typeMatch( type, "long" ) || type.equalsIgnoreCase( "l" ) ) {
            info.setContentClass( Long.class );
            return new ColumnReader( info ) {
                Object readValue( String token ) {
                    if ( hasBlank && blankVal.equals( token ) ) {
                        return null;
                    }
                    else {
                        try {
                            return Long.valueOf( token );
                        }
                        catch ( NumberFormatException e ) {
                            return null;
                        }
                    }
                }
            };
        }
        else if ( typeMatch( type, "double" ) ||
                  type.equalsIgnoreCase( "d" ) ) {
            info.setContentClass( Double.class );
            return new ColumnReader( info ) {
                Object readValue( String token ) {
                    if ( hasBlank && blankVal.equals( token ) ) {
                        return null;
                    }
                    else {
                        try {
                            return Double.valueOf( token );
                        }
                        catch ( NumberFormatException e ) {
                            return null;
                        }
                    }
                }
            };
        }
        else if ( typeMatch( type, "float" ) || type.equalsIgnoreCase( "f" ) ||
                  typeMatch( type, "real" ) || type.equalsIgnoreCase( "r" ) ) {
            info.setContentClass( Float.class );
            return new ColumnReader( info ) {
                Object readValue( String token ) {
                    if ( hasBlank && blankVal.equals( token ) ) {
                        return null;
                    }
                    else {
                        try {
                            return Float.valueOf( token );
                        }
                        catch ( NumberFormatException e ) {
                            return null;
                        }
                    }
                }
            };  
        }
        else if ( typeMatch( type, "char" ) || type.equalsIgnoreCase( "c" ) ) {
            info.setContentClass( String.class );
            return new ColumnReader( info ) {
                Object readValue( String token ) {
                    if ( hasBlank && blankVal.equals( token ) ) {
                        return null;
                    }
                    else {
                        return token;
                    }
                }
            };
        }
        else if ( typeMatch( type, "date" ) ) {
            info.setContentClass( String.class );
            info.setXtype( "timestamp" );
            info.setUCD( "time.epoch" );
            info.setDomainMappers( new DomainMapper[] { TimeMapper.ISO_8601 } );
            info.setNullable( hasBlank );
            return new ColumnReader( info ) {
                Object readValue( String token ) {
                    if ( hasBlank && blankVal.equals( token ) ) {
                        return null;
                    }
                    else {
                        return token;
                    }
                }
            };
        }
        else {
            throw new TableFormatException( "Unknown IPAC data type " + type );
        }
    }

    /**
     * Determines whether a given type matches one of the defined IPAC types.
     * This does not handle the case of the special 1-character abbreviations.
     *
     * <p>Type name matching is case-insensitive.
     * Although the IPAC specification originally listed only lower case
     * type names, the last time I checked (it says "Version 1.2 5/23/13")
     * there are hints that accepting upper case type names is a good
     * idea, and I have encountered at least some such in the wild.
     *
     * @param  type  supplied type from file
     * @param  name  IPAC-defined type name
     * @return  true iff type matches name
     */
    private static boolean typeMatch( String type, String name ) {

        /* Equality is a match. */
        if ( name.equalsIgnoreCase( type ) ) {
            return true;
        }

        /* Many IRSA catalogues violate the documented IPAC rules and 
         * provide type names truncated to whatever length is convenient,
         * e.g. "doub" for "double".  Work around this here, by calling
         * any match of more than one character a match. */
        if ( WORKAROUND_TRUNCATION ) {
            int tleng = type.length();
            if ( tleng > 1 && tleng < name.length() ) {
                for ( int i = 0; i < tleng; i++ ) {
                    if ( Character.toLowerCase( type.charAt( i ) ) !=
                         Character.toLowerCase( name.charAt( i ) ) ) {
                        return false;
                    }
                }
                logger_.info( "Assume declared IPAC data type \"" + type 
                            + "\" means \"" + name
                            + "\" (illegal truncation)" );
                return true;
            }
        }
        return false;
    }

    /**
     * Interface for an object which can decode strings in one of the columns
     * of an IPAC table.
     */
    private static abstract class ColumnReader {
        final ColumnInfo info_;

        /**
         * Constructor.
         *
         * @param   info  base metadata
         */
        ColumnReader( ValueInfo info ) {
            info_ = new ColumnInfo( info );
        }

        /**
         * Returns the metadata object for the column this reader can read.
         *
         * @return   column metadata
         */
        ColumnInfo getInfo() {
            return info_;
        }

        /**
         * Decodes a string value representation, returning an object.
         *
         * @param  token  trimmed string representation of a value in the column
         * @return  value object which is the decoded form of <code>token</code>
         */
        abstract Object readValue( String token );
    }
}
