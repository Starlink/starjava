package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.UwsJob;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableVersion;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * UploadMatcher implementation for a TAP service.
 *
 * <p>Note that although the lon/lat coordinates are phrased in the API
 * as RA/Dec, to match the language of the {@link ConeQueryRowSequence}
 * interface, in fact any lon/lat coordinate pairs can be used,
 * of course providing that the same coordinates are understood in the
 * (uploaded) query sequence and the remote TAP table.
 *
 * @author   Mark Taylor
 * @since    4 Oct 2014
 */
public class TapUploadMatcher implements UploadMatcher {

    private final URL serviceUrl_;
    private final String tableName_;
    private final String raExpr_;
    private final String decExpr_;
    private final String radiusDegExpr_;
    private final boolean isSync_;
    private final ServiceFindMode serviceMode_;
    private final int pollMillis_ = 10000;

    private static final String TABLE_ID = "up";
    private static final String ID_NAME = "TAPUPLOAD_ID";
    private static final String RA_NAME = "LON";
    private static final String DEC_NAME = "LAT";

    /**
     * Constructor.
     *
     * @param  serviceUrl  TAP service base URL
     * @param  tableName   name of table in TAP service to match against
     * @param  raExpr    column name (or ADQL expression) for RA
     *                   in decimal degrees in TAP table
     * @param  decExpr   column name (or ADQL expression) for Declination
     *                   in decimal degrees in TAP table
     * @param  radiusDegExpr  ADQL expression (maybe constant) for search
     *                        radius in decimal degrees
     * @param  isSync     true for synchronous, false for asynchronous
     * @param  serviceMode  type of match
     */
    public TapUploadMatcher( URL serviceUrl, String tableName,
                             String raExpr, String decExpr,
                             String radiusDegExpr, boolean isSync,
                             ServiceFindMode serviceMode ) {
        serviceUrl_ = serviceUrl;
        tableName_ = tableName;
        raExpr_ = raExpr;
        decExpr_ = decExpr;
        radiusDegExpr_ = radiusDegExpr;
        isSync_ = isSync;
        switch ( serviceMode ) {
            case ALL:
            case ALL_SCORE:
            case BEST_SCORE:
                serviceMode_ = serviceMode;
                break;  // supported
            default:
                throw new IllegalArgumentException( "Unsupported mode: "
                                                  + serviceMode );
        }
    }

    public boolean streamRawResult( ConeQueryRowSequence coneSeq,
                                    TableSink rawResultSink,
                                    RowMapper<?> rowMapper, long maxrec )
            throws IOException {
        String adql = getAdql( maxrec );
        Map<String,String> extraParams = new HashMap<String,String>();
        Map<String,StarTable> uploadMap = new HashMap<String,StarTable>();
        uploadMap.put( TABLE_ID,
                       new UploadConeTable( coneSeq, rowMapper,
                                            ID_NAME, RA_NAME, DEC_NAME ) );
        VOTableWriter voWriter =
            new VOTableWriter( DataFormat.BINARY, true, VOTableVersion.V12 );
        TapQuery tapQuery =
            new TapQuery( serviceUrl_, adql, extraParams, uploadMap, -1,
                          voWriter );
        final URLConnection conn;
        if ( isSync_ ) {
            conn = tapQuery.createSyncConnection();
        }
        else {

            /* You could imagine submitting multiple async jobs asychronously,
             * but let's not get into that for now. */
            UwsJob job = tapQuery.submitAsync();
            job.start();
            URL url;
            try {
                url = TapQuery.waitForResultUrl( job, pollMillis_ );
            }
            catch ( InterruptedException e ) {
                throw (IOException)
                      new IOException( "Interrupted" ).initCause( e );
            }
            conn = url.openConnection();
        }
        try {
            return TapQuery.streamResultVOTable( conn, rawResultSink );
        }
        catch ( SAXException e ) {
            throw (IOException)
                  new IOException( "Parse error from TAP service result: " + e )
                 .initCause( e );
        }
    }

    public ColumnPlan getColumnPlan( ColumnInfo[] resultCols,
                                     ColumnInfo[] uploadCols ) {
        return new TapColumnPlan( resultCols, uploadCols );
    }

