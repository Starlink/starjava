package uk.ac.starlink.treeview;

import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

/**
 * Generates help text for the StaticTreeViewer.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
class StaticTreeViewerHelp extends JEditorPane {
    static StaticTreeViewerHelp help;
    static JScrollPane helpscroller;
    public static final String DOCS_PATH = "uk/ac/starlink/treeview/docs/";
    private static URL sunbase;
    private static Document basedoc;
    private static Document sundoc;
    private static EditorKit defaultkit;
    private static HTMLEditorKit htmlkit;
    final static String SUN_TOP = "sun244.html#stardoccontents";
    public final static String 
        SUNBASE_PROPERTY = "uk.ac.starlink.treeview.sunbase";
    public final static String
        STARDIR_PROPERTY = "uk.ac.starlink.treeview.stardir";


    private StaticTreeViewerHelp() {
        setEditable( false );
        defaultkit = getEditorKit();
        boolean success = false;
        String hname = DOCS_PATH + "treeview.html";
        ClassLoader loader = this.getClass().getClassLoader();
        InputStream helpstrm = loader.getResourceAsStream( hname );
        if ( helpstrm != null ) {
            try {

                /* Configure the EditorPane to read and display HTML. */
                htmlkit = new HTMLEditorKit();
                try {
                    StyleSheet css = new StyleSheet();
                    String cssname = DOCS_PATH + "help.css";
                    InputStream cssstrm = loader.getResourceAsStream( cssname );
                    css.loadRules( new InputStreamReader( cssstrm ), null );
                    htmlkit.setStyleSheet( css );
                }
                catch ( Exception e ) {
                }
                setEditorKit( htmlkit );

                /* Load in the HTML document. */
                StyleSheet css = htmlkit.getStyleSheet();
                HTMLDocument hdoc = new HTMLDocument( css );
                hdoc.setBase( sunbase );
                read( helpstrm, hdoc );
                basedoc = getDocument();
                success = true;
            }
            catch ( IOException e ) {
            }

            /* Arrange for activated hyperlinks to load a new document. */
            addHyperlinkListener( new HyperlinkListener() {
                public void hyperlinkUpdate( HyperlinkEvent evt ) {
                    if ( evt.getEventType() ==
                         HyperlinkEvent.EventType.ACTIVATED ) {
                        URL url = evt.getURL();
                        if ( sunbase != null && 
                             url.toString().startsWith( sunbase.toString() ) ) {
                            try {
                                setPage( evt.getURL() );
                            }
                            catch ( IOException e ) {
                                System.out.println( "Document " + evt.getURL() +
                                                    " not found" );
                            }
                        }
                        else if ( sunbase == null ) {
                            System.out.println( "User document not available" );
                            help.getToolkit().beep();
                        }
                        else {
                            /* We don't allow access outside this document. */
                            help.getToolkit().beep();
                        }
                    }
                }
            } );

            StyleSheet styles = ( (HTMLEditorKit) getEditorKit() )
                               .getStyleSheet();
            java.util.Enumeration rules = styles.getStyleNames();
            while (rules.hasMoreElements()) {
                String name = (String) rules.nextElement();
                Style rule = styles.getStyle(name);
            }

        }

        /* Reading the HTML document failed - present an apology. */
        if ( ! success ) {
            setEditorKit( defaultkit );
            setText( "No help text is available\n" );
        }
    }

    /**
     * Returns a JComponent representing the help information for the
     * StaticTreeViewer widget.  If there is some sort of error in
     * obtaining it, a valid JComponent indicating this fact in a 
     * human-readable form will be returned.
     */
    static JComponent getHelp() {
        if ( help == null ) {
            init();
        }
        if ( basedoc != null ) {
            help.setEditorKit( htmlkit );
            help.setDocument( basedoc );
        }
        return helpscroller;
    }

    static JComponent getSun() {
        if ( help == null ) {
            init();
        }
        if ( sunbase != null ) {
            if ( sundoc == null ) {
                try {
                    help.setPage( new URL( sunbase, SUN_TOP ) );
                    sundoc = help.getDocument();
                }
                catch ( Exception e ) {
                   help.setEditorKit( defaultkit );
                   help.setText( "No user document is available\n" );
                }
            }
            else {
                help.setEditorKit( htmlkit );
                help.setDocument( sundoc );
            }
        }
        return helpscroller;
    }

    private static void init() {
        /* Set up the base URL for resolving hyperlinks. */
        sunbase = null;
        try{
            String propval = System.getProperty( SUNBASE_PROPERTY );
            if ( propval != null ) {
                sunbase = new URL( "file", "localhost", propval );
            }
            else {
                String stardir = 
                    System.getProperty( STARDIR_PROPERTY );
                if ( stardir == null ) {
                    stardir = "/star";
                }
                sunbase = new URL( "file", "localhost", 
                                   stardir + "/docs/sun244.htx/" );
            }
        }
        catch ( MalformedURLException e ) {
            sunbase = null;
        }
        help = new StaticTreeViewerHelp();
        helpscroller = new JScrollPane( help );
    }

}
