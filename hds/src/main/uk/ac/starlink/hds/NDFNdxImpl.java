package uk.ac.starlink.hds;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.ast.AstPackage;
import uk.ac.starlink.ast.Channel;
import uk.ac.starlink.ast.CmpMap;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.LutMap;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.ShiftMap;
import uk.ac.starlink.ast.UnitMap;
import uk.ac.starlink.ast.WinMap;
import uk.ac.starlink.ndx.NdxImpl;
import uk.ac.starlink.util.URLUtils;

/**
 * Implementation of the NdxImpl interface based on an NDF.
 * Since the NDF and NDX data models are pretty similar, the mapping of
 * data items between the two forms is quite straightforward.
 *
 * @author   Mark Taylor (Starlink)
 */
class NDFNdxImpl implements NdxImpl {

    private final URL persistentUrl;
    private final HDSObject ndf;
    private final AccessMode mode;
    private NDArray image;
    private NDArray variance;
    private NDArray quality;

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.hds" );

    /**
     * Present an NdxImpl view of an existing NDF specified by an 
     * <code>HDSReference</code>.  The HDS file will only be closed 
     * (the last primary locator to the structure will only be annulled)
     * at such time as the object gets finalised by the garbage collector.
     *
     * @param   nref  an HDSReference pointing to the NDF
     * @param   persistentUrl  the URL referring to the NDF;
     *          if <code>nref</code>
     *          references a temporary file it should be <code>null</code>
     * @param   mode  the read/write/update mode for the array data access
     * @throws  HDSException  if an HDS error occurs, or this doesn't 
     *          look like an NDF
     */
    public NDFNdxImpl( HDSReference nref, URL persistentUrl, AccessMode mode )
            throws HDSException {
        this( nref.getObject( HDSReference.hdsMode( mode ) ), persistentUrl,
              mode );
    }

    /**
     * Present an NdxImpl view of an existing NDF specified by an
     * <code>HDSObject</code>.
     * <p>
     * A reference is kept to the supplied HDSObject <code>nobj</code>,
     * but no further action is taken to ensure that a primary locator
     * to the NDF structure is retained.  Calling code should therefore
     * either set <code>nobj</code> itself primary (in which case it will
     * be annulled during garbage collection when this NDFNdxImpl is no 
     * longer referenced), or retain a suitable primary locator for as
     * long as this NDFNdxImpl will be used.
     *
     * @param   nobj  the HDSObject where the NDF lives
     * @param   persistentUrl  the URL referring to <code>nobj</code>, 
     *          or <code>null</code> if it does not represent
     *          a permanent address
     * @param   mode    the read/write/update mode for array data access.
     *          <code>nobj</code> itself must have been opened with a
     *          compatible access mode
     * @throws  HDSException  if an HDS error occurs, or this doesn't 
     *          look like an NDF
     */
    public NDFNdxImpl( HDSObject nobj, URL persistentUrl, AccessMode mode )
            throws HDSException {
        this.ndf = nobj;
        this.persistentUrl = persistentUrl;
        this.mode = mode;
        NDArray[] arrays = makeArrayData( ndf, persistentUrl, mode );
        this.image = arrays[ 0 ];
        this.variance = arrays[ 1 ];
        this.quality = arrays[ 2 ];
    }

    public boolean hasTitle() {
        try {
            return ndf.datThere( "TITLE" );
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }
    }

    public String getTitle() {
        try {
            return ndf.datFind( "TITLE" ).datGet0c();
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }
    }

    public boolean hasLabel() {
        try {
            return ndf.datThere( "LABEL" );
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }
    }

    public String getLabel() {
        try {
            return ndf.datFind( "LABEL" ).datGet0c();
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }
    }

    public boolean hasUnits() {
        try {
            return ndf.datThere( "UNITS" );
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }
    }

    public String getUnits() {
        try {
            return ndf.datFind( "UNITS" ).datGet0c();
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }
    }

    public boolean hasWCS() {
        return AstPackage.isAvailable() 
            && ( wcsArray() != null || axisArray() != null );
    }

