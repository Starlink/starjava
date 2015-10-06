package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;


import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.StarTableModel;
import uk.ac.starlink.table.gui.TableLoadPanel;
import uk.ac.starlink.util.ProxySetup;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.ProxySetupFrame;




/**
 * SSAServerTable is a panel displaying the SSA services as a table. It includes
 * also selection options, as waveband and data source options, 
 * as well as user generated tags. By rightclicking on a row, 
 * a menu will appear to allos tagging the service or displaying its information.
 *
 * @author Margarida Castro Neves 
 * @version $Id: SSAServerTree.java 10350 2012-11-15 13:27:36Z mcneves $
 *
 */


public class SSAServerTable extends JPanel  implements PropertyChangeListener {
    
    
    // the services table
    
/*   
    private static final int NRCOLS = 15;                    // the number of columns in the table

    // the table indexes

  //  private static final int SELECTED_INDEX = 0;
    private static final int SHORTNAME_INDEX = 0;
    private static final int TITLE_INDEX = 1;
    private static final int DESCRIPTION_INDEX = 2;
    private static final int IDENTIFIER_INDEX = 3;    
    private static final int PUBLISHER_INDEX = 4;
    private static final int CONTACT_INDEX = 5;
    private static final int ACCESSURL_INDEX = 6;
    private static final int REFURL_INDEX = 7;
    private static final int WAVEBAND_INDEX = 8;
    private static final int CONTTYPE_INDEX = 9;
    private static final int DATASOURCE_INDEX = 10;
    private static final int CREATIONTYPE_INDEX = 11;
    private static final int STDID_INDEX = 12;
    private static final int VERSION_INDEX = 13;
    private static final int SUBJECTS_INDEX = 14;
    // total number of servers that returned parameters
    // private int nrServers;
    // the table headers
    private String[] headers = { "short name", "title", "description", "identifier",
                                "publisher", "contact", "access URL", "reference URL", "waveband", "content type", 
                                "data source", "creation type", "stantardid", "version", "subjects", "tags"};
*/
    
    // Logger.
    private static Logger logger =
            Logger.getLogger( "uk.ac.starlink.splat.vo.SSAServerTable" );

    /**
     * The object that manages the actual list of servers.
     */
    private SSAServerList serverList = null;
    
    /**
     * File chooser for storing and restoring server lists.
     */
    protected BasicFileChooser fileChooser = null;
    
    /**
     * Frame for adding a new server.
     */
    protected AddNewServerFrame addServerWindow = null;

      
    /* where to save the tags information */
    private String tagsFile = "defaultTagsV2.xml";
    
    /**
     * The object that manages the actual list of servers.
     */

    private JPanel serverPanel;
    private JPanel controlPanel;
    
    
    // options
    private JCheckBox band_rad ;
    private JCheckBox band_mm ;
    private JCheckBox band_ir;
    private JCheckBox band_opt;
    private JCheckBox band_uv ;
    private JCheckBox band_euv;
    private JCheckBox band_xr;
    private JCheckBox band_gr;
    private JCheckBox band_all;
    private JRadioButton src_theo = null;
    private JRadioButton src_obs = null;
    
    
    // user defined tags
 //   private ArrayList<JCheckBox> userTags;
 //   private JTabbedPane optionTabs;
//    private JPanel tagsPanel;
//    private DefaultListModel tagsListModel;
//    private JList tagsList;
    private JComboBox tagCombo;
    
    // the table popupmenu
    // JPopupMenu tablePopup;
    
    // sizes
    
    private  int WIDTH = 600;
    private  int HEIGHT = 500;
   
    private CheckBoxListener checkBoxlistener = null;
   
    
    /** The cell table renderer */
   // static ServerTableRenderer renderer ;
    
    /** The proxy server dialog */
    protected ProxySetupFrame proxyWindow = null;

    private static ServerPopupTable serverTable;


    private TableRowSorter<DefaultTableModel> sorter;
    private RowFilter<DefaultTableModel, Object> obsFilter;
    private RowFilter<DefaultTableModel, Object> theoFilter;
    private RowFilter<DefaultTableModel, Object> band_GammaFilter;
    private RowFilter<DefaultTableModel, Object> band_EUVFilter;
    private RowFilter<DefaultTableModel, Object> band_XRayFilter;
    private RowFilter<DefaultTableModel, Object> band_UVFilter;
    private RowFilter<DefaultTableModel, Object> band_OpticalFilter;
    private RowFilter<DefaultTableModel, Object> band_IRFilter;
    private RowFilter<DefaultTableModel, Object> band_MmFilter;
    private RowFilter<DefaultTableModel, Object> band_RadioFilter;
    
  
    /** the tags: one tag -> many servers **/
    
    Map<String,ArrayList<String>> tagsMap;
    
    /** the tags: one server -> many tags **/
    Map<String,ArrayList<String>> serverTagsMap;

    private  String MANUALLY_ADDED_STR = "ManuallyAdded";

  

    /** Make sure the proxy environment is setup */
    static {
        ProxySetup.getInstance().restore();
    }

