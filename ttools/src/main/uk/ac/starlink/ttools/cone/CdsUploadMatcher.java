package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.UnrepeatableSequenceException;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.vo.HttpStreamParam;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.UwsJob;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableVersion;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * UploadMatcher implementation for the CDS Xmatch service.
 * This class encapsulates all the information about the CDS Xmatch
 * I/O interface.
 *
 * @author   Mark Taylor
 * @since    14 May 2014
 */
public class CdsUploadMatcher implements UploadMatcher {

    private final URL serviceUrl_;
    private final String tableId_;
    private final double srArcsec_;
    private final ServiceFindMode serviceMode_;
    private final boolean uploadFirst_;

    /** URL for the CDS Xmatch service. */
    public static final String XMATCH_URL =
        "http://cdsxmatch.u-strasbg.fr/xmatch/api/v1/sync";

    private static final String RA_NAME = "__UPLOAD_RA__";
    private static final String DEC_NAME = "__UPLOAD_DEC__";
    private static final String ID_NAME = "__UPLOAD_ID__";
    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * Constructor.
     *
     * @param  serviceUrl   URL of Xmatch service
     * @param  tableId      identifier of remote table
     * @param  srArcsec     match radius in arcseconds
     * @param  serviceMode  type of match
     */
    public CdsUploadMatcher( URL serviceUrl, String tableId, double srArcsec,
                             ServiceFindMode serviceMode ) {
        serviceUrl_ = serviceUrl;
        tableId_ = tableId;
        srArcsec_ = srArcsec;
        serviceMode_ = serviceMode;
        uploadFirst_ = serviceMode_ != ServiceFindMode.BEST_REMOTE;
    }

    public boolean streamRawResult( ConeQueryRowSequence coneSeq,
                                    TableSink rawResultSink,
                                    RowMapper<?> rowMapper, long maxrec )
            throws IOException {

        /* Prepare string parameters. */
        final String uploadIndex;
        final String remoteIndex;
        if ( uploadFirst_ ) {
            uploadIndex = "1";
            remoteIndex = "2";
        }
        else {
            uploadIndex = "2";
            remoteIndex = "1";
        }
        Map<String,String> stringMap = new LinkedHashMap<String,String>();
        stringMap.put( "REQUEST", "xmatch" );
        stringMap.put( "RESPONSEFORMAT", "votable" );
        stringMap.put( "distMaxArcsec", Double.toString( srArcsec_ ) );
        stringMap.put( "selection",
                       serviceMode_.isBestOnly() ? "best" : "all" );
        stringMap.put( "cat" + remoteIndex, tableId_ );
        stringMap.put( "colRA" + uploadIndex, RA_NAME );
        stringMap.put( "colDec" + uploadIndex, DEC_NAME );
        stringMap.put( "cols" + uploadIndex, ID_NAME );
        if ( serviceMode_.isScoreOnly() ) {
            stringMap.put( "cols" + remoteIndex, "" );
        }
        if ( maxrec >= 0 ) {
            stringMap.put( "MAXREC", Long.toString( maxrec ) );
        }

        /* Prepare streamed parameter containing uploaded table. */
        Map<String,HttpStreamParam> streamMap =
            new LinkedHashMap<String,HttpStreamParam>();
        String uploadParamName = "cat" + uploadIndex;
        final Map<String,String> headerMap = new LinkedHashMap<String,String>();
        headerMap.put( "Content-Type", "application/x-votable+xml" );
        final VOTableWriter vowriter =
            new VOTableWriter( DataFormat.BINARY, true, VOTableVersion.V12 );
        final StarTable coneTable = new UploadConeTable( coneSeq, rowMapper );
        HttpStreamParam param = new HttpStreamParam() {
            public Map<String,String> getHttpHeaders() {
                return headerMap;
            }
            public long getContentLength() {
                return -1;
            }
            public void writeContent( OutputStream out )
                    throws IOException {
                vowriter.writeStarTable( coneTable, out );
            }
        };
        streamMap.put( uploadParamName, param );

        /* Invoke the service using HTTP POST, and stream the output
         * to the supplied table sink. */
        URLConnection conn =
            UwsJob.postForm( serviceUrl_, stringMap, streamMap );
        try {
            return TapQuery.streamResultVOTable( conn, rawResultSink );
        }
        catch ( SAXException e ) {
            throw (IOException)
                  new IOException( "Parse error from CDS Xmatch service"
                                 + " result " + e )
                 .initCause( e );
        }
    }

    public ColumnPlan getColumnPlan( ColumnInfo[] resultCols,
                                     ColumnInfo[] uploadCols ) {
        return new CdsColumnPlan( resultCols, uploadCols, uploadFirst_ );
    }

    /**
     * Turns a user-supplied string referencing a CDS table into an
     * identifier recognisable by the CDS Xmatch service.
     * At present this just prepends a "vizier:" where applicable.
     *
     * @param   txt    table name
     * @return  xmatch service identifier
     */
    public static String toCdsId( String txt ) {
        if ( txt == null ) {
            return null;
        }
        txt = txt.trim();
        if ( txt.length() == 0 ) {
            return null;
        }
        else if ( txt.equalsIgnoreCase( "simbad" ) ) {
            return "simbad";
        }
        else {
            return "vizier:" + txt;
        }
    }

    /**
     * Table suitable for uploading based on a sequence of positional queries
     * and an RowMapper.
     * The resulting table contains just three columns: ID, RA, Dec.
     *
     * <p>This is a one-shot sequential table - only one row sequence
     * may be taken out from it.
     */
    private static class UploadConeTable extends AbstractStarTable {
        private ConeQueryRowSequence coneSeq_;
        private final RowMapper<?> rowMapper_;
        private final ColumnInfo[] colInfos_;

