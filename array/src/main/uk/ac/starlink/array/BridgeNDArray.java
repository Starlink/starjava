package uk.ac.starlink.array;

import java.io.IOException;
import java.net.URL;
import uk.ac.starlink.hdx.DOMFacade;
import uk.ac.starlink.hdx.HdxException;
import uk.ac.starlink.hdx.HdxResourceType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class providing an {@link NDArray} implementation based on an
 * implementation of the {@link ArrayImpl} interface.
 * The idea is that all the implementation work common to any
 * underlying implementation of array functionality is contained in this
 * class, while the details specific to different underlying array types
 * can be contained in different implementations of the ArrayImpl
 * interface.  In particular, this class does extensive validation on
 * method parameters and takes care of throwing the right exceptions
 * so that the burden of worrying about invalid parameters is removed
 * from the ArrayImpl implementations.
 * <p>
 * This exemplifies the Bridge Pattern.
 *
 * @author   Mark Taylor (Starlink)
 */
public class BridgeNDArray extends DefaultArrayDescription implements NDArray {

    private final ArrayImpl impl;
    private final URL url;
    private boolean opened = false;
    private boolean closed = false;
    private AccessImpl soleAccessImpl;
    private Object mappedArray;

    /**
     * Constructs a BridgeNDArray from an ArrayImpl with a given URL.
     *
     * @param  impl  an ArrayImpl on which this BridgeNDArray will be based
     * @param  url   the URL at which this NDArray can be found.  It is only
     *               used to dispense via the getURL method.  May be null
     *               if the NDArray represented by impl is not persistent
     */
    public BridgeNDArray( ArrayImpl impl, URL url ) {

        /* Prepare the array description basics. */
        super( impl.getShape(),
               impl.getType(),
               BadHandler.getHandler( impl.getType(), impl.getBadValue() ),
               impl.isRandom(), impl.isReadable(), impl.isWritable() );

        /* Store the implementation we are based on. */
        this.impl = impl;

        /* Store the URL.  This is only used to dispense via the getURL
         * method.  It will be null if there is no persistent representation
         * of this NDArray. */
        this.url = url;
    }

    /**
     * Constructs a non-persistent BridgeNDArray (one without a URL) 
     * from an ArrayImpl.
     *
     * @param  impl  an ArrayImpl on which this BridgeNDArray will be based
     */
    public BridgeNDArray( ArrayImpl impl ) {
        this( impl, null );
    }

    public URL getURL() {
        return url;
    }

    public boolean multipleAccess() {
        return impl.multipleAccess() || impl.isRandom();
    }

    public ArrayAccess getAccess() throws IOException {

        /* Ensure that the ArrayImpl is in an open state, and capable of
         * providing a new accessor. */
        if ( closed ) {
            throw new IllegalStateException( "NDArray has been closed" );
        }
        else if ( ! opened ) {
            impl.open();
            opened = true;
            mappedArray = impl.canMap() ? impl.getMapped() : null;
            if ( ! impl.multipleAccess() ) {
                soleAccessImpl = impl.getAccess();
            }
        }
        else if ( ! impl.isRandom() && ! impl.multipleAccess() ) {
            throw new UnsupportedOperationException(
                "Sole ArrayAccess object has already been used." );
        }

        /* If the impl is capable of producing multiple AccessImpls, 
         * obtain a new AccessImpl and build an ArrayAccess object on it. */
        if ( impl.multipleAccess() ) {
            return new DefaultArrayAccess( this, impl.getAccess(), 
                                           mappedArray );
        }

        /* Otherwise, if the impl is random then build an ArrayAccess on 
         * top of a shared AccessImpl. */
        else if ( impl.isRandom() ) {
            return new MultiplexArrayAccess( this, soleAccessImpl,
                                             mappedArray );
        }

        /* Otherwise, just build a sole ArrayAccess object on top
         * of a sole AccessImpl object. */
        else {
            return new DefaultArrayAccess( this, soleAccessImpl,
                                           mappedArray );
        }
    }

