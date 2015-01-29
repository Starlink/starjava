package uk.ac.starlink.topcat.plot2;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ToolTipManager;

/**
 * MouseListener that, when installed on a component, causes tooltips
 * to appear instantly and without dismissal.
 *
 * <p>This works by doctoring the global ToolTipManager.
 * It will not play nicely with any other code that is making global
 * adjustments to tool tip management.  I'd like to do it differently,
 * but ToolTipManager itself is a singleton, which makes it difficult
 * to handle this in a more localised way.
 *
 * <p>Singleton class.
 *
 * @author   Mark Taylor
 * @since    29 Jan 2015
 */
public class InstantTipper extends MouseAdapter {

    private final ToolTipManager ttm_;
    private final int dfltInitialDelay_;
    private final int dfltDismissDelay_;
 
    private static InstantTipper instance_;

    /**
     * Private constructor for singleton instance.
     */
    private InstantTipper() {
        ttm_ = ToolTipManager.sharedInstance();
        dfltInitialDelay_ = ttm_.getInitialDelay();
        dfltDismissDelay_ = ttm_.getDismissDelay();
    }

    @Override
    public void mouseEntered( MouseEvent evt ) {
        ttm_.setInitialDelay( 0 );
        ttm_.setDismissDelay( Integer.MAX_VALUE );
    }

    @Override
    public void mouseExited( MouseEvent evt ) {
        ttm_.setInitialDelay( dfltInitialDelay_ );
        ttm_.setDismissDelay( dfltDismissDelay_ );
    }

    /**
     * Returns the sole instance of this class.
     */
    public static InstantTipper getInstance() {

        /* Construct the singleton instance lazily in case the default
         * delay settings are done some time late in GUI setup. */
        if ( instance_ == null ) {
            instance_ = new InstantTipper();
        }
        return instance_;
    }
}
