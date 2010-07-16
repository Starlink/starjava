package uk.ac.starlink.topcat;

import uk.ac.starlink.table.StarTable;

/**
 * Performs encoding and decoding for TopcatModels in order to 
 * perform per-table session save/restore.  This class translates 
 * between a TopcatModel and a StarTable; the StarTable can be 
 * de/serialized using one of the standard STIL I/O handlers
 * (probably a VOTable-based one since there will be significant
 * amounts of metadata).
 *
 * <p>This class is currently a singleton.
 *
 * @author   Mark Taylor
 * @since    16 Jul 2010
 */
public class TopcatCodec {

    private static final TopcatCodec instance_ = new TopcatCodec();

    /**
     * Private constructor prevents public instantiation.
     */
    private TopcatCodec() {
    }

    /**
     * Turns a TopcatModel into a StarTable, ready for serialization.
     *
     * @param  tcModel  model
     * @return   table
     */
    public StarTable encode( TopcatModel tcModel ) {
        return tcModel.getDataModel();
    }

    /**
     * Takes a table which has been previously serialized by calling 
     * this class's {@link #encode} method, and turns it into a TopcatModel.
     * If it looks like the table is not one which was the result of an
     * earlier <code>encode</code> call, null will be returned.
     *
     * @param  table  encoded table
     * @return   topcat model, or null
     */
    public TopcatModel decode( StarTable table ) {
            return null;
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return   instance
     */
    public static TopcatCodec getInstance() {
        return instance_;
    }
}
