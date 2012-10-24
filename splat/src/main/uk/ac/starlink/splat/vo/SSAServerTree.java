package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import uk.ac.starlink.splat.iface.HelpFrame;
import uk.ac.starlink.splat.iface.ProgressPanel;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
/*import uk.ac.starlink.splat.vo.SSAServerFrame.AddNewAction;
import uk.ac.starlink.splat.vo.SSAServerFrame.CloseAction;
import uk.ac.starlink.splat.vo.SSAServerFrame.DeleteAction;
import uk.ac.starlink.splat.vo.SSAServerFrame.ProxyAction;
import uk.ac.starlink.splat.vo.SSAServerFrame.QueryNewAction;
import uk.ac.starlink.splat.vo.SSAServerFrame.ReadAction;
import uk.ac.starlink.splat.vo.SSAServerFrame.RemoveAction;
import uk.ac.starlink.splat.vo.SSAServerFrame.RemoveUnAction;
import uk.ac.starlink.splat.vo.SSAServerFrame.SaveAction;
import uk.ac.starlink.splat.vo.SSAServerFrame.SelectAllAction;
*/
import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.TableLoadPanel;
import uk.ac.starlink.util.ProxySetup;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.ProxySetupFrame;
import uk.ac.starlink.vo.RegCapabilityInterface;
import uk.ac.starlink.vo.RegResource;
import uk.ac.starlink.vo.RegistryTable;
import uk.ac.starlink.vo.RegistryTableLoadDialog;
import uk.ac.starlink.vo.ResourceTableModel;
import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import javax.swing.SwingConstants;

public class SSAServerTree extends JPanel  implements PropertyChangeListener {
    
    
    // Logger.
    private static Logger logger =
            Logger.getLogger( "uk.ac.starlink.splat.vo.SSAServerTree" );

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

    /**
     * Two way mapping servers-parameters
     */
    protected ServerParamRelation serverParam;
    
    
    /**
     * The object that manages the actual list of servers.
     */
    private JTree serverTree;
    private JPanel treePanel;
    private JPanel mainPanel;
    private JPanel buttonsPanel;
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
    
    // user defined tags
    private ArrayList<JCheckBox> userTags;
    private JTabbedPane optionTabs;
    private JPanel tagsPanel;
    private DefaultListModel tagsListModel;
    private JList tagsList;
    
    // sizes
    
    private  int WIDTH = 300;
    private  int HEIGTH = 650;
    private int  TAB_HEIGTH = 200;
   
   
    CheckBoxListener checkBoxlistener = null;
  
    
    /** The cell tree renderer */
    ServerTreeCellRenderer treeRenderer ;
    
    /** The proxy server dialog */
    protected ProxySetupFrame proxyWindow = null;

    /** Make sure the proxy environment is setup */
    static {
        ProxySetup.getInstance().restore();
    }

