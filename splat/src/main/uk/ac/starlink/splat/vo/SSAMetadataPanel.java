/*
 * Copyright (C) 2001-2005 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     23-FEB-2012 (Margarida Castro Neves mcneves@ari.uni-heidelberg.de)
 *        Original version.
 */
package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Class SSAMetadataPanel
 * 
 * This class supports displaying metadata parameters that can be used for a SSA query, 
 * selecting parameters and modifying their values.
 *
 * @author Margarida Castro Neves
 * @version $Id: SSAMetadataPanel.java 
 */

public class SSAMetadataPanel extends JPanel implements ActionListener, TableModelListener
{
 
    /**
     *  Panel for the central region.
     */
    protected JPanel centrePanel = new JPanel();

    /**
     * File chooser for storing and restoring server lists.
     */
    protected BasicFileChooser fileChooser = null;
    private static String configFile = "SSAPMetaParams.txt";
    // used to trigger a new server metadata query by SSAQueryBrowser

  //  private PropertyChangeSupport ////;

    private static JTable metadataTable;

    private static MetadataTableModel metadataTableModel;
  

    /** The list of all input parameters read from the servers as a hash map */
    private static HashMap<String, MetadataInputParameter> metaParam=null;

    // the metadata table
    private static final int NRCOLS = 6;					// the number of columns in the table
    // the table indexes

    private static final int SELECTED_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int VALUE_INDEX = 2;    
    private static final int UCD_INDEX = 3;
    private static final int DESCRIPTION_INDEX = 4;
    private static final int SERVERS_INDEX = 5;

    

    // total number of servers that returned parameters
    // private int nrServers;
    // the table headers
    private String[] headers;
    private String[] headersToolTips;

   
    // cell renderer for the parameter name column
    private ParamCellRenderer paramRenderer=null;


    /**
     * Constructor: 
     */
    //public SSAMetadataFrame( HashMap<String, MetadataInputParameter> metaParam , int nrServers)
    public SSAMetadataPanel( HashMap<String, MetadataInputParameter> metaParam )
    {

        this.metaParam = metaParam;
        
      
        initMetadataTable();
        initUI();
        initMenus();
       // initFrame();
   //     queryMetadata = new PropertyChangeSupport(this);

    } 

    /**
     * Constructor: creates an empty table
     */

    public SSAMetadataPanel( )
    {

        metaParam = new HashMap<String, MetadataInputParameter>();
        // nrServers = 0;
       
  //      queryMetadata = new PropertyChangeSupport(this);
        initMetadataTable();
        initUI();
        initMenus();
 
    } //

    
    public HashMap<String, MetadataInputParameter> getParams() {
        return metaParam;
    }
    
    /**
     * Initialize the metadata table
     */
    public void initMetadataTable()
    {

        metadataTable = new JTable( );    
        metadataTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        
        // the table headers
        headers = new String[NRCOLS];

        headers[SELECTED_INDEX] = "Use";     
        headers[NAME_INDEX] = "Name";
        headers[VALUE_INDEX] = "Value";
        headers[DESCRIPTION_INDEX] = "Description";
        headers[UCD_INDEX] = "UCD";
        headers[SERVERS_INDEX] = "Supported by";
        // the tooltip Texts for the headers
        headersToolTips = new String[NRCOLS];

        headersToolTips[SELECTED_INDEX] = "Select for Query";     
        headersToolTips[NAME_INDEX] = "Parameter name";
        headersToolTips[VALUE_INDEX] = "Parameter value";
        headersToolTips[DESCRIPTION_INDEX] = "Description";
        headersToolTips[UCD_INDEX] = "UCD";
        headersToolTips[SERVERS_INDEX] = "Servers supporting this parameter";

        //  Table of metadata parameters goes into a scrollpane in the center of
        //  window (along with a set of buttons, see initUI).
        // set the model and change the appearance
        // the table data
        
        metadataTableModel = new MetadataTableModel(headers);
       
        metadataTable.setModel( metadataTableModel );
        metadataTableModel.addTableModelListener(this);
        
        if (metaParam != null)
        {
            addRows();        
        } 
        
         
        metadataTable.setShowGrid(true);
        metadataTable.setGridColor(Color.lightGray);
        metadataTable.getTableHeader().setReorderingAllowed(false);
      
        paramRenderer = new ParamCellRenderer(); // set cell renderer for description column

        adjustColumns();

    }

    
    /**
     * updateMetadata( HashMap<String, MetadataInputParameter> metaParam )
     * updates the metadata table information after a "refresh"
     * 
     * @param metaParam
     */
    public void updateMetadata(HashMap<String, MetadataInputParameter> metaParam ) 
    {
    
        metadataTableModel = new MetadataTableModel(headers);
        metadataTable.setModel(metadataTableModel );
        adjustColumns();   
        // ???fireProperty();
    }


