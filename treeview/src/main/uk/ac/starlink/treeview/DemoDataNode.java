package uk.ac.starlink.treeview;

import java.io.File;
import java.util.Iterator;
import uk.ac.starlink.datanode.factory.CreationState;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.DetailViewer;
import uk.ac.starlink.datanode.nodes.FileDataNode;
import uk.ac.starlink.datanode.nodes.NdxDataNode;
import uk.ac.starlink.datanode.nodes.IconFactory;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;

/**
 * A DataNode implementation which displays Treeview's known demo data
 * directory.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DemoDataNode extends FileDataNode {

    public static final String DEMO_DIR_PROPERTY = 
        "uk.ac.starlink.treeview.demodir";

    private String name = "Demonstration data";

    public DemoDataNode() throws NoSuchDataException {
        super( getDemoDir() );
        setLabel( name );
        setCreator( new CreationState( null, null ) );
        setIconID( IconFactory.DEMO );
    }

    public String getName() {
        return name;
    }

    public String getPathElement() {
        return "[DemoData]";
    }

    public String getPathSeparator() {
        return ":";
    }

    public Object getParentObject() {
        return null;
    }

    public String getNodeTLA() {
        return "DEM";
    }

    public String getNodeType() {
        return "Demonstration data";
    }

    public Iterator getChildIterator() {
        final Iterator it = super.getChildIterator();
        return new Iterator() {
            public boolean hasNext() {
                return it.hasNext();
            }
            public Object next() {
                DataNode node = (DataNode) it.next();
                if ( node.getName().equals( "ndx" ) ) {
                    DataNodeFactory maker =
                        new DataNodeFactory( node.getChildMaker() );
                    maker.setPreferredClass( NdxDataNode.class );
                    node.setChildMaker( maker );
                }
                return node;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void configureDetail( DetailViewer dv ) {
        super.configureDetail( dv );
        dv.addPane( "Information", 
                    new HTMLDocComponentMaker( "demo.html" ) );

        /* Display the info panel not the overview immediately to make it
         * very clear to users what is going on. */
        if ( dv instanceof ApplicationDetailViewer ) {
            ((ApplicationDetailViewer) dv).setSelectedIndex( 1 );
        }
    }

    /**
     * Returns the demo data directory.
     */
    public static File getDemoDir() throws NoSuchDataException {
        try {
            String demoloc = System.getProperty( DEMO_DIR_PROPERTY );
            if ( demoloc == null || demoloc.trim().length() == 0 ) {
                throw new NoSuchDataException( "No demo data available" );
            }
            File demodir = new File( demoloc );
            if ( ! demodir.canRead() ) {
                throw new NoSuchDataException( "Demo data directory " 
                                               + demodir + 
                                               " is not readable" );
            }
            return demodir;
        }
        catch ( SecurityException e ) {
            throw new NoSuchDataException( e.getMessage(), e );
        }
    }
}