    /**
     * Create an instance.
     */
    public SSAServerTable( SSAServerList list )
    {
      
        this.addComponentListener(new resizeListener());
        this.serverList = list;
        serverTable = new ServerPopupTable(serverList);
        sorter = (TableRowSorter<DefaultTableModel>) serverTable.getRowSorter();
           
        initUI();
  //    
        initFilters();
        setFilters();
          
        tagsMap = new HashMap<String,ArrayList<String>>();
        serverTagsMap = new HashMap<String,ArrayList<String>>();     
        
        try {
            restoreTags();
        } catch (SplatException e) {
            // then no tags
        }  
//       
        
       
    }  

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {
    
      // this.setPreferredSize(new Dimension(this.WIDTH,this.HEIGHT));
       this.setMinimumSize(new Dimension(this.WIDTH-450,this.HEIGHT-300));
       this.setPreferredSize(new Dimension(this.WIDTH-285,this.HEIGHT-300));
      // setLayout( new BorderLayout() );
       
       /************
        * The Options panel
        ************
        */
     //  optionTabs = new JTabbedPane();
      
       JScrollPane optionsScroller = new JScrollPane();
       
       JPanel optionsPanel = new JPanel(new GridBagLayout());
       optionsPanel.setBorder(BorderFactory.createTitledBorder( "Service selection options" ));
       GridBagConstraints gbcOptions = new GridBagConstraints();
    
       checkBoxlistener = new CheckBoxListener();
       
       // Options Component: BAND 
    
       JPanel bandPanel = new JPanel (new GridLayout(3,3));
       bandPanel.setBorder ( BorderFactory.createTitledBorder( "Wave Band" ) );
       band_rad = new JCheckBox( "Radio", false);
       bandPanel.add(band_rad);
       band_mm = new JCheckBox( "Millimeter",  false);
       bandPanel.add(band_mm);
       band_ir = new JCheckBox( "Infrared",  false);
       bandPanel.add(band_ir);
       band_opt = new JCheckBox( "Optical",false);
       bandPanel.add(band_opt);
       band_uv = new JCheckBox( "UV",  false);
       bandPanel.add(band_uv);
       band_euv = new JCheckBox( "EUV",  false);
       bandPanel.add(band_euv);
       band_xr = new JCheckBox( "X-ray",  false);
       bandPanel.add(band_xr);
       band_gr = new JCheckBox( "Gamma-ray", false);
       bandPanel.add(band_gr);      
       band_all = new JCheckBox( "ALL", true);
       bandPanel.add(band_all);
       
       band_rad.addItemListener(checkBoxlistener);
       band_rad.setToolTipText("<html>any wavelength > 10 mm (or frequency < 30 GHz)</html>");
       band_mm.addItemListener(checkBoxlistener);
       band_mm.setToolTipText("<html>0.1 mm <= wavelength <= 10 mm; <BR>3000 GHz >= frequency >= 30 GHz.</html>");
       band_ir.addItemListener(checkBoxlistener);
       band_ir.setToolTipText("<html>1 micron <= wavelength <= 100 microns</html>");
       band_opt.addItemListener(checkBoxlistener);
       band_opt.setToolTipText("<html>0.3 microns <= wavelength <= 1 micron; <BR>300 nm <= wavelength <= 1000 nm; <BR>3000 Angstroms <= wavelength <= 10000 Angstroms</html>");
       band_uv.addItemListener(checkBoxlistener);
       band_uv.setToolTipText("<html>0.1 micron <= wavelength <= 0.3 microns; <BR> 100 nm <= wavelength <= 300 nm;  <BR>1000 Angstroms <= wavelength <= 3000 Angstroms</html>");
       band_euv.addItemListener(checkBoxlistener);    
       band_euv.setToolTipText("<html>100 Angstroms <= wavelength <= 1000 Angstroms; <BR>12 eV <= energy <= 120 eV</html>");
       band_xr.addItemListener(checkBoxlistener);
       band_xr.setToolTipText("<html>0.1 Angstroms <= wavelength <= 100 Angstroms; <BR>0.12 keV <= energy <= 120 keV</html>");
       band_gr.addItemListener(checkBoxlistener);
       band_gr.setToolTipText("<html>energy >= 120 keV</html>");
       band_all.addItemListener(checkBoxlistener);
      
       band_rad.setName("band");
       band_mm.setName("band");
       band_opt.setName("band");
       band_ir.setName("band");
       band_uv.setName("band");
       band_euv.setName("band");
       band_xr.setName("band");
       band_gr.setName("band");
       band_all.setName("band_all");
      
       // Options component: Content (theoretical/observed)
       
       JPanel srcPanel = new JPanel (new GridLayout(1, 2));
       srcPanel.setBorder ( BorderFactory.createTitledBorder( "Data Source" ) );
       
       ButtonGroup bg = new ButtonGroup();
       
       src_obs = new JRadioButton("Observed data", true);
       src_obs.setToolTipText("<html>All observation servers.</html>");
      
       bg.add(src_obs);       
       src_obs.setName("src_obs");
       src_obs.addItemListener(checkBoxlistener);
       
       src_theo = new JRadioButton("Theoretical data", false);
       src_theo.setToolTipText("<html>All theoretical servers.</html>");
    
       bg.add(src_theo);
       src_theo.setName("src_theo");
       src_theo.addItemListener(checkBoxlistener);
       
       srcPanel.add(src_obs);
       srcPanel.add(src_theo);
 
       // Options Component: User Defined Tags
       
       GridBagConstraints gbcTags = new GridBagConstraints();
 //      gbcTags.anchor = GridBagConstraints.LINE_START;
 //      gbcTags.fill = GridBagConstraints.HORIZONTAL;
       gbcTags.gridwidth=3;
       gbcTags.insets=new Insets(0,5,5,0);
        
       SelectTagAction selectTagAction = new SelectTagAction("select tagged services");
       JPanel tagPanel= new JPanel();
       tagPanel.setLayout(new GridBagLayout());
       tagPanel.setBorder(BorderFactory.createTitledBorder( "Tags" ));
  //     JLabel taglabel = new JLabel("CurrentTag: ");
  //     tagPanel.add(taglabel, BorderLayout.LINE_START);
       tagCombo = new JComboBox();
       tagCombo.addActionListener(selectTagAction);
       tagCombo.setPrototypeDisplayValue("------------");
       tagCombo.addItem("");
       gbcOptions.weightx=1;
       gbcOptions.gridx=0;
       gbcOptions.gridy=0;
       tagPanel.add((tagCombo),gbcTags);
       AddTagAction addTagAction = new AddTagAction( "+" );
       JButton addTagButton = new JButton(addTagAction);
       addTagButton.setToolTipText("Add tag to selected servers");
       gbcOptions.weightx=0.3;
       gbcOptions.gridx=1;

       tagPanel.add(addTagButton,gbcTags);
       RemoveTagAction removeTagAction = new RemoveTagAction( "-" );
       JButton removeTagButton = new JButton(removeTagAction);    
       removeTagButton.setToolTipText("Remove current tag");
       gbcOptions.gridx=2;
       tagPanel.add(removeTagButton,gbcTags);

       // Adding all to the Options panel
           
       gbcOptions.anchor = GridBagConstraints.NORTHWEST;
       gbcOptions.fill = GridBagConstraints.HORIZONTAL;
       gbcOptions.weightx=.5;
       gbcOptions.weighty=0;
       gbcOptions.gridx=0;
       gbcOptions.gridy=0;
     
       optionsPanel.add(srcPanel,gbcOptions);
       gbcOptions.weighty=.5;
       gbcOptions.gridy=1;
       optionsPanel.add(bandPanel,gbcOptions);
       
       gbcOptions.gridy=2;
       gbcOptions.weighty=1;
       optionsPanel.add(tagPanel, gbcOptions);
       
       JPanel invOptionsPanel = new JPanel(); // invisible, just to adjust size and position of frames

       invOptionsPanel.setLayout(new BorderLayout());
       
       invOptionsPanel.add(optionsPanel, BorderLayout.LINE_START);
      
      invOptionsPanel.getAccessibleContext().setAccessibleName("Selection by data source, wave band");
      invOptionsPanel.getAccessibleContext().setAccessibleDescription("Detailed");
       
 
       
        serverPanel=new JPanel();
        serverPanel.setLayout(new GridBagLayout() );
        serverPanel.setBorder ( BorderFactory.createTitledBorder( "SSAP Servers" ) );
        
      
        JPopupMenu popupMenu = new JPopupMenu();
   //     PopupMenuListener popupMenuListener = new PopupMenuListener();

        JMenuItem infoMenuItem = new JMenuItem("Info");
        infoMenuItem.addActionListener(new PopupMenuAction());
        popupMenu.add(infoMenuItem);

        JMenuItem addTagMenuItem = new JMenuItem("Tag");
        addTagMenuItem.addActionListener(new PopupMenuAction());
        popupMenu.add(addTagMenuItem);
      
        serverTable.setComponentPopupMenu(popupMenu);
        serverTable.addMouseListener(new ServerTableMouseListener());
        serverTable.getSelectionModel().addListSelectionListener(new SelectionListener());
        
        JScrollPane jsp = new JScrollPane(serverTable);
        
        jsp.getAccessibleContext().setAccessibleName("Services");
    //    jsp.setViewportView(mainPanel);
       // jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

       
        GridBagConstraints gbcServer=new GridBagConstraints();
        gbcServer.gridx=0;
        gbcServer.gridy=0;
        gbcServer.weighty=1;
        gbcServer.weightx=1;
        gbcServer.fill=GridBagConstraints.BOTH;
        serverPanel.add(jsp,gbcServer);
        serverTable.updateUI();
        
      
        initMenus();
        gbcServer.anchor = GridBagConstraints.SOUTHWEST;
        gbcServer.gridx=0;
        gbcServer.gridy=1;
        gbcServer.weighty=0;
        serverPanel.add(controlPanel, gbcServer);
      
        optionsScroller.getViewport().add( invOptionsPanel, null );
        optionsScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        optionsScroller.setMinimumSize(new Dimension(220,240));
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth=1;
        gbc.gridheight=1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx=1;
        gbc.weighty=0.;
        gbc.gridx=0;  
        gbc.gridy=0;
        gbc.fill = GridBagConstraints.BOTH;
        this.add(optionsScroller, gbc);
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.gridheight=GridBagConstraints.REMAINDER;
        gbc.weighty=1;
        gbc.gridx=0;  
        gbc.gridy=1;
        gbc.fill = GridBagConstraints.BOTH;
        this.add(serverPanel, gbc);
        
    }


