package uk.ac.starlink.datanode.tree.select;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import uk.ac.starlink.datanode.nodes.DataNode;

/**
 * Renderer used for a NodeListComboBox.
 *
 * @author   Mark Taylor (Starlink)
 * @since    10 Mar 2005
 */
class NodeListCellRenderer implements ListCellRenderer {

    private final ListCellRenderer baseRenderer_;

    /**
     * Constructor.  This renderer will base its behaviour on the
     * given renderer, just tweaking it a bit.
     *
     * @param   base  template cell renderer
     */
    public NodeListCellRenderer( ListCellRenderer base ) {
        baseRenderer_ = base;
    }

    public Component getListCellRendererComponent( JList list, Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean hasFocus ) {
        /* Prepare suitable text and icon. */
        final int depth;
        final Icon baseIcon;
        if ( value instanceof DataNode ) {
            DataNode node = (DataNode) value;
            value = node.getName();
            depth = new NodeChain( node ).getDepth();
            baseIcon = node.getIcon();
        }
        else {
            depth = 0;
            baseIcon = null;
        }

        /* Re-use default behaviour. */
        Component comp =
            baseRenderer_.getListCellRendererComponent( list, value, index,
                                                        isSelected, hasFocus );

        /* Offset the icon according to how deep we are in the node
         * hierarchy. */
        if ( comp instanceof JLabel && baseIcon != null ) {
            final int offset = 2 + ( ( index >= 0 ) ? depth * 10 : 0 );
            final Icon icon = new Icon() {
                public int getIconHeight() {
                    return baseIcon.getIconHeight();
                }
                public int getIconWidth() {
                    return baseIcon.getIconWidth() + offset;
                }
                public void paintIcon( Component c, Graphics g, int x, int y ) {
                    baseIcon.paintIcon( c, g, x + offset, y );
                }
            };
            ((JLabel) comp).setIcon( icon );
        }

        /* Return. */
        return comp;
    }
}
