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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import uk.ac.starlink.splat.vo.SSAQueryBrowser.MetadataInputParameter;
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


    private static JTable metadataTable;

    private static MetadataTableModel metadataTableModel;
  

    /** The list of all input parameters read from the servers as a hash map */
    private HashMap<String, MetadataInputParameter> metaParam=null;

    // the metadata table
    private static final int NRCOLS = 6;					// the number of columns in the table
    // the table indexes

    private static final int SELECTED_INDEX = 0;
    private static final int NR_SERVERS_INDEX = 1;
    private static final int NAME_INDEX = 2;
    private static final int VALUE_INDEX = 3;    
    private static final int DESCRIPTION_INDEX = 4;
    private static final int UCD_INDEX = 5;

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
        //this.nrServers = nrServers;
      
        initMetadataTable();
        initUI();
        initMenus();

    } 

    /**
     * Constructor: creates an empty table
     */

    public SSAMetadataPanel( )
    {

        metaParam = null;
        // nrServers = 0;
       
        initMetadataTable();
        initUI();
        initMenus();
 
    } //

    /**
     * Initialize the metadata table
     */
    public void initMetadataTable()
    {

        metadataTable = new JTable( );    
        // the table headers
        headers = new String[NRCOLS];

        headers[SELECTED_INDEX] = "Use";     
        headers[NR_SERVERS_INDEX] = "Nr servers";
        headers[NAME_INDEX] = "Name";
        headers[VALUE_INDEX] = "Value";
        headers[DESCRIPTION_INDEX] = "Description";
        headers[UCD_INDEX] = "UCD";
        // the tooltip Texts for the headers
        headersToolTips = new String[NRCOLS];

        headersToolTips[SELECTED_INDEX] = "Select for Query";     
        headersToolTips[NR_SERVERS_INDEX] = "Nr servers supporting this parameter";
        headersToolTips[NAME_INDEX] = "Parameter name";
        headersToolTips[VALUE_INDEX] = "Parameter value";
        headersToolTips[DESCRIPTION_INDEX] = "Description";
        headersToolTips[UCD_INDEX] = "UCD";

        //  Table of metadata parameters goes into a scrollpane in the center of
        //  window (along with a set of buttons, see initUI).
        // set the model and change the appearance
        // the table data
        if (metaParam != null)
        {
            String[][]  paramList = getParamList();
            metadataTableModel = new MetadataTableModel(paramList, headers);         
        } else 
            metadataTableModel = new MetadataTableModel(headers);
        
        metadataTableModel.addTableModelListener(this);
      
        metadataTable.setModel( metadataTableModel );
        
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
    public String[][] getParamList()
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

    /**
     * comparator to sort the parameter list by the nr of servers that support a parameter
     */
    class SupportedComparator  implements Comparator<String[]>
    { 
        public int compare(String[] object1, String[] object2) {
            // compare the frequency counter
            return ( Integer.parseInt(object2[NR_SERVERS_INDEX]) - Integer.parseInt(object1[NR_SERVERS_INDEX]) );
        }
    }


    /**
     * adjust column sizes, renderers and editors
     */
    private void adjustColumns() {
        JCheckBox  cb = new JCheckBox();
        JTextField  tf = new JTextField();

        metadataTable.getColumnModel().getColumn(SELECTED_INDEX).setCellEditor(new DefaultCellEditor(cb));
        metadataTable.getColumnModel().getColumn(SELECTED_INDEX).setMaxWidth(30);
        metadataTable.getColumnModel().getColumn(SELECTED_INDEX).setMinWidth(30);
        metadataTable.getColumnModel().getColumn(NR_SERVERS_INDEX).setMaxWidth(0);
        metadataTable.getColumnModel().getColumn(NAME_INDEX).setMinWidth(100);
        metadataTable.getColumnModel().getColumn(VALUE_INDEX).setMinWidth(100);
        metadataTable.getColumnModel().getColumn(VALUE_INDEX).setCellEditor(new DefaultCellEditor(tf));
        metadataTable.getColumnModel().getColumn(NAME_INDEX).setCellRenderer(paramRenderer);
        metadataTable.getColumnModel().getColumn(UCD_INDEX).setMinWidth(100);
        metadataTable.getColumnModel().getColumn (DESCRIPTION_INDEX).setMaxWidth(0);
        // Remove the description column. Its contents will not be removed. They'll be displayed as tooltip text in the NAME_INDEX column.  
        metadataTable.removeColumn(metadataTable.getColumnModel().getColumn (DESCRIPTION_INDEX));
        // Remove the Nr Servers column. Its contents will not be removed.  
        metadataTable.removeColumn(metadataTable.getColumnModel().getColumn (NR_SERVERS_INDEX));
    }


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
        JMenuItem loadFile = new JMenuItem("(U)pdate Params from servers", updateImage);
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

        //  Add action to query the servers for parameters 
//        JButton queryButton = new JButton( "Update" , updateImage); 
//        queryButton.setActionCommand( "refresh" );
//        queryButton.setToolTipText( "Query the servers for a current list of parameters" );
//        queryButton.addActionListener( this );
//        buttonsPanel.add( queryButton );

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
            BasicFileFilter csvFileFilter =
                    new BasicFileFilter( "csv", "CSV files" );
            fileChooser.addChoosableFileFilter( csvFileFilter );

            //  But allow all files as well.
            fileChooser.addChoosableFileFilter
            ( fileChooser.getAcceptAllFileFilter() );
        }
    } //initFileChooser


    /**
     *  Restore  metadata that has been previously written to a
     *  CSV file. The file name is obtained interactively.
     */
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
                saveTable( file );
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
    private void saveTable(File paramFile) throws IOException
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
        //    tableWriter.append(';');
       //     tableWriter.append(p.getValueAt(row, DESCRIPTION_INDEX).toString());
            tableWriter.append('\n');
        }
        tableWriter.flush();
        tableWriter.close();

    } //saveTable()

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

        MetadataTableModel newmodel = new MetadataTableModel(headers);
        BufferedReader CSVFile = new BufferedReader(new FileReader(paramFile)); 	

        String tableRow = CSVFile.readLine();  	
        while (tableRow != null) {
            String [] paramRow = new String [NRCOLS];
            paramRow = tableRow.split(";", NRCOLS);	 		
            newmodel.addRow(paramRow);
            tableRow = CSVFile.readLine(); 
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
    // checkbox included 
    public void removeAll( ) {
        MetadataTableModel mtm = (MetadataTableModel) metadataTable.getModel();
        mtm.setRowCount(0);
       
    }
    
    /**
     * Adds a new row to the table 
     * @param mip the metadata parameter that will be added to the table
     */
    // checkbox included 
    public void addRow(MetadataInputParameter mip ) {
        addRow ( mip, false );
    }
    
    // useCheckBox true: checkbox included
    // useCheckBox false: checkbox not included, parameter automatically "checked"
    public void addRow (MetadataInputParameter mip, boolean selected) {

        String [] paramRow = new String [NRCOLS];
        paramRow[NR_SERVERS_INDEX] = Integer.toString(mip.getCounter());//+"/"+Integer.toString(nrServers);       // nr supporting servers
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

        MetadataTableModel mtm = (MetadataTableModel) metadataTable.getModel();
        mtm.addRow( paramRow );

        if ( selected ) {         
            mtm.setValueAt(true, mtm.getRowCount()-1, SELECTED_INDEX);
        }
   
    } //addRow
    


    /**
     * Set number of servers to the respective column in the table
     * @param counter the nr of servers supporting the parameter
     * @param name the name of the parameter
     */
    public void setNrServers(int counter, String UCD) {

        // boolean found=false;
        int row=0;
       // String paramName = name.replace("INPUT:", ""); 
        MetadataTableModel mtm = (MetadataTableModel) metadataTable.getModel();
       
        while (row<mtm.getRowCount() )
        {
            if ( mtm.getValueAt(row, UCD_INDEX).toString().equalsIgnoreCase(UCD)) {
              
                mtm.setValueAt(counter, row, NR_SERVERS_INDEX); 
                return;
            }
            row++;
        }

    }//setnrServers

    /** 
     * TableModelListener method
     *
     */
    public void tableChanged(TableModelEvent tme) {
      
        if ( (tme.getColumn() != TableModelEvent.ALL_COLUMNS) && (tme.getColumn() == SELECTED_INDEX  || tme.getColumn() == VALUE_INDEX) )
             this.firePropertyChange("changeQuery", false, true);      
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
