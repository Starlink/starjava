package uk.ac.starlink.treeview;

import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;
import uk.ac.starlink.ast.AstPackage;
import uk.ac.starlink.hds.HDSPackage;
import uk.ac.starlink.util.Loader;

public class Driver {
    public final static String CMDNAME_PROPERTY =
        "uk.ac.starlink.treeview.cmdname";

    public static boolean hasAST;
    public static boolean hasHDS;
    public static boolean hasJAI;

    private static Logger logger = 
        Logger.getLogger( "uk.ac.starlink.treeview" );

    public static void main( String[] args ) {
        boolean textView = false;
        short orient = StaticTreeViewer.DETAIL_BESIDE;

        /* Ensure we have best guesses for various properties. */
        guessProperties();

        /* Get the name we are running under. */
        String cmdName = 
            System.getProperty( CMDNAME_PROPERTY );
        if ( cmdName == null ) {
            cmdName = "uk.ac.starlink.treeview.Driver";
        }

        /* Check requisites.  We may be able to proceed without JNIAST
         * and JNIHDS, but we should warn up front that their absence
         * is likely to lead to problems. */
        hasAST = AstPackage.isAvailable();
        hasHDS = HDSPackage.isAvailable();

        /* Set up a HashMap mapping flags to expected Node type of argument. */
        HashMap nodeTypeFlags = new HashMap();
        if ( hasHDS ) {
            nodeTypeFlags.put( "-hds", HDSDataNode.class );
        }
        nodeTypeFlags.put( "-file", FileDataNode.class );
        if ( hasHDS ) {
            nodeTypeFlags.put( "-ary", ARYDataNode.class );
            nodeTypeFlags.put( "-ndf", NDFDataNode.class );
        }
        if ( hasAST ) {
            nodeTypeFlags.put( "-wcs", WCSDataNode.class );
        }
        nodeTypeFlags.put( "-zip", ZipFileDataNode.class );
        nodeTypeFlags.put( "-fit", FITSDataNode.class );
        nodeTypeFlags.put( "-xml", XMLDataNode.class );
        // nodeTypeFlags.put( "-hdx", HDXContainerDataNode.class );
        nodeTypeFlags.put( "-ndx", NdxDataNode.class );
        nodeTypeFlags.put( "-vot", VOTableDataNode.class );
        nodeTypeFlags.put( "-nda", NDArrayDataNode.class );

        /* Construct the usage message. */
        String usageMsg = 
              "Usage: " + cmdName +
            "\n         [-demo] [-text] [-strict] [-debug] [-split(x|y|0)]" +
            "\n         ";
        Iterator flagIt = nodeTypeFlags.keySet().iterator();
        while ( flagIt.hasNext() ) {
            usageMsg += " [" + flagIt.next().toString() + "]";
        }
        usageMsg +=       "\n         [item ...]\n";

        /* Construct the factory which will build the requested DataNodes. */
        DataNodeFactory nodeFactory = new DataNodeFactory();

        /* Process arguments. */
        int iarg;
        Vector topNodes = new Vector( args.length );
        for ( iarg = 0; iarg < args.length; iarg++ ) {
            String arg = args[ iarg ];

            /* Process flag argument. */
            if ( arg.charAt( 0 ) == '-' ) {
                if ( arg.equals( "-text" ) ) {
                    textView = true;
                }
                else if ( arg.equals( "-splitx" ) ) {
                    orient = StaticTreeViewer.DETAIL_BESIDE;
                }
                else if ( arg.equals( "-splity" ) ) {
                    orient = StaticTreeViewer.DETAIL_BELOW;
                }
                else if ( arg.equals( "-split0" ) ) {
                    orient = StaticTreeViewer.DETAIL_NONE;
                }
                else if ( arg.equals( "-strict" ) ) {
                    nodeFactory.setNodeClassList( new ArrayList() );
                }
                else if ( arg.equals( "-debug" ) ) {
                    nodeFactory.setVerbose( true );
                    DataNodeBuilder.verbose = true;
                }
                else if ( arg.equals( "-demo" ) ) {
                    try {
                        topNodes.add( new DemoDataNode() );
                    }
                    catch ( NoSuchDataException e ) {
                        exitWithError( e.getMessage() + "\n" );
                        throw new Error();  // not reached
                    }
                }
                else if ( nodeTypeFlags.containsKey( arg ) ) {
                    Class prefClass = (Class) nodeTypeFlags.get( arg );
                    nodeFactory.setPreferredClass( prefClass );
                }
                else {
                    exitWithError( usageMsg );
                }
            }

            /* Process node argument; the current factory settings are used. */
            else {
                topNodes.add( makeDataNode( nodeFactory, arg ) );
            }
        }

        /* If there were no nodes specified, default to the current 
         * directory. */
        if ( topNodes.size() == 0 ) {
            String dfltarg = new File( "." ).getAbsolutePath();
            if ( dfltarg.endsWith( File.separatorChar + "." ) ) {
                dfltarg = dfltarg.substring( 0, dfltarg.length() - 1 );
            }
            topNodes.add( makeDataNode( nodeFactory, dfltarg ) );
        }

        /* Check for presence of JAI if we might need it (i.e. if we are
         * running in graphical mode. */
        if ( textView ) {
            hasJAI = false;
        }
        else {
            try {
                /* Use this class because it's lightweight and won't cause a
                 * whole cascade of other classes to be loaded. */
                new javax.media.jai.util.CaselessStringKey( "dummy" );
                hasJAI = true;
            }
            catch ( NoClassDefFoundError e ) {
                hasJAI = false;
                logger.warning( 
                    "JAI extension not present - no image display" );
            }
        }

        /* Make a tree out of all the node arguments we got. */
        DefaultDataNode root;
        final DataNode[] topChildren = 
            (DataNode[]) topNodes.toArray( new DataNode[ 0 ] );

        /* Construct root as a DefaultDataNode able also to bear children. */
        root = new DefaultDataNode() {
            public boolean allowsChildren() {
                return true;
            }
            public DataNode[] getChildren() {
                return topChildren;
            }
        };

        /* View the tree. */
        if ( textView ) {
            viewAsText( root );
        }
        else {
            viewAsGUI( root, orient );
        }
    }

