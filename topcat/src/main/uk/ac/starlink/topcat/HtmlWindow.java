package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * Window which displays simple HTML.
 *
 * @author   Mark Taylor (Starlink)
 * @since    10 Jun 2005
 */
public class HtmlWindow extends AuxWindow {

    private final JEditorPane textPane_;
    private final JLabel urlLabel_;
    private final List history_;
    private final static int HISTORY_LIMIT = 100;
    private int historyPoint_;
    private final Action forwardAct_;
    private final Action backAct_;

    /**
     * Constructs a new HtmlWindow.
     *
     * @param  parent  parent component
     */
    public HtmlWindow( Component parent ) {
        super( "Html Browser", parent );
        JComponent main = getMainArea();
        main.setLayout( new BorderLayout() );

        /* Add text display component in frame centre. */
        textPane_ = new JEditorPane();
        textPane_.setEditable( false );
        JScrollPane scroller = new JScrollPane( textPane_ );
        scroller.setPreferredSize( new Dimension( 450, 400 ) );
        main.add( scroller, BorderLayout.CENTER );

        /* Add location display line at frame bottom. */
        Box line = Box.createHorizontalBox();
        line.add( new JLabel( "URL: " ) );
        urlLabel_ = new JLabel();
        line.add( urlLabel_ );
        main.add( line, BorderLayout.SOUTH );

        /* Add a listener for links. */
        textPane_.addHyperlinkListener( new HyperlinkListener() {
            public void hyperlinkUpdate( HyperlinkEvent evt ) {
                if ( evt.getEventType() == 
                     HyperlinkEvent.EventType.ACTIVATED ) {
                    try {
                        setURL( evt.getURL() );
                    }
                    catch ( IOException e ) {
                        Toolkit.getDefaultToolkit().beep();
                    }
                }
            }
        } );

        /* Set up history list. */
        history_ = new ArrayList();
        historyPoint_ = -1;

        /* Define and configure forward and back actions. */
        forwardAct_ = new AdvanceAction( "Forward", ResourceIcon.FORWARD,
                                         "Go forward a page", +1 );
        backAct_ = new AdvanceAction( "Back", ResourceIcon.BACKWARD,
                                      "Go back a page", -1 );
        configureActions();
        getToolBar().add( backAct_ );
        getToolBar().add( forwardAct_ );
        getToolBar().addSeparator();

        /* Final window setup. */
        addHelp( "documentViewers" );
    }

    /**
     * Attempts to display a new URL in the browser window, updating
     * history state accordingly.
     * Loading is asynchronous, but if the named resource doesn't exist
     * an IOException will (probably) be thrown.
     *
     * @param   url   URL
     */
    public void setURL( URL url ) throws IOException {

        /* Attempt to load the new URL.  This will throw an IOException if
         * the URL doesn't exist. */
        moveToURL( url );

        /* If we're not currently at the head of the history list, trim it
         * until we are. */
        while ( history_.size() > historyPoint_ + 1 ) {
            history_.remove( history_.size() - 1 );
        }
        assert historyPoint_ == history_.size() - 1;

        /* Add the new URL to the history list. */
        history_.add( url );
        historyPoint_++;

        /* If the history list is getting too long, trim it. */
        while ( history_.size() > HISTORY_LIMIT && historyPoint_ >= 0 ) {
            history_.remove( 0 );
            historyPoint_--;
        }

        /* Ensure that the forward and back buttons are configured correctly
         * for the new state. */
        configureActions();
    }

    /**
     * Does the work of actually loading the URL into the GUI.
     *
     * @param  url   URL
     */
    private void moveToURL( URL url ) throws IOException {
        textPane_.setPage( url );
        urlLabel_.setText( url.toString() );
    }

    /**
     * Ensures that the forward and back actions are configured appropriately
     * for the current history state.
     */
    private void configureActions() {
        backAct_.setEnabled( historyPoint_ > 0 );
        forwardAct_.setEnabled( historyPoint_ < history_.size() - 1 );
    }

    /**
     * Action for moving around in the history list.
     */
    private class AdvanceAction extends BasicAction {
        private final int increment_;

        /**
         * Constructor.
         *
         * @param   name          action name
         * @param   icon          action icon
         * @param   description   action description
         * @param   increment     number of pages forward the action is to move
         */
        public AdvanceAction( String name, Icon icon, String description,
                              int increment ) {
            super( name, icon, description );
            increment_ = increment;
        }

        public void actionPerformed( ActionEvent evt ) {
            try {
                moveToURL( (URL) history_.get( historyPoint_ + increment_ ) );
                historyPoint_ += increment_;
                configureActions();
            }
            catch ( IOException e ) {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }
}
