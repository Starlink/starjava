package uk.ac.starlink.array;

import java.io.IOException;
import java.net.URL;
import java.net.URI;
import uk.ac.starlink.hdx.HdxFacade;
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
     * Returns the <code>ArrayImpl</code> object supplying the implementation
     * for this <code>BridgeNDArray</code>.
     *
     * @return  the object which supplies services to this
     *          <code>BridgeNDArray</code>
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

    /**
     * Obtains a {@link uk.ac.starlink.hdx.HdxFacade} which can
     * represent this object.
     *
     * @param hdxType the registered type which indicates which Hdx
     * type this facade is representing as a DOM element.  This may
     * not be <code>null</code> nor {@link HdxResourceType#NONE}.
     */
    public HdxFacade getHdxFacade( HdxResourceType hdxType ) {
        return new BridgeNDArrayHdxFacade( hdxType );
    }
    
    protected class BridgeNDArrayHdxFacade
            extends uk.ac.starlink.hdx.AbstractHdxFacade {
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
        //private Document cachedDoc;

        public BridgeNDArrayHdxFacade( HdxResourceType type ) {
            if ( type == null
                || type == HdxResourceType.NONE )
                throw new IllegalArgumentException
                        ( "BridgeNDArrayDOMFacade: type is null or NONE" );
            this.type = type;
        }
        
        public Object synchronizeElement( Element el, Object memento ) 
                throws HdxException {
            /*
             * ignore memento -- this Array can't be changed
             * (interface NDArray has no mutator methods), so if the
             * given element has children then it can only be because
             * we've been here before.
             */
            if ( el.hasAttributes() )
                return null;
            
            if ( ! el.getTagName().equals( type.xmlName() ) )
                // The world has gone mad -- this shouldn't happen
                throw new HdxException
                        ( "synchronizeElement given element <"
                          + el.getTagName()
                          + ">, not <"
                          + type.xmlName()
                          + "> as expected" );

            if ( url != null ) {
                el.setAttribute( "uri", url.toString() );
            }

            return null;
        }

        public Object getObject( Element el ) 
                throws HdxException {
            if ( type != HdxResourceType.match( el ) )
                throw new HdxException
                        ( "getObject asked to realise bad type "
                         + el.getTagName() );
            return BridgeNDArray.this;
        }

        public HdxResourceType getHdxResourceType() {
            return type;
        }
    }
}
