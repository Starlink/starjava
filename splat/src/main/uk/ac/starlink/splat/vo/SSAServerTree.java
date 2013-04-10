package uk.ac.starlink.splat.vo;



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.TableLoadPanel;
import uk.ac.starlink.util.ProxySetup;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.ProxySetupFrame;



/**
 * SSAServerTree is a panel displaying the SSA servers as a tree, with the server capabilities as branches,
 * and information as leaves . Includes also selection options for the servers, as waveband and data source options
 * as well as user generated tags.
 *
 * @author Margarida Castro Neves 
 * @version $Id: SSAServerTree.java 10350 2012-11-15 13:27:36Z mcneves $
 *
 */






/**
 * SSAServerTree is a panel displaying the SSA servers as a tree, with the server capabilities as branches,
 * and information as leaves . Includes also selection options for the servers, as waveband and data source options
 * as well as user generated tags.
 *
 * @author Margarida Castro Neves 
 * @version $Id: SSAServerTree.java 10350 2012-11-15 13:27:36Z mcneves $
 *
 */



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
    
    /* where to save the tags information */
    private String tagsFile = "defaultTags.xml";
    
    /**
     * The object that manages the actual list of servers.
     */
    private JTree serverTree;
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
    private JCheckBox src_theo = null;
    private JCheckBox src_obs = null;
    private JCheckBox src_sur;
    private JCheckBox src_tmod;
    private JCheckBox src_point;
    private JCheckBox src_cust;
    private JCheckBox src_art;
    private JCheckBox src_all;
    private JCheckBox src_inv;

    
    // user defined tags
    private ArrayList<JCheckBox> userTags;
    private JTabbedPane optionTabs;
    private JPanel tagsPanel;
    private DefaultListModel tagsListModel;
    private JList tagsList;
    
    // sizes
    
    private  int WIDTH = 600;
    private  int HEIGHT = 500;
    private  int WIDTH = 300;
    private  int HEIGHT = 700;
    private int  TAB_HEIGHT = 150;
   
    private CheckBoxListener checkBoxlistener = null;
   
    
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
      
        this.addComponentListener(new resizeListener());
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
    
      // this.setPreferredSize(new Dimension(this.WIDTH,this.HEIGHT));
       this.setMinimumSize(new Dimension(this.WIDTH-450,this.HEIGHT-300));
       this.setPreferredSize(new Dimension(this.WIDTH-285,this.HEIGHT-300));
      // setLayout( new BorderLayout() );
       
       optionTabs = new JTabbedPane();
       JScrollPane optionsScroller = new JScrollPane();
       
       JPanel invOptionsPanel = new JPanel(); // invisible, just to adjust size of frames
       JPanel optionsPanel = new JPanel(new GridBagLayout());
       GridBagConstraints gbcOptions = new GridBagConstraints();
    
       treeRenderer = new ServerTreeCellRenderer();
       checkBoxlistener = new CheckBoxListener();
       
       // BAND
    
       JPanel bandPanel = new JPanel (new GridLayout(3,3));
       bandPanel.setBorder ( BorderFactory.createTitledBorder( "Wave Band" ) );
       band_rad = new JCheckBox( "Radio", false);
       bandPanel.add(band_rad);
<<<<<<< HEAD
       band_mm = new JCheckBox( "Millimeter",  false);
       bandPanel.add(band_mm);
       band_ir = new JCheckBox( "Infrared",  false);
=======
       band_mm = new JCheckBox( "Millimeter",  true);
       bandPanel.add(band_mm);
       band_ir = new JCheckBox( "Infrared",  true);
>>>>>>> When reading servers from registry, create a tree node for each ssap capability
       bandPanel.add(band_ir);
       band_opt = new JCheckBox( "Optical",false);
       bandPanel.add(band_opt);
       band_uv = new JCheckBox( "UV",  false);
       bandPanel.add(band_uv);
       band_euv = new JCheckBox( "EUV",  false);
       bandPanel.add(band_euv);
<<<<<<< HEAD
       band_xr = new JCheckBox( "X-ray",  false);
       bandPanel.add(band_xr);
       band_gr = new JCheckBox( "Gamma-ray", false);
