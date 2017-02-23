package uk.ac.starlink.ttools.plot2.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * ColorChooserPanel implementation that provides a scrollable JList
 * of named colours.
 *
 * <p> Because of the way that this component sets its preferred size,
 * it should ideally be added to the JColorChooser after the other
 * chooser panels.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2017
 */
public class NamedColorChooserPanel extends AbstractColorChooserPanel {

    private final Map<String,Color> colorMap_;
    private final JColorChooser chooser_;

    /**
     * Constructor.
     * See the class {@link NamedColorSet} for some example colour maps.
     *
     * @param   colorMap  ordered map of colours to use
     * @param   chooser  color chooser on behalf of which this will work
     */
    public NamedColorChooserPanel( Map<String,Color> colorMap,
                                   JColorChooser chooser ) {
        colorMap_ = colorMap;
        chooser_ = chooser;
    }

    protected void buildChooser() {
        final JList list =
            new JList( colorMap_.keySet().toArray( new String[ 0 ] ) );
        final ColorIcon icon = new ColorIcon( 24, 12 );
        list.setCellRenderer( new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent( JList list,
                                                           Object value,
                                                           int index,
                                                           boolean isSelected,
                                                           boolean hasFocus ) {
                Component c = 
                    super.getListCellRendererComponent( list, value, index,
                                                        isSelected, hasFocus );
                Color color = colorMap_.get( value );
                if ( c instanceof JLabel && color != null ) {
                    icon.setColor( color );
                    ((JLabel) c).setIcon( icon );
                }
                return c;
            }
        } );
        list.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                if ( ! evt.getValueIsAdjusting() ) {
                    Color color = colorMap_.get( list.getSelectedValue() );
                    if ( color != null ) {
                        chooser_.getSelectionModel().setSelectedColor( color );
                    }
                }
            }
        } );
        setLayout( new BorderLayout() );

        /* We need to set the size here since it will be in a scroll pane.
         * Try to make it the same size as the parent component it will
         * get displayed in; smaller and it looks funny, larger and it
         * makes the JColorChooser too big.  This seems to work. */
        Dimension size = chooser_.getPreferredSize();
        JComponent preview = chooser_.getPreviewPanel();
        if ( preview != null ) {
            size.height -= preview.getPreferredSize().height;
        }
        setPreferredSize( size );
        add( new JScrollPane( list,
                              JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ),
             BorderLayout.CENTER );
    }

    public String getDisplayName() {
        return "Names";
    }

    public int getMnemonic() {
        return KeyEvent.VK_N;
    }

    /**
     * Returns null.
     * At least up to java 8, these seem to be ignored anyway.
     */
    public Icon getSmallDisplayIcon() {
        return null;
    }

    /**
     * Returns null.
     * At least up to java 8, these seem to be ignored anyway.
     */
    public Icon getLargeDisplayIcon() {
        return null;
    }

    public void updateChooser() {
    }
   
    /**
     * Icon that paints a patch of colour.
     */
    private static class ColorIcon implements Icon {
        private final int width_;
        private final int height_;
        private Color color_;

        /**
         * Constructor.
         *
         * @param  width   width
         * @param  height  height
         */
        ColorIcon( int width, int height ) {
            width_ = width;
            height_ = height;
        }

        /**
         * Sets the colour.
         *
         * @param  color   new colour
         */
        public void setColor( Color color ) { 
            color_ = color;
        }

        public int getIconWidth() {
            return width_;
        }

        public int getIconHeight() {
            return height_;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            if ( color_ != null ) {
                Color color0 = g.getColor();
                g.setColor( color_ );
                g.fillRect( x, y, width_, height_ );
                g.setColor( color0 );
            }
        }
    }
}
