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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

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
implements ActionListener, ItemListener
{

    /**
     * Menu components to ask the user for new server information
     */

    private int status = 0; 
    private JPanel centrePanel = null;
    private JPanel buttonsPanel = null;

    private JTextField shortNameField;
    private JTextField descriptionField;
    private JTextField titleField;
    private JTextField accessURLField;
    private JPanel dataSourcePanel;
    private JPanel bandPanel;
    private JLabel statusLabel;
    private ButtonGroup srcGroup;
    

    
    private String type="";
    private String[] waveBandValue;
    private String dataSourceValue="";
    
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
    
    public AddNewServerFrame( String type ) {
        this.type = type;
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
        centrePanel.setBorder( BorderFactory.createTitledBorder( "Add New Service" ) );

        buttonsPanel = new JPanel( new BorderLayout() );
        getContentPane().add( buttonsPanel, BorderLayout.PAGE_END );

    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    private void initFrame()
    {
        if ( type.equals("OBSCORE")) {
            setTitle( "Add New OBSCORE Service" );
        } else {
            setTitle( "Add New SSAP Service" );
        }
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 500, 400 ) );
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

        
        bandPanel = new JPanel();
        bandPanel.setBorder( BorderFactory.createTitledBorder("Wave Band"));
        JCheckBox radButton = new JCheckBox("Radio");
        radButton.addItemListener(this);
        radButton.setName("band");
        bandPanel.add(radButton);
        JCheckBox mmButton = new JCheckBox("Millimeter");
        mmButton.addItemListener(this);
        mmButton.setName("band");
        bandPanel.add(mmButton);
        JCheckBox irButton = new JCheckBox("IR");
        irButton.addItemListener(this);
        irButton.setName("band");
        bandPanel.add(irButton);
        JCheckBox optButton = new JCheckBox("Optical");
        optButton.addItemListener(this);
        optButton.setName("band");
        bandPanel.add(optButton);
        JCheckBox uvButton = new JCheckBox("UV");
        uvButton.addItemListener(this);
        uvButton.setName("band");
        bandPanel.add(uvButton);
        JCheckBox euvButton = new JCheckBox("EUV");
        euvButton.addItemListener(this);
        euvButton.setName("band");
        bandPanel.add(euvButton);
        JCheckBox xrButton = new JCheckBox("X-ray");
        xrButton.addItemListener(this);
        xrButton.setName("band");
        bandPanel.add(xrButton);
        JCheckBox grButton = new JCheckBox("Gamma-ray");
        grButton.addItemListener(this);
        grButton.setName("band");
        bandPanel.add(grButton);
        
        centrePanel.add( bandPanel );
        
        dataSourcePanel = new JPanel();
        dataSourcePanel.setBorder( BorderFactory.createTitledBorder("Data Source"));
        
        JCheckBox src_sur = new JCheckBox("Survey", false);
        src_sur.setName("src");
        src_sur.addItemListener(this);
        dataSourcePanel.add(src_sur);
        JCheckBox src_tmod = new JCheckBox("Theory", false);
        src_tmod.setName("src");
        src_tmod.addItemListener(this);
        dataSourcePanel.add(src_tmod);
        JCheckBox src_point = new JCheckBox("Pointed", false);
        src_point.setName("src");
        src_point.addItemListener(this);
        dataSourcePanel.add(src_point);
        JCheckBox src_cust = new JCheckBox("Custom", false);
        src_cust.setName("src");
        src_cust.addItemListener(this);
        dataSourcePanel.add(src_cust);
        JCheckBox src_art = new JCheckBox("Artificial", false);
        src_art.setName("src");
        src_art.addItemListener(this);
        dataSourcePanel.add(src_art);
        srcGroup = new ButtonGroup();
        
      
        srcGroup.add(src_sur);
        srcGroup.add(src_tmod);
        srcGroup.add(src_point);
        srcGroup.add(src_cust);
        srcGroup.add(src_art);
        
       
                
        centrePanel.add( dataSourcePanel );
        
        // the status information line
        JPanel statusPanel = new JPanel(new BorderLayout() );
        statusLabel = new JLabel( "", JLabel.CENTER );
        statusLabel.getAccessibleContext().setAccessibleName("Status");
        statusPanel.add( statusLabel );
        centrePanel.add( statusPanel );
        
        

        // the action buttons

        //  Add action to add the new server to the list
        JButton addButton = new JButton( "Add New Service" );
        addButton.setActionCommand( "add" );
        //addButton.setToolTipText( "Add new server" );
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
                closeWindowEvent();
            }
        }
        if ( command.equals( "reset" ) ) // reset text fields
        {
            statusLabel.setText(new String(""));
            resetFields();
            resetButtons();
        }
        if ( command.equals( "close" ) ) // close window
        {
            closeWindowEvent();
        }
        // if everything OK (status changed) fire an event - this will cause the
        // new resource to be added to the list
        statusChange.firePropertyChange("AddNewServer", oldstatus, status);
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
     *  Reset all buttons
     */
    private void resetButtons() 
    {
        for(Component c : bandPanel.getComponents()) {
            if(c instanceof JCheckBox ) {  
                ((JCheckBox) c).setSelected(false);
            }
        }
        
        srcGroup.clearSelection();
    }

    /**
     *  itemStateChanged
     *  process checked/unchecked buttons
     */
    public void itemStateChanged(ItemEvent iev) {
        JCheckBox cb = (JCheckBox) iev.getSource();
        String name = cb.getName();

        if (name.equals("src")) {
            dataSourceValue=cb.getText();

        } else  if (name.equals("band")) {
            ArrayList<String> wb = new ArrayList<String>();

            for(Component c : bandPanel.getComponents()) {
                if(c instanceof JCheckBox && ((JCheckBox) c).isSelected()) {  
                    wb.add(((JCheckBox) c).getText());
                }
            }
            waveBandValue=(String[]) wb.toArray(new String[wb.size()]);
        }

    }
    /**
     *  Sets the new resource to be added to the server list
     */
    private void setResource()
    {
            newResource = new SSAPRegResource(shortNameField.getText(), titleField.getText(), descriptionField.getText(), accessURLField.getText(), waveBandValue, dataSourceValue ); 
           
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
    public SSAPRegResource getResource()
    {
        return newResource;
    }
    public String getShortName()
    {
        return newResource.getShortName();
    }
    public String getServerTitle()
    {
        return newResource.getTitle();
    }
    public String getAccessURL()
    {
         return newResource.getCapabilities()[0].getAccessUrl();
    }
    public String getDescription()
    {
        return newResource.getCapabilities()[0].getDescription();
    }
    public String[] getwaveBand()
    {
        return newResource.getWaveband();
    }
    public String getDataSource()
    {
        return newResource.getCapabilities()[0].getDataSource();
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
