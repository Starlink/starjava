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
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Mapping;
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

    /* Indices at which standard Frames are expected in NDF's WCS FrameSet. */
    private static final int GRID_FRAME = 1;
    private static final int PIXEL_FRAME = 2;
    private static final int AXIS_FRAME = 3;

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.hds" );

    /**
     * Present an NdxImpl view of an existing NDF specified by an 
     * <tt>HDSReference</tt>.  The HDS file will only be closed 
     * (the last primary locator to the structure will only be annulled)
     * at such time as the object gets finalised by the garbage collector.
     *
     * @param   nref  an HDSReference pointing to the NDF
     * @param   persistentUrl  the URL referring to the NDF; if <tt>nref</tt>
     *          references a temporary file it should be <tt>null</tt>
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
     * <tt>HDSObject</tt>.
     * <p>
     * A reference is kept to the supplied HDSObject <tt>nobj</tt>,
     * but no further action is taken to ensure that a primary locator
     * to the NDF structure is retained.  Calling code should therefore
     * either set <tt>nobj</tt> itself primary (in which case it will
     * be annulled during garbage collection when this NDFNdxImpl is no 
     * longer referenced), or retain a suitable primary locator for as
     * long as this NDFNdxImpl will be used.
     *
     * @param   nobj  the HDSObject where the NDF lives
     * @param   persistentUrl  the URL referring to <tt>nobj</tt>, 
     *          or <tt>null</tt> if it does not represent a permanent address
     * @param   mode    the read/write/update mode for array data access.
     *          <tt>nobj</tt> itself must have been opened with a
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

    public boolean hasWCS() {
        return AstPackage.isAvailable() && wcsArray() != null;
    }

    public Object getWCS() {
        try {

            /* Get an AST Channel from the WCS component. */
            Channel chan = new ARYReadChannel( wcsArray() );

            /* Read a FrameSet from it. */
            FrameSet fset = (FrameSet) chan.read();

            /* This will require some doctoring, since its PIXEL and AXIS 
             * Frames are not trustworthy (the Fortran NDF library would 
             * ignore these and regenerate them during a call to NDF_GTWCS).
             * First check that it looks as expected. */
            if ( fset.getFrame( GRID_FRAME ).getDomain().equals( "GRID" ) &&
                 fset.getFrame( PIXEL_FRAME ).getDomain().equals( "PIXEL" ) &&
                 fset.getFrame( AXIS_FRAME ).getDomain().equals( "AXIS" ) ) {

                /* Get and check the shape of the image grid. */
                NDShape shape = image.getShape();
                if ( fset.getFrame( 1 ).getNaxes() != shape.getNumDims() ) {
                    logger.warning( "Wrong shaped WCS object in NDF" );
                    return null;
                }

                /* Remap a PIXEL Frame correctly using the GRID Frame
                 * and the origin offset. */
                int ndim = shape.getNumDims();
                double[] ina = new double[ ndim ];
                double[] inb = new double[ ndim ];
                double[] outa = new double[ ndim ];
                double[] outb = new double[ ndim ];
                long[] origin = shape.getOrigin();
                for ( int i = 0; i < ndim; i++ ) {
                    ina[ i ] = 0.0;
                    inb[ i ] = 1.0;
                    outa[ i ] = ina[ i ] + origin[ i ] - 1.5;
                    outb[ i ] = inb[ i ] + origin[ i ] - 1.5;
                }
                Mapping pmap =
                    new CmpMap( fset.getMapping( PIXEL_FRAME, GRID_FRAME ),
                                new WinMap( ndim, ina, inb, outa, outb ), true )
                   .simplify();
                fset.remapFrame( PIXEL_FRAME, pmap );

                /* The AXIS Frame will probably either be identical to the 
                 * PIXEL Frame or wrong, so just delete it.  
                 * TODO: Should really write code to construct a correct AXIS
                 * Frame from any existing AXIS component in the NDF. */
                fset.removeFrame( AXIS_FRAME );
            }

            /* Unexpected configuration of frameset read from WCS component. */
            else {
                logger.warning( "Unexpected Frame configuration in read WCS" );
            }

            /* Return the doctored or undoctored FrameSet. */
            return fset;
        }

        /* Treat errors by logging an error and returning null. */
        catch ( HDSException e ) {
            logger.warning( "Trouble reading WCS FrameSet from NDF: " + e );
            return null;
        }
        catch ( IOException e ) {
            logger.warning( "Trouble reading WCS FrameSet from NDF: " + e );
            return null;
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
