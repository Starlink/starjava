package uk.ac.starlink.hdx;

import java.io.IOException;
import java.net.URL;

import org.w3c.dom.Element;

import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.Channel;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.WinMap;
import uk.ac.starlink.ast.xml.XAstWriter;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.HDSReference;
import uk.ac.starlink.hdx.array.NDArray;
import uk.ac.starlink.hdx.array.NDArrayFactory;
import uk.ac.starlink.hdx.array.NDShape;


/**
 * An NdxImpl implementation which uses HDS as the data storage medium.
 *
 * @author Mark Taylor
 * @author Peter W. Draper
 */
class HdsNdxImpl implements NdxImpl {

    private HDSObject ndf;   // Keep this - it's a primary HDS object
    private HDSReference ndfref;
    private NDArray image;
    private NDArray variance;
    private NDArray quality;
    private boolean hasVariance;
    private boolean hasQuality;
    private String title;
    private byte badbits;
    private Element wcsElement = null;

    /**
     * Constructs an NdxImpl from a URL pointing to an HDS object.
     *
     * @param   url   a URL pointing to an HDS object.  The format of
     *                this URL is documented in the 
     *                {@link uk.ac.starlink.hds.HDSReference} class
     * @param   mode  an access mode string used to open the HDS object 
     *                for access - one of "READ", "WRITE", "UPDATE"
     * @throws  HdxException  if the URL does not refer to a suitable HDS object
     */
    HdsNdxImpl( URL url, String mode ) throws HdxException {
        try {
            ndfref = new HDSReference( url );
            HDSObject hobj = ndfref.getObject( mode );
            initialiseFromHDS( hobj, url.toString() );
        }
        catch ( HDSException e ) {
            throw (HdxException) new HdxException( "No NDX at " + url )
                                .initCause( e );
        }
    }

    /**
     * Constructs an NdxImpl from an HDSReference.
     *
     * @param   href   an HDSReference pointing to an NDF structure
     * @param   mode  an access mode string used to open the HDS object 
     *                for access - one of "READ", "WRITE", "UPDATE"
     * @throws  HdxException if there is a problem turning href into an NDX
     */
    HdsNdxImpl( HDSReference href, String mode ) throws HdxException {
        this.ndfref = href;
        try {
            HDSObject hobj = ndfref.getObject( mode );
            initialiseFromHDS( hobj, ndfref.toString() );
        }
        catch ( HDSException e ) {
            throw (HdxException) new HdxException( "No NDX at " + ndfref )
                                .initCause( e );
        }
    }

    /**
     * Constructs an NdxImpl from an HDSObject.
     *
     * @param   hobj  an HDSObject containing an NDF structure
     * @throws  HdxException  if there is a problem turning hobj into an NDX
     */
    HdsNdxImpl( HDSObject hobj ) throws HdxException {
        try {
            this.ndfref = new HDSReference( hobj );
        }
        catch ( HDSException e ) {
            throw (HdxException) new HdxException( "Can't get HDSReference" )
                                .initCause( e );
        }
        initialiseFromHDS( hobj, null );
    }

    /**
     * Performs necessary initialisation on the object given an HDSObject
     * at which the NDF resides.  This method to be called only and always
     * by constructors.
     *
     * @param   ndf   an HDSObject containing an NDF structure
     * @param   source  a string used to identify where ndf came from,
     *                  used only in constructing exception messages.
     *                  May be null.
     */
    private void initialiseFromHDS( HDSObject ndf, String source ) 
            throws HdxException {

        try {
            if ( ! ndf.datThere( "DATA_ARRAY" ) ) {
                throw new HdxException( "No DATA_ARRAY at " + source );
            }
            if ( ndf.datThere( "TITLE" ) ) {
                title = ndf.datFind( "TITLE" ).datGet0c();
            }
 
            hasVariance = ndf.datThere( "VARIANCE" );
            hasQuality = ndf.datThere( "QUALITY" );
            if ( hasQuality ) {
                HDSObject qobj = ndf.datFind( "QUALITY" );
                badbits = qobj.datThere( "BADBITS" ) 
                              ? (byte) qobj.datFind( "BADBITS" ).datGet0i()
                              : (byte) 0xff;
            }
        }
        catch ( HDSException e ) {
            String src = ( source == null ) ? ( " from " + source ) : "";
            throw (HdxException) 
                  new HdxException( "Cannot construct NDX" + source )
                 .initCause( e );
        }
    }

    public NDArray getImage() {
        if ( image == null ) {
            image = getArrayComponent( "DATA_ARRAY" );
        }
        return image;
    }

    public NDArray getVariance() {
        if ( variance == null && hasVariance ) {
            variance = getArrayComponent( "VARIANCE" );
        }
        return variance;
    }

    public NDArray getQuality() {
        if ( quality == null && hasQuality ) {
            quality = getArrayComponent( "QUALITY" );
        }
        return quality;
    }

    public boolean hasImage() {
        return true;
    }
    public boolean hasVariance() {
        return hasVariance;
    }
    public boolean hasQuality() {
        return hasQuality;
    }
    public String getTitle() {
        return title;
    }
    public byte getBadBits() {
        return badbits;
    }