    public void close() throws IOException {
        if ( ! closed ) {
            impl.close();
            mappedArray = null;
            if ( soleAccessImpl != null && impl.isRandom() ) {
                soleAccessImpl.close();
            }
            closed = true;
        }
    }

    /**
     * Returns the <tt>ArrayImpl</tt> object supplying the implementation
     * for this <tt>BridgeNDArray</tt>.
     *
     * @return  the object which supplies services to this
     *          <tt>BridgeNDArray</tt>
     */
    public ArrayImpl getImpl() {
        return impl;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer( "BridgeNDArray " );
        buf.append( arrayIsReadable ? "r" : "" )
           .append( arrayIsWritable ? "w" : "" )
           .append( ' ' )
           .append( arrayShape )
           .append( ' ' )
           .append( arrayType )
           .append( ' ' )
           .append( arrayHandler )
           .append( ' ' )
           .append( ( url == null ) ? "<transient>" : ( "<" + url + ">" ) )
           .append( "; impl={ " )
           .append( impl.toString() )
           .append( " }" );
        return buf.toString();
    }

    public DOMFacade getDOMFacade(HdxResourceType hdxType) {
        return new BridgeNDArrayDOMFacade(hdxType);
    }
    
    protected class BridgeNDArrayDOMFacade
            extends uk.ac.starlink.hdx.AbstractDOMFacade {
        /*
         * Implement the DOMFacade by creating a DOM using
         * HdxElements, and caching it.
         * Implement setAttribute by calling setAttribute on the top
         * element of that cached DOM.
         *
         * XXX Is this sufficient?  Can this become out of date?  Are
         * the attributes purely for information, or should they modify
         * instance variables of the BridgeNDArray?
         */
        private HdxResourceType type;
        private Document cachedDoc;

        public BridgeNDArrayDOMFacade(HdxResourceType type) {
            this.type = type;
        }
        
        public Element getDOM(URL base) {
            if (cachedDoc == null) {
                cachedDoc = uk.ac.starlink.hdx.HdxDOMImplementation
                        .getInstance()
                        .createDocument(null, "array", null);
                Element el = cachedDoc.createElement(type.xmlName());
                cachedDoc.appendChild(el);
            }
            assert cachedDoc != null;
            Element ret = cachedDoc.getDocumentElement();
            assert ret.getTagName().equals(type.xmlName());
            return ret;
        }
        
        public Object getObject(Element el) 
                throws HdxException {
            if (type != HdxResourceType.match(el))
                throw new HdxException
                        ("getObject asked to realise bad type "
                         + el.getTagName());
            return BridgeNDArray.this;
        }

        /** 
         * Sets an attribute on the element this object is the facade
         * for.  If an attribute is `set' to a null value, it is removed.
         *
         * @param el the element which is to have the attribute set
         *
         * @param name the attribute which is to be set
         *
         * @param value the new value of the attribute.  If the value is
         * null, the attribute is removed.  Setting the value to the empty
         * string is allowed, and is not the same as setting it to null.
         *
         * @return true if the operation succeeded, or false if there
         * was some problem with the arguments
         */
        public boolean setAttribute(Element el, String name, String value) {
            if (el == null || name == null) {
                System.err.println("BridgeNDArrayDOMFacade: null el or name");
                return false;
            }
            Element myDocEl = getDOM(null);
            if (! myDocEl.getTagName().equals(el.getTagName())) {
                System.err.println
                       ("BridgeNDArrayDOMFacade: inconsitent arguments: given "
                        + el.getTagName() + ", but managing "
                        + myDocEl.getTagName());
                return false;
            }
            if (value == null)
                myDocEl.removeAttribute(name);
            else
                myDocEl.setAttribute(name, value);
            return true;
        }
    }
}
