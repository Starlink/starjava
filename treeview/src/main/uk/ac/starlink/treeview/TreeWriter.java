package uk.ac.starlink.treeview;

import java.util.*;
import java.io.*;

/**
 * Generates a textual representation of a tree of 
 * {@link DataNode}s.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class TreeWriter {
    PrintStream stream;

    /**
     * Construct a <code>TreeWriter</code> which writes to a given stream.
     *
     * @param  stream  the <code>PrintStream</code> to which the tree
     *                 will be written.
     */
    public TreeWriter( PrintStream stream ) {
        this.stream = stream;
    }

    /**
     * Construct a <code>TreeWriter</code> which writes to standard output.
     */
    public TreeWriter() {
        this( System.out );
    }
 
    private void outLine( String line ) {
        stream.println( line );
    }

    private String indent( int level, boolean hasChildren ) {
        String result = "";
        for ( int i = 0; i < level; i++ ) {
            result += "  ";
        }
        result += hasChildren ? "+ " : "- ";
        return result;
    }

    /**
     * Write a textual representation of the given node.
     *
     * @param  node  a <code>DataNode</code> representing the top of the tree
     *               to write.  The node itself will not be written.
     */
    public void write( DataNode node ) {
        processNode( 0, node );
    }

    private void processNode( int level, DataNode node ) {
        boolean hasChildren = false;
        Iterator cIt = null;
        if ( node.allowsChildren() ) {
            cIt = node.getChildIterator();
            hasChildren = cIt.hasNext();
        }
        if ( level > 0 ) {
            outLine( indent( level, hasChildren ) +
                        "[" + ( node.getNodeTLA() + "   " ).substring( 0, 3 ) +
                        "] " + node );
        }
        if ( hasChildren ) {
            while ( cIt.hasNext() ) {
                processNode( level + 1, (DataNode) cIt.next() );
            }
        }
    }
}
