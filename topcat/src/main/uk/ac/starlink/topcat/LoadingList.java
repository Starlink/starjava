package uk.ac.starlink.topcat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ListModel;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * List component which displays LoadingToken objects.
 * No selections are visible.
 *
 * @author   Mark Taylor
 * @since    9 April 2009
 */
public class LoadingList extends JList<LoadingToken> {

    /**
     * Constructor.
     *
     * @param   model  list model
     */
    @SuppressWarnings("this-escape")
    public LoadingList( ListModel<LoadingToken> model ) {
        super( model );
        setCellRenderer( new LoadingRenderer() );
    }

    protected void paintComponent( Graphics g ) {

        /* Set text antialiasing, otherwise (for me) italics look ugly. */
        Graphics2D g2 = (Graphics2D) g;
        Object hint = 
            g2.getRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING );
        g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
        super.paintComponent( g );
        g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, hint );
    }

    /**
     * Renderer intended for LoadingToken objects.
     */
    private static class LoadingRenderer extends DefaultListCellRenderer {
        private Color fg_;
        private Font font_;
        public Component getListCellRendererComponent( JList<?> list,
                                                       Object value,
                                                       int index, boolean isSel,
                                                       boolean hasFocus ) {
            Component comp = 
                super.getListCellRendererComponent( list, value, index, false,
                                                    false );
            if ( comp instanceof JLabel ) {
                JLabel label = (JLabel) comp;
                if ( font_ == null ) {
                    font_ = label.getFont().deriveFont( Font.ITALIC );
                    int rgba = ( label.getForeground().getRGB() & 0x00ffffff )
                             | 0xc0000000;
                    fg_ = new Color( rgba, true );
                }
                label.setFont( font_ );
                label.setForeground( fg_ );
            }
            return this;
        }
    }
}
