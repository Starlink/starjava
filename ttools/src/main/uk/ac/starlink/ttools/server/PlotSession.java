package uk.ac.starlink.ttools.server;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUnits;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.CoordSequence;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.IndicatedRow;
import uk.ac.starlink.ttools.plot2.NavAction;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotScene;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.DiskCache;
import uk.ac.starlink.ttools.plot2.data.TupleRunner;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurface;
import uk.ac.starlink.ttools.plot2.task.HighlightIcon;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.util.SplitCollector;

/**
 * Maintains state for a server-side plot.
 * This object manages client- and server-side communications to
 * allow plot navigation driven by user mouse actions in the browser.
 *
 * @author   Mark Taylor
 * @since    13 Dec 2019
 */
public class PlotSession<P,A> {

    private final String plotTxt_;
    private final PlotScene<P,A> scene_;
    private final Navigator<A> navigator_;
    private final GraphicExporter exporter_;
    private final DataStore dataStore_;
    private final String imgSuffix_;
    private final DiskCache imgCache_;
    private final A[] initialAspects_;
    private final Dimension initialSize_;
    private List<HighlightPosition> highlights_;
    private DragContext dragged_;
    private Dimension size_;

    /** Name of JavaScript plotting library resource. */
    public static final String JS_FILE = "plot2Lib.js";

    public static final String IMGSRC_KEY = "imgSrc";
    public static final String TRANSIENTSVG_KEY = "transientSvg";
    public static final String STATICSVG_KEY = "staticSvg";
    public static final String DATAPOS_KEY = "dataPos";
    public static final String TXTPOS_KEY = "txtPos";
    public static final String MESSAGE_KEY = "message";
    public static final String BOUNDS_KEY = "bounds";
    public static final String FORMAT_KEY = "format";

    public static final PlotService HTML_SERVICE;
    public static final PlotService STATE_SERVICE;
    public static final PlotService IMGSRC_SERVICE;
    public static final PlotService POSITION_SERVICE;
    public static final PlotService COUNT_SERVICE;
    public static final PlotService ROW_SERVICE;

    /** Available PlotService instances. */
    public static final PlotService[] SERVICES = new PlotService[] {
        HTML_SERVICE = createHtmlService( "html" ),
        STATE_SERVICE = createStructureService( "state" ),
        IMGSRC_SERVICE = createImageService( "imgsrc" ),
        POSITION_SERVICE = createPlotPositionService( "position" ),
        COUNT_SERVICE = createCountService( "count" ),
        ROW_SERVICE = createRowService( "row" ),
    };

    private static String JS_TEXT;
    private static final boolean IS_COUNT_PARALLEL = false;
    private static final GraphicExporter[] EXPORTERS =
        GraphicExporter.getKnownExporters( PlotUtil.LATEX_PDF_EXPORTER );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.server" );
   
    /**
     * Constructor.
     *
     * @param   plotTxt   plot text; this is transmitted alongside the
     *                    sessionId in plot requests so that the session
     *                    can be reconstructed if the session expires
     * @param   scene    contains parsed plot information
     * @param   navigator  plot navigation object
     * @param   exporter  defines image graphics format
     * @param   dataStore  data storage
     * @param   size   initial dimension of IMG element
     * @param   imgCache  disk cache for storing default image files;
     *                    may be null for no caching
     */
    public PlotSession( String plotTxt, PlotScene<P,A> scene,
                        Navigator<A> navigator, GraphicExporter exporter,
                        DataStore dataStore, Dimension size,
                        DiskCache imgCache ) {
        plotTxt_ = plotTxt;
        scene_ = scene;
        navigator_ = navigator;
        exporter_ = exporter;
        dataStore_ = dataStore;
        size_ = size;
        imgCache_ = imgCache;
        String[] suffixes = exporter.getFileSuffixes();
        imgSuffix_ = suffixes.length > 0 ? suffixes[ 0 ] : "";
        initialSize_ = new Dimension( size );
        initialAspects_ = scene_.getAspects().clone();
        highlights_ = new ArrayList<HighlightPosition>();
    }

    /**
     * Writes the bytes containing the image file for the current
     * state of this session to a given output stream.
     * The stream is not closed by this method.
     *
     * @param  out  output stream
     * @param  exporter  controls output format
     */
    private void writeImageData( OutputStream out, GraphicExporter exporter )
            throws IOException {
        final int width = size_.width;
        final int height = size_.height;
        Picture picture = new Picture() {
            public int getPictureWidth() {
                return width;
            }
            public int getPictureHeight() {
                return height;
            }
            public void paintPicture( Graphics2D g2 ) {
                scene_.paintScene( g2, getExternalBounds(), dataStore_ );
            }
        };
        BufferedOutputStream bout = new BufferedOutputStream( out );
        exporter.exportGraphic( picture, bout );
        bout.flush();
    }

    /**
     * Returns an object that can output the image file corresponding
     * to the state of this session as determined by a supplied request.
     *
     * @param   request   request
     * @param   exporter  controls output format
     * @return  image writer
     */
    private ImageWriter getImageWriter( HttpServletRequest request,
                                        final GraphicExporter exporter )
            throws IOException {
        final UpdateResult result;

        /* If this is the initial request unadorned by navigation query
         * parameters, then attempt to use a cached version of the image.
         * The reasoning is that a page with an interactive image may be
         * loaded often without anyone actually interacting with the image.
         * Then it's much cheaper just to cache the bytes than to
         * reconstruct them.  Only once people want to interact with the
         * plot do we dispense uncached custom image files. */
        if ( isInitialRequest( request ) ) {
            result = UpdateResult.CHANGED;
            scene_.setAspects( initialAspects_ );
            final File imgFile = getInitialCachedImageFile();
            if ( imgFile != null ) {
                return new ImageWriter() {
                    public long getByteCount() {
                        return imgFile.length();
                    }
                    public boolean isChanged() {
                        return true;
                    }
                    public Decoration getDecoration() {
                        return null;
                    }
                    public void writeImage( OutputStream out )
                            throws IOException {
                        InputStream in = new FileInputStream( imgFile );
                        IOUtils.copy( in, out );
                        in.close();
                    }
                };
            }
        }
        else {
            result = updateAspect( request );
        }
        return new ImageWriter() {
            public long getByteCount() {
                return -1;
            }
            public boolean isChanged() {
                return result.isImageChanged_;
            }
            public Decoration getDecoration() {
                return result.decoration_;
            }
            public void writeImage( OutputStream out ) throws IOException {
                writeImageData( out, exporter );
            }
        };
    }

