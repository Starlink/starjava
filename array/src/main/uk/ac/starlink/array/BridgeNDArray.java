package uk.ac.starlink.array;

import java.io.IOException;
import java.net.URL;

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
         * or we are only going to dispense a single ArrayAccess in any
         * case, obtain a new AccessImpl and build an ArrayAccess object 
         * on it. */
        if ( impl.multipleAccess() || ! impl.isRandom() ) {
            return new DefaultArrayAccess( this, impl.getAccess(), 
                                           mappedArray );
        }

        /* Otherwise, we will need to build an ArrayAccess object on top
         * of a shared sole AccessImpl object. */
        else {
            // assert ( ! impl.multipleAccess() && impl.isRandom() );
            return new MultiplexArrayAccess( this, soleAccessImpl,
                                             mappedArray );
        }
    }

    public void close() throws IOException {
        if ( ! closed ) {
            impl.close();
            mappedArray = null;
            if ( soleAccessImpl != null ) {
                soleAccessImpl.close();
            }
            closed = true;
        }
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

}