    /**
     * Transform the metadata Hash into a two-dimensional String array (an array of rows).
     * The rows will be sorted by number of servers supporting the parameter
     *
     * @return the metadata array
     */
  /*  public String[][] getParamList()
    {

        // Iterate through metaParam, add the entries, populate the table

        Collection<MetadataInputParameter> mp = metaParam.values();
        String[][] metadataList = new String[mp.size()][NRCOLS];


        Iterator<MetadataInputParameter> it = mp.iterator();

        int row=0;
        while (it.hasNext()) {
            MetadataInputParameter mip = it.next();

            metadataList[row][NR_SERVERS_INDEX] = Integer.toString(mip.getCounter());//+"/"+Integer.toString(nrServers);       // nr supporting servers
            metadataList[row][NAME_INDEX] = mip.getName().replace("INPUT:", "");                // name
            metadataList[row][VALUE_INDEX] =  ""; //mip.getValue();  
            metadataList[row][UCD_INDEX] =  mip.getUCD(); 
            metadataList[row][SERVERS_INDEX] =  mip.getServers().toString(); 
            String unit = mip.getUnit();
            String desc = mip.getDescription();
            desc = desc.replaceAll("\n+", "<br>");
            desc = desc.replaceAll("\t+", " ");
            desc = desc.replaceAll("\\s+", " ");

            if (unit != null && unit.length() >0) {
                desc = desc + "  ("+unit+")"; // description (unit)
            }
            metadataList[row][DESCRIPTION_INDEX] = desc; // description
            row++;

        } // while 

        Arrays.sort(metadataList, new SupportedComparator());
        return metadataList;
    }
*/
    /**
     * comparator to sort the parameter list by the nr of servers that support a parameter
     */
 //   class SupportedComparator  implements Comparator<String[]>
 //   { 
 //       public int compare(String[] object1, String[] object2) {
 //           // compare the frequency counter
 //           return ( object2[NR_SERVERS_INDEX]) - Integer.parseInt(object1[NR_SERVERS_INDEX]) );
  //      }
 //   }
    
    public void refreshParams() {
        adjustColumns();
        this.updateUI();
    }


    /**
     * adjust column sizes, renderers and editors
     */
    private void adjustColumns() {
        JCheckBox  cb = new JCheckBox();
        JTextField  tf = new JTextField();

       
        metadataTable.getColumn(metadataTable.getColumnName(SELECTED_INDEX)).setCellEditor(new DefaultCellEditor(cb));
        metadataTable.getColumn(metadataTable.getColumnName(SELECTED_INDEX)).setMaxWidth(30);
        metadataTable.getColumn(metadataTable.getColumnName(SELECTED_INDEX)).setMinWidth(30);      
        metadataTable.getColumn(metadataTable.getColumnName(NAME_INDEX)).setMinWidth(100);
        metadataTable.getColumn(metadataTable.getColumnName(VALUE_INDEX)).setMinWidth(100);
        metadataTable.getColumn(metadataTable.getColumnName(VALUE_INDEX)).setCellEditor(new DefaultCellEditor(tf));
        metadataTable.getColumn(metadataTable.getColumnName(NAME_INDEX)).setCellRenderer(paramRenderer);
        metadataTable.getColumn(metadataTable.getColumnName(UCD_INDEX)).setMinWidth(100);
        
        if ( metadataTable.getColumnCount() > 4) { // remove columns that are not visible, it not removed yet
            //metadataTable.getColumn(metadataTable.getColumnName(NR_SERVERS_INDEX)).setMaxWidth(0);
           //metadataTable.getColumn(metadataTable.getColumnName(DESCRIPTION_INDEX)).setMaxWidth(0);
           //metadataTable.getColumn(metadataTable.getColumnName(SERVERS_INDEX)).setMaxWidth(0);

            // Remove the Servers column. Its contents will not be removed.  
            metadataTable.removeColumn(metadataTable.getColumn(metadataTable.getColumnName(SERVERS_INDEX)));
            // Remove the description column. Its contents will not be removed. They'll be displayed as tooltip text in the NAME_INDEX column.  
            metadataTable.removeColumn(metadataTable.getColumn(metadataTable.getColumnName(DESCRIPTION_INDEX)));
           
        }

    }


