/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-NOV-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Component;

import java.awt.Graphics;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

import uk.ac.starlink.splat.data.SpecData;

/**
 * LineRenderer is a ListCellRenderer that displays a rendering of a SpecData
 * object using its current thickness, style and colour along with a text
 * description. The description can either be the full or short name of the
 * object, this is controlled globally for the whole application.
 * <p>
 * Special care is taken to display the drop down list at the same size as the
 * parent JComboBox and to also truncate any names to the left, not right, so
 * that file names show the most meaningful parts.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see PlotControl
 */
public class LineRenderer
    extends DefaultListCellRenderer
{
    /**
     * The icon that renders the line.
     */
    protected static ColourIconLine icon = new ColourIconLine();

    /**
     * The global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * The JComboBox hosting these rendering instances.
     */
    protected JComboBox parent = null;

    /**
     * Whether rendering uses full or short names, short by default.
     */
    private static boolean showShortNames = true;

    /**
     * Length of the line drawn to show the symbolic rendering.
     */
    protected static final int LINELENGTH = 50;
    
    /**
     * Create an instance. Requires the parent JComboBox so that the JList
     * used as the drop-down menu can be made the same size (otherwise long
     * names are allowed to extend past the visible right of the list).
     */
    public LineRenderer( JComboBox parent )
    {
        super();
        this.parent = parent;
    }

    /**
     * Return the requested component that renders the line.
     *
     * @param list the JList we're painting.
     * @param value the value to assign to the cell. This should be a SpecData
     *              instance.
     * @param index the cell's index (not used).
     * @param isSelected true if the specified cell was selected.
     * @param cellHasFocus true if the specified cell has the focus.
     */
    public Component getListCellRendererComponent( JList list,
                                                   Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean cellHasFocus )
    {
        //  Make JList same size as what is actually shown.
        int boxWidth = parent.getWidth();
        int listWidth = list.getFixedCellWidth();
        if ( listWidth != boxWidth && boxWidth != 0 ) {
            list.setFixedCellWidth( boxWidth );
        }

        if ( value instanceof SpecData ) {
            try {
                SpecData spec = (SpecData) value;

                if ( showShortNames ) {
                    this.setText( spec.getShortName() );
                }
                else {
                    this.setText( spec.getFullName() );
                }

                icon.setIconWidth( LINELENGTH );
                icon.setLineThickness( (int)spec.getLineThickness() );
                icon.setLineStyle( (int)spec.getLineStyle() );
                icon.setLineColour( (int)spec.getLineColour() );
                setIcon( icon );
                if ( isSelected ) {
                    setBackground( list.getSelectionBackground() );
                    setForeground( list.getSelectionForeground() );
                }
                else {
                    setBackground( list.getBackground() );
                    setForeground( list.getForeground() );
                }
            } catch (Exception e) {
                // Do nothing.
            }
        }
        else if ( value instanceof String ) {
            this.setText( (String) value );
        }
        return this;
    }

    /**
     * Set whether all LineRenders use the short or full versions of the
     * spectra names.
     */
    public static void setShowShortNames( boolean showShortNames )
    {
        LineRenderer.showShortNames = showShortNames;
    }

    /**
     * Get whether all LineRenders use the short or full versions of the
     * spectra names.
     */
    public static boolean isShowShortNames()
    {
        return showShortNames;
    }

    //  String used to indicate truncation.
    protected final static String ELLIPSIS = "...";

    //  Override paintComponent to draw the String with truncation on the left
    //  rather than the right. This is more natural for the name associated
    //  with spectra (usually filenames when very long).
    public void paintComponent( Graphics g )
    {
        //  Super class draws all but the text String, so hide that.
        String text = getText();
        setText( " " );
        super.paintComponent( g );
        if ( text == null ) {
            text = " ";
        }
        setText( text );

        java.awt.Graphics2D g2D = (java.awt.Graphics2D) g;
        g2D.setPaint( getForeground() );

        //  Compute the length of the text as it would be drawn. If it's
        //  longer than available (+/- some slack for scrollbars) then
        //  truncate the rendering to the left.
        java.awt.FontMetrics fm = g2D.getFontMetrics();
        int x = LINELENGTH + getIconTextGap();
        int y = fm.getMaxAscent() + 1;
        int width = fm.stringWidth( text );

        int compWidth = parent.getWidth() - parent.getInsets().left -
                        parent.getInsets().right;
        int availTextWidth = compWidth - x;
        if ( width > availTextWidth ) {
            int totalWidth = fm.stringWidth( ELLIPSIS );
            int nChars;
            int length = text.length();
            for ( nChars = 0; nChars < length; nChars++ ) {
                totalWidth += fm.charWidth( text.charAt( nChars ) );
                if ( totalWidth >= availTextWidth ) {
                    break;
                }
            }
            text = ELLIPSIS + text.substring( length - nChars + 2, length );
        }
        g2D.drawString( text, x, y );
    }
}
