package uk.ac.starlink.array;

/**
 * NDArray backed by no data.  It is read-only, and all its pixels have 
 * the bad value.  It may be used as a source of bad values, or as a 
 * place-holder where an NDArray of a certain shape and type is required 
 * but whose data is not needed.
 * <p>
 * For a more flexible no-data array use the {@link DeterministicArrayImpl}
 * class (or write your own).
 *
 * @author   Mark Taylor (Starlink)
 * @see  DeterministicArrayImpl
 */
public class DummyNDArray extends BridgeNDArray {

    /**
     * Constructs a new DummyNDArray with a given shape and type and 
     * bad value handler.  By setting the bad value handler appropriately,
     * the primitive value that the array returns can be modified.
     *
     * @param  oshape  the pixel sequence
     * @param  type    the numeric type
     * @param  bh      the bad value handler
     */
    public DummyNDArray( OrderedNDShape oshape, Type type, BadHandler bh ) {
        super( new DummyArrayImpl( oshape, type, bh.getBadValue() ) );
    }

    /**
     * Constructs a new DummyNDArray with a given shape and type and
     * default pixel sequence and bad value.
     *
     * @param  shape  the shape
     * @param  type   the numeric type
     */
    public DummyNDArray( NDShape shape, Type type ) {
        this( new OrderedNDShape( shape ), type, type.defaultBadHandler() );
    }

    /**
     * Constructs a new DummyNDArray with the same characteristics 
     * (pixel sequence, type and bad value handler) as a template NDArray.
     *
     * @param  nda  template NDArray
     */
    public DummyNDArray( NDArray nda ) {
        this( nda.getShape(), nda.getType(), nda.getBadHandler() );
    }

}