  //  public void setParams( HashMap<String, MetadataInputParameter> mp) {
 //       this.metaParam = mp;
//    }
    /**
     * Retrieve parameter names and values from table and returns a query substring 
     * Only the parameters on selected rows, and having non-empty values will be added to the table 
     */
    public String getParamsQueryString() 
    {
        String query="";
        HashMap  <String, String> UCDList = new HashMap <String, String> () ;
        // iterate through all rows
        // String suffix = "";
        for (int i=0; i< metadataTable.getRowCount(); i++) 
        {
            if (rowChecked( i )) 
            {
                String val = getTableData(i,VALUE_INDEX).toString().trim();
                String ucd = (String) getTableData(i,UCD_INDEX);
                String name = (String) getTableData(i,NAME_INDEX);

                /* if (ucd.endsWith("max") )
                    suffix = "max";
                else if (ucd.endsWith("min") )
                    suffix = "min";
                 */
                if (val != null && val.length() > 0) { // all non-empty values
                    if ( ucd != null && ! ucd.isEmpty())   // if there are no UCDs assigned just add the parameters to the  query string
                        UCDList.put(ucd, val);
                    query += "&"+name+"="+val;
                } 
            }
        }
            /*
        // set the same value and add to the query all params with the same UCD as the selected parameters
        for (int i=0; i< metadataTable.getRowCount(); i++) 
        {
            String suffix2 = "";
            String ucd = (String) getTableData(i,UCD_INDEX);
            if ( ucd != null && !ucd.isEmpty() && UCDList.containsKey(ucd)) 
            {
               
                if (ucd.endsWith("max") )
                    suffix2 = "max";
                else if (ucd.endsWith("min") )
                    suffix2 = "min";
                if ( suffix.isEmpty() || suffix.equals(suffix2) ) 
                {
                  String name = getTableData(i,NAME_INDEX).toString().trim();
                   query += "&"+name+"="+UCDList.get(ucd);
                }
            } 
        }
        */
        return query;	
    }


    /**
     *  retrieves table content at cell position row, col 
     * 
     * @param row
     * @param col
     * @return
     */
    private Object getTableData(int row, int col ) 
    {
        return metadataTable.getModel().getValueAt(row, col);
    }

    /**
     *  retrieves the state of the checkbox row (parameter selected)
     * 
     * @param row
     * @return  - true if it is checked, false if unchecked
     */
    private boolean rowChecked(int row) 
    {
        Boolean val = (Boolean) metadataTable.getModel().getValueAt(row, SELECTED_INDEX);
        if (Boolean.TRUE.equals(val)) 
            return true;
        else return false;
    }