    private void populateTable() {
        
        serverTable.setRowSorter(null); //to reset the old model
        serverTable.populate();
        //updateServerTable();
        if ( src_obs.isSelected() ) {
            sorter.setRowFilter(obsFilter);
        }
        if ( src_theo.isSelected() ) {
            sorter.setRowFilter(theoFilter);
        }
        serverTable.setRowSorter(sorter);
        
  
    }
  
    
     
    /**
     * Initialise the menu bar, action bar and related actions.
     */
    protected void initMenus()
    {
 
       //  Action bars use BoxLayouts.
        
       controlPanel = new JPanel(new GridBagLayout() );
       JPanel topActionBar = new JPanel(new GridBagLayout());
       JPanel botActionBar = new JPanel(new GridBagLayout());
       GridBagConstraints gbc = new GridBagConstraints();
    
        //  Get icons.
        Icon closeImage =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        Icon helpImage =
            new ImageIcon( ImageHolder.class.getResource( "help.gif" ) );
        Icon readImage =
            new ImageIcon( ImageHolder.class.getResource( "read.gif" ) );
        Icon saveImage =
            new ImageIcon( ImageHolder.class.getResource( "save.gif" ) );

   
        //  Add action to do read a list of servers from disk file.
        ReadAction readAction = new ReadAction( "Restore Server List", readImage );
 //       fileMenu.add( readAction );
        JButton readButton = new JButton( readAction );
     //   botActionBar.add( Box.createGlue() );
 //       botActionBar.add( readButton );
        readButton.setToolTipText( "Read server list back from a disk-file" );

        //  Add action to save the server list to disk file.
        SaveAction saveAction = new SaveAction( "Save Server List", saveImage );
//        fileMenu.add( saveAction );
        JButton saveButton = new JButton( saveAction );
   //     botActionBar.add( Box.createGlue() );
//        botActionBar.add( saveButton );
        saveButton.setToolTipText( "Save server list to a disk-file" );


        //  Remove selected servers from table.
   //     RemoveAction removeAction = new RemoveAction( "Remove selected" );
 //       optionsMenu.add( removeAction );
   //     JButton removeButton = new JButton( removeAction );
    //    topActionBar.add( removeButton, gbc );

        //  Add action to select all servers.
        SelectAllAction selectAllAction = new SelectAllAction( "Select all" );
  //      optionsMenu.add( selectAllAction );
        JButton selectAllButton = new JButton( selectAllAction );
        selectAllButton.setToolTipText( "Select all services" );

        //  Add action to select all servers.
        DeselectAllAction deselectAllAction = new DeselectAllAction( "Deselect all" );
  //      optionsMenu.add( selectAllAction );
        JButton deselectAllButton = new JButton( deselectAllAction );
        deselectAllButton.setToolTipText( "Deselect all services" );

 
  //      removeButton.setMargin(new Insets(2,2,2,2));  
        selectAllButton.setMargin(new Insets(2,2,2,2));
        //topActionBar.add( Box.createGlue() );
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.weightx=0.5;
        gbc.fill=GridBagConstraints.NONE;
        topActionBar.add( selectAllButton, gbc );
        gbc.gridx=1;
        topActionBar.add( deselectAllButton, gbc );
    //    gbc.gridx=2;
    //    topActionBar.add( saveButton, gbc );
  //      controlPanel.add( removeButton, BorderLayout.PAGE_START );
 //       removeButton.setToolTipText
 //           ( "Remove selected servers from current list" );

        //  Action to check a registry for additional/updated servers.
        QueryNewAction newAction = new QueryNewAction( "Query registry" );
   //     optionsMenu.add( newAction );
        JButton newQueryButton = new JButton( newAction );
        newQueryButton.setMargin(new Insets(2,2,2,2));  
  //      topActionBar.add( Box.createGlue() );
        gbc.gridx=0;
        botActionBar.add( newQueryButton, gbc );
       // controlPanel.add( newQueryButton, BorderLayout.PAGE_END );
        newQueryButton.setToolTipText( "Query registry for new SSAP services" );
        

        //  Add action to just delete all servers.
        DeleteAction deleteAction = new DeleteAction( "Delete all" );
        
        //  Add action to manually add a new server to the list
        AddNewAction addNewAction = new AddNewAction( "Add New Server" );
      //  JPanel addPanel = new JPanel();
      //  addPanel.setLayout(new BorderLayout());
        JButton addButton1 = new JButton(addNewAction);
        addButton1.setToolTipText("Add new service to the list");
        addButton1.setMargin(new Insets(2,2,2,2));  
        addButton1.getAccessibleContext().setAccessibleDescription("addnew");
        addButton1.getAccessibleContext().setAccessibleName("addnew");
  //      addPanel.add(addButton1);
        gbc.gridx=1;
        botActionBar.add(addButton1, gbc);
      //  gbc.gridx=2;
    //    botActionBar.add(restoreButton, gbc);
   //     mainPanel.add(addPanel, BorderLayout.EAST);
        
 //       optionsMenu.add( deleteAction );
        JButton deleteButton = new JButton( deleteAction );
 //       topActionBar.add( deleteButton );
        deleteButton.setToolTipText( "Delete all servers from current list" );

        //  Finish action bars.
          gbc.gridx=0;
          gbc.gridy=0;
          gbc.fill=GridBagConstraints.HORIZONTAL;
          controlPanel.add(topActionBar, gbc);//, BorderLayout.NORTH);
          gbc.gridy=1;
          controlPanel.add(botActionBar, gbc);//, BorderLayout.SOUTH);
     
    
    
    }

 
    /**
     * Set the SSAServerList.
     *
     * @param serverList the SSAServerList reference.
     */
    public void setSSAServerList( SSAServerList serverList )
    {
        this.serverList = serverList;
      //  updateTree();
        updateTable();
    }
    
 
    /**
     * update the table
     *
     */
    public void updateTable()
    {
        // check existing nodes
        populateTable();
        
        try {
            restoreTags();
        } catch (SplatException e) {

//            e.printStackTrace();
        }
    }
 

