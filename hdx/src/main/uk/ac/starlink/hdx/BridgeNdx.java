package uk.ac.starlink.hdx;

import java.io.IOException;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import uk.ac.starlink.hdx.array.DefaultAccess;
import uk.ac.starlink.hdx.array.Requirements;
import uk.ac.starlink.hdx.array.NDArray;
import uk.ac.starlink.hdx.array.NdxAccess;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.xml.XAstReader;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;

import java.io.StringWriter;
import java.io.StringReader;

/**
 * Default Ndx implementation.
 */
class BridgeNdx implements Ndx {

    private final NdxImpl impl;

    BridgeNdx( NdxImpl impl ) {
        this.impl = impl;
    }

    public NdxAccess getAccess( Requirements req, boolean wantVariance,
                                boolean wantQuality ) throws IOException {
        return new DefaultAccess( impl.getImage(), impl.getVariance(),
                                  impl.getQuality(), impl.getBadBits(),
                                  req, wantVariance, wantQuality );
    }

    public byte getBadBits() {
        return impl.getBadBits();
    }

    public NDArray getImage() {
        return impl.getImage();
    }
    public NDArray getVariance() {
        return impl.getVariance();
    }
    public NDArray getQuality() {
        return impl.getQuality();
    }
    public String getTitle() {
        return impl.getTitle();
    }

    public boolean hasImage() {
        return impl.hasImage();
    }
    public boolean hasVariance() {
        return impl.hasVariance();
    }
    public boolean hasQuality() {
        return impl.hasQuality();
    }

    public boolean hasWCS() {
        return impl.hasWCS();
    }

    public FrameSet getWCS() throws HdxException
    {
        //  Assume implementation returns an Element that contains the
        //  FrameSet as an XML description.
        try {
            XAstReader xr = new XAstReader();

            //  Note namespace prefix is null as HDX should have
            //  transformed it, also the parent node is <wcs> and Ast
            //  just wants the content.
            NodeList nodes = impl.getWCSElement().getElementsByTagName( "*" );
            if ( nodes.getLength() > 0 ) {
                return (FrameSet) xr.makeAst( (Element)nodes.item( 0 ), null );
            }
            else {
                return null;
            }
        }
        catch (Exception e) {
            throw new HdxException( e );
        }
    }

    public boolean isPersistent() {
        return ( getImage().getURL() != null )
            && ( ! hasVariance() || getVariance().getURL() != null )
            && ( ! hasQuality() || getQuality().getURL() != null );
    }

    //  PWD: note DOM is now a full HDX document, i.e. has <hdx> around it.
    public Element toDOM() {
        DocumentBuilderFactory dfact = DocumentBuilderFactory.newInstance();
        DocumentBuilder dbuild;
        try {
            dbuild = dfact.newDocumentBuilder();
        }
        catch ( ParserConfigurationException e ) {
            // this really shouldn't happen?
            throw new RuntimeException( "Unable to create basic XML parser: "
                                      + e.getMessage() );
        }

        Document doc = dbuild.newDocument();
        Element hdxEl = doc.createElement( "hdx" );
        doc.appendChild( hdxEl );  // makes a full document
        Element ndxEl = doc.createElement( "ndx" );
        hdxEl.appendChild( ndxEl );
        if ( getTitle() != null ) {
            Element titleEl =
                doc.createElement( HdxResourceType.TITLE.xmlName() );
            ndxEl.appendChild( titleEl );
            Node titleNode = doc.createTextNode( getTitle() );
            titleEl.appendChild( titleNode );
        }
        if ( hasImage() ) {
            Element imEl =
                doc.createElement( HdxResourceType.DATA.xmlName() );
            ndxEl.appendChild( imEl );
            if ( getImage().getURL() != null ) {
                Node imUrl = doc.createTextNode( getImage().getURL()
                                                .toExternalForm() );
                imEl.appendChild( imUrl );
            }
            else {
                Node imComm = doc.createComment( "Data array is virtual" );
                imEl.appendChild( imComm );
            }
        }
        if ( hasVariance() ) {
            Element varEl =
                doc.createElement( HdxResourceType.VARIANCE.xmlName() );
            ndxEl.appendChild( varEl );
            if ( getVariance().getURL() != null ) {
                Node varUrl = doc.createTextNode( getVariance().getURL()
                                                 .toExternalForm() );
                varEl.appendChild( varUrl );
            }
            else {
                Node varComm = doc.createComment( "Variance array is virtual" );
                varEl.appendChild( varComm );
            }
        }
        if ( hasQuality() ) {
            Element qualEl =
                doc.createElement( HdxResourceType.QUALITY.xmlName() );
            ndxEl.appendChild( qualEl );
            if ( getQuality().getURL() != null ) {
                Node qualUrl = doc.createTextNode( getQuality().getURL()
                                                  .toExternalForm() );
                qualEl.appendChild( qualUrl );
            }
            else {
                Node qualComm = doc.createComment( "Quality array is virtual" );
                qualEl.appendChild( qualComm );
            }
        }
        if ( getBadBits() != (byte) 0x00 ) {
            Element bbEl =
                doc.createElement( HdxResourceType.BADBITS.xmlName() );
            ndxEl.appendChild( bbEl );
            bbEl.setAttribute( "value", Byte.toString( getBadBits() ) );
        }

        if ( hasWCS() ) {
            Node wcsNode = doc.importNode( impl.getWCSElement(), true );
            ndxEl.appendChild( wcsNode );
        }
        return hdxEl;
    }

    public String toXML()
    {
        try {
            return toXML( toDOM() );
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    //  Factory for transforming an Source into a Result.
    private TransformerFactory transformerFactory = null;

    /**
     * Convert an w3c Element into an XML description stored in a
     * String.
     *
     * @param element the Element
     *
     * @return the XML form of the Element or null if the conversion
     *         cannot be performed.
     */
    public String toXML( Element element )
    {
        try {
            if ( transformerFactory == null ) {
                transformerFactory = TransformerFactory.newInstance();
            }
            Transformer t = transformerFactory.newTransformer();
            StringWriter w = new StringWriter();
            t.transform( new DOMSource( element ), new StreamResult( w ) );
            return w.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