    /**
     * Returns an ADQL expression to retrieve the raw result table from
     * the TAP service.
     *
     * @param  maxrec  maximum number of records permitted
     * @return  ADQL text
     */
    public String getAdql( long maxrec ) {
        String nl = "\n   ";
        String uploadName_ = "TAP_UPLOAD.up1";
        String sysarg = "'ICRS'";
        String uPosargs = new StringBuffer()
            .append( sysarg )
            .append( ", u." )
            .append( RA_NAME )
            .append( ", u." )
            .append( DEC_NAME )
            .toString();
        String rPosargs = new StringBuffer()
            .append( sysarg )
            .append( ", r." )
            .append( raExpr_ )
            .append( ", r." )
            .append( decExpr_ )
            .toString();
        boolean isBestScore = serviceMode_ == ServiceFindMode.BEST_SCORE;
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "SELECT" );
        if ( maxrec >= 0 ) {
            sbuf.append( nl )
                .append( "TOP " )
                .append( maxrec );
        }
        sbuf.append( nl );
        if ( ! serviceMode_.isScoreOnly() ) {
            sbuf.append( "r.*, " );
        }
        sbuf.append( "u." )
            .append( ID_NAME )
            .append( "," );
        sbuf.append( nl );
        if ( isBestScore ) {
            sbuf.append( "MIN(" );
        }
        sbuf.append( "DISTANCE(POINT(" )
            .append( uPosargs )
            .append( ")," )
            .append( nl );
        if ( isBestScore ) {
            sbuf.append( "    " );
        }
        sbuf.append( "         POINT(" )
            .append( rPosargs )
            .append( "))" );
        if ( isBestScore ) {
            sbuf.append( ")" );
        }
        sbuf.append( "*3600." )
            .append( " AS SEP_ARCSEC" )
            .append( nl )
            .append( "FROM " )
            .append( tableName_ )
            .append( " AS r" )
            .append( nl )
            .append( "JOIN " )
            .append( "TAP_UPLOAD." )
            .append( TABLE_ID )
            .append( " AS u" )
            .append( nl )
            .append( "ON 1=CONTAINS(POINT(" )
            .append( rPosargs )
            .append( ")," )
            .append( nl )
            .append( "              CIRCLE(" )
            .append( uPosargs )
            .append( ", " )
            .append( radiusDegExpr_ )
            .append( "))" );
        if ( isBestScore ) {
            sbuf.append( nl )
                .append( "GROUP BY u." )
                .append( ID_NAME );
        }
        return sbuf.toString();
    }

    /**
     * ColumnPlan implementation for this class.
     */
    private static class TapColumnPlan implements ColumnPlan {
        private final int ncUp_;
        private final int ncRem_;
        private final int ncOut_;
        private final int icId_;
        private final int icDist_;

        /**
         * Constructor.
         *
         * @param   resultCols  column metadata for the raw result table
         * @param   uploadCols  column metadata for the uploaded table
         */
        TapColumnPlan( ColumnInfo[] resultCols, ColumnInfo[] uploadCols ) {
            int ncRes = resultCols.length;
            ncUp_ = uploadCols.length;
            ncRem_ = ncRes - 2;
            ncOut_ = ncUp_ + ncRem_ + 1;
            icId_ = ncRem_;
            icDist_ = ncRem_ + 1;
        }

        public int getOutputColumnCount() {
            return ncOut_;
        }

        public int getOutputColumnLocation( int icolOutput ) {
            if ( icolOutput < 0 ) {
                throw new IllegalArgumentException( "Out of range" );
            }
            else if ( icolOutput < ncUp_ ) {
                int iUp = icolOutput;
                return - iUp - 1;
            }
            else if ( icolOutput < ncUp_ + ncRem_ ) {
                int iRes = icolOutput - ncUp_;
                return iRes;
            }
            else if ( icolOutput == ncUp_ + ncRem_ ) {
                int iRes = icDist_;
                return iRes;
            }
            else {
                throw new IllegalArgumentException( "Out of range" );
            }
        }

        public int getResultIdColumnIndex() {
            return icId_;
        }

        public int getResultScoreColumnIndex() {
            return icDist_;
        }
    }
}