    /**
     * Inner class defining action for adding a new server to the list
     */
    protected class AddNewAction
        extends AbstractAction
    {
        public AddNewAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            addNewServer();
          
            serverTable.updateUI();
        }
    }

    /**
     * Inner class defining action for adding a new tag 
     */
    protected class AddTagAction
        extends AbstractAction
    {
        public AddTagAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            addNewTag();
            try {
                  saveServerTags();
            } catch (SplatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
               
            }          
        }
    }
    
    protected class PopupMenuAction extends AbstractAction
    {
        
        public void actionPerformed( ActionEvent e) {
                                  
            int r = serverTable.getPopupRow();
          //  ServerTableRenderer renderer = (ServerTableRenderer) serverTable.getCellRenderer(r, 0);
          //  renderer.repaint();
            
            if (e.getActionCommand().equals("Info")) {
                serverTable.showInfo(r, "SSAP");
            }
            else if (e.getActionCommand().equals("Tag")) {
                addNewTag(r);
            }         
        }
    }
 
    /**
     * Inner class defining action for removing a new tag 
     */
    protected class RemoveTagAction
        extends AbstractAction
    {
        public RemoveTagAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            removeSelectedTag();
            
        }

    }
    /**
     * Inner class defining action for reading a list of servers.
     */
    protected class ReadAction
        extends AbstractAction
    {
        public ReadAction( String name, Icon icon )
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae )
        {
            readServers();
        }
    }

    /**
     * Inner class defining action for saving a list of servers.
     */
    protected class SaveAction
        extends AbstractAction
    {
        public SaveAction( String name, Icon icon )
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae )
        {
            saveServers();
            try {
                saveServerTags();
            } catch (SplatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Inner class defining action for setting the proxy server.
     */
    protected class ProxyAction
        extends AbstractAction
    {
        public ProxyAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
   //         showProxyDialog();
        }
    }


    /**
     * Inner class defining action for query registry for new SSAP servers.
     */
    protected class QueryNewAction
        extends AbstractAction
    {
        public QueryNewAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            updateServers();
        }
    }

    /**
     * Inner class defining action for removing selected servers.
     *
    protected class RemoveAction
        extends AbstractAction
    {
        public RemoveAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            removeSelectedServers();
        }
    }
    */
    /**
     * Inner class defining action for removing unselected servers.
     *
    protected class RemoveUnAction
        extends AbstractAction
    {
        public RemoveUnAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            removeUnSelectedServers();
        }
    }
    */
    /**
     * Inner class defining action for selecting all known servers.
     */
    protected class SelectAllAction
        extends AbstractAction
    {
        public SelectAllAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            selectAllServers();
        }
    }
    /**
     * Inner class defining action for selecting all known servers.
     */
    protected class DeselectAllAction
        extends AbstractAction
    {
        public DeselectAllAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            deselectAllServers();
        }
    }

    /**
     * Inner class defining action for deleting all known servers.
     */
    protected class DeleteAction
        extends AbstractAction
    {
        public DeleteAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            deleteServers();
        }
    }
    
    /**
     * Inner class defining action for deleting all known servers.
     */
    protected class SelectTagAction
        extends AbstractAction
    {
        public SelectTagAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            JComboBox tagscombo = (JComboBox) ae.getSource();
            String selected = (String) tagscombo.getSelectedItem();
            if (selected.isEmpty()) 
                serverTable.clearSelection();
            else {
     //           tagsList.setSelectedValue(selected, true);
                //TagsListSelectionListener listener = (TagsListSelectionListener) tagsList.getListSelectionListeners()[0];
                selectTaggedServers(selected);
            }
            updateUI();
        }
    }
    
    /**
     *  Remove all servers.
     */
    protected void deleteServers()
    {
        serverTable.removeAll();
      
    }
    
 


    /**
     *  select all servers.
     */
    protected void selectAllServers()
    {    
    
        tagCombo.setSelectedIndex(0);
        serverTable.selectAll();
        serverTable.updateUI();
      
    } 
    
    /**
     *  deselect all servers.
     */
    protected void deselectAllServers()
    {    
        serverTable.clearSelection();
   //     tagsList.clearSelection();
        tagCombo.setSelectedIndex(0);
         serverTable.updateUI();
    } 
    
    /**
     *  Remove selected servers.
     *
    protected void removeSelectedServers()
    {
       
        StarTableModel model = (StarTableModel) serverTable.getModel();
        int [] selected = serverTable.getSelectedRows();
        for ( int i=selected.length-1; i>=0; i-- ) {
            int row = serverTable.convertColumnIndexToModel(selected[i]);
            serverTable.removeRow(row);
            removeFromTags((String) serverTable.getModel().getValueAt(row, ServerPopupTable.SHORTNAME_INDEX));
            
        }
    //.clearSelection();
        tagCombo.setSelectedIndex(0);
        serverTable.updateUI();

    }
    
    */
    /**
     *  remove all entries of shortname from the map;
     */
    private void removeFromTags(String shortname) {
     
        ArrayList<String> tags= serverTagsMap.get(shortname);
        for (int i=0;i<tags.size();i++) {
            ArrayList<String> servers = tagsMap.get(tags.get(i));
            servers.remove(shortname);
        }
    }

    /**
     *  Remove UNselected servers.
     *
    protected void removeUnSelectedServers()
    {
        //  Get selected indices.
        
        StarTableModel model = (StarTableModel) serverTable.getModel();
        int [] selected = serverTable.getSelectedRows();
        int j = 0;
        for ( int i=serverTable.getRowCount()-1; i>=0; i-- ) {
            if (i == selected[j] ) {
                model.removeRowSelectionInterval(selected[i]);
                serverTable.re
                j++;
            }
            
        }
        serverTable.updateUI();
    }
    */
    
    /**
     * Query a registry for any new SSAP servers. New servers must have a
     * different short name.
     */
    public void updateServers()
    {
                
        StarTable table = null;
        try {
                 
            table = TableLoadPanel.loadTable( this, new SSARegistryQueryDialog("SSA"), new StarTableFactory() );
         }
        catch ( IOException e ) {
            ErrorDialog.showError( this, "Registry query failed", e );
            return;
        }
        
        serverTable.updateServers(table); 
        serverList = serverTable.getServerList();
        this.firePropertyChange("changeServerlist", false, true);
            
    } // updateServers

    private String convertToString(String[] rowobj) {
        String str="";
        for (String s:rowobj)
            str += s + ", ";
        if (str.length()>2)
            str=str.substring(0, str.length()-2);
        return str;
    }

