package uk.ac.starlink.datanode.nodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.datanode.factory.CreationState;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.ResourceDataSource;

/**
 * A DataNode representing a list of resources available from the class path.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ResourceListDataNode extends DefaultDataNode {

    private List sources;

    /**
     * Constructs a new node whose children are the system resources
     * named in a given list.
     *
     * @param  resources  a list of strings naming resources available
     *         from the class path
     */
    public ResourceListDataNode( List resources ) {
        super( "Resources" );
        sources = new ArrayList( resources.size() );
        for ( Iterator it = resources.iterator(); it.hasNext(); ) {
            sources.add( new ResourceDataSource( (String) it.next() ) );
        }
        setCreator( new CreationState( null, null ) );
    }

    public String getNodeTLA() {
        return "RES";
    }

    public String getNodeType() {
        return "Resource set";
    }

    public String getPathElement() {
        return "[Resources]";
    }

    public String getPathSeparator() {
        return ":";
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        return new Iterator() {
            Iterator it = sources.iterator();
            public Object next() {
                DataSource datsrc = (DataSource) it.next();
                DataNode node = makeChild( datsrc );
                node.setLabel( datsrc.getName() );
                return node;
            }
            public boolean hasNext() {
                return it.hasNext();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
