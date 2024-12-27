/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-MAY-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.util.gui;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.io.File;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

/**
 * FileNameListCellRenderer is a {@link javax.swing.ListCellRenderer}
 * that displays a
 * {@link java.io.File} or {@link java.lang.String} truncated to the left,
 * not right, so that
 * the most meaningful parts of a file name are shown. For instance the string
 * "/some/where/file.ext" could be rendered as ".../where/file.ext", depending
 * on the component width.
 * <p>
 * This class can also make sure that the width of the {@link javax.swing.JList}
 * associated with a {@link javax.swing.JComboBox} are matched
 * (for long names JComboBoxes
 * tend to just expand the drop-down JList to whatever size is needed, so if
 * you're using this renderer with JComboBox you'll need to use this option).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class FileNameListCellRenderer
    extends DefaultListCellRenderer
{
    /**
     * The JComboBox hosting these rendering instances, if used.
     */
    protected JComboBox<?> parent = null;

    /**
     * Create an instance with default behaviour.
     */
    public FileNameListCellRenderer()
    {
        super();
    }

    /**
     * Create an instance that matches its width to that of a given
     * {@link JComboBox}. When this is set the {@link JList} used as the
     * drop-down menu can be made the same size (otherwise long names are
     * allowed to extend past the visible right of the list).
     */
    public FileNameListCellRenderer( JComboBox<?> parent )
    {
        super();
        this.parent = parent;
    }

    /**
     * Return the requested component that renders the text.
     *
     * @param list the JList we're painting.
     * @param value the value to assign to the cell. This should be a
     *        a {@link File} or {@link String}.
     * @param index the cell's index (not used).
     * @param isSelected true if the specified cell was selected.
     * @param cellHasFocus true if the specified cell has the focus.
     */
    public Component getListCellRendererComponent( JList<?> list,
                                                   Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean cellHasFocus )
    {
        //  If required make the JList the same width as the associated
        //  JComboBox.
        if ( parent != null ) {
            int boxWidth = parent.getWidth();
            int listWidth = list.getFixedCellWidth();
            if ( listWidth != boxWidth && boxWidth != 0 ) {
                list.setFixedCellWidth( boxWidth );
            }
        }

        //  Set the background and foreground to match the JList.
        if ( isSelected ) {
            setBackground( list.getSelectionBackground() );
            setForeground( list.getSelectionForeground() );
        }
        else {
            setBackground( list.getBackground() );
            setForeground( list.getForeground() );
        }

        if ( value instanceof String ) {
            this.setText( (String) value );
        }
        else {
            this.setText( value.toString() );
        }
        return this;
    }

    //  String used to indicate truncation.
    protected final static String ELLIPSIS = "...";

    //  Override paintComponent to draw the String with truncation on the left
    //  rather than the right. This is more natural for the name associated
    //  with filenames.
    public void paintComponent( Graphics g )
    {
        //  Don't want super class to draw text, so hide that for now.
        String text = getText();
        setText( " " );
        super.paintComponent( g );
        setText( text );

        Graphics2D g2D = (Graphics2D) g;
        g2D.setPaint( getForeground() );

        //  Compute the length of the text as it would be drawn. If it's
        //  longer than available then truncate the rendering to the left.

        //  Implementation note: This is all to a not very accurate scale, to
        //  make this look really good you need to use a proper rendering of
        //  the full string at all times, so use getStringBounds of
        //  FontMetrics on what you might draw each time, easy to code, but
        //  will be very slow on large lists.

        FontMetrics fm = g2D.getFontMetrics();
        int x = 0;
        int y = fm.getMaxAscent() + 1;
        int width = fm.stringWidth( text );

        int compWidth = 0;
        if ( parent != null ) {
            //  JComboBox needs has more decorations, need to accomodate these.
            compWidth = parent.getWidth() - parent.getInsets().left -
                        parent.getInsets().right;
        }
        else {
            compWidth = getWidth();
        }

        if ( width > compWidth ) {
            int totalWidth = fm.stringWidth( ELLIPSIS );
            int nChars;
            int length = text.length();
            String buf = null;
            for ( nChars = 0; nChars < length; nChars++ ) {
                totalWidth += fm.charWidth( text.charAt( nChars ) );
                if ( totalWidth >= compWidth ) {
                    nChars--;
                    //  JComboBox needs has more decorations, need to
                    //  accomodate these. 
                    if ( parent != null ) nChars -= 2;
                    break;
                }
            }
            text = ELLIPSIS + text.substring( length - nChars, length );
        }
        g2D.drawString( text, x, y );
    }
}
