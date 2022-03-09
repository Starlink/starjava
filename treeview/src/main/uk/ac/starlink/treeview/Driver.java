package uk.ac.starlink.treeview;

import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.datanode.nodes.ARYDataNode;
import uk.ac.starlink.datanode.nodes.CompressedDataNode;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.DefaultDataNode;
import uk.ac.starlink.datanode.nodes.FileDataNode;
import uk.ac.starlink.datanode.nodes.FITSDataNode;
import uk.ac.starlink.datanode.nodes.FtpDirectoryDataNode;
import uk.ac.starlink.datanode.nodes.HDSDataNode;
import uk.ac.starlink.datanode.nodes.HDXDataNode;
import uk.ac.starlink.datanode.nodes.JDBCDataNode;
import uk.ac.starlink.datanode.nodes.NDArrayDataNode;
import uk.ac.starlink.datanode.nodes.NDFDataNode;
import uk.ac.starlink.datanode.nodes.NdxDataNode;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;
import uk.ac.starlink.datanode.nodes.NodeUtil;
import uk.ac.starlink.datanode.nodes.PlainDataNode;
import uk.ac.starlink.datanode.nodes.StarTableDataNode;
import uk.ac.starlink.datanode.nodes.TarStreamDataNode;
import uk.ac.starlink.datanode.nodes.VOTableDataNode;
import uk.ac.starlink.datanode.nodes.WCSDataNode;
import uk.ac.starlink.datanode.nodes.XMLDataNode;
import uk.ac.starlink.datanode.nodes.ZipFileDataNode;
import uk.ac.starlink.datanode.nodes.ZipStreamDataNode;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.hds.NDFNdxHandler;
import uk.ac.starlink.oldfits.FitsNdxHandler;
import uk.ac.starlink.util.Loader;
import uk.ac.starlink.util.URLUtils;

public class Driver {
    public final static String CMDNAME_PROPERTY =
        "uk.ac.starlink.treeview.cmdname";

    private static Logger logger = 
        Logger.getLogger( "uk.ac.starlink.treeview" );

    public static void main( String[] args ) {
        boolean textView = false;
        boolean textPath = false;
        short orient = StaticTreeViewer.DETAIL_BESIDE;

        /* Ensure that global preferences are installed. */
        Loader.loadProperties();
        Loader.tweakGuiForMac();
        URLUtils.installCustomHandlers();

        /* Ensure we have best guesses for various properties. */
        guessProperties();

        /* Set logging level to warning. */
        Logger.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );

        /* Get the name we are running under. */
        String cmdName = 
            System.getProperty( CMDNAME_PROPERTY );
        if ( cmdName == null ) {
            cmdName = "treeview";
        }

        /* Check requisites.  We may be able to proceed without JNIAST
         * and JNIHDS, but we should warn up front that their absence
         * is likely to lead to problems. */
        boolean hasAST = NodeUtil.hasAST();
        boolean hasHDS = NodeUtil.hasHDS();

        /* Set up a Map mapping flags to expected Node type of argument. */
        Map nodeTypeFlags = new HashMap();
        if ( hasHDS ) {
            nodeTypeFlags.put( "-hds", HDSDataNode.class );
        }
        nodeTypeFlags.put( "-file", FileDataNode.class );
        nodeTypeFlags.put( "-plain", PlainDataNode.class );
        nodeTypeFlags.put( "-comp", CompressedDataNode.class );
        nodeTypeFlags.put( "-jdbc", JDBCDataNode.class );
        nodeTypeFlags.put( "-table", StarTableDataNode.class );
        nodeTypeFlags.put( "-src", PlainDataNode.class );
        if ( hasHDS ) {
            nodeTypeFlags.put( "-ary", ARYDataNode.class );
            nodeTypeFlags.put( "-ndf", NDFDataNode.class );
        }
        if ( hasAST ) {
            nodeTypeFlags.put( "-wcs", WCSDataNode.class );
        }
        nodeTypeFlags.put( "-zip", ZipFileDataNode.class );
        nodeTypeFlags.put( "-zips", ZipStreamDataNode.class );
        nodeTypeFlags.put( "-tar", TarStreamDataNode.class );
        nodeTypeFlags.put( "-fit", FITSDataNode.class );
        nodeTypeFlags.put( "-xml", XMLDataNode.class );
        nodeTypeFlags.put( "-hdx", HDXDataNode.class );
        nodeTypeFlags.put( "-ndx", NdxDataNode.class );
        nodeTypeFlags.put( "-vot", VOTableDataNode.class );
        nodeTypeFlags.put( "-nda", NDArrayDataNode.class );
        nodeTypeFlags.put( "-ftp", FtpDirectoryDataNode.class );

