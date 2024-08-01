package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Very basic HTML browser window.
 * This class handles very simple decoration and history management,
 * the HTML rendering is done by a supplied AbstractHtmlPanel instance.
 *
 * @author   Mark Taylor (Starlink)
 * @since    10 Jun 2005
 */
public class HtmlWindow extends AuxWindow {

    private final JTextField urlField_;
    private final AbstractHtmlPanel htmlPanel_;
    private final List<URL> history_;
    private final static int HISTORY_LIMIT = 100;
    private final Action forwardAct_;
    private final Action backAct_;
    private int historyPoint_;

    /**
     * Constructs a new HtmlWindow.
     *
     * @param  parent  parent component
     * @param  htmlPanel  HTML rendering panel
     */
    @SuppressWarnings("this-escape")
    public HtmlWindow( Component parent, AbstractHtmlPanel htmlPanel ) {
        super( "Html Browser", parent );
        htmlPanel_ = htmlPanel;
        JComponent main = getMainArea();
        main.setLayout( new BorderLayout() );

        /* Add text display component in frame centre. */
        htmlPanel.setPreferredSize( new Dimension( 450, 400 ) );
        main.add( htmlPanel, BorderLayout.CENTER );

        /* Add location display line at frame bottom. */
        Box line = Box.createHorizontalBox();
        line.add( new JLabel( "URL: " ) );
        urlField_ = new JTextField();
        urlField_.setEditable( false );
        line.add( urlField_ );
        htmlPanel_.addPropertyChangeListener( "url",
                                              new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                Object url = evt.getNewValue();
                urlField_.setText( url instanceof URL ? url.toString() : null );
            }
        } );
        main.add( line, BorderLayout.SOUTH );

        /* Set up history list. */
        history_ = new ArrayList<URL>();
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
     *
     * @param   url   URL
     */
    public void setURL( URL url ) {

        /* Attempt to load the new URL. */
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
    private void moveToURL( URL url ) {
        htmlPanel_.setUrl( url );
        urlField_.setText( url.toString() );
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
            moveToURL( history_.get( historyPoint_ + increment_ ) );
            historyPoint_ += increment_;
            configureActions();
        }
    }
}