    /**
     *  changes  content to newValue at cell position row, col 
     * 
     * @param newValue
     * @param row
     * @param col
     */
    private void setTableData(String newValue, int row, int col )
    {
        metadataTable.getModel().setValueAt(newValue, row, col);
    }

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {
        this.setPreferredSize(new Dimension(500,150));
        setLayout( new BorderLayout() );
       
        JScrollPane scroller = new JScrollPane( metadataTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
       // metadataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

       // centrePanel.setSize(centrePanel.getWidth(), 50);
        centrePanel.setLayout( new BorderLayout() );
        centrePanel.add( scroller, BorderLayout.CENTER );
        add( centrePanel, BorderLayout.CENTER );
      
        //centrePanel.setSize(centrePanel.getWidth(), centrePanel.getHeight()-20);
      
       // centrePanel.setBorder( BorderFactory.createTitledBorder( "Optional SSAP Parameters" ) );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
   // protected void initFrame()
//    {
     //   setSize( new Dimension( 425, 500 ) );
//        setVisible( true );
 //   }

    /**
     * Initialise the menu bar, action bar and related actions.
     */
    protected void initMenus()
    {
        
        // get the icons
        
        //  Get icons.
        ImageIcon closeImage = new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon saveImage = new ImageIcon( ImageHolder.class.getResource( "savefile.gif" ) );
        ImageIcon readImage = new ImageIcon( ImageHolder.class.getResource( "openfile.gif" ) );
       // ImageIcon helpImage = new ImageIcon( ImageHolder.class.getResource( "help.gif" ) );
        ImageIcon updateImage = new ImageIcon( ImageHolder.class.getResource("ssapservers.gif") );
        ImageIcon resetImage = new ImageIcon( ImageHolder.class.getResource("reset.gif") );

        // The Menu bar
        
        //  Add the menuBar.
 /*       JMenuBar menuBar = new JMenuBar();
        setJMenuBar( menuBar );
        //  Create the File menu.
        JMenu fileMenu = new JMenu( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );
        
        JMenuItem saveFile = new JMenuItem("(S)ave Param List to File", saveImage);
        saveFile.setMnemonic( KeyEvent.VK_S );
        saveFile.addActionListener(this);
        saveFile.setActionCommand( "save" );
        fileMenu.add(saveFile);
        JMenuItem readFile = new JMenuItem("Read Param List from (F)ile", readImage);
        fileMenu.add(readFile);
        readFile.setMnemonic( KeyEvent.VK_F );
        readFile.addActionListener(this);
        readFile.setActionCommand( "restore" );
        JMenuItem loadFile = new JMenuItem("(U)pdate Params from services", updateImage);
        fileMenu.add(loadFile);
        loadFile.setMnemonic( KeyEvent.VK_U );
        loadFile.addActionListener(this);
        loadFile.setActionCommand( "load" );
        
        JMenuItem resetFile = new JMenuItem("(R)eset all values", resetImage);
        fileMenu.add(loadFile);
        resetFile.setMnemonic( KeyEvent.VK_R );
        resetFile.addActionListener(this);
        resetFile.setActionCommand( "reset" );

        //  Create the Help menu.
        HelpFrame.createButtonHelpMenu( "ssa-metadata", "Help on window", menuBar, null );
 */      
        // The Buttons bar
        // the action buttons
       
        JPanel   buttonsPanel = new JPanel( new GridLayout(1,5) );
      //  buttonsPanel.setPreferredSize(new Dimension(500, 20));

        //  Add action to save the parameter list into a file
        JButton saveButton = new JButton( "Save" , saveImage ); 
        saveButton.setActionCommand( "save" );
        saveButton.setToolTipText( "Save parameter list to a file" );
        saveButton.addActionListener( this );
        buttonsPanel.add( saveButton );

        //  Add action to save the parameter list into a file
        JButton restoreButton = new JButton( "Read" , readImage); 
        restoreButton.setActionCommand( "restore" );
        restoreButton.setToolTipText( "Restore parameter list from a file" );
        restoreButton.addActionListener( this );
        buttonsPanel.add( restoreButton  );

        // Add action to query the servers for parameters 
        JButton queryButton = new JButton( "Update" , updateImage); 
        queryButton.setActionCommand( "refresh" );
        queryButton.setToolTipText( "Query the servers for a current list of parameters" );
        queryButton.addActionListener( this );
        buttonsPanel.add( queryButton );

        //  Add action to do reset the form
        JButton resetButton = new JButton( "Reset", resetImage );
        resetButton.setActionCommand( "reset" );
        resetButton.setToolTipText( "Clear all fields" );
        resetButton.addActionListener( this );
        buttonsPanel.add( resetButton  );

     /*   //  Add an action to close the window.
        JButton closeButton = new JButton( "Close", closeImage );
        //centrePanel.add( closeButton );
        closeButton.addActionListener( this );
        closeButton.setActionCommand( "close" );
        closeButton.setToolTipText( "Close window" );
        buttonsPanel.add( closeButton);
        */
     //   centrePanel.add( buttonsPanel, BorderLayout.SOUTH);

    } // initMenus


  
    /**
     *  action performed
     *  process the actions when a button is clicked
     */
    public void actionPerformed(ActionEvent e) {

        Object command = e.getActionCommand();
        
        if (metadataTable.isEditing())
            metadataTable.getCellEditor().stopCellEditing();

        if ( command.equals( "save" ) ) // save table values to a file
        {
            saveMetadataToFile();
        }
        if ( command.equals( "load" ) ) // read saved table values from a file
        {
           
            readMetadataFromFile();
            fireProperty();
        }
 //       if ( command.equals( "refresh" ) ) // add new server to list
 //       {
 //           queryMetadata.firePropertyChange("refresh", false, true);
//        }
        if ( command.equals( "reset" ) ) // reset text fields
        {
            resetFields();
        }
        if ( command.equals( "close" ) ) // close window
        {
            closeWindow();
        }

    } // actionPerformed


    /**
     *  Close (hide) the window.
     */
    private void closeWindow()
    {
        this.setVisible( false );
    }

    /**
     *  Open (show) the window.
     */
    public void openWindow()
    {
        this.setVisible( true );  
    }

    /**
     *  Select all Parameters from table
     */
    public void selectAll()
    {
       
        MetadataTableModel mtm = (MetadataTableModel) metadataTable.getModel(); 
        for (int i=0; i<mtm.getRowCount(); i++) {
            //deselect it first to remove from query string
            mtm.setValueAt(Boolean.TRUE, i, SELECTED_INDEX);
            
        }
        fireProperty();
    }
    /**
     *  Deselect all Parameters from table
     */
    public void deselectAll()
    {
        
        MetadataTableModel mtm = (MetadataTableModel) metadataTable.getModel(); 
        for (int i=0; i<mtm.getRowCount(); i++) {
            //deselect it first to remove from query string
            mtm.setValueAt(Boolean.FALSE, i, SELECTED_INDEX);
        }
        fireProperty();
    }
    /**
     *  Reset all fields
     */
    private void resetFields() 
    {
        for (int i=0; i< metadataTable.getRowCount(); i++) {
            String val = getTableData(i,VALUE_INDEX).toString().trim();
            if (val != null && val.length() > 0) {
                setTableData("", i,VALUE_INDEX);
            }
        }
        fireProperty();
    } //resetFields

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
    } //initFileChooser


    /**
     *  Restore  metadata that has been previously written to a
     *  CSV file. The file name is obtained interactively.
     */
    public void readMetadataFromFile(File file) {
         try {
            readParams(file);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void readMetadataFromFile()
    {
        initFileChooser();
        int result = fileChooser.showOpenDialog( this );
        if ( result == JFileChooser.APPROVE_OPTION ) 
        {
            File file = fileChooser.getSelectedFile();
            try {
                readTable( file );
            }
            catch (Exception e) {
                ErrorDialog.showError( this, e );
            }
        }
    } // readMetadataFromFile

    /**
     *  Interactively gets a file name and save current metadata table  to it in CSV format
     *  a VOTable.
     */
    public void saveMetadataToFile()
    {
        if ( metadataTable == null || metadataTable.getRowCount() == 0 ) {
            JOptionPane.showMessageDialog( this,
                    "There are no parameters to save",
                    "No parameters", JOptionPane.ERROR_MESSAGE );
            return;
        }

        initFileChooser();
        int result = fileChooser.showSaveDialog( this );
        if ( result == JFileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            try {
                saveParams( file );
            }
            catch (Exception e) {
                ErrorDialog.showError( this, e );
            }
        }
    } // saveMetadataToFile

    /**
     * saveTable(file)
     * saves the metadata table to a file in csv format 
     * 
     * @param paramFile     - file where to save the table 
     * @throws IOException
     */
 /*   private void saveTable(File paramFile) throws IOException
    {

        
        BufferedWriter tableWriter = new BufferedWriter(new FileWriter(paramFile));

        MetadataTableModel mtm = (MetadataTableModel) metadataTable.getModel();
        for ( int row=0; row< mtm.getRowCount(); row++) {
            tableWriter.append(mtm.getValueAt(row, NR_SERVERS_INDEX).toString());
            tableWriter.append(';');
            tableWriter.append(mtm.getValueAt(row, NAME_INDEX).toString());
            tableWriter.append(';');
            tableWriter.append(mtm.getValueAt(row, VALUE_INDEX).toString());
            tableWriter.append(';');
            tableWriter.append(mtm.getValueAt(row, DESCRIPTION_INDEX).toString());
         //   tableWriter.append(';');
         //   tableWriter.append(mtm.getServers().toString());
            tableWriter.append('\n');
        }
        tableWriter.flush();
        tableWriter.close();

    } //saveTable()
    */
    public void backupParams() throws IOException {
        File paramsFile = Utilities.getConfigFile( configFile );
        saveParams(paramsFile);
    } 
    /**
     * saveParams(file)
     * saves the metadata table to a file in csv format 
     * 
     * @param paramFile     - file where to save the table 
     * @throws IOException
     */
    private void saveParams(File paramFile) throws IOException
    {

    //    Collection<MetadataInputParameter> mp = metaParam.values();
      

      //  Iterator<MetadataInputParameter> it = mp.iterator();
        FileOutputStream out = new FileOutputStream(paramFile);
        OutputStream buffer = new BufferedOutputStream(out);
        ObjectOutputStream paramWriter = new ObjectOutputStream(buffer);

        
     //   while (it.hasNext()) {
      //      MetadataInputParameter mip = it.next();
            paramWriter.writeObject(metaParam);
      
     //   }
        
        paramWriter.flush();
        paramWriter.close();

    } //saveTable()
    
    public void restoreParams() throws IOException, ClassNotFoundException {
        File paramsFile = Utilities.getConfigFile( configFile );
        readParams(paramsFile);
    }
    private void readParams(File paramFile) throws IOException, ClassNotFoundException
    {
   
        metaParam = new HashMap<String, MetadataInputParameter>();
   
        
        FileInputStream in = new FileInputStream(paramFile);
        InputStream buffer = new BufferedInputStream(in);
        ObjectInputStream paramReader = new ObjectInputStream(buffer);

        
       // while (paramReader != null) {
           // MetadataInputParameter mip ;        
              //  mip = (MetadataInputParameter) paramReader.readObject();
                metaParam = (HashMap<String, MetadataInputParameter>) paramReader.readObject();
               // metaParam.put( mip.getName(), mip );
              //  addRow(mip, newmodel);     
       // }
         
        paramReader.close();
        addRows();
     
        //adjustColumns(); // adjust column sizes in the new model

    } //readParams()
    
    /**
     * readTable( File paramFile)
     * reads the metadata table previously saved with saveTable() from a file in csv format 
     * 
     * @param paramFile - the csv file to be read
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void readTable(File paramFile) throws  IOException, FileNotFoundException
    {

        metaParam = new HashMap<String, MetadataInputParameter>();
        MetadataTableModel newmodel = new MetadataTableModel(headers);
        BufferedReader CSVFile = new BufferedReader(new FileReader(paramFile)); 	

        String tableRow = CSVFile.readLine();  	
        while (tableRow != null) {
            String [] paramRow = new String [NRCOLS];
            paramRow = tableRow.split(";", NRCOLS);	 		
            newmodel.addRow(paramRow);
            tableRow = CSVFile.readLine(); 
         //   MetadataInputParameter mip = new MetadataInputParameter();
        }
        // Close the file once all data has been read.
        CSVFile.close();
        // set the new model
        metadataTable.setModel(newmodel);
        adjustColumns(); // adjust column sizes in the new model

    } //readTable()


    /**
     * Remove all rows from the table 
     * @param mip the metadata parameter that will be added to the table
     */
    
    public void removeAll( ) {
        metaParam.clear();
        MetadataTableModel mtm = (MetadataTableModel) metadataTable.getModel();
        int rowCount = mtm.getRowCount();
        // empty the table
        for (int i = rowCount - 1; i >= 0; i--) {
            mtm.removeRow(i);
        }       
    }
    
    public static synchronized void addParam( MetadataInputParameter mip ) {
        String paramname = mip.getName().replace("INPUT:", "");
        if (metaParam.containsKey(mip.getName())) {
           MetadataInputParameter mmip = metaParam.get(mip.getName()) ;
        // if two servers support same parameter with different initial values, don't write any value.
           if (! mmip.getValue().equals(mip.getValue()))
               mmip.setValue(""); 
        // if one entry has an UCD, and the other not, keep the UCD
           if ( mmip.getUCD() == null || mmip.getUCD().isEmpty())   
               mmip.setUCD(mip.getUCD());
        // if one entry has a description, and the other not, keep the (first) non empty
           if (mmip.getDescription() == null || mmip.getDescription().isEmpty())
           mmip.setDescription(mip.getDescription());
           metaParam.put(mip.getName(), mmip);
           replaceRow(mip);
           // eventually reload the row - right now the servers are not displayed
        } else {
            metaParam.put(mip.getName(), mip);
            addRow(mip);
        }       
    }
    
    public synchronized void addParams( ArrayList<MetadataInputParameter> mips ) {
        if (mips==null)
            return;
       for (int i=0;i<mips.size();i++)
           addParam(mips.get(i));
    }
    
    /**
     * create new table from metaParams
     * @param mip the metadata parameter that will be added to the table
     */
    
    public void addRows() {
        removeAll();
        MetadataTableModel mtm = (MetadataTableModel) metadataTable.getModel();
       
        Collection<MetadataInputParameter> mp = metaParam.values();
        Iterator<MetadataInputParameter> it = mp.iterator();
        
        while (it.hasNext()) {
           MetadataInputParameter mip = it.next();         
           addRow ( mip, mtm, mip.isChecked() );
        }
    }
    
    
    /**
     * Replaces a row to the table 
     * @param mip the metadata parameter that will be added to the table
     */
    public static void replaceRow(MetadataInputParameter mip ) {
        MetadataTableModel mtm = (MetadataTableModel) metadataTable.getModel();
        boolean found = false;
       
        String paramName = mip.getName().replace("INPUT:", "");
        for (int row=0; (row<mtm.getRowCount() && !found);row++) {
            String name = (String) mtm.getValueAt(row, NAME_INDEX);
            if (name.equals(paramName)) {
                found=true;
                mtm.removeRow(row);
                addRow ( mip, mtm, mip.isChecked() );
            }
        }
      
    }
    /**
     * Adds a new row to the table 
     * @param mip the metadata parameter that will be added to the table
     */
    // checkbox included 
    public static void addRow(MetadataInputParameter mip, MetadataTableModel mtm ) {
        addRow ( mip, mtm, mip.isChecked() );
    }
    
    public static void addRow(MetadataInputParameter mip ) {
        MetadataTableModel mtm = (MetadataTableModel) metadataTable.getModel();
        addRow ( mip, mtm, mip.isChecked() );
    }
    
    public static void addRow(MetadataInputParameter mip, boolean selected ) {
        MetadataTableModel mtm = (MetadataTableModel) metadataTable.getModel();
        addRow ( mip, mtm, selected );
    }
    
    // useCheckBox true: checkbox included
    // useCheckBox false: checkbox not included, parameter automatically "checked"
    public static void addRow (MetadataInputParameter mip,  MetadataTableModel mtm, boolean selected) {

        String [] paramRow = new String [NRCOLS];
        paramRow[NAME_INDEX] = mip.getName().replace("INPUT:", "");                // name
        paramRow[VALUE_INDEX] =  mip.getValue();                         // value (default value or "")
        paramRow[UCD_INDEX] =  mip.getUCD();  
        String unit = mip.getUnit();
        String desc = mip.getDescription();
        //String utype = mip.getUtype();
        
        if (desc != null) 
        {
            // remove newline, tabs, and multiple spaces
            desc = desc.replaceAll("\n+", " ");
            desc = desc.replaceAll("\t+", " ");
            desc = desc.replaceAll("\\s+", " ");

            // insert linebreaks for multi-lined tooltip
            int linelength=100;
            int sum=0;
            String [] words = desc.split( " " );
            desc="";
            for (int i=0; i<words.length; i++ ) {
                sum+=words[i].length()+1;
                if (sum > linelength) {
                    desc+="<br>"+words[i]+" ";
                    sum=words[i].length();
                } else {
                    desc+=words[i]+" ";
                }
            }
        }

        if (unit != null && unit.length() >0) {
            desc = desc + "  ("+unit+")"; //  (unit)
        }
        paramRow[DESCRIPTION_INDEX] = desc; // description
        mtm.addRow( paramRow );

        if ( selected ) {         
            mtm.setValueAt(true, mtm.getRowCount()-1, SELECTED_INDEX);
        }
   
    } //addRow
    



    /** 
     * TableModelListener method
     *
     */
    public void tableChanged(TableModelEvent tme) {
      
        if ( (tme.getColumn() != TableModelEvent.ALL_COLUMNS) && (tme.getColumn() == SELECTED_INDEX  || tme.getColumn() == VALUE_INDEX) ) {
                        
             MetadataTableModel mtm = (MetadataTableModel) tme.getSource();
             String paramname = (String) mtm.getValueAt(tme.getFirstRow(), NAME_INDEX);
             String value = mtm.getValueAt(tme.getFirstRow(), VALUE_INDEX).toString();
             MetadataInputParameter param = metaParam.get("INPUT:"+paramname);
             param.setChecked(rowChecked(tme.getFirstRow()));
             if (value != null && !value.isEmpty()) {

                 param.setValue(value);
                 this.firePropertyChange("changedValue", null, param );
                 //    this.firePropertyChange("changeQuery", false, true);      
             }
        }
    }
    // fire this only in certain events
    public void fireProperty() 
    {
        this.firePropertyChange("changeQuery", false, true);
    }


    /** 
     * the cell renderer for the parameter name column ( show description as toolTipText )
     *
     */
    class ParamCellRenderer  extends JLabel implements TableCellRenderer 
    {  
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,  boolean hasFocus, int row,  int column)
        {   
           
            setText(value.toString());
            setToolTipText("<html><p>"+table.getModel().getValueAt(row, DESCRIPTION_INDEX).toString()+"</p></html>");
            if (isSelected)
                setBackground(table.getSelectionBackground());
            return this;
        } 
    }  //ParamCellRenderer


    /** 
     * MetadataTableModel
     * defines the model of the metadata table
     */
    class MetadataTableModel extends DefaultTableModel 
    {
        // creates a metadataTableModel with headers and data
        public MetadataTableModel( String [][] data, String [] headers )  {
            super(data, headers);
        }
        // creates a metadataTableModel with headers and no data rows
        public MetadataTableModel( String [] headers )  {
            super(headers, 0);
        }
        @Override
        public boolean isCellEditable(int row, int column) {
            return (column == VALUE_INDEX || column == SELECTED_INDEX ); // the Values column is editable
        }

        @Override
        public Class getColumnClass(int column) {
            if (column == SELECTED_INDEX )
                return Boolean.class;
            return String.class;
        }
      
    } //MetadataTableModel


} //SSAMetadataFrame
