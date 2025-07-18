package uk.ac.starlink.topcat;

/**
 * Describes an event which a {@link TopcatListener} may be interested in.
 *
 * @author   Mark Taylor
 * @since    27 Oct 2005
 */
public class TopcatEvent {

    private final TopcatModel model_;
    private final int code_;
    private final Object datum_;

    /** Code indicating that the model's label has changed. */
    public static final int LABEL = 1;

    /** Code indicating that the model's activator has changed. */
    public static final int ACTIVATOR = 2;

    /** Code indicating that the model's parameter list has changed. */
    public static final int PARAMETERS = 3;

    /** Code indicating that one of the model's parameter values has changed. */
    public static final int PARAMETER_VALUE = 4;

    /** Code indicating change in current RowSubset. */
    public static final int CURRENT_SUBSET = 5;

    /** Code indicating change in current sort sequence. */
    public static final int CURRENT_ORDER = 6;

    /**
     * Code indicating that a row has been highlighted.
     * The datum is a <code>Long</code> giving the highlighted row,
     * or null if activation status is being cleared.
     */
    public static final int ROW = 7;

    /**
     * Code indicating that a column has been highlighted or selected in
     * some relevant way.
     * The datum is the relevant 
     * {@link uk.ac.starlink.table.gui.StarTableColumn}.
     */
    public static final int COLUMN = 8;

    /**
     * Code indicating that a given RowSubset should be made visible.
     * The subset may also have changed its content, so a replot may be
     * in order.
     * The datum is the relevant {@link RowSubset}.
     */
    public static final int SHOW_SUBSET = 9;

    /**
     * Code indicating that the definition of one of the existing columns
     * may have changed.  The datum is the relevant 
     * {@link uk.ac.starlink.table.gui.StarTableColumn},
     * if known (may be null).
     */
    public static final int COLUMN_DEF = 10;

    /**
     * Code indicating that the definition of one of the existing subsets
     * may have changed.  The datum is the relevant {@link RowSubset}.
     */
    public static final int SUBSET_DEF = 11;

    /**
     * Constructor.
     *
     * @param  model  the model which generated this event
     * @param  code   one of the numeric codes defined in this class
     *                which describes the nature of the event
     * @param  datum  optional datum giving additional information -
     *                this is code-specfic and may be null
     */
    public TopcatEvent( TopcatModel model, int code, Object datum ) {
        model_ = model;
        code_ = code;
        datum_ = datum;
    }

    /**
     * Gets the model from which this event originated.
     *
     * @return  topcat model
     */
    public TopcatModel getModel() {
        return model_;
    }

    /**
     * Gets the numeric code which specifies the type of this event.
     * The value is one of the static final constants defined in this class.
     *
     * @return   event type code
     */
    public int getCode() {
        return code_;
    }

    /**
     * Gets an additional object further specifying the nature of the event.
     * The value is code-specific, and may be null.
     *
     * @return   additional event characterisation object
     */
    public Object getDatum() {
        return datum_;
    }
}
