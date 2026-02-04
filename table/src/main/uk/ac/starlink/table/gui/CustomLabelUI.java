package uk.ac.starlink.table.gui;

import java.awt.FontMetrics;
import java.awt.Rectangle;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicLabelUI;

/**
 * An implementation of the (Basic)LabelUI for overriding JLabel formatting in
 * the cells of JTables.
 *
 * This is used to change the location of the ellipsis when a string is
 * rendered in a cell that cannot fit. By default, the ellipsis is always at
 * the end of the string, but this class instead places the ellipsis in the
 * middle of the string.
 *
 * @author   Fergus Baker
 * @since    03 Feb 2026
 * @see      javax.swing.plaf.basic.BasicLabelUI
 */
public class CustomLabelUI extends BasicLabelUI {
    private static final String ELLIPSES = "...";
    private static final int ELLIPSES_LEN = ELLIPSES.length();
    private String displayedText_;

    /**
     * Implicit constructor, called by Swing with a particular component in
     * order to have something for drawing labels to the screen.
     */
    public static CustomLabelUI createUI(JComponent c) {
        return new CustomLabelUI();
    }

    /**
     * Returns the text displayed in the label.
     *
     * @return   String
     */
    public String getText() {
        return displayedText_;
    }

    private int findTruncationSplit( int maxWidth, FontMetrics fontMetrics,
                                     String text ) {
        int textLength = text.length();
        for ( int i = 1; i <= textLength; i++ ) {
            int subTextWidth =
                fontMetrics.stringWidth( text.substring( textLength - i ) );
            if ( subTextWidth > maxWidth ) {
                return textLength - i + 1;
            }
        }
        return 0;
    }

    @Override
    protected String layoutCL( JLabel label, FontMetrics fontMetrics,
                               String text, Icon icon, Rectangle viewR,
                               Rectangle iconR, Rectangle textR ) {

        /* Draw the text with the usual layout for a compound label. */
        displayedText_ = super.layoutCL( label, fontMetrics, text, icon, viewR,
                                         iconR, textR );

        /* Sometimes the font metrics is null if the text is being displayed
         * before the UI is ready. In such a case, there is nothing that this
         * class needs to do. */
        if ( fontMetrics == null ) {
            return displayedText_;
        }


        /* Check if a truncation occurred, and if the text is long enough. If
         * the text is too short, there is no point in truncating differently.*/
        int displayedLength = displayedText_.length();
        if ( !displayedText_.equals( text ) &&
             displayedLength > 2 * ELLIPSES_LEN ) {
            int splitLength =
                ( displayedLength + ELLIPSES_LEN ) / 2 - ELLIPSES_LEN;
            String beforeEllipses = displayedText_.substring( 0, splitLength );

            /* Determine the maximum width (in pixels) that the remaining text
             * should fit into. */
            int displayedTextWidth = fontMetrics.stringWidth( displayedText_ );
            int maxWidth = displayedTextWidth
                         - fontMetrics.stringWidth( ELLIPSES )
                         - fontMetrics.stringWidth( beforeEllipses );
            int splitIndex = findTruncationSplit( maxWidth, fontMetrics, text );
            displayedText_ =
                beforeEllipses + ELLIPSES + text.substring( splitIndex );
        }

        return displayedText_;
    }
}