    public Object getWCS() {

        /* Get the number of dimensions. */
        NDShape shape = image.getShape();
        int ndim = shape.getNumDims();

        /* Create and configure the three default frames. */
        Frame gridFrame = new Frame( ndim );
        Frame pixelFrame = new Frame( ndim );
        Frame axisFrame = new Frame( ndim );
        gridFrame.setDomain( "GRID" );
        pixelFrame.setDomain( "PIXEL" );
        axisFrame.setDomain( "AXIS" );
        for ( int i = 0; i < ndim; i++ ) {
            int ifrm = i + 1;
            gridFrame.setLabel( ifrm, "GRID" + ifrm );
            pixelFrame.setLabel( ifrm, "PIXEL" + ifrm );
            axisFrame.setLabel( ifrm, "AXIS" + ifrm );
        }

        /* Work out the mapping between GRID and PIXEL frames. */
        long[] origin = shape.getOrigin();
        double[] pixShift = new double[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            pixShift[ i ] = origin[ i ] - 1.5;
        }

        /* Stick these frames together into a basic FrameSet using the
         * standard mappings. */
        FrameSet fset = new FrameSet( gridFrame );
        int jGridFrm = fset.getNframe();  // 1
        fset.addFrame( jGridFrm, new ShiftMap( pixShift ), pixelFrame );
        int jPixelFrm = fset.getNframe(); // 2
        fset.addFrame( jPixelFrm, new UnitMap( ndim ), axisFrame );
        int jAxisFrm = fset.getNframe(); // 3
        FrameSet basicFset = (FrameSet) fset.copy();

        /* If there is an AXIS component, use the information in it to 
         * doctor and remap the AXIS frame. */
        try {
            HDSObject axes = axisArray();
            if ( axes != null ) {
                Mapping amap = null;
                for ( int i = 0; i < ndim; i++ ) {
                    int ifrm = i + 1;
                    HDSObject axobj = axes.datCell( new long[] { ifrm } );
 
                    /* Use the AXIS component label and units descriptions if
                     * present. */
                    if ( axobj.datThere( "UNITS" ) ) {
                        axisFrame.setUnit( ifrm, axobj.datFind( "UNITS" )
                                                      .datGet0c() );
                    }
                    if ( axobj.datThere( "LABEL" ) ) {
                        axisFrame.setLabel( ifrm, axobj.datFind( "LABEL" )
                                                       .datGet0c() );
                    }

                    /* Accumulate LutMaps from the AXIS elements one dimension
                     * at a time into a map which describes the relationship
                     * between the GRID and AXIS frame. */
                    ArrayStructure axary = 
                        new ArrayStructure( axobj.datFind( "DATA_ARRAY" ) );
                    HDSObject axdat = axary.getData();
                    double[] lut = axdat.datGetvd();
                    Mapping map1 = lut.length > 1 
                                 ? (Mapping) new LutMap( lut, 1.0, 1.0 )
                                 : (Mapping) new ShiftMap( lut );
                    amap = i == 0 ? map1
                                  : (Mapping) new CmpMap( amap, map1, false );
                }

                /* Now use the map we have constructed to change the 
                 * relationship between the GRID and AXIS frames. */
                assert fset.getFrame( 3 ).getDomain().equals( "AXIS" );
                fset.remapFrame( 3, amap.simplify() );
            }

            /* Now if there is a WCS component in the NDF, augment our basic
             * frameset with any additional frames it contains. */
            HDSObject wcsa = wcsArray();
            if ( wcsa != null ) {

                /* Get an AST Channel from the WCS component. */
                Channel chan = new ARYReadChannel( wcsArray() );

                /* Read a FrameSet from it. */
                FrameSet wcs = (FrameSet) chan.read();

                /* Only proceed if it looks like what we expect and the read
                 * frameset contains at least one non-default frame. */
                if ( wcs != null &&
                     wcs.getFrame( 1 ).getNaxes() == ndim &&
                     wcs.getFrame( 1 ).getDomain().equals( "GRID" ) &&
                     wcs.getFrame( 2 ).getDomain().equals( "PIXEL" ) &&
                     wcs.getFrame( 3 ).getDomain().equals( "AXIS" ) ) {
                    if ( wcs.getNframe() > 3 ) {

                        /* Record the position of the current frame. */
                        int jCurrent = wcs.getCurrent();

                        /* Add a copy of the GRID frame, which we will use
                         * as the link frame when merging. */
                        Frame gridCopy = (Frame) wcs.getFrame( 1 ).copy();
                        gridCopy.setDomain( "DUMMY" );
                        wcs.addFrame( 1, new UnitMap( ndim ), gridCopy );
                        int jDummy = wcs.getNframe();

                        /* Remove the default frames from the WCS frameset
                         * we are about to add, since we already have 
                         * (more reliable) versions of those. */
                        wcs.removeFrame( 3 );
                        wcs.removeFrame( 2 );
                        wcs.removeFrame( 1 );

                        /* Merge the two framesets, joining them using the 
                         * GRID frame in the one we've constructed and the
                         * copy of the GRID frame of the one we read. */
                        fset.addFrame( 1, new UnitMap( ndim ), wcs );

                        /* Discard the grid copy frame, which we no longer
                         * need. */
                        assert fset.getFrame( jDummy ).getDomain()
                                                      .equals( "DUMMY" );
                        fset.removeFrame( jDummy );

                        /* Restore the current frame index. */
                        fset.setCurrent( jCurrent );
                    }
                }
                else {
                    logger.warning( "Ignoring funny-looking WCS component" );
                }
            }

            /* Return the doctored or undoctored FrameSet. */
            return fset;
        }

        /* Treat errors by logging an error and returning the basic frameset. */
        catch ( HDSException e ) {
            logger.warning( "Trouble reading WCS FrameSet from NDF: " + e );
            return basicFset;
        }
        catch ( IOException e ) {
            logger.warning( "Trouble reading WCS FrameSet from NDF: " + e );
            return basicFset;
        }
    }

