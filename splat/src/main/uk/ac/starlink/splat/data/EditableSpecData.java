/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     03-FEB-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.data;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.util.SplatException;

/**
 * Extends {@link SpecData} for types of SpecDataImpl that also
 * implement the EditableSpecDataImpl interface, i.e.&nbsp;this provides
 * facilities for modifying the values and coordinates of a
 * SpecData object.
 * <p>
 * If requested an object can also provide a UndoManager instance that
 * can be used to undo and redo any changes.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class EditableSpecData
    extends SpecData
{
    /**
     * Reference to the EditableSpecDataImpl.
     */
    protected transient EditableSpecDataImpl editableImpl = null;

    /**
     * The last UndoableEdit object created.
     */
    protected transient UndoableEdit undoableEdit = null;

    /**
     * The UndoManager for local UndoableEdit objects.
     */
    protected UndoManager undoManager = null;

    /**
     * Whether changes to this spectrum can be undone using the
     * UndoManager.
     */
    protected boolean undoable = false;

    /**
     * Create an instance using the data in a given an EditableSpecDataImpl
     * object.
     *
     * @param impl a concrete implementation of an EditableSpecDataImpl
     *             class that is accessing spectral data in of some format.
     * @exception SplatException thrown if there are problems obtaining
     *                           the spectrum.
     */
    public EditableSpecData( EditableSpecDataImpl impl )
        throws SplatException
    {
        super( impl, true );
        this.editableImpl = impl;
    }

    /**
     * Set the UndoManager to be used for any modifications. This
     * discards the local UndoManager and will disable the propagation
     * of undoable actions to other objects that already hold an
     * instance of the UndoManager, so take care.
     */
    public void setUndoManager( UndoManager undoManager )
    {
        this.undoManager = undoManager;
        setUndoable( true );
    }

    /**
     * Request a reference to the local UndoManager. This
     * automatically makes any future modifications undoable.
     */
    public UndoManager getUndoManager()
    {
        setUndoable( true );
        return undoManager;
    }

    /**
     * Set if modifications should be controllable using the
     * UndoManager.
     */
    public void setUndoable( boolean undoable )
    {
        if ( undoable && undoManager == null ) {
            undoManager = new UndoManager();
        }
        else if ( ! undoable && undoManager != null ) {
            //  Loose all edits when switched off.
            undoManager.discardAllEdits();
        }
        this.undoable = undoable;
    }

    /**
     * See if this instance is current supporting undoable events.
     */
    public boolean isUndoable()
    {
        return undoable;
    }

    /**
     * Change the complete spectrum data. Copies all data.
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param data the spectrum data values.
     */
    public void setData( double[] coords, double[] data )
        throws SplatException
    {
        if ( data.length != coords.length ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        constructColumnUndo( coords, data, null, false );
        editableImpl.setData( coords, data );
        readData();
    }

    /**
     * Change the complete spectrum data and WCS. Copies all data.
     *
     * @param frameSet a FrameSet for the WCS of this data
     * @param data the spectrum data values.
     */
    public void setData( FrameSet frameSet, double[] data )
        throws SplatException
    {
        constructColumnAndFrameSetUndo( data, null, false );
        editableImpl.setData( frameSet, data );
        readData();
    }

    /**
     * Change the complete spectrum data. Doesn't copy data, just
     * keeps references.
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param data the spectrum data values.
     */
    public void setDataQuick( double[] coords, double[] data )
        throws SplatException
    {
        if ( data.length != coords.length ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        constructColumnUndo( coords, data, null, true );
        editableImpl.setDataQuick( coords, data );
        readData();
    }

    /**
     * Change the complete spectrum data and WCS. Doesn't copy data, just
     * keeps references.
     *
     * @param frameSet a FrameSet for the WCS of this data
     * @param data the spectrum data values.
     */
    public void setDataQuick( FrameSet frameSet, double[] data )
        throws SplatException
    {
        constructColumnAndFrameSetUndo( data, null, true );
        editableImpl.setDataQuick( frameSet, data );
        readData();
    }

    /**
     * Change the complete spectrum data. Copies all data.
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setData( double[] coords, double[] data, double[] errors )
        throws SplatException
    {
        if ( ( data.length != coords.length && errors == null ) ||
             ( data.length != coords.length &&
               data.length != errors.length ) ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        constructColumnUndo( coords, data, errors, false );
        editableImpl.setData( coords, data, errors );
        readData();
    }

    /**
     * Change the complete spectrum data and WCS. Copies all data.
     *
     * @param frameSet a FrameSet for the WCS of this data
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setData( FrameSet frameSet, double[] data, double[] errors )
        throws SplatException
    {
        if ( ( errors == null ) || ( data.length != errors.length ) ) {
            throw new SplatException( "Data and errors must have " +
                                      "the same number of values" );
        }
        constructColumnAndFrameSetUndo( data, errors, false );
        editableImpl.setData( frameSet, data, errors );
        readData();
    }

    /**
     * Change the complete spectrum data. Doesn't copy data.
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setDataQuick( double[] coords, double[] data, double[] errors )
        throws SplatException
    {
        if ( ( data.length != coords.length ) ||
             ( ( errors != null ) && ( data.length != errors.length ) ) ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        constructColumnUndo( coords, data, errors, true );
        editableImpl.setDataQuick( coords, data, errors );
        readData();
    }

    /**
     * Change the complete spectrum data and WCS. Doesn't copy data.
     *
     * @param frameSet a FrameSet for the WCS of this data
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setDataQuick( FrameSet frameSet, double[] data,
                              double[] errors )
        throws SplatException
    {
        if ( ( errors != null ) && ( data.length != errors.length ) ) {
            throw new SplatException( "Data and errors must have " +
                                      "the same number of values" );
        }
        constructColumnAndFrameSetUndo( data, errors, true );
        editableImpl.setDataQuick( frameSet, data, errors );
        readData();
    }

    /**
     * Change a coordinate value.
     */
    public void setXDataValue( int index, double value )
        throws SplatException
    {
        constructCellUndo( EditCell.XCOLUMN, index );
        editableImpl.setXDataValue( index, value );
        readData();
    }

    /**
     * Change a data value.
     */
    public void setYDataValue( int index, double value )
        throws SplatException
    {
        constructCellUndo( EditCell.YCOLUMN, index );
        editableImpl.setYDataValue( index, value );
        readData();
    }

    /**
     * Change a data error value.
     */
    public void setYDataErrorValue( int index, double value )
        throws SplatException
    {
        constructCellUndo( EditCell.ECOLUMN, index );
        editableImpl.setYDataErrorValue( index, value );
        readData();
    }

    /**
     * Accept a FrameSet that defines a new set of coordinates.
     */
    public void setFrameSet( FrameSet frameSet )
        throws SplatException
    {
        // Give the new FrameSet to the implementation, then cause a
        // reset of the coordinates.
        constructFrameSetUndo();
        editableImpl.setAst( frameSet );
        readData();
    }

    /**
     * If required constructor a undoable object to restore the state
     * of any of the data columns.
     */
    protected void constructColumnUndo( double[] coords,
                                        double[] data,
                                        double[] errors,
                                        boolean needCopy )
    {
        if ( undoable && undoManager != null ) {
            undoableEdit = new EditColumn( this, coords, data,
                                           errors, needCopy );
            undoManager.addEdit( undoableEdit );
        }
    }

    /**
     * Edit of column data inner class, extends AbstractUndoableEdit
     * to provide an implementation of UndoableEdit that can be stored
     * in the UndoManager. This is very very simple, just get copies
     * of the data columns that are about to be changed and when
     * asked, put them back.
     */
    protected class EditColumn
        extends AbstractUndoableEdit
    {
        private EditableSpecData specData = null;
        private double[] data = null;
        private double[] coords = null;
        private double[] errors = null;

        /**
         * Constructor.
         *
         * @param specData the specData object about to be modified
         * @param newCoords the coordinates about to applied (only used
         *                  if needCopy is true).
         * @param newData the data values about to applied (only used
         *                if needCopy is true).
         * @param newErrors the data errors about to applied. If null
         *                  then errors are assumed to be not currently
         *                  present, otherwise they will not be used in
         *                  needCopy is false).
         */
        public EditColumn( EditableSpecData specData,
                           double[] newCoords, double[] newData,
                           double[] newErrors, boolean needCopy )
        {
            super();
            this.specData = specData;

            coords = specData.getXData();
            if ( needCopy && newCoords != null ) {
                //  Don't really need a copy if this is just a
                //  replacement operation.
                if ( coords != newCoords ) {
                    coords = makeCopy( coords );
                }
            }

            data = specData.getYData();
            if ( needCopy && newData != null ) {
                if ( data != newData ) {
                    data = makeCopy( data );
                }
            }

            errors = specData.getYDataErrors();
            if ( needCopy && newErrors != null ) {
                if ( errors != newErrors & errors != null ) {
                    errors = makeCopy( errors );
                }
            }
        }

        public void undo()
            throws CannotUndoException
        {
            super.undo();
            switchData();
        }

        public void redo()
            throws CannotUndoException
        {
            super.redo();
            switchData();
        }

        public String getPresentationName()
        {
            return "EditableSpecData.EditColumn";
        }

        protected void switchData()
        {
            //  Replace data from storage. This routine also takes
            //  references to the existing data so that the next
            //  undo/redo backs out of this change.
            double[] newCoords = coords;
            coords = specData.getXData();

            double[] newData = data;
            data = specData.getYData();

            double[] newErrors = errors;
            errors = specData.getYDataErrors();

            try {
                specData.undoable = false;
                specData.setDataQuick( newCoords, newData, newErrors );
                specData.undoable = true;
            }
            catch (SplatException e) {
                e.printStackTrace();
            }
        }

        protected double[] makeCopy( double[] from )
        {
            double[] to = new double[from.length];
            System.arraycopy( from, 0, to, 0, from.length );
            return to;
        }
    }

    /**
     * Record an undo state for restoring the current FrameSet.
     */
    protected void constructFrameSetUndo()
    {
        if ( undoable && undoManager != null ) {
            undoableEdit = new EditFrameSet( this );
            undoManager.addEdit( undoableEdit );
        }
    }

    /**
     * Edit of SpecData FrameSet inner class, extends AbstractUndoableEdit
     * to provide an implementation of UndoableEdit that can be stored
     * in the UndoManager.
     */
    protected class EditFrameSet
        extends AbstractUndoableEdit
    {
        private EditableSpecData specData = null;
        private FrameSet frameSet = null;

        public EditFrameSet( EditableSpecData specData )
        {
            super();
            this.specData = specData;

            // Take clone so that annuls elsewhere are OK.
            this.frameSet = (FrameSet) specData.getFrameSet().clone();
        }

        public void undo()
            throws CannotUndoException
        {
            super.undo();
            switchFrameSet();
        }

        public void redo()
            throws CannotUndoException
        {
            super.redo();
            switchFrameSet();
        }

        public String getPresentationName()
        {
            return "EditableSpecData.EditFrameSet";
        }

        protected void switchFrameSet()
        {
            FrameSet targetFrameSet = frameSet;
            frameSet = (FrameSet) specData.getFrameSet().clone();
            try {
                specData.undoable = false;
                specData.setFrameSet( targetFrameSet );
                specData.undoable = true;
            }
            catch (SplatException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * If required constructor a undoable object to restore the
     * current state of the data columns and FrameSet (coordinates are
     * re-generated from this).
     */
    protected void constructColumnAndFrameSetUndo( double[] data,
                                                   double[] errors,
                                                   boolean needCopy )
    {
        if ( undoable && undoManager != null ) {
            undoableEdit = new EditColumnAndFrameSet( this, data, errors, 
                                                      needCopy );
            undoManager.addEdit( undoableEdit );
        }
    }

    /**
     * Simulatenous edit of data and FrameSet inner class, extends
     * AbstractUndoableEdit to provide an implementation of
     * UndoableEdit that can be stored in the UndoManager.
     */
    protected class EditColumnAndFrameSet
        extends AbstractUndoableEdit
    {
        private EditableSpecData specData = null;
        private FrameSet frameSet = null;
        private double[] data = null;
        private double[] errors = null;

        /**
         * Constructor.
         *
         * @param specData the specData object about to be modified
         * @param newData the data values about to applied (only used
         *                if needCopy is true).
         * @param newErrors the data errors about to applied. If null
         *                  then errors are assumed to be not currently
         *                  present, otherwise they will not be used in
         *                  needCopy is false).
         */
        public EditColumnAndFrameSet( EditableSpecData specData,
                                      double[] newData, double[] newErrors,
                                      boolean needCopy )
        {
            super();
            this.specData = specData;

            // Take clone of existing FrameSet so that annuls
            // elsewhere are OK.
            this.frameSet = (FrameSet) specData.getFrameSet().clone();

            data = specData.getYData();
            if ( needCopy && newData != null ) {
                if ( data != newData ) {
                    data = makeCopy( data );
                }
            }

            errors = specData.getYDataErrors();
            if ( needCopy && newErrors != null ) {
                if ( errors != newErrors & errors != null ) {
                    errors = makeCopy( errors );
                }
            }
        }

        public void undo()
            throws CannotUndoException
        {
            super.undo();
            switchBack();
        }

        public void redo()
            throws CannotUndoException
        {
            super.redo();
            switchBack();
        }

        public String getPresentationName()
        {
            return "EditableSpecData.EditColumnAndFrameSet";
        }

        protected void switchBack()
        {
            //  Replace data from storage. This routine also takes
            //  references to the existing data so that the next
            //  undo/redo backs out of this change.
            double[] newData = data;
            data = specData.getYData();

            double[] newErrors = errors;
            errors = specData.getYDataErrors();

            FrameSet targetFrameSet = frameSet;
            frameSet = (FrameSet) specData.getFrameSet().clone();

            try {
                specData.undoable = false;
                specData.setDataQuick( targetFrameSet, newData, newErrors );
                specData.undoable = true;
            }
            catch (SplatException e) {
                e.printStackTrace();
            }
        }

        protected double[] makeCopy( double[] from )
        {
            double[] to = new double[from.length];
            System.arraycopy( from, 0, to, 0, from.length );
            return to;
        }
    }

    /**
     * Record an undo state for restoring a cell edit.
     */
    protected void constructCellUndo( int column, int index )
    {
        if ( undoable && undoManager != null ) {
            undoableEdit = new EditCell( this, column, index );
            undoManager.addEdit( undoableEdit );
        }
    }

    /**
     * Edit of SpecData FrameSet inner class, extends AbstractUndoableEdit
     * to provide an implementation of UndoableEdit that can be stored
     * in the UndoManager.
     */
    protected class EditCell
        extends AbstractUndoableEdit
    {
        //  COLUMN indices for cell edits.
        public static final int XCOLUMN = 0;
        public static final int YCOLUMN = 1;
        public static final int ECOLUMN = 2;

        private EditableSpecData specData = null;
        private double value = 0.0;
        private int index = 0;
        private int column = XCOLUMN;

        public EditCell( EditableSpecData specData, int column, int index )
        {
            super();
            this.specData = specData;
            this.column = column;
            this.index = index;
            this.value = getValue( column, index );
        }

        public void undo()
            throws CannotUndoException
        {
            super.undo();
            resetCell();
        }

        public void redo()
            throws CannotUndoException
        {
            super.redo();
            resetCell();
        }

        public String getPresentationName()
        {
            return "EditableSpecData.EditCell";
        }

        protected void resetCell()
        {
            double oldValue = value;
            value = getValue( column, index );
            setValue( column, index, oldValue );
        }

        protected double getValue( int column, int index )
        {
            try {
                switch (column )
                {
                    case XCOLUMN:
                        return specData.getXData()[index];
                    case YCOLUMN:
                        return specData.getYData()[index];
                    case ECOLUMN:
                        return specData.getYDataErrors()[index];
                }
            }
            catch (Exception e) {
                // Bad but not fatal (probably undo out of sequence
                // causing index error).
                e.printStackTrace();
            }
            return SpecData.BAD;
        }

        protected void setValue( int column, int index, double value )
        {
            specData.undoable = false;
            try {
                switch (column )
                {
                    case XCOLUMN: {
                        specData.setXDataValue( index, value );
                    }
                    break;
                    case YCOLUMN: {
                        specData.setYDataValue( index, value );
                    }
                    break;
                    case ECOLUMN: {
                        specData.setYDataErrorValue( index, value );
                    }
                    break;
                }
            }
            catch (SplatException e) {
                e.printStackTrace();
            }
            finally {
                specData.undoable = true;
            }
        }
    }
}
