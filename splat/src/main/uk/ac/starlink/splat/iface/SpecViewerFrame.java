/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
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
import javax.swing.border.TitledBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import uk.ac.starlink.ast.gui.AstCellEditor;
import uk.ac.starlink.ast.gui.AstDouble;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.util.ExceptionDialog;
import uk.ac.starlink.splat.util.SplatException;

/**
 * Displays a table of the values in a selected spectrum. Also allows
 * the values to be editted, if the spectrum is memory resident
 * (non-memory spectra need to have a copy created first). Edits are
 * direct and no undo facilities are provided (yet).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SpecViewerFrame
    extends JFrame
    implements ItemListener, SpecListener, ColumnGeneratorListener
{
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
    protected GlobalSpecPlotList globalList =
        GlobalSpecPlotList.getReference();

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
        topPanel.setToolTipText( "If spectrum is readonly, toggle to "+
                                 "get a writeable copy" );

        //  Respond to modifications (may make a copy).
        readOnly.addItemListener( this );

        // Add an action to close the window (appears in File menu
        // and action bar).
        ImageIcon image =
            new ImageIcon( ImageHolder.class.getResource( "exit.gif" ) );
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
        menuBar.add( fileMenu );
        fileMenu.add( closeAction );

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
        // XXX TODO. Need undo support throughout...
        editMenu.setText( "Edit" );
        menuBar.add( editMenu );

        //  Delete selected rows
        DeleteSelectedRowsAction deleteSelectedRowsAction = 
            new DeleteSelectedRowsAction( "Delete Selected Rows",
                                          "Delete the selected rows" );
        editMenu.add( deleteSelectedRowsAction );

        //  Delete the error column.
        DeleteErrorColumnAction deleteErrorColumnAction = 
            new DeleteErrorColumnAction( "Delete Error Column",
                                         "Delete the error column" );
        editMenu.add( deleteErrorColumnAction );
    }

    /**
     * Create and populate the operations menu.
     */
    protected void createOperationsMenu()
    {
        opsMenu.setText( "Operations" );
        menuBar.add( opsMenu );

        //  Modify the data column.
        CreateDataColumnAction createDataColumnAction = 
            new CreateDataColumnAction( "Modify Data Column",
                                        "Modify the data column" );
        opsMenu.add( createDataColumnAction );

        //  Create an error column.
        CreateErrorColumnAction createErrorColumnAction = 
            new CreateErrorColumnAction( "Create Error Column",
                                         "Create or modify the error column" );
        opsMenu.add( createErrorColumnAction );
    }

    /**
     * Display the given Spectrum.
     */
    public void setSpecData( SpecData specData )
    {
        this.specData = specData;
        model.setSpecData( specData );
        splatName.setSpecData( specData );
        readOnly.setSelected( model.isReadOnly() );
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
        globalList.notifySpecListeners( specData );
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
                    new ErrorColumnGenerator( specData, this );
                errorColumnWindow = 
                    new ColumnGeneratorFrame( errorColumnGenerator );
            }
            errorColumnWindow.setVisible( true );
            errorColumnGenerator.setSpecData( specData );
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
                    new DataColumnGenerator( specData, this );
                dataColumnWindow = 
                    new ColumnGeneratorFrame( dataColumnGenerator );
            }
            dataColumnWindow.setVisible( true );
            dataColumnGenerator.setSpecData( specData );
        }
        else {
            JOptionPane.showMessageDialog
                ( this, "Cannot re-create or modify the data column " +
                  "of a readonly spectrum", "Readonly", 
                  JOptionPane.ERROR_MESSAGE );
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
                if ( source == dataColumnGenerator ) {
                    // Data column.
                    double[] coords = specData.getXData();
                    double[] errors = specData.getYDataErrors();
                    ((EditableSpecData)specData).setDataQuick( column,
                                                               coords, 
                                                               errors );
                }
                else {
                    // Error column.
                    double[] coords = specData.getXData();
                    double[] values = specData.getYData();
                    ((EditableSpecData)specData).setDataQuick( values,
                                                               coords, 
                                                               column );
                }
                specDataChanged();
                dataColumnGenerator.setSpecData( specData );
            }
            catch (SplatException e) {
                new ExceptionDialog( this, e );
            }
        }
    }

    /**
     * Delete the error column.
     */
    protected void deleteErrorColumn()
    {
        if ( ! model.isReadOnly() && specData.haveYDataErrors() ) {
            double[] coords = specData.getXData();
            double[] values = specData.getYData();
            double[] errors = null;
            try {
                ((EditableSpecData)specData).setData( values, coords, errors );
                specDataChanged();
            }
            catch (SplatException e) {
                ExceptionDialog eDialog = new ExceptionDialog( this, e );
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
                    ((EditableSpecData)specData).setData( newValues,
                                                          newCoords, 
                                                          newErrors ); 
                    specDataChanged();
                }
                catch (SplatException e) {
                    ExceptionDialog eDialog = new ExceptionDialog( this, e );
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
                            newSpec = SpecDataFactory.getReference().
                                createEditable( name, specData );
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
            }
        }
    }

    /**
     *  Send when a spectrum becomes "current".
     */
    public void spectrumCurrent( SpecChangedEvent e )
    {
        // Do nothing.
    }
}
