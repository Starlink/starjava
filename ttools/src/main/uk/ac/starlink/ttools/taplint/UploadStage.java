package uk.ac.starlink.ttools.taplint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
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
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapCapability;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableBuilder;
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

    /**
     * Constructor.
     *
     * @param   tapRunner   runs TAP queries
     * @param   capHolder   
     */
    public UploadStage( TapRunner tapRunner, CapabilityHolder capHolder ) {
        tapRunner_ = tapRunner;
        capHolder_ = capHolder;
    }

    public String getDescription() {
        return "Make queries with table uploads";
    }

    public void run( Reporter reporter, URL serviceUrl ) {
        TapCapability tcap = capHolder_.getCapability();
        if ( tcap != null && ( tcap.getUploadMethods() == null ||
                               tcap.getUploadMethods().length == 0 ) ) {
            reporter.report( ReportType.FAILURE, "NOUP",
                             "Table capabilities lists no upload methods - "
                           + "will not attempt upload tests" );
            return;
        }
        new UploadRunner( reporter, serviceUrl, tcap, tapRunner_ ).run();
    }

    /**
     * Does the work for the upload testing.
     */
    private static class UploadRunner implements Runnable {
        private final Reporter reporter_;
        private final URL serviceUrl_;
        private final TapCapability tcap_;
        private final TapRunner tRunner_;

        /**
         * Constructor.
         *
         * @param  reporter   validation message destination
         * @param  serviceUrl  TAP base URL
         * @param  tcap  TAP capability information object
         * @param  tapRunner   runs TAP queries
         */
        UploadRunner( Reporter reporter, URL serviceUrl, TapCapability tcap,
                      TapRunner tapRunner ) {
            reporter_ = reporter;
            serviceUrl_ = serviceUrl;
            tcap_ = tcap;
            tRunner_ = tapRunner;
        }

        /**
         * Runs the test.
         */
        public void run() {
            String upName = "t1";
            StarTable upTable = createUploadTable( 23 );
            String adql = "SELECT * FROM TAP_UPLOAD." + upName;
            Map<String,StarTable> upMap = new LinkedHashMap<String,StarTable>();
            upMap.put( upName, upTable );
            TapQuery tq;
            try {
                tq = new TapQuery( serviceUrl_, adql, null, upMap, -1 );
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "UPER",
                                  "Upload error failed", e );
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
                reporter_.report( ReportType.FAILURE, "TRND",
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
                reporter_.report( ReportType.ERROR, "TMNR", msg );
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
                reporter_.report( ReportType.ERROR, "TMNC", msg );
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
                        reporter_.report( ReportType.ERROR, "TMCN", msg );
                    }
                    compareStringAuxMetadata( c1, c2,
                                              VOStarTable.DATATYPE_INFO );
                    compareStringAuxMetadata( c1, c2, VOStarTable.XTYPE_INFO );
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
                        Object v1 = render( t1.getCell( ir, ic ) );
                        Object v2 = render( t2.getCell( ir, ic ) );
                        if ( ! v1.equals( v2 ) ) {
                            String msg = new StringBuilder()
                               .append( "Upload result value mismatch" )
                               .append( " for column " )
                               .append( t1.getColumnInfo( ic ) )
                               .append( " in row " )
                               .append( ir ) 
                               .append( ": " )
                               .append( v2 )
                               .append( " != " )
                               .append( v1 )
                               .toString();
                            reporter_.report( ReportType.ERROR, "TMCD", msg );
                        }
                    }
                }
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.FAILURE, "DTIO",
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
                String code = "TM" + metaInfo.getName().substring( 0, 2 )
                                                       .toUpperCase();
                reporter_.report( ReportType.WARNING, code, msg );
            }
        }
    }

    /**
     * Turns the contents of a cell into a string.
     *
     * @param   cell   cell data
     * @return  strinigified value
     */
    private static String render( Object cell ) {
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
            floatData[ ir ] = ir;
            doubleData[ ir ] = ir;
            charData[ ir ] = (char) ( 'A' + ir );
            stringData[ ir ] = Integer.toString( ir );
            timeData[ ir ] = Times.mjdToIso( ir );
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
        ctable.addColumn( makeColumn( "d_time", timeData, "adql:TIMESTAMP" ) );

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
            VOTableWriter vow = new VOTableWriter( DataFormat.BINARY, true );
            vow.setVotableVersion( "1.2" );
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
            cinfo.setAuxDatum( new DescribedValue( VOStarTable.XTYPE_INFO,
                                                   xtype ) );
        }
        Class clazz = cinfo.getContentClass();
        final boolean nullable;
        if ( clazz == String.class ) {
            nullable = false;
            String[] sdata = (String[]) data;
            int size = 0;
            for ( int i = 0; i < sdata.length; i++ ) {
                size = Math.max( size, sdata[ i ].length() );
            }
            cinfo.setElementSize( size );
        }
        else if ( clazz.isArray() ) {
            nullable = false;
            int size = 0;
            for ( int i = 0; i < Array.getLength( data ); i++ ) {
                Object item = Array.get( data, i );
                size = Math.max( size, Array.getLength( item ) );
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
