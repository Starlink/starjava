/*
 * Copyright (C) 2001 Central Laboratory of the Research Councils
 * Copyright (C) 2005 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     19-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;

import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;

/**
 * Component for displaying the results of a series of measurements of
 * spectral lines. The results are shown in several JTables that are
 * controlled using a tabbed pane. <p>
 *
 * Utility methods are provided to write the results out to a disk file.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LineFitView
    extends JPanel
{
    /**
     * The JTable models of the various types of spectral-line data.
     */
    protected LineFitViewModel[] models =
        new LineFitViewModel[LineProperties.NTYPES];

    /**
     * Environment for displaying the tables.
     */
    protected MultiTableView tableView = new MultiTableView();

    /**
     * The DivaPlot that we are working with.
     */
    protected DivaPlot plot = null;

    /**
     * Border and title for the complete component.
     */
    protected TitledBorder title =
        BorderFactory.createTitledBorder( "Spectral fitting results:" );

    /**
     * Create an instance. This provides for the default Quick type to be
     * viewed.
     */
    public LineFitView( DivaPlot plot )
    {
        setPlot( plot );
        initUI();
        addView( LineProperties.QUICK );
    }

    /**
     * Set the plot used.
     */
    protected void setPlot( DivaPlot plot )
    {
        this.plot = plot;
    }

    /**
     * Initialise the various user interface components.
     */
    protected void initUI()
    {
        setLayout( new BorderLayout() );
        setBorder( title );
        add( tableView, BorderLayout.CENTER );
    }

    /**
     * Add a view for a fit type, return the model to be used for tabulating
     * the measurements for this type.
     *
     * @param type the type of fit to be tabulated. One of the pre-defined
     *             LineProperties types "QUICK", "GAUSS", "LORENTZ" or "VOIGT".
     * @return Description of the Return Value
     */
    public LineFitViewModel addView( int type )
    {
        //  Make sure a view for the results of such a fit is shown.
        JTable table = tableView.add( LineProperties.NAMES[type], false );
        if ( table != null ) {

            //  Create a table model for this type and set this up for
            //  the table.
            if ( models[type] == null ) {
                models[type] = new LineFitViewModel( type );
            }

            //  Set the model of lines used by the JTables, columns
            //  have full size at start.
            table.setModel( models[type] );
            table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

            //  Use a cell renderer better suited for the numbers we
            //  display.
            table.setDefaultRenderer( Double.class, new NumberCellRenderer() );
            table.setDefaultRenderer( Float.class, new NumberCellRenderer() );
            table.setDefaultRenderer( Integer.class, new NumberCellRenderer() );

            //  Selecting a row makes that line current.
            table.getSelectionModel().addListSelectionListener
                (
                 new ListSelectionListener()
                 {
                     public void valueChanged( ListSelectionEvent e )
                     {
                         selectionChanged( e );
                     }
                 } 
                );
            
            //  The default column width is not enough for exponent
            //  numbers, so increase it to 100 (default == 75).
            TableColumnModel tModel = table.getColumnModel();
            for ( int i = 0; i < table.getColumnCount(); i++ ) {
                tModel.getColumn( i ).setWidth( 100 );
                tModel.getColumn( i ).setPreferredWidth( 100 );
            }
        }
        
        //  Return the model.
        return models[type];
    }
    
    /**
     * Remove a view for a fit type.
     *
     * @param type the type of fit to be tabulated. One of the pre-defined
     *             types "QUICK", "GAUSS", "LORENTZ" or "VOIGT".
     */
    public void removeView( int type )
    {
        tableView.delete( LineProperties.NAMES[type] );
        models[type] = null;
    }

    /**
     * Invoked when the selected line changes.
     */
    protected void selectionChanged( ListSelectionEvent e )
    {
        int index = e.getFirstIndex();
        //  TODO: inform any listeners?
    }

    /**
     * Select a line of all tables. This should correspond to an individual
     * line.
     */
    public void selectLine( int row )
    {
        for ( int i = 0; i < LineProperties.NTYPES; i++ ) {
            if ( tableView.exists( LineProperties.NAMES[i] ) ) {
                tableView.get( LineProperties.NAMES[i] ).
                    getSelectionModel().addSelectionInterval( row, row );
            }
        }
    }

    /**
     * Add a line and its measurements to a model.
     *
     * @param type the type of line to be added (also define target model).
     * @param values the values of the line.
     */
    public void addLine( int type, double[] values )
    {
        if ( models[type] != null ) {
            LineProperties props = new LineProperties( type, plot );
            props.setFields( values );
            models[type].addLine( props );
        }
    }

    /**
     * Add or update an existing line and its measurements in a a model. The
     * line is updated if a line with its ID field already exists, otherwise a
     * new line is created.
     *
     * @param type the type of line to be added (also define target model).
     * @param values the values of the line.
     */
    public void addOrUpdateLine( int type, double[] values )
    {
        if ( models[type] != null ) {
            LineProperties props = models[type].matchID( values );
            if ( props == null ) {
                props = new LineProperties( type, plot );
                props.setFields( values );
                models[type].addLine( props );
            }
            else {
                props.setFields( values );
                models[type].changeRange( (int) values[0] );
            }
        }
    }

    /**
     * Clear all results.
     */
    public void clear()
    {
        for ( int i = 0; i < LineProperties.NTYPES; i++ ) {
            if ( models[i] != null ) {
                int size = models[i].getRowCount();
                for ( int j = size - 1; j >= 0; j-- ) {
                    models[i].removeLine( j );
                }
            }
        }
    }

