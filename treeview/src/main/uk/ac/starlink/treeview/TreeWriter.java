package uk.ac.starlink.treeview;

import java.io.PrintStream;
import java.util.Iterator;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.NodeUtil;

/**
 * Generates a textual representation of a tree of 
 * {@link DataNode}s.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class TreeWriter {
    PrintStream stream;
    boolean showPath;

    /**
     * Construct a <code>TreeWriter</code> which writes to a given stream,
     * optionally recording the full path of each node.
     *
     * @param  stream  the <code>PrintStream</code> to which the tree
     *                 will be written.
     * @param  showPath  whether the path of each node is to be appended
     *                   to the output
     */
    public TreeWriter( PrintStream stream, boolean showPath ) {
        this.stream = stream;
        this.showPath = showPath;
    }

    /**
     * Construct a <code>TreeWriter</code> which writes to standard output,
     * without paths.
     */
    public TreeWriter() {
        this( System.out, false );
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
            StringBuffer line = new StringBuffer()
                .append( indent( level, hasChildren ) )
                .append( '[' )
                .append( ( node.getNodeTLA() + "   " ).substring( 0, 3 ) )
                .append( "] " )
                .append( node );
            if ( showPath ) {
                String path = NodeUtil.getNodePath( node );
                if ( path != null ) {
                    line.append( "      " )
                        .append( path );
                }
            }
            outLine( line.toString() );
        }
        if ( hasChildren ) {
            while ( cIt.hasNext() ) {
                processNode( level + 1, (DataNode) cIt.next() );
            }
        }
    }
}
