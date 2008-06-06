/*
 * Copyright (C) 2007 Particle Physics and Astronomy Research Council
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     23-MAR-2007 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;
import java.util.Iterator;

import javax.print.attribute.PrintRequestAttributeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;


import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.PrintUtilities;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.util.gui.ErrorDialog;

import jsky.util.gui.PrintableJTable;

/**
 * Window for viewing the FITS cards of a spectrum. 
 *
 * @author Peter Draper
 * @version $Id$
 */
public class FITSHeaderFrame 
    extends JFrame
{
    /**
     * UI preferences.
     */
    private static Preferences prefs =
        Preferences.userNodeForPackage( FITSHeaderFrame.class );

    /** 
     * The {@link JTable} used to display the cards decomposed into keyword,
     * value and comment.
     */
    private PrintableJTable table = null;

    /**
     * {@link SpecData} instance to view.
     */
    private SpecData specData = null;

    /**
     * Menubar and various menus and items that it contains.
     */
    private JMenuBar menuBar = null;
    private JMenu fileMenu = null;

    /**
     * Printer page settings.
     */
    private PrintRequestAttributeSet pageSet = null;

    /**
     * Create an instance. 
     *
     * @param specData A {@link SpecData} instance.
     */
    public FITSHeaderFrame( SpecData specData )
    {
        this.specData = specData;
        initUI();
        initFrame();
        updateDisplay();
    }

    /**
     * Initialisation of the UI.
     */
    private void initUI()
    {
        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout( new BorderLayout() );

        //  Add the menuBar.
        menuBar = new JMenuBar();
        setJMenuBar( menuBar );

        //  Create the File menu.
        fileMenu = new JMenu( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Name of the spectrum.
        SplatName splatName = new SplatName( specData );
        TitledBorder splatTitle =
            BorderFactory.createTitledBorder( "Spectrum:" );
        splatName.setBorder( splatTitle );

        //  Goes at the top of window.
        add( splatName, BorderLayout.NORTH );

        //  Create the table.
        table = new PrintableJTable();

        table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        table.setIntercellSpacing( new Dimension( 6, 3 ) );
        table.setRowSelectionAllowed( true );
        table.setColumnSelectionAllowed( false );
        JTableHeader header = table.getTableHeader();
        header.setUpdateTableInRealTime( false );

        JScrollPane scrollPane = new JScrollPane( table );
        TitledBorder scrollTitle =
            BorderFactory.createTitledBorder( "FITS headers:" );
        scrollPane.setBorder( scrollTitle );
        
        // Goes in the center and takes any extra space.
        add( scrollPane, BorderLayout.CENTER );

        // Action bar uses a BoxLayout and is placed at the south.
        JPanel actionBar = new JPanel();
        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        add( actionBar, BorderLayout.SOUTH );
        
        // Print the headers.
        PrintAction printAction = new PrintAction();
        fileMenu.add( printAction ).setMnemonic( KeyEvent.VK_P );

        // Add an action to close the window (appears in File menu and action
        // bar).
        CloseAction closeAction = new CloseAction();
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        actionBar.add( Box.createGlue() );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    private void initFrame()
    {
        setTitle( Utilities.getTitle( "FITS Headers" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        Utilities.setFrameLocation( this, null, prefs, "FITSHeaderFrame" );
        pack();
        setVisible( true );
    }

    /**
     * Update the display to show the FITS headers of the spectrum
     */
    public void updateDisplay() 
    {
        Header header = null;
        if ( specData != null ) {
            header = specData.getHeaders();
        }

        //  Stop with null table if no spectrum or no headers.
        if ( specData == null || header == null ) {
            table.setModel( new DefaultTableModel() );
            return;
        }

        String[] columnNames = { "Keyword", "Value", "Comment" };
        int numKeywords = header.getNumberOfCards();
        String[][] values = new String[numKeywords][3];
        Iterator it = header.iterator();
        int n = 0;
        while ( it.hasNext() ) {
            HeaderCard card = (HeaderCard) (it.next());
            String key = card.getKey();
            String value = card.getValue();
            String comment = card.getComment();
            //  A blank keyword is really a comment line.
            if ( "".equals( key ) ) {
                value = comment;
                comment = "";
            }
            values[n][0] = key;
            values[n][1] = value;
            values[n++][2] = comment;
        }
        table.setModel( new DefaultTableModel( values, columnNames ) );

        //  Set default widths, matched to FITS format.
        TableColumn column = table.getColumnModel().getColumn( 0 );
        column.setPreferredWidth( 100 );
        column = table.getColumnModel().getColumn( 1 );
        column.setPreferredWidth( 300 );
        column = table.getColumnModel().getColumn( 2 );
        column.setPreferredWidth( 450 );
    }

    /**
     * Print headers.
     */
    protected void printHeaders()
    {
        Rectangle bounds = table.getBounds();
        if ( pageSet == null ) {
            pageSet = PrintUtilities.makePageSet( false );
        }
        try {
            PrintUtilities.print( table, pageSet, bounds, "out.ps", false );
        }
        catch (SplatException e) {
            ErrorDialog.showError( this, e );
        }
    }

    /**
     * Close the window.
     */
    protected void closeWindowEvent()
    {
        Utilities.saveFrameLocation( this, prefs, "FITSHeaderFrame" );
        dispose();
    }

    private final static ImageIcon closeImage =
        new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction
        extends AbstractAction
    {
        public CloseAction()
        {
            super( "Close", closeImage );
            putValue( SHORT_DESCRIPTION, "Close window" );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control W" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            closeWindowEvent();
        }
    }

    // Print the headers.
    private final static ImageIcon printImage = 
        new ImageIcon( ImageHolder.class.getResource( "print.gif" ) );

    /**
     * Inner class defining Action for printing window.
     */
    protected class PrintAction
        extends AbstractAction
    {
        public PrintAction()
        {
            super( "Print", printImage );
            putValue( SHORT_DESCRIPTION, "Print headers" );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control P" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            printHeaders();
        }
    }
}

