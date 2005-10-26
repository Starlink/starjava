package uk.ac.starlink.topcat;

import java.util.EventListener;

/**
 * Listener interface for objects that want to notice changes in a 
 * {@link TopcatModel}.  
 * <p>
 * Note that TopcatModel is currently a mixed bag of existing models
 * together with its own data.  I'm not sure whether it makes sense to
 * amalgamate these at some point.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Feb 2004
 */
public interface TopcatListener extends EventListener {

    /** Code indicating that the model's label has changed. */
    static final int LABEL = 1;

    /** Code indicating that the model's activator has changed. */
    static final int ACTIVATOR = 2;

    /** Code indicating that the model's parameter list has changed. */
    static final int PARAMETERS = 3;

    /** Code indicating change in current RowSubset. */
    static final int SUBSET = 4;

    /** Code indicating that a row has been highlighted. */
    static final int ROW = 5;

    /**
     * Invoked when the model has changed in some way.
     * 
     * @param  tcModel   the model which has changed
     * @param  code      the kind of change which has taken place, as defined
     *                   by one of the static final ints in this interface
     */
    void modelChanged( TopcatModel tcModel, int code );
}
