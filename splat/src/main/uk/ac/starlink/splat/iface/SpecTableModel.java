/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     30-JAN-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import javax.swing.table.AbstractTableModel;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.LineIDSpecData;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.ast.gui.AstDouble;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * SpecTableModel is an implementation of the TableModel interface for
 * displaying the values of a spectrum.
 *
 * @version $Id$
 * @author Peter W. Draper
 */
public class SpecTableModel
    extends AbstractTableModel
{
    /**
     * The spectrum that we are displaying.
     */
    protected SpecData specData = null;

    /**
     * Whether the spectrum is readonly.
     */
    protected boolean readOnly = true;

    /**
     *  The global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Names for the table columns.
     */
    protected String[] columnNames = new String[3];
    //{ "coords", "data", "error" };

    /**
     * Values of the spectrum
     */
    protected double[] xData = null;
    protected double[] yData = null;
    protected double[] yDataErrors = null;
    protected String[] lineIDs = null;

    /**
     * Is the spectrum a LineIDSpecData?
     */
    protected boolean lineID = false;

    /**
     *  Create an instance of this class.
     */
    public SpecTableModel()
    {
        // Do nothing.
    }

    /**
     *  Create an instance of this class.
     */
    public SpecTableModel( SpecData specData )
    {
        setSpecData( specData );
    }

    /**
     *  Set the spectrum.
     */
    public void setSpecData( SpecData specData )
    {
        if ( specData == null ) {
            this.specData = specData;
            xData = null;
            yData = null;
            yDataErrors = null;
            columnNames = null;
            readOnly = true;
            lineID = false;
            update( false );
        }
        else if ( this.specData != specData ) {
            this.specData = specData;

            // Set readonly false, if possible.
            setReadOnly( false );
            lineID = ( specData instanceof LineIDSpecData );
            update( true );
        }
    }

    /**
     * Get the reference to the FrameSet. Note need to do this each
     * time as these can change.
     */
    private FrameSet getAstFrameSet()
    {
        return specData.getAst().getRef();
    }

    /**
     * Say if spectrum is readonly.
     */
    public boolean isReadOnly()
    {
        return readOnly;
    }

    /**
     * Set if spectrum is readonly. Only allow if modification if
     * spectrum is really writable.
     */
    public void setReadOnly( boolean readOnly )
    {
        if ( specData instanceof EditableSpecData ) {

            // Both states allowed.
            this.readOnly = readOnly;
        }
        else {
            readOnly = true;
        }
    }

    /**
     * Ask for a complete re-draw. If complete is true then the
     * coordinate system or presence of the error column may have
     * changed too.
     */
    public void update( boolean complete )
    {
        if ( complete ) {
            xData = specData.getXData();
            yData = specData.getYData();

            columnNames[0] = specData.getAst().getRef().getC( "label(1)" );
            columnNames[1] = specData.getAst().getRef().getC( "label(2)" );

            if ( lineID  ) {
                columnNames[2] = "Line ID";
                yDataErrors = null;
                lineIDs = ((LineIDSpecData) specData).getLabels();
            }
            else {
                yDataErrors = specData.getYDataErrors();
                columnNames[2] = "Error";
                lineIDs = null;
            }
        }
        fireTableStructureChanged();
    }

//
//  Implement rest of ListModel interface (listeners are free from
//  AbstractTableModel)
//
    /**
     *  Returns the number of records managed by the data source
     *  object (i.e.<!-- --> the number of plots).
     */
    public int getRowCount()
    {
        if ( specData != null ) {
            return specData.size();
        }
        return 0;
    }

    /**
     *  Returns the number of columns. Two or three, depends on
     *  whether there are errors.
     */
    public int getColumnCount()
    {
        if ( specData != null ) {
            if ( lineID ) {
                return 3;
            }
            else {
                if ( yDataErrors != null ) {
                    return 3;
                }
            }
        }
        return 2;
    }

    /**
     *  Return the value of a given cell.
     */
    public Object getValueAt( int row, int column )
    {
        if ( specData != null ) {
            switch ( column ) {
               case 0: {
                   return new AstDouble( xData[row], getAstFrameSet(), 1 );
               }
               case 1: {
                   return new AstDouble( yData[row], getAstFrameSet(), 2 );
               }
               case 2: {
                   if ( lineID ) {
                       return lineIDs[row];
                   }
                   else {
                       return new AstDouble( yDataErrors[row], 
                                             getAstFrameSet(), 2 );

                   }
               }
            }
        }
        return null;
    }

    /**
     *  Return the column names.
     */
    public String getColumnName( int index )
    {
        if ( columnNames != null ) {
            return columnNames[index];
        }
        return "";
    }

    /**
     *  Return the column classes. Use AstDouble to deal with
     *  formatting issues.
     */
    public Class getColumnClass( int index )
    {
        if ( lineID && index == 2 ) {
            return String.class;
        }
        return AstDouble.class;
    }

    /**
     *  Displayed field is editable.
     */
    public boolean isCellEditable( int row, int column )
    {
        return (! readOnly);
    }

    /**
     *  Must be able to change displayed status of the selected spectra.
     */
    public void setValueAt( Object value, int row, int column )
    {
        if ( specData != null && ! readOnly ) {

            //  These modifications could go wrong.
            try {   
                if ( lineID && column == 2 ) {
                    ((LineIDSpecData) specData).setLabel( row, 
                                                          (String) value );
                }
                else {
                    
                    //  Only get here for EditableSpecData types.
                    double dvalue = ((AstDouble)value).doubleValue();
                    EditableSpecData edit = (EditableSpecData) specData;
                    
                    switch ( column ) {
                       case 0: {
                           edit.setXDataValue( row, dvalue );
                           update( true );
                           break;
                       }
                       case 1: {
                           edit.setYDataValue( row, dvalue );
                           fireTableCellUpdated( row, column );
                           break;
                       }
                       case 2: {
                           edit.setYDataErrorValue(row, dvalue);
                           fireTableCellUpdated( row, column );
                           break;
                       }
                    }
                }
                globalList.notifySpecListenersChange( specData );
            }
            catch (Exception e) {
                ErrorDialog.showError( null, e );
            }
        }
    }
}