=======
       band_xr = new JCheckBox( "X-ray",  true);
       bandPanel.add(band_xr);
       band_gr = new JCheckBox( "Gamma-ray", true);
>>>>>>> When reading servers from registry, create a tree node for each ssap capability
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
       JPanel srcAllPanel = new JPanel(new GridBagLayout());
  
       JPanel srcPanel = new JPanel (new GridLayout(2, 3));
       
       srcPanel.setBorder ( BorderFactory.createTitledBorder( "Source" ) );
       GridBagConstraints c = new GridBagConstraints();
       c.fill = GridBagConstraints.HORIZONTAL;
            
       src_sur = new JCheckBox("Survey", false);
       srcPanel.add(src_sur);
       src_tmod = new JCheckBox("Theory", false);
       srcPanel.add(src_tmod);
       src_point = new JCheckBox("Pointed", false);
       srcPanel.add(src_point);
       src_cust = new JCheckBox("Custom", false);
       srcPanel.add(src_cust);
       src_art = new JCheckBox("Artificial", false);
       srcPanel.add(src_art);
       src_all = new JCheckBox("ALL", true);     
       srcPanel.add(src_all);
       
       src_sur.setToolTipText("<html>A survey dataset, which typically covers some region of observational <br>" +
       		                   "parameter space in a uniform fashion, with as complete as possible <br>" +
       		                   "coverage in the region of parameter space observed.</html>");
       src_tmod.setToolTipText("<html>Theory data, or any data generated from a theoretical model, <br>for example a synthetic spectrum.</html>");
       src_point.setToolTipText("<html>A pointed observation of a particular astronomical object or field. <br> " +
               " Typically these are instrumental observations taken as part of some PI observing program.<br> " +
               " The data quality and characteristics may be variable, but the observations of a particular <br>" +
               " object or field may be more extensive than for a survey.</html>");
       src_cust.setToolTipText("Data which has been custom processed, e.g., as part of a specific research project.");
       src_art.setToolTipText("<html>Artificial or simulated data.  This is similar to theory data but need not be based <br>" +
               "on a physical model, and is often used for testing purposes.</html>");
       src_all.setToolTipText("All servers (including the ones with no data source set)");

       src_sur.setName("src_sur");
       src_tmod.setName("src_tmod");
       src_point.setName("src_point");
       src_cust.setName("src_cust");
       src_art.setName("src_art");
       src_all.setName("src_all");
       
       src_sur.addItemListener(checkBoxlistener); 
       src_tmod.addItemListener(checkBoxlistener);
       src_point.addItemListener(checkBoxlistener);
       src_cust.addItemListener(checkBoxlistener);
       src_art.addItemListener(checkBoxlistener);
       src_all.addItemListener(checkBoxlistener);
       
       treeRenderer.addSrc(src_sur.getText());
       treeRenderer.addSrc(src_point.getText());
       treeRenderer.addSrc(src_tmod.getText());
       treeRenderer.addSrc(src_cust.getText());
       treeRenderer.addSrc(src_art.getText());
       treeRenderer.addSrc(src_all.getText());
        
       JPanel srcPanel2 = new JPanel (new GridLayout(2, 1));
       srcPanel2.setBorder ( BorderFactory.createTitledBorder( "" ) );
       src_obs = new JCheckBox("Observed data", false);
       src_obs.setToolTipText("<html>All observation servers.</html>");
      
       srcPanel2.add(src_obs);       
       src_obs.setName("src_obs");
       src_obs.addItemListener(checkBoxlistener);
       treeRenderer.addSrc(src_obs.getText());
       
       src_theo = new JCheckBox("Theoretical data", false);
       src_theo.setToolTipText("<html>All theoretical servers.</html>");
    
       srcPanel2.add(src_theo);
       src_theo.setName("src_theo");
       src_theo.addItemListener(checkBoxlistener);
       treeRenderer.addSrc(src_theo.getText());
       src_inv = new JCheckBox("Invisible"); // so I can uncheck both theo/obs if I need
      
       
       c.weightx=1;
       c.gridy = 0;
      
       srcAllPanel.add( srcPanel2, c);
       c.gridy = 1;
       srcAllPanel.add( srcPanel, c);
     
       gbcOptions.anchor = GridBagConstraints.NORTHWEST;
       gbcOptions.fill = GridBagConstraints.HORIZONTAL;
       gbcOptions.weightx=.5;
       gbcOptions.weighty=0;
       gbcOptions.gridx=0;
       gbcOptions.gridy=0;
     
       optionsPanel.add(srcAllPanel,gbcOptions);
       gbcOptions.weighty=1;
       gbcOptions.gridy=1;
       optionsPanel.add(bandPanel,gbcOptions);
       invOptionsPanel.setLayout(new BorderLayout());
       
       invOptionsPanel.add(optionsPanel, BorderLayout.LINE_START);
       optionTabs.addTab("Options", invOptionsPanel);

       // User defined tags
       
       tagsPanel = new JPanel ();
       tagsPanel.setLayout(new GridBagLayout());
       GridBagConstraints gbcTags = new GridBagConstraints();
       gbcTags.anchor = GridBagConstraints.NORTHWEST;
       gbcTags.weightx=.5;
       gbcTags.weighty=0;
       gbcTags.gridx=0;  
       gbcTags.gridy=0;
       gbcTags.fill = GridBagConstraints.NONE;
     
       JPanel tagsButtonsPanel=new JPanel();
       AddTagAction addTagAction = new AddTagAction( "Add tag" );
       JButton addTagButton = new JButton(addTagAction);

       addTagButton.setToolTipText("Please select servers to be tagged");
       
       tagsButtonsPanel.add(addTagButton);
       RemoveTagAction removeTagAction = new RemoveTagAction( "Remove tag" );
       JButton removeTagButton = new JButton(removeTagAction);
     
       tagsButtonsPanel.add(removeTagButton);
     
   
       tagsPanel.add(tagsButtonsPanel,gbcTags);
       tagsPanel.setBorder(BorderFactory.createTitledBorder( "Add tags to selected servers" ) );
       optionTabs.addTab("Tags", tagsPanel);
       
       tagsListModel = new DefaultListModel();
       tagsList = new JList(tagsListModel);
       ListSelectionModel selectionModel = tagsList.getSelectionModel();
       selectionModel.addListSelectionListener(new TagsListSelectionListener());
      
       JScrollPane tagScroller = new JScrollPane( tagsList );
   
       gbcTags.gridx=0;  
       gbcTags.gridy=1;
       gbcTags.weighty=1;
       gbcTags.weightx=1;
     
       gbcTags.fill=GridBagConstraints.HORIZONTAL;
       tagsPanel.add(tagScroller,gbcTags);
 
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
       
        serverPanel=new JPanel();
        serverPanel.setLayout(new GridBagLayout() );
        serverPanel.setBorder ( BorderFactory.createTitledBorder( "SSAP Servers" ) );
