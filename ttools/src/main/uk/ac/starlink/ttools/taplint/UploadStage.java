package uk.ac.starlink.ttools.taplint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.func.Times;
import uk.ac.starlink.vo.TapService;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapCapability;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.votable.VOTableVersion;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * TapLint stage which attempts table uploads.
 *
 * @author   Mark Taylor
 * @since    27 Jun 2011
 */
public class UploadStage implements Stage {

    private final TapRunner tapRunner_;
    private final CapabilityHolder capHolder_;

    // DALI 1.1 is a bit equivocal about a trailing 'Z' here.
    private static final Pattern DALI_ISO_REGEX =
        Pattern.compile( "[0-9]{4}-[01][0-9]-[0-3][0-9]"
                       + "(T[0-2][0-9]:[0-5][0-9]:[0-6][0-9](\\.[0-9]+)?)?" );

    /**
     * Constructor.
     *
     * @param   tapRunner   runs TAP queries
     * @param   capHolder     contains capability information
     */
    public UploadStage( TapRunner tapRunner, CapabilityHolder capHolder ) {
        tapRunner_ = tapRunner;
        capHolder_ = capHolder;
    }

    public String getDescription() {
        return "Make queries with table uploads";
    }

    public void run( Reporter reporter, TapService tapService ) {
        TapCapability tcap = capHolder_.getCapability();
        if ( tcap != null && ( tcap.getUploadMethods() == null ||
                               tcap.getUploadMethods().length == 0 ) ) {
            reporter.report( FixedCode.F_NOUP,
                             "Table capabilities lists no upload methods - "
                           + "will not attempt upload tests" );
            return;
        }
        new UploadRunner( reporter, tapService, tcap, tapRunner_ ).run();
    }

    /**
     * Does the work for the upload testing.
     */
    private static class UploadRunner implements Runnable {
        private final Reporter reporter_;
        private final TapService tapService_;
        private final TapCapability tcap_;
        private final TapRunner tRunner_;

        /**
         * Constructor.
         *
         * @param  reporter   validation message destination
         * @param  tapService  TAP service description
         * @param  tcap  TAP capability information object
         * @param  tapRunner   runs TAP queries
         */
        UploadRunner( Reporter reporter, TapService tapService,
                      TapCapability tcap, TapRunner tapRunner ) {
            reporter_ = reporter;
            tapService_ = tapService;
            tcap_ = tcap;
            tRunner_ = tapRunner;
        }

        /**
         * Runs the test.
         */
        public void run() {
            VOTableWriter[] vowriters = new VOTableWriter[] {
                new VOTableWriter( DataFormat.TABLEDATA, true,
                                   VOTableVersion.V12 ),
                new VOTableWriter( DataFormat.BINARY, true,
                                   VOTableVersion.V12 ),
            };
            StarTable upTable = createUploadTable( 23 );
            for ( int i = 0; i < vowriters.length; i++ ) {
                runUploadQuery( upTable, vowriters[ i ] );
            }
        }

        /**
         * Performs one upload query.
         *
         * @param  upTable  table to upload
         * @param  vowriter  VOTable serializer for uploaded table
         */
        private void runUploadQuery( StarTable upTable,
                                     VOTableWriter vowriter ) {
            String upName = "t1";
            String adql = "SELECT * FROM TAP_UPLOAD." + upName;
            Map<String,StarTable> upMap = new LinkedHashMap<String,StarTable>();
            upMap.put( upName, upTable );
            TapQuery tq;
            try {
                tq = new TapQuery( tapService_, adql, null, upMap,
                                   -1, vowriter );
            }
            catch ( IOException e ) {
                String msg = new StringBuffer()
                    .append( "Upload failed" )
                    .append( " using VOTable serializer " )
                    .append( vowriter )
                    .toString();
                reporter_.report( FixedCode.E_UPER, msg, e );
                return;
            }
            StarTable resultTable = tRunner_.getResultTable( reporter_, tq );
            if ( resultTable == null ) {
                return;
            }
            try {
                upTable = Tables.randomTable( upTable );
                resultTable = Tables.randomTable( resultTable );
            }
            catch ( IOException e ) {
                reporter_.report( FixedCode.F_TRND,
                                  "Unexpected error randomising tables", e );
            }
            compareTables( upTable, resultTable );
        }