    /**
     * Use command parameters supplied with an HTTP request to adjust
     * the aspect of the image that will be plotted next time.
     *
     * @param   request  HTTP request
     * @return  object indicating the outcome of the operation
     */
    private UpdateResult updateAspect( HttpServletRequest request ) {
        ensureSurfaces();

        /* Extract basic parameters from the request. */
        Map<String,String> paramMap = getSingleParameterMap( request );
        String cmdName = paramMap.get( "navigate" );
        Point pos = parseXY( paramMap.get( "pos" ) );
        int ibutton = parseInteger( paramMap.get( "ibutton" ), 0 );
        int wheelrot = parseInteger( paramMap.get( "wheelrot" ), 0 );
        Surface[] surfs = scene_.getSurfaces();
        A[] aspects = scene_.getAspects().clone();

        /* Handle non-positional request specially. */
        if ( "resize".equals( cmdName ) ) {
            Point sizePt = parseXY( paramMap.get( "size" ) );
            Dimension size = new Dimension( sizePt.x, sizePt.y );
            if ( ! size.equals( size_ ) ) {
                size_ = size;
                scene_.clearPlot();
                return UpdateResult.CHANGED;
            }
            else {
                return UpdateResult.UNCHANGED;
            }
        }

        /* Otherwise, try to interpret request as a navigation action. */
        if ( pos == null ) {
            return UpdateResult.UNCHANGED;
        }
        final NavAction<A> navAction;
        final int iz;
        if ( "click".equals( cmdName ) ) {
            iz = scene_.getGang().getNavigationZoneIndex( pos );
            if ( iz >= 0 ) {
                PlotLayer[] layers = scene_.getLayers( iz );
                Supplier<CoordSequence> dposSupplier =
                    new PointCloud( SubCloud.createSubClouds( layers, true ) )
                   .createDataPosSupplier( dataStore_ );
                navAction =
                    navigator_.click( surfs[ iz ], pos, ibutton, dposSupplier );
            }
            else {
                navAction = null;
            }
        }
        else if ( "drag".equals( cmdName ) ) {
            DragContext dragged = dragged_;
            if ( dragged == null ) {
                Point origin = parseXY( paramMap.get( "origin" ) );
                int isurf = scene_.getGang().getNavigationZoneIndex( origin );
                dragged = new DragContext( origin );
                if ( isurf >= 0 ) {
                    dragged.isurf_ = isurf;
                    dragged.surface_ = surfs[ isurf ];
                }
                dragged_ = dragged;
            }
            Point origin = dragged.start_;
            iz = dragged_.isurf_;
            Surface surf = dragged.surface_;
            boolean isEnd = paramMap.containsKey( "end" );
            if ( isEnd ) {
                dragged_ = null;
            }
            if ( surf != null ) {
                navAction = isEnd
                          ? navigator_.endDrag( surf, pos, ibutton, origin )
                          : navigator_.drag( surf, pos, ibutton, origin );
            }
            else {
                navAction = null;
            }
        }
        else if ( "wheel".equals( cmdName ) && wheelrot != 0 ) {
            iz = scene_.getGang().getNavigationZoneIndex( pos );
            navAction = iz >= 0
                      ? navigator_.wheel( surfs[ iz ], pos, wheelrot )
                      : null;
        }
        else {
            return UpdateResult.UNCHANGED;
        }

        /* Update the scene accordingly. */
        if ( navAction == null ) {
            return UpdateResult.UNCHANGED;
        }
        else {
            A aspect = navAction.getAspect();
            Decoration dec = navAction.getDecoration();
            if ( aspect == null ) {
                return new UpdateResult( false, dec );
            }
            else {
                aspects[ iz ] = aspect;
                A[] newAspects =
                    scene_.getGanger().adjustAspects( aspects, iz );
                return new UpdateResult( scene_.setAspects( newAspects ), dec );
            }
        }
    }

    /**
     * Checks that the scene has been initialised, i.e. has been painted
     * at least once, otherwise various activities that rely on knowing
     * the surface geometry won't work.
     * At most points in the session lifecycle there will have been
     * an earlier repaint, but there might not in the case that only
     * the initial default plot has so far been dispensed for this session,
     * and that it was got from the image cache rather than by
     * actually doing the painting.
     */
    private void ensureSurfaces() {
        if ( scene_.getSurfaces()[ 0 ] == null ) {
            scene_.prepareScene( getExternalBounds(), dataStore_ );
        }
    }

    /**
     * Indicates whether the given request is for the unchanged initial image.
     *
     * @param   request   request object
     * @return  true for initial image;
     *          false if some navigation change has been requested
     */
    private boolean isInitialRequest( HttpServletRequest request ) {
        Map<String,String> paramMap = getSingleParameterMap( request );
        String cmdName = paramMap.get( "navigate" );
        return "reset".equals( cmdName ) && initialSize_.equals( size_ );
    }

