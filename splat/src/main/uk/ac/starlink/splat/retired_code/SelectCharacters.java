package uk.ac.starlink.splat.iface;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.Utilities;

/**
 * SelectCharacters provides a frame that displays all the characters
 * in a given font. A series of characters may be selected and sent to
 * registered listener objects that implement the
 * SelectCharactersListener interface and register themselves.
 *
 * @since $Date$
 * @since 03-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 * @see InsertCharacters, SelectCharactersModel
 */
public class SelectCharacters extends JFrame
{
    /**
     * The font to display.
     */
    private Font displayFont = null;

    /**
     * The JFrame content panel.
     */
    JPanel contentPane = null;

    /**
     * Menu bar components
     */
    JMenuBar menuBar = new JMenuBar();
    JMenu menuFile = new JMenu();
    JMenuItem menuFileCancel = new JMenuItem();
    JMenu menuHelp = new JMenu();
    JMenuItem menuHelpAbout = new JMenuItem();

    /**
     * The actions bar.
     */
    JPanel actionBar = new JPanel();

    /**
     * ScrollPane for control view of table.
     */
    JScrollPane scrollPane = new JScrollPane();

    /**
     * Table that displays the characters in the font.
     */
    JTable table = new JTable();

    /**
     * JTextField for displaying the selected characters.
     */
    JTextField text = new JTextField();

    /**
     * The model of the character table.
     */
    SelectCharactersModel model = null;

    /**
     * Create an instance of this class. Accepts the font that is to
     * be displayed.
     */
    public SelectCharacters( Font displayFont )
    {
        try {
            initUI();
            setDisplayFont( displayFont );
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
        setup( "Select Special Characters" );
    }

    /**
     *  Make the frame visible and set the default action for when we
     *  are closed.
     */
    protected void setup( String title )
    {
        enableEvents( AWTEvent.WINDOW_EVENT_MASK );
        setTitle( Utilities.getTitle( title ) );
        setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        pack();
        setVisible( true );
    }

    /**
     * Set the font that that is displayed.
     */
    public void setDisplayFont( Font displayFont )
    {
        this.displayFont = displayFont;

        //  Do initialisation/reinitialisation of interface to display
        //  this font.
        initFont();
    }

    /**
     * Initialise the interface components.
     */
    private void initUI() throws Exception
    {
        // Create images for the action bar.
        ImageIcon acceptimage = new ImageIcon(
            ImageHolder.class.getResource( "accept.gif" ) );
        ImageIcon cancelimage = new ImageIcon(
            ImageHolder.class.getResource( "exit.gif" ) );
        ImageIcon helpimage = new ImageIcon(
            ImageHolder.class.getResource( "help.gif" ) );

        //  Get the content Pane and give it a BorderLayout
        contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout( new BorderLayout() );

        //  Set the size of the full window.
        this.setSize(new Dimension(400, 300));

        //  Set the table properties. This has no header and is set to
        //  select only single cells.
        table.setToolTipText( "Select a character" );
        table.setTableHeader( null );
        table.setCellSelectionEnabled( true );
        table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        ListSelectionModel listModel = table.getSelectionModel();
        listModel.addListSelectionListener(
            new ListSelectionListener() {
                    public void valueChanged( ListSelectionEvent e ) {
                        cellSelected( e );
                    }
                });

        //  Add the menubar.
        this.setJMenuBar( menuBar );

        //  Setup the file menu.
        menuFile.setText( "File" );
        menuBar.add( menuFile );

        //  Text field.
        text.setToolTipText( "Editable list of selected characters" );

        //  Action bar uses a BoxLayout.
        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );

        //  Create an action to accept the selected characters.
        AcceptAction acceptAction  = new AcceptAction( "Accept", acceptimage );
        menuFile.add( acceptAction );
        JButton acceptButton = new JButton( acceptAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( acceptButton );
        acceptButton.setToolTipText(
            "Accept the selected characters and close window" );

        //  Create an action to cancel selection.
        CancelAction cancelAction  = new CancelAction( "Cancel", cancelimage );
        menuFile.add( cancelAction );
        JButton cancelButton = new JButton( cancelAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( cancelButton );
        cancelButton.setToolTipText(
            "Close window, cancelling selected characters" );

        actionBar.add( Box.createGlue() );

        //  Add an action to display the about dialog.
        Action aboutAction = AboutFrame.getAction( this );
        menuHelp.add( aboutAction );

        //  Add the components to the content panel
        contentPane.add( scrollPane, BorderLayout.CENTER );
        JPanel actions = new JPanel( new BorderLayout() );
        contentPane.add( actions, BorderLayout.SOUTH );

        //  Add table to the ScrollPane.
        scrollPane.getViewport().add( table, null );

        //  Text field and actionbar go at bottom.
        actions.add( actionBar, BorderLayout.SOUTH );
        actions.add( text, BorderLayout.NORTH );
    }

    /**
     * Close window, either sending the text to any registered
     * listeners, or not.
     */
    protected void closeWindowEvent( boolean accept )
    {
        if ( accept ) {
            fireExiting();
        }
        this.dispose();
    }

    /**
     * Initialise the interface to use the display font.
     */
    protected void initFont()
    {
        // Create a model that uses this font and add it to the
        // table.
        model = new SelectCharactersModel( displayFont );
        table.setModel( model );

        // Table uses a scaled version of font, that display size is
        // matched.
        table.setFont( displayFont.deriveFont( 12.0F ) );

        //  Text field uses full sized font.
        text.setFont( displayFont );
    }

    /**
     * A cell has been selected.
     */
    protected void cellSelected( ListSelectionEvent e )
    {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        text.replaceSelection( (String) model.getValueAt( row, col ));
        table.clearSelection();
    }

    /**
     *  Inner class defining Action for accepting characters.
     */
    protected class AcceptAction extends AbstractAction
    {
        public AcceptAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            closeWindowEvent( true );
        }
    }

    /**
     *  Inner class defining Action for cancelling acceptance of
     *  characters.
     */
    protected class CancelAction extends AbstractAction
    {
        public CancelAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            closeWindowEvent( false );
        }
    }

//
//  Listeners modules.
//
    protected EventListenerList listeners = new EventListenerList();

    /**
     * Registers a listener who wants to be informed about the final
     * character string (when the window exits).
     *
     *  @param l the SelectCharactersListener
     */
    public void addListener( SelectCharactersListener l ) 
    {
        listeners.add( SelectCharactersListener.class, l );
    }

    /**
     * Send SelectCharactersEvent event to all listeners.
     *
     */
    protected void fireExiting() 
    {
        Object[] la = listeners.getListenerList();
        SelectCharactersEvent e = null;
        for ( int i = la.length - 2; i >= 0; i -= 2 ) {
            if ( la[i] == SelectCharactersListener.class ) {
                if ( e == null ) {
                    e = new SelectCharactersEvent( this, text.getText() );
                }
                ((SelectCharactersListener)la[i+1]).newCharacters( e );
            }
        }
    }
}
