package uk.ac.starlink.array;

/**
 * Specifies a set of requirements for an array object.
 * A Requirements object may be passed to a constructor or factory method
 * which returns an array object to indicate some required characteristics
 * of the returned array.  In general, any fields which are filled in
 * constitute a requirement for the given attribute, while a
 * null (or false) value indicates that the given attribute is
 * not required; if the method/constructor in question cannot comply
 * it should throw an exception rather than return an object lacking
 * any of the requirements.
 * <p>
 * The attributes which may be stipulated are as follows:
 * <dl>
 * <dt>type</dt>
 * <dd>Numerical data type</dd>
 * <dt>window</dt>
 * <dd>Shape of the array to be returned 
 *     (as a window on the underlying array)</dd>
 * <dt>order</dt>
 * <dd>Pixel ordering scheme
 * <dt>badHandler</dt>
 * <dd>Bad value handler</dd>
 * <dt>random</dt>
 * <dd>Random access required flag</dd>
 * <dt>mode</dt>
 * <dd>Access mode</dd>
 * </dl>
 * The <code>mode</code> requirement is slightly different from the others; 
 * it is examined
 * by some methods to determine the use to which the resulting 
 * array object will be put, and controls whether data is copied
 * from a source into it on return, or copied out of it to a sink at
 * close time.
 * <p>
 * The attribute setter methods are declared to return the Requirements
 * object itself for convenience so that settings may be chained.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class Requirements implements Cloneable {

    private Type type;
    private NDShape window;
    private Order order;
    private BadHandler badHandler;
    private boolean random;
    private AccessMode mode;

    /**
     * Constructs a Requirements object with no specifications.
     */
    public Requirements() {
        this( null );
    }

    /**
     * Constructs a Requirements object specifying a given access mode.
     */
    public Requirements( AccessMode mode ) {
        setMode( mode );
    }

    /**
     * Gets this object's required data type.
     * 
     * @return   this object's required data type
     */
    public Type getType() {
        return type;
    }
    /**
     * Sets this object's required data type.  If the BadHandler is set
     * to an incompatible type, this method will clear the BadHandler 
     * field (set it to null).
     *
     * @param  type  the required data type
     * @return  this object
     */
    public Requirements setType( Type type ) { 
        this.type = type;
        if ( badHandler != null && badHandler.getType() != type ) {
            setBadHandler( null );
        }
        return this;
    }

    /**
     * Gets this object's required window; the shape it must have.
     *
     * @return  this object's required window
     */
    public NDShape getWindow() {
        return window;
    }
    /**
     * Sets this object's required window; the shape it must have.
     *
     * @param  window the required window
     * @return   this object
     */
    public Requirements setWindow( NDShape window ) {
        this.window = new NDShape( window );
        return this;
    }

    /**
     * Gets this object's required pixel ordering scheme.
     *
     * @return   this object's required ordering
     */
    public Order getOrder() {
        return order;
    }
    /**
     * Sets this object's required pixel ordering scheme.
     *
     * @param  order  the required ordering
     * @return  this object
     */
    public Requirements setOrder( Order order ) {
        this.order = order;
        return this;
    }

    /**
     * Sets this object's required ordered shape (pixel sequence).
     * This is simply a shortcut way of calling <code>setWindow</code>
     * and <code>setOrder</code> in one go.
     *
     * @param  oshape  the ordered shape (or equivalently, pixel sequence)
     *                 required
     * @return   this object
     */
    public Requirements setShape( OrderedNDShape oshape ) {
        setWindow( oshape );
        setOrder( ( oshape != null ) ? oshape.getOrder() : null );
        return this;
    }

    /**
     * Gets this object's required bad value handler.
     *
     * @return   this object's required bad value handler
     */
    public BadHandler getBadHandler() {
        return badHandler;
    }
    /**
     * Sets this object's required bad value handler.  This must only 
     * be set if the Type is already set, and the types must match
     * (<code>handler.getType()==this.getType()</code>).
     *
     * @param  handler   the required handler
     * @return  this object
     * @throws  IllegalStateException  if no type has been set
     * @throws  IllegalArgumentException  if the type of <code>handler</code>
     *              does not match the type of this object
     */
    public Requirements setBadHandler( BadHandler handler ) { 
        if ( type == null && handler != null ) {
            throw new IllegalStateException( 
                "Cannot set BadHandler without set Type" );
        }
        else if ( handler != null && handler.getType() != this.type ) {
            throw new IllegalArgumentException(
                "BadHandler type " + handler.getType() + 
                " is inconsistent with Requirements type " + this.type );
        }
        this.badHandler = handler;
        return this;
    }
    /**
     * Sets this object's required bad value handler to be one with a
     * given bad value.  This convenience method does almost exactly
     * the same as
     * <pre>
     *    setBadHandler(BadHandler.getHandler(getType(),badValue))
     * </pre>
     * It may only be called if the type has already been set.
     *
     * @param  badValue  the bad value which the required bad value handler
     *         must use
     * @return  this object
     * @throws IllegalStateException  if no type has been set
     * @throws IllegalArgumentException  if the type of <code>badValue</code>
     *           does not match the reqired type of this object
     */
    public Requirements setBadValue( Number badValue ) {
        if ( type == null && badValue != null ) {
            throw new IllegalStateException(
                "Cannot set bad value without set Type" );
        }
        else {
            BadHandler handler = BadHandler.getHandler( type, badValue );
            setBadHandler( handler );
        }
        return this;
    }

    /**
     * Gets a flag indicating whether random access is required.
     *
     * @return  true if random access is required
     */
    public boolean getRandom() {
        return random;
    }
    /**
     * Sets a flag indicating whether random access is required.
     *
     * @param  random  whether random access will be required
     */
    public Requirements setRandom( boolean random ) {
        this.random = random;
        return this;
    }

    /**
     * Gets an object indicating the use to which the required array will
     * be put.
     * 
     * @return   the access mode required
     */
    public AccessMode getMode() {
        return mode;
    }
    /**
     * Sets an object indicating the use to which the required array will
     * be put.
     *
     * @param   mode  the required access mode
     * @return   this object
     */
    public Requirements setMode( AccessMode mode ) {
        this.mode = mode;
        return this;
    }

    /**
     * Provides a snapshot of this object; modifying the returned object
     * will not affect the object from which it was cloned.
     */
    public Object clone() {
        try {
            return super.clone();
        }
        catch ( CloneNotSupportedException e ) {
            throw new AssertionError();
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        if ( type != null ) buf.append( " type=" + type );
        if ( window != null ) buf.append( " window=" 
                                        + NDShape.toString( window ) );
        if ( order != null ) buf.append( " order=" + order );
        if ( badHandler != null ) buf.append( " badHandler=" + badHandler );
        if ( random ) buf.append( " random=true" );
        if ( mode != null ) buf.append( " mode=" + mode );
        if ( buf.length() == 0 ) buf.append( " <no requirements>" );
        return "Requirements: " + buf.toString();
    }
    
}
