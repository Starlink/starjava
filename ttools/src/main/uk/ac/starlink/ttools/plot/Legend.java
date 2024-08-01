package uk.ac.starlink.ttools.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * Draws the legend for identifying points on a plot.
 *
 * @author   Mark Taylor
 * @since    4 Jan 2006
 */
public class Legend extends JComponent {

    private LabelledStyle[] labelledStyles_;
    private ErrorModeSelection[] errorModeSelections_;
    private int iconWidth_;
    private int lineHeight_;
    private int prefWidth_;
    private int prefHeight_;

    private final static int IL_PAD = 8;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public Legend() {
        setOpaque( false );
        labelledStyles_ = new LabelledStyle[ 0 ];
        errorModeSelections_ = new ErrorModeSelection[ 0 ];
    }

    /**
     * Configures this legend to use a given set of error mode selections.
     * These can affect how marker style icons are drawn.
     *
     * @param  errorSelections   new error mode selections
     */
    public void setErrorModeSelections( ErrorModeSelection[] errorSelections ) {
        errorModeSelections_ = errorSelections;
    }

    /**
     * Sets the plot styles and their associated text labels.
     * The two arrays must have the same length.
     * Only styles with labels which are not blank will be shown.
     *
     * @param   styles   style array
     * @param   labels   label array
     */
    public void setStyles( Style[] styles, String[] labels ) {

        /* Validate and store state.  Any style with a blank associated
         * label is ignored. */
        if ( labels.length != styles.length ) {
            throw new IllegalArgumentException();
        }
        List<LabelledStyle> lsList = new ArrayList<LabelledStyle>();
        for ( int i = 0; i < styles.length; i++ ) {
            Style style = styles[ i ];
            String label = labels[ i ];
            if ( label != null && label.trim().length() > 0 ) {
                lsList.add( new LabelledStyle( style, label ) );
            }
        }
        setStyles( lsList.toArray( new LabelledStyle[ 0 ] ) );
    }

    /**
     * Sets the labelled plot styles.  
     * No additional validation is performed; all elements of 
     * <code>labelledStyles</code> will be displayed.
     *
     * @param  labelledStyles  labelled style object array for display
     */
    private void setStyles( LabelledStyle[] labelledStyles ) {
        labelledStyles_ = labelledStyles.clone();
        int nstyle = labelledStyles.length;

        /* Calculate geometry. */
        int ixmax = 0;
        int iymax = 0;
        int sxmax = 0;
        int height = 0;
        int width = 0;
        Font font = getFont();
        if ( font == null ) {
            return;
        }
        FontMetrics fm = getFontMetrics( font );
        lineHeight_ = (int) ( 1.2 * Math.max( fm.getHeight(), iymax ) );
        ErrorMode[] errorModes = getErrorModes();
        for ( int is = 0; is < nstyle; is++ ) {
            LabelledStyle ls = labelledStyles[ is ];
            Icon icon = getIcon( ls.getStyle(), errorModes );
            ixmax = Math.max( ixmax, icon.getIconWidth() );
            iymax = Math.max( iymax, icon.getIconHeight() );
            sxmax = Math.max( sxmax, fm.stringWidth( ls.getLabel() ) + 1 );
            height += lineHeight_;
        }
        iconWidth_ = ixmax;
        width = iconWidth_ + IL_PAD + sxmax;
        Insets insets = getInsets();
        int xpad = insets.left + insets.right;
        int ypad = insets.top + insets.bottom;

        /* Store new preferred geometry.  Width never decreases - this is
         * here simply so that the plotting in TOPCAT is smoother when 
         * flicking between subset selections. */
        prefWidth_ =
            Math.max( width + insets.left + insets.right, prefWidth_ );
        prefHeight_ = height + insets.top + insets.bottom;

        /* Ensure updates take place. */
        revalidate();
        repaint();
    }

    /**
     * Resets the width to the minimum necessary for this component.
     * Otherwise, the width is so arranged that it never shrinks, only grows
     * as longer legend text elements are added - that behaviour reduces
     * ugly resizing in TOPCAT.
     */
    public void resetWidth() {
        prefWidth_ = 0;
        setStyles( labelledStyles_ );
    }

    protected void paintComponent( Graphics g ) {

        /* JComponent boilerplate. */
        if ( isOpaque() ) {
            Color col = g.getColor();
            g.setColor( getBackground() );
            g.fillRect( 0, 0, getWidth(), getHeight() );
            g.setColor( col ); 
        }

        /* Draw icons and labels. */
        int nstyle = labelledStyles_.length;
        Insets insets = getInsets();
        FontMetrics fm = g.getFontMetrics();
        ErrorMode[] errorModes = getErrorModes();
        int xoff = insets.left;
        int ys = lineHeight_ / 2 + fm.getHeight() / 2;
        int xs = xoff + iconWidth_ + IL_PAD;
        for ( int is = 0; is < nstyle; is++ ) {
            LabelledStyle ls = labelledStyles_[ is ];
            int yoff = insets.top + lineHeight_ * is;
            Icon icon = getIcon( ls.getStyle(), errorModes );
            int yi = yoff + ( lineHeight_ - icon.getIconHeight() ) / 2;
            int xi = xoff + ( iconWidth_ - icon.getIconWidth() ) / 2;
            icon.paintIcon( this, g, xi, yi );
            g.drawString( ls.getLabel(), xs, ys + yoff );
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension( prefWidth_, prefHeight_ );
    }

    public Dimension getMaximumSize() {
        return new Dimension( Integer.MAX_VALUE, prefHeight_ );
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /**
     * Returns the icon to be drawn for a given style.
     *
     * @param   style   style to represent
     * @param   errorModes  error modes currently in force
     */
    private Icon getIcon( Style style, ErrorMode[] errorModes ) {
        return style instanceof MarkStyle
             ? ((MarkStyle) style).getLegendIcon( errorModes )
             : style.getLegendIcon();
    }

    /** 
     * Returns the list of currently selected error modes.
     *
     * @return  error modes
     */
    private ErrorMode[] getErrorModes() {
        int nerr = errorModeSelections_.length;
        ErrorMode[] modes = new ErrorMode[ nerr ];
        for ( int ierr = 0; ierr < nerr; ierr++ ) {
            modes[ ierr ] = errorModeSelections_[ ierr ].getErrorMode();
        }
        return modes;
    }

    /**
     * Struct-type utility class which aggregates a plot style and its
     * associated label.
     */
    private static class LabelledStyle {
        private final Style style_;
        private final String label_;

        /**
         * Constructor.
         *
         * @param  style  plot style
         * @param  label  style label
         */
        LabelledStyle( Style style, String label ) {
            style_ = style;
            label_ = label;
        }

        /**
         * Returns the style.
         *
         * @return  style
         */
        Style getStyle() {
            return style_;
        }

        /**
         * Returns the label.
         *
         * @return  label
         */
        String getLabel() {
            return label_;
        }
    }
}
