package uk.ac.starlink.topcat;

import uk.ac.starlink.table.StarTable;

/**
 * Defines encoding and decoding for TopcatModels in order to 
 * perform per-table session save/restore.  Information beyond the
 * StarTable data and metadata itself should include things like
 * defined row subsets, hidden columns, apparent table sort order etc.
 * This class translates between a TopcatModel and a StarTable;
 * the StarTable can be de/serialized using one of the standard
 * STIL I/O handlers (probably a VOTable-based one since there
 * will be significant amounts of metadata).
 *
 * <p>Note this means that the "serialization" done here is not free-form,
 * all the required metadata and data has to be encoded in a form
 * that can be captured by the StarTable data model
 * (basically, columns and parameters with associated metadata).
 * Additionally, to be useful, it needs to store this information in
 * a way that can be later serialized by at least one of the available
 * STIL I/O handlers.  Formats based on the VOTable format are generally
 * capable enough for this, though note that ColumnInfo "auxiliary"
 * metadata items will probably be lost during VOTable serialization,
 * so probably can't be used when implementing this interface.
 *
 * @author   Mark Taylor
 * @since    16 Jul 2010
 */
public interface TopcatCodec {

    /**
     * Turns a TopcatModel into a StarTable, ready for serialization.
     * Optionally, the output table may include global information
     * associated with the state of the application as a whole
     * alongside state specific to the provided TopcatModel.
     *
     * @param  tcModel  model
     * @param  withGlobals  if true, include global state in the output
     * @return   table
     */
    StarTable encode( TopcatModel tcModel, boolean withGlobals );

    /**
     * Indicates whether a given table is a candidate for this codec's
     * {@link #decode decode} method.  It should in general return true
     * for a table that has been returned from this object's
     * {@link #encode encode} method (possibly following a table
     * write/read cycle) and false for other tables.
     *
     * @param   table   data+metadata table
     * @return  true  iff it looks like the table was written by this
     *                codec and can be decoded by it
     */
    boolean isEncoded( StarTable table );

    /**
     * Takes a table which has been previously serialized by calling 
     * this codec's {@link #encode encode} method, and for which the
     * {@link #isEncoded isEncoded} method returns true,
     * and turns it into a TopcatModel.
     * If decoding fails, null is returned.
     *
     * <p>If used with a ControlWindow that users may be interacting with,
     * this method should be called from the AWT event dispatch thread.
     * This method may (for instance during testing) be called with
     * a null value for the <code>controlWindow</code> parameter,
     * but the resulting TopcatModel may not be suitable for all kinds
     * of user interactions.
     *
     * @param  table  encoded table
     * @param  location  table location string
     * @param  controlWindow  control window, or null if necessary
     * @return   topcat model, or null
     */
    TopcatModel decode( StarTable table, String location,
                        ControlWindow controlWindow );
}