        /**
         * Constructor.
         *
         * @param  coneSeq  sequence of positional queries
         * @param  rowMapper  maps index of query to an identifier object
         */
        UploadConeTable( ConeQueryRowSequence coneSeq, RowMapper rowMapper ) {
            coneSeq_ = coneSeq;
            rowMapper_ = rowMapper;
            colInfos_ = new ColumnInfo[] {
                new ColumnInfo( ID_NAME, rowMapper_.getIdClass(),
                                "Row identifier" ),
                new ColumnInfo( RA_NAME, Double.class, "ICRS Right Ascension" ),
                new ColumnInfo( DEC_NAME, Double.class, "ICRS Declination" ),
            };
        }

        public int getColumnCount() {
            return colInfos_.length;
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return colInfos_[ icol ];
        }

        public long getRowCount() {
            return -1;
        }

        public synchronized RowSequence getRowSequence() throws IOException {
            if ( coneSeq_ == null ) {
                throw new UnrepeatableSequenceException();
            }
            final ConeQueryRowSequence coneSeq = coneSeq_;
            coneSeq_ = null;
            return new RowSequence() {
                private int irow_;
                private Object[] row_;
                public boolean next() throws IOException {
                    boolean hasNext = coneSeq.next();
                    row_ = hasNext
                         ? new Object[] {
                               rowMapper_.rowIndexToId( irow_++ ),
                               new Double( coneSeq.getRa() ),
                               new Double( coneSeq.getDec() ),
                           }
                         : null;
                    assert ! hasNext || isRowCompatible( row_, colInfos_ );
                    return hasNext;
                }
                public Object[] getRow() {
                    return row_;
                }
                public Object getCell( int icol ) {
                    return row_[ icol ];
                }
                public void close() throws IOException {
                    coneSeq.close();
                }
            };
        }

        /**
         * Determines whether the contents of a given row are
         * compatible with a given list of column metadata objects.
         * Used for assertions.
         *
         * @param  row  tuple of values
         * @param  infos  matching tuple of value metadata objects
         * @return  true iff compatible
         */
        private boolean isRowCompatible( Object[] row, ValueInfo[] infos ) {
            int n = row.length;
            if ( infos.length != n ) {
                return false;
            }
            for ( int i = 0; i < n; i++ ) {
                Object cell = row[ i ];
                if ( cell != null &&
                     ! infos[ i ].getContentClass()
                      .isAssignableFrom( cell.getClass() ) ) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * ColumnPlan implementation for this matcher.
     */
    private static class CdsColumnPlan implements ColumnPlan {

        private final int ncUp_;
        private final int ncRem_;
        private final int ncOut_;
        private final int icId_;
        private final int icDist_;
        private final int ic0Rem_;
        private final int jcDist_;
        private final int jc0Up_;
        private final int jc0Rem_;

        /**
         * Constructor.
         *
         * @param   resultCols  column metadata for the raw result table
         * @param   uploadCols  column metadata for the uploaded table
         * @param   isUploadFirst  true if the upload table is table 1 and
         *                         the remote table is table 2;
         *                         false for the other way round
         */
        CdsColumnPlan( ColumnInfo[] resultCols, ColumnInfo[] uploadCols,
                       boolean isUploadFirst ) {
            int ncRes = resultCols.length;
            ncUp_ = uploadCols.length;
            ncRem_ = ncRes - 2;
            ncOut_ = ncUp_ + ncRem_ + 1;

            /* The way the CDS Xmatch service is currently defined/documented,
             * the first output column is a separation value.
             * Then come all the included columns of table 1,
             * then all the included columns of table 2. */
            icDist_ = 0;
            if ( isUploadFirst ) {
                icId_ = 1;
                ic0Rem_ = 2;
            }
            else {
                ic0Rem_ = 1;
                icId_ = ncRes - 1;
            }
            jc0Up_ = 0;
            jc0Rem_ = jc0Up_ + ncUp_;
            jcDist_ = jc0Rem_ + ncRem_;

            /* Check that the columns in the result are as expected.
             * If not, the match will probably go horribly wrong. */
            ColumnInfo distInfo = resultCols[ icDist_ ];
            if ( ! "angDist".equalsIgnoreCase( distInfo.getName() ) ||
                 ! "arcsec".equals( distInfo.getUnitString() ) ||
                 ! Number.class
                         .isAssignableFrom( distInfo.getContentClass() ) ) {
                logger_.warning( "Unexpected Distance column " + icDist_
                               + " from CDS Xmatch: " + distInfo );
            }
            ColumnInfo idInfo = resultCols[ icId_ ];
            if ( ! ID_NAME.equals( idInfo.getName() ) ) {
                logger_.warning( "Unexpected ID column " + icId_
                               + " from CDS Xmatch: " + idInfo );
            }
        }

        public int getOutputColumnCount() {
            return ncOut_;
        }

        public int getOutputColumnLocation( int icolOutput ) {
            assert 0 <= jc0Up_
                && jc0Up_ <= jc0Rem_
                && jc0Rem_ <= jcDist_
                && jcDist_ == ncOut_ - 1;
            if ( icolOutput < jc0Up_ ) {
                throw new IllegalArgumentException( "out of range" );
            }
            else if ( icolOutput < jc0Rem_ ) {
                int iUp = icolOutput - jc0Up_;
                return -iUp - 1;
            }
            else if ( icolOutput < jcDist_ ) {
                int iRes = icolOutput + ic0Rem_ - jc0Rem_;
                return iRes;
            }
            else if ( icolOutput == jcDist_ ) {
                int iRes = icDist_;
                return iRes;
            }
            else {
                throw new IllegalArgumentException( "out of range" );
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
