package uk.ac.starlink.treeview;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * TreeCellRenderer designed for rendering {@link DataNode} values.
 */
public class DataNodeTreeCellRenderer extends DefaultTreeCellRenderer {

    private Font normalFont;
    private Font specialFont;

    public Component getTreeCellRendererComponent( JTree jtree, Object value,
                                                   boolean selected,
                                                   boolean expanded,
                                                   boolean leaf, int row,
                                                   boolean hasFocus ) {

        /* Use the superclass to construct a default component. */
        super.getTreeCellRendererComponent( jtree, value, selected, 
                                            expanded, leaf, row, hasFocus );

        /* Temporary compatibility measure. */
        if ( value instanceof javax.swing.tree.DefaultMutableTreeNode ) {
            value = ((javax.swing.tree.DefaultMutableTreeNode) value)
                   .getUserObject();
        }

        /* Customise it according to the data, if we have a DataNode. */
        boolean isExpanding = false;
        if ( value instanceof DataNode ) {
            DataNode node = (DataNode) value;
            setIcon( node.getIcon() );
            String text = TreeviewUtil.toString( node );
            if ( text.trim().length() == 0 ) {
                text = "...";
            }
            setText( text );
            DataNodeTreeModel treeModel = (DataNodeTreeModel) jtree.getModel();
            if ( treeModel.containsNode( node ) ) {
                TreeModelNode modelNode = treeModel.getModelNode( node );
                NodeExpander expander = modelNode.getExpander();
                isExpanding = expander != null && ! expander.isStopped();
            }
        }

        /* Visual feedback for nodes that are in the process of expanding. */
        configureFont( isExpanding );

        /* Return the configured label. */
        return this;
    }

    private void configureFont( boolean special ) {
        if ( normalFont == null ) {
            normalFont = getFont();
            specialFont = normalFont.deriveFont( Font.ITALIC );
        }
        setFont( special ? specialFont : normalFont );
    }
}