//        serverPanel.setMinimumSize(new Dimension(200,400));
//        optionTabs.setMinimumSize(new Dimension(200,400));
       
        ServerTreeNode root = new ServerTreeNode("SSAP Servers");
        populateTree(root);
      
      
        serverTree = new JTree(root);
        DefaultTreeModel model = (DefaultTreeModel) serverTree.getModel();
       
        try {
            restoreTags();
        } catch (SplatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        model.reload();
      //  serverTree.expandRow(0);
    //    serverTree.updateUI();
            
        serverTree.setCellRenderer(treeRenderer);
       // treeRenderer.setPreferredSize(preferredSize)
        JScrollPane jsp = new JScrollPane(serverTree);
    //    jsp.setViewportView(mainPanel);
        jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
       
        GridBagConstraints gbcServer=new GridBagConstraints();
        gbcServer.gridx=0;
        gbcServer.gridy=0;
        gbcServer.weighty=1;
        gbcServer.weightx=1;
        gbcServer.fill=GridBagConstraints.BOTH;
        serverPanel.add(jsp,gbcServer);
        
      
        initMenus();
        gbcServer.anchor = GridBagConstraints.SOUTHWEST;
        gbcServer.gridx=0;
        gbcServer.gridy=1;
        gbcServer.weighty=0;
        serverPanel.add(controlPanel, gbcServer);
      
        optionsScroller.getViewport().add( optionTabs, null );
        optionsScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        optionsScroller.setMinimumSize(new Dimension(200,280));
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
        gbc.weighty=0.5;
        gbc.gridx=0;  
        gbc.gridy=1;
        gbc.fill = GridBagConstraints.BOTH;
        this.add(serverPanel, gbc);
        
/*        
        JSplitPane splitPanel = new JSplitPane();
        splitPanel.setOneTouchExpandable(true);
        splitPanel.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        splitPanel.setDividerLocation(0.8);
      
        splitPanel.setLeftComponent(ServerPanServer
        splitPanel.setRightComponent(optionsScroller);
       
        this.add(splitPanel);
 */       
    }


    
    /**
     * Add SSAServerList elements to the tree.
     *
     * @param root: the root of the tree.
     */
    private void populateTree( ServerTreeNode root )
    {      
       
        Iterator  i =  serverList.getIterator();
        while( i.hasNext()) {
            SSAPRegResource server= (SSAPRegResource) i.next(); 
            String name = server.getShortName();
<<<<<<< HEAD
  
            SSAPRegCapability caps[] = server.getCapabilities();

            ServerTreeNode stn = new ServerTreeNode( name  ); 
<<<<<<< HEAD
                
            addInfoNodes(server, caps[0], stn);
            if (stn.isSelected())
                 serverList.selectServer(server.getShortName());
=======
            addInfoNodes(server, stn);
            if (stn.isSelected())
                serverList.selectServer(server.getShortName());
>>>>>>> Bug fix on server selection after querying registry for updates
            root.addsort( stn );
                 
=======
    //        if (name == null || name.length() == 0)
    //            name = "<>" ;
    //        ServerTreeNode stn = new ServerTreeNode( name  ); 
            SSAPRegCapability caps[] = server.getCapabilities();
 /* 
            int nrssacaps=0;
            for (int c=0; c< server.getCapabilities().length; c++){
              //  SSAPRegCapability cap = server.getCapabilities()[c];
                String xsi= caps[c].getXsiType();
                if (xsi != null && xsi.startsWith("ssa")) {*/
                    ServerTreeNode stn = new ServerTreeNode( name  ); 
                  //  addInfoNodes(server, caps[c], nrssacaps, stn);
                    addInfoNodes(server, caps[0], stn);
                    if (stn.isSelected())
                        serverList.selectServer(server.getShortName());
                    root.addsort( stn );
                 //   nrssacaps++;
              /*  }
            }*/
>>>>>>> When reading servers from registry, create a tree node for each ssap capability
         }
    }
    
    /**
     * Add SSAServerList Resource und Capability elements to the tree.
     *
     * @param serverList the SSAServerList reference.
     */
