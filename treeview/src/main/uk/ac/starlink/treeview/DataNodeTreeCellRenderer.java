package uk.ac.starlink.treeview;

import java.awt.Component;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * TreeCellRenderer designed for rendering {@link DataNode} values.
 */
public class DataNodeTreeCellRenderer extends DefaultTreeCellRenderer {

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
        if ( value instanceof DataNode ) {
            DataNode node = (DataNode) value;
            setIcon( node.getIcon() );
            String text = node.toString();
            if ( text.trim().length() == 0 ) {
                text = "...";
            }
            setText( text );
        }

        /* Return the configured label. */
        return this;
    }
}