    /**
     * Attempts to return a cached file containing the initial, unnavigated
     * image corresponding to this plot session.
     * If a file representing that image does not currently exist
     * in the cache, it will first be written.
     *
     * @return  initial image file, or null
     */
    private File getInitialCachedImageFile() throws IOException {

        /* If no cache is in operation, return null. */
        if ( imgCache_ == null ) {
            return null;
        }

        /* Check the cache directory is present and writable. */
        imgCache_.ready();

        /* Work out the filename corresponding to the initial image. */
        File file =
            new File( imgCache_.getDir(),
                      "I-" + DiskCache.hashText( plotTxt_ ) + imgSuffix_ );

        /* If it already exists, update its last-modified time so that
         * it goes to the head of the LRU cache invalication list. */
        if ( file.exists() ) {
            imgCache_.touch( file );
        }

        /* If it doesn't exist, write it.  Write to a temporary file and
         * then rename it to the target filename.  This atomic creation
         * of the target file prevents other accesses picking up a
         * half-written version. */
        else {
            File workFile = DiskCache.toWorkFilename( file );
            try {
                OutputStream out = new FileOutputStream( workFile );
                writeImageData( out, exporter_ );
                out.close();
                workFile.renameTo( file );
                imgCache_.fileAdded( file );
                imgCache_.log( "Wrote cached image file to " + file );
                imgCache_.tidy();
            }
            catch ( IOException e ) {
                String msg = "Failed write to cached file " + workFile
                           + " (" + e + ")";
                logger_.log( Level.WARNING, msg, e );
                return null;
            }
        }

        /* Return the existing or newly-written file. */
        return file.exists() && file.canRead() ? file : null;
    }

    /**
     * Returns the external bounds of this plot as a rectangle.
     *
     * @return  external plot bounds
     */
    private Rectangle getExternalBounds() {
        return new Rectangle( size_ );
    }

    /**
     * Returns the graphical representation of a plot decoration
     * as the content of an SVG element.
     *
     * @param  decs  decorations
     * @return   SVG element text
     */
    private String decorationSvg( List<Decoration> decs ) {
        SVGGraphics2D g2 =
            new SVGGraphics2D( size_.width, size_.height, SVGUnits.PX );
        for ( Decoration dec : decs ) {
            dec.paintDecoration( g2 );
        }
        return g2.getSVGElement();
    }
 
    /**
     * Set headers appropriately for replying with image data.
     *
     * @param  response  output object
     * @param  exporter   controls image output format
     */
    private void prepareImageResponse( HttpServletResponse response,
                                       GraphicExporter exporter ) {
        response.setContentType( exporter.getMimeType() );
        String encoding = exporter.getContentEncoding();
        if ( encoding != null ) {
            response.setHeader( "Content-Encoding", encoding );
        }
        response.setStatus( 200 );
    }

    /**
     * Must be called before any session is instantiated.
     */
    public static void init() throws IOException {
        JS_TEXT = readText( JS_FILE );
    }

    /**
     * Returns XML documentation for navigation parameters that apply
     * to those PlotServices that can update the plot state.
     *
     * @return  XML doc text
     */
    private static String getNavigationXmlDoc() {
        String posSpec = "&amp;pos=x,y";
        String originSpec = "&amp;origin=x0,y0";
        String toposSpec = "&amp;pos=x1,y1";
        String ibuttSpec = "&amp;ibutton=1|2|3";
        String wheelSpec = "&amp;wheelrot=nstep";
        return String.join( "\n",
            "<ul>",
            "<li><code>navigate=reset</code>:",
            "    reset the plot to initial state</li>",
            "<li><code>navigate=resize&amp;size=width,height</code>:",
            "    resize the plot to <code>width</code> x <code>height</code>",
            "    pixels</li>",
            "<li><code>navigate=click" + posSpec + ibuttSpec + "</code>:",
            "    emulate TOPCAT click on plot at graphics position",
            "    <code>x</code>,<code>y</code></li>",
            "<li><code>navigate=drag" + originSpec + toposSpec + ibuttSpec
                                      + "</code>:",
            "    emulate TOPCAT continuing drag from start graphics position",
            "    <code>x0</code>,<code>y0</code> to",
            "    <code>x1</code>,<code>y1</code></li>",
            "<li><code>navigate=drag" + originSpec + toposSpec + ibuttSpec
                                      + "&amp;end=true</code>:",
            "    emulate TOPCAT end-drag from start graphics position",
            "    <code>x0</code>,<code>y0</code> to",
            "    <code>x1</code>,<code>y1</code></li>",
            "<li><code>navigate=wheel" + posSpec + wheelSpec + "</code>:",
            "    emulate TOPCAT mouse wheel at graphics position",
            "    <code>x</code>,<code>y</code></li>",
            "<li><code>navigate=none</code>:",
            "    no change to last position</li>",
            "</ul>",
        "" );
    }

    /**
     * Utility method to make the parameter map got from an HTTP request
     * easier to use.  It assumes that each parameter is supplied only once,
     * so the result can just be a String-&gt;String map.
     *
     * @param  request  HTTP request
     * @return  name-&gt;value pair map for parameters
     */
    private static Map<String,String>
            getSingleParameterMap( HttpServletRequest request ) {
        @SuppressWarnings("unchecked")
        Map<String,String[]> amap =
            (Map<String,String[]>) request.getParameterMap();
        Map<String,String> smap = new LinkedHashMap<String,String>();
        for ( Map.Entry<String,String[]> entry : amap.entrySet() ) {
            String[] array = entry.getValue();
            if ( array != null && array.length == 1 ) {
                String txt = array[ 0 ];
                if ( txt != null && txt.trim().length() > 0 ) {
                    smap.put( entry.getKey(), txt );
                }
            }
        }
        return smap;
    }

