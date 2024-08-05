package uk.ac.starlink.ttools.cone;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.CgiQuery;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.URLUtils;
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
    private final ContentCoding coding_;
    private final boolean uploadFirst_;

    /** URL for the CDS Xmatch service. */
    public static final String XMATCH_URL =
        "http://cdsxmatch.u-strasbg.fr/xmatch/api/v1/sync";

    /** Alias for Simbad flat view table. */
    public static final String SIMBAD_NAME = "simbad";

    /** Whether it is safe/recommended to upload empty tables to match. */
    public static final boolean UPLOAD_EMPTY = false;

    /* Names of columns in uploaded table. */
    private static final String ID_NAME = "__UPLOAD_ID__";
    private static final String RA_NAME = "__UPLOAD_RA__";
    private static final String DEC_NAME = "__UPLOAD_DEC__";

    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * Constructor.
     *
     * @param  serviceUrl   URL of Xmatch service
     * @param  tableId      identifier of remote table
     * @param  srArcsec     match radius in arcseconds
     * @param  serviceMode  type of match
     * @param  coding       configures HTTP compression for result
     */
    public CdsUploadMatcher( URL serviceUrl, String tableId, double srArcsec,
                             ServiceFindMode serviceMode,
                             ContentCoding coding ) {
        serviceUrl_ = serviceUrl;
        tableId_ = tableId;
        srArcsec_ = srArcsec;
        serviceMode_ = serviceMode;
        coding_ = coding;
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
        final StarTable coneTable =
            new UploadConeTable( coneSeq, rowMapper,
                                 ID_NAME, RA_NAME, DEC_NAME );
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
            UwsJob.postForm( serviceUrl_, coding_, stringMap, streamMap );
        try {
            return TapQuery.streamResultVOTable( conn, coding_, rawResultSink );
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
        else if ( txt.equalsIgnoreCase( SIMBAD_NAME ) ) {
            return SIMBAD_NAME;
        }
        else {
            return "vizier:" + txt;
        }
    }

    /**
     * Reads the list of VizieR table aliases that can be used with
     * the Xmatch service.  This is expected to be a short list
     * (a few tens of entries).
     *
     * @return alias list
     */
    public static String[] readAliases() throws IOException {
        String url = XMATCH_URL + "/tables?action=getPrettyNames";
        logger_.info( "Reading VizieR aliases from " + url );
        List<String> aliasList = new ArrayList<String>();
        BufferedReader rdr = getLineReader( url, ContentCoding.NONE );
        try {
            for ( String line; ( line = rdr.readLine() ) != null; ) {
                aliasList.add( line );
            }
        }
        finally {
            rdr.close();
        }
        String[] aliases = aliasList.toArray( new String[ 0 ] );
        logger_.info( "Got " + aliases.length + " VizieR aliases" );
        Arrays.sort( aliases );
        return aliases;
    }

    /**
     * Reads the list of VizieR table names that can be used with
     * the Xmatch service.  This is expected to be several thousand
     * entries long.
     *
     * @return  table name list
     */
    public static String[] readVizierNames() throws IOException {
        String url = XMATCH_URL + "/tables?action=getVizieRTableNames";
        logger_.info( "Reading VizieR table names from " + url );
        List<String> nameList = new ArrayList<String>();
        BufferedReader rdr = getLineReader( url, ContentCoding.GZIP );
        try {
            for ( String line; ( line = rdr.readLine() ) != null; ) {
                String name = line.replaceAll( "[\\[\\]\",]", "" ).trim();
                if ( name.length() > 0 ) {
                    nameList.add( name );
                }
            }
        }
        finally {
            rdr.close();
        }
        String[] names = nameList.toArray( new String[ 0 ] );
        logger_.info( "Got " + names.length + "VizieR table names" );
        Arrays.sort( names );
        return names;
    }

    /**
     * Reads basic table metadata for a given VizieR table.
     *
     * @param  vizName  vizier table name or ID
     * @return   basic metadata object
     */
    public static VizierMeta readVizierMetadata( String vizName )
            throws IOException {
        CgiQuery query = new CgiQuery( XMATCH_URL + "/tables" );
        query.addArgument( "action", "getInfo" );
        query.addArgument( "tabName", vizName );
        URL url = query.toURL();
        logger_.info( "Reading " + vizName + " metadata from " + url );
        InputStream in =
            new BufferedInputStream( ContentCoding.NONE.openStream( url ) );
        JSONObject infoObj;
        try {
            JSONTokener jt = new JSONTokener( in );
            Object next = jt.nextValue();
            if ( next instanceof JSONObject ) {
                return new VizierMeta( (JSONObject) next );
            }
            else {
                throw new IOException( "Unexpected JSON object from " + url );
            }
        }
        finally {
            in.close();
        }
    }

    /**
     * Metadata provided for Vizier tables by the CDS Xmatch service.
     */
    public static class VizierMeta {
        private final String name_;
        private final String prettyName_;
        private final Long rowCount_;
        private final String desc_;

        /**
         * Constructs an instance from a JSON object.
         *
         * @param   jobj   vizier table info JSON object
         */
        private VizierMeta( JSONObject jobj ) {
            name_ = jobj.optString( "name", null );
            prettyName_ = jobj.optString( "prettyName", null );
            desc_ = jobj.optString( "desc", null );
            long nbrows = jobj.optLong( "nbrows", -1L );
            rowCount_ = nbrows >=0 ? Long.valueOf( nbrows ) : null;
        }

        /**
         * Returns the canonical VizieR table ID.
         *
         * @return  table name
         */
        public String getName() {
            return name_;
        }

        /**
         * Returns a table alias, if available.
         *
         * @return  table alias, or null
         */
        public String getPrettyName() {
            return prettyName_;
        }

        /**
         * Returns a table description.
         *
         * @return  table description
         */
        public String getDescription() {
            return desc_;
        }

        /**
         * Returns table row count.
         *
         * @return  row count, or null
         */
        public Long getRowCount() {
            return rowCount_;
        }

        @Override
        public String toString() {
            StringBuffer sbuf = new StringBuffer();
            if ( name_ != null ) {
                sbuf.append( "name: " )
                    .append( name_ );
            }
            if ( prettyName_ != null ) {
                sbuf.append( "; " )
                    .append( "prettyName: " )
                    .append( prettyName_ );
            }
            if ( rowCount_ != null ) {
                sbuf.append( "; " )
                    .append( "rowCount: " )
                    .append( rowCount_ );
            }
            if ( desc_ != null ) {
                sbuf.append( "; " )
                    .append( "desc: " )
                    .append( desc_ );
            }
            return sbuf.toString();
        }
    }

    /**
     * Returns a BufferedReader to read text lines from a URL,
     * suitable for VizieR output.
     *
     * @param  urltxt  target URL
     * @param  coding  defines HTTP-level compression
     * @return  UTF8 reader
     */
    private static BufferedReader getLineReader( String urltxt,
                                                 ContentCoding coding )
            throws IOException {
        URL url = URLUtils.newURL( urltxt );
        InputStream in = coding.openStream( url );
        return new BufferedReader( new InputStreamReader( in, "utf8" ) );
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
