/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-NOV-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.plot.PlotControl;

/**
 * LineRenderer is a ListCellRenderer that displays a line in a given
 * colour, style and thickness, together with a text description. This
 * is designed to correspond to the current line properties that a
 * spectrum will have when drawn (the text, if supplied, being the
 * spectrum name)
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
    GlobalSpecPlotList globalList = GlobalSpecPlotList.getReference();


    public LineRenderer() 
    {
        super();
    }


    /**
     * Return the requested component that renders the line.
     *
     * @param list the JList we're painting.
     * @param value the value to assign to the cell. This should be
     *              an Integer whose value is the index of the related
     *              spectrum in the global list.
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
        try {
            SpecData spec = (SpecData) value;
            this.setText( spec.getShortName() );
            icon.setIconWidth( 50 );
            icon.setLineThickness( (int)spec.getLineThickness() );
            icon.setLineStyle( (int)spec.getLineStyle() );
            icon.setLineColour( (int)spec.getLineColour() );
            setIcon( icon );
            if ( isSelected ) {
                setBackground( list.getSelectionBackground() );
                setForeground( list.getSelectionForeground() );
            } else {
                setBackground( list.getBackground() );
                setForeground( list.getForeground() );
            }
        } catch (Exception e) {
            //  Do nothing.
        }
        return this;
    }
}
