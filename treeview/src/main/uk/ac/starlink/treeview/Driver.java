package uk.ac.starlink.treeview;

import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.hds.HDSObject;

public class Driver {
    public final static String CMDNAME_PROPERTY =
        "uk.ac.starlink.treeview.cmdname";

    public static void main( String[] args ) throws NoSuchDataException,
                                                    ClassNotFoundException {
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
        try {
            double b = AstObject.AST__BAD;
        }
        catch ( UnsatisfiedLinkError e ) {
            System.err.println( 
                "WARNING: Requisite package JNIAST (uk.ac.starlink.ast) " +
                "is not installed" );
        }
        try {
            int s = HDSObject.DAT__SZNAM;
        }
        catch ( UnsatisfiedLinkError e ) {
            System.err.println(
                "WARNING: Requisite package JNIHDS (uk.ac.starlink.hds) " +
                "is not installed" );
        }

        /* Treat the special case in which no command-line arguments are
         * specified and give a very simple usage message. */
        if ( args.length == 0 ) {
            exitWithError( "Usage: " + cmdName + "[flags] item ..." );
        }
     
        /* Set up a HashMap mapping flags to expected Node type of argument. */
        HashMap nodeTypeFlags = new HashMap();
        nodeTypeFlags.put( "-hds", HDSDataNode.class );
        nodeTypeFlags.put( "-file", FileDataNode.class );
        nodeTypeFlags.put( "-ary", ARYDataNode.class );
        nodeTypeFlags.put( "-ndf", NDFDataNode.class );
        nodeTypeFlags.put( "-wcs", WCSDataNode.class );
        nodeTypeFlags.put( "-zip", ZipFileDataNode.class );
        nodeTypeFlags.put( "-fit", FITSDataNode.class );
        // nodeTypeFlags.put( "-xml", XMLDocumentDataNode.class );
        // nodeTypeFlags.put( "-hdx", HDXContainerDataNode.class );
        // nodeTypeFlags.put( "-ndx", NDXDataNode.class );
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
                    nodeFactory.setNodeClassList( new Class[ 0 ] );
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
                    Class[] tried = nodeFactory.getClassesTried();
                    for ( int i = 0; i < tried.length; i++ ) {
                        String cname = tried[ i ].getName();
                        msg += " " 
                             + cname.substring( cname.lastIndexOf( '.' ) + 1,
                                                cname.indexOf( "DataNode" ) )
                             + ( ( i == tried.length - 1 ) ? ")" : "," );
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
