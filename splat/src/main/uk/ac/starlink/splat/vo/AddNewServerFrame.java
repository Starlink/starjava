/*
 * Copyright (C) 2001-2005 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     22-NOV-2011 (Margarida Castro Neves mcneves@ari.uni-heidelberg.de)
 *        Original version.
 */
package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.net.MalformedURLException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

//import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.vo.RegResource;

/**
 * Class AddNewServerFrame
 * 
 * This class allows manually adding a SSAP server to the server list. 
 * The AddServerFrame object is used by the SSAServerFrame class.
 *
 * @author Margarida Castro Neves
 */
public class AddNewServerFrame
extends JFrame
implements ActionListener
{

    /**
     * Menu components to ask the user for new server information
     */

    private JTextField shortNameField;
    private JTextField descriptionField;
    private JTextField titleField;
    private JTextField accessURLField;
    private JLabel statusLabel;

    private int status = 0; 
    private JPanel centrePanel = null;
    private JPanel buttonsPanel = null;

    /**
     * The server list to be updated
     */
    private PropertyChangeSupport statusChange;
    private SSAPRegResource newResource;

    /**
     * Create an instance.
     */
    public AddNewServerFrame()
    {
        initUI();
        initMenus();
        initFrame();
        status=0;
        statusChange= new PropertyChangeSupport(this); 
    }

    /**
     * Initialise the main part of the user interface.
     */
    private void initUI()
    {

        getContentPane().setLayout( new BorderLayout() );
        centrePanel = new JPanel();
        centrePanel.setLayout( new BoxLayout(centrePanel, BoxLayout.PAGE_AXIS ));
        getContentPane().add( centrePanel, BorderLayout.CENTER );
        centrePanel.setBorder( BorderFactory.createTitledBorder( "Add New Server" ) );

        buttonsPanel = new JPanel( new BorderLayout() );
        getContentPane().add( buttonsPanel, BorderLayout.PAGE_END );

    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    private void initFrame()
    {
        setTitle( "Add New SSAP Server" );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 500, 200 ) );
        setVisible( true );
    }

    /**
     * Initialise the menu bar, action bar and related actions.
     */
    private void initMenus()
    {
        // Add the menu and form items to the panels

        // The formular 
        JPanel shortNamePanel= new JPanel( new BorderLayout() );
        JLabel shortNameLabel = new JLabel( "Short name:" );
        shortNameField = new JTextField( 30 );
        shortNamePanel.add( shortNameLabel, BorderLayout.LINE_START );
        shortNamePanel.add( shortNameField,  BorderLayout.LINE_END );
        centrePanel.add( shortNamePanel );

        JPanel titlePanel= new JPanel( new BorderLayout() );
        JLabel titleLabel = new JLabel( "Title:" );
        titleField = new JTextField( 30 );
        titlePanel.add( titleLabel, BorderLayout.LINE_START );
        titlePanel.add( titleField, BorderLayout.LINE_END );
        centrePanel.add( titlePanel );

        JPanel descriptionPanel= new JPanel( new BorderLayout() );
        JLabel descriptionLabel = new JLabel( "Description:" );
        descriptionField = new JTextField(30);
        descriptionPanel.add( descriptionLabel, BorderLayout.LINE_START );
        descriptionPanel.add( descriptionField, BorderLayout.LINE_END );
        centrePanel.add( descriptionPanel );

        JPanel accessURLPanel = new JPanel( new BorderLayout() );
        JLabel accessURLLabel = new JLabel( "Access URL:" );
        accessURLField = new JTextField( 30 );
        accessURLPanel.add( accessURLLabel, BorderLayout.LINE_START );
        accessURLPanel.add( accessURLField, BorderLayout.LINE_END );
        centrePanel.add( accessURLPanel );

        // the status information line
        JPanel statusPanel = new JPanel(new BorderLayout() );
        statusLabel = new JLabel( "", JLabel.CENTER );
        statusPanel.add( statusLabel );
        centrePanel.add( statusPanel );

        // the action buttons

        //  Add action to add the new server to the list
        JButton addButton = new JButton( "Add New Server" );
        addButton.setActionCommand( "add" );
        addButton.setToolTipText( "Add new server" );
        addButton.addActionListener( this );
        buttonsPanel.add( addButton, BorderLayout.LINE_START );

        //  Add action to do reset the form
        JButton resetButton = new JButton( "Reset" );
        resetButton.setActionCommand( "reset" );
        resetButton.setToolTipText( "Clear all fields" );
        resetButton.addActionListener( this );
        buttonsPanel.add( resetButton, BorderLayout.CENTER );

        //  Add an action to close the window.
        JButton closeButton = new JButton( "Close" );
        centrePanel.add( closeButton );
        closeButton.addActionListener( this );
        closeButton.setActionCommand( "close" );
        closeButton.setToolTipText( "Cancel and Close window" );
        buttonsPanel.add( closeButton, BorderLayout.LINE_END );

    }

    /**
     *  action performed
     *  process the actions
     */
    public void actionPerformed(ActionEvent e) {

        Object command = e.getActionCommand();
        int oldstatus = status; // save old status value

        if ( command.equals( "add" ) ) // add new server to list
        {
            // first, validate the input
            status = validateResource(shortNameField.getText(), accessURLField.getText());
            if ( status==1) {
                setResource(); // create a new resoure object
                statusLabel.setText(new String( "Added "+shortNameField.getText()+ "("+ accessURLField.getText()+ ")") );
            }
        }
        if ( command.equals( "reset" ) ) // reset text fields
        {
            statusLabel.setText(new String(""));
            resetFields();
        }
        if ( command.equals( "close" ) ) // close window
        {
            closeWindowEvent();
        }
        // if everything OK (status changed) fire an event - this will cause the
        // new resource to be added to the list
        statusChange.firePropertyChange("status", oldstatus, status);
        status=0;
    }

    /**
     *  Close (hide) the window.
     */
    private void closeWindowEvent()
    {
        this.setVisible( false );
    }

    /**
     *  Reset all fields
     */
    private void resetFields() 
    {
        titleField.setText("");
        descriptionField.setText("");
        shortNameField.setText("");
        accessURLField.setText("");
    }


    /**
     *  Sets the new resource to be added to the server list
     */
    private void setResource()
    {
        newResource = new SSAPRegResource(shortNameField.getText(), titleField.getText(), descriptionField.getText(), accessURLField.getText()); 

    }

    /**
     *  Validate user's input. 
     *  Only minimal validation is performed.
     *  Returns 1 if no error occurred, 0 otherwise.
     */
    protected int validateResource( String shortName, String accessURL ) 
    {
        int ok = 0;
        statusLabel.setForeground(Color.red);
        if ( shortName.trim().length() == 0 ) {
            // short name cannot be null

            statusLabel.setText(new String( "Short Name cannot be empty!") );

        } else if  ( accessURL.trim().length() == 0 ) {
            // access url cannot be null

            statusLabel.setText(new String( "Access URL cannot be empty!") );

        } else if ( validateURL(accessURL) == false ) {
            // check URL syntax 

            statusLabel.setText(new String( "Malformed URL: "+accessURL));

        } else {
            // else OK

            statusLabel.setForeground(Color.black);
            ok=1;
        }
        return ok;
    }

    /**
     *  Returns the new resource to be added to the list
     */
    public RegResource getResource()
    {
        return newResource;
    }

    /**
     *  Register new Property Change Listener
     */

    public void addPropertyChangeListener(PropertyChangeListener l) 
    {
        statusChange.addPropertyChangeListener(l);
    }


    /**
     *  URL Validation
     */
    public boolean validateURL(String urlStr) 
    {
        try {
            new URL(urlStr);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
