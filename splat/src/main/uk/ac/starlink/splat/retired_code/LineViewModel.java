package uk.ac.starlink.splat.iface;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;
import uk.ac.starlink.splat.plot.PlotRectangle;

/**
 * LineViewModel extends AbstractTableModel to provide a
 * description of the data shown in a JTable that relates to a series
 * spectral line fits.
 *
 * @since $Date$
 * @since 19-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Peter W. Draper
 * @see LineView, FitLineFrame
 */
public class LineViewModel extends AbstractTableModel
{
    /**
     * Array of objects that define the line properties.
     */
    private ArrayList lineList = new ArrayList();

    /**
     * Whether to show gaussian headers.
     */
    private boolean showGaussianHeaders = true;

    /**
     * Whether to show lorentzian headers.
     */
    private boolean showLorentzianHeaders = true;

    /**
     * Whether to show voigt headers.
     */
    private boolean showVoigtHeaders = true;

    /**
     *  Create an instance of this class.
     */
    public LineViewModel()
    {
        // Do nothing.
    }

//
//  Implement rest of TableModel interface (listeners are free from
//  AbstractTableModel)
//
    /**
     *  Returns the number of records managed by the data source
     *  object (i.e. the number of lines).
     */
    public int getRowCount() 
    {
        return lineList.size();
    }

    /**
     *  Returns the number of columns.
     */
    public int getColumnCount() 
    {
        return LineProperties.count( showGaussianHeaders,
                                     showLorentzianHeaders,
                                     showVoigtHeaders );
    }

    /**
     *  Return the value of a given cell.
     */
    public Object getValueAt( int row, int column ) 
    {
        if ( column == 0 ) {
            return new Integer( row );
        }
        LineProperties props = (LineProperties) lineList.get( row );
        return new Double( props.getField( column ) );
    }

    /**
     *  Return the column name.
     */
    public String getColumnName( int index ) 
    {
        return LineProperties.getName( index, showGaussianHeaders,
                                       showLorentzianHeaders, 
                                       showVoigtHeaders );
    }

   /**
     *  Return the column classes.
     */
    public Class getColumnClass( int index ) 
    {
        return getValueAt( 0, index ).getClass();
    }

    /**
     *  Displayed field is editable.
     */
    public boolean isCellEditable( int row, int column ) 
    {
        if ( column == 0 ) {
            return false;
        } else {
            return true;
        }
    }

    /**
     *  Must be able to change displayed status of spectrum.
     */
    public void setValueAt( Object value, int row, int column ) 
    {
        LineProperties props = (LineProperties) lineList.get( row );
        props.setField( column, ((Number)value).doubleValue() );
    }

//
//  Bespoke interface. Allow the addition, removal and query of a
//  new XGraphicsRange objects.
//
    /**
     *  React to a new line being added.
     *
     *  @param xRange the new LineProperties object.
     */
    public void addLine( LineProperties props )
    {
        lineList.add( props );
        fireTableRowsInserted( getRowCount() - 1, getRowCount() - 1 );
    }

    /**
     *  Lookup which LineProperties object contains an PlotRectangle.
     */
    public int findFigure( PlotRectangle figure )
    {
        LineProperties props;
        for ( int i = 0, j = 0; i < getRowCount(); i++, j += 2 ) {
            props = (LineProperties) lineList.get( i );
            if ( props.isFigure( figure ) ) {
                return i;
            }
        }
        return -1;
    }

    /**
     *  React to a line being removed
     *
     *  @param index list index of the line to remove.
     */
    public void removeLine( int index )
    {
        try {
            LineProperties props = (LineProperties) lineList.get( index );
            props.delete();
            lineList.remove( index );
            fireTableRowsDeleted( index, index );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  React to a line properties being changed, just need to update its
     *  description.
     *
     *  @param index list index of the range to remove.
     */
    public void changeRange( int index )
    {
        fireTableRowsUpdated( index, index );
    }

    /**
     * Return a list of the lines. Each line is specified by a pair of
     * consecutive values, constituting the fit range followed by a
     * single position, indicating the peak.
     */
    public double[] getLines()
    {
        double[] allLines = new double[getRowCount()*3];
        double[] thisLine;
        LineProperties props;
        for ( int i = 0, j = 0; i < getRowCount(); i++, j += 3 ) {
            props = (LineProperties) lineList.get( i );
            thisLine = props.getLine();
            allLines[j] = thisLine[0];
            allLines[j+1] = thisLine[1];
            allLines[j+2] = thisLine[2];
        }
        return allLines;
    }

    /**
     * Set if gaussian headers should be shown.
     */
    public void setShowGaussian( boolean show )
    {
        LineProperties props;
        for ( int i = 0, j = 0; i < getRowCount(); i++, j += 3 ) {
            props = (LineProperties) lineList.get( i );
            props.setShowGaussian( show );
        }
        this.showGaussianHeaders = show;
        fireTableStructureChanged();
    }

    /**
     * Set if lorentzian headers should be shown.
     */
    public void setShowLorentzian( boolean show )
    {
        LineProperties props;
        for ( int i = 0, j = 0; i < getRowCount(); i++, j += 3 ) {
            props = (LineProperties) lineList.get( i );
            props.setShowLorentzian( show );
        }
        this.showLorentzianHeaders = show;
        fireTableStructureChanged();
    }

    /**
     * Set if voigt headers should be shown.
     */
    public void setShowVoigts( boolean show )
    {
        LineProperties props;
        for ( int i = 0, j = 0; i < getRowCount(); i++, j += 3 ) {
            props = (LineProperties) lineList.get( i );
            props.setShowVoigts( show );
        }
        this.showVoigtHeaders = show;
        fireTableStructureChanged();
    }

    /**
     * Set the "quick" results for a line.
     */
    public void setQuickResults( int index, double[] values )
    {
        LineProperties props = (LineProperties) lineList.get( index );
        props.setAbsoluteField( 1, values[0] );
        props.setAbsoluteField( 2, values[1] );
        props.setAbsoluteField( 3, values[2] );
        props.setAbsoluteField( 4, values[3] );
        props.setAbsoluteField( 5, values[4] );
        changeRange( index );
    }

    /**
     * Set the "gaussian" results for a line.
     */
    public void setGaussianResults( int index, double[] values )
    {
        LineProperties props = (LineProperties) lineList.get( index );
        props.setAbsoluteField( 6, values[0] );
        props.setAbsoluteField( 7, values[1] );
        props.setAbsoluteField( 8, values[2] );
        props.setAbsoluteField( 9, values[3] );
        props.setAbsoluteField( 10, values[4] );
        changeRange( index );
    }

    /**
     * Set the "lorentzian" results for a line.
     */
    public void setLorentzianResults( int index, double[] values )
    {
        LineProperties props = (LineProperties) lineList.get( index );
        props.setAbsoluteField( 11, values[0] );
        props.setAbsoluteField( 12, values[1] );
        props.setAbsoluteField( 13, values[2] );
        props.setAbsoluteField( 14, values[3] );
        props.setAbsoluteField( 15, values[4] );
        changeRange( index );
    }

    /**
     * Set the "voigt" results for a line.
     */
    public void setVoigtResults( int index, double[] values )
    {
        LineProperties props = (LineProperties) lineList.get( index );
        props.setAbsoluteField( 16, values[0] );
        props.setAbsoluteField( 17, values[1] );
        props.setAbsoluteField( 18, values[2] );
        props.setAbsoluteField( 19, values[3] );
        props.setAbsoluteField( 20, values[4] );
        props.setAbsoluteField( 21, values[5] );
        changeRange( index );
    }
}