//
//  Utilities for writing results to a disk file.
//
    /**
     * File chooser used for writing text files.
     */
    protected BasicFileChooser fileChooser = null;

    /**
     * Initialise the file chooser to have the necessary filters.
     */
    protected void initFileChooser()
    {
        if ( fileChooser == null ) {
            fileChooser = new BasicFileChooser( false );
            fileChooser.setMultiSelectionEnabled( false );

            //  Add a filter for text files.
            BasicFileFilter textFileFilter =
                new BasicFileFilter( "txt", "TEXT files" );
            fileChooser.addChoosableFileFilter( textFileFilter );

            //  But allow all files as well.
            fileChooser.addChoosableFileFilter
                ( fileChooser.getAcceptAllFileFilter() );
        }
    }

    /**
     * Initiate a file selection dialog and choose a file to contain the
     * results.
     */
    public void getFileForResults()
    {
        initFileChooser();
        int result = fileChooser.showSaveDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();

            //  If file exists then we need to overwrite. Permission
            //  please.
            if ( file.exists() ) {
                if ( file.canWrite() && file.isFile() ) {
                    int choice = JOptionPane.showConfirmDialog
                        ( this, "The file '" + file.getName() +
                          "' already exists.\nDo you want to overwrite it?",
                          "File exists",
                          JOptionPane.YES_NO_OPTION );
                    if ( choice == JOptionPane.NO_OPTION ) {
                        return;
                    }
                }
                else {

                    //  File exists, but is directory or read-only, so
                    //  cannot overwrite.
                    JOptionPane.showMessageDialog
                        ( this, "The file '" + file.getName() +
                          "' cannot be overwritten",
                          "Error writing ",
                          JOptionPane.ERROR_MESSAGE );
                    return;
                }
            }
            writeResultsToFile( file );
        }
    }

    /**
     * Write the results from each fit type to a file. The layout of
     * this file is simple (for now) and has the following format.
     * <pre>
     *    # File created by <program>.
     *
     *    # Quick results:
     *    # column_name_1 .... column_name_n
     *    index ....
     *
     *    # Gaussian results:
     *    # column_name_1 ....
     * </pre>
     * @param file reference to the file.
     */
    public void writeResultsToFile( File file )
    {
        //  Get a BufferedWriter to write the file line-by-line.
        FileOutputStream f = null;
        PrintStream p = null;
        try {
            f = new FileOutputStream( file );
            p = new PrintStream( f );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            return;
        }

        // Add a header to the file.
        p.println( "# File created by " + Utilities.getReleaseName() );

        // Now write the data. Check for each known line fitting type
        // and write out the results for each LineProperties object.
        for ( int i = 0; i < LineProperties.NTYPES; i++ ) {
            if ( models[i] != null ) {
                int size = models[i].getRowCount();
                if ( size > 0 ) {
                    p.println();
                    writeHeader( p, i );
                    for ( int j = 0; j < size; j++ ) {
                        p.println( models[i].getLine( j ).toString() );
                    }
                }
            }
        }
        p.close();
        try {
            f.close();
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Write a formatted header section.
     */
    protected void writeHeader( PrintStream p, int type )
    {
        p.println( "# " + LineProperties.NAMES[type] + " results:" );
        StringBuffer names = new StringBuffer( "# " );
        for ( int i = 0; i < LineProperties.count( type ); i++ ) {
            names.append( LineProperties.getName( type, i ) + "\t" );
        }
        p.println( names );
    }

    /**
     * Create an action for starting to write to a disk file.
     */
    public Action getWriteAction( String name, Icon icon )
    {
        return new WriteAction( name, icon );
    }

    /**
     * Inner Action that writes ranges to a disk file.
     */
    protected class WriteAction extends AbstractAction
    {
        public WriteAction( String name, Icon icon )
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae )
        {
            getFileForResults();
        }
    }

}
