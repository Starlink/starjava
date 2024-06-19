package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.logging.Logger;
import javax.help.BadIDException;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.swing.AbstractAction;

/**
 * Action which invokes help.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2006
 */
public class HelpAction extends AbstractAction {

    private final String helpID_;
    private final Component parent_;
    private HelpWindow helpWin_;

    private static HelpSet hset_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs a new help window for a given helpID.
     * If helpID is non-null, the corresponding topic will be displayed
     * in the browser when the action is invoked.  If it's null,
     * the browser will come up as is.
     *
     * @param  helpID  help id string
     * @param  parent  parent window - may be used for positioning
     */
    public HelpAction( String helpID, Component parent ) {
        helpID_ = helpID;
        parent_ = parent;
        putValue( NAME, helpID == null ? "Help"
                                       : "Help for Window" );
        putValue( SMALL_ICON, helpID == null ? ResourceIcon.MANUAL
                                             : ResourceIcon.HELP );
        putValue( SHORT_DESCRIPTION,
                  helpID == null
                      ? "Display help browser"
                      : "Display help for this window in browser" );

        if ( ! helpIdExists( helpID ) ) {
            logger_.warning( "Unknown help ID " + helpID );
            putValue( SHORT_DESCRIPTION,
                      getValue( SHORT_DESCRIPTION ) + " (unavailable)" );
            setEnabled( false );
        }
    }

    public void actionPerformed( ActionEvent evt ) {
        if ( helpWin_ == null ) {
            helpWin_ = HelpWindow.getInstance( parent_ );
        }
        helpWin_.makeVisible();
        helpWin_.setID( helpID_ );
    }

    /**
     * Tests whether a given helpID is available.
     *
     * @param  helpID  the help ID to test
     * @return  true  iff <code>helpID</code> is a known ID
     *                in this application's HelpSet
     */
    public static boolean helpIdExists( String helpID ) {
        if ( hset_ == null ) {
            URL hsResource = HelpWindow.class.getResource( HelpWindow
                                                          .HELPSET_LOCATION );
            try {
                hset_ = new HelpSet( null, hsResource );
            }
            catch ( HelpSetException e ) {
                logger_.warning( "Can't locate helpset at " + hsResource );
            }
        }
        try {
            javax.help.Map.ID.create( helpID, hset_ );
            return true;
        }
        catch ( BadIDException e ) {
            return false;
        }
    }
}
