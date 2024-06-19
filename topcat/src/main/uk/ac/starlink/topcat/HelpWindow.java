package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.help.JHelpContentViewer;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.help.InvalidHelpSetContextException;
import javax.help.JHelp;
import javax.help.JHelpTOCNavigator;
import javax.help.event.HelpModelEvent;
import javax.help.event.HelpModelListener;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.UIManager;

/**
 * Window for displaying the help browser.  Get the instance of this
 * singleton class using the {@link #getInstance} method.
 * Sun's JavaHelp package is used for the hard work.
 *
 * @author   Mark Taylor (Starlink)
 * @see   <a href="http://java.sun.com/products/javahelp/">JavaHelp</a>
 */
public class HelpWindow extends AuxWindow {

    /** Location of the HelpSet file relative to this class. */
    public static final String HELPSET_LOCATION = "help/sun253.hs";

    private final Action toBrowserAction_;
    private final JLabel urlHead_;
    private final JTextField urlInfo_;
    private final Desktop desktop_;
    private JHelp jhelp_;
    private HelpSet hset_;
    private String helpId_;
    private boolean fontSet_;

    private static HelpWindow instance_;
    private static Logger logger_ = Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs a new HelpWindow. 
     * If supplied, a <code>parent</code> window is used only for positioning.
     * This private sole constructor is invoked by <code>getInstance</code>.
     *
     * @param  parent  parent window
     */
    private HelpWindow( Component parent ) {
        super( "TOPCAT Help", parent );
        JComponent helpComponent;

        /* Set up a component for general information to user. */
        Box infoBox = new Box( BoxLayout.X_AXIS );
        urlHead_ = new JLabel( " External URL: " );
        urlInfo_ = new JTextField();
        urlInfo_.setEditable( false );
        infoBox.add( urlHead_ );
        infoBox.add( urlInfo_ );
        externalURL( null );
        JComponent mainArea = getMainArea();
        mainArea.add( infoBox, BorderLayout.SOUTH );

        /* Create the HelpSet if there is not already one. */
        try {
            URL hsResource = HelpWindow.class.getResource( HELPSET_LOCATION );
            hset_ = new HelpSet( null, hsResource );
            jhelp_ = new JHelp( hset_ );
            jhelp_.setPreferredSize( new Dimension( 700, 500 ) );
            helpComponent = jhelp_;

            /* Fiddle around with presentation. */
            prepareTOC( jhelp_ );

            /* Add a listener which can inform about the location of
             * external URLs. */
            JHelpContentViewer cview = jhelp_.getContentViewer();
            cview.addHelpModelListener( new HelpModelListener() {
                public void idChanged( HelpModelEvent evt ) {
                    javax.help.Map.ID mapId = evt.getID();
                    helpId_ = mapId == null ? null : mapId.id;
                    URL url = mapId == null ? evt.getURL() : null;
                    toBrowserAction_.setEnabled( helpId_ != null &&
                                                 desktop_ != null );
                    externalURL( url );
                }
             } );

            /* Vague hope this might load images synchronously (to make 
             * display less jerky) but don't think it does. */
            cview.setSynch( true );

            /* Muck about with toolbars. */
            pinchHelpToolBarTools( jhelp_ );
        }

        /* If there was an error, present the error message where the
         * help would have been. */
        catch ( HelpSetException e ) {
            StringWriter swriter = new StringWriter();
            String msg;
            try {
                e.printStackTrace( new PrintWriter( swriter ) );
                swriter.close();
            }
            catch ( IOException e2 ) {
                msg = e.toString();
            }
            msg = swriter.toString();
            helpComponent = new JTextArea( msg );
        }

        /* Action which displays help page in a web browser. */
        desktop_ = TopcatUtils.getBrowserDesktop();
        toBrowserAction_ =
                new BasicAction( "To Browser", ResourceIcon.TO_BROWSER,
                                 "Display current help page in WWW browser" ) {
            public void actionPerformed( ActionEvent evt ) {
                URI helpUri = helpId_ == null
                            ? null
                            : BrowserHelpAction.getHelpUri( helpId_ + ".html" );
                if ( helpUri != null && desktop_ != null ) {
                    try {
                        desktop_.browse( helpUri );
                    }
                    catch ( IOException e ) {
                        logger_.log( Level.WARNING,
                                     "Browser trouble with " + helpUri, e );
                        beep();
                    }
                }
                else {
                    beep();
                }
            }
        };
        toBrowserAction_.setEnabled( helpId_ != null && desktop_ != null );
        getToolBar().add( toBrowserAction_ );
        getToolBar().addSeparator();

        /* Even a help window needs help. */
        addHelp( "HelpWindow" );

        /* Install it into this window. */
        mainArea.add( helpComponent, BorderLayout.CENTER );
    }