        /**
         * Takes two tables and checks they have the same metadata and data.
         * Discrepancies are reported.
         *
         * @param  t1  reference table
         * @param  t2  table to assess
         */
        private void compareTables( StarTable t1, StarTable t2 ) {

            /* Check row counts match. */
            boolean ok = true;
            long nrow = t1.getRowCount();
            if ( nrow != t2.getRowCount() ) {
                ok = false;
                String msg = new StringBuilder()
                   .append( "Upload result row count wrong, " )
                   .append( t2.getRowCount() )
                   .append( " != " )
                   .append( t1.getRowCount() )
                   .toString();
                reporter_.report( FixedCode.E_TMNR, msg );
            }

            /* Check column counts match. */
            int ncol = t1.getColumnCount();
            if ( ncol != t2.getColumnCount() ) {
                ok = false;
                String msg = new StringBuilder()
                   .append( "Upload result column count wrong, " )
                   .append( t2.getColumnCount() )
                   .append( " != " )
                   .append( t1.getColumnCount() )
                   .toString();
                reporter_.report( FixedCode.E_TMNC, msg );
            }
            else {

                /* Check column metadata matches. */
                for ( int ic = 0; ic < ncol; ic++ ) {
                    ColumnInfo c1 = t1.getColumnInfo( ic );
                    ColumnInfo c2 = t2.getColumnInfo( ic );
                    String name1 = c1.getName();
                    String name2 = c2.getName();
                    if ( ! name1.equalsIgnoreCase( name2 ) ) {
                        String msg = new StringBuffer()
                           .append( "Upload result column name mismatch " )
                           .append( name2 )
                           .append( " != " )
                           .append( name1 )
                           .toString();
                        reporter_.report( FixedCode.E_TMCN, msg );
                    }
                    compareStringAuxMetadata( c1, c2,
                                              VOStarTable.DATATYPE_INFO );
                    String xtype1 = normaliseXtype( c1.getXtype() );
                    String xtype2 = normaliseXtype( c2.getXtype() );
                    if ( ( xtype1 == null && xtype2 != null ) ||
                         ( xtype1 != null && ! xtype1.equals( xtype2 ) ) ) {
                        String msg = new StringBuffer()
                            .append( "Upload result column xtype mismatch " )
                            .append( c1.getXtype() )
                            .append( " != " )
                            .append( c2.getXtype() )
                            .toString();
                        reporter_.report( FixedCode.E_TMCX, msg );
                    }
                }
            }

            /* Bail out if column and row counts are not equal. */
            if ( ! ok ) {
                return;
            }

            /* Check data is equal. */
            try {
                for ( int ir = 0; ir < nrow; ir++ ) {
                    for ( int ic = 0; ic < ncol; ic++ ) {
                        Object o1 = t1.getCell( ir, ic );
                        Object o2 = t2.getCell( ir, ic );
                        String s1 = renderCell( o1 );
                        String s2 = renderCell( o2 );
                        final boolean reportMismatch;

                        /* Treat non-null time columns specially. */
                        boolean isTimeCol =
                            "timestamp"
                           .equals( t2.getColumnInfo( ic ).getXtype() );
                        if ( isTimeCol && s1.length() > 0 && s2.length() > 0 ) {

                            /* See TAP 1.0 sec 2.3.4/2.5, TAP 1.1 sec 2.7.2;
                             * also DALI 1.0 sec 3.1.2, DALI 1.1 sec 3.3.3.
                             * The main error this is likely to catch is using
                             * a ' ' instead of 'T' to separate time and date.
                             * DALI 1.1 explicitly says that output must be
                             * in this format for timestamp columns;
                             * TAP 1.0/DALI 1.0 does not, but it's pretty much
                             * implied by the fact that this format has to work
                             * for queries.
                             * In any case, we can't assess value equality if
                             * we don't have the returned values in a defined
                             * format. */
                            if ( ! DALI_ISO_REGEX.matcher( s2 ).matches() ) {
                                String msg = new StringBuffer()
                                    .append( "Incorrect time syntax: \"" )
                                    .append( s2 )
                                    .append( "\" does not match " )
                                    .append( "yyyy-MM-dd['T'HH:mm:ss[.SSS]]" )
                                    .toString();
                                reporter_.report( FixedCode.W_TSDL, msg );
                                reportMismatch = false;
                            }

                            /* Format is OK, so we can check value equality. */
                            else {
                                double d1 = Times.isoToMjd( s1 );
                                double d2 = Times.isoToMjd( s2 );
                                reportMismatch = d1 != d2;
                            }
                        }

                        /* Other columns, just check display equality. */
                        else {
                            reportMismatch = ! s1.equals( s2 );
                        }
                        if ( reportMismatch ) {
                            String msg = new StringBuilder()
                               .append( "Upload result value mismatch" )
                               .append( " for column " )
                               .append( t1.getColumnInfo( ic ) )
                               .append( " in row " )
                               .append( ir ) 
                               .append( ": " )
                               .append( s2 )
                               .append( " != " )
                               .append( s1 )
                               .toString();
                            reporter_.report( FixedCode.E_TMCD, msg );
                        }
                    }
                }
            }
            catch ( IOException e ) {
                reporter_.report( FixedCode.F_DTIO,
                                  "Unexpected IO error reading table", e );
            }
        }

