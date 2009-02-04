/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     03-FEB-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.data;

import java.io.Serializable;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.util.Sort;
import uk.ac.starlink.splat.util.SplatException;

/**
 * An editable version of the {@link SpecData} type.  It extends
 * {@link SpecData} to also support the {@link EditableSpecDataImpl}
 * interface. This provides facilities for modifying the values and
 * coordinates.
 * <p>
 * If requested an instance of this class can also provide an
 * {@link UndoManager} that can be used to undo and redo any changes.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class EditableSpecData
    extends SpecData
    implements Serializable
{
    /**
     * The last UndoableEdit object created.
     */
    protected transient UndoableEdit undoableEdit = null;

    /**
     * The UndoManager for local UndoableEdit objects.
     */
    protected transient UndoManager undoManager = null;

    /**
     * Whether changes to this spectrum can be undone using the
     * UndoManager.
     */
    protected transient boolean undoable = false;

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
     * @param dataUnits the data units if known.
     * @param data the spectrum data values.
     */
    public void setSimpleData( double[] coords, String dataUnits,
                               double[] data )
        throws SplatException
    {
        if ( data.length != coords.length ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        constructColumnUndo( coords, dataUnits, data, null, false );
        ((EditableSpecDataImpl)impl).setSimpleData( coords, dataUnits, data );
        readData();
    }

    /**
     * Change the complete spectrum data, but preserving the properties of an
     * existing FrameSet as part of a new FrameSet that describes the spectrum.
     * Original data is not copied.
     *
     * @param frameSet the 1D FrameSet to be used for properties. The current
     *                 Frame defines the spectrum coordinate system.
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the spectrum data value units, if known.
     * @param data the spectrum data values.
     */
    public void setSimpleUnitData( FrameSet frameSet, double[] coords,
                                   String dataUnits, double[] data )
        throws SplatException
    {
        if ( data.length != coords.length ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        constructColumnUndo( coords, dataUnits, data, null, false );
        ((EditableSpecDataImpl)impl).setSimpleUnitData( frameSet, coords, 
                                                        dataUnits, data );
        readData();
    }

    /**
     * Change the complete spectrum data and WCS. Copies all data.
     *
     * @param frameSet a FrameSet for the WCS of this data
     * @param dataUnits the data units if known.
     * @param data the spectrum data values.
     */
    public void setFullData( FrameSet frameSet, String dataUnits,
                             double[] data )
        throws SplatException
    {
        constructColumnAndFrameSetUndo( dataUnits, data, null, false );
        ((EditableSpecDataImpl)impl).setFullData( frameSet, dataUnits, data );
        readData();
    }

    /**
     * Change the complete spectrum data. Doesn't copy data, just
     * keeps references.
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units if known.
     * @param data the spectrum data values.
     */
    public void setSimpleDataQuick( double[] coords, String dataUnits,
                                    double[] data )
        throws SplatException
    {
        if ( data.length != coords.length ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        constructColumnUndo( coords, dataUnits, data, null, true );
        ((EditableSpecDataImpl)impl).setSimpleDataQuick( coords, dataUnits, 
                                                         data );
        readData();
    }

    /**
     * Change the complete spectrum data, but preserving the properties of an
     * existing FrameSet as part of a new FrameSet that describes the spectrum.
     * Original data is not copied.
     *
     * @param frameSet the 1D FrameSet to be used for properties. The current
     *                 Frame defines the spectrum coordinate system.
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units if known
     * @param data the spectrum data values.
     */
    public void setSimpleUnitDataQuick( FrameSet frameSet, double[] coords,
                                        String dataUnits, double[] data )
        throws SplatException
    {
        if ( data.length != coords.length ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        constructColumnUndo( coords, dataUnits, data, null, true );
        ((EditableSpecDataImpl)impl).setSimpleUnitDataQuick( frameSet, coords,
                                                             dataUnits, data );
        readData();
    }

    /**
     * Change the complete spectrum data and WCS. Doesn't copy data, just
     * keeps references.
     *
     * @param frameSet a FrameSet for the WCS of this data
     * @param dataUnits the data units if known
     * @param data the spectrum data values.
     */
    public void setFullDataQuick( FrameSet frameSet, String dataUnits,
                                  double[] data )
        throws SplatException
    {
        constructColumnAndFrameSetUndo( dataUnits, data, null, true );
        ((EditableSpecDataImpl)impl).setFullDataQuick( frameSet, dataUnits, 
                                                       data );
        readData();
    }

    /**
     * Change the complete spectrum data. Copies all data.
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units if known
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setSimpleData( double[] coords, String dataUnits,
                               double[] data, double[] errors )
        throws SplatException
    {
        if ( ( data.length != coords.length && errors == null ) ||
             ( data.length != coords.length &&
               data.length != errors.length ) ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        constructColumnUndo( coords, dataUnits, data, errors, false );
        ((EditableSpecDataImpl)impl).setSimpleData( coords, dataUnits, data, 
                                                    errors );
        readData();
    }

    /**
     * Change the complete spectrum data, but preserving the properties of an
     * existing FrameSet as part of a new FrameSet that describes the spectrum.
     * Copies all data.
     *
     * @param frameSet the 1D FrameSet to be used for properties. The current
     *                 Frame defines the spectrum coordinate system.
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units if known
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setSimpleUnitData( FrameSet frameSet, double[] coords,
                                   String dataUnits, double[] data,
                                   double[] errors )
        throws SplatException
    {
        if ( ( data.length != coords.length && errors == null ) ||
             ( data.length != coords.length &&
               data.length != errors.length ) ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        constructColumnUndo( coords, dataUnits, data, errors, false );
        ((EditableSpecDataImpl)impl).setSimpleUnitData( frameSet, coords, 
                                                        dataUnits, data,
                                                        errors );
        readData();
    }

    /**
     * Change the complete spectrum data and WCS. Copies all data.
     *
     * @param frameSet a FrameSet for the WCS of this data
     * @param dataUnits the data units if known
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setFullData( FrameSet frameSet, String dataUnits,
                             double[] data, double[] errors )
        throws SplatException
    {
        if ( ( errors != null ) && ( data.length != errors.length ) ) {
            throw new SplatException( "Data and errors must have " +
                                      "the same number of values" );
        }
        constructColumnAndFrameSetUndo( dataUnits, data, errors, false );
        ((EditableSpecDataImpl)impl).setFullData( frameSet, dataUnits, data, 
                                                  errors );
        readData();
    }

    /**
     * Change the complete spectrum data. Doesn't copy data.
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units if known
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setSimpleDataQuick( double[] coords, String dataUnits,
                                    double[] data, double[] errors )
        throws SplatException
    {
        if ( ( data.length != coords.length ) ||
             ( ( errors != null ) && ( data.length != errors.length ) ) ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        constructColumnUndo( coords, dataUnits, data, errors, true );
        ((EditableSpecDataImpl)impl).setSimpleDataQuick( coords, dataUnits, 
                                                         data, errors );
        readData();
    }

    /**
     * Change the complete spectrum data, but preserving the properties of an
     * existing FrameSet as part of a new FrameSet that describes the spectrum.
     * Doesn't copy data.
     *
     * @param frameSet the 1D FrameSet to be used for properties. The current
     *                 Frame defines the spectrum coordinate system.
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units if known
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setSimpleUnitDataQuick( FrameSet frameSet, double[] coords,
                                        String dataUnits, double[] data,
                                        double[] errors )
        throws SplatException
    {
        if ( ( data.length != coords.length ) ||
             ( ( errors != null ) && ( data.length != errors.length ) ) ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        constructColumnUndo( coords, dataUnits, data, errors, true );
        ((EditableSpecDataImpl)impl).setSimpleUnitDataQuick( frameSet, coords,
                                                             dataUnits,
                                                             data, errors );
        readData();
    }

    /**
     * Change the complete spectrum data and WCS. Doesn't copy data.
     *
     * @param frameSet a FrameSet for the WCS of this data
     * @param dataUnits the data units if known
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setFullDataQuick( FrameSet frameSet, String dataUnits,
                                  double[] data, double[] errors )
        throws SplatException
    {
        if ( ( errors != null ) && ( data.length != errors.length ) ) {
            throw new SplatException( "Data and errors must have " +
                                      "the same number of values" );
        }
        constructColumnAndFrameSetUndo( dataUnits, data, errors, true );
        ((EditableSpecDataImpl)impl).setFullDataQuick( frameSet, dataUnits, 
                                                       data, errors );
        readData();
    }

    /**
     * Change a coordinate value.
     */
    public void setXDataValue( int index, double value )
        throws SplatException
    {
        constructCellUndo( EditCell.XCOLUMN, index );
        ((EditableSpecDataImpl)impl).setXDataValue( index, value );
        readData();
    }

    /**
     * Change a data value.
     */
    public void setYDataValue( int index, double value )
        throws SplatException
    {
        constructCellUndo( EditCell.YCOLUMN, index );
        ((EditableSpecDataImpl)impl).setYDataValue( index, value );
        readData();
    }

    /**
     * Change a data error value.
     */
    public void setYDataErrorValue( int index, double value )
        throws SplatException
    {
        constructCellUndo( EditCell.ECOLUMN, index );
        ((EditableSpecDataImpl)impl).setYDataErrorValue( index, value );
        readData();
    }

    /**
     * Sort the coordinates into increasing order and remove any duplicates.
     * This makes the spectrum monotonic.
     */
    public void sort()
        throws SplatException
    {
        //  First sort coordinates.
        if ( yErr != null ) {
            Sort.sort( xPos, yPos, yErr );
        }
        else {
            Sort.sort( xPos, yPos );
        }

        //  Check for duplicates, including runs of BAD.
        int ndup = 0;
        for ( int i = 1; i < xPos.length; i++ ) {
            if ( xPos[i-1] == xPos[i] ) {
                ndup++;
            }
        }
        if ( ndup > 0 ) {
            int size = xPos.length - ndup;
            double[] nc = new double[size];
            double[] nd = new double[size];
            double[] ne = null;
            if ( yErr != null ) {
                ne = new double[size];
                nc[0] = xPos[0];
                nd[0] = yPos[0];
                ne[0] = yErr[0];
                for ( int i = 1, j = 1; i < xPos.length; i++ ) {
                    if ( xPos[i-1] != xPos[i] ) {
                        nc[j] = xPos[i];
                        nd[j] = yPos[i];
                        ne[j] = yErr[i];
                        j++;
                    }
                }
            }
            else {
                nc[0] = xPos[0];
                nd[0] = yPos[0];
                for ( int i = 1, j = 1; i < xPos.length; i++ ) {
                    if ( xPos[i-1] != xPos[i] ) {
                        nc[j] = xPos[i];
                        nd[j] = yPos[i];
                        j++;
                    }
                }
            }
            setSimpleUnitDataQuick( getFrameSet(), nc, getCurrentDataUnits(),
                                    nd, ne );
        }
        else {
            setSimpleUnitDataQuick( getFrameSet(), xPos, getCurrentDataUnits(),
                                    yPos, yErr );
        }
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
        ((EditableSpecDataImpl)impl).setAst( frameSet );
        readData();
    }

    /**
     * If required constructor a undoable object to restore the state
     * of any of the data columns.
     */
    protected void constructColumnUndo( double[] coords,
                                        String dataUnits,
                                        double[] data,
                                        double[] errors,
                                        boolean needCopy )
    {
        if ( undoable && undoManager != null ) {
            undoableEdit = new EditColumn( this, coords, dataUnits, data,
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
    protected static class EditColumn
        extends AbstractUndoableEdit
    {
        private EditableSpecData specData = null;
        private String dataUnits = null;
        private double[] data = null;
        private double[] coords = null;
        private FrameSet frameSet = null;
        private double[] errors = null;

        /**
         * Constructor.
         *
         * @param specData the specData object about to be modified
         * @param newCoords the coordinates about to applied (only used
         *                  if needCopy is true).
         * @param newDataUnits the new data units (not used).
         * @param newData the data values about to applied (only used
         *                if needCopy is true).
         * @param newErrors the data errors about to applied. If null
         *                  then errors are assumed to be not currently
         *                  present, otherwise they will not be used in
         *                  needCopy is false).
         */
        public EditColumn( EditableSpecData specData,
                           double[] newCoords, String newDataUnits,
                           double[] newData, double[] newErrors, 
                           boolean needCopy )
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

            // Take reference of existing FrameSet to keep the coordinate
            // system properties. The actual mappings are not used.
            this.frameSet = specData.getFrameSet();

            data = specData.getYData();
            dataUnits = specData.getCurrentDataUnits();
            if ( needCopy && newData != null ) {
                if ( data != newData ) {
                    data = makeCopy( data );
                }
            }

            errors = specData.getYDataErrors();
            if ( needCopy && newErrors != null ) {
                if ( errors != newErrors && errors != null ) {
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

            FrameSet newFrameSet = frameSet;
            frameSet = (FrameSet) specData.getFrameSet();

            String newDataUnits = dataUnits;
            dataUnits = specData.getCurrentDataUnits();

            double[] newData = data;
            data = specData.getYData();

            double[] newErrors = errors;
            errors = specData.getYDataErrors();

            try {
                specData.undoable = false;
                specData.setSimpleUnitDataQuick( newFrameSet, newCoords, 
                                                 newDataUnits, 
                                                 newData, newErrors );
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
    protected static class EditFrameSet
        extends AbstractUndoableEdit
    {
        private EditableSpecData specData = null;
        private FrameSet frameSet = null;

        public EditFrameSet( EditableSpecData specData )
        {
            super();
            this.specData = specData;
            this.frameSet = specData.getFrameSet();
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
            frameSet = specData.getFrameSet();
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
    protected void constructColumnAndFrameSetUndo( String dataUnits,
                                                   double[] data,
                                                   double[] errors,
                                                   boolean needCopy )
    {
        if ( undoable && undoManager != null ) {
            undoableEdit = new EditColumnAndFrameSet( this, dataUnits,
                                                      data, errors,
                                                      needCopy );
            undoManager.addEdit( undoableEdit );
        }
    }

    /**
     * Simulatenous edit of data and FrameSet inner class, extends
     * AbstractUndoableEdit to provide an implementation of
     * UndoableEdit that can be stored in the UndoManager.
     */
    protected static class EditColumnAndFrameSet
        extends AbstractUndoableEdit
    {
        private EditableSpecData specData = null;
        private FrameSet frameSet = null;
        private double[] data = null;
        private String dataUnits = null;
        private double[] errors = null;

        /**
         * Constructor.
         *
         * @param specData the specData object about to be modified
         * @param newDataUnits the new data units (not used)
         * @param newData the data values about to applied (only used
         *                if needCopy is true).
         * @param newErrors the data errors about to applied. If null
         *                  then errors are assumed to be not currently
         *                  present, otherwise they will not be used in
         *                  needCopy is false).
         */
        public EditColumnAndFrameSet( EditableSpecData specData,
                                      String newDataUnits,
                                      double[] newData, double[] newErrors,
                                      boolean needCopy )
        {
            super();
            this.specData = specData;
            this.frameSet = specData.getFrameSet();

            data = specData.getYData();
            dataUnits = specData.getCurrentDataUnits();
            if ( needCopy && newData != null ) {
                if ( data != newData ) {
                    data = makeCopy( data );
                }
            }

            errors = specData.getYDataErrors();
            if ( needCopy && newErrors != null ) {
                if ( errors != newErrors && errors != null ) {
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

            String newDataUnits = dataUnits;
            dataUnits = specData.getCurrentDataUnits();

            double[] newErrors = errors;
            errors = specData.getYDataErrors();

            FrameSet targetFrameSet = frameSet;
            frameSet = specData.getFrameSet();

            try {
                specData.undoable = false;
                specData.setFullDataQuick( targetFrameSet, newDataUnits,
                                           newData, newErrors );
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
    protected static class EditCell
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
