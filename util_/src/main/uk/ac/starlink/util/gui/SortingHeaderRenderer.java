package uk.ac.starlink.util.gui;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Renderer for JTable (header) cells that can indicate sorting status.
 * It modifies a supplied default renderer by adding a little arrow
 * pointing up or down for the single sorted column in a table
 * as reported by a couple of abstract methods.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2017
 */
public abstract class SortingHeaderRenderer implements TableCellRenderer {

    private final TableCellRenderer baseRenderer_;

    /**
     * Constructor.
     *
     * @param  baseRenderer  renderer on which this one is based
     */
    protected SortingHeaderRenderer( TableCellRenderer baseRenderer ) {
        baseRenderer_ = baseRenderer;
    }

    /**
     * Returns the renderer on which this one is based.
     *
     * @return  base renderer
     */
    public TableCellRenderer getBaseRenderer() {
        return baseRenderer_;
    }

    /**
     * Indicates the index of the table column on which sorting is performed.
     * If the table is unsorted (uses natural order) this method should
     * return a negative number.
     *
     * @return  index of sorting column, or negative value
     */
    public abstract int getSortColumnIndex();

    /**
     * Indicates the sense of the sorting.
     *
     * @return   true for descending sort, false for ascending
     */
    public abstract boolean isSortDescending();

    public Component getTableCellRendererComponent( JTable table, Object value,
                                                    boolean isSelected,
                                                    boolean hasFocus,
                                                    int irow, int icol ) {
        Component comp = baseRenderer_
            .getTableCellRendererComponent( table, value, isSelected,
                                            hasFocus, irow, icol );
        if ( comp instanceof JLabel ) {
            JLabel label = (JLabel) comp;
            label.setHorizontalTextPosition( JLabel.RIGHT );
            int iModelcol = table.convertColumnIndexToModel( icol );
            Icon icon = iModelcol == getSortColumnIndex()
                      ? new ArrowIcon( isSortDescending(),
                                       label.getFont().getSize() )
                      : null;
            label.setIcon( icon );
        }
        return comp;
    }

    /**
     * Returns an icon suitable for marking a column as sorted.
     *
     * @param  isDescending  false for sort up, true for sort down
     * @param  size  icon size, for instance font size of associated text
     * @return  icon
     */
    public static Icon createArrowIcon( boolean isDescending, int size ) {
        return new ArrowIcon( isDescending, size );
    }

    /**
     * Paints a little up or down arrow.
     * Code largely pinched from
     * http://java.sun.com/docs/books/tutorial/uiswing/examples/components/TableSorterDemoProject/src/components/TableSorter.java.
     */
    private static class ArrowIcon implements Icon {
        private final boolean descending_;
        private final int size_;

        /**
         * Constructor.
         *
         * @param   descending  false for up, true for down
         * @param   size  font size in pixels
         */
        ArrowIcon( boolean descending, int size ) {
            descending_ = descending;
            size_ = size;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            int dx = ( size_ + 1 ) / 2;
            int dy = descending_ ? dx : -dx;
            y += 5 * size_ / 6 + ( descending_ ? -dy : 0 );
            int shift = descending_ ? 1 : -1;
            g.translate( x, y );
            g.drawLine( dx / 2, dy, 0, 0 );
            g.drawLine( dx / 2, dy + shift, 0, shift );
            g.drawLine( dx / 2, dy, dx, 0 );
            g.drawLine( dx / 2, dy + shift, dx, shift );
            g.drawLine( dx, 0, 0, 0 );
            g.translate( -x, -y );
        }

        public int getIconWidth() {
            return size_;
        }

        public int getIconHeight() {
            return size_;
        }
    }
}
