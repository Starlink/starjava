package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.RowData;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.WrapperRowAccess;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.util.IOUtils;

/**
 * Output handler for Tab-Separated Table format.
 * This is used by GAIA/SkyCat amongst other software.
 * Documentation of the format can be found in Starlink System Note 75
 * (<a href="http://www.starlink.rl.ac.uk/star/docs/ssn75.htx">SSN/75</a>).
 *
 * @author   Mark Taylor
 * @since    27 Jul 2006
 */
public class TstTableWriter extends DocumentedStreamStarTableWriter {

    /** Longest field that will get written without truncation. */
    private final static int MAX_CHARS = 10240;

    private static final ColumnInfo ID_INFO =
        new ColumnInfo( "ID", String.class, "Identifier" );
    private static final ColumnInfo RA_INFO =
        new ColumnInfo( "RA", Number.class, "Right Ascension J2000" );
    private static final ColumnInfo DEC_INFO =
        new ColumnInfo( "DEC", Number.class, "Declination J2000" );
    private static final ColumnInfo X_INFO =
        new ColumnInfo( "X", Number.class, "X pixel index" );
    private static final ColumnInfo Y_INFO =
        new ColumnInfo( "Y", Number.class, "Y pixel index" );
    private static final ValueInfo LABEL_INFO = 
        new DefaultValueInfo( "TstTableWriter.Label", String.class,
                              "Identifier private to this class" );
    static {
        ID_INFO.setAuxDatum( new DescribedValue( LABEL_INFO, "ID" ) );
        RA_INFO.setAuxDatum( new DescribedValue( LABEL_INFO, "RA" ) );
        DEC_INFO.setAuxDatum( new DescribedValue( LABEL_INFO, "DEC" ) );
        X_INFO.setAuxDatum( new DescribedValue( LABEL_INFO, "X" ) );
        Y_INFO.setAuxDatum( new DescribedValue( LABEL_INFO, "Y" ) );
    }

    public TstTableWriter() {
        super( new String[] { "tst" } );
    }

    /**
     * Returns "TST".
     *
     * @return  format name
     */
    public String getFormatName() {
        return "TST";
    }

    /**
     * Returns "text/plain".
     *
     * @return  MIME type
     */
    public String getMimeType() {
        return "text/plain";
    }

    public boolean docIncludesExample() {
        return true;
    }

    public String getXmlDescription() {
        return readText( "TstTableWriter.xml" );
    }

    public void writeStarTable( StarTable st, OutputStream out )
            throws IOException {

        /* Prepare the table. */
        st = prepareTable( st );

        /* Write some sort of heading. */
        String tname = st.getName();
        URL turl = st.getURL();
        if ( tname != null && tname.trim().length() > 0 ) {
            printLine( out, tname );
        }
        else if ( turl != null ) {
            printLine( out, turl.toString().replaceFirst( "^.*[:/]*", "" ) );
        }
        else {
            printLine( out, "# Tab Separated Table" );
        }
        printLine( out, "" );

        /* Write parameters. */
        if ( ! st.getParameters().isEmpty() ) {
            printLine( out, "# Table parameters" );
            for ( DescribedValue dval : st.getParameters() ) {
                String name = dval.getInfo().getName();
                Object value = dval.getValue();
                if ( name != null && name.trim().length() > 0 &&
                     value != null ) {
                    name = name.trim()
                          .replaceAll( ":", "_" )
                          .replaceAll( "\\s+", " " );
                    String sval = dval.getValueAsString( 320 )
                                 .trim().replaceAll( "\\s+", " " );
                    printLine( out, name + ": " + sval );
                }
            }
            printLine( out, "" );
        }

        /* Get column information. */
        int ncol = st.getColumnCount();
        ColumnInfo[] colInfos = new ColumnInfo[ ncol ];
        String[] colNames = new String[ ncol ];
        int raIndex = -1;
        int decIndex = -1;
        int idIndex = -1;
        int xIndex = -1;
        int yIndex = -1;
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info = st.getColumnInfo( icol );
            colInfos[ icol ] = info;
            colNames[ icol ] = info.getName().trim().replaceAll( "\\s+", "_" );
            if ( matches( info, ID_INFO ) ) {
                idIndex = icol;
            }
            if ( matches( info, RA_INFO ) ) {
                raIndex = icol;
            }
            if ( matches( info, DEC_INFO ) ) {
                decIndex = icol;
            }
            if ( matches( info, X_INFO ) ) {
                xIndex = icol;
            }
            if ( matches( info, Y_INFO ) ) {
                yIndex = icol;
            }
        }

