/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     30-JAN-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import javax.swing.table.AbstractTableModel;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.util.ExceptionDialog;
import uk.ac.starlink.ast.gui.AstDouble;
import uk.ac.starlink.ast.FrameSet;

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
    protected GlobalSpecPlotList
        globalList = GlobalSpecPlotList.getReference();

    /**
     * Names for the table columns.
     */
    protected String[] columnNames = { "coords", "data", "error" };

    /**
     * Values of the spectrum
     */
    protected double[] xData = null;
    protected double[] yData = null;
    protected double[] yDataErrors = null;

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
            readOnly = true;
            update( false );
        }
        else if ( this.specData != specData ) {
            this.specData = specData;

            // Set readonly false, if possible.
            setReadOnly( false );
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
            yDataErrors = specData.getYDataErrors();
        }
        fireTableStructureChanged();
    }

//
//  Implement rest of ListModel interface (listeners are free from
//  AbstractTableModel)
//
    /**
     *  Returns the number of records managed by the data source
     *  object (i.e. the number of plots).
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
            if ( yDataErrors != null ) {
                return 3;
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
                   return new AstDouble( yDataErrors[row], getAstFrameSet(), 2 );
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
        return columnNames[index];
    }

    /**
     *  Return the column classes. Use AstDouble to deal with
     *  formatting issues.
     */
    public Class getColumnClass( int index )
    {
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

            //  Only get here for EditableSpecData types.
            double dvalue = ((AstDouble)value).doubleValue();
            EditableSpecData edit = (EditableSpecData) specData;

            //  These modifications could go wrong.
            try {
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
                globalList.notifySpecListeners( edit );
            }
            catch (Exception e) {
                ExceptionDialog eDialog = new ExceptionDialog( null, e );
            }
        }
    }
}