// server table formatting and sorting
/*    private void updateServerTable() {
       
      for (int i=serverTable.getColumnCount()-1; i>1; i--)  {
            /// update server table to show only the two first rows
            serverTable.removeColumn(serverTable.getColumn(serverTable.getColumnName(i)));
      }
      serverTable.updateUI();
   
    }
*/
/*
 *     private void updateServerList(DefaultTableModel serverModel) {
        
    
        for ( int i= 0; i<serverModel.getRowCount(); i++) {
            SSAPRegResource res = new SSAPRegResource();
            SSAPRegCapability[] cap = new SSAPRegCapability[1];
          
            res.setIdentifier( (String) serverModel.getValueAt(i, ServerPopupTable.IDENTIFIER_INDEX ));
            res.setContact( (String) serverModel.getValueAt(i, ServerPopupTable.CONTACT_INDEX ) );
            res.setPublisher( (String) serverModel.getValueAt(i, ServerPopupTable.PUBLISHER_INDEX ) ); 
            res.setReferenceUrl((String) serverModel.getValueAt(i, ServerPopupTable.REFURL_INDEX));
            String shortname = ((String) serverModel.getValueAt(i, ServerPopupTable.SHORTNAME_INDEX));
            if (shortname != null)
                shortname = shortname.trim();
            if (shortname == null || shortname.isEmpty()) // use title as shortname if shortname not available
                shortname = (String) serverModel.getValueAt(i, ServerPopupTable.TITLE_INDEX);
            
            res.setShortName(shortname);
           
            res.setTitle((String) serverModel.getValueAt(i, ServerPopupTable.TITLE_INDEX));
            res.setVersion((String) serverModel.getValueAt(i, ServerPopupTable.VERSION_INDEX));
            String waveband = (String)serverModel.getValueAt(i, ServerPopupTable.WAVEBAND_INDEX);
            if ( waveband != null )
               res.setWaveband(waveband.split(","));
            String subjects = (String)serverModel.getValueAt(i, ServerPopupTable.SUBJECTS_INDEX);
            if ( subjects != null )
            res.setSubjects(subjects.split(","));
            res.setContentType((String)serverModel.getValueAt(i, ServerPopupTable.CONTTYPE_INDEX));
            cap[0] = new SSAPRegCapability();
            cap[0].setAccessUrl((String)serverModel.getValueAt(i, ServerPopupTable.ACCESSURL_INDEX));
            cap[0].setDataSource((String)serverModel.getValueAt(i, ServerPopupTable.DATASOURCE_INDEX));
            cap[0].setDescription((String)serverModel.getValueAt(i, ServerPopupTable.DESCRIPTION_INDEX));
            cap[0].setCreationType((String)serverModel.getValueAt(i, ServerPopupTable.CREATIONTYPE_INDEX));
           // cap[0].setContentType((String)serverModel.getValueAt(i, ServerPopupTable.CONTTYPE_INDEX));
           // cap[0].setXsiType(xsiType)
            cap[0].setStandardId((String)serverModel.getValueAt(i, ServerPopupTable.STDID_INDEX));
            res.setCapabilities(cap);
            serverList.addServer(res);
     
        }
        
        
    }
 */   
    private void initFilters() {
        
         theoFilter = new RowFilter<DefaultTableModel, Object>() {
            
            public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                return (entry.getStringValue(ServerPopupTable.CONTTYPE_INDEX).toLowerCase().contains("simulation") || 
                        entry.getStringValue(ServerPopupTable.DATASOURCE_INDEX).toLowerCase().contains("theory") );
            }    
          };
          obsFilter = RowFilter.notFilter(theoFilter);    
          
          band_RadioFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("radio") );
              }  
          };
          band_MmFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("millimeter") ||  entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).equalsIgnoreCase("mm"));
              }  
          };
          band_IRFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("infrared") || entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("ir") ||
                           entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("infra-red"));
              }  
          };
          band_OpticalFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("optical") );
              }  
          };
          band_UVFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("uv") &&  !entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("euv"));
              }  
          };
          band_EUVFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("euv") );
              }  
          };
          band_XRayFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("x-ray") || entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("xray") );
              }  
          };
          band_GammaFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("gamma") );
              }  
          };
    }
    
    private void setFilters() {
      
        
        RowFilter<DefaultTableModel,Object> sourceFilter = src_obs.isSelected()?obsFilter:theoFilter;
       
        
        if ( band_all.isSelected()) {
            sorter.setRowFilter(sourceFilter);
            
        } 
        else {   
            
            List<RowFilter<DefaultTableModel,Object>> bandfilters = new ArrayList<RowFilter<DefaultTableModel,Object>>();   
            List<RowFilter<DefaultTableModel,Object>> filters = new ArrayList<RowFilter<DefaultTableModel,Object>>(); 
            
            String selected = "";
            
            filters.add(sourceFilter);
            
            if ( band_rad.isSelected()) {
                selected+=" radio ";
              //  composedFilter = RowFilter.andFilter(sourceFilter, band_RadioFilter);
                bandfilters.add(band_RadioFilter);
            }
            if ( band_mm.isSelected()) {
   //             composedFilter = RowFilter.andFilter(composedFilter, band_MmFilter);
                bandfilters.add(band_MmFilter);
                selected+=" millimeter ";
            }
            if ( band_ir.isSelected()) {
                bandfilters.add(band_IRFilter);
                selected+=" IR ";
            }
            if ( band_opt.isSelected()) {
                bandfilters.add(band_OpticalFilter);
            selected+=" optical ";
            }
            if ( band_uv.isSelected()) {
                bandfilters.add(band_UVFilter);
            selected+=" UV ";
            }
            if ( band_euv.isSelected()) {
                bandfilters.add(band_EUVFilter);
            selected+=" EUV ";
            }
            if ( band_xr.isSelected()) {
                bandfilters.add(band_XRayFilter);
            selected+=" xray ";
            }
            if ( band_gr.isSelected()) {
                bandfilters.add(band_GammaFilter);
            selected+=" gamma ";
            }
            
            logger.info("Selected: "+selected);
            if (filters.size() >0) {  
              filters.add(RowFilter.orFilter(bandfilters));
              sorter.setRowFilter(RowFilter.andFilter(filters));
            }
            
        } 
        serverTable.setRowSorter(sorter);
            
    }

    /**
     *  Save server list to a disk file
     */
    protected void saveServers()
    {
        initFileChooser();
        int result = fileChooser.showSaveDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            try {
                serverList.saveServers( file );
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }
    }
    
    /**
     *  Restore servers from a previously saved server list.
     */
    protected void readServers()
    {
        initFileChooser();
        int result = fileChooser.showOpenDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            try {
                serverList.restoreServers( file );
            //    updateTree();
                updateTable();
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }
    }
    
    /**
     * Initialise the file chooser to have the necessary filters.
     */
    protected void initFileChooser()
    {
        if ( fileChooser == null ) {
            fileChooser = new BasicFileChooser( false );
            fileChooser.setMultiSelectionEnabled( false );

            //  Add a filter for XML files.
            BasicFileFilter xmlFileFilter =
                new BasicFileFilter( "xml", "XML files" );
            fileChooser.addChoosableFileFilter( xmlFileFilter );

            //  But allow all files as well.
            fileChooser.addChoosableFileFilter
                ( fileChooser.getAcceptAllFileFilter() );
        }
    }
    
    /**
     * Initialise the window to insert a new server to the list.
     */
    protected void initAddServerWindow()
    {
        if ( addServerWindow == null ) {
            addServerWindow = new AddNewServerFrame();
            addServerWindow.addPropertyChangeListener(this);
        }
    }
    
    /**
     *  Add new server to the server list
     */
    protected void addNewServer()
    {
        initAddServerWindow();
        addServerWindow.setVisible( true );
    }
    
    /**
     *  Add new tag to the server list
     */
    protected void addNewTag()
    {
             
        // are there selected servers?
        int nrSelected=serverTable.getSelectedRowCount();
        if (nrSelected  == 0) {
            //dialog "Please select"!!!!
            JOptionPane.showMessageDialog( this,
                    "Please select servers from the list below",
                    "No servers selected", JOptionPane.ERROR_MESSAGE );
            return;
        }
        
        String tagname = getTagnameMenu();
        if ( tagname == null || tagname.isEmpty() )
            return;
      
        int[] selected = serverTable.getSelectedRows();
       
//        ArrayList<String> servers=tagsMap.get(tagname);
//        if (servers == null)
 //           servers = new ArrayList<String>();
        for (int i=0;i<selected.length;i++) {
            int row = serverTable.convertRowIndexToModel(selected[i]);
            String shortname = (String) serverTable.getModel().getValueAt(row, ServerPopupTable.SHORTNAME_INDEX);
            addTagMaps( shortname, tagname );         
           
        }
      
        DefaultComboBoxModel comboModel = (DefaultComboBoxModel) tagCombo.getModel();
        if (comboModel.getIndexOf(tagname) == -1) 
            tagCombo.addItem(tagname);
        tagCombo.setSelectedItem(tagname);
        selectTaggedServers(tagname);
        updateUI();
    }
    

 
    private void addNewTag(int row) {


        String tagname = getTagnameMenu();

        if ( tagname == null )
            return;

        String shortname = (String) serverTable.getModel().getValueAt(row, ServerPopupTable.SHORTNAME_INDEX);

        addTagMaps( shortname, tagname );  

    //    if (! tagsListModel.contains(tagname))
   //         tagsListModel.addElement(tagname);  
     //   tagsList.setSelectedValue(tagname, true);
      //  ((TagsListSelectionListener) tagsList.getListSelectionListeners()[0]).selectTaggedServers((String) tagsList.getSelectedValue());
      

        DefaultComboBoxModel comboModel = (DefaultComboBoxModel) tagCombo.getModel();
        if (comboModel.getIndexOf(tagname) == -1) {
            tagCombo.addItem(tagname);
            tagCombo.setSelectedItem(tagname);
        }
        selectTaggedServers(tagname);
        updateUI();
    }

  
   private void addTagMaps(String shortname, String tagname) {
        
        ArrayList<String> servers;
        if (tagsMap.containsKey(tagname))  // add tag to reverse tags map
            servers =tagsMap.get(tagname);
        else
            servers = new ArrayList<String>();

        if (! servers.contains(shortname)) {
            servers.add(shortname);
        }
        tagsMap.put(tagname, servers);
        
        ArrayList<String> smap;
        if (serverTagsMap.containsKey(shortname))  // add tag to reverse tags map
            smap =serverTagsMap.get(shortname);
        else
            smap = new ArrayList<String>(); // create new entry in reverse tags map
        
        smap.add(tagname);
        serverTagsMap.put(shortname, smap);                        
    }
 
    
    private String getTagnameMenu() {
        
       // boolean newTag = true;
        String tagname = (String)JOptionPane.showInputDialog(this, "Enter Tagname:\n", "Add tag", JOptionPane.QUESTION_MESSAGE );   
        if (tagname == null || tagname.length() == 0) {
            return null;
        }
        if ( ((DefaultComboBoxModel) tagCombo.getModel()).getIndexOf(tagname) != -1) { 
          //Custom button text
            Object[] options = {"Overwrite",
                                "Add to existing tag",
                                "Cancel"};
            int n = JOptionPane.showOptionDialog(this,"The tag "+ tagname+ " exists already.\n",
                " ",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]);
            
            if (n == JOptionPane.YES_OPTION) {
                 // remove the old tag, create it again       
                removeTag(tagname);
            }
            else if ( n == JOptionPane.CANCEL_OPTION ) 
                return null;
        //    else if (n == JOptionPane.NO_OPTION)
        //        newTag = false; // the tags will be added to the existing tag
        }
        return tagname;

    }
    
    
    private void selectTaggedServers(String tagname) {
        serverTable.clearSelection();
        boolean firstSelection=true;
        ArrayList<String> servers = tagsMap.get(tagname);
        
        if (tagname.isEmpty())
            return;
        
        for (int i=0;i<serverTable.getRowCount();i++) {
            String shortname = (String) serverTable.getValueAt(i, ServerPopupTable.SHORTNAME_INDEX); //getModel().getValueAt( i, SHORTNAME_INDEX); 
            if (servers.contains(shortname)) {
                //int row = serverTable.convertRowIndexToView(i);
                int row = i;
                //if (row > -1) {
                if (firstSelection){
                    logger.info("index: "+i+"row: "+row);                         
                    serverTable.setRowSelectionInterval(row, row);
                }
                else
                    serverTable.addRowSelectionInterval(row, row);
                firstSelection=false;
                // }
            }                   
        }
    }
    /**
     *  Remove  tag
     */
    private void removeTag(String tagname)
    {
    //    tagsListModel.removeElement(tagname);
        ArrayList<String> st = tagsMap.get(tagname);
        tagsMap.remove(tagname);
        
        for (int i=0;i<st.size();i++) { //remove from reverse tags map
            ArrayList<String> ts = serverTagsMap.get(st.get(i));
            if (ts.contains(tagname) )
                    ts.remove(tagname);
            serverTagsMap.put(st.get(i), ts);
        }
        
    }

  
  
    
    /**
     *  Remove selected tag
     */
    private void removeSelectedTag()
    {
        String tag = (String) tagCombo.getSelectedItem();
        
        if (tag == null || tag.isEmpty() ) {// no tag selected
            JOptionPane.showMessageDialog( this,   "No tags have been selected for removal" ,                
                    "No tags", JOptionPane.ERROR_MESSAGE );
            return;
        }

        tagCombo.removeItem(tag);
        tagCombo.setSelectedIndex(0);
        removeTag(tag);   
     
    }
   
    /**
     *  Save tag information to a file
     */
    public void saveServerTags() throws SplatException { 
        
        File tagfile = Utilities.getConfigFile(tagsFile);
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream( tagfile );
        }
        catch (FileNotFoundException e) {
            throw new SplatException( e );
        }
        XMLEncoder encoder = new XMLEncoder( outputStream );
        Set<String> tags = tagsMap.keySet();
        encoder.writeObject(tagsMap);
    /*    Iterator it = tags.iterator();
        while (it.hasNext()) {
            String tag = it.next();
            ServerTags st = new ServerTags(shortname, tags);
            try {
                encoder.writeObject(st);
            } 
            catch (Exception e) {
                    e.printStackTrace();
            }  
            
        }  */      
        encoder.close();  
     
    }
    
    /**
     * Initialise the known servers which are kept in a resource file along
     * with SPLAT. The format of this file is determined by the
     * {@link XMLEncode} form produced for an {@link SSAPRegResource}.
     */
    protected void restoreTags()
        throws SplatException
    {
        //  Locate the description file. This may exist in the user's
        //  application specific directory or, the first time, as part of the
        //  application resources.
        //  User file first.
        File backingStore = Utilities.getConfigFile( tagsFile );
        InputStream inputStream = null;
        if ( backingStore.canRead() ) {
            try {
                inputStream = new FileInputStream( backingStore );
            }
            catch (FileNotFoundException e) {
               // e.printStackTrace();
            }
        }
     //   boolean restored = false;
        if ( inputStream != null ) {
            restoreTags( inputStream );
            try {
                inputStream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        } 
    }  
        /**
         * Read an InputStream that contains a list of servers to restore.
         */
        protected void restoreTags( InputStream inputStream )
            throws SplatException
        {
            XMLDecoder decoder = new XMLDecoder( inputStream );
            boolean ok = true;
            //ServerTags st = null;           
        
            while ( true ) {
                try {
                    Object ob =  decoder.readObject();
                    //st = (ServerTags)  ob;
                    tagsMap = (Map<String, ArrayList<String>>) ob;
                }
                catch( ArrayIndexOutOfBoundsException e ) {
                    break; // End of list.
                }
                catch ( NoSuchElementException e ) {
                    System.out.println( "Failed to read server list " +
                                        " (old-style or invalid):  '" +
                                        e.getMessage() + "'"  );
                    ok = false;
                    break;
                }
                serverTagsMap.clear();
                
                Set <String> tags = tagsMap.keySet();
              
               // tagCombo.addItem("");
                Iterator <String> it = tags.iterator();
                while (it.hasNext()) {
                    String tag = it.next();
                     tagCombo.addItem(tag);
                    ArrayList<String> servers = tagsMap.get(tag);
                    for (int i=0;i<servers.size();i++) {
                        String server = servers.get(i);
                        ArrayList<String> tagnames;
                        if (serverTagsMap.containsKey(server))
                            tagnames = serverTagsMap.get(server);
                        else 
                            tagnames = new ArrayList<String>();
                        tagnames.add(tag);
                        serverTagsMap.put(server, tagnames);
                    }
                    
                }
                    
 /*               for (int i=0;i<serverTable.getRowCount();i++) {
                    String shortname =   (String) serverTable.getModel().getValueAt( i, SHORTNAME_INDEX);
                    if (shortname.equals(st.getName())) {
                         ArrayList tags = st.getTags();
                         serverTable.getModel().setValueAt( tags, lue(ServerPopupTable.TAGS_INDEX);
                         for (int j=0;j<=tags.size();j++) {
                             ArrayList<String> servers = tagsMap.get(tags.get(j));
                             servers.add(shortname);
                             tagsMap.put((String) tags.get(j), servers);
                         }
                    }
                }  */
            }  
          
            decoder.close();
     
        }
    
      
    /**
     * Event listener to trigger a list update when a new server is
     * added to addServerWIndow
     */
    public void propertyChange(PropertyChangeEvent pvt)
    {
        SSAPRegResource res = addServerWindow.getResource();
        serverList.addServer(res);
        serverTable.setServerList(serverList);
        populateTable();
        
        // add a special tag for manually added servers
        addTagMaps( res.getShortName(), MANUALLY_ADDED_STR  );
        DefaultComboBoxModel comboModel = (DefaultComboBoxModel) tagCombo.getModel();
        if (comboModel.getIndexOf(MANUALLY_ADDED_STR) == -1) 
            tagCombo.addItem(MANUALLY_ADDED_STR);
        // and announce the change of serverList 
        this.firePropertyChange("changeServerlist", false, true);      
    }
    
    // returns the serverlist
    public SSAServerList getServerList() {
        return serverList;
    }
    
    //sets a modified serverList
    public void setServerList(SSAServerList slist) {

        this.serverList = slist;
    }
    
    /**
     * returns true if server is selected, false otherwise.
     */
    public boolean isServerSelected(String shortName) {
        int [] selected = serverTable.getSelectedRows();
        DefaultTableModel serverModel = (DefaultTableModel) serverTable.getModel();
        for (int i=0;i<selected.length;i++) {
            int row = serverTable.convertRowIndexToModel(selected[i]);
            if ((boolean) serverModel.getValueAt(row, ServerPopupTable.SHORTNAME_INDEX).equals(shortName)) {
                return true;
            }
        }
        return false;
    }
    /**
     * returns the number of selected rows
     */
    public int getSelectionCount() {
        return serverTable.getSelectedRowCount();
        
    }
 
 
    /**
     * Set the proxy server and port.
     */
    protected void showProxyDialog()
    {
        if ( proxyWindow == null ) {
            ProxySetupFrame.restore( null );
            proxyWindow = new ProxySetupFrame();
        }
        proxyWindow.setVisible(true);
    }
    
    
 /*   public class ServerTableModel extends DefaultTableModel
    {
      
        public Class getColumnClass(int column) {
            
            return String.class;
         
        }
    }
  
*/
  
    //Listens to the check boxes events
    class CheckBoxListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {

            JComponent source = (JComponent) e.getItemSelectable();
            String name = source.getName();


            if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED ) {  
          //      setFilters();
          //      serverTable.updateUI();

                if (name.equals("band_all")) {
                    if ( e.getStateChange() == ItemEvent.SELECTED) {                    
                        band_rad.setSelected(false);
                        band_mm.setSelected(false);
                        band_opt.setSelected(false);
                        band_ir.setSelected(false);
                        band_uv.setSelected(false);
                        band_euv.setSelected(false);
                        band_xr.setSelected(false);
                        band_gr.setSelected(false);
                        band_all.setSelected(true);
                    }  
                  
                } else if (name.startsWith("band") ) {// and not band_all 
                   
                    if ( e.getStateChange() == ItemEvent.DESELECTED) {  
                        // if all bands are deselected, then select "all"
                        if (  !band_rad.isSelected() && !band_mm.isSelected() && 
                              !band_opt.isSelected() && !band_ir.isSelected() && 
                              !band_uv.isSelected() && !band_euv.isSelected() && 
                              !band_xr.isSelected() && !band_gr.isSelected() )
                            band_all.setSelected(true);
                    } else {
                        band_all.setSelected(false);
                    }
                }
                
            } // if selected/deselected
            
            setFilters();
            serverTable.updateUI();
            // if view is changed, tags have to be re-selected.
        //    TagsListSelectionListener listener = (TagsListSelectionListener) tagsList.getListSelectionListeners()[0];
       //     listener.selectTaggedServers((String) tagsList.getSelectedValue());
            selectTaggedServers((String) tagCombo.getSelectedItem());

        } // itemStateChanged()  
    } // class CheckBoxListener

    class SelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent lse) {
            
            if ( !lse.getValueIsAdjusting() )
                if (lse.getSource() == serverTable.getSelectionModel()) {
               
                    firePropertyChange("selectionChanged", false, true );
                    // if (! lse.getValueIsAdjusting()) {
                    //      tagsList.clearSelection();
                    // }
                }
        }
        
    }
    
    
    
    private class ServerTableMouseListener extends MouseAdapter {
      
        
    
        public void mousePressed( MouseEvent e ) {
            if ( e.getSource() == (Object) serverTable ) {
                if ( SwingUtilities.isRightMouseButton( e ) )
                {
                    // get the row index that contains that coordinate
                   // int row = serverTable.rowAtPoint( e.getPoint() ); 
                    serverTable.repaint();
                }
                else {
                    tagCombo.setSelectedIndex(0);
                }
            }
        }
       
    }
    
    private class resizeListener extends ComponentAdapter {
        public void componentResized(ComponentEvent e) {
            updateUI();
        }
    }
