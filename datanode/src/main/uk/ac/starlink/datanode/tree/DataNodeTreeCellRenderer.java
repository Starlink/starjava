package uk.ac.starlink.datanode.tree;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.NodeUtil;

/**
 * TreeCellRenderer designed for rendering {@link DataNode} values.
 */
public class DataNodeTreeCellRenderer extends DefaultTreeCellRenderer {

    private Font normalFont;
    private Font expandingFont;

    public Component getTreeCellRendererComponent( JTree jtree, Object value,
                                                   boolean selected,
                                                   boolean expanded,
                                                   boolean leaf, int row,
                                                   boolean hasFocus ) {

        /* Use the superclass to construct a default component. */
        super.getTreeCellRendererComponent( jtree, value, selected, 
                                            expanded, leaf, row, hasFocus );

        /* Customise it according to the data, if we have a DataNode. */
        if ( value instanceof DataNode ) {
            DataNode node = (DataNode) value;
            setIcon( node.getIcon() );
            String text = NodeUtil.toString( node );
            if ( text.trim().length() == 0 ) {
                text = "...";
            }
            setText( text );
            DataNodeTreeModel treeModel = (DataNodeTreeModel) jtree.getModel();
            boolean isExpanding = false;
            if ( treeModel.containsNode( node ) ) {
                TreeModelNode modelNode = treeModel.getModelNode( node );
                NodeExpander expander = modelNode.getExpander();
                isExpanding = expander != null && ! expander.isStopped();
            }
            configureNode( node, isExpanding );
        }

        /* Return the configured label. */
        return this;
    }

    /**
     * Performs additional configuration on the rendered cell based 
     * on the node and its expanding status.  This method is called
     * from <code>getTreeCellRendererComponent</code> and may be 
     * overridden by subclasses to provide additional visual control
     * over the rendering.
     *
     * @param  node  the DataNode being rendered
     * @param  isExpanding  whether <code>node</code> is currently in the
     *         process of expanding
     */
    protected void configureNode( DataNode node, boolean isExpanding ) {
        if ( normalFont == null ) {
            normalFont = getFont();
            expandingFont = normalFont.deriveFont( Font.ITALIC );
        }
        setFont( isExpanding ? expandingFont : normalFont );
    }
}
