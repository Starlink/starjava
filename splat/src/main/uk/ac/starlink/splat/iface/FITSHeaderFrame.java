/*
 * Copyright (C) 2007 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     18-APR-2005 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;

import uk.ac.starlink.splat.data.FITSHeaderSource;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.Utilities;

/**
 * Window for viewing and modifying the FITS cards of a spectrum. 
 * <p>
 * The spectrum is chosen from the a list of spectra, which are rendered 
 * in a JComboBox.
 *
 * @author Peter Draper
 * @version $Id$
 */
public class FITSHeaderFrame 
    extends JFrame
    implements ActionListener
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
    private JTable table = null;

    /**
     * {@link JComboBox} used to select a spectrum.
     */
    private JComboBox nameList = null;

    /**
     * {@link SpecDataComp} containing the list of spectra.
     */
    private SpecDataComp spectra = null;

   /**
    * Menubar and various menus and items that it contains.
    */
    private JMenuBar menuBar = null;
    private JMenu fileMenu = null;

    /**
     * Create an instance. 
     *
     * @param spectra {@link SpecDataComp}instance containing all the
     *                spectra to view/modify.
     */
    public FITSHeaderFrame( SpecDataComp spectra )
    {
        this.spectra = spectra;
        initUI();
        initFrame();
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
        fileMenu = new JMenu();
        setJMenuBar( menuBar );

        //  The Spectral selector JComboBox.
        nameList = new JComboBox();

        //  The list of names uses a special renderer to also display
        //  some of the line properties.
        nameList.setRenderer( new LineRenderer( nameList ) );

        //  The nameList uses the SpecDataComp as its model.
        nameList.setModel( spectra );

        //  JComboBox sets default size this way!
        nameList.
            setPrototypeDisplayValue( "                                    " );

        //  When the current spectrum is modified we need to update the
        //  display.
        nameList.addActionListener( this );

        //  Goes at the top of window.
        add( nameList, BorderLayout.NORTH );

        //  Create the table.
        table = new JTable();

        table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        table.setIntercellSpacing( new Dimension( 6, 3 ) );
        table.setRowSelectionAllowed( true );
        table.setColumnSelectionAllowed( false );
        JTableHeader header = table.getTableHeader();
        header.setUpdateTableInRealTime( false );

        JScrollPane scrollPane = new JScrollPane( table );
        
        //  Goes in the center and takes any extra space.
        add( scrollPane, BorderLayout.CENTER );

        // Action bar uses a BoxLayout and is placed at the south.
        JPanel actionBar = new JPanel();
        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        add( actionBar, BorderLayout.SOUTH );

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
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "FITS Headers" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        Utilities.setFrameLocation( this, null, prefs, "FITSHeaderFrame" );
        pack();
        setVisible( true );
    }

    /**
     * Update the display to show the FITS headers of the current spectrum
     */
    public void updateDisplay() 
    {
        SpecData specData = spectra.getCurrentSpectrum();
        if ( specData == null || ! ( specData instanceof FITSHeaderSource ) ) {
            table.setModel( new DefaultTableModel() );
            return;
        }

        String[] columnNames = { "Keyword", "Value", "Comment" };
        Header header = ((FITSHeaderSource)specData).getFitsHeaders();
        int numKeywords = header.getNumberOfCards();
        String[][] values = new String[numKeywords][3];
        Iterator it = header.iterator();
        int n = 0;
        while ( it.hasNext() ) {
            HeaderCard card = (HeaderCard) (it.next());
            String name = card.getKey();
            String value = card.getValue();
            String comment = card.getComment();
            values[n][0] = name;
            values[n][1] = value;
            values[n++][2] = comment;
        }
        table.setModel( new DefaultTableModel( values, columnNames ) );
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
    

    //
    // Implement ActionListener interface.
    //

    /**
     * Respond to selection of a new spectrum as the current one.
     *
     * @param e object describing the event.
     */
    public void actionPerformed( ActionEvent e )
    {
        updateDisplay();
    }
}