<<<<<<< HEAD
=======
//    private void addInfoNodes ( SSAPRegResource server, SSAPRegCapability cap, int capnr, ServerTreeNode servernode )
>>>>>>> When reading servers from registry, create a tree node for each ssap capability
    private void addInfoNodes ( SSAPRegResource server, SSAPRegCapability cap, ServerTreeNode servernode )
    {      
    
      //  private RegCapabilityInterface[] capabilities;
      //   private String[] subjects = null;

            servernode.add( new ServerTreeNode( "Title: " + server.getTitle() ));
            servernode.add( new ServerTreeNode( "Identifier: " + server.getIdentifier() ));
            servernode.add( new ServerTreeNode( "Publisher: " + server.getPublisher() ));
            servernode.add( new ServerTreeNode( "Contact: " + server.getContact()));
            servernode.add( new ServerTreeNode( "Ref. URL: " + server.getReferenceUrl()));
          
         
            servernode.add( new ServerTreeNode( "Waveband: " + Arrays.toString(server.getWaveband())));
            
<<<<<<< HEAD
=======
      //      servernode.add( new ServerTreeNode( "Version: " + server.getVersion()));
            
            // get the right capability      
        //    SSAPRegCapability caps[] = server.getCapabilities();
        //    SSAPRegCapability cap=null;
       //     for (int i=0; i< caps.length; i++){
    //            SSAPRegCapability c = server.getCapabilities()[i];
   //             if (c.getXsiType().equals("ssa:SimpleSpectralAccess"))
  //          }
>>>>>>> When reading servers from registry, create a tree node for each ssap capability
            ServerTreeNode capnode = new ServerTreeNode( "Capability" );
            capnode.add( new ServerTreeNode( "Access URL: " + cap.getAccessUrl() ));
            capnode.add( new ServerTreeNode( "Description: " + cap.getDescription() )); 
            capnode.add( new ServerTreeNode( "Data Source: " + cap.getDataSource() )); 
            capnode.add( new ServerTreeNode( "Data Type: " + cap.getDataType() ));
            capnode.add( new ServerTreeNode( "Creation Type: " + cap.getCreationType() ));
          
            capnode.add( new ServerTreeNode( "Standard ID: " + cap.getStandardId() )); 
            capnode.add( new ServerTreeNode( "Version: " + cap.getVersion() )); 
            capnode.add( new ServerTreeNode( "XSI Type: " + cap.getXsiType() ));   
           
<<<<<<< HEAD
            servernode.setUserObject((String) servernode.getUserObject() + "       [" + server.getTitle() + "]");

       
//            logger.info( server.getShortName()+":" + Arrays.toString(server.getWaveband())+":"+cap.getDataSource()+":"+cap.getDataType()+":"+cap.getCreationType()+":"+cap.getAccessUrl()+":"+server.getContact() );
=======
         
      //      if (capnr > 0)
       //         servernode.setUserObject((String) servernode.getUserObject() + "("+ capnr + ")"+"       [" + server.getTitle() + "]");
       //     else 
                servernode.setUserObject((String) servernode.getUserObject() + "       [" + server.getTitle() + "]");

         //   servernode.setUserObject((String) servernode.getUserObject() + "       [" + cap.getDataSource() + "]" +"["+ cap.getDataType() + "]") ;
            
       
           logger.info( server.getShortName()+":" + Arrays.toString(server.getWaveband())+":"+cap.getDataSource()+":"+cap.getDataType()+":"+cap.getCreationType()+":"+cap.getAccessUrl()+":"+server.getContact() );
>>>>>>> When reading servers from registry, create a tree node for each ssap capability
           
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
        RemoveAction removeAction = new RemoveAction( "Remove selected" );
 //       optionsMenu.add( removeAction );
        JButton removeButton = new JButton( removeAction );
 
        removeButton.setMargin(new Insets(2,2,2,2));  
  //      topActionBar.add( Box.createGlue() );
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.weightx=0.5;
        gbc.fill=GridBagConstraints.NONE;
        topActionBar.add( removeButton, gbc );
  //      controlPanel.add( removeButton, BorderLayout.PAGE_START );
        removeButton.setToolTipText
            ( "Remove selected servers from current list" );

        //  Action to check a registry for additional/updated servers.
        QueryNewAction newAction = new QueryNewAction( "Query registry" );
   //     optionsMenu.add( newAction );
        JButton newQueryButton = new JButton( newAction );
        newQueryButton.setMargin(new Insets(2,2,2,2));  
  //      topActionBar.add( Box.createGlue() );
        
        botActionBar.add( newQueryButton, gbc );
       // controlPanel.add( newQueryButton, BorderLayout.PAGE_END );
        newQueryButton.setToolTipText( "Query registry for new SSAP services" );
        

        //  Add action to select all servers.
        SelectAllAction selectAllAction = new SelectAllAction( "Select all" );
  //      optionsMenu.add( selectAllAction );
        JButton selectAllButton = new JButton( selectAllAction );
   //     topActionBar.add( selectAllButton );
        selectAllButton.setToolTipText( "Select all servers" );

        //  Add action to just delete all servers.
        DeleteAction deleteAction = new DeleteAction( "Delete all" );
        
        //  Add action to manually add a new server to the list
        AddNewAction addNewAction = new AddNewAction( "Add New Server" );
      //  JPanel addPanel = new JPanel();
      //  addPanel.setLayout(new BorderLayout());
        JButton addButton1 = new JButton(addNewAction);
        addButton1.setToolTipText("Add new service to the list");
        addButton1.setMargin(new Insets(2,2,2,2));  
  //      addPanel.add(addButton1);
        gbc.gridx=1;
        botActionBar.add(addButton1, gbc);
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
        try {
            restoreTags();
        } catch (SplatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
            try {
                  saveServerTags();
            } catch (SplatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> When reading servers from registry, create a tree node for each ssap capability
                    
                    SSAPRegResource server = (SSAPRegResource)resources[i];
                    String shortname = server.getShortName();
                    if (shortname == null || shortname.length()==0)
<<<<<<< HEAD
                        shortname = server.getTitle(); // avoid problems if server has no name (should not happen, but it does!!!)
=======
                        shortname = "<>"; // avoid problems if server has no name (should not happen!!!)
>>>>>>> When reading servers from registry, create a tree node for each ssap capability
                    SSAPRegCapability caps[] = server.getCapabilities();
                    int nrcaps = server.getCapabilities().length;
                    int nrssacaps=0;
                    // create one serverlist entry for each ssap capability
                    for (int c=0; c< nrcaps; c++) {
                         String xsi= caps[c].getXsiType();
                         if (xsi != null && xsi.startsWith("ssa")) {
                            SSAPRegResource ssapserver = new SSAPRegResource(server);
                            SSAPRegCapability onecap[] = new SSAPRegCapability[1];
                            onecap[0] = caps[c];  
                            String name = shortname;
                            ssapserver.setCapabilities(onecap);
                            if (nrssacaps > 0) 
                                name =  name + "(" + nrssacaps + ")";
                            ssapserver.setShortName(name);
                            serverList.addServer( ssapserver );
                            serverList.unselectServer(ssapserver.getShortName());
                            nrssacaps++;
                        }
                    }
                  //  serverList.addServer( (SSAPRegResource)resources[i] );
                //    serverList.unselectServer(((SSAPRegResource)resources[i]).getShortName());
<<<<<<< HEAD
=======
                    serverList.addServer( (SSAPRegResource)resources[i] );
                    serverList.unselectServer(((SSAPRegResource)resources[i]).getShortName());
>>>>>>> Bug fix on server selection after querying registry for updates
=======
>>>>>>> When reading servers from registry, create a tree node for each ssap capability
                }
            }
           
        }
        updateTree();
        this.firePropertyChange("changeServerlist", false, true);
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
        
        boolean newTag = true;
        
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
        if ( tagsListModel.contains(tagname)) {
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
            
            if (n == JOptionPane.YES_OPTION) 
                removeTag(tagname); // remove the old tag, create it again            
            else if ( n == JOptionPane.CANCEL_OPTION ) // add selected to existing tag
                return;
            else if (n == JOptionPane.NO_OPTION)
                newTag = false; // the tags will be added to the existing tag
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
       
        tagsList.setSelectedValue(tagname, true);
        if ( newTag ) {
            tagsListModel.addElement(tagname);       
            treeRenderer.addTag(tagname);
        }

        this.repaint();      

    }
  
    /**
     *  Remove tag
     */
    
    protected void removeTag()
    {
        String tagname = tagsList.getSelectedValue().toString();
        removeTag(tagname);
    
    }
    protected void removeTag(String tagname) {
        //
        tagsListModel.removeElement(tagname);
        //tagsListModel.remove(tagsList.getSelectedIndex());
        //tagsListModel.remove(tagsListModel.getindex);
        
        DefaultTreeModel model = (DefaultTreeModel) serverTree.getModel();
        ServerTreeNode root = (ServerTreeNode)  model.getRoot();
        for (int i=0; i<root.getChildCount(); i++) {
            ServerTreeNode currentNode = (ServerTreeNode) model.getChild(root, i);
            
            if ( currentNode.containsTag(tagname))
                currentNode.removeTag(tagname);
        }
 
        treeRenderer.removeTag(tagname) ;
        try {
            saveServerTags();
        } catch (SplatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.repaint();      
              
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
        DefaultTreeModel model = (DefaultTreeModel) serverTree.getModel();
        ServerTreeNode root = (ServerTreeNode)  model.getRoot();
        for (int i=0; i<root.getChildCount(); i++) {
            ServerTreeNode currentNode = (ServerTreeNode) model.getChild(root, i);
            if ( currentNode.hasTags() ) {
                
            //    ServerTags st = new ServerTags(currentNode.getShortName(), currentNode.getTags());
                ServerTags st = new ServerTags(currentNode.getShortName(), currentNode.getTags());
                
                try {
                    encoder.writeObject(st);
                } 
                catch (Exception e) {
                        e.printStackTrace();
                }  
            }
        }
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
                e.printStackTrace();
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
            ServerTags st = null;
            
       
            DefaultTreeModel model = (DefaultTreeModel) serverTree.getModel();
            ServerTreeNode root = (ServerTreeNode)  model.getRoot();
         
            
        
            while ( true ) {
                try {
                    Object ob =  decoder.readObject();
                    st = (ServerTags)  ob;
                    // search for shortname
                    for (int i=0; i<root.getChildCount(); i++) { 
                        
                        ServerTreeNode node = (ServerTreeNode) model.getChild(root, i); 
                        if (st.getName().equals(node.getShortName())) {
                            ArrayList<String> tags = st.getTags();
                            for (int t=0; t<tags.size(); t++) {
                                String tagname = tags.get(t);
                                node.addTag(tagname);
                                if ( ! tagsListModel.contains(tagname) )
                                    tagsListModel.addElement(tagname);
                           ////     tagsList.setSelectedValue(tagname, true);
                                treeRenderer.addTag(tagname);
                            }
                        }
                    }
                    //!!!!!addServer(server, false);
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
            }
            decoder.close();
            model.reload();
          
        }
    
      
    /**
     * Event listener to trigger a list update when a new server is
     * added to addServerWIndow
     */
    public void propertyChange(PropertyChangeEvent pvt)
    {
        serverList.addServer(addServerWindow.getResource());
        serverList.unselectServer(addServerWindow.getResource().getShortName());
        updateTree();
    }
    
    // returns the serverlist
    public SSAServerList getServerList() {
        return serverList;
    }
    
<<<<<<< HEAD
=======

>>>>>>> Bug fix on server selection after querying registry for updates
  
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
    
    public class ServerTreeNode extends DefaultMutableTreeNode {
        
 
        protected ArrayList<String> tags = null;
       // String sortingTag="";
        protected boolean isSelected = false;
        protected String accessUrl = null;
  
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
            addTag(tag, true);
            
        }
        public void addTag(String tag, boolean save) {
                tags.add(tag); 
           // save: TO DO
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
        protected boolean hasTags() {
            return (tags.size() > 0);
        }
        protected ArrayList<String> getTags() {
            return (tags);
        }
        
      
        
        public String toString() {
           
                return getUserObject().toString();
          
        }
        public String getShortName() {
            String shortname=getUserObject().toString(); ///!!!!!!
            int end = shortname.indexOf("[");
            if (end >0)
                shortname = shortname.substring(0,end).trim();
            else shortname = shortname.trim();
            return shortname;
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
        
        
        public String[]  getWavebands () {
            if (this.getLevel() != 1)
                return null;
            Enumeration<DefaultMutableTreeNode> e = this.children();
            while (e.hasMoreElements()) {
                String nodeLabel = e.nextElement().getUserObject().toString();
                if (nodeLabel.startsWith("Waveband: ")) {
                    String bands = nodeLabel.replace("Waveband: ", "");
                    bands = bands.replaceAll(",", "");
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
        public String getDataType () {
            
            if (this.getLevel() != 1)
                return null;
            // capabilities is the last node
            DefaultMutableTreeNode capnode = (DefaultMutableTreeNode) this.getLastChild();
           
            Enumeration<DefaultMutableTreeNode> e = capnode.children();
            while (e.hasMoreElements()) {
                String nodeLabel = e.nextElement().getUserObject().toString();
                if (nodeLabel.startsWith("Data Type: ")) {
                    return nodeLabel.replace("Data Type: ", "");
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
        private boolean allObsSources = false;
        private boolean allTheoSources = false;

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
                            c.setForeground(Color.GRAY);
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
                        c.setForeground(Color.GRAY);
                        serverList.unselectServer(shortname);
                    }
                }
                return c; //empty;
        
            }
         
            return c;
            
        }//

        private boolean matchesBandFilter(ServerTreeNode node) {
  
            if (allBands)
                return true;
            String []  bands = node.getWavebands();
            for (int i=0;i< bands.length; i++) {
<<<<<<< HEAD
             //   logger.info("band[i]="+bands[i]+" "+bandList.toString());
=======
                logger.info("band[i]="+bands[i]+" "+bandList.toString());
>>>>>>> When reading servers from registry, create a tree node for each ssap capability
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
        
            if (allTheoSources) {
                String srctype = node.getDataType();
                return ( srctag.equalsIgnoreCase("theory") || srctag.equalsIgnoreCase("artificial") || (srctag.equalsIgnoreCase("custom") && srctype.equalsIgnoreCase("simulation")));
            }
            if (allObsSources) {
                String srctype = node.getDataType();
                return ( ! (srctag.equalsIgnoreCase("theory") || srctag.equalsIgnoreCase("artificial") || (srctag.equalsIgnoreCase("custom") && srctype.equalsIgnoreCase("simulation"))));                  
            }

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
        public void setAllObsSources( boolean set) {
            allObsSources = set;
        }
        public void setAllTheoSources( boolean set) {
            allTheoSources = set;
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
                        if (src.equals("ALL")) {
                            treeRenderer.setAllSources(true);
                            src_sur.setSelected(false);
                            src_tmod.setSelected(false);
                            src_point.setSelected(false);
                            src_cust.setSelected(false);
                            src_art.setSelected(false);
                            src_obs.setSelected(false);
                            src_theo.setSelected(false);
                            src_inv.setSelected(true);
                          
                        }
                        else  if (src.equals("Observed data")) {                              
                            treeRenderer.setAllSources(false);
                            src_sur.setSelected(false);
                            src_tmod.setSelected(false);
                            src_point.setSelected(false);
                            src_cust.setSelected(false);
                            src_art.setSelected(false);
                            src_all.setSelected(false);
                            src_inv.setSelected(false);
                            src_obs.setSelected(true);
                            treeRenderer.setAllObsSources(true);                           
                        }
                        else if (src.equals("Theoretical data")) {
                            src_sur.setSelected(false);
                            src_tmod.setSelected(false);
                            src_point.setSelected(false);
                            src_cust.setSelected(false);
                            src_art.setSelected(false);
                            src_all.setSelected(false);
                            src_inv.setSelected(false);
                            src_theo.setSelected(true);
                            treeRenderer.setAllSources(false);
                            treeRenderer.setAllTheoSources(true);
                        }
                        else {
                            src_theo.setSelected(false);
                            src_obs.setSelected(false);
                            src_inv.setSelected(true);
                            src_all.setSelected(false);
                            treeRenderer.addSrc(src);     
                        }
                    } else { // band
                        if (src.equals("ALL")) {
                            treeRenderer.setAllBands(true);
                            band_rad.setSelected(false);
                            band_mm.setSelected(false);
                            band_opt.setSelected(false);
                            band_ir.setSelected(false);
                            band_uv.setSelected(false);
                            band_euv.setSelected(false);
                            band_xr.setSelected(false);
                            band_gr.setSelected(false); 
                        }    
                        else {
                            treeRenderer.addBand(src);
                            band_all.setSelected(false);
                        }
                    }
                }
                else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    if (name.startsWith("tag")) {
                        treeRenderer.removeTag(src);                       
                    } else if (name.startsWith("src")) {
                        if (src.equals("ALL")) { // deselect ALL : 
                            src_theo.setSelected(false);
                            src_obs.setSelected(false);
                            src_inv.setSelected(true);
                            treeRenderer.setAllSources(false);
                        }
                        else   if (src.equals("Observed data")) {
                           
                            treeRenderer.setAllObsSources(false);
                        }
                        else if (src.equals("Theoretical data")) {
                           
                            treeRenderer.setAllTheoSources(false);
                        }
                        else {
                            src_theo.setSelected(false);
                            src_obs.setSelected(false);
                            treeRenderer.removeSrc(src); 
                        }
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
                treeRenderer.setTagsSelection(true); // selection from tags
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
<<<<<<< HEAD
=======

            
>>>>>>> Bug fix on server selection after querying registry for updates

    class resizeListener extends ComponentAdapter {
        public void componentResized(ComponentEvent e) {
            updateUI();
        }
    }
    
    
}
