package uk.ac.starlink.treeview;

import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import uk.ac.starlink.ast.AstPackage;
import uk.ac.starlink.hds.HDSPackage;

public class Driver {
    public final static String CMDNAME_PROPERTY =
        "uk.ac.starlink.treeview.cmdname";

    public static boolean hasAST;
    public static boolean hasHDS;

    public static void main( String[] args ) {
        boolean textView = false;
        short orient = StaticTreeViewer.DETAIL_BESIDE;

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

        /* Treat the special case in which no command-line arguments are
         * specified and give a very simple usage message. */
        if ( args.length == 0 ) {
            exitWithError( "Usage: " + cmdName + "[flags] item ..." );
        }
     
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
        nodeTypeFlags.put( "-nda", NDArrayDataNode.class );

        /* Construct the usage message. */
        String usageMsg = 
              "Usage: " + cmdName +
            "\n         [-text] [-strict] [-debug] [-split(x|y|0)]" +
            "\n         ";
        Iterator flagIt = nodeTypeFlags.keySet().iterator();
        while ( flagIt.hasNext() ) {
            usageMsg += " [" + flagIt.next().toString() + "]";
        }
        usageMsg +=       "\n         item ...\n";

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
                try {
                    DataNode node = nodeFactory.makeDataNode( arg );
                    node.setLabel( arg );
                    topNodes.add( node );
                }
                catch ( NoSuchDataException e ) {
                    String msg = "\nNo such object \"" + arg + "\"\n";
                    msg += "(tried";
                    List tried = nodeFactory.getClassesTried();
                    for ( Iterator it = tried.iterator(); it.hasNext(); ) {
                        msg += it.toString()
                             + ( it.hasNext() ? ":" : "" );
                    }
                    msg += ".";
                    exitWithError( msg );
                    throw new Error();  // not reached
                }
            }
        }

        /* Make a tree out of all the node arguments we got. */
        DefaultDataNode root;
        final DataNode[] topChildren = 
                (DataNode[]) topNodes.toArray( new DataNode[ 0 ] );
        if ( topNodes.size() > 0 ) {

            /* Construct root as a DefaultDataNode able also to bear 
             * children. */
            root = new DefaultDataNode() {
                private DataNode[] children = topChildren;
                public boolean allowsChildren() {
                    return true;
                }
                public DataNode[] getChildren() {
                    return children;
                }
            };
        }
        else {
            root = null;
            exitWithError( usageMsg );
        }

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
        StaticTreeViewer tv = new StaticTreeViewer( root, "Treeview", orient );
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