        /**
         * Compares column auxiliary metadata items for two columns;
         * if they are different, a warning is issued.
         *
         * @param  c1  upload column
         * @param  c2  result column
         * @param  metaInfo  the metadata item to be compared
         */
        private void compareStringAuxMetadata( ColumnInfo c1, ColumnInfo c2,
                                               ValueInfo metaInfo ) {
            DescribedValue dv1 = c1.getAuxDatum( metaInfo );
            DescribedValue dv2 = c2.getAuxDatum( metaInfo );
            String s1 = dv1 == null ? null : (String) dv1.getValue();
            String s2 = dv2 == null ? null : (String) dv2.getValue();
            if ( ( s1 == null && s2 != null ) ||
                 ( s1 != null && ! s1.equals( s2 ) ) ) {
                String msg = new StringBuilder()
                   .append( "Upload result column " )
                   .append( metaInfo.getName() )
                   .append( " mismatch (" )
                   .append( s1 )
                   .append( " != " )
                   .append( s2 )
                   .append( ")" )
                   .append( " for column " )
                   .append( c1.getName() )
                   .toString();
                ReportCode code =
                    new AdhocCode( ReportType.WARNING,
                                   "TM" + metaInfo.getName().substring( 0, 2 )
                                                            .toUpperCase() );
                reporter_.report( code, msg );
            }
        }
    }

    /**
     * Converts Xtype values to canonical form.  In some cases different
     * Xtypes are considered (by the grace of DALI) to have equivalent
     * meanings, so convert here to the most DALI-compliant form in such cases.
     *  
     * @param  input xtype, may be null
     * @return  canonical xtype, may be null
     */
    private static String normaliseXtype( String xtype ) {
        if ( xtype == null ) {
            return null;
        }

        /* Normalise the old TAP1.1 "adql:TIMESTAMP" to DALI "timestamp";
         * see email from Pat Dowler on DAL list 16 Apr 2021. */
        else if ( "adql:TIMESTAMP".equals( xtype ) ) {
            return "timestamp";
        }
        else {
            return xtype;
        }
    }

    /**
     * Turns the contents of a cell into a string.
     *
     * @param   cell   cell data
     * @return  strinigified value
     */
    private static String renderCell( Object cell ) {
        if ( Tables.isBlank( cell ) ) {
            return "";
        }
        else if ( cell.getClass().isArray() ) {
            StringBuilder sbuf = new StringBuilder();
            for ( int i = 0; i < Array.getLength( cell ); i++ ) {
                sbuf.append( i == 0 ? '[' : ',' )
                    .append( Array.get( cell, i ) );
            }
            sbuf.append( ']' );
            return sbuf.toString();
        }
        else {
            return cell.toString();
        }
    }