/*    
    protected class ServerPopupTable extends JTable {
        
        
        public Point getPopupLocation(MouseEvent event) {
            setPopupTriggerLocation(event);
          
            return super.getPopupLocation(event);
        }
        protected void setPopupTriggerLocation(MouseEvent event) {
            putClientProperty("popupTriggerLocation", 
                    event != null ? event.getPoint() : null);
        }
    
        public int getPopupRow () {
            int row = rowAtPoint( (Point) getClientProperty("popupTriggerLocation") );
            return convertRowIndexToModel(row);
        }
        
        public boolean isCellEditable(int row, int col) {
            return false; //Disable cell editing
        }
        
    }
*/    
  static class ServerTableRenderer extends DefaultTableCellRenderer {
             
       Component c;
        public ServerTableRenderer() { 
            super();
        }
        public Component getTableCellRendererComponent(ServerPopupTable table, Object value, boolean   isSelected, boolean hasFocus, int row, int column) 
        { 
            c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); 
           
            int popuprow = table.getPopupRow();
            if (row == table.getPopupRow() ) {
                c.setBackground(Color.lightGray);
            } else if (isSelected) {
                c.setBackground(table.getSelectionBackground());
            } else {
                c.setBackground(table.getBackground());
            }
          
            return c; 
        } 
        
    }
   
 
    
}