        /* Write column identifier lines. */
        printLine( out, "# Attempted guesses about identity of columns " 
                      + "in the table." );
        printLine( out, "# These have been inferred from column UCDs "
                      + "and/or names" );
        printLine( out, "# in the original table data." );
        printLine( out, "# The algorithm which identifies these columns "
                      + "is not particularly reliable," );
        printLine( out, "# so it is possible that these are incorrect." );
        printLine( out, "id_col: " + idIndex );
        printLine( out, "ra_col: " + raIndex );
        printLine( out, "dec_col: " + decIndex );
        if ( xIndex >= 0 ) {
            printLine( out, "x_col: " + xIndex );
        }
        if ( yIndex >= 0 ) {
            printLine( out, "y_col: " + yIndex );
        }
        printLine( out, "" );

        /* Write advert. */
        printLine( out, "# This TST file generated by STIL v" +
                        IOUtils.getResourceContents( StarTable.class,
                                                     "stil.version", null ) );
        printLine( out, "" );

        /* Write the column headings. */
        printRow( out, colNames );

        /* Write the heading/data separator. */
        String[] separators = new String[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            separators[ icol ] = colNames[ icol ].replaceAll( ".", "-" );
        }
        printRow( out, separators );

        /* Write the data. */
        RowSequence rseq = st.getRowSequence();
        try {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                String[] srow = new String[ ncol ];
                for ( int icol = 0; icol < ncol; icol++ ) {
                    String sval = colInfos[ icol ]
                                 .formatValue( row[ icol ], MAX_CHARS );
                    srow[ icol ] = escapeText( sval );
                }
                printRow( out, srow );
            }
            printLine( out, "[EOD]" );
        }
        finally {
            rseq.close();
        }
    }

    /**
     * Writes a line of text to an output stream.
     *
     * @param  out  destination stream
     * @param  line  text to write
     */
    private static void printLine( OutputStream out, String line )
            throws IOException {
        out.write( getBytes( line ) );
        out.write( '\n' );
    }

    /**
     * Writes a tab-separated row of strings to an output stream.
     *
     * @param  out  destination stream
     * @param  row  array of strings to write
     */
    private static void printRow( OutputStream out, String[] row )
            throws IOException {
        for ( int icol = 0; icol < row.length; icol++ ) {
            if ( icol > 0 ) {
                out.write( '\t' );
            }
            out.write( getBytes( row[ icol ] ) );
        }
        out.write( '\n' );
    }

    /**
     * Returns a byte array representing the characters in a string.
     * May play fast and loose with Unicode.
     *
     * @param   str  input string
     * @param   byte array giving characters in <code>str</code>
     */
    private static byte[] getBytes( String str ) {

        /* The decoding here is not that respectable (doesn't properly
         * handle Unicode), but it makes a big performance difference,
         * e.g. when writing out a table.
         * Leave it unless we find ourselves using much in the way of
         * unicode characters.
         * The correct way would be do use str.decode(). */
        int leng = str.length();
        byte[] buf = new byte[ leng ];
        for ( int i = 0; i < leng; i++ ) {
            buf[ i ] = (byte) str.charAt( i );
        }
        return buf;
    }

    /**
     * Prepares a table for output to TST format.
     * The main difficult job is trying to identify which columns (if any)
     * represent RA, DEC and ID.  The output table will resemble the 
     * input one, but the {@link #matches} method may return true
     * for matches with the constants RA_INFO, DEC_INFO, ID_INFO.
     * Numeric RA/DEC columns which appear to have units of radians are
     * converted to degrees (the TST format requires degrees or 
     * sexagesimal for sky position angles).
     *
     * @param  in  input table
     * @return  doctored table
     */
    private static StarTable prepareTable( StarTable in ) {
        int ncol = in.getColumnCount();

        /* Look through each column trying to find ID, RA and DEC columns. */
        int raIndex = -1;
        int decIndex = -1;
        int idIndex = -1;
        int xIndex = -1;
        int yIndex = -1;
        double raFactor = 1.0;
        double decFactor = 1.0;
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info = in.getColumnInfo( icol );
            String name = info.getName();
            String ucd = info.getUCD();
            String units = info.getUnitString();
            Class<?> clazz = info.getContentClass();
            String lucd = ucd == null ? "" : ucd.toLowerCase();
            String lname = name == null ? "" : name.toLowerCase();
            String lunits = units == null ? "" : units.toLowerCase();
            if ( raIndex < 0 ) {
                if ( lucd.startsWith( "pos.eq.ra" ) ||
                     lname.matches( "ra_?(2000)?" ) ||
                     lname.matches( "right.asc.*" ) ) {
                    raIndex = icol;
                    if ( Number.class.isAssignableFrom( clazz ) &&
                         lunits.startsWith( "rad" ) ) {
                        raFactor = 180.0 / Math.PI;
                    }
                }
            }
            if ( decIndex < 0 ) {
                if ( lucd.startsWith( "pos.eq.dec" ) ||
                     lname.matches( "dec?_?(2000)?" ) ||
                     lname.matches( "decl?_?(2000)?" ) ||
                     lname.startsWith( "declination" ) ) {
                    decIndex = icol;
                    if ( Number.class.isAssignableFrom( clazz ) &&
                         lunits.startsWith( "rad" ) ) {
                        decFactor = 180.0 / Math.PI;
                    }
                }
            }
            if ( idIndex < 0 ) {
                if ( lucd.startsWith( "meta.id" ) ||
                     lucd.startsWith( "id_" ) ||
                     lname.equals( "id" ) ||
                     lname.startsWith( "ident" ) ||
                     lname.equals( "name" ) ) {
                    idIndex = icol;
                }
            }
            if ( xIndex < 0 ) {
                if ( lucd.startsWith( "pos.cartesian.x" ) ||
                     lname.equals( "x" ) ||
                     lname.equals( "xpos" ) ) {
                    xIndex = icol;
                }
            }
            if ( yIndex < 0 ) {
                if ( lucd.startsWith( "pos.cartesian.y" ) ||
                     lname.equals( "y" ) ||
                     lname.equals( "ypos" ) ) {
                    yIndex = icol;
                }
            }
        }

        /* Prepare an output table which is just like the input one but has
         * radians -> degrees conversions as appropriate. */
        double[] factors = new double[ ncol ];
        Arrays.fill( factors, 1.0 );
        if ( raIndex >= 0 ) {
            factors[ raIndex ] = raFactor;
        }
        if ( decIndex >= 0 ) {
            factors[ decIndex ] = decFactor;
        }
        StarTable out = new FactorStarTable( in, factors );

        /* Ensure that we have an ID column, by faking one if necessary.
         * Although SSN/75 appears to say that an ID column is optional
         * and can be indicated with an "id_col: -1" parameter,
         * GAIA complains when loading such a table that -1 is out of
         * range and refuses to plot the data.
         * I'm not currently sure if this is a GAIA bug or not. */
        if ( idIndex < 0 ) {
            ColumnInfo indexInfo =
                new ColumnInfo( "Index", Long.class,
                                "Row index within table" );
            ColumnData indexCol = new ColumnData( indexInfo ) {
                public Object readValue( long irow ) {
                    return Long.valueOf( irow + 1 );
                }
            };

            /* The index column must report a determinate row count, since
             * it's a random table.  If it's not know, use a maximum value;
             * the joined table will use the smallest row count of all its
             * constituent tables. */
            long ixCount = out.getRowCount();
            if ( ixCount < 0 ) {
                ixCount = Long.MAX_VALUE;
            }
            ColumnStarTable indexTable = 
                ColumnStarTable.makeTableWithRows( ixCount );
            indexTable.addColumn( indexCol );
            idIndex = out.getColumnCount();
            AbstractStarTable joined = 
                new JoinStarTable( new StarTable[] { out, indexTable } );
            joined.setName( out.getName() );
            joined.setURL( out.getURL() );
            joined.setParameters( out.getParameters() );
            out = joined;
            assert out.getColumnCount() == idIndex + 1;
        }

        /* Doctor the column metadata in such a way that the matches() 
         * method will be able to identify ID, RA and DEC columns. */
        String labelKey = LABEL_INFO.getName();
        if ( idIndex >= 0 ) {
            out.getColumnInfo( idIndex )
               .setAuxDatum( ID_INFO.getAuxDatumByName( labelKey ) );
        }
        if ( raIndex >= 0 ) {
            out.getColumnInfo( raIndex )
               .setAuxDatum( RA_INFO.getAuxDatumByName( labelKey ) );
        }
        if ( decIndex >= 0 ) {
            out.getColumnInfo( decIndex )
               .setAuxDatum( DEC_INFO.getAuxDatumByName( labelKey ) );
        }
        if ( xIndex >= 0 ) {
            out.getColumnInfo( xIndex )
               .setAuxDatum( X_INFO.getAuxDatumByName( labelKey ) );
        }
        if ( yIndex >= 0 ) {
            out.getColumnInfo( yIndex )
               .setAuxDatum( Y_INFO.getAuxDatumByName( labelKey ) );
        }
        return out;
    }

    /**
     * Indicates whether two ColumnInfo objects match (in some sense).
     *
     * @param  info1  first info
     * @param  info2  second info
     * @return  true iff they match
     */
    private static boolean matches( ColumnInfo info1, ColumnInfo info2 ) {
        String labelKey = LABEL_INFO.getName();
        String label1 = info1.getAuxDatumValueByName( labelKey, String.class );
        String label2 = info2.getAuxDatumValueByName( labelKey, String.class );
        return label1 != null && label2 != null && label1.equals( label2 );
    }

    /**
     * Escapes text so that it's suitable for writing to a TST file.
     * This most importantly consists of removing tab characters.
     *
     * @param   in  input text
     * @return  harmless version of <code>in</code>
     */
    private static String escapeText( String in ) {
        int leng = in.length();
        boolean ok = true;
        for ( int ic = 0; ic < leng; ic++ ) {
            switch ( in.charAt( ic ) ) {
                case '\n':
                case '\r':
                case '\t':
                    ok = false;
            }
        }
        if ( ok ) {
            return in;
        }
        else {
            StringBuffer sbuf = new StringBuffer( in );
            for ( int ic = 0; ic < leng; ic++ ) {
                switch ( in.charAt( ic ) ) {
                    case '\n':
                    case '\r':
                    case '\t':
                        sbuf.setCharAt( ic, ' ' );
                }
            }
            return sbuf.toString();
        }
    }

    /**
     * Wrapper table which applies an optional multiplication factor to
     * numeric values in some columns.  Multiplied columns have 
     * output type Double.
     */
    private static class FactorStarTable extends WrapperStarTable {
        private final double[] factors_;
        private final ColumnInfo[] colInfos_;

        /**
         * Constructor.
         *
         * @param  base  base table
         * @param  array of per-column multiplication factors - only factors
         *         not equal to 1.0 affect the output
         */
        FactorStarTable( StarTable base, double[] factors ) {
            super( base );
            factors_ = factors;
            int ncol = base.getColumnCount();
            colInfos_ = new ColumnInfo[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                ColumnInfo info = new ColumnInfo( base.getColumnInfo( icol ) );
                if ( factors[ icol ] != 1.0 ) {
                    info.setContentClass( Double.class );
                }
                colInfos_[ icol ] = info;
            }
        }

        public Object getCell( long lrow, int icol ) throws IOException {
            Object val = super.getCell( lrow, icol );
            return ( factors_[ icol ] == 1.0 || val == null )
                 ? val
                 : Double.valueOf( ((Number) val).doubleValue()
                                 * factors_[ icol ] );
        }

        public Object[] getRow( long lrow ) throws IOException {
            Object[] row = super.getRow( lrow );
            int ncol = row.length;
            for ( int icol = 0; icol < ncol; icol++ ) {
                Object val = row[ icol ];
                double factor = factors_[ icol ];
                if ( factor != 1.0 && val != null ) {
                    row[ icol ] = Double.valueOf( ((Number) val).doubleValue()
                                                * factor );
                }
            }
            return row;
        }

        public RowSequence getRowSequence() throws IOException {
            RowSequence baseSeq = super.getRowSequence();
            FactorRow frow = new FactorRow( baseSeq, factors_ );
            return new WrapperRowSequence( baseSeq ) {
                public Object getCell( int icol ) throws IOException {
                    return frow.getCell( icol );
                }
                public Object[] getRow() throws IOException {
                    return frow.getRow();
                }
            };
        }

        public RowAccess getRowAccess() throws IOException {
            RowAccess baseAcc = super.getRowAccess();
            FactorRow frow = new FactorRow( baseAcc, factors_ );
            return new WrapperRowAccess( baseAcc ) {
                public Object getCell( int icol ) throws IOException {
                    return frow.getCell( icol );
                }
                public Object[] getRow() throws IOException {
                    return frow.getRow();
                }
            };
        }
    }

    private static class FactorRow implements RowData {
        private final RowData baseRow_;
        private final double[] factors_;
        FactorRow( RowData baseRow, double[] factors ) {
            baseRow_ = baseRow;
            factors_ = factors;
        }
        public Object getCell( int icol ) throws IOException {
            Object val = baseRow_.getCell( icol );
            return ( factors_[ icol ] == 1.0 || val == null )
                ? val
                : Double.valueOf( ((Number) val).doubleValue()
                                * factors_[ icol ] );
        }
        public Object[] getRow() throws IOException {
            Object[] row = baseRow_.getRow();
            int ncol = row.length;
            for ( int icol = 0; icol < ncol; icol++ ) {
                Object val = row[ icol ];
                double factor = factors_[ icol ];
                if ( factor != 1.0 && val != null ) {
                    row[ icol ] =
                        Double.valueOf( ((Number) val).doubleValue() * factor );
                }
            }
            return row;
        }
    }
}