    /**
     * Parses a parameter string of the form "x,y" as a Point.
     *
     * @param  txt  comma-separated pair of integers
     * @return  Point object, or null if there's a problem
     */
    private static Point parseXY( String txt ) {
        int ipos = txt == null ? -1 : txt.indexOf( ',' );
        if ( ipos < 0 ) {
            return null;
        }
        try {
            int px = (int) Double.parseDouble( txt.substring( 0, ipos ) );
            int py = (int) Double.parseDouble( txt.substring( ipos + 1 ) );
            return new Point( px, py );
        }
        catch ( NumberFormatException e ) {
            return null;
        }
    }

    /**
     * Parses a parameter string representing an integer.
     *
     * @param  txt  integer string
     * @param  dflt   value to return if parse fails
     * @return  integer value, or dflt in case of trouble
     */
    private static int parseInteger( String txt, int dflt ) {
        if ( txt != null ) {
            try {
                return Integer.parseInt( txt );
            }
            catch ( NumberFormatException e ) {
                return dflt;
            }
        }
        else {
            return dflt;
        }
    }

    /**
     * Converts an image output format name to a GraphicExporter.
     *
     * @param   fmtName  name of supported output format (case-insensitive)
     * @return   GraphicExporter, or null if no match
     */
    private static GraphicExporter parseFormatName( String fmtName ) {
        for ( GraphicExporter exp : EXPORTERS ) {
            if ( exp.getName().equalsIgnoreCase( fmtName ) ) {
                return exp;
            }
        }
        return null;
    }

    /**
     * Returns the content of a table row as a JSON-friendly
     * column-name-&gt;column-value map.  That means it's not allowed to
     * contain any NaN or infinite floating point values.
     *
     * @param  table  table
     * @param  irow  row index
     * @return  labelled content of given row
     */
    private static Map<String,Object> getJsonRowData( StarTable table,
                                                      long irow )
            throws IOException {
        Object[] row = getRow( table, irow );
        if ( row == null ) {
            return null;
        }
        int nc = row.length;
        Map<String,Object> rowData = new LinkedHashMap<>();
        for ( int ic = 0; ic < nc; ic++ ) {
            String key = table.getColumnInfo( ic ).getName();
            Object value = row[ ic ];
            if ( value instanceof Number ) {
                double dval = ((Number) value).doubleValue();
                if ( dval != dval ) {
                    value = null;
                }
                else if ( Double.isInfinite( dval ) ) {
                    value = value.toString();
                }
            }
            rowData.put( key, value );
        }
        return rowData;
    }

    /**
     * Returns a 2- or 3-element (for planar or cube surface) giving
     * (lower,upper) bounds for the extent of the plot in data coordinates
     * in each plotted dimension.
     * Attempt to return values which are rounded (in decimal) to
     * sensible precisions.
     *
     * @param  surf  surface
     * @return   data bounds array, or null if not available
     */
    private static double[][] getDataBounds( Surface surf ) {
        if ( surf instanceof PlaneSurface ) {
            PlaneSurface psurf = (PlaneSurface) surf;
            double[][] limits = psurf.getDataLimits();
            double[][] rlimits = new double[ 2 ][];
            for ( int id = 0; id < 2; id++ ) {
               Axis axis = psurf.getAxes()[ id ];
               String smin =
                   PlaneSurface.formatPosition( axis, limits[ id ][ 0 ] );
               String smax =
                   PlaneSurface.formatPosition( axis, limits[ id ][ 1 ] );
               rlimits[ id ] = new double[] {
                   Double.parseDouble( smin ), Double.parseDouble( smax ),
               };
            }
            return rlimits;
        }
        else if ( surf instanceof CubeSurface ) {
            CubeSurface csurf = (CubeSurface) surf;
            Rectangle gbox = csurf.getPlotBounds();
            int npix = Math.max( gbox.width, gbox.height );
            Scale[] scales = csurf.getScales();
            double[][] rlimits = new double[ 3 ][];
            for ( int id = 0; id < 3; id++ ) {
                double[] lims = csurf.getDataLimits( id );
                String[] slims =
                    PlotUtil.formatAxisRangeLimits( lims[ 0 ], lims[ 1 ],
                                                    scales[ id ], npix );
                rlimits[ id ] = new double[] {
                    Double.parseDouble( slims[ 0 ] ),
                    Double.parseDouble( slims[ 1 ] ),
                };
            }
            return rlimits;
        }
        else if ( surf instanceof PlanarSurface ) {
            return ((PlanarSurface) surf).getDataLimits();
        }
        else {
            return null;
        }
    }

    /**
     * Returns the content of a table row as an Object array.
     * This might be a slow operation.
     *
     * @param  table  table
     * @param  irow  row index
     * @return  labelled content of given row
     */
    private static Object[] getRow( StarTable table, long irow )
            throws IOException {
        if ( table.isRandom() ) {
            return table.getRow( irow );
        }
        else {
            RowSequence rseq = table.getRowSequence();
            try {
                for ( long ir = 0; ir < irow; ir++ ) {
                    if ( ! rseq.next() ) {
                        return null;
                    }
                }
                return rseq.getRow();
            }
            finally {
                rseq.close();
            }
        }
    }

    /**
     * Utility method to read text from a given resource.
     *
     * @param  resourceName  resource location
     * @throws  IOException  if it can't be loaded
     */
    private static String readText( String resourceName ) throws IOException {
        InputStream in = PlotSession.class.getResourceAsStream( resourceName );
        if ( in == null ) {
            throw new FileNotFoundException( "No resource " + resourceName );
        }
        Reader rdr = new InputStreamReader( new BufferedInputStream( in ) );
        StringBuffer fbuf = new StringBuffer();
        for ( int chr; ( chr = rdr.read() ) >= 0; ) {
            fbuf.append( (char) chr );
        }
        rdr.close();
        return fbuf.toString();
    }

    /**
     * Writes a characters sequence to an output stream, on the assumption
     * that the characters are all 7-bit ASCII.
     *
     * @param   out   destination stream
     * @param   asciiTxt   string assumed to be ASCII
     */
    private static void writeAscii( OutputStream out, CharSequence asciiTxt )
            throws IOException {
        int leng = asciiTxt.length();
        for ( int i = 0; i < leng; i++ ) {
            out.write( asciiTxt.charAt( i ) );
        }
    }