        /* Construct the usage message. */
        String usageMsg = 
              "Usage: " + cmdName +
            "\n         [-demo] [-text [-path]] [-strict] [-debug] " +
                       "[-split(x|y|0)]" +
            "\n        ";
        Iterator flagIt = nodeTypeFlags.keySet().iterator();
        while ( flagIt.hasNext() ) {
            usageMsg += " [" + flagIt.next().toString() + "]";
        }
        usageMsg +=       "\n         [item ...]\n";

        /* Ensure that we will load the right HDX factories. */
        System.setProperty( "HdxDocumentFactory.load." +
                            FitsNdxHandler.class.getName(), "true" );
        if ( hasHDS ) {
            System.setProperty( "HdxDocumentFactory.load." +
                                NDFNdxHandler.class.getName(), "true" );
        }

        /* Construct the factory which will build the requested DataNodes. */
        final DataNodeFactory nodeFactory = new DataNodeFactory();

        /* Process arguments. */
        int iarg;
        final List topNodes = new ArrayList( args.length );
        for ( iarg = 0; iarg < args.length; iarg++ ) {
            String arg = args[ iarg ];

            /* Process flag argument. */
            if ( arg.charAt( 0 ) == '-' ) {
                if ( arg.equals( "-text" ) ) {
                    textView = true;
                }
                else if ( arg.equals( "-path" ) ) {
                    textPath = true;
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
                    List builders = nodeFactory.getBuilders();
                    nodeFactory.getBuilders().removeAll( builders );
                }
                else if ( arg.equals( "-debug" ) ) {
                    nodeFactory.setDebug( true );
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

        /* Make sure that the top-level nodes have the correct custom
         * child node creation factory. */
        for ( Iterator it = topNodes.iterator(); it.hasNext(); ) {
            DataNode node = (DataNode) it.next();
            node.setChildMaker( nodeFactory );
        }

        /* Construct root as a DefaultDataNode able also to bear children. */
        DataNode root = new DefaultDataNode() {
            public boolean allowsChildren() {
                return true;
            }
            public Iterator getChildIterator() {
                return topNodes.iterator();
            }
            public DataNodeFactory getChildMaker() {
                return nodeFactory;
            }
        };

        /* View the tree. */
        if ( textView ) {
            NodeUtil.setGUI( false );
            viewAsText( root, textPath );
        }
        else {
            NodeUtil.setGUI( true );
            viewAsGUI( root, orient );
        }
    }

    /*
     * Create and display the viewer object.
     */
    private static void viewAsGUI( final DataNode root, final short orient ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                JFrame tv = new StaticTreeViewer( root, "Starlink Treeview",
                                                  orient );
                tv.addWindowListener( new WindowAdapter() {
                    public void windowClosing( WindowEvent e ) {
                        System.exit( 0 );
                    }
                } );
                tv.setVisible( true );
            }
        } );
    }

    public static void viewAsText( DataNode root, boolean showPath ) {
        TreeWriter tw = new TreeWriter( System.out, showPath );
        tw.write( root );
    }

    /**
     * Make a node from a string argument, and exit the JVM gracefully
     * if it can't be done.
     */
    private static DataNode makeDataNode( DataNodeFactory nodeFactory, 
                                          String nodename ) {
        try {
            DataNode node = nodeFactory.makeDataNode( null, nodename );
            node.setLabel( nodename );
            return node;
        }
        catch ( NoSuchDataException e ) {
            StringBuffer msg = new StringBuffer();
            msg.append( "\nNo such object " )
               .append( '"' )
               .append( nodename )
               .append( '"' );
            exitWithError( msg.toString() );
            throw new AssertionError();
        }
    }

    /**
     * Gets values for various properties.  With luck these will have
     * been set by whoever invoked this driver, but in the case that
     * they haven't make pretty-good guesses about them.
     */
    private static void guessProperties() {
        Loader.loadProperties();
        Properties props = System.getProperties();
        String prefix = "uk.ac.starlink.treeview.";
        String sc = "" + File.separatorChar;

        String cmdnameProp = prefix + "cmdname";
        if ( ! props.containsKey( cmdnameProp ) ) {
            props.setProperty( cmdnameProp, "treeview" );
        }

        try {
            File sdir = Loader.starjavaDirectory();
            if ( sdir != null ) {
                String stardir = sdir.toString() + sc;
                String demodirProp = prefix + "demodir";
                if ( ! props.containsKey( demodirProp ) ) {
                    props.setProperty( demodirProp, stardir + "etc" + sc + 
                                       "treeview" + sc + "demo" );
                }
            }
        } catch (Exception e) {
            //  Not fatal, but happens when using webstart (should
            //  move these files into a jar and use getResource()).
            System.err.println( "Failed to locate Treeview etc. directory" );
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