    /**
     * Create an instance.
     */
    public SSAServerTree( SSAServerList serverList, ServerParamRelation spr )
    {
      
        this.serverList = serverList;
        this.serverParam = spr;
        initUI();
      
        //setSSAServerList( serverList );
    }
    
 

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {
    
       this.setPreferredSize(new Dimension(this.WIDTH,this.HEIGHT));
       setLayout( new BorderLayout() );
       optionTabs = new JTabbedPane();
      
       JPanel optionsPanel = new JPanel();      
       optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
    //   optionsPanel.setBorder ( BorderFactory.createTitledBorder( "Server Options" ) );
       optionsPanel.setPreferredSize(new Dimension(this.WIDTH,this.TAB_HEIGTH));
       
       treeRenderer = new ServerTreeCellRenderer();
       checkBoxlistener = new CheckBoxListener();
       
       // BAND
       JPanel bandPanel = new JPanel (new GridLayout(3,3));
       bandPanel.setBorder ( BorderFactory.createTitledBorder( "Wave Band" ) );
       band_rad = new JCheckBox( "Radio", true);
       bandPanel.add(band_rad);
       band_mm = new JCheckBox( "Milimeter",  true);
       bandPanel.add(band_mm);
       band_ir = new JCheckBox( "IR",  true);
       bandPanel.add(band_ir);
       band_opt = new JCheckBox( "Optical", true);
       bandPanel.add(band_opt);
       band_uv = new JCheckBox( "UV",  true);
       bandPanel.add(band_uv);
       band_euv = new JCheckBox( "EUV",  true);
       bandPanel.add(band_euv);
       band_xr = new JCheckBox( "X Ray",  true);
       bandPanel.add(band_xr);
       band_gr = new JCheckBox( "Gamma Ray", true);
       bandPanel.add(band_gr);      
       band_all = new JCheckBox( "ALL", true);
       bandPanel.add(band_all);
       
       band_rad.addItemListener(checkBoxlistener);
       band_mm.addItemListener(checkBoxlistener);
       band_ir.addItemListener(checkBoxlistener);
       band_opt.addItemListener(checkBoxlistener);
       band_uv.addItemListener(checkBoxlistener);
       band_euv.addItemListener(checkBoxlistener);       
       band_xr.addItemListener(checkBoxlistener);
       band_gr.addItemListener(checkBoxlistener);
       band_all.addItemListener(checkBoxlistener);
       treeRenderer.addBand(band_rad.getText());
       treeRenderer.addBand(band_mm.getText());
       treeRenderer.addBand(band_ir.getText());
       treeRenderer.addBand(band_opt.getText());
       treeRenderer.addBand(band_uv.getText());
       treeRenderer.addBand(band_euv.getText());
       treeRenderer.addBand(band_xr.getText());
       treeRenderer.addBand(band_gr.getText());
       treeRenderer.setAllBands(true);

       band_rad.setName("band");
       band_mm.setName("band");
       band_opt.setName("band");
       band_ir.setName("band");
       band_uv.setName("band");
       band_euv.setName("band");
       band_xr.setName("band");
       band_gr.setName("band");
       band_all.setName("band");
      
       
       // Data Source
       JPanel srcPanel = new JPanel (new GridLayout(2, 3));
       srcPanel.setBorder ( BorderFactory.createTitledBorder( "Source" ) );
       
       JCheckBox src_obs = new JCheckBox("Survey", true);
       src_obs.setToolTipText("<html>A survey dataset, which typically covers some region of observational <br>" +
       		                   "parameter space in a uniform fashion, with as complete as possible <br>" +
       		                   "coverage in the region of parameter space observed.</html>");
       srcPanel.add(src_obs);       
       src_obs.setName("src_obs");
       src_obs.addItemListener(checkBoxlistener);
       treeRenderer.addSrc(src_obs.getText());
       
       JCheckBox src_theo = new JCheckBox("Theory", true);
       src_theo.setToolTipText("<html>Theory data, or any data generated from a theoretical model, <br>for example a synthetic spectrum.</html>");
       srcPanel.add(src_theo);
       src_theo.setName("src_theo");
       src_theo.addItemListener(checkBoxlistener);
       treeRenderer.addSrc(src_theo.getText());
       
       JCheckBox src_point = new JCheckBox("Pointed", true);
       src_point.setToolTipText("<html>A pointed observation of a particular astronomical object or field. <br> " +
       		                    " Typically these are instrumental observations taken as part of some PI observing program.<br> " +
       		                    " The data quality and characteristics may be variable, but the observations of a particular <br>" +
       		                    " object or field may be more extensive than for a survey.</html>");
       srcPanel.add(src_point);
       src_point.setName("src_point");
       src_point.addItemListener(checkBoxlistener);
       treeRenderer.addSrc(src_point.getText());
       
       JCheckBox src_cust = new JCheckBox("Custom", true);
       src_cust.setToolTipText("Data which has been custom processed, e.g., as part of a specific research project.");
       srcPanel.add(src_cust);
       src_cust.setName("src_cust");
       src_cust.addItemListener(checkBoxlistener);
       treeRenderer.addSrc(src_cust.getText());
       
       JCheckBox src_art = new JCheckBox("Artificial", true);
       src_art.setToolTipText("<html>Artificial or simulated data.  This is similar to theory data but need not be based <br>" +
       		                    "on a physical model, and is often used for testing purposes.</html>");
       srcPanel.add(src_art);
       src_art.setName("src_art");
       src_art.addItemListener(checkBoxlistener);
       treeRenderer.addSrc(src_art.getText());
       
       JCheckBox src_all = new JCheckBox("ALL", false);
       src_all.setToolTipText("All servers (including the ones with no data source set)");
       srcPanel.add(src_all);
       src_all.setName("src_all");
       src_all.addItemListener(checkBoxlistener);
       treeRenderer.addSrc(src_all.getText());
       
       optionsPanel.add(srcPanel);       
       optionsPanel.add(bandPanel);
     
       optionTabs.addTab("Options", optionsPanel);
     //  src_all = new JCheckBox( "ALL", true);
     //  srcPanel.add(band_all);
       
       // User defined tags
       
       tagsPanel = new JPanel ();
      // tagsPanel.setBorder ( BorderFactory.createTitledBorder( "Tags" ) );
       tagsPanel.setLayout (new BoxLayout(tagsPanel, BoxLayout.PAGE_AXIS));
     //  tagsPanel.setPreferredSize(optionTabs.getPreferredSize());
       JLabel txt = new JLabel("Add tags to selected servers");
       tagsPanel.add(txt);
       JPanel tagsButtonsPanel=new JPanel();
       AddTagAction addTagAction = new AddTagAction( "Add tag" );
       JButton addTagButton = new JButton(addTagAction);
       addTagButton.setToolTipText("Please select servers to be tagged");
       tagsButtonsPanel.add(addTagButton);
       RemoveTagAction removeTagAction = new RemoveTagAction( "Remove tag" );
       JButton removeTagButton = new JButton(removeTagAction);
       addTagButton.setToolTipText("Please select servers to be tagged");
       tagsButtonsPanel.add(removeTagButton);
       tagsPanel.add(tagsButtonsPanel);
       optionTabs.addTab("Tags", tagsPanel);
       
       tagsListModel = new DefaultListModel();
       tagsList = new JList(tagsListModel);
       ListSelectionModel selectionModel = tagsList.getSelectionModel();
       selectionModel.addListSelectionListener(new TagsListSelectionListener());
       //tagsList.setLayout(new BoxLayout(tagsList, BoxLayout.PAGE_AXIS));
       
       //tagsList.setPreferredSize(tagsPanel.getPreferredSize());
      // tagsList.setBackground(Color.WHITE);
       JScrollPane tagScroller = new JScrollPane( tagsList );
       tagScroller.setPreferredSize(new Dimension (this.WIDTH-10, this.TAB_HEIGTH-40));
    //   tagScroller.set
       tagsPanel.add(tagScroller);
    //   tagScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
   //    tagScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
       
       // 
       
        add(optionTabs, BorderLayout.NORTH);
        
        optionTabs.addChangeListener(new ChangeListener() {
            // This method is called whenever the selected tab changes
            public void stateChanged(ChangeEvent evt) {
                JTabbedPane pane = (JTabbedPane)evt.getSource();

                // Get current tab
                int sel = pane.getSelectedIndex();
                if (sel == 0)
                    treeRenderer.setTagsSelection(false);
                else 
                    treeRenderer.setTagsSelection(true);
                serverTree.updateUI();
            }
        });
       
        mainPanel=new JPanel();
        mainPanel.setLayout(new BorderLayout() );
        mainPanel.setBorder ( BorderFactory.createTitledBorder( "SSAP Servers" ) );
        buttonsPanel=new JPanel();
         
        ServerTreeNode root = new ServerTreeNode("SSAP Servers");
        populateTree(root);

        serverTree = new JTree(root);
        serverTree.expandRow(0);
        serverTree.updateUI();
       // serverTree.setVisibleRowCount(30);
     
        serverTree.setCellRenderer(treeRenderer);
  
       // treePanel.add (new JScrollPane(serverTree));
        mainPanel.add(new JScrollPane(serverTree), BorderLayout.NORTH);
     
         
       // }
        //  RegistryTable of servers goes into a scrollpane in the center of
        //  window (along with a set of buttons, see initUI).
  //      registryTable = new RegistryTable( new ResourceTableModel( true ) );
  //      JScrollPane scroller = new JScrollPane(treePanel );

   //     centrePanel.setLayout( new BorderLayout() );
     //   centrePanel.add( scroller, BorderLayout.CENTER );
    //    getContentPane().add( centrePanel, BorderLayout.CENTER );
   //     centrePanel.setBorder( BorderFactory.createTitledBorder( "Servers" ) );
        initMenus();
        add(mainPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
       
    }

    public void adaptTreeHeight(int heigth) {
        int maxHeight = heigth - buttonsPanel.getHeight() - controlPanel.getHeight();
        if (maxHeight > 0)
            serverTree.setVisibleRowCount((int) (serverTree.getRowHeight()/maxHeight));
    }
    
    /**
     * Add SSAServerList elements to the tree.
     *
     * @param serverList the SSAServerList reference.
     */
    private void populateTree( ServerTreeNode root )
    {      
       
        Iterator  i =  serverList.getIterator();
        while( i.hasNext()) {
            SSAPRegResource server= (SSAPRegResource) i.next(); 
       
            ServerTreeNode stn = new ServerTreeNode( server.getShortName()  ); 
            addInfoNodes(server, stn);
            root.addsort( stn );
            
        }

    }
    
    /**
     * Add SSAServerList elements to the tree.
     *
     * @param serverList the SSAServerList reference.
     */
    private void addInfoNodes ( SSAPRegResource server, ServerTreeNode servernode )
    {      
    
      //  private RegCapabilityInterface[] capabilities;
     //   private String[] subjects = null;

            servernode.add( new ServerTreeNode( "Title: " + server.getTitle() ));
            servernode.add( new ServerTreeNode( "Identifier: " + server.getIdentifier() ));
            servernode.add( new ServerTreeNode( "Publisher: " + server.getPublisher() ));
            servernode.add( new ServerTreeNode( "Contact: " + server.getContact()));
            servernode.add( new ServerTreeNode( "Ref. URL: " + server.getReferenceUrl()));
          
         
            servernode.add( new ServerTreeNode( "Waveband: " + Arrays.toString(server.getWaveband())));
            
      //      servernode.add( new ServerTreeNode( "Version: " + server.getVersion()));
            SSAPRegCapability cap = server.getCapabilities()[0];
            
            ServerTreeNode capnode = new ServerTreeNode( "Capabilities" );
            capnode.add( new ServerTreeNode( "Access URL: " + cap.getAccessUrl() ));
            capnode.add( new ServerTreeNode( "Description: " + cap.getDescription() )); 
            capnode.add( new ServerTreeNode( "Data Source: " + cap.getDataSource() )); 
            capnode.add( new ServerTreeNode( "Data Type: " + cap.getDataType() ));
            capnode.add( new ServerTreeNode( "Creation Type: " + cap.getCreationType() ));
          
            capnode.add( new ServerTreeNode( "Standard ID: " + cap.getStandardId() )); 
            capnode.add( new ServerTreeNode( "Version: " + cap.getVersion() )); 
            capnode.add( new ServerTreeNode( "XSI Type: " + cap.getXsiType() ));   
           
          
           servernode.setUserObject((String) servernode.getUserObject() + "       [" + server.getTitle() + "]");
         //   servernode.setUserObject((String) servernode.getUserObject() + "       [" + cap.getDataSource() + "]" +"["+ cap.getDataType() + "]") ;
            
       
          
           
            ArrayList<String> params = serverParam.getParams(server.getShortName());
            if ( params != null ) {
                ServerTreeNode paramnode = new ServerTreeNode( "Parameters" );
                for (int i=0;i<params.size();i++)
                    paramnode.add(new ServerTreeNode(params.get(i)));
                capnode.add(paramnode);
            }
            
            servernode.add(capnode);
       
    }
    
    /**
     * Initialise the menu bar, action bar and related actions.
     */
    protected void initMenus()
    {
 
        //  Action bars use BoxLayouts.
        
      // setBorder( BorderFactory.createTitledBorder( "Servers" ) );
       controlPanel = new JPanel( new BorderLayout() );
       JPanel topActionBar1 = new JPanel();
       JPanel topActionBar2 = new JPanel();
       JPanel topActionBar3 = new JPanel();
      // topActionBar1.setLayout(new BoxLayout(topActionBar1, BoxLayout.Y_AXIS));
      //  topActionBar.setBorder(BorderFactory.createEmptyBorder( 3, 3, 3, 3 ));

  // botActionBar = new JPanel();
      //  botActionBar.setLayout(new BoxLayout(botActionBar, BoxLayout.X_AXIS));
     //   botActionBar.setBorder(BorderFactory.createEmptyBorder( 3, 3, 3, 3 ));

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

        //  Add an action to close the window.
   //     CloseAction closeAction = new CloseAction( "Close", closeImage );
//        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

    //    JButton closeButton = new JButton( closeAction );
   //     botActionBar.add( Box.createGlue() );
    //    botActionBar.add( closeButton );
    //    closeButton.setToolTipText( "Close window" );


        //  Configure the proxy server.
        ProxyAction proxyAction =
            new ProxyAction( "Configure connection proxy..." );
  //      optionsMenu.add( proxyAction );

        //  Action to check a registry for additional/updated servers.
        QueryNewAction newAction = new QueryNewAction( "Query registry" );
   //     optionsMenu.add( newAction );
        JButton newQueryButton = new JButton( newAction );
  //      topActionBar.add( Box.createGlue() );
        topActionBar2.add( newQueryButton );
        newQueryButton.setToolTipText( "Query registry for new SSAP services" );
        
        //  Add action to manually add a new server to the list
   //     AddNewAction addNewAction = new AddNewAction( "New Server" );
 //       fileMenu.add( addNewAction );
     //   JButton addButton = new JButton( addNewAction );
  //      topActionBar.add( Box.createGlue() );
   //     topActionBar2.add( addButton );
   //     addButton.setToolTipText( "Add a new server to the list" );

        //  Remove selected servers from table.
        RemoveAction removeAction = new RemoveAction( "Remove selected" );
 //       optionsMenu.add( removeAction );
        JButton removeButton = new JButton( removeAction );
  //      topActionBar.add( Box.createGlue() );
        topActionBar1.add( removeButton );
        removeButton.setToolTipText
            ( "Remove selected servers from current list" );

        //  Remove all but the selected servers from table.
        RemoveUnAction removeUnAction = 
          new RemoveUnAction( "Remove unselected" );
 //       optionsMenu.add( removeUnAction );
        JButton removeUnButton = new JButton( removeUnAction );
 //       topActionBar.add( Box.createGlue() );
        topActionBar3.add( removeUnButton );
        removeUnButton.setToolTipText
            ( "Remove unselected servers from current list" );

        //  Add action to select all servers.
        SelectAllAction selectAllAction = new SelectAllAction( "Select all" );
  //      optionsMenu.add( selectAllAction );
        JButton selectAllButton = new JButton( selectAllAction );
  //      topActionBar.add( Box.createGlue() );
   //     topActionBar1.add( selectAllButton );
        selectAllButton.setToolTipText( "Select all servers" );

        //  Add action to just delete all servers.
        DeleteAction deleteAction = new DeleteAction( "Delete all" );
        
        AddNewAction addNewAction = new AddNewAction( "+" );
        JPanel addPanel = new JPanel();
      //  addPanel.setLayout(new BorderLayout());
        JButton addButton1 = new JButton(addNewAction);
        addButton1.setToolTipText("Add new service to the list");
        addPanel.add(addButton1);
        mainPanel.add(addPanel, BorderLayout.EAST);
        
 //       optionsMenu.add( deleteAction );
        JButton deleteButton = new JButton( deleteAction );
   //     topActionBar.add( Box.createGlue() );
 //       topActionBar1.add( deleteButton );
        deleteButton.setToolTipText( "Delete all servers from current list" );

        //  Finish action bars.
      //  topActionBar.add( Box.createGlue() );
   //     botActionBar.add( Box.createGlue() );

        controlPanel.add(topActionBar1, BorderLayout.NORTH);
        controlPanel.add(topActionBar3, BorderLayout.CENTER);
        controlPanel.add(topActionBar2, BorderLayout.SOUTH);
        mainPanel.add( controlPanel, BorderLayout.SOUTH );
     //   buttonsPanel.add( botActionBar, BorderLayout.SOUTH );

        //  Add a Column menu that allows the choice of which registry
        //  query columns to show.
       // JMenu columnsMenu = makeColumnVisibilityMenu( "Columns" );
     //   columnsMenu.setMnemonic( KeyEvent.VK_L );
     //   menuBar.add( columnsMenu );

        //  Create the Help menu.
    //    HelpFrame.createHelpMenu( "ssap-server-window", "Help on window",
    //                              menuBar, null );
    }

 
    /**
     * Set the SSAServerList.
     *
     * @param serverList the SSAServerList reference.
     */
    public void setSSAServerList( SSAServerList serverList )
    {
        this.serverList = serverList;
        updateTree();
    }
    
    /**
     * Set the Parameter Mapping 
     *
     * @param paramList the SSAServerList reference.
     */
    public void setParamMap(   ServerParamRelation spr )
    {
     
        this.serverParam = spr;
        updateTree();
    }
    /**
     * Update the RegistryTable to match the current list of servers.
     */
    public void updateTree()
    {
        // check existing nodes
        DefaultTreeModel model = (DefaultTreeModel) serverTree.getModel();
        ServerTreeNode root = (ServerTreeNode) model.getRoot(); 
        root.removeAllChildren();
        populateTree(root);
        model.reload();
        serverTree.updateUI();
      // registryTable.setData( serverList.getData() );
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
            serverTree.updateUI();
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
            removeTag();
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
     */
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

    /**
     * Inner class defining action for removing unselected servers.
     */
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
     *  Remove all servers.
     */
    protected void deleteServers()
    {
        DefaultTreeModel model = (DefaultTreeModel) serverTree.getModel();
        ServerTreeNode root = (ServerTreeNode) model.getRoot(); 
        root.removeAllChildren();
        model.reload();
    }
    
    /**
     *  select all servers.
     */
    protected void selectAllServers()
    {    
        serverTree.setSelectionInterval(0, serverTree.getRowCount());
        serverTree.updateUI();
    } 
    
    /**
     *  Remove selected servers.
     */
    protected void removeSelectedServers()
    {
        //  Get selected indices.
        int[] selected =  serverTree.getSelectionRows();
        DefaultTreeModel model = (DefaultTreeModel) serverTree.getModel();
        ServerTreeNode root = (ServerTreeNode) model.getRoot();
        
        if ( selected != null && selected.length > 0 ) {

            //  And remove these from the server list.
            ServerTreeNode [] node = new ServerTreeNode[selected.length];
            // first create an array of nodes (instead of indexes)
            for ( int i = 0; i < selected.length; i++ ) {        
                node[i]= (ServerTreeNode) model.getChild(root, selected[i]-1);
            }
            // have to run the loop again with the nodes, because when removing a node the indexes get wrong.
            for ( int i = 0; i < selected.length; i++ ) {
                try {
                String name = node[i].getUserObject().toString();
                name = name.substring(0, name.indexOf("[")).trim();
                serverList.removeServer( name); // name cannot be null!!!!!!!!!
                } catch ( Exception e) {}
                model.removeNodeFromParent(node[i]);              
            }
            model.reload();
        }
    }
    /**
     *  Remove UNselected servers.
     */
    protected void removeUnSelectedServers()
    {
        //  Get selected indices.
        int[] selected =   serverTree.getSelectionRows();
        DefaultTreeModel model = (DefaultTreeModel) serverTree.getModel();
        ServerTreeNode root = (ServerTreeNode) model.getRoot();
        
        if ( selected != null && selected.length > 0 ) {
            int i=0;
            int total = model.getChildCount(root);
            for (int j=0; j<total; j++) // loop through all root's children
            {                            
                int sel=-1;
                if (i<selected.length)
                    sel = selected[i]-1;             
               
                if ( sel != j ) {
                    ServerTreeNode currentNode = (ServerTreeNode) model.getChild(root, i); 
              
                    if (currentNode.getUserObject() != null ) {
                        String name = currentNode.getUserObject().toString();
                        name = name.substring(0, name.indexOf("[")).trim();
                        try{
                            serverList.removeServer( name ); // NAME Cannot be empty!
                        }catch (Exception e) {
                            
                        }
                    }
                        model.removeNodeFromParent(currentNode);
                        // how to remove from serverlist if name is empty?
                } else {
                    i++;
                }                  
            }         
            model.reload();
        }
    }
    
    /**
     * Query a registry for any new SSAP servers. New servers must have a
     * different short name.
     */
    public void updateServers()
    {
      //  treePanel.setVisible(false);
        StarTable table = null;
        try {
                 
            table = TableLoadPanel.loadTable( this, new SSARegistryQueryDialog(), new StarTableFactory() );
         }
        catch ( IOException e ) {
            ErrorDialog.showError( this, "Registry query failed", e );
//            treePanel.setVisible(true);
            return;
        }
        if ( table != null ) {
            if ( table instanceof BeanStarTable ) {
                Object[] resources = ((BeanStarTable)table).getData();
                for ( int i = 0; i < resources.length; i++ ) {
                    serverList.addServer( (SSAPRegResource)resources[i] );
                }
            }
           
        }
        updateTree();
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
                updateTree();
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
        int nrSelected=serverTree.getSelectionCount();
        if (nrSelected  == 0) {
            //dialog "Please select"!!!!
            JOptionPane.showMessageDialog( this,
                    "Please select servers from the list below",
                    "No servers selected", JOptionPane.ERROR_MESSAGE );
            return;
        }
      
        String tagname = (String)JOptionPane.showInputDialog ( this,"Enter Tagname:\n");
        if (tagname == null || tagname.length() == 0) {
            return;
        }
        
        DefaultTreeModel model = (DefaultTreeModel) serverTree.getModel();
        ServerTreeNode root = (ServerTreeNode)  model.getRoot();
        int[] selected = serverTree.getSelectionRows();
        
        
        if (userTags == null) {
            userTags = new ArrayList<JCheckBox>();
        }
        
        // String tagdescr = "tag_"+tagname; // add prefix
       
        // Add tag to selected servers, and tag checkbox to panel
        for (int i=0;i<nrSelected; i++) {
            ServerTreeNode currentNode = (ServerTreeNode) model.getChild(root, selected[i] -1); 
            currentNode.addTag(tagname);
        }       
        
    //    JCheckBox tag = new JCheckBox(tagname);
        tagsListModel.addElement(tagname);
       // tagsList.updateUI();
       // tag.setName(tagdescr);
    //    tag.addItemListener(checkBoxlistener);
        treeRenderer.addTag(tagname);
        
    //    userTags.add(tag);
       // tag.setSelected(true);
  //      treeRenderer.addTag(tagdescr);
        
   //     tagsPanel.add(tag);
     //   tagsList.revalidate();
    //    tagsList.repaint();
        this.repaint();      
        //to do save / reload user tags!!!!!!!!!!!!!!!!!!!
        // to do delete user tags
        
    }
    
    /**
     *  Add new tag to the server list
     */
    protected void removeTag()
    {
        // are there selected servers?
      
        
        String tagname = tagsList.getSelectedValue().toString();
        int index = tagsList.getSelectedIndex();
        tagsListModel.remove(tagsList.getSelectedIndex());
        //tagsListModel.remove(tagsListModel.getindex);
        
        DefaultTreeModel model = (DefaultTreeModel) serverTree.getModel();
        ServerTreeNode root = (ServerTreeNode)  model.getRoot();
        for (int i=0; i<root.getChildCount(); i++) {
            ServerTreeNode currentNode = (ServerTreeNode) model.getChild(root, i);
            
            if ( currentNode.containsTag(tagname))
                currentNode.removeTag(tagname);
        }
 
        treeRenderer.removeTag(tagname) ;
        this.repaint();      
        //to do save / reload user tags!!!!!!!!!!!!!!!!!!!       
    }
    
    /**
     * Event listener to trigger a list update when a new server is
     * added to addServerWIndow
     */
    public void propertyChange(PropertyChangeEvent pvt)
    {
        serverList.addServer(addServerWindow.getResource());
        updateTree();
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
    
    private class ServerTreeNode extends DefaultMutableTreeNode {
        
 
        ArrayList<String> tags = null;
       // String sortingTag="";
        boolean isSelected = false;
  
        public ServerTreeNode( Object o) {
            super(o);
            tags = new ArrayList<String>();
       }
        
 //       public void insert(ServerTreeNode newChild, int childIndex) {
//            super.insert(newChild, childIndex);
        public void addsort (ServerTreeNode newChild) {
            super.add(newChild);
            Collections.sort(this.children, nodeComparator);
        }
        
        public void addTag(String tag) {
            tags.add(tag);
        }
        
        private boolean containsTag(String tag) {
            return tags.contains(tag);
        }
        
        
        private void removeTag(String tag) {
            tags.remove(tag);
        }
        
   //     public void setTagComparator( String tag) {
  //          sortingTag=tag;
 //       }
        
   //     public void resetComparator() {
   //         sortingTag="";
           
   //     }
       
        
        public String toString() {
           
                return getUserObject().toString();
          
        }
        
        public boolean isSelected() {
            return this.isSelected;
        }
        public void setSelected(boolean sel) {
            this.isSelected = sel;
        }
        
      
        protected final Comparator nodeComparator = new Comparator () {
            
            public int compare(Object o1, Object o2) {
                  //    if (sortingTag != null) {
                      boolean a,b;
                    //  a= (((ServerTreeNode) o1).containsTag(sortingTag));
                    //  b= (((ServerTreeNode) o2).containsTag(sortingTag));
                      a= (((ServerTreeNode) o1).isSelected());
                      b= (((ServerTreeNode) o2).isSelected());
                      if (a && !b )
                       return 1;
                      if (!a && b )
                          return -1;           
                  //    }
                      //if ((a && b) || (!a && !b))
                      return (o1.toString().compareToIgnoreCase(o2.toString()));
               
            }
            
            public boolean equals(Object obj) {
                return false;
            }
        };
        
        
        public String[]  getWaveband () {
            if (this.getLevel() != 1)
                return null;
            Enumeration<DefaultMutableTreeNode> e = this.children();
            while (e.hasMoreElements()) {
                String nodeLabel = e.nextElement().getUserObject().toString();
                if (nodeLabel.startsWith("Waveband: ")) {
                    String bands = nodeLabel.replace("Waveband: ", "");
                   // bands = bands.replaceAll("[(*)]", "$1");
                    nodeLabel = bands.replace("[", "");
                    nodeLabel = nodeLabel.replace("]", "");
                    return nodeLabel.split(" "); 
                }
            }
            return null;           
        }   
        public String getDataSource () {
            
            if (this.getLevel() != 1)
                return null;
            // capabilities is the last node
            DefaultMutableTreeNode capnode = (DefaultMutableTreeNode) this.getLastChild();
           
            Enumeration<DefaultMutableTreeNode> e = capnode.children();
            while (e.hasMoreElements()) {
                String nodeLabel = e.nextElement().getUserObject().toString();
                if (nodeLabel.startsWith("Data Source: ")) {
                    return nodeLabel.replace("Data Source: ", "");
                }
            }
            return null;           
        }   
        
   
    } // class servertreenode
    
    private class ServerTreeCellRenderer extends DefaultTreeCellRenderer {
        private JLabel empty = new JLabel();
        private ArrayList<String> tagList;
        private ArrayList<String> srcList;
        private ArrayList<String> bandList;
        private HashMap selectHash;
        private boolean allBands = false;
        private boolean allSources = false;
        private boolean selectTags = false; // if not use options selection

        public ServerTreeCellRenderer () {
            super();
            bandList = new ArrayList<String>();
            tagList = new ArrayList<String>();
            srcList = new ArrayList<String>();
            selectHash = new HashMap ();
        }
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean arg2, boolean arg3, boolean arg4, int arg5, boolean arg6) {

            Component c = super.getTreeCellRendererComponent(tree, value, arg2, arg3, arg4, arg5, arg6);

            ServerTreeNode node = (ServerTreeNode) value;
            String shortname=node.getUserObject().toString(); ///!!!!!!
            int end = shortname.indexOf("[");
            if (end >0)
                shortname = shortname.substring(0,end).trim();
            else shortname = shortname.trim();
           
            if (node.isRoot()) {
                c.setForeground(Color.BLACK);
                return c;
            } else if (node.getLevel() == 1){
                if (selectTags ) { // selection from tags tabbed pane
                    if ( tagList.size() >0)  {
               
                        if (matchesTagFilter(node)) {
                            node.setSelected(true);                   
                            c.setForeground(Color.BLACK);
                            serverList.selectServer(shortname);
                        } else {
                            node.setSelected(false);               
                            c.setForeground(Color.RED);
                            serverList.unselectServer(shortname);
                        }
                    } 
                } else { // selection from options tabbed pane
                    if (matchesBandFilter(node) && matchesSrcFilter(node)) {
                        node.setSelected(true);
                        c.setForeground(Color.BLACK);
                        serverList.selectServer(shortname);
                    } else {                        
                        node.setSelected(false);
                        c.setForeground(Color.RED);
                        serverList.unselectServer(shortname);
                    }
                }
                return c; //empty;
           // if (containsMatchingChild(node)) {
          //      c.setForeground(Color.BLACK);
           //     return c;
           // }
          //  }else 
               // c.setForeground(Color.GRAY);
            }
            c.setForeground(Color.GRAY);
            return c;
            
        }//

        private boolean matchesBandFilter(ServerTreeNode node) {
  
            if (allBands)
                return true;
            String []  bands = node.getWaveband();
            for (int i=0;i< bands.length; i++) {
                if (bandList.contains(bands[i]))
                    return true;
            }
            return false;
        }
        private boolean matchesTagFilter(ServerTreeNode node) {
                      
            for (int i=0;i< tagList.size(); i++) {
                if (node.containsTag(tagList.get(i)))
                    return true;
            }
            return false;
        }
        private boolean matchesSrcFilter(ServerTreeNode node) {
            if (allSources)
                return true;
            String srctag = node.getDataSource();
            for (int i=0;i< srcList.size(); i++) {
                if (srctag.equalsIgnoreCase(srcList.get(i)))
                    return true;
            }
            return false;
        }
   
        public void addBand( String band ) {
            if (! bandList.contains(band))
                bandList.add(band);
        }
      
        public void removeBand(String band) {     
            if (bandList.contains(band))
                bandList.remove(bandList.indexOf(band));
               
        }
        
        public void addTag( String tag ) {
            if (! tagList.contains(tag)) {
                tagList.add(tag);
            }
        }
        public void addSrc( String src ) {
            if (! srcList.contains(src)) {
                srcList.add(src);
            }
        }
        public void removeTag(String tag) {     
            if (tagList.contains(tag))
            tagList.remove(tagList.indexOf(tag));
               
        }
        public void removeTags() {     
             tagList.clear();
        }
        public void removeSrc(String src) {      
            if (srcList.contains(src))
                srcList.remove(srcList.indexOf(src));
               
        }
        public void setAllBands( boolean set) {
            allBands = set;
        }
        public void setAllSources( boolean set) {
            allSources = set;
        }
      
        public void setTagsSelection( boolean sel) {
            selectTags = sel;
        }
   
    }
    
    //Listens to the check boxes events
    class CheckBoxListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            
                treeRenderer.setTagsSelection(false); // selection from options
                JCheckBox source = (JCheckBox) e.getItemSelectable();
                String src = source.getText(); 
                String name = source.getName();

                if (e.getStateChange() == ItemEvent.SELECTED) {  
                 //   if (name.startsWith("tag") ) {
                 //       treeRenderer.addTag(src);                       
                 //   } else  
                    if (name.startsWith("src") ) {
                        if (src.equals("ALL"))
                            treeRenderer.setAllSources(true);
                        else
                            treeRenderer.addSrc(src);                       
                    } else { // band
                        if (src.equals("ALL"))
                            treeRenderer.setAllBands(true);
                        else treeRenderer.addBand(src);
                    }
                }
                else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    if (name.startsWith("tag")) {
                        treeRenderer.removeTag(src);                       
                    } else if (name.startsWith("src")) {
                        if (src.equals("ALL"))
                            treeRenderer.setAllSources(false);
                        else
                            treeRenderer.removeSrc(src); 
                    } else {
                        
                        if (src.equals("ALL"))
                            treeRenderer.setAllBands(false);
                        else
                            treeRenderer.removeBand(src);    
                   }
                      
               }
                
              serverTree.updateUI();
                
            }
           
    }
    
    class TagsListSelectionListener implements ListSelectionListener {    
            /**
             * Event listener to check if a tag has been selected
             */
            public void valueChanged(ListSelectionEvent lse) {
                treeRenderer.setTagsSelection(true); // selection from options
                if( !lse.getValueIsAdjusting() )
                {
                    treeRenderer.removeTags();
                    String selectedTag = (String)tagsList.getSelectedValue();
                    if( selectedTag != null )
                        treeRenderer.addTag(selectedTag);  
                  //  else 
                  //      treeRenderer.removeTags();
                }
                serverTree.updateUI();
                
            } // valueChanged
    } // TagsListSelectionListener
            

}
