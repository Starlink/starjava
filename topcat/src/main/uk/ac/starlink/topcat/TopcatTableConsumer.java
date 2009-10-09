package uk.ac.starlink.topcat;

import java.awt.Component;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.BasicTableConsumer;

/**
 * Partial TableConsumer implementation which consumes tables by loading
 * them into the ControlWindow.
 *
 * @author   Mark Taylor
 * @since    9 Oct 2009
 */
public abstract class TopcatTableConsumer extends BasicTableConsumer {

    private final Component parent_;
    private final ControlWindow control_;
    private String id_;
    private LoadingToken token_;

    /**
     * Constructor.
     *
     * @param  parent  parent component
     * @param  control  control window
     */
    public TopcatTableConsumer( Component parent, ControlWindow control ) {
        super( parent );
        parent_ = parent;
        control_ = control;
    }

    /**
     * Returns the ID of the table which is currently being consumed.
     * 
     * @return   loading table id, or null
     */
    public String getLoadingId() {
        return id_;
    }

    /**
     * Returns the parent component declared for this object.
     *
     * @return   parent component
     */
    public Component getParent() {
        return parent_;
    }

    public void loadStarted( String id ) {
        id_ = id;
        token_ = new LoadingToken( id );
        control_.addLoadingToken( token_ );
        super.loadStarted( id );
    }

    public void loadSucceeded( StarTable table ) {
        control_.removeLoadingToken( token_ );
        token_ = null;
        super.loadSucceeded( table );
        id_ = null;
    }

    public void loadFailed( Throwable error ) {
        control_.removeLoadingToken( token_ );
        token_ = null;
        super.loadFailed( error );
        id_ = null;
    }

    protected void processError( Throwable error ) {
        if ( error instanceof OutOfMemoryError ) {
            TopcatUtils.memoryError( (OutOfMemoryError) error );
        }
        else {
            super.processError( error );
        }
    }
}