    /**
     * Writes a string to JSON output as a quoted string.
     * The supplied text is wrapped in double quotes and the contents
     * are escaped as required.
     *
     * @param   out  destination stream
     * @param   txt  literal string to output
     */
    private static void writeJsonQuotedString( OutputStream out, String txt )
            throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter( out, "UTF-8" );
        JSONObject.quote( txt, writer );
        writer.flush();
    }

    /**
     * Write the output from an ImageWriter base64-encoded to a given
     * output stream.
     *
     * @param  out  destination stream, not closed by this method
     * @param  imwriter  image writer
     */
    private static void writeBase64( OutputStream out, ImageWriter imwriter )
            throws IOException {

        /* This is almost straightforward using java.util.Base64,
         * but not quite; the stream returned by Base64.Encoder.wrap
         * needs to be closed to make sure the final bytes are written,
         * but that close method closes the underlying stream which we
         * don't want to do since it's necessary to write some
         * non-Base64-encoded bytes afterwards.  So we have to use
         * a FilterOutputStream with the close method overridden as a no-op. */
        OutputStream b64out =
            Base64.getEncoder().wrap( new FilterOutputStream( out ) {
                @Override
                public void close() throws IOException {
                    // do not close the underlying stream
                }
            } );
        imwriter.writeImage( b64out );
        b64out.close();
    }

    /**
     * Represents the result of a plot update instruction;
     * it aggregates a flag indicating whether the plot has changed since
     * last time, and a plot decoration representing the recent navigation
     * action.
     */
    private static class UpdateResult {
        final boolean isImageChanged_;
        final Decoration decoration_;

        /** Instance indicating changed plot, no decoration. */
        final static UpdateResult CHANGED = new UpdateResult( true, null );

        /** Instance indicating unchanged plot, no decoration. */
        final static UpdateResult UNCHANGED = new UpdateResult( false, null );

        /**
         * Constructs an instance with given changed flag and decoration.
         *
         * @param  isImageChanged  flag indicating whether image has changed
         *                         since last time
         * @param  decoration   navigation decoration, may be null
         */
        UpdateResult( boolean isImageChanged, Decoration decoration ) {
            isImageChanged_ = isImageChanged;
            decoration_ = decoration;
        }
    }

    /**
     * Creates a service that provides a standalone HTML plot document.
     * 
     * @param  name  service name
     * @return  service
     */
    private static PlotService createHtmlService( String name ) {
        return new AbstractPlotService( name ) {
            @Override
            public boolean canCreateSession() {
                return true;
            }
            public String getXmlDescription() {
                return String.join( "\n",
                    "<p>Returns a new standalone HTML page containing",
                    "the interactive plot specified by the supplied",
                    "plot specification.",
                    "Note this does not require a session ID to be supplied,",
                    "and it has no parameters.",
                    "This action is easy to use, but not very flexible.",
                    "</p>",
                "" );
            }
            public void sessionRespond( PlotSession<?,?> session,
                                        HttpServletRequest request,
                                        HttpServletResponse response )
                    throws IOException {
                String servletUrl = request.getContextPath()
                                  + request.getServletPath();
                String plotTxt = session.plotTxt_;
                String html = String.join( "\n",
                    "<html>",
                    "<head>",
                    "<meta charset='UTF-8'>",
                    "<title>STILTS Plot</title>",
                    "</head>",
                    "<body>",
                    "<script>",
                    JS_TEXT,
                    "onload = function() {",
                    "   var servlet_url = '" + servletUrl + "';",
                    "   var plot_txt = '" + plotTxt + "';",
                    "   var options = {};",
                    "   options[plot2.RATE] = true;",
                    "   options[plot2.RESET] = true;",
                    "   options[plot2.HELP] = true;",
                    "   options[plot2.DOWNLOAD] = true;",
                    "   options[plot2.MSG] = false;",
                    "   var plt = plot2.createPlotNode"
                                + "(servlet_url, plot_txt, options);",
                    "   var parent = document.getElementsByTagName('div')[0];",
                    "   parent.appendChild(plt);",
                    "}",
                    "</script>",
                    "<div></div>",
                    "</body>",
                    "</html>",
                "" );
                response.setContentType( "text/html" );
                response.setStatus( 200 );
                ServletOutputStream out = response.getOutputStream();
                out.println( html );
            }
        };
    }

    /**
     * Creates a service that provides plot image data.
     *
     * @param  name  service name
     * @return  service
     */
    private static PlotService createImageService( String name ) {
        return new AbstractPlotService( name ) {
            public String getXmlDescription() {
                return String.join( "\n",
                    "<p>Returns image data suitable for reference by an",
                    "<code>IMG/@src</code> attribute value",
                    "for the current state of the session.",
                    "The session state may be updated by supplying",
                    "navigation parameters as follows:",
                    getNavigationXmlDoc(),
                    "</p>",
                    "<p>The response MIME type can be influenced by",
                    "the <code>ofmt</code> parameter in the",
                    "<code>&lt;plot-spec&gt;</code>",
                    "<em>or</em> the <code>format</code> parameter in the",
                    "<code>&lt;arg-list&gt;</code>.",
                    "In case of disagreement, the latter takes precedence.",
                    "The available options are",
                    Arrays.stream( EXPORTERS )
                          .map( s -> "\"<code>" + s + "</code>\"" )
                          .collect( Collectors.joining( ", " ) )
                    + ".",
                    "</p>",
                "" );
            }
            public void sessionRespond( PlotSession<?,?> session,
                                        HttpServletRequest request,
                                        HttpServletResponse response )
                    throws IOException {
                Map<String,String> paramMap = getSingleParameterMap( request );
                String fmtName = paramMap.get( FORMAT_KEY );
                GraphicExporter exporter = null;
                if ( fmtName != null && fmtName.trim().length() > 0 ) {
                    exporter = parseFormatName( fmtName );
                }
                if ( exporter == null ) {
                    exporter = session.exporter_;
                }
                ImageWriter imwriter =
                    session.getImageWriter( request, exporter );
                session.prepareImageResponse( response, exporter );
                long size = imwriter.getByteCount();
                if ( size > 0 && size < Integer.MAX_VALUE ) {
                    response.setContentLength( (int) size );
                }
                imwriter.writeImage( response.getOutputStream() );
            }
        };
    }

    /**
     * Creates a service that contains structured plot image information.
     *
     * @param  name  service name
     * @return  service
     */
    private static PlotService createStructureService( String name ) {
        return new AbstractPlotService( name ) {
            public String getXmlDescription() {
                return String.join( "\n",
                    "<p>Returns a JSON structure giving the image and/or",
                    "image annotations for the current state of the session.",
                    "The session state may be updated by supplying",
                    "navigation parameters, as follows:",
                    getNavigationXmlDoc(),
                    "</p>",
                    "<p>The members of the returned structure are:",
                    "<dl>",
                    "<dt><code>" + IMGSRC_KEY + "</code>",
                    "    (present if plot has changed since last request):",
                    "    </dt>",
                    "<dd>Contains a <code>data:</code> URL suitable for use",
                    "    as the content of an <code>IMG/@src</code> attribute",
                    "    to display the current state of the plot",
                    "    </dd>",
                    "<dt><code>" + STATICSVG_KEY + "</code>",
                    "    (present if there are static decorations):</dt>",
                    "<dd>SVG content, suitable as the content of",
                    "    an <code>SVG</code> node, giving decorations",
                    "    to superimpose over the plot image.",
                    "    </dd>",
                    "<dt><code>" + TRANSIENTSVG_KEY + "</code>",
                    "    (present if there are transient decorations):</dt>",
                    "<dd>SVG content, suitable as the content of",
                    "    an <code>SVG</code> node, giving decorations",
                    "    to superimpose over the plot image",
                    "    representing a navigation action.",
                    "    Such decorations should only be displayed",
                    "    for a short time (e.g. 0.5 second).",
                    "    </dd>",
                    "<dt><code>" + BOUNDS_KEY + "</code>",
                    "    (present for planar and cubic plots):</dt>",
                    "<dd>A 2- or 3-element array of (lower,upper) data bound",
                    "    pairs giving the extent of the current plot.",
                    "    </dd>",
                    "</dl>",
                    "</p>",
                "" );
            }
            public void sessionRespond( PlotSession<?,?> session,
                                        HttpServletRequest request,
                                        HttpServletResponse response )
                    throws IOException {

                /* Write image data. */
                ImageWriter imwriter =
                    session.getImageWriter( request, session.exporter_ );
                response.setContentType( "application/json" );
                response.setStatus( 200 );
                int nMember = 0;
                OutputStream out = response.getOutputStream();
                out.write( '{' );
                if ( imwriter.isChanged() ) {
                    if ( nMember++ > 0 ) {
                        out.write( ',' );
                    }
                    writeAscii( out, new StringBuffer()
                       .append( '"' ) 
                       .append( IMGSRC_KEY )
                       .append( '"' )
                       .append( ": " )
                       .append( '"' )
                       .append( "data:" )
                       .append( session.exporter_.getMimeType() )
                       .append( ";base64" )
                       .append( ',' ) );
                    writeBase64( out, imwriter );
                    out.write( '"' );
                }

                /* Write navigation decorations. */
                Decoration navdec = imwriter.getDecoration();
                if ( navdec != null ) {
                    if ( nMember++ > 0 ) {
                        out.write( ',' );
                    }
                    writeAscii( out, "\"" + TRANSIENTSVG_KEY + "\": " );
                    List<Decoration> decs = Collections.singletonList( navdec );
                    writeJsonQuotedString( out, session.decorationSvg( decs ) );
                }

                /* Write highlight markers. */
                if ( session.highlights_.size() > 0 ) {
                    List<Decoration> hidecs = new ArrayList<>();
                    Surface[] surfaces = session.scene_.getSurfaces();
                    Point2D.Double gp = new Point2D.Double();
                    for ( HighlightPosition hpos : session.highlights_ ) {
                        Surface surface = surfaces[ hpos.iz_ ];
                        if ( surface.dataToGraphics( hpos.dpos_, true, gp ) ) {
                            hidecs.add( HighlightIcon.INSTANCE
                                       .createDecoration( gp ) );
                        }
                    }
                    if ( hidecs.size() > 0 ) {
                        if ( nMember++ > 0 ) {
                            out.write( ',' );
                        }
                        writeAscii( out, "\"" + STATICSVG_KEY + "\": " );
                        writeJsonQuotedString( out,
                                               session.decorationSvg( hidecs ));
                    }
                }

                /* Write data bounds. */
                Surface[] surfaces = session.scene_.getSurfaces();
                double[][] dbounds = surfaces.length == 1
                                   ? getDataBounds( surfaces[ 0 ] )
                                   : null;
                if ( dbounds != null ) {
                    if ( nMember++ > 0 ) {
                        out.write( ',' );
                    }
                    writeAscii( out, "\"" + BOUNDS_KEY + "\": " );
                    writeAscii( out, new JSONArray( dbounds ).toString() );
                }
                out.write( '}' );
            }
        };
    }

    /**
     * Creates a service for graphics to data position conversion.
     * 
     * @param  name  service name
     * @return  service
     */
    private static PlotService createPlotPositionService( String name ) {
        return new AbstractPlotService( name ) {
            public String getXmlDescription() {
                return String.join( "\n",
                    "<p>Provides a service to convert a position in plot",
                    "graphics coordinates to data coordinates.",
                    "The request must have a <code>pos</code> argument giving",
                    "the graphics position in the form <code>x,y</code>",
                    "(e.g. \"<code>pos=203,144</code>\"),",
                    "and the response will be a JSON structure",
                    "with the following members:",
                    "<dl>",
                    "<dt><code>" + DATAPOS_KEY + "</code>:</dt>",
                    "<dd>On success, contains data coordinates",
                    "    as a numeric array.</dd>",
                    "<dt><code>" + TXTPOS_KEY + "</code>:</dt>",
                    "<dd>On success, contains data coordinates",
                    "    as a formatted string.</dd>",
                    "<dt><code>" + MESSAGE_KEY + "</code>:</dt>",
                    "<dd>Textual indication of conversion status.",
                    "    It will be \"<code>ok</code>\" on success,",
                    "    but may have some other value, such as",
                    "    \"<code>out of plot bounds</code>\", on failure.",
                    "    </dd>",
                    "</dl>",
                    "</p>",
                "" );
            }
            public void sessionRespond( PlotSession<?,?> session,
                                        HttpServletRequest request,
                                        HttpServletResponse response )
                    throws IOException {
                final String msg;
                Map<String,String> paramMap = getSingleParameterMap( request );
                Point gpos = parseXY( paramMap.get( "pos" ) );
                JSONObject json = new JSONObject();
                PlotScene<?,?> scene = session.scene_;
                Surface[] surfs = scene.getSurfaces();
                if ( gpos != null && surfs[ 0 ] != null ) {
                    int iz = scene.getGang().getNavigationZoneIndex( gpos );
                    if ( iz >= 0 ) {
                        Surface surf = surfs[ iz ];
                        if ( surf.getPlotBounds().contains( gpos ) ) {
                            Supplier<CoordSequence> cseqSupplier = null;
                            double[] dpos =
                                surf.graphicsToData( gpos, cseqSupplier );
                            if ( dpos != null ) {
                                msg = "ok";
                                String txtPos = surf.formatPosition( dpos );
                                json.put( "dataPos", dpos );
                                json.put( "txtPos", txtPos );
                            }
                            else {
                                msg = "out of data range";
                            }
                        }
                        else {
                            msg = "out of plot bounds";
                        }
                    }
                    else {
                        msg = "outside of plots";
                    }
                }
                else {
                    msg = "plot not initialised";
                }
                if ( msg != null ) {
                    json.put( MESSAGE_KEY, msg );
                }
                response.setStatus( 200 );
                response.setContentType( "application/json" );
                response.getOutputStream().println( json.toString() );
            }
        };
    }

    /**
     * Creates a service that counts visible points.
     * 
     * @param  name  service name
     * @return  service
     */
    private static PlotService createCountService( String name ) {
        return new AbstractPlotService( name ) {
            public String getXmlDescription() {
                return String.join( "\n",
                    "<p>Returns the number of points currently visible",
                    "in this plot.",
                    "Note that determining this value does take some",
                    "computation (it's not free).",
                    "The output is a plain text decimal value;",
                    "it can also be interpreted as JSON.",
                    "</p>",
                "" );
            }
            public void sessionRespond( PlotSession<?,?> session,
                                        HttpServletRequest request,
                                        HttpServletResponse response )
                    throws IOException {
                session.ensureSurfaces();
                PlotScene<?,?> scene = session.scene_;
                DataStore dataStore = session.dataStore_;
                long count = 0;
                int nz = scene.getGang().getZoneCount();
                TupleRunner tupleRunner = IS_COUNT_PARALLEL
                                        ? dataStore.getTupleRunner()
                                        : TupleRunner.SEQUENTIAL;
                for ( int iz = 0; iz < nz; iz++ ) {
                    PlotLayer[] layers = scene.getLayers( iz );
                    Surface surface = scene.getSurfaces()[ iz ];
                    SubCloud[] clouds =
                        SubCloud.createSubClouds( layers, true );
                    for ( SubCloud cloud : clouds ) {
                        DataSpec dataSpec = cloud.getDataSpec();
                        long[] acc =
                            tupleRunner
                           .collect( new PointCounter( cloud, surface ),
                                     () -> dataStore
                                          .getTupleSequence( dataSpec ) );
                        count += acc[ 0 ];
                    }
                }
                response.setStatus( 200 );
                response.setContentType( "text/plain" );
                response.getOutputStream().println( Long.toString( count ) );
            }
        };
    }

    /**
     * Creates a service that returns row data near a given graphics position.
     * 
     * @param  name  service name
     * @return  service
     */
    private static PlotService createRowService( String name ) {
        return new AbstractPlotService( name ) {
            public String getXmlDescription() {
                return String.join( "\n",
                    "<p>Services a request for row information,",
                    "at or near a submitted graphics position on the plot.",
                    "The request must have a <code>pos</code> argument giving",
                    "the graphics position in the form <code>x,y</code>",
                    "(e.g. \"<code>pos=203,144</code>\"),",
                    "</p>",
                    "<p>The output is a JSON array of objects;",
                    "there is one array element for each table represented,",
                    "and each such element is a",
                    "column-name-&gt;column-value map for the row indicated.",
                    "If the submitted point is not near any plotted points,",
                    "the result will therefore be an empty array.",
                    "</p>",
                "" );
            }
            public void sessionRespond( PlotSession<?,?> session,
                                        HttpServletRequest request,
                                        HttpServletResponse response )
                    throws IOException {
                Map<String,String> paramMap = getSingleParameterMap( request );
                Point pos = parseXY( paramMap.get( "pos" ) );
                boolean isHighlight = paramMap.containsKey( "highlight" );
                session.ensureSurfaces();
                PlotScene<?,?> scene = session.scene_;
                Set<TableRow> rowSet = new LinkedHashSet<>();
                List<HighlightPosition> hposList = new ArrayList<>();
                int iz = scene.getZoneIndex( pos );
                if ( iz >= 0 ) {
                    Surface surface = scene.getSurfaces()[ iz ];
                    PlotLayer[] layers = scene.getLayers( iz );
                    int nl = layers.length;
                    if ( surface != null && nl > 0 &&
                         surface.getPlotBounds().contains( pos ) ) {
                        IndicatedRow[] closestRows =
                            scene.findClosestRows( surface, layers, pos,
                                                   session.dataStore_ );
                        for ( int il = 0; il < nl; il++ ) {
                            IndicatedRow indRow = closestRows[ il ];
                            if ( indRow != null ) {
                                long lrow = indRow.getIndex();
                                StarTable table =
                                    layers[ il ].getDataSpec().getSourceTable();
                                boolean isNewItem =
                                    rowSet.add( new TableRow( table, lrow ) );
                                if ( isNewItem ) {
                                    double[] dpos = indRow.getDataPos();
                                    HighlightPosition hpos =
                                        new HighlightPosition( iz, dpos );
                                    hposList.add( hpos );
                                }
                            }
                        }
                    }
                }
                JSONArray jsonArray = new JSONArray();
                for ( TableRow trow : rowSet ) {
                    Map<String,Object> rowData =
                        getJsonRowData( trow.table_, trow.irow_ );
                    if ( rowData != null && rowData.size() > 0 ) {
                        jsonArray.put( rowData );
                    }
                }
                response.setStatus( 200 );
                response.setContentType( "application/json" );
                response.getOutputStream().println( jsonArray.toString() );
                if ( isHighlight ) {
                    session.highlights_ = hposList;
                }
            }
        };
    }

    /**
     * Writes an image to an output stream.
     */
    private interface ImageWriter {

        /**
         * Returns the number of bytes that will be written, if known.
         *
         * @return   number of bytes that will be written, or -1 if not known
         */
        long getByteCount();

        /**
         * Indicates whether the image has changed since the previous request.
         *
         * @return  true if the image to be written is or may be different
         *          to that at the time of the last request;
         *          false if it's known to be the same
         */
        boolean isChanged();

        /**
         * Returns the navigation decoration associated with this image update.
         *
         * @return  plot decoration or null
         */
        Decoration getDecoration();

        /**
         * Writes image data to a given output stream.
         *
         * @param  out  destination stream; will not be closed by this method
         */
        void writeImage( OutputStream out ) throws IOException;
    }

    /**
     * Utility class that aggregates a table and a row index.
     */
    private static class TableRow {
        final StarTable table_;
        final long irow_;
        TableRow( StarTable table, long irow ) {
            table_ = table;
            irow_ = irow;
        }
        @Override
        public int hashCode() {
            return table_.hashCode() + (int) ( 23 * irow_ );
        }
        @Override
        public boolean equals( Object o ) {
            if ( o instanceof TableRow ) {
                TableRow other = (TableRow) o;
                return this.table_.equals( other.table_ )
                    && this.irow_ == other.irow_;
            }
            else {
                return false;
            }
        }
        @Override
        public String toString() {
            return irow_ + ": " + table_;
        }
    }

    /**
     * Utility class aggregating a zone index and a data space position vector.
     */
    private static class HighlightPosition {
        final int iz_;
        final double[] dpos_;

        /**
         * Constructor.
         *
         * @param  iz  zone index
         * @param  dpos  position in data space
         */
        HighlightPosition( int iz, double[] dpos ) {
            iz_ = iz;
            dpos_ = dpos.clone();
        }
        @Override
        public String toString() {
            return iz_ + ": " + Arrays.toString( dpos_ );
        }
    }

    /**
     * Collector implementation that can count the points actually plotted
     * (within surface plot bounds) for a given point cloud.
     */
    private static class PointCounter
            implements SplitCollector<TupleSequence,long[]> {
        final SubCloud cloud_;
        final Surface surface_;

        /**
         * Constructor.
         *
         * @param  cloud  point cloud
         * @param  surface  plot surface
         */
        PointCounter( SubCloud cloud, Surface surface ) {
            cloud_ = cloud;
            surface_ = surface;
        }

        public long[] createAccumulator() {
            return new long[ 1 ];
        }

        public void accumulate( TupleSequence tseq, long[] acc ) {
            DataGeom geom = cloud_.getDataGeom();
            int iPosCoord = cloud_.getPosCoordIndex();
            double[] dpos = new double[ geom.getDataDimCount() ];
            Point2D.Double gp = new Point2D.Double();
            long count = 0;
            while ( tseq.next() ) {
                if ( geom.readDataPos( tseq, iPosCoord, dpos ) &&
                     surface_.dataToGraphics( dpos, true, gp ) ) {
                    count++;
                }
            }
            acc[ 0 ] += count;
        }

        public long[] combine( long[] acc1, long[] acc2 ) {
            return new long[] { acc1[ 0 ] + acc2[ 0 ] };
        }
    }

    /**
     * Aggregates a drag origin and miscellaneous information associated
     * with dragging.
     */
    private static class DragContext {
        final Point start_;
        int isurf_;
        Surface surface_;

        /**
         * Constructor.
         *
         * @param  start  drag start position
         */
        DragContext( Point start ) {
            start_ = start;
        }
    }

    /**
     * Partial plot service implementation for convenience.
     */
    private static abstract class AbstractPlotService implements PlotService {
        private final String name_;

        /**
         * Constructor.
         *
         * @param  name   service name
         */
        AbstractPlotService( String name ) {
            name_ = name;
        }

        /**
         * This implementation returns false.
         */
        public boolean canCreateSession() {
            return false;
        }

        public String getServiceName() {
            return name_;
        }
    }
}
