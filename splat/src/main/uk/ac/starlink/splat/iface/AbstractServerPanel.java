package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import jsky.util.Logger;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.vo.AbstractServerList;
import uk.ac.starlink.splat.vo.AddNewServerFrame;
import uk.ac.starlink.splat.vo.ObsCoreServerList;
import uk.ac.starlink.splat.vo.SSAPRegResource;
import uk.ac.starlink.splat.vo.ServerPopupTable;
import uk.ac.starlink.splat.vo.ServerTags;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;
import uk.ac.starlink.util.gui.ErrorDialog;



public abstract class AbstractServerPanel extends JPanel implements PropertyChangeListener  {
    
    protected static JPanel serverPanel;
    protected static JPanel controlPanel;
    protected static JPanel optionsPanel;
    
    private  String serviceType="";
    private  int WIDTH;
    private  int HEIGHT;
  
  //  private AbstractServerList serverList;
    
    private  ServerPopupTable serverTable;
   
    protected JButton addServerButton = null;
  
    private JComboBox tagCombo;
    private  String MANUALLY_ADDED_STR = "ManuallyAdded";
    private static boolean ManuallyAddPossible = true;

  
    
    /**
     * File chooser for storing and restoring server lists.
     */
    protected BasicFileChooser fileChooser = null;
    /**
     * Frame for adding a new server.
     */
    protected AddNewServerFrame addServerWindow = null;

    protected ServerTags serverTags;
   // private boolean selectedTagFlag;
    
    /**
     * Create an instance.
     */
    public AbstractServerPanel ( )
    {      
       
    //    WIDTH=getWidth();
    //    HEIGHT=getHeight(); 
       // tagsFile = getTagsFilename();
        serviceType = getServiceType();        
        serverTags = new ServerTags(getTagsFilename());
       //tagsMap = new HashMap<String,ArrayList<String>>();
       // serverTagsMap = new HashMap<String,ArrayList<String>>();     
        
        this.addComponentListener( new resizeListener());
         
    }  
   
    /**
     * Create an instance.
     */
    public AbstractServerPanel ( AbstractServerList list )
    {      
        this();
        serverTable = new ServerPopupTable(list);  
       
    }  
   
 
    /*
    abstract public String getTagsFile();
    abstract public void updateServers();
    abstract public Logger getLogger();
    abstract public AbstractServerList  getServerList();
    */
    abstract public int getWidth();
    abstract public int getHeight();
    abstract public String getServiceType();
    abstract public String getTagsFilename();
   
    
 
  
   abstract protected StarTable makeRegistryQuery();
    
 // returns the serverlist
   public AbstractServerList getServerList() {
        return serverTable.getServerList();
   }


   
    public ServerPopupTable getServerTable() {
        return serverTable;
    }
    
    public void setServerTable(ServerPopupTable table) {
        serverTable=table;
        selectTaggedServers(getSelectedTag());
    }
    
    public StarTable  queryRegistryWhenServersNotFound() {
        int option = JOptionPane.showConfirmDialog(this, "No previously saved services list found. Query the Registry?", 
                "Not Found", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            StarTable table = makeRegistryQuery();    
            return table;
        } else
            return null;
    }

   
  /*  private void sortTable() {
        sorter = (TableRowSorter<DefaultTableModel>) serverTable.getRowSorter();
         List<RowSorter.SortKey> sortKeys = new ArrayList<SortKey>();
         sortKeys.add(new RowSorter.SortKey(ServerPopupTable.SHORTNAME_INDEX, SortOrder.ASCENDING));
         sorter.setSortKeys(sortKeys);
         sorter.sort();
         
     }*/
    /**
     * Initialise the main part of the user interface.
     * @param jPanel 
     * @param jComponent can be a JPanel or a JScrollPane
     */
    protected void initUI(JComponent optionsComponent, JComponent serverPanel)
    {
        WIDTH=getWidth();
        HEIGHT=getHeight(); 
      // this.setPreferredSize(new Dimension(this.WIDTH,this.HEIGHT));
       setMinimumSize(new Dimension(WIDTH-100,HEIGHT-300));
       setPreferredSize(new Dimension(WIDTH,HEIGHT));
   
       
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth=1;
        gbc.gridheight=1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx=1;
        gbc.weighty=0;
        gbc.gridx=0;  
        gbc.gridy=0;
        gbc.fill = GridBagConstraints.BOTH;
        add(optionsComponent, gbc);
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.gridheight=GridBagConstraints.REMAINDER;
        gbc.weighty=1;
        gbc.weightx=1;
        gbc.gridx=0;  
        gbc.gridy=2;
     //   gbc.fill = GridBagConstraints.BOTH;
        add(serverPanel, gbc);
        
        updateUI();
    }
    