    private NDArray getArrayComponent( String title ) {
        HDSReference ref = (HDSReference) ndfref.clone();
        ref.push( title );
        NDArray array;
        try {
            URL url = ref.getURL();
            array = NDArrayFactory.makeReadableNDArray( url );
        }
        catch ( IOException e ) {
            throw (RuntimeException) new RuntimeException()
                                    .initCause( e );
        }
        return array;
    }

    public boolean hasWCS() 
    {
        //  NDF always has a WCS of somekind.
        return true;
    }

    public Element getWCSElement()
    {
        if ( wcsElement == null ) {
            try {
                wcsElement = createWCSElement();
            }
            catch (HdxException e) {
                return null;
            }
        }
        return wcsElement;
    }

    /**
     * Create an Element that contains a description of the NDF WCS.
     */
    protected Element createWCSElement() throws HdxException
    {
        HDSObject wcsComponent = getWCSComponent();
        if ( wcsComponent == null ) {
            return generateDefaultWCSElement();
        }
        return FrameSetToElement( WCSComponentToFrameSet( wcsComponent ) );
    }

    
    /**
     * Locate and return the WCS component.
     */
    protected HDSObject getWCSComponent()
    {
        try {

            HDSReference ref = (HDSReference) ndfref.clone();

            ref.push( "WCS" );
            return ref.getObject( "READ" );
        }
        catch (HDSException e) {
            return null;
        }
    }


    /**
     * Create a default WCS, returning the result in an Element.
     */
    protected Element generateDefaultWCSElement()
    {
        //  Default system is PIXEL and GRID (forget AXIS). 
        //  Just need origin to define the necessary WinMap.
        NDShape shape = getImage().getShape();
        long[] origin = shape.getOrigin();

        Frame gridFrame = new Frame( origin.length );
        gridFrame.setDomain( "GRID" );
        FrameSet frameSet = new FrameSet( gridFrame );

        Frame pixelFrame = new Frame( origin.length );
        gridFrame.setDomain( "PIXEL" );

        double[] ina = new double[ origin.length ];
        double[] inb = new double[ origin.length ];
        double[] outa = new double[ origin.length ];
        double[] outb = new double[ origin.length ];
        for ( int i = 0; i < origin.length; i++ ) {
            ina[i] = 0.5;
            inb[i] = 1.5;
            outa[i] = (double) origin[i] - 1.0;
            outb[i] = (double) origin[i];
        }
        WinMap map = new WinMap( origin.length, ina, inb, outa, outb );
        frameSet.addFrame( FrameSet.AST__BASE, map, pixelFrame );

        return FrameSetToElement( frameSet );
    }

    /**
     * Convert an AST FrameSet into an wcs Element.
     */
    protected Element FrameSetToElement( FrameSet frameSet )
    {
        XAstWriter writer = new XAstWriter();
        
        // A <wcs> element should contain the FrameSet so we work harder.
        return writer.makeElement( frameSet, null );
    }
    
    /**
     * Convert a WCS component into an AstFrameSet.
     */
    protected FrameSet WCSComponentToFrameSet( HDSObject wcs )
        throws HdxException 
    {
        //  Pinched code from treeview.
        try {
            if ( wcs.datStruc() && wcs.datShape().length == 0 ) {
                HDSObject data = wcs.datFind( "DATA" );
                if ( data.datType().startsWith( "_CHAR" ) &&
                     data.datShape().length == 1 ) {
                    Channel chan = new HdsWCSChannel( data );
                    FrameSet fs;
                    try {
                        fs = (FrameSet) chan.read();
                    }
                    catch ( ClassCastException e ) {
                        throw new HdxException( 
                           "Object read from channel is not an AST FrameSet" );
                    }
                    catch ( IOException e ) {
                        throw (HdxException) 
                              new HdxException( "Trouble reading from channel" )
                             .initCause( e );
                    }
                    catch ( AstException e ) {
                        throw (HdxException)
                              new HdxException( "Trouble reading from channel" )
                             .initCause( e );
                    }
                    if ( fs == null ) {
                        throw new HdxException( 
                            "No object read from AST channel" );
                    }
                    return fs;
                }
                else {
                    throw new HdxException( 
                        "HDSObject is not a 1-D _CHAR array" ); 
                }
            }
            else {
                throw new HdxException(
                        "WCS component has no DATA component" );
            }
        }
        catch ( HDSException e ) {
            throw (HdxException) new HdxException( e.getMessage() )
                                .initCause( e ); 
        }
    }

    /**
     * Convert a dom4j Element into a w3c one. Returns null if fails.
     */
    protected Element dom4jElementToW3c( org.dom4j.Element wcs )
    {
        //  Somewhat nastily this can only be done via a complete
        //  dom4j Document. TODO: refactor this away or extract into a
        //  utility class.
        org.dom4j.io.DOMWriter writer = new org.dom4j.io.DOMWriter();
        org.w3c.dom.Document domDoc = null;
        org.dom4j.Document dom4jDoc = null;
        org.dom4j.DocumentFactory factory = 
            org.dom4j.DocumentFactory.getInstance();
        dom4jDoc = factory.createDocument( wcs );
        try {
            domDoc = writer.write( dom4jDoc );
            return domDoc.getDocumentElement();
        } 
        catch ( org.dom4j.DocumentException e ) {
            e.printStackTrace();
            return null;
        }
    }
}