    /*
     * Create and display the viewer object.
     */
    private static void viewAsGUI( DataNode root, short orient ) {
        StaticTreeViewer tv = 
            new StaticTreeViewer( root, "Starlink Treeview", orient );
        tv.addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                System.exit( 0 );
            }
        } );
        tv.setVisible( true );
    }

    public static void viewAsText( DataNode root ) {
        TreeWriter tw = new TreeWriter( System.out );
        tw.write( root );
    }

    /**
     * Make a node from a string argument, and exit the JVM gracefully
     * if it can't be done.
     */
    private static DataNode makeDataNode( DataNodeFactory nodeFactory, 
                                          String nodename ) {
        try {
            DataNode node = nodeFactory.makeDataNode( nodename );
            node.setLabel( nodename );
            return node;
        }
        catch ( NoSuchDataException e ) {
            StringBuffer msg = new StringBuffer();
            msg.append( "\nNo such object " )
               .append( '"' )
               .append( nodename )
               .append( '"' )
               .append( "\nTried:\n" );
            List tried = nodeFactory.getClassesTried();
            for ( Iterator it = tried.iterator(); it.hasNext(); ) {
                msg.append( "    " )
                   .append( it.next() )
                   .append( "\n" );
            }
            msg.append( "\n" );
            exitWithError( msg.toString() );
            throw new Error();  // not reached
        }
    }

    /**
     * Gets values for various properties.  With luck these will have
     * been set by whoever invoked this driver, but in the case that
     * they haven't make pretty-good guesses about them.
     */
    private static void guessProperties() {
        Properties props = System.getProperties();
        String prefix = "uk.ac.starlink.treeview.";
        String sc = "" + File.separatorChar;

        String cmdnameProp = prefix + "cmdname";
        if ( ! props.containsKey( cmdnameProp ) ) {
            props.setProperty( cmdnameProp, "treeview" );
        }

        File sdir = Loader.starjavaDirectory();
        if ( sdir != null ) {
            String stardir = sdir.toString() + sc;
            String demodirProp = prefix + "demodir";
            if ( ! props.containsKey( demodirProp ) ) {
                props.setProperty( demodirProp, stardir + "etc" + sc + 
                                                "treeview" + sc + "demo" );
            }

            String sogdirProp = prefix + "sogdir";
            if ( ! props.containsKey( sogdirProp ) ) {
                props.setProperty( sogdirProp, stardir + "bin" + sc + "sog" );
            }

            String splatdirProp = prefix + "splatdir";
            if ( ! props.containsKey( splatdirProp ) ) {
                props.setProperty( splatdirProp, stardir + "bin" + 
                                                 sc + "splat" );
            }
        }
    }


    private static void exitWithError( String msg ) {
        System.err.println();
        for ( StringTokenizer st = new StringTokenizer( msg, "\n" );
              st.hasMoreTokens(); ) {
            System.err.println( "   " + st.nextToken() );
        }
        System.err.println();
        System.exit( 1 );
    }
}