    /**
     * Returns the sole instance of this class.  
     * If supplied, a <code>parent</code> window is used only for positioning.
     *
     * @param  parent  parent window
     */
    public static HelpWindow getInstance( Component parent ) {
        if ( instance_ == null ) {
            instance_ = new HelpWindow( parent );
        }
        return instance_;
    }

    /**
     * Returns the JHelp window which does the viewing of the pages in 
     * this window.
     *
     * @return  the JHelp component
     */
    public JHelp getJHelp() {
        return jhelp_;
    }

    /**
     * Sets the current help ID to the one represented by the given string.
     * If <code>helpID</code> is <code>null</code>, or does not refer to a 
     * real ID in this HelpSet, no change is made to the current view.
     *
     * @param   helpID  the ID to change to
     */
    public void setID( String helpID ) {
        if ( hset_ != null && helpID != null ) {
            javax.help.Map.ID mapID = javax.help.Map.ID.create( helpID, hset_ );
            if ( mapID != null ) {
                try {
                    jhelp_.setCurrentID( mapID );

                    /* Tweak terminally ugly default font. */
                    if ( ! fontSet_ ) {
                        Font font = UIManager.getFont( "TextField.font" );
                        if ( font != null ) {
                            jhelp_.setFont( font );
                            fontSet_ = true;
                        }
                    }
                }
                catch ( InvalidHelpSetContextException e ) {
                    logger_.info( "Bad help ID: " + helpID );
                }
            }
            else {
                logger_.info( "Unknown help ID: " + helpID );
            }
        }
    }

    /**
     * This naughty method pokes around in a JHelp window to find its
     * toolbar and grabs the tools out of it, copying them into 
     * the toolbar controlled by the AuxWindow class.  In this way we
     * can ensure consistency between the look and feel of this help
     * window and other windows in the application.
     * <p>
     * Of course this behaviour depends on undocumented details of the
     * JHelp class and so is not recommended behaviour.  It is written
     * so that if any of its assumptions prove incorrect the worst that
     * will happen is that the tool bars will look wrong.
     * 
     * @param   helpComponent  the component which (we hope) contains a toolbar
     */
    private void pinchHelpToolBarTools( JHelp helpComponent ) {

        /* Acquire a list of the components within the JHelp. */
        Component[] contents = helpComponent.getComponents();

        /* Try to identify one which is a JToolBar. */
        JToolBar jhBar = null;
        for ( int i = 0; i < contents.length; i++ ) {
            if ( contents[ i ] instanceof JToolBar ) {
                jhBar = (JToolBar) contents[ i ];
                break;
            }
        }

        /* If succesful so far, get a list of all the tools in the help 
         * toolbar. */
        if ( jhBar != null ) {
            List<Component> tools = new ArrayList<Component>();
            for ( int i = 0; ; i++ ) {
                Component tool = jhBar.getComponentAtIndex( i );
                if ( tool == null ) {
                    break;
                }
                tools.add( tool );
            }

            /* Move each one from the help toolbar to this window's toolbar. */
            for ( Component tool : tools ) {
                jhBar.remove( tool );
                getToolBar().add( tool );
            }

            /* Dispose of the JHelp toolbar. */
            helpComponent.remove( jhBar );
        }
    }

    /**
     * Indicate to the user the external URL of the page which is currently
     * displayed in the content browser.
     *
     * @param  url  the url or null if it's not external
     */
    private void externalURL( URL url ) {
        boolean isActive = url != null;
        urlHead_.setEnabled( isActive );
        urlInfo_.setText( isActive ? url.toString() : null );
    }

    private static void prepareTOC( JHelp jhelp ) {
        JHelpTOCNavigator tocnav = null;
        for ( Enumeration<?> en = jhelp.getHelpNavigators();
              en.hasMoreElements(); ) {
            Object nav = en.nextElement();
            if ( nav instanceof JHelpTOCNavigator ) {
                tocnav = (JHelpTOCNavigator) nav;
                break;
            }
        }
        if ( tocnav == null ) {
            return;  // oh well
        }
        HelpSet hs = jhelp.getModel().getHelpSet();
        for ( Enumeration<?> en = hs.getCombinedMap().getAllIDs();
              en.hasMoreElements(); ) {
            String id = ((javax.help.Map.ID) en.nextElement()).id;
            tocnav.collapseID( id );
        }
    }
    
}