    /**
     * Initialise the main part of the user interface.
     */
    protected JPanel initServerPanel() {
        return initServerPanel(serverTable, true);
    }
    protected JPanel initServerPanel(ServerPopupTable table) {
        return initServerPanel(table, false);
    }
    protected JPanel initServerPanel(ServerPopupTable table, boolean useBorder)
    {
      
        serverTable=table; //!!!
        serverPanel=new JPanel();
        serverPanel.setLayout(new GridBagLayout() );
        if (useBorder)
            serverPanel.setBorder ( BorderFactory.createTitledBorder( serviceType+" Servers" ) );
        
      
        serverTable.setComponentPopupMenu(makePopupMenu());
        serverTable.addMouseListener(new ServerTableMouseListener());
        serverTable.getSelectionModel().addListSelectionListener(new SelectionListener());
         
        JScrollPane jsp = new JScrollPane(serverTable);
        
        jsp.getAccessibleContext().setAccessibleName("Services");
        
      //  jsp.setViewportView(serverTable);
   //     jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
   //     jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        //jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

       
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
      
        return serverPanel;
        
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
        readButton.setToolTipText( "Read server list back from a disk-file" );

        //  Add action to save the server list to disk file.
        SaveAction saveAction = new SaveAction( "Save Server List", saveImage );
//        fileMenu.add( saveAction );
        JButton saveButton = new JButton( saveAction );
        saveButton.setToolTipText( "Save server list to a disk-file" );

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

 
        selectAllButton.setMargin(new Insets(2,2,2,2));
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.weightx=0.5;
        gbc.fill=GridBagConstraints.NONE;
        topActionBar.add( selectAllButton, gbc );
        gbc.gridx=1;
        topActionBar.add( deselectAllButton, gbc );
 
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
        
        addServerButton = new JButton(addNewAction);
       
        addServerButton.setToolTipText("Add new service to the list");
        addServerButton.setMargin(new Insets(2,2,2,2));  
        addServerButton.getAccessibleContext().setAccessibleDescription("addnew");
        addServerButton.getAccessibleContext().setAccessibleName("addnew");
        gbc.gridx=1;
        botActionBar.add(addServerButton, gbc);
 //       optionsMenu.add( deleteAction );
        JButton deleteButton = new JButton( deleteAction );
        deleteButton.setToolTipText( "Delete all servers from current list" );

        //  Finish action bars.
          gbc.gridx=0;
          gbc.gridy=0;
          gbc.fill=GridBagConstraints.HORIZONTAL;
          controlPanel.add(topActionBar, gbc);//, BorderLayout.NORTH);
          gbc.gridy=1;
          controlPanel.add(botActionBar, gbc);//, BorderLayout.SOUTH);    
    }
    
    protected JPopupMenu makePopupMenu () {

        JPopupMenu popupMenu = new JPopupMenu();
   //     PopupMenuListener popupMenuListener = new PopupMenuListener();

        JMenuItem infoMenuItem = new JMenuItem("Info");
        infoMenuItem.addActionListener(new PopupMenuAction());
        popupMenu.add(infoMenuItem);

        JMenuItem addTagMenuItem = new JMenuItem("Tag current service");
        addTagMenuItem.addActionListener(new PopupMenuAction());
        popupMenu.add(addTagMenuItem);
        
        JMenuItem addTagXMenuItem = new JMenuItem("Tag selected services");
        addTagXMenuItem.addActionListener(new PopupMenuAction());
        popupMenu.add(addTagXMenuItem);
        
        RemoveMenu removeMenu = new RemoveMenu("Remove");
     //   deleteMenuItem.addActionListener(new PopupMenuAction());
        popupMenu.add(removeMenu);
      
        return popupMenu;
    }
    
   

    
    /**
     * Set the ServerList and reconstruct the table.
     *
     * @param serverList the ServerList reference.
     */
    public void setServerList( AbstractServerList serverList )
    {
        TableRowSorter<DefaultTableModel> savedSorter = getTableRowSorter();
        serverTable.setServerList(serverList);
        setTableRowSorter(savedSorter);         
    }
    /**
     * Set the ServerList without reconstructing the table.
     *
     * @param serverList the ServerList reference.
     */ 
    public void setServerListValue( AbstractServerList serverList )
    {
       // TableRowSorter<DefaultTableModel> savedSorter = getTableRowSorter();
        serverTable.setServerListValue(serverList);
      // setTableRowSorter(savedSorter);         
    }
   