    /**
     * Returns a test table suitable for uploading to a TAP server.
     * It contains a variety of data types.
     *
     * @param  nrow   row count
     * @return   new table
     */
    private static final StarTable createUploadTable( int nrow ) {
        int arraySize = 4;

        /* Set up data arrays per column. */
        short[] shortData = new short[ nrow ];
        int[] intData = new int[ nrow ];
        long[] longData = new long[ nrow ];
        float[] floatData = new float[ nrow ];
        double[] doubleData = new double[ nrow ];
        char[] charData = new char[ nrow ];
        String[] stringData = new String[ nrow ];
        String[] timeData = new String[ nrow ];

        /* Initialise values for each element of each data array. */
        /* I've avoided byte types here because STIL is not comfortable
         * with them; it tends to convert them to shorts when writing
         * VOTables.  That is probably a deficiency in STIL. */
        for ( int ir = 0; ir < nrow; ir++ ) {
            shortData[ ir ] = (short) ir;
            intData[ ir ] = ir;
            longData[ ir ] = ir;
            floatData[ ir ] = 1.125f * ir;
            doubleData[ ir ] = 1.125 * ir;
            charData[ ir ] = (char) ( 'A' + ir );
            stringData[ ir ] = Integer.toString( ir );
            timeData[ ir ] = Times.mjdToIso( 51544 + 1.01 * ir );
        }

        /* Construct a table based on the data arrays. */
        ColumnStarTable ctable = ColumnStarTable.makeTableWithRows( nrow );
        ctable.addColumn( makeColumn( "d_short", shortData, null ) );
        ctable.addColumn( makeColumn( "d_int", intData, null ) );
        ctable.addColumn( makeColumn( "d_long", longData, null ) );
        ctable.addColumn( makeColumn( "d_float", floatData, null ) );
        ctable.addColumn( makeColumn( "d_double", doubleData, null ) );
        ctable.addColumn( makeColumn( "d_char", charData, null ) );
        ctable.addColumn( makeColumn( "d_string", stringData, null ) );
        ctable.addColumn( makeColumn( "d_time", timeData, "timestamp" ) );

        /* Populate the final row with blank values, where appropriate. */
        int irBlank = nrow - 1;
        for ( int ic = 0; ic < ctable.getColumnCount(); ic++ ) {
            ColumnData dcol = ctable.getColumnData( ic );
            if ( dcol.getColumnInfo().isNullable() ) {
                ((ArrayColumn) dcol).storeValue( irBlank, null );
            }
        }

        /* Write and re-read the table.  The purpose of this is so that
         * the columns have VOTable-specific aux metadata, for comparison
         * with that from the query response VOTable. */
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            VOTableWriter vow = new VOTableWriter( DataFormat.BINARY, true,
                                                   VOTableVersion.V12 );
            vow.writeStarTable( ctable, bout );
            bout.close();
            byte[] bbuf = bout.toByteArray();
            StarTableFactory stfact = new StarTableFactory();
            stfact.setStoragePolicy( StoragePolicy.PREFER_MEMORY );
            return stfact
                  .makeStarTable( new ByteArrayInputStream( bbuf ),
                                  new VOTableBuilder( true ) );
        }
        catch ( IOException e ) {
            throw (AssertionError)
                  new AssertionError( "Unexpected error re-reading VOTable" )
                 .initCause( e );
        }
    }

    /**
     * Constructs a table column for a given data array.
     *
     * @param   name  column name
     * @param   data  data array, one element for each row
     * @param   xtype  VOTable xtype attributed for the column
     * @return  column
     */
    private static ColumnData makeColumn( String name, Object data,
                                          String xtype ) {
        ColumnData col = ArrayColumn.makeColumn( name, data );
        ColumnInfo cinfo = col.getColumnInfo();
        if ( xtype != null ) {
            cinfo.setXtype( xtype );
        }
        Class<?> clazz = cinfo.getContentClass();
        final boolean nullable;
        if ( clazz == String.class ) {
            nullable = false;
            String[] sdata = (String[]) data;
            int size = sdata[ 0 ].length();
            for ( int i = 0; i < sdata.length; i++ ) {
                if ( sdata[ i ].length() != size ) {
                    size = -1;
                }
            }
            if ( size >= 0 ) {
                cinfo.setElementSize( size );
            }
        }
        else if ( clazz.isArray() ) {
            nullable = false;
            int size = Array.getLength( Array.get( data, 0 ) );
            for ( int i = 0; i < Array.getLength( data ); i++ ) {
                Object item = Array.get( data, i );
                if ( Array.getLength( Array.get( data, i ) ) != size ) {
                    size = -1;
                }
            }
            cinfo.setShape( new int[] { size } );
        }
        else if ( clazz == Character.class ) {
            nullable = false;
        }
        else {
            nullable = true;
        }
        cinfo.setNullable( nullable );
        return col;
    }
}
