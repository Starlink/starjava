package uk.ac.starlink.ndx;

import java.io.IOException;
import uk.ac.starlink.array.AccessImpl;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Type;

/**
 * Partially implements the ArrayImpl interface based on an NdxAccessMaker
 * object.
 * Used by AccessBulkDataImpl.
 *
 * @author   Mark Taylor (Starlink)
 */
abstract class NdxAccessMakerArrayImpl implements ArrayImpl {

    private final NdxAccessMaker acmaker;

    public abstract AccessImpl getAccess() throws IOException;

    public NdxAccessMakerArrayImpl( NdxAccessMaker acmaker ) {
        this.acmaker = acmaker;
    }

    public OrderedNDShape getShape() {
        return acmaker.getShape();
    }
    public Type getType() {
        return acmaker.getType();
    }
    public Number getBadValue() {
        return acmaker.getBadHandler().getBadValue();
    }
    public boolean isReadable() {
        return acmaker.isReadable();
    }
    public boolean isWritable() {
        return acmaker.isWritable();
    }
    public boolean isRandom() {
        return acmaker.isRandom();
    }
    public boolean multipleAccess() {
        return acmaker.multipleAccess();
    }
    public void open() {
    }
    public boolean canMap() {
        return false;
    }
    public Object getMapped() {
        return null;
    }
    public void close() {
    }
}
