package uk.ac.starlink.hds;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import uk.ac.starlink.ast.Channel;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ndx.ArraysBulkDataImpl;
import uk.ac.starlink.ndx.BulkDataImpl;
import uk.ac.starlink.ndx.NdxImpl;

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

    /**
     * Present an NdxImpl view of an existing NDF.
     */
    public NDFNdxImpl( HDSReference nref, URL persistentUrl, AccessMode mode )
            throws HDSException {
        this.persistentUrl = persistentUrl;
        this.ndf = nref.getObject( HDSReference.hdsMode( mode ) );
        this.mode = mode;
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
        return wcsArray() != null;
    }

    public Object getWCS() {
        try {
            Channel chan = new ARYReadChannel( wcsArray() );
            FrameSet fset = (FrameSet) chan.read();
            return fset;
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }
        catch ( IOException e ) {
            throw new RuntimeException( e );
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

    public byte getBadBits() {
        try {
            if ( ndf.datThere( "QUALITY" ) ) {
                HDSObject qcomp = ndf.datFind( "QUALITY" );
                if ( qcomp.datThere( "BADBITS" ) ) {
                    HDSObject bbobj = qcomp.datFind( "BADBITS" );
                    if ( ! bbobj.datStruc() &&
                        bbobj.datType().equals( "_UBYTE" ) &&
                        bbobj.datShape().length == 0 ) {
                        int bbval = bbobj.datGet0i();
                        return (byte) bbval;
                    }
                }
            }
            return (byte) 0;
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }
    }

    public BulkDataImpl getBulkData() {
        NDArray inda = null;
        NDArray vnda = null;
        NDArray qnda = null;

        /* In the below we subvert the HDSReference class a bit; we need to
         * use its string handling properties on URLs which are not file:
         * protocol ones, which it wouldn't allow.  So pretend they are
         * file: ones for as long as HDSReference sees them. */ 
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

        try {
            String iname = "DATA_ARRAY";
            if ( ndf.datThere( iname ) ) {
                HDSReference iref = (HDSReference) baseRef.clone();
                iref.push( iname );
                HDSObject iobj = ndf.datFind( iname );
                ArrayStructure iary = new ArrayStructure( iobj );
                URL iurl = null;
                if ( baseRef != null ) {
                    try {
                        iurl = new URL( context, iref.getURL().toString().replaceFirst( "^file://localhost", "" ) );
                    }
                    catch ( MalformedURLException e ) {
                        throw new RuntimeException( e.getMessage(), e );
                    }
                }
                inda = new BridgeNDArray( new HDSArrayImpl( iary, mode ),
                                          iurl );
            }

            String vname = "VARIANCE";
            if ( ndf.datThere( vname ) ) {
                HDSReference vref = (HDSReference) baseRef.clone();
                vref.push( vname );
                HDSObject vobj = ndf.datFind( vname );
                ArrayStructure vary = new ArrayStructure( vobj );
                URL vurl = null;
                if ( baseRef != null ) {
                    try {
                        vurl = new URL( context, vref.getURL().toString().replaceFirst( "file://localhost", "" ) );
                    }
                    catch ( MalformedURLException e ) {
                        throw new RuntimeException( e.getMessage(), e );
                    }
                }
                vnda = new BridgeNDArray( new HDSArrayImpl( vary, mode ),
                                          vurl );
            }

            String qname = "QUALITY";
            if ( ndf.datThere( qname ) ) {
                HDSReference qref = (HDSReference) baseRef.clone();
                qref.push( qname );
                HDSObject qobj = ndf.datFind( qname );
                if ( qobj.datType().equals( "QUALITY" ) ) {
                    String qsubname = "QUALITY";
                    qref.push( qsubname );
                    qobj = qobj.datFind( qsubname );
                }
                ArrayStructure qary = new ArrayStructure( qobj );
                URL qurl = null;
                if ( baseRef != null ) {
                    try {
                        qurl = new URL( context, qref.getURL().toString().replaceFirst( "^file://localhost", "" ) );
                    }
                    catch ( MalformedURLException e ) {
                        throw new RuntimeException( e.getMessage(), e );
                    }
                }
                qnda = new BridgeNDArray( new HDSArrayImpl( qary, mode ),
                                          qurl );
            }
        }
        catch ( HDSException e ) {
            throw new RuntimeException( e );
        }

        return new ArraysBulkDataImpl( inda, vnda, qnda );
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
