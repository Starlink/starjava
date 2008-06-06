/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     30-JAN-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;
import javax.swing.undo.UndoManager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.gui.AstCellEditor;
import uk.ac.starlink.ast.gui.AstDouble;
import uk.ac.starlink.ast.gui.ScientificFormat;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Displays a table of the values in a selected spectrum and allows
 * their modification (readonly spectra can be changed to modifiable
 * ones trivially by making a memory copy).
 * <p>
 * There are two basic ways of modifying the data, either values may
 * be changed individually, or columns of data may be changed as a
 * whole. In this latter case tools are provided to generate the new
 * values from functions of the existing values.
 * <p>
 * The error column can be created or deleted and rows of data can be
 * added or removed.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SpecViewerFrame
    extends JFrame
    implements ItemListener, SpecListener, ColumnGeneratorListener,
               CoordinateGeneratorListener
{
    //
    // TODO: add cut and paste of spectra.
    //
    /**
     * Spectrum we're viewing.
     */
    protected SpecData specData = null;

    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     * Read only status. Can cause a modification.
     */
    protected JCheckBox readOnly = null;

    /**
     * Actions tool bar.
     */
    protected JPanel windowActionBar = new JPanel();

    /**
     * Reference to global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Table of values.
     */
    protected JTable table = null;
    protected SpecTableModel model = null;

    /**
     * Close window action button.
     */
    protected JButton closeButton = new JButton();

    /**
     * Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenu editMenu = new JMenu();
    protected JMenu opsMenu = new JMenu();
    protected JMenu helpMenu = new JMenu();
    protected JMenuItem helpMenuAbout = new JMenuItem();

    /**
     * Name of spectrum
     */
    protected SplatName splatName = null;

    //  Undo and redo actions.
    protected UndoAction undoAction = null;
    protected RedoAction redoAction = null;

    /**
     * Create an instance.
     */
    public SpecViewerFrame( SpecData specData )
    {
        contentPane = (JPanel) getContentPane();
        initUI();
        HelpFrame.createHelpMenu( "viewer", "Help on window", menuBar, null );
        setSize( new Dimension( 300, 500 ) );
        setTitle( Utilities.getTitle( "View/modify a spectrum" ) );
        setSpecData( specData );
        pack();
        setVisible( true );

        //  Listen for changes to spectra.
        globalList.addSpecListener( this );
    }

    /**
     * Initialise the user interface.
     */
    protected void initUI()
    {
        //  Create and configure the JTable.
        model = new SpecTableModel();
        table = new JTable( model );
        JScrollPane tableScroller = new JScrollPane( table );
        contentPane.add( tableScroller );

        //  The formatting of cells is defined by AST.
        table.setDefaultRenderer( AstDouble.class, new NumberCellRenderer() );
        table.setDefaultEditor( AstDouble.class, new AstCellEditor() );

        TitledBorder tableViewTitle =
            BorderFactory.createTitledBorder( "Values:" );
        tableScroller.setBorder( tableViewTitle );

        //  The SplatName contains the short, full and format names
        //  for the spectrum.
        JPanel topPanel = new JPanel( new BorderLayout() );
        splatName = new SplatName();
        TitledBorder splatTitle =
            BorderFactory.createTitledBorder( "Spectrum:" );
        splatName.setBorder( splatTitle );
        topPanel.add( splatName, BorderLayout.NORTH );

        //  Display and allow the change of the readonly status.
        readOnly = new JCheckBox( "Readonly" );
        topPanel.add( readOnly, BorderLayout.SOUTH );
        readOnly.setToolTipText( "If spectrum is readonly, toggle to "+
                                 "get a writeable copy" );

        //  Respond to modifications (may make a copy).
        readOnly.addItemListener( this );

        // Add an action to close the window (appears in File menu
        // and action bar).
        ImageIcon image =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        CloseAction closeAction = new CloseAction( "Close", image,
                                                   "Close window" );
        closeButton = new JButton( closeAction );

        windowActionBar.setLayout( new BoxLayout( windowActionBar,
                                                  BoxLayout.X_AXIS ) );
        windowActionBar.setBorder( BorderFactory.createEmptyBorder(3,3,3,3) );
        windowActionBar.add( Box.createGlue() );
        windowActionBar.add( closeButton );
        windowActionBar.add( Box.createGlue() );

        // Set the the menuBar.
        setJMenuBar( menuBar );

        // Create and populate the File menu.
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        //  Create and populate the Edit menu.
        createEditMenu();

        //  Create and populate the operations menu.
        createOperationsMenu();

        //  Finally add components to main window.
        contentPane.add( topPanel, BorderLayout.NORTH );
        contentPane.add( tableScroller, BorderLayout.CENTER );
        contentPane.add( windowActionBar, BorderLayout.SOUTH );
    }

    /**
     * Create and populate the edit menu. This allows the cut and
     * paste of rows of the spectrum, if writable.
     */
    protected void createEditMenu()
    {
        editMenu.setText( "Edit" );
        editMenu.setMnemonic( KeyEvent.VK_E );
        menuBar.add( editMenu );

        // Undo the last change...
        undoAction = new UndoAction( "Undo", "Undo the last change" );
        editMenu.add( undoAction );

        // Redo the last undo...
        redoAction = new RedoAction( "Redo", "Undo the last undo" );
        editMenu.add( redoAction );

        //  Insert new rows
        InsertRowsAction insertRowsAction =
            new InsertRowsAction( "Insert new rows",
                                  "Insert new rows at the selected position" );
        editMenu.add( insertRowsAction );

        //  Delete selected rows
        DeleteSelectedRowsAction deleteSelectedRowsAction =
            new DeleteSelectedRowsAction( "Delete selected rows",
                                          "Delete the selected rows" );
        editMenu.add( deleteSelectedRowsAction );

        //  Delete the error column.
        DeleteErrorColumnAction deleteErrorColumnAction =
            new DeleteErrorColumnAction( "Delete error column",
                                         "Delete the error column" );
        editMenu.add( deleteErrorColumnAction );
    }

    /**
     * Create and populate the operations menu.
     */
    protected void createOperationsMenu()
    {
        opsMenu.setText( "Operations" );
        opsMenu.setMnemonic( KeyEvent.VK_O );
        menuBar.add( opsMenu );

        //  Modify the coordinates
        CreateCoordinatesAction createCoordinatesAction =
            new CreateCoordinatesAction( "Modify coordinates",
                                         "Modify or create new coordinates" );
        opsMenu.add( createCoordinatesAction );

        //  Modify the data column.
        CreateDataColumnAction createDataColumnAction =
            new CreateDataColumnAction( "Modify data column",
                                        "Modify the data column" );
        opsMenu.add( createDataColumnAction );

        //  Create or modify the error column.
        CreateErrorColumnAction createErrorColumnAction =
            new CreateErrorColumnAction( "Modify error column",
                                         "Create or modify the error column" );
        opsMenu.add( createErrorColumnAction );
    }

    /**
     * Display the given Spectrum.
     */
    public void setSpecData( SpecData specData )
    {
        if ( specData != this.specData ) {
            this.specData = specData;
            model.setSpecData( specData );
            splatName.setSpecData( specData );
            readOnly.setSelected( model.isReadOnly() );
            
            //  Get an undo manager for this EditableSpecData.
            if ( specData instanceof EditableSpecData ) {
                undoManager = ((EditableSpecData) specData).getUndoManager();
                specDataChanged();
            }
            else {
                
                //  No UndoManager means no undos.
                undoManager = null;
                refreshUndoRedo();
            }
        }
    }

    /**
     *  Close the window.
     */
    protected void closeWindow()
    {
        this.dispose();
    }

    /**
     * Inform the model and global list that the spectrum coordinates
     * and/or values have changed. There will be listeners that need
     * to be informed.
     */
    protected void specDataChanged()
    {
        model.update( true );
        globalList.notifySpecListenersModified( specData );

        //  Inform any local windows.
        if ( dataColumnGenerator != null ) {
            dataColumnGenerator
                .setEditableSpecData( (EditableSpecData) specData );
        }
        if ( errorColumnGenerator != null ) {
            errorColumnGenerator
                .setEditableSpecData( (EditableSpecData) specData );
        }
        if ( coordinateGenerator != null ) {
            coordinateGenerator
                .setEditableSpecData( (EditableSpecData) specData );
        }
    }

    protected CoordinateGeneratorFrame coordinateGeneratorWindow = null;
    protected CoordinateGenerator coordinateGenerator = null;

    /**
     * Create or modify the coordinates.
     */
    protected void createCoordinates()
    {
        if ( ! model.isReadOnly() ) {
            if ( coordinateGeneratorWindow == null ) {
                coordinateGenerator =
                    new CoordinateGenerator( (EditableSpecData) specData,
                                             this );
                coordinateGeneratorWindow =
                    new CoordinateGeneratorFrame( coordinateGenerator );
            }
            coordinateGeneratorWindow.setVisible( true );
            coordinateGenerator.setEditableSpecData
                ( (EditableSpecData) specData );
        }
        else {
            JOptionPane.showMessageDialog
                ( this, "Cannot create or modify the coordinates " +
                  "of a readonly spectrum", "Readonly",
                  JOptionPane.ERROR_MESSAGE );
        }
    }

    protected ColumnGeneratorFrame errorColumnWindow = null;
    protected ErrorColumnGenerator errorColumnGenerator = null;

    /**
     * Create or modify the error column. Use a tool to generate from an
     * expression based on the data values etc.
     */
    protected void createErrorColumn()
    {
        if ( ! model.isReadOnly() ) {
            if ( errorColumnWindow == null ) {
                errorColumnGenerator =
                    new ErrorColumnGenerator( (EditableSpecData) specData,
                                              this );
                errorColumnWindow =
                    new ColumnGeneratorFrame( errorColumnGenerator );
            }
            errorColumnWindow.setVisible( true );
            errorColumnGenerator.setEditableSpecData
                ( (EditableSpecData) specData );
        }
        else {
            JOptionPane.showMessageDialog
                ( this, "Cannot create or modify the error column " +
                  "for a readonly spectrum", "Readonly",
                  JOptionPane.ERROR_MESSAGE );
        }
    }

    protected ColumnGeneratorFrame dataColumnWindow = null;
    protected DataColumnGenerator dataColumnGenerator = null;

    /**
     * Re-create or modify the data column. Use a tool to generate from an
     * expression based on the data values etc.
     */
    protected void createDataColumn()
    {
        if ( ! model.isReadOnly() ) {
            if ( dataColumnWindow == null ) {
                dataColumnGenerator =
                    new DataColumnGenerator( (EditableSpecData) specData,
                                             this );
                dataColumnWindow =
                    new ColumnGeneratorFrame( dataColumnGenerator );
            }
            dataColumnWindow.setVisible( true );
            dataColumnGenerator.setEditableSpecData
                ( (EditableSpecData) specData );
        }
        else {
            JOptionPane.showMessageDialog
                ( this, "Cannot re-create or modify the data column " +
                  "of a readonly spectrum", "Readonly",
                  JOptionPane.ERROR_MESSAGE );
        }
    }

    //
    // CoordinateGeneratorListener
    //

    /**
     * Accept a change of coordinate column.
     */
    public void acceptGeneratedCoords( double[] coords )
    {
        try {
            EditableSpecData editSpec = (EditableSpecData) specData;
            FrameSet frameSet = 
                ASTJ.get1DFrameSet( editSpec.getAst().getRef(), 1 );
            editSpec.setSimpleUnitDataQuick( frameSet, coords, 
                                             specData.getCurrentDataUnits(),
                                             specData.getYData(),
                                             specData.getYDataErrors() );
            specDataChanged();
        }
        catch (SplatException e) {
            ErrorDialog.showError( this, e );
        }
    }

    /**
     * Change the spectrum FrameSet. This modifies the coordinates
     * according to some new mapping.
     */
    public void changeFrameSet( FrameSet frameSet )
    {
        try {
            ((EditableSpecData)specData).setFrameSet( frameSet );
            specDataChanged();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //
    // ColumnGeneratorListener
    //

    /**
     * Respond to column generation events.
     */
    public void acceptGeneratedColumn( Object source, double[] column )
    {
        if ( column != null && source != null ) {
            try {
                FrameSet frameSet = specData.getFrameSet();
                if ( source == dataColumnGenerator ) {
                    // Data column.
                    double[] errors = specData.getYDataErrors();
                    ((EditableSpecData)specData).setFullDataQuick
                        ( frameSet, specData.getCurrentDataUnits(), 
                          column, errors );
                }
                else {
                    // Error column.
                    double[] values = specData.getYData();
                    ((EditableSpecData)specData).setFullDataQuick
                        ( frameSet, specData.getCurrentDataUnits(), 
                          values, column );
                }
                specDataChanged();
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }
    }

    /** Number of rows added by default */
    protected Integer defaultRows = new Integer( 1 );

    /** ScientificFormat used to parse numbers */
    protected ScientificFormat defaultFormat = new ScientificFormat();

    /**
     * Insert a number of new rows into the spectrum.
     */
    protected void insertNewRows()
    {
        if ( ! model.isReadOnly() ) {
            //  Need to find out how many.
            Number number =
                DecimalDialog.showDialog( this, "Add new rows",
                                          "Number of rows to create",
                                          defaultFormat, defaultRows );
            if ( number != null ) {
                int nrows = number.intValue();

                // Generate the required data and insert it.
                generateAndInsertRows( nrows );

                // Dialog shows same value next time.
                defaultRows = new Integer( nrows );
            }
        }
        else {
            JOptionPane.showMessageDialog
                ( this,
                  "Cannot modify a read-only spectrum",
                  "Readonly", JOptionPane.ERROR_MESSAGE );
        }
    }

    /**
     * Generate a sequence of new rows and insert these into the
     * currently selected position in the table.
     */
    protected void generateAndInsertRows( int nrows )
    {
        double[] coords = specData.getXData();

        // The main problem here is that coordinates must increase or
        // decrease, so need to interpolate or extrapolate in some
        // sense.
        int index = table.getSelectedRow();
        if ( index == - 1 ) {
            index = coords.length - 1;
        }

        //  Get nearest values.
        double lower = coords[index];
        double upper = lower;
        if ( index < coords.length - 1 ) {
            upper = coords[index + 1];
        }
        double incr = 1.0;
        if ( upper > lower ) {
            incr = ( upper - lower ) / (double) ( nrows + 1 );
        }

        //  Insert after the selected position.
        index++;

        //  Create new arrays for spectrum.
        double[] values = specData.getYData();
        double[] errors = specData.getYDataErrors();

        int length = coords.length + nrows;
        double[] newCoords = new double[length];
        double[] newValues = new double[length];
        double[] newErrors = null;
        if ( errors != null ) {
            newErrors = new double[length];
        }

        //  Copy the first part, i.e. up to the selected row into new arrays.
        int start = 0;
        System.arraycopy( coords, start, newCoords, start, index );
        System.arraycopy( values, start, newValues, start, index );
        if ( errors != null ) {
            System.arraycopy( errors, start, newErrors, start, index );
        }
        start = index + nrows;

        //  Generate the new rows. Coordinates increment by incr each
        //  row and all values are <bad>, making them break the spectrum.
        lower += incr;
        if ( errors == null ) {
            for ( int i = index; i < start; i++ ) {
                newCoords[i] = lower;
                lower += incr;
                newValues[i] = SpecData.BAD;
            }
        }
        else {
            for ( int i = index; i < start; i++ ) {
                newCoords[i] = lower;
                lower += incr;
                newValues[i] = SpecData.BAD;
                newErrors[i] = SpecData.BAD;
            }
        }

        //  Copy any remaining data from the original arrays.
        if ( start < length - 1 ) {
            length = length - start;
            System.arraycopy( coords, index, newCoords, start, length );
            System.arraycopy( values, index, newValues, start, length );
            if ( errors != null ) {
                System.arraycopy( errors, index, newErrors, start, length );
            }
        }

        //  Update spectrum.
        try {
            EditableSpecData editSpec = (EditableSpecData) specData;
            FrameSet frameSet = 
                ASTJ.get1DFrameSet( editSpec.getAst().getRef(), 1 );
            editSpec.setSimpleUnitDataQuick( frameSet, newCoords,
                                             editSpec.getCurrentDataUnits(),
                                             newValues, newErrors );
            specDataChanged();
        }
        catch (SplatException e) {
            ErrorDialog.showError( this, e );
        }
    }

    /**
     * Delete the error column.
     */
    protected void deleteErrorColumn()
    {
        if ( ! model.isReadOnly() && specData.haveYDataErrors() ) {
            FrameSet frameSet = specData.getFrameSet();
            double[] values = specData.getYData();
            String valueUnits = specData.getCurrentDataUnits();
            double[] errors = null;
            try {
                ((EditableSpecData)specData).setFullData( frameSet, 
                                                          valueUnits,
                                                          values,
                                                          errors );
                specDataChanged();
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }
        else if ( specData.haveYDataErrors() ) {
            JOptionPane.showMessageDialog
                ( this,
                  "Cannot delete an error column from a readonly spectrum",
                  "Readonly", JOptionPane.ERROR_MESSAGE );
        }
        else {
            JOptionPane.showMessageDialog
                ( this,
                  "Cannot delete an error column as none exists",
                  "No errors", JOptionPane.ERROR_MESSAGE );
        }
    }

    /**
     * Delete any selected rows from the spectrum.
     */
    protected void deleteSelectedRows()
    {
        if ( ! model.isReadOnly() ) {

            //  Get the selected rows.
            int[] indices = table.getSelectedRows();
            if ( indices.length > 0 ) {

                double[] coords = specData.getXData();
                double[] values = specData.getYData();
                double[] errors = null;
                double[] newCoords = new double[coords.length-indices.length];
                double[] newValues = new double[coords.length-indices.length];
                double[] newErrors = null;
                if ( specData.haveYDataErrors() ) {
                    errors = specData.getYDataErrors();
                    newErrors = new double[coords.length-indices.length];
                    int l = 0;
                    int k = indices[l];
                    for ( int i = 0, j = 0; i < coords.length; i++ ) {
                        if ( i < k ) {
                            newCoords[j] = coords[i];
                            newValues[j] = values[i];
                            newErrors[j] = errors[i];
                            j++;
                        }
                        else {
                            l++;
                            if ( l < indices.length ) {
                                k = indices[l];
                            }
                            else {
                                k = coords.length; // Run to end.
                            }
                        }
                    }
                }
                else {
                    int l = 0;
                    int k = indices[l];
                    for ( int i = 0, j = 0; i < coords.length; i++ ) {
                        if ( i < k ) {
                            newCoords[j] = coords[i];
                            newValues[j] = values[i];
                            j++;
                        }
                        else {
                            l++;
                            if ( l < indices.length ) {
                                k = indices[l];
                            }
                            else {
                                k = coords.length; // Run to end.
                            }
                        }
                    }

                }

                try {
                    EditableSpecData editSpec = (EditableSpecData) specData;
                    FrameSet frameSet = 
                        ASTJ.get1DFrameSet( editSpec.getAst().getRef(), 1 );
                    editSpec.setSimpleUnitData( frameSet, newCoords, 
                                                editSpec.getCurrentDataUnits(),
                                                newValues, 
                                                newErrors );
                    specDataChanged();
                }
                catch (SplatException e) {
                    ErrorDialog.showError( this, e );
                }
            }
            else {
                JOptionPane.showMessageDialog
                    ( this,
                      "No rows are currently selected",
                      "No selection", JOptionPane.ERROR_MESSAGE );

            }
        }
        else {
            JOptionPane.showMessageDialog
                ( this,
                  "Cannot delete from a readonly spectrum",
                  "Readonly", JOptionPane.ERROR_MESSAGE );
        }
    }

    /**
     * Inner class defining Action for closing a window.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction( String name, Icon icon, String shortHelp )
        {
            super( name, icon  );
            putValue( SHORT_DESCRIPTION, shortHelp );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control W" ) );
        }

        /**
         * Respond to actions from the buttons.
         */
        public void actionPerformed( ActionEvent ae )
        {
            closeWindow();
        }
    }

    /**
     * Inner class defining Action for modifying coordinates.
     */
    protected class CreateCoordinatesAction extends AbstractAction
    {
        public CreateCoordinatesAction( String name, String shortHelp )
        {
            super( name );
            putValue( SHORT_DESCRIPTION, shortHelp );
        }

        /**
         * Respond to actions from the buttons.
         */
        public void actionPerformed( ActionEvent ae )
        {
            createCoordinates();
        }
    }

    /**
     * Inner class defining Action for creating a new error column.
     */
    protected class CreateErrorColumnAction extends AbstractAction
    {
        public CreateErrorColumnAction( String name, String shortHelp )
        {
            super( name );
            putValue( SHORT_DESCRIPTION, shortHelp );
        }

        /**
         * Respond to actions from the buttons.
         */
        public void actionPerformed( ActionEvent ae )
        {
            createErrorColumn();
        }
    }

    /**
     * Inner class defining Action for modifying the data column.
     */
    protected class CreateDataColumnAction extends AbstractAction
    {
        public CreateDataColumnAction( String name, String shortHelp )
        {
            super( name );
            putValue( SHORT_DESCRIPTION, shortHelp );
        }

        /**
         * Respond to actions from the buttons.
         */
        public void actionPerformed( ActionEvent ae )
        {
            createDataColumn();
        }
    }

    /**
     * Inner class defining Action for inserting new rows.
     */
    protected class InsertRowsAction extends AbstractAction
    {
        public InsertRowsAction( String name, String shortHelp )
        {
            super( name );
            putValue( SHORT_DESCRIPTION, shortHelp );
        }

        /**
         * Respond to actions from the buttons.
         */
        public void actionPerformed( ActionEvent ae )
        {
            insertNewRows();
        }
    }

    /**
     * Inner class defining Action for deleting the error column.
     */
    protected class DeleteErrorColumnAction extends AbstractAction
    {
        public DeleteErrorColumnAction( String name, String shortHelp )
        {
            super( name );
            putValue( SHORT_DESCRIPTION, shortHelp );
        }

        /**
         * Respond to actions from the buttons.
         */
        public void actionPerformed( ActionEvent ae )
        {
            deleteErrorColumn();
        }
    }

    /**
     * Inner class defining Action for deleting the selected rows.
     */
    protected class DeleteSelectedRowsAction extends AbstractAction
    {
        public DeleteSelectedRowsAction( String name, String shortHelp )
        {
            super( name );
            putValue( SHORT_DESCRIPTION, shortHelp );
        }

        /**
         * Respond to actions from the buttons.
         */
        public void actionPerformed( ActionEvent ae )
        {
            deleteSelectedRows();
        }
    }

    //
    // ItemListener interface.
    //

    /**
     * Change the editable state of the table. If spectrum cannot be
     * editted then make a memory copy.
     */
    public void itemStateChanged( ItemEvent itemEvent )
    {
        if ( specData != null ) {
            boolean needReadOnly = readOnly.isSelected();
            if ( model.isReadOnly() != needReadOnly ) {

                // Check actual status of spectrum, not current readonly
                // status.
                if ( ! ( specData instanceof EditableSpecData ) ) {
                    if ( ! needReadOnly ) {
                        //  Spectrum is not writable, so need to create a copy.
                        EditableSpecData newSpec = null;
                        String name = "Copy of: " + specData.getShortName();
                        try {
                            newSpec = SpecDataFactory.getInstance()
                                .createEditable( name, specData );
                            globalList.add( newSpec );
                            setSpecData( newSpec );
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                model.setReadOnly( needReadOnly );
            }
        }
    }

    //
    // SpecListener interface. Want to know if spectrum is changed elsewhere!
    //
    public void spectrumAdded( SpecChangedEvent e )
    {
        // Do nothing
    }

    /**
     *  Sent when a spectrum is removed.
     */
    public void spectrumRemoved( SpecChangedEvent e )
    {
        if ( specData != null ) {
            // Could be this spectrum.
            int index = globalList.getSpectrumIndex( specData );
             if ( index == e.getIndex() ) {
                setSpecData( null );
            }

            // Nothing to do (XXX maybe add way to select another spectrum).
            closeWindow();
        }
    }

    /**
     *  Send when a spectrum property is changed.
     */
    public void spectrumChanged( SpecChangedEvent e )
    {
        if ( specData != null ) {
            int index = globalList.getSpectrumIndex( specData );
            if ( index == e.getIndex() ) {
                //  Re-draw table, assume coordinates have new AST backing.
                model.update( true );

                //  This is an edit we can undo.
                refreshUndoRedo();
            }
        }
    }

    /**
     *  Send when a spectrum is modified.
     */
    public void spectrumModified( SpecChangedEvent e )
    {
        spectrumChanged( e );
    }

    /**
     *  Send when a spectrum becomes "current".
     */
    public void spectrumCurrent( SpecChangedEvent e )
    {
        // Do nothing.
    }

    //
    // Undo support.
    //

    // UndoManager of the EditableSpecData instance.
    protected UndoManager undoManager = null;

    /**
     * Inner class defining Action for undo the last change.
     */
    protected class UndoAction extends AbstractAction
    {
        public UndoAction( String name, String shortHelp )
        {
            super( name );
            putValue( SHORT_DESCRIPTION, shortHelp );
        }

        /**
         * Respond to actions from the buttons.
         */
        public void actionPerformed( ActionEvent ae )
        {
            undo();
        }
    }

    /**
     * Inner class defining Action for undoing the last undo
     */
    protected class RedoAction extends AbstractAction
    {
        public RedoAction( String name, String shortHelp )
        {
            super( name );
            putValue( SHORT_DESCRIPTION, shortHelp );
        }

        /**
         * Respond to actions from the buttons.
         */
        public void actionPerformed( ActionEvent ae )
        {
            redo();
        }
    }

    /**
     * Set the states of the Undo and Redo actions.
     */
    public void refreshUndoRedo() 
    {
        if ( undoManager != null ) {
            undoAction.setEnabled( undoManager.canUndo() );
            redoAction.setEnabled( undoManager.canRedo() );
        }
        else {
            undoAction.setEnabled( false );
            redoAction.setEnabled( false );
        }
    } 

    /**
     * Undo the last change.
     */
    protected void undo()
    {
        if ( undoManager.canUndo() ) {
            undoManager.undo();
            specDataChanged();
        }
    }

    /**
     * Undo the last undo.
     */
    protected void redo()
    {
        if ( undoManager.canRedo() ) {
            undoManager.redo();
            specDataChanged();
        }
    }
}