    /**
     * update the table
     *
     */
    public void updateTable()
    {
        // check existing nodes
        serverTable.populate();
        serverTags.restoreTags();
       
    }

    protected void setTableRowSorter(TableRowSorter<DefaultTableModel> sorter) {
        serverTable.setRowSorter(sorter);        
    }
    
    protected TableRowSorter<DefaultTableModel> getTableRowSorter() {
        return (TableRowSorter<DefaultTableModel>) serverTable.getRowSorter();    
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
        tagCombo.setSelectedIndex(0);
        serverTable.updateUI();
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
                serverTable.restoreServers( file );
            //    updateTree();
                updateTable();
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }
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
                serverTable.saveServers( file );
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }
    }
    
    public void saveAll() {
        //serverList.saveServers();
        try {
            serverTable.saveServers();
            serverTags.save();
        } catch (SplatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
     *  Remove all servers.
     */
    protected void deleteServers()
    {
        serverTable.removeAll();
      
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
     * update the server list making a registry query
     *
     */
    protected void updateServers () {
        updateServers( makeRegistryQuery() );
    }
    protected void updateServers (StarTable table) {
        TableRowSorter<DefaultTableModel> savedSorter = getTableRowSorter();
        List<RowSorter.SortKey> sortKeys = (List<SortKey>) savedSorter.getSortKeys();
        if (isManuallyAddPossible())
            serverTable.updateServers(table, serverTags.getList(MANUALLY_ADDED_STR));
        else 
            serverTable.updateServers(table);
        //tagsMap.get(MANUALLY_ADDED_STR)); 
        //       serverList = (SSAServerList) serverTable.getServerList();
        savedSorter.setSortKeys(sortKeys);
        setTableRowSorter(savedSorter);  
        serverTable.sortTable();
        serverTable.updateUI();
        
        this.firePropertyChange("changeServerlist", false, true);
      //  this.firePropertyChange("changeServerTable", false, true);
    }
    
    /**
     * Event listener to trigger a list update when a new server is
     * added to addServerWIndow
     */
    public void propertyChange(PropertyChangeEvent pvt)
    {
        TableRowSorter<DefaultTableModel> savedSorter = getTableRowSorter();
        List<RowSorter.SortKey> sortKeys = (List<SortKey>) savedSorter.getSortKeys();

        if (pvt.getPropertyName().equals("AddNewServer")) {
            SSAPRegResource res = addServerWindow.getResource();
  
            serverTable.addNewServer(res);
        
            // add a special tag for manually added servers
            serverTags.addTag( res.getShortName(), MANUALLY_ADDED_STR  );
            DefaultComboBoxModel comboModel = (DefaultComboBoxModel) tagCombo.getModel();
            if (comboModel.getIndexOf(MANUALLY_ADDED_STR) == -1) 
                tagCombo.addItem(MANUALLY_ADDED_STR);
        } 
        savedSorter.setSortKeys(sortKeys);
        setTableRowSorter(savedSorter);  
        saveAll();
        // and announce the change of serverList 
        this.firePropertyChange("changeServerlist", false, true);      
    }
    
    

    /**
     *  Remove service with row index r
     */
    
    protected void removeService(int row) 
    {
        TableRowSorter<DefaultTableModel> savedSorter = getTableRowSorter();
        serverTags.removeFromTags(serverTable.getModel().getValueAt(row, ServerPopupTable.SHORTNAME_INDEX).toString());
        serverTable.removeServer(row);
        setTableRowSorter(savedSorter); 
        saveAll();
    }
    
    protected void removeAllServices() 
    {
        TableRowSorter<DefaultTableModel> savedSorter = getTableRowSorter();

        int rowCount = serverTable.getModel().getRowCount();
        for (int row=rowCount-1;row>=0;row--) {
            serverTags.removeFromTags(serverTable.getModel().getValueAt(row, ServerPopupTable.SHORTNAME_INDEX).toString());
            serverTable.removeServer(row);
        }
        setTableRowSorter(savedSorter);
        saveAll();
        //serverTable.sort();
    }
    
    /**
     *  Remove selected services.
     */
    protected void removeSelectedServices()
    {       
       
        TableRowSorter<DefaultTableModel> savedSorter = getTableRowSorter();
        int [] selected = serverTable.getSelectedRows();
        int [] rowsSelected = new int [selected.length];
        for ( int i=0; i<selected.length; i++ ) {
            rowsSelected[i] = serverTable.convertRowIndexToModel(selected[i]);
        }
        Arrays.sort(rowsSelected);
        for ( int i=rowsSelected.length-1; i>=0; i-- ) {
            serverTags.removeFromTags((String) serverTable.getModel().getValueAt(rowsSelected[i], ServerPopupTable.SHORTNAME_INDEX));
            serverTable.removeServer(rowsSelected[i]);
        }
        tagCombo.setSelectedIndex(0);
        setTableRowSorter(savedSorter); 
        saveAll();

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
 
    
    //=================== Tags =====================  
         
        protected void selectTaggedServers(String tagname) {
            
           
            if (tagname.isEmpty()) {// no tag selected
               
                return;
            }
           
            boolean firstSelection=true;
            ArrayList<String> servers = serverTags.getList(tagname);
            
            if (servers == null || servers.isEmpty())
                return;
            serverTable.clearSelection();
             for (int i=0;i<serverTable.getRowCount();i++) {
                String shortname = (String) serverTable.getValueAt(i, ServerPopupTable.SHORTNAME_INDEX); //getModel().getValueAt( i, SHORTNAME_INDEX); 
                if (servers.contains(shortname)) {
                    //int row = serverTable.convertRowIndexToView(i);
                    int row = i;
                    //if (row > -1) {
                    if (firstSelection){
                     //   logger.info("index: "+i+"row: "+row);                         
                        serverTable.setRowSelectionInterval(row, row);
                    }
                    else
                        serverTable.addRowSelectionInterval(row, row);
                    firstSelection=false;
                    // }
                }                   
            }
          
        }
        
        
        
        private void addNewTag(int row) {


            String tagname = getTagnameMenu();

            if ( tagname == null )
                return;

            String shortname = (String) serverTable.getShortName(row);

            ArrayList<String> servers=serverTags.getList(tagname);
           
            
            DefaultComboBoxModel comboModel = (DefaultComboBoxModel) tagCombo.getModel();
            if (comboModel.getIndexOf(tagname) == -1) {
                tagCombo.addItem(tagname);
               // tagCombo.setSelectedItem(tagname);
            } 
            tagCombo.setSelectedItem(tagname);
            
            if ( servers == null || !servers.contains(shortname)) {
                serverTags.addTag( shortname, tagname );   
               // serverTable.setServerTags( row, serverTags.getTags(shortname) );
            }


            selectTaggedServers(tagname);
            try {
                serverTags.save();
            } catch (SplatException e) {
                // TODO Auto-generated catch block
               Logger.warn(this, "could not save tags");
            }
            updateUI();
        }
   
        
        /**
         *  Add new tag to the server list - tag selected rows
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
           
            ArrayList<String> servers=serverTags.getList(tagname);
            if (servers == null)
                servers = new ArrayList<String>();
            
            for (int i=0;i<selected.length;i++) {
                int row = serverTable.convertRowIndexToModel(selected[i]);
                String shortname = (String) serverTable.getModel().getValueAt(row, ServerPopupTable.SHORTNAME_INDEX);
                if ( ! servers.contains(shortname)) {
                    serverTags.addTag( shortname, tagname );    
                  //  serverTable.setServerTags( row, serverTags.getTags(shortname) );
                }
            }
          
            DefaultComboBoxModel comboModel = (DefaultComboBoxModel) tagCombo.getModel();
            if (comboModel.getIndexOf(tagname) == -1) {
                tagCombo.addItem(tagname);
                tagCombo.setSelectedItem(tagname);
            }
            selectTaggedServers(tagname);
            updateUI();
            try {
                serverTags.save();
            } catch (SplatException e) {
                // TODO Auto-generated catch block
               Logger.warn(this, "could not save tags");
            }

        }
        
        
        private String getTagnameMenu() {
            
            String tagname="" ;
            JPanel getTagPanel = new JPanel();
            getTagPanel.setLayout(new BorderLayout());
           // boolean newTag = true;
           final JComboBox newTagCombo = new JComboBox();
           final JTextField tagText=new JTextField(15);
           
           for (int i=0;i<tagCombo.getItemCount();i++)
              newTagCombo.addItem(tagCombo.getItemAt(i));
            newTagCombo.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    tagText.setText((String) newTagCombo.getSelectedItem());
                }
            });
      
            getTagPanel.add(new JLabel("Tagname:"),BorderLayout.PAGE_START);
            getTagPanel.add(tagText, BorderLayout.LINE_START);
            getTagPanel.add(newTagCombo, BorderLayout.PAGE_END);
            newTagCombo.setSelectedItem("");
           
            int result = JOptionPane.showConfirmDialog(this, getTagPanel,
                    "Please add/choose a tag", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                tagname=tagText.getText();
            } else {
                return null;
            }
           // String tagname = (String)JOptionPane.showInputDialog(this, "Enter Tagname:\n", "Add tag", JOptionPane.QUESTION_MESSAGE, null,  );  
          //  String tagname = (String)JOptionPane.showInputDialog(this, newTagCombo  );  
            
            if (tagname == null || tagname.isEmpty()) {
                return null;
            }
             return tagname;

        }
        

      
        
        /**
         *  Remove selected tag
         */
        private void removeSelectedTag()
        {
            String tag = getSelectedTag();
            
            if (tag == null || tag.isEmpty() ) {// no tag selected
                JOptionPane.showMessageDialog( this,   "No tags have been selected for removal" ,                
                        "No tags", JOptionPane.ERROR_MESSAGE );
                return;
            }

            tagCombo.removeItem(tag);
            tagCombo.setSelectedIndex(0);
            serverTags.removeTag(tag);   
            try {
                serverTags.save();
            } catch (SplatException e) {
                // TODO Auto-generated catch block
               Logger.warn(this, "could not save tags");
            }

         
        }
        
        protected String getSelectedTag() {
            return (String) tagCombo.getSelectedItem();
        }
        
    //=================== gui components =====================  
    
   
    protected JPanel makeTagPanel() {
        JPanel tagPanel = new JPanel();
        tagPanel.setLayout(new GridBagLayout());
        tagPanel.setBorder(BorderFactory.createTitledBorder( "Tags" ));
        GridBagConstraints gbcTags = new GridBagConstraints();
        //      gbcTags.anchor = GridBagConstraints.LINE_START;
        //      gbcTags.fill = GridBagConstraints.HORIZONTAL;
              gbcTags.gridwidth=3;
              gbcTags.insets=new Insets(0,5,5,0);
               
            //  SelectTagAction selectTagAction = new SelectTagAction("select tagged services");
           
         //     JLabel taglabel = new JLabel("CurrentTag: ");
         //     tagPanel.add(taglabel, BorderLayout.LINE_START);
             

              serverTags.restoreTags();
              String[] tags = serverTags.getTags();
              tagCombo = new JComboBox(); 

              tagCombo.setPrototypeDisplayValue("------------");
              tagCombo.addItem(""); 
              for (int i=0;i<tags.length;i++)
                  tagCombo.addItem(tags[i]);
              
              tagCombo.setSelectedIndex(0);
            //  tagCombo.addActionListener(selectTagAction);
              tagCombo.addItemListener(new ItemListener() {
                  @Override
                  public void itemStateChanged(ItemEvent e) {
                      String selected = (String) tagCombo.getSelectedItem();
                      if ( tagCombo.getSelectedIndex()==0 )
                              serverTable.clearSelection();
                          else {
                              selectTaggedServers(selected);
                          }
                      updateUI();
                  }
              });
              tagPanel.add((tagCombo),gbcTags);
              AddTagAction addTagAction = new AddTagAction( "+" );
              JButton addTagButton = new JButton(addTagAction);
              addTagButton.setToolTipText("Add tag to selected servers");
              tagPanel.add(addTagButton,gbcTags);
              RemoveTagAction removeTagAction = new RemoveTagAction( "-" );
              JButton removeTagButton = new JButton(removeTagAction);    
              removeTagButton.setToolTipText("Remove current tag");
       
              tagPanel.add(removeTagButton,gbcTags);
              return tagPanel;
    }
    
    public static boolean isManuallyAddPossible() {
        return ManuallyAddPossible;
    }

    public static void setManuallyAddPossible(boolean manuallyAddPossible) {
        ManuallyAddPossible = manuallyAddPossible;
    }

    @SuppressWarnings("serial")
    protected class RemoveMenu extends JMenu {
        public RemoveMenu(String title) {
            super(title);
            JMenuItem removeThisService = new JMenuItem("Remove current service");
            JMenuItem removeSelectedServices = new JMenuItem("Remove selected services");
            JMenuItem removeAllServices = new JMenuItem("Remove all services");
            removeThisService.addActionListener(new PopupMenuAction());
            removeSelectedServices.addActionListener(new PopupMenuAction());
            removeAllServices.addActionListener(new PopupMenuAction());
            this.add(removeThisService);
            this.add(removeSelectedServices);
            this.add(removeAllServices);
        }
    }
    
    @SuppressWarnings("serial")
    static class ServerTableRenderer extends DefaultTableCellRenderer {
               
         Component c;
          public ServerTableRenderer() { 
              super();
          }
          public Component getTableCellRendererComponent(ServerPopupTable table, Object value, boolean   isSelected, boolean hasFocus, int row, int column) 
          { 
              c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); 
             
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
    
    //=================== actions =====================  
   
    
    /**
     * Inner class defining action for reading a list of servers.
     */
    @SuppressWarnings("serial")
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
    @SuppressWarnings("serial")
    protected class SaveAction
        extends AbstractAction
    {
        public SaveAction( String name, Icon icon )
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae )
        {
            saveAll();
        }
    }
    
    /**
     * Inner class defining action for selecting all known servers.
     */
    @SuppressWarnings("serial")
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
    @SuppressWarnings("serial")
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
    @SuppressWarnings("serial")
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
     * Inner class defining action for adding a new server to the list
     */
    @SuppressWarnings("serial")
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
            //serverTable.sortTable();
            serverTable.updateUI();
        }
    }
    
    /**
     * Inner class defining action for query registry for new servers.
     */
    @SuppressWarnings("serial")
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
    
    @SuppressWarnings("serial")
    protected class PopupMenuAction extends AbstractAction
    {
        
        public void actionPerformed( ActionEvent e) {
                                  
            int r = serverTable.getPopupRow();
          //  ServerTableRenderer renderer = (ServerTableRenderer) serverTable.getCellRenderer(r, 0);
          //  renderer.repaint();
            
            if (e.getActionCommand().equals("Info")) {
                serverTable.showInfo(r, serviceType, serverTags.getTags(serverTable.getShortName(r)));
            }
            else if (e.getActionCommand().equals("Tag current service")) {
                addNewTag(r);
            }   
            else if (e.getActionCommand().equals("Tag selected services")) {
               //addNewTag(serverTable.getSelectedRows());
                addNewTag();
            }  
            else if (e.getActionCommand().equals("Remove current service")) {
                removeService(r);
                this.firePropertyChange("changeServerlist", false, true);
                
            } 
            else if (e.getActionCommand().equals("Remove selected services")) {
                removeSelectedServices();
                this.firePropertyChange("changeServerlist", false, true);
                
            }  else if (e.getActionCommand().equals("Remove all services")) {
                removeAllServices();
                this.firePropertyChange("changeServerlist", false, true);
            }      
        }

        
    }
    
    /**
     * 
     *
    @SuppressWarnings("serial")
    protected class SelectTagAction
    extends AbstractAction
    {
        public SelectTagAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            if (ae.getSource()==tagCombo) {
                JComboBox tagscombo = (JComboBox) ae.getSource();
                String selected = (String) tagscombo.getSelectedItem();
                if ( tagscombo.getSelectedIndex()==0 )
                    serverTable.clearSelection();
                else {
                    selectTaggedServers(selected);
                }
                updateUI();
            }
        }
    }
*/
    /**
     * Inner class defining action for removing a new tag 
     */
    @SuppressWarnings("serial")
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
     * Inner class defining action for adding a new tag 
     */
    @SuppressWarnings("serial")
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
                  serverTags.save();
            } catch (SplatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
               
            }          
        }
    }
    
 
    //=================== listeners =====================
    class SelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent lse) {
           
            if ( !lse.getValueIsAdjusting() )
                if (lse.getSource() == serverTable.getSelectionModel()) {
                    firePropertyChange("selectionChanged", false, true );                   
                }
        }
        
    }
   

    
    private class resizeListener extends ComponentAdapter {
        public void componentResized(ComponentEvent e) {
            updateUI();
        }
    }

    private class ServerTableMouseListener extends MouseAdapter {

        public void mousePressed( MouseEvent e ) {
            if ( e.getSource()==serverTable)
                if ( SwingUtilities.isRightMouseButton( e ) )
                {
                    serverTable.repaint();
                }
                else {
                    tagCombo.setSelectedIndex(0);
                }
            }
        }

    }
