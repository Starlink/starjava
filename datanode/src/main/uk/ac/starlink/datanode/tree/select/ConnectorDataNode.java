package uk.ac.starlink.datanode.tree.select;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.Iterator;
import javax.swing.Icon;
import uk.ac.starlink.connect.Branch;
import uk.ac.starlink.connect.Connection;
import uk.ac.starlink.connect.Connector;
import uk.ac.starlink.connect.ConnectorAction;
import uk.ac.starlink.datanode.nodes.BranchDataNode;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.DefaultDataNode;

/**
 * DataNode which represents a ConnectorAction.  It is schizophrenic;
 * if the action's connection is open it behaves like the connection's
 * root node, otherwise it just presents its connector name and has 
 * no children.
 *
 * @author   Mark Taylor (Starlink)
 */
class ConnectorDataNode extends DefaultDataNode
                        implements PropertyChangeListener {

    private final ConnectorAction connAct_;
    private final Connector connector_;
    private DataNode rootNode_;

    /**
     * Constructor.
     *
     * @param   connAct  action on which this node is based
     */
    public ConnectorDataNode( ConnectorAction connAct ) {
        connAct_ = connAct;
        connector_ = connAct.getConnector();
        setLabel( connector_.getName() );
        connAct_.addPropertyChangeListener( this );
    }

    public String getName() {
        return rootNode_ == null ? connector_.getName()
                                 : rootNode_.getName();
    }

    public ConnectorAction getConnectorAction() {
        return connAct_;
    }

    public Icon getIcon() {
        return rootNode_ == null ? connector_.getIcon()
                                 : rootNode_.getIcon();
    }

    public String getNodeTLA() {
        return "CON";
    }

    public String getNodeType() {
        return "Remote Connector";
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        return rootNode_ == null 
             ? Arrays.asList( new DataNode[ 0 ] ).iterator()
             : rootNode_.getChildIterator();
    }

    /**
     * Property change implementation.  Makes sure that this object
     * knows when the connection goes up or down.  Note this does not
     * by itself ensure that any tree containing the node notices
     * the change.
     */
    public void propertyChange( PropertyChangeEvent evt ) {
        if ( evt.getPropertyName()
                .equals( ConnectorAction.CONNECTION_PROPERTY ) ) {
            Connection conn = connAct_.getConnection();
            if ( conn == null ) {
                rootNode_ = null;
            }
            else {
                Branch root = conn.getRoot();
                rootNode_ = getChildMaker().makeChildNode( null, root );
            }
        }
    }
}
