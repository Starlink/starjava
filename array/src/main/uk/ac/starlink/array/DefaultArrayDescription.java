package uk.ac.starlink.array;

/**
 * Straightforward immutable implementation of the ArrayDescription interface.
 * Doesn't do anything clever - just provided for convenience when 
 * implementing subinterfaces of ArrayDescription.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DefaultArrayDescription implements ArrayDescription {

    /** The array shape as returned by the getShape method. */
    protected final OrderedNDShape arrayShape;

    /** The array type as returned by the getType method. */
    protected final Type arrayType;

    /** The array bad value handler as returned by the getBadHandler method. */
    protected final BadHandler arrayHandler;

    /** The array random access availability as returned by the isRandom method.     */
    protected final boolean arrayIsRandom;

    /** The array readability as returned by the isReadable method. */
    protected final boolean arrayIsReadable;

    /** The array writability as returned by teh isWritable method. */
    protected final boolean arrayIsWritable;

    /** Pixel ordering scheme; equal to <code>arrayShape.getOrder()</code>. */
    protected final Order arrayOrder;

    /** Number of pixels; equal to <code>arrayShape.getNumPixels()</code>. */
    protected final long arrayNpix;

    /** Number of dimensions; equal to <code>arrayShape.getNumDims()</code>. */
    protected final long arrayNdim;

    /** Array dimensions; equal to <code>arrayShape.getDims()</code>. */
    protected final long[] arrayDims;

    /** Array origin; equal to <code>arrayShape.getOrigin()</code>. */
    protected final long[] arrayOrigin;

    /**
     * Constructs an ArrayDescription object with all its attributes
     * specified explicitly.
     *
     * @param  oshape      array shape
     * @param  type        array type
     * @param  badHandler  array bad value handler
     * @param  isRandom    whether random access is provided
     * @param  isReadable  whether read access is provided
     * @param  isWritable  whether write access is provided
     */
    public DefaultArrayDescription( OrderedNDShape oshape,
                                    Type type, 
                                    BadHandler badHandler,
                                    boolean isRandom,
                                    boolean isReadable,
                                    boolean isWritable ) {

        /* Store principle attributes. */
        this.arrayShape = oshape;
        this.arrayType = type;
        this.arrayHandler = badHandler;
        this.arrayIsRandom = isRandom;
        this.arrayIsReadable = isReadable;
        this.arrayIsWritable = isWritable;

        /* Store derived attributes. */
        this.arrayOrder = oshape.getOrder();
        this.arrayNpix = oshape.getNumPixels();
        this.arrayNdim = oshape.getNumDims();
        this.arrayDims = oshape.getDims();
        this.arrayOrigin = oshape.getOrigin();
    }

    /**
     * Constructs an ArrayDescription object with attributes copied from
     * an existing one.
     *
     * @param   descrip   an existing ArrayDescription object whose properties
     *                    this one is to inherit 
     */
    public DefaultArrayDescription( ArrayDescription descrip ) {
        this( descrip.getShape(), 
              descrip.getType(), 
              descrip.getBadHandler(),
              descrip.isRandom(),
              descrip.isReadable(),
              descrip.isWritable() );
    }

    /**
     * Constructs an ArrayDescription object with attributes copied from
     * an existing one except as overridden by the requirements of a 
     * Requirements object.  The constructed ArrayDescription will 
     * be the same as the the <code>desc</code> parameter, except that the 
     * BadHandler, Type, Window and Order attributes of the <code>req</code>
     * parameter if they are not null, and its Random attribute will be the 
     * logical OR of the Random attributes of <code>desc</code>
     * and <code>req</code>.
     *
     * @param   desc     an existing ArrayDescription object
     * @param   req      a Requirements object
     */
    public DefaultArrayDescription( ArrayDescription desc, Requirements req ) {
        this( new OrderedNDShape( 
                  ( ( req.getWindow() != null ) 
                         ? req.getWindow() : desc.getShape() ),
                  ( ( req.getOrder() != null )
                         ? req.getOrder() : desc.getShape().getOrder() ) ),
              ( req.getType() != null )
                   ? req.getType() : desc.getType(),
              ( req.getBadHandler() != null ) 
                   ? req.getBadHandler() : desc.getBadHandler(),
              req.getRandom() && desc.isRandom(),
              desc.isReadable(),
              desc.isWritable() );
    }


    public OrderedNDShape getShape() {
        return arrayShape;
    }

    public Type getType() {
        return arrayType;
    }

    public BadHandler getBadHandler() {
        return arrayHandler;
    }

    public boolean isRandom() {
        return arrayIsRandom;
    }

    public boolean isReadable() {
        return arrayIsReadable;
    }

    public boolean isWritable() {
        return arrayIsWritable;
    }

}
