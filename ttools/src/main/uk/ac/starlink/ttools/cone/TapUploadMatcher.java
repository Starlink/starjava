package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.SAXException;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.TapService;
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

    private final TapService tapService_;
    private final String tableName_;
    private final String raExpr_;
    private final String decExpr_;
    private final String radiusDegExpr_;
    private final boolean isSync_;
    private final String[] tapCols_;
    private final ServiceFindMode serviceMode_;
    private final int pollMillis_ = 10000;
    private final Map<String,String> extraParams_;
    private final ContentCoding coding_;

    private static final String TABLE_ID = "up";
    private static final String ID_NAME = "tapupload_id";
    private static final String ID_ALIAS = "tapupload_id_a";
    private static final String RA_NAME = "lon";
    private static final String DEC_NAME = "lat";

    /**
     * Constructor.
     *
     * @param  tapService  TAP service description
     * @param  tableName   name of table in TAP service to match against
     * @param  raExpr    column name (or ADQL expression) for RA
     *                   in decimal degrees in TAP table
     * @param  decExpr   column name (or ADQL expression) for Declination
     *                   in decimal degrees in TAP table
     * @param  radiusDegExpr  ADQL expression (maybe constant) for search
     *                        radius in decimal degrees
     * @param  isSync     true for synchronous, false for asynchronous
     * @param  tapCols    column names from the remote table to be included
     *                    in the output table; if null, all are included
     * @param  serviceMode  type of match
     * @param  extraParams  map of additional parameters for TAP query
     * @param  coding     configures HTTP compression for result
     */
    public TapUploadMatcher( TapService tapService, String tableName,
                             String raExpr, String decExpr,
                             String radiusDegExpr, boolean isSync,
                             String[] tapCols, ServiceFindMode serviceMode,
                             Map<String,String> extraParams,
                             ContentCoding coding ) {
        tapService_ = tapService;
        tableName_ = tableName;
        raExpr_ = raExpr;
        decExpr_ = decExpr;
        radiusDegExpr_ = radiusDegExpr;
        isSync_ = isSync;
        tapCols_ = tapCols;
        serviceMode_ = serviceMode;
        extraParams_ = extraParams;
        coding_ = coding;
        if ( ! Arrays.asList( getSupportedServiceModes() )
                     .contains( serviceMode ) ) {
            throw new IllegalArgumentException( "Unsupported mode: "
                                              + serviceMode );
        }
    }

    public boolean streamRawResult( ConeQueryRowSequence coneSeq,
                                    TableSink rawResultSink,
                                    RowMapper<?> rowMapper, long maxrec )
            throws IOException {
        String adql = getAdql( maxrec );
        Map<String,StarTable> uploadMap = new HashMap<String,StarTable>();
        uploadMap.put( TABLE_ID,
                       new UploadConeTable( coneSeq, rowMapper,
                                            ID_NAME, RA_NAME, DEC_NAME ) );
        VOTableWriter voWriter =
            new VOTableWriter( DataFormat.BINARY, true, VOTableVersion.V12 );
        TapQuery tapQuery =
            new TapQuery( tapService_, adql, extraParams_, uploadMap, -1,
                          voWriter );
        final URLConnection conn;
        if ( isSync_ ) {
            conn = tapQuery.createSyncConnection( coding_ );
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
            conn = AuthManager.getInstance().connect( url, coding_ );
        }

        /* There is, as far as I can tell, no way to write ADQL that gives
         * you closest-only matches.  So to get that effect we have to do
         * an ALL match, order the results by identifier,
         * and then pick the best one per-identifier client-side.
         * If the match radius is large, this will be much more expensive
         * on data transfer than it should be, but I don't know any
         * other way round it. */
        if ( serviceMode_ == ServiceFindMode.BEST ) {
            rawResultSink = new FilterBestSink( rawResultSink );
            if ( maxrec >= 0 ) {
                rawResultSink = new LimitRowSink( rawResultSink, maxrec );
            }
        }

        /* Pass the results to the output sink. */
        try {
            return TapQuery.streamResultVOTable( conn, coding_, rawResultSink )
                || ( (rawResultSink instanceof LimitRowSink) &&
                     ((LimitRowSink) rawResultSink).isTruncated() );
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
        boolean isBest = serviceMode_ == ServiceFindMode.BEST;
        boolean isClientFilter = serviceMode_ == ServiceFindMode.BEST;
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "SELECT" );
        if ( maxrec >= 0 && ! isClientFilter ) {
            sbuf.append( nl )
                .append( "TOP " )
                .append( maxrec );
        }
        sbuf.append( nl );
        if ( ! serviceMode_.isScoreOnly() ) {
            if ( tapCols_ == null ) {
                sbuf.append( "r.*, " );
            }
            else {
                for ( String colname : tapCols_ ) {
                    sbuf.append( "r." )
                        .append( colname )
                        .append( ", " );
                }
            }
        }
        sbuf.append( "u." )
            .append( ID_NAME )
            .append( " AS " )
            .append( ID_ALIAS )
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
        sbuf.append( "*3600.0" )
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
                .append( "GROUP BY " )
                .append( ID_ALIAS );
        }
        else if ( isBest ) {
            sbuf.append( nl )
                .append( "ORDER BY " )
                .append( ID_ALIAS )
                .append( " ASC" );
        }
        return sbuf.toString();
    }

    /**
     * Returns service modes supported by this class.
     * Currently, all are supported apart from
     * {@link ServiceFindMode#BEST_REMOTE}.
     * That one is basically impossible to do using ADQL as far as I can tell,
     * since there is in general no way to tell what remote table row
     * a given row of a query result is referring to.
     * You could do it in special cases if the table in question has a
     * primary key. 
     *
     * @return  supported find modes
     */
    public static ServiceFindMode[] getSupportedServiceModes() {
        return new ServiceFindMode[] {
            ServiceFindMode.ALL,
            ServiceFindMode.ALL_SCORE,
            ServiceFindMode.BEST,
            ServiceFindMode.BEST_SCORE,
        };
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

    /**
     * Wrapper TableSink implementation that turns an ALL-type match into
     * a BEST-type match.
     * From each contiguous group of input rows sharing a value of the
     * identifier column, it forwards to its base sink only the row
     * with the lowest value in the score column, discarding the rest.
     * Of course this only works properly if the rows it receives are
     * ordered in such a way that all rows sharing a single identifier
     * are contiguous.
     */
    private static class FilterBestSink implements TableSink {
        private final TableSink base_;
        private int icId_;
        private int icScore_;
        private Object currentId_;
        private double bestScore_;
        private Object[] bestRow_;

        /**
         * Constructor.
         *
         * @param  base  base sink
         */
        public FilterBestSink( TableSink base ) {
            base_ = base;
        }

        public void acceptMetadata( StarTable meta )
                throws TableFormatException {
            ColumnPlan cplan = new TapColumnPlan( Tables.getColumnInfos( meta ),
                                                  new ColumnInfo[ 0 ] );
            icId_ = cplan.getResultIdColumnIndex();
            icScore_ = cplan.getResultScoreColumnIndex();
            base_.acceptMetadata( meta );
        }

        public void acceptRow( Object[] row ) throws IOException {
            Object id = row[ icId_ ];
            Object scoreObj = row[ icScore_ ];
            double score = scoreObj instanceof Number
                         ? ((Number) scoreObj).doubleValue()
                         : Double.NaN;
            if ( ! id.equals( currentId_ ) ) {
                flushBestRow();
                currentId_ = id;
                bestScore_ = Double.POSITIVE_INFINITY;
            }
            if ( score < bestScore_ ) {
                bestScore_ = score;
                bestRow_ = row.clone();
            }
        }

        public void endRows() throws IOException {
            flushBestRow();
            base_.endRows();
        }

        /**
         * If a row with the best score has been identified and not yet
         * passed to the base sink, do it now.
         */
        private void flushBestRow() throws IOException {
            if ( bestRow_ != null ) {
                base_.acceptRow( bestRow_ );
                bestRow_ = null;
            }
        }
    }
}
