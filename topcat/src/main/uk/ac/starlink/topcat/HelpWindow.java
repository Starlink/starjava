package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.help.JHelpContentViewer;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.help.InvalidHelpSetContextException;
import javax.help.JHelp;
import javax.help.JHelpTOCNavigator;
import javax.help.event.HelpModelEvent;
import javax.help.event.HelpModelListener;
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
    public static final String HELPSET_LOCATION = "help/sun178.hs";

    private JLabel urlHead;
    private JTextField urlInfo;
    private JHelp jhelp;
    private HelpSet hset;
    private boolean fontSet;

    private static HelpWindow instance;
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs a new HelpWindow. 
     * If supplied, a <tt>parent</tt> window is used only for positioning.
     * This private sole constructor is invoked by <tt>getInstance</tt>.
     *
     * @param  parent  parent window
     */
    private HelpWindow( Component parent ) {
        super( "TOPCAT Help", parent );
        JComponent helpComponent;

        /* Set up a component for general information to user. */
        Box infoBox = new Box( BoxLayout.X_AXIS );
        urlHead = new JLabel( " External URL: " );
        urlInfo = new JTextField();
        urlInfo.setEditable( false );
        infoBox.add( urlHead );
        infoBox.add( urlInfo );
        externalURL( null );
        getContentPane().add( infoBox, BorderLayout.SOUTH );

        /* Create the HelpSet if there is not already one. */
        try {
            URL hsResource = HelpWindow.class.getResource( HELPSET_LOCATION );
            hset = new HelpSet( null, hsResource );
            jhelp = new JHelp( hset );
            jhelp.setPreferredSize( new Dimension( 700, 500 ) );
            helpComponent = jhelp;

            /* Fiddle around with presentation. */
            openTOC( jhelp );

            /* Add a listener which can inform about the location of
             * external URLs. */
            JHelpContentViewer cview = jhelp.getContentViewer();
            cview.addHelpModelListener( new HelpModelListener() {
                public void idChanged( HelpModelEvent evt ) {
                    URL url = evt.getID() == null ? evt.getURL() : null;
                    externalURL( url );
                }
             } );

            /* Vague hope this might load images synchronously (to make 
             * display less jerky) but don't think it does. */
            cview.setSynch( true );

            /* Muck about with toolbars. */
            pinchHelpToolBarTools( jhelp );
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

        /* Even a help window needs help. */
        addHelp( "HelpWindow" );

        /* Install it into this window. */
        getContentPane().add( helpComponent );

        /* Make visible. */
        pack();
        setVisible( true );
    }

    /**
     * Returns the sole instance of this class.  
     * If supplied, a <tt>parent</tt> window is used only for positioning.
     *
     * @param  parent  parent window
     */
    public static HelpWindow getInstance( Component parent ) {
        if ( instance == null ) {
            instance = new HelpWindow( parent );
        }
        return instance;
    }

    /**
     * Returns the JHelp window which does the viewing of the pages in 
     * this window.
     *
     * @return  the JHelp component
     */
    public JHelp getJHelp() {
        return jhelp;
    }

    /**
     * Sets the current help ID to the one represented by the given string.
     * If <tt>helpID</tt> is <tt>null</tt>, or does not refer to a 
     * real ID in this HelpSet, no change is made to the current view.
     *
     * @param   helpID  the ID to change to
     */
    public void setID( String helpID ) {
        if ( hset != null && helpID != null ) {
            javax.help.Map.ID mapID = javax.help.Map.ID.create( helpID, hset );
            if ( mapID != null ) {
                try {
                    jhelp.setCurrentID( mapID );

                    /* Tweak terminally ugly default font. */
                    if ( ! fontSet ) {
                        Font font = UIManager.getFont( "TextField.font" );
                        if ( font != null ) {
                            jhelp.setFont( font );
                        }
                    }
                }
                catch ( InvalidHelpSetContextException e ) {
                    logger.info( "Bad help ID: " + helpID );
                }
            }
            else {
                logger.info( "Unknown help ID: " + helpID );
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
            List tools = new ArrayList();
            for ( int i = 0; ; i++ ) {
                Component tool = jhBar.getComponentAtIndex( i );
                if ( tool == null ) {
                    break;
                }
                tools.add( tool );
            }

            /* Move each one from the help toolbar to this window's toolbar. */
            for ( Iterator it = tools.iterator(); it.hasNext(); ) {
                Component tool = (Component) it.next();
                jhBar.remove( tool );
                getToolBar().add( tool );
            }
            getToolBar().addSeparator();

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
        urlHead.setEnabled( isActive );
        urlInfo.setText( isActive ? url.toString() : null );
    }

    private static void openTOC( JHelp jhelp ) {
        JHelpTOCNavigator tocnav = null;
        for ( Enumeration en = jhelp.getHelpNavigators();
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
        for ( Enumeration en = hs.getCombinedMap().getAllIDs();
              en.hasMoreElements(); ) {
            String id = ((javax.help.Map.ID) en.nextElement()).id;
            tocnav.expandID( id );
        }
    }
    
}