    public boolean hasEtc() {
        try {
            return moreObject() != null;
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }
    }

    public Source getEtc() {
        try {
            HDSObject more = moreObject();
            Document doc = DocumentBuilderFactory
                          .newInstance()
                          .newDocumentBuilder()
                          .newDocument();
            Node etc = doc.createElement( "etc" );
            int ncomp = more.datNcomp();
            for ( int i = 0; i < ncomp; i++ ) {
                HDSObject extobj = more.datIndex( i + 1 );
                String name = extobj.datName();
                String type = extobj.datType();
                Element extel = doc.createElement( name );
                extel.setAttribute( "type", type );
                Node content = doc.createComment( 
                    "Extension content not yet implemented" );
                extel.appendChild( content );
                etc.appendChild( extel );
            }
            return new DOMSource( etc );
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }
        catch ( ParserConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

    public int getBadBits() {
        try {
            if ( ndf.datThere( "QUALITY" ) ) {
                HDSObject qcomp = ndf.datFind( "QUALITY" );
                if ( qcomp.datThere( "BADBITS" ) ) {
                    HDSObject bbobj = qcomp.datFind( "BADBITS" );
                    if ( ! bbobj.datStruc() &&
                        ( bbobj.datType().equals( "_UBYTE" ) ||
                          bbobj.datType().equals( "_BYTE" ) ) &&
                        bbobj.datShape().length == 0 ) {
                        int bbval = (byte) bbobj.datGet0i();
                        return bbval;
                    }
                }
            }
            return 0;
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }
    }

    public NDArray getImage() {
        return image;
    }

    public boolean hasVariance() {
        return variance != null;
    }
    public NDArray getVariance() {
        return variance;
    }

    public boolean hasQuality() {
        return quality != null;
    }
    public NDArray getQuality() {
        return quality;
    }

    /**
     * Constructs the array objects serving a given NDF.
     *
     * @param   ndf  the HDSObject where the NDF lives
     * @param   persistentUrl  the URL pointing to the NDF; if it resides
     *          in a temporary file this should be null
     * @param   mode  the read/write/update access mode for array access
     * @return  a 3-element array of NDArrays containing image, variance,
     *          quality (second and/or third element may be null)
     * @throws  HDSException  if there is an HDS error
     * @throws  IllegalArgumentException  if it doesn't look like an NDF
     */
    private static NDArray[] makeArrayData( HDSObject ndf, URL persistentUrl, 
                                            AccessMode mode ) 
            throws HDSException {
        NDArray inda = null;
        NDArray vnda = null;
        NDArray qnda = null;

        /* Check we are being asked about a structure. */
        if ( ! ndf.datStruc() ) {
            throw new IllegalArgumentException( "Not a structure" );
        }

        /* In the below we subvert the HDSReference class a bit; we need to
         * use its string handling properties on URLs which are not file:
         * protocol ones, which it wouldn't allow.  So pretend they are
         * file: ones for as long as HDSReference sees them.
         * This may point up a deficiency in the HDSReference class. */
        String proto = null;
        HDSReference baseRef = null;
        URL context = null;
        if ( persistentUrl != null ) {
            proto = persistentUrl.getProtocol();
            try {
                String frag = persistentUrl.getRef();
                URL baseUrl = new URL( "file:" + persistentUrl.getFile() + 
                                       ( frag != null ? ( "#" + frag ) : "" ) );
                baseRef = new HDSReference( baseUrl );
                context = new URL( persistentUrl.getProtocol(),
                                   persistentUrl.getHost(),
                                   persistentUrl.getPort(),
                                   persistentUrl.getFile() );
            }
            catch ( MalformedURLException e ) {
                throw new RuntimeException( e.getMessage(), e );
            }
        }

        String iname = "DATA_ARRAY";
        if ( ndf.datThere( iname ) ) {
            HDSObject iobj = ndf.datFind( iname );
            ArrayStructure iary = new ArrayStructure( iobj );
            URL iurl = null;
            if ( baseRef != null ) {
                HDSReference iref = (HDSReference) baseRef.clone();
                iref.push( iname );
                iurl = URLUtils.makeURL( context.toString(), 
                                         getLocation( iref ) );
            }
            inda = new BridgeNDArray( new HDSArrayImpl( iary, mode ), iurl );
        }
        else {
            throw new IllegalArgumentException( 
                "No DATA_ARRAY component - not an NDF" );
        }

        String vname = "VARIANCE";
        if ( ndf.datThere( vname ) ) {
            HDSObject vobj = ndf.datFind( vname );
            ArrayStructure vary = new ArrayStructure( vobj );
            URL vurl = null;
            if ( baseRef != null ) {
                HDSReference vref = (HDSReference) baseRef.clone();
                vref.push( vname );
                vurl = URLUtils.makeURL( context.toString(), 
                                         getLocation( vref ) );
            }
            vnda = new BridgeNDArray( new HDSArrayImpl( vary, mode ), vurl );
        }

        String qname = "QUALITY";
        if ( ndf.datThere( qname ) ) {
            HDSObject qobj = ndf.datFind( qname );

            URL qurl = null;
            HDSReference qref = null;
            if ( baseRef != null ) {
                qref = (HDSReference) baseRef.clone();
                qref.push( qname );
                qurl = URLUtils.makeURL( context.toString(),
                                         getLocation( qref ) );
            }

            if ( qobj.datType().equals( "QUALITY" ) ) {
                String qsubname = "QUALITY";
                if ( qref != null ) {
                    qref.push( qsubname );
                    qurl = URLUtils.makeURL( context.toString(),
                                             getLocation( qref ) );
                }
                qobj = qobj.datFind( qsubname );
            }
            ArrayStructure qary = new ArrayStructure( qobj );

            qnda = new BridgeNDArray( new HDSArrayImpl( qary, mode ), qurl );
        }
 
        return new NDArray[] { inda, vnda, qnda };
    }
 

    /**
     * Return the location part of a HDSReference as a String.
     */
    protected static String getLocation( HDSReference ref ) {
        
        // Clearly just returning the filename and optional fragment
        // isn't a full URL reconstruction...
        URL url = ref.getURL();
        String frag = url.getRef();
        if ( frag != null ) {
            return url.getPath() + "#" + frag;
        }
        return url.getPath();
    }

    /**
     * Returns the HDS character array object containing the channelised
     * WCS FrameSet representation.  If none exists, returns null.
     */
    private HDSObject wcsArray() {
        try {
            if ( ndf.datThere( "WCS" ) ) {
                HDSObject wobj = ndf.datFind( "WCS" );
                if ( wobj.datThere( "DATA" ) ) {
                    HDSObject warr = wobj.datFind( "DATA" );
                    if ( ! warr.datStruc() &&
                         warr.datType().startsWith( "_CHAR" ) &&
                         warr.datShape().length == 1 ) {
                        return warr;
                    }
                }
            }
            return null;
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Returns the AXIS structure array representing axis information in 
     * the NDF.  If none exists, return null.
     *
     * @return  HDSObject which is a 1-d array of structures 
     *          each representing an NDF axis
     */
    private HDSObject axisArray() {
        try {
            if ( ndf.datThere( "AXIS" ) ) {
                HDSObject aobj = ndf.datFind( "AXIS" );
                long[] ashape = aobj.datShape();
                if ( aobj.datStruc() && ashape.length == 1 ) {
                    return aobj;
                }
                else {
                    logger.warning( "Ignoring strangely-formed " 
                                  + "AXIS component" );
                    return null;
                }
            }
            else {
                return null;
            }
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Returns the .MORE extension component.  If none exists, returns null.
     */
    private HDSObject moreObject() throws HDSException {
        if ( ndf.datThere( "MORE" ) ) {
            HDSObject more = ndf.datFind( "MORE" );
            if ( more.datStruc() ) {
                return more;
            }
        }
        return null;
    }

}
