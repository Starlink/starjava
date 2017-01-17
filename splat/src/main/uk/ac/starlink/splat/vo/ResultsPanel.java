package uk.ac.starlink.splat.vo;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.SpectrumIO;
import uk.ac.starlink.splat.iface.SpectrumIO.Props;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOSerializer;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableWriter;


/**
 * Panel to handle the query results (from SSAP,  ObsCore or spectral line queries)
 * 
 *
 * @author Margarida Castro Neves 
 * 
 */
public class ResultsPanel extends JPanel implements ActionListener, MouseListener  {
 
   
    protected JTabbedPane resultsPane;
    protected JPanel controlPanel = null;
    private boolean dataLinkEnabled = false;
    protected JButton displaySelectedButton;
    protected JButton displayAllButton;
    protected JButton downloadSelectedButton;
    protected JButton downloadAllButton;
    protected JButton deselectVisibleButton;
    protected JButton deselectAllButton;
    private JToggleButton dataLinkButton;
//    private SSAQueryBrowser ssaQueryBrowser=null;
//    private ObsCorePanel obsQueryBrowser=null;
  //  protected LineBrowser slQueryBrowser=null;

    protected JPopupMenu popupMenu;
    
    private JFrame browser;
    private int datatype=-1;
    
    private static final int SSAP=0;
    private static final int OBSCORE=1;
   
    /**
     * @uml.property  name="dataLinkFrame"
     * @uml.associationEnd  
     */
    private DataLinkQueryFrame dataLinkFrame = null;
    private static Logger logger =  Logger.getLogger( "uk.ac.starlink.splat.vo.ResultsPanel" );

 

    public ResultsPanel() {
        
    }
    
    public ResultsPanel( JTabbedPane resultsPane, SSAQueryBrowser browser ) {
        this.resultsPane=resultsPane;
        this.browser=browser;
        if (browser.getClass().equals(SSAQueryBrowser.class))
            datatype=SSAP; 
        initComponents();
        popupMenu = makeSpecPopup();
        
    }
    
    
    public ResultsPanel(ObsCorePanel browser) {
        this.browser=browser;
        datatype=OBSCORE;
        initComponents();
        popupMenu = makeSpecPopup();       
    }

    

    protected void initComponents() {
        this.setLayout(new GridBagLayout());
        this.setBorder ( BorderFactory.createTitledBorder( "Query results:" ) );
        this.setToolTipText( "Results of query to the current list of services. One table per service" );
     
        GridBagConstraints gbc=new GridBagConstraints();
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.weighty=1;
        gbc.weightx=1;
        gbc.anchor=GridBagConstraints.NORTHWEST;
        gbc.fill=GridBagConstraints.BOTH;
        resultsPane = new JTabbedPane();
        resultsPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if ( dataLinkEnabled ) {
                   if (resultsPane.getIconAt(resultsPane.getSelectedIndex())!=null) { // it's a datalink service

                        if (dataLinkFrame != null && dataLinkEnabled) {
                            dataLinkFrame.setServer(resultsPane.getTitleAt(resultsPane.getSelectedIndex()));
                            dataLinkFrame.setVisible(true);
                        } 
                    } else {
                        dataLinkFrame=null;
                    }
                }
            }
        });
        this.add( resultsPane , gbc);
    
        gbc.gridx=0;
        gbc.gridy=1;
        gbc.weighty=0;
        gbc.anchor = GridBagConstraints.PAGE_END;
        gbc.fill=GridBagConstraints.HORIZONTAL;
        if (controlPanel == null)
            controlPanel=initControlPanel();
        add( controlPanel, gbc );
    }
    
    private JPanel initControlPanel() 
    {
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcontrol = new GridBagConstraints();
        gbcontrol.gridx=0;
        gbcontrol.gridy=0;
        gbcontrol.weightx=1;
        gbcontrol.weighty=0;
        gbcontrol.fill = GridBagConstraints.HORIZONTAL;

        //  Download and display.
        displaySelectedButton = new JButton( "<html>Display<BR> selected</html>" );
        displaySelectedButton.addActionListener( this );
        displaySelectedButton.setMargin(new Insets(1, 10, 1, 10));  
        displaySelectedButton.setToolTipText
        ( "Download and display all spectra selected in all tables" );
        controlPanel.add( displaySelectedButton,gbcontrol );


        displayAllButton = new JButton( "<html>Display<BR>all</html>" );
        displayAllButton.addActionListener( this );
        displayAllButton.setMargin(new Insets(1,10,1,10));  
        displayAllButton.setToolTipText
        ( "Download and display all spectra in all tables" );
        gbcontrol.gridx=1;
        controlPanel.add( displayAllButton, gbcontrol );

        //  Just download.
        downloadSelectedButton = new JButton( "<html>Download<BR>selected</html>" );
        downloadSelectedButton.addActionListener( this );
        downloadSelectedButton.setMargin(new Insets(1,10,1,10));  
        downloadSelectedButton.setToolTipText
        ( "Download all spectra selected in all tables");
        gbcontrol.gridx=2;
        controlPanel.add( downloadSelectedButton, gbcontrol );
      

        downloadAllButton = new JButton( "<html>Download<BR> all</html>" );
        downloadAllButton.addActionListener( this );
        downloadAllButton.setMargin(new Insets(1,10,1,10));  
        downloadAllButton.setToolTipText
        ( "Download all spectra in all tables");
        gbcontrol.gridx=3;
        controlPanel.add( downloadAllButton , gbcontrol);


        //  Deselect
        deselectVisibleButton = new JButton( "<html>Deselect<br>table</html>" );
        deselectVisibleButton.addActionListener( this );
        deselectVisibleButton.setMargin(new Insets(1,10,1,10));  
        deselectVisibleButton.setToolTipText
        ( "Deselect all spectra in displayed table" );
      //  controlPanel2.add( deselectVisibleButton );
        gbcontrol.gridx=4;
        controlPanel.add( deselectVisibleButton, gbcontrol );


        deselectAllButton = new JButton( "<html>Deselect <BR>all</html>" );
        deselectAllButton.addActionListener( this );
        deselectAllButton.setMargin(new Insets(1,10,1,10));  
        deselectAllButton.setToolTipText
        ( "Deselect all spectra in all tables" );
     //   controlPanel2.add( deselectAllButton );
        gbcontrol.gridx=5;
        controlPanel.add( deselectAllButton , gbcontrol);

        dataLinkButton = new JToggleButton( "<html>DataLink<BR>Services</html>" );
        dataLinkButton.addActionListener( this );
        dataLinkButton.setMargin(new Insets(1,10,1,10));  
        dataLinkButton.setToolTipText ( "DataLink parameters" );
        dataLinkButton.setEnabled(false);
        dataLinkButton.setVisible(false);
     //   controlPanel2.add( deselectAllButton );
        gbcontrol.gridx=6;
        controlPanel.add( dataLinkButton, gbcontrol );
       
        return controlPanel;
     
    }

    
    private JPopupMenu makeSpecPopup() {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem dlMenuItem = new JMenuItem("Download");
        
        dlMenuItem.addActionListener(new SpecPopupMenuAction());
        popup.add(dlMenuItem);
        JMenuItem infoMenuItem = new JMenuItem("Info");
        infoMenuItem.addActionListener(new SpecPopupMenuAction());
        popup.add(infoMenuItem);
        JMenuItem dispMenuItem = new JMenuItem("Display");
        dispMenuItem.addActionListener(new SpecPopupMenuAction());
        popup.add(dispMenuItem);
        
        // selection menu
        JMenu selectionMenu = new SSAQueryResultsTableSelectionMenu();
        popup.add(selectionMenu);
        
        return popup;
    }

    //
    // ActionListener interface.
    //
    public void actionPerformed( ActionEvent e )
    {
        Object source = e.getSource();
        
        if ( source.equals( displaySelectedButton ) ) {
            displaySpectra( true, true, null, -1 );
            return;
        }
        if ( source.equals( displayAllButton ) ) {
            displaySpectra( false, true, null, -1 );
            return;
        }

        if ( source.equals( downloadSelectedButton ) ) {
            displaySpectra( true, false, null, -1 );
            return;
        }
        if ( source.equals( downloadAllButton ) ) {
            displaySpectra( false, false, null, -1 );
            return;
        }

        if ( source.equals( deselectVisibleButton ) ) {
            deselectSpectra( false );
            return;
        }
        if ( source.equals( deselectAllButton ) ) {
            deselectSpectra( true );
            return;
        }
        if ( source.equals( dataLinkButton ) ) { 
            if (dataLinkFrame != null && dataLinkFrame.getParams() != null) {
                if ( dataLinkButton.getModel().isSelected() ) {
                    activateDataLinkSupport();
                    if (resultsPane.isEnabledAt(resultsPane.getSelectedIndex()))
                        dataLinkFrame.setVisible(true);  
                }
                else {
                    if (dataLinkFrame.isVisible()) { // deactivate
                        dataLinkFrame.setVisible(false);
                    }
                    deactivateDataLinkSupport();
                }
            }

            return;
        }

    }
    
    /**
     * Get the main SPLAT browser to download and display spectra.
     * <p>
     * spectrum from a row in a given table. If table is null, then the
     * selected parameter determines the behaviour of all or just the selected
     * spectra.
     */
    protected void displaySpectra( boolean selected, boolean display,
            StarJTable table, int row )
    {
     
      Props[] propList = prepareSpectra(selected, getCurrentTable(table), row);
      if (propList==null||propList.length==0)
          return;
      if (datatype==SSAP)
        ((SSAQueryBrowser) browser).displaySpectra(propList, display);
      else if (datatype==OBSCORE)
        ((ObsCorePanel) browser).displaySpectra(propList, display);
    }
    
    protected StarJTable  getCurrentTable(StarJTable table) 
    {       
        if (table == null) {
            JScrollPane pane = (JScrollPane) resultsPane.getSelectedComponent();
            return (StarJTable) pane.getViewport().getView();
        } else {
             return table;
        }        
    }

    
    /**
     * Deselect all spectra
     * <p>
     * spectrum from a row in a given table. If table is null, then the
     * selected parameter determines the behaviour of all or just the selected
     * spectra.
     */
    protected void deselectSpectra( boolean all )
    {
      
     
 
      if (datatype==SSAP)
          ((SSAQueryBrowser) browser).deselectSpectra(all, resultsPane.getSelectedComponent());
      else if (datatype==OBSCORE){
          if (all ) {
              for (int i=0;i<resultsPane.getTabCount(); i++) {
                  JScrollPane pane = (JScrollPane) resultsPane.getComponentAt(i);
                  StarPopupTable table = (StarPopupTable) pane.getViewport().getView();
                  table.clearSelection();
              }
          } else {
              JScrollPane pane = (JScrollPane) resultsPane.getSelectedComponent();
              StarPopupTable table = (StarPopupTable) pane.getViewport().getView();
              table.clearSelection();
          }
      }
      
    }
    
    /**
     * ActivateDataLinkSupport
     * deactivate all sites that do not support DataLink
     * activate DataLink queries on supported sites
     */
    protected void activateDataLinkSupport() {
       
        dataLinkEnabled=true;
        int selected=-1;
        //int anyIndex = -1;
        int nrTabs = resultsPane.getTabCount();
        for(int i = 0; i < nrTabs; i++)
        {
           if (resultsPane.getIconAt(i) == null) { // no datalink service
               resultsPane.setEnabledAt(i, false);
           }
           else {
               if (resultsPane.getSelectedIndex() == i)
                   selected = i;
           }
        }
        
        // if current selection is not a DataLink service do nothing
        if (selected < 0)
            return;      
         dataLinkFrame.setServer(resultsPane.getTitleAt(selected)); 
    }
    
    /**
     * DeactivateDataLinkSupport
     * activate all sites, without Datalink support
     */
    protected void deactivateDataLinkSupport() {
        
        dataLinkEnabled=false;
        int nrTabs = resultsPane.getTabCount();
        
        for(int i = 0; i < nrTabs; i++)
        {
            resultsPane.setEnabledAt(i, true);      
        }
        dataLinkButton.setSelected(false);
        
    }


    /**
     * Remove all results
     * <p>
     *
     */
    protected void removeAllResults()
    {
      resultsPane.removeAll();
    }
    
    /**
     * Add a results Tab
     * <p>
     *
     */
    protected void addTab(String shortName, StarPopupTable table)
    {
        table.setComponentPopupMenu(popupMenu);
        table.configureColumnWidths(200, table.getRowCount());
        JScrollPane resultScroller=new JScrollPane(table); 
        resultsPane.addTab(shortName, resultScroller );       
    }
    
    protected void addTab(String shortName, StarPopupTable table, JPopupMenu popupmenu)
    {
        table.setComponentPopupMenu(popupmenu);
        table.configureColumnWidths(200, table.getRowCount());
        JScrollPane resultScroller=new JScrollPane(table); 
        resultsPane.addTab(shortName, resultScroller );       
    }
  
    protected void addTab(String shortName, ImageIcon icon, StarPopupTable table)
    {
        table.setComponentPopupMenu(popupMenu);
        table.configureColumnWidths(200, table.getRowCount());
        JScrollPane resultScroller=new JScrollPane(table); 
        resultsPane.addTab(shortName, icon, resultScroller );   
    }

    public void removeDataLinkButton() {
        dataLinkButton.setVisible(false);        
    }
    
    public void enableDataLink(DataLinkQueryFrame datalinkframe) {
        dataLinkButton.setEnabled(true);
        dataLinkButton.setVisible(true);
        dataLinkFrame=datalinkframe;
        
    }
    
    /**
     *  Interactively get a file name and save current query results to it as
     *  a VOTable.
     */
    public void saveQueryToFile()
    {
        if ( resultsPane == null || resultsPane.getTabCount() == 0 ) {
            JOptionPane.showMessageDialog( this,
                    "There are no queries to save",
                    "No queries", JOptionPane.ERROR_MESSAGE );
            return;
        }

       
        BasicFileChooser fileChooser = new BasicFileChooser( false );
        fileChooser.setMultiSelectionEnabled( false );

        //  Add a filter for XML files.
        BasicFileFilter xmlFileFilter = new BasicFileFilter( "xml", "XML files" );
       fileChooser.addChoosableFileFilter( xmlFileFilter );

        //  But allow all files as well.
        fileChooser.addChoosableFileFilter( fileChooser.getAcceptAllFileFilter() );
        int result = fileChooser.showSaveDialog( this );
        if ( result == JFileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            try {
                saveQuery( file );
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }
    }
   
    
    /**
     *  Save current query to a File, writing the results as a VOTable.
     */
    protected void saveQuery( File file )
            throws SplatException
     {
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter( new FileWriter( file ) );
        }
        catch (IOException e) {
            throw new SplatException( e );
        }
        saveQuery(writer);
     }

    /**
     *  Save current query results to a BufferedWriter. The resulting document
     *  is a VOTable with a RESOURCE that contains a TABLE for each set of
     *  query results.
     */
    protected void saveQuery( BufferedWriter writer )
            throws SplatException
            {

        String xmlDec = VOTableWriter.DEFAULT_XML_DECLARATION;
    
        try {
            writer.write( xmlDec );
            writer.newLine();
            writer.write( "<VOTABLE version=\"1.1\"" );
            writer.newLine();
            writer.write( "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" );
            writer.newLine();
            writer.write( "xsi:schemaLocation=\"http://www.ivoa.net/xml/VOTable/v1.1\"" );
            writer.newLine();
            writer.write( "xmlns=\"http://www.ivoa.net/xml/VOTable/v1.1\">" );
            writer.newLine();
            writer.write( "<RESOURCE>" );
            writer.newLine();
           
            StarJTable starJTable = null;
            VOSerializer serializer = null;
            StarTable table = null;
            

            for (int i=0;i<resultsPane.getTabCount(); i++ ) {
                  
                Component comp = resultsPane.getComponentAt(i);
                if (comp.getClass()==JScrollPane.class) {
                    JScrollPane pane = (JScrollPane) comp;               
                    starJTable = (StarJTable) pane.getViewport().getView();
                } else {
                    starJTable = (StarJTable) comp;
                }
                table = starJTable.getStarTable();
                table.setName(resultsPane.getTitleAt(i));
               
                //String name = table.getName();
                int n = table.getColumnCount();
                for ( int j = 0; j < n; j++ ) {
                    ColumnInfo ci = table.getColumnInfo( j );
                    ci.setAuxDatum( new DescribedValue( VOStarTable.ID_INFO, null ) );
                }
                serializer = VOSerializer.makeSerializer( DataFormat.TABLEDATA, table );
                serializer.writeInlineTableElement( writer );                
            }
            writer.write( "</RESOURCE>" );
            writer.newLine();
            if (dataLinkFrame != null)
                saveDataLinkParams(writer);
            writer.write( "</VOTABLE>" );
            writer.newLine();
            writer.close();
            
        }
        catch (IOException e) {
            throw new SplatException( "Failed to save queries", e );
        }
    }
    
    protected void saveDataLinkParams( BufferedWriter writer ) throws IOException 
    {
        
        String [] servers = dataLinkFrame.getServers();
        for (int i=0;i<servers.length; i++) {
            DataLinkParams dlp = dataLinkFrame.getServerParams(servers[i]);
            if ( dlp.getServiceCount() > 0 )
                dlp.writeParamToFile(writer, servers[i]);            
        } 

    }

    /**
     *  Restore a set of previous query results that have been written to a
     *  VOTable. The file name is obtained interactively.
     */
    public ArrayList<VOStarTable> readQueryFromFile()
    {
        BasicFileChooser fileChooser = new BasicFileChooser( false );
        fileChooser.setMultiSelectionEnabled( false );

        //  Add a filter for XML files.
        BasicFileFilter xmlFileFilter = new BasicFileFilter( "xml", "XML files" );
        fileChooser.addChoosableFileFilter( xmlFileFilter );
        int result = fileChooser.showOpenDialog( this );
        if ( result == JFileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            try {
                return readQuery( file );
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }
        return null;
    }

    /**
     *  Restore a set of query results from a File. The File should have the
     *  results written previously as a VOTable, with a RESOURCE containing
     *  the various query results as TABLEs.
     */
    
    protected ArrayList<VOStarTable> readQuery( File file )
            throws SplatException
    {
    
        DataLinkQueryFrame newFrame = null;
        VOElement rootElement = null;
        try {
             rootElement = new VOElementFactory().makeVOElement( file );
        } catch (IOException e) {
            throw new SplatException( "Failed to open query results file", e );
        } catch (SAXException e) {
            //TODO Auto-generated catch block
            e.printStackTrace();
        }
       
        //  First element should be a RESOURCE.
        VOElement[] resource = rootElement.getChildren();
        VOStarTable table = null;
        ArrayList<VOStarTable> tableList = new ArrayList<VOStarTable>();
        String tagName = null;        
        
        for ( int i = 0; i < resource.length; i++ ) {
            tagName = resource[i].getTagName();
            if ( "RESOURCE".equals( tagName ) ) {
               String utype=resource[i].getAttribute("utype");
                //  Look for the TABLEs.
                VOElement child[] = resource[i].getChildren();
                for ( int j = 0; j < child.length; j++ ) {
                    tagName = child[j].getTagName();
                    if ( "TABLE".equals( tagName ) ) {
                        try {
                            table = new VOStarTable( (TableElement) child[j] );
                            
                        }
                        catch (IOException e) {
                            throw new SplatException( "Failed to read query result", e );
                        }
                        tableList.add( table );
                    }
                    else if (utype!= null && utype.equals("adhoc:service")) {
                        String name=resource[i].getAttribute("name");
                        DataLinkParams dlp = new DataLinkParams(resource[i]);
                        if ( newFrame == null ) {
                            newFrame = new DataLinkQueryFrame();
                       } 
                       newFrame.addServer(name, dlp);  // associate this datalink service information to the current server
                    }
                }
            }
        }
        if ( tableList.size() > 0 ) {
            
            if (newFrame != null) {
                enableDataLink(newFrame);
                deactivateDataLinkSupport();
            }
            else {
                dataLinkButton.setVisible(false);
            }
            return tableList ;
        }
        else {
            throw new SplatException( "No query results found" );
        }
       
     }
    
    protected DataLinkQueryFrame getDataLinkFrame() {
        return dataLinkFrame;
    }

    public int getSelectedIndex() {
        return resultsPane.getSelectedIndex();
    }    
    
    
    /**
     * Creates a list of spectra to be loaded and their data formats and metadata
     * @param selected returns all selected spectra
     * @param table returns only the spectra from this table
     * @param row if not -1, only this spectra on this row will be returned
     * @return the spectra to be loaded/displayed
     */
    protected Props[] prepareSpectra( boolean selected, StarJTable table, int row )
    {
        //  List of all spectra to be loaded 
        ArrayList<Props> specList = null;
     
        
        if ( table == null ) { 
            if (resultsPane.getTabCount()==0)  // avoids NPE if no results are present
                return null;
            //  Visit all the tabbed StarJTables.
            for (int i=0;i<resultsPane.getTabCount();i++) {
                specList = extractSpectraFromTable(  (StarJTable) resultsPane.getComponentAt(i), selected, -1 );
            }
        }
        else {
            specList = extractSpectraFromTable( table, selected, row );
        }

        //  If we have no spectra complain and stop.
        if ( specList.size() == 0 ) {
            String mess;
            if ( selected ) {
                mess = "There are no spectra selected";
            }
            else {
                mess = "No spectra available";
            }
            JOptionPane.showMessageDialog( this, mess, "No spectra",
                    JOptionPane.ERROR_MESSAGE );
            return null;
        }

        //  And load and display...
        SpectrumIO.Props[] propList = new SpectrumIO.Props[specList.size()];
        specList.toArray( propList );
        
        // check for authentication
        for (int p=0; p<propList.length; p++ ) {
            URL url=null;
            try {
                 url = new URL(propList[p].getSpectrum());
                 logger.info("Spectrum URL"+url);
            } catch (MalformedURLException mue) {
                logger.info(mue.getMessage());
            }
        }
        return propList;

    }


    /**
     * Extract all the links to spectra for downloading, plus the associated
     * information available in the VOTable. Each set of spectral information
     * is used to populated a SpectrumIO.Prop object that is added to the
     * specList list.
     * <p>
     * Can return the selected spectra, if requested, otherwise all spectra
     * are returned or if a row value other than -1 is given just one row.
     * @throws SplatException 
     */ 
    private ArrayList<Props> extractSpectraFromTable( StarJTable starJTable,
          //  ArrayList<Props> specList,
            boolean selected,
            int row )
    {
        int[] selection = null;
        ArrayList<Props> specList = new ArrayList<Props>();
        
        HashMap< String, String > dataLinkQueryParams = null;
        String idSource = null;
        String accessURL = null;
        if ( dataLinkFrame != null && dataLinkFrame.isVisible() ) {
            dataLinkQueryParams = dataLinkFrame.getParams();
            idSource = dataLinkFrame.getIDSource(); 
            accessURL = dataLinkFrame.getAccessURL();
        }
       
        
        //  Check for a selection if required, otherwise we're using the given
        //  row.
        if ( selected && row == -1 ) {
            selection = starJTable.getSelectedRows();
        }
        else if ( row != -1 ) {
            selection = new int[1];
            selection[0] = row;
        }

        // Only do this if we're processing all rows or we have a selection.
        if ( selection == null || selection.length > 0 ) {
            StarTable starTable = starJTable.getStarTable();

            //  Check for a column that contains links to the actual data
            //  (XXX these could be XML links to data within this
            //  document). The signature for this is an UCD of DATA_LINK,
            //  or a UTYPE of Access.Reference.
            int ncol = starTable.getColumnCount();
            int linkcol = -1;
            int typecol = -1;
            int namecol = -1;
            int axescol = -1;
            int specaxiscol = -1;
            int fluxaxiscol = -1;
            int unitscol = -1;
            int specunitscol = -1;
            int fluxunitscol = -1;
            int fluxerrorcol = -1;
            int pubdidcol=-1;
            int idsrccol=-1;
            int specstartcol=-1;
            int specstopcol=-1;
            int ucdcol=-1;
            int timecol=-1;
            int timeunitscol=-1;
            
            ColumnInfo colInfo;
            String ucd;
            String utype;
            String dataLinkRequest="";

            for( int k = 0; k < ncol; k++ ) {
                colInfo = starTable.getColumnInfo( k );
                ucd = colInfo.getUCD();
                utype = colInfo.getUtype();


                // for Obscore, use the column name

                String colName = colInfo.getName();
                if (datatype == OBSCORE ) {
                    if (colName != null) {
                        colName = colName.toLowerCase();
                        if ( colName.endsWith( "access_url" ) ) {
                            linkcol = k;
                        }
                        else if ( colName.endsWith( "access_format" ) ) {
                            typecol = k;
                        }
                        else if ( colName.endsWith( "target_name" ) ) {
                            namecol = k;
                        }
                        else if ( colName.endsWith( "obs_ucd" ) ) {
                            ucdcol = k;
                        }
                        else if ( colName.endsWith( "obs_publisher_did" ) ) {
                            pubdidcol = k;
                        }
                        else if ( colName.endsWith( "em_min" ) ) {
                            specstartcol = k;
                        }
                        else if ( colName.endsWith( "em_max" ) ) {
                            specstopcol = k;
                        }
                    }
                }
                if (datatype==SSAP ) {
                    if ( ucd != null && !ucd.isEmpty()) {

                        //  Old-style UCDs for backwards compatibility.

                        ucd = ucd.toLowerCase();
                        if ( ucd.equals( "data_link" ) ) {
                            linkcol = k;
                        }
                        else if ( ucd.equals( "vox:spectrum_format" ) ) {
                            typecol = k;
                        }
                        else if ( ucd.equals( "vox:image_title" ) ) {
                            namecol = k;
                        }
                        else if ( ucd.equals( "vox:spectrum_axes" ) ) {
                            axescol = k;
                        }
                        else if ( ucd.equals( "vox:spectrum_units" ) ) {
                            unitscol = k;
                        }
                    } 
                    if (utype != null ){
                        //  Version 1.0 utypes. XXX not sure if axes names
                        //  are in columns or are really parameters. Assume
                        //  these work like the old-style scheme and appear in
                        //  the columns.
                        utype = utype.toLowerCase();
                        if ( utype.endsWith( "access.reference" ) ) {
                            linkcol = k;
                        }
                        else if ( utype.endsWith( "access.format" ) ) {
                            typecol = k;
                        }
                        else if ( utype.endsWith( "target.name" ) ) {
                            namecol = k;
                        }
                        else if ( utype.endsWith( "char.spectralaxis.name" ) ) {
                            specaxiscol = k;
                        }
                        else if ( utype.endsWith( "char.spectralaxis.unit" ) ) {
                            specunitscol = k;
                        }
                        else if ( utype.endsWith( "char.fluxaxis.name" ) ) {
                            fluxaxiscol = k;
                        }
                        else if ( utype.endsWith( "char.fluxaxis.accuracy.staterror" ) ) {
                            fluxerrorcol = k;
                        }
                        else if ( utype.endsWith( "char.fluxaxis.unit" ) ) {
                            fluxunitscol = k;
                        }
                        else if ( utype.endsWith( "Curation.PublisherDID" ) ) {
                            pubdidcol = k;
                        }
                        else if ( utype.endsWith( "char.spectralAxis.coverage.bounds.start" ) ) {
                            specstartcol = k;
                        }
                        else if ( utype.endsWith( "char.spectralAxis.coverage.bounds.stop" ) ) {
                            specstopcol = k;
                        }

                    }
                    if (colInfo.getName().equals("ssa_pubDID"))
                        pubdidcol = k;
                }

                if (colInfo.getName().equals(idSource))
                    idsrccol = k;

            } // for

            if (datatype == SSAP && idsrccol != -1  && dataLinkQueryParams != null ) { // check if datalink parameters are present

                if ( ! dataLinkQueryParams.isEmpty() ) {                   
                    for (String key : dataLinkQueryParams.keySet()) {
                        String value = dataLinkQueryParams.get(key);
                        if (value != null && value.length() > 0) {
                            try {//

                                if (! key.equals("IDSource") && ! (key.equals("AccessURL"))) {
                                    dataLinkRequest+="&"+key+"="+URLEncoder.encode(value, "UTF-8");
                                }

                            } catch (UnsupportedEncodingException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }                                     
                        }
                    }
                }
            }


            //  If we have a DATA_LINK column, gather the URLs it contains
            //  that are appropriate.
            if ( linkcol != -1 ) {
                RowSequence rseq = null;
                SpectrumIO.Props props = null;
                String value = null;
                String[] axes;
                String[] units;
                try {
                    if ( ! selected && selection == null ) {
                        //  Using all rows.
                        rseq = starTable.getRowSequence();
                        while ( rseq.next() ) {
                            value = ( (String) rseq.getCell( linkcol ).toString() );
                            value = value.trim();
                            props = new SpectrumIO.Props( value );
                            if ( typecol != -1 ) {
                                value = ((String)rseq.getCell( typecol ).toString() );
                                if ( value != null ) {
                                    value = value.trim();
                                    props.setType( SpecDataFactory.mimeToSPLATType( value ) );
                                }
                            } //while
                            if ( namecol != -1 ) {
                                value = ( (String)rseq.getCell( namecol ).toString() );
                                if ( value != null ) {
                                    value = value.trim();
                                    props.setShortName( value );
                                }
                            }

                            if ( axescol != -1 ) {

                                //  Old style column names.
                                value = ( (String)rseq.getCell( axescol ).toString() );
                                if ( value != null ) {
                                    value = value.trim();
                                    axes = value.split("\\s");
                                    props.setCoordColumn( axes[0] );
                                    props.setDataColumn( axes[1] );
                                    if ( axes.length == 3 ) {
                                        props.setErrorColumn( axes[2] );
                                    }
                                }
                            } // if axescol !- 1
                            else {

                                //  Version 1.0 style.
                                if ( specaxiscol != -1 ) {
                                    value = (String)rseq.getCell(specaxiscol).toString();
                                    props.setCoordColumn( value );
                                }
                                if ( fluxaxiscol != -1 ) {
                                    value = (String)rseq.getCell(fluxaxiscol).toString();
                                    props.setDataColumn( value );
                                }
                                if ( fluxerrorcol != -1 ) {
                                    value = (String)rseq.getCell(fluxerrorcol).toString();
                                    props.setErrorColumn( value );
                                }
                            } //else 

                            if ( unitscol != -1 ) {

                                //  Old style column names.
                                value = ( (String)rseq.getCell( unitscol ).toString() );
                                if ( value != null ) {
                                    value = value.trim();
                                    units = value.split("\\s");
                                    props.setCoordUnits( units[0] );
                                    props.setDataUnits( units[1] );
                                    //  Error must have same units as data.
                                }
                            }
                            else {

                                //  Version 1.0 style.
                                if ( specunitscol != -1 ) {
                                    value = (String)rseq.getCell(specunitscol).toString();
                                    props.setCoordUnits( value );
                                }
                                if ( fluxunitscol != -1 ) {
                                    value = (String)rseq.getCell(fluxunitscol).toString();
                                    props.setDataUnits( value );
                                }
                            }
                         
                            if (idsrccol != -1  && dataLinkQueryParams != null) { 
                                
                                if (! dataLinkQueryParams.isEmpty()) { 
                                   props.setIdValue(rseq.getCell(idsrccol).toString());
                                   props.setIdSource(idSource);
                                   props.setDataLinkRequest(dataLinkRequest);
                                   props.setServerURL(dataLinkQueryParams.get("AccessURL"));
                                   String format = dataLinkQueryParams.get("FORMAT");
                                   if (format != null && format != "") {
                                       props.setDataLinkFormat(format);
                                       props.setType(SpecDataFactory.mimeToSPLATType( format ));
                                   }
                                }
                            }
                            specList.add( props );
                        } //while
                    } // if selected
                    else {
                        //  Just using selected rows. To do this we step
                        //  through the table and check if that row is the
                        //  next one in the selection (the selection is
                        //  sorted).
                        rseq = starTable.getRowSequence();
                        int k = 0; // Table row
                        int l = 0; // selection index
                        while ( rseq.next() ) {
                            if ( k == selection[l] ) {

                                // Store this one as matches selection.
                                if (rseq.getCell( linkcol ) != null)                                      
                                    value = ( (String)rseq.getCell( linkcol ).toString() );
                                if (value != null ) {         
                                    value = value.trim();
                                    props = new SpectrumIO.Props( value );
                                } 
                                if ( typecol != -1 ) {
                                    value = null;
                                    Object obj = rseq.getCell(typecol);
                                    if (obj != null) 
                                        value =((String)rseq.getCell(typecol).toString());
                                    if ( value != null ) {
                                        value = value.trim();
                                        props.setType( SpecDataFactory.mimeToSPLATType( value ) );
                                    }
                                }
                                if ( namecol != -1 ) {
                                    value = null;
                                    Object obj = rseq.getCell(namecol);
                                    if (obj != null) 
                                    value = ((String)rseq.getCell( namecol ).toString());
                                    if ( value != null ) {
                                        value = value.trim();
                                        props.setShortName( value );
                                    }
                                }
                                if ( axescol != -1 ) {
                                    value = null;
                                    Object obj = rseq.getCell(axescol);
                                    if (obj != null) 
                                        value = ((String)obj.toString());
                                    
                                    if (value != null ) {
                                         value = value.trim();
                                        axes = value.split("\\s");
                                        props.setCoordColumn( axes[0] );
                                        props.setDataColumn( axes[1] );
                                    }
                                }
                                if ( unitscol != -1 ) {
                                    value = null;
                                    Object obj = rseq.getCell(unitscol);
                                    if (obj != null) 
                                        value = ((String)rseq.getCell(unitscol).toString());
                                    if ( value != null ) {
                                        units = value.split("\\s");
                                        props.setCoordUnits( units[0] );
                                        props.setDataUnits( units[1] );
                                    }
                                }
                              
                                if (idsrccol != -1  && dataLinkQueryParams != null) {  
                                    
                                    if (! dataLinkQueryParams.isEmpty()) { 
                                        props.setIdValue(rseq.getCell(idsrccol).toString());
                                        props.setIdSource(idSource);
                                       props.setDataLinkRequest(dataLinkRequest);
                                      // props.setServerURL(dataLinkQueryParam.get("AccessURL"));
                                       props.setServerURL(accessURL);
                                       String format = dataLinkQueryParams.get("FORMAT");
                                       if (format != null && format != "") {
                                           props.setDataLinkFormat(format);
                                           props.setType(SpecDataFactory.mimeToSPLATType( format ) );
                                       }
                                    }
                                }
                                specList.add( props );

                                //  Move to next selection.
                                l++;
                                if ( l >= selection.length ) {
                                    break;
                                }
                            }
                            k++;
                        }
                    } // if selected
                } // try
                catch (IOException ie) {
                    ie.printStackTrace();
                }
                catch (NullPointerException ee) {
                    ErrorDialog.showError( this, "Failed to parse query results file", ee );
                }
                finally {
                    try {
                        if ( rseq != null ) {
                            rseq.close();
                        }
                    }
                    catch (IOException iie) {
                        // Ignore.
                    }
                }
            }// if linkcol != -1
        } 
        return specList;
    }
    

    //
    // MouseListener interface. Double clicks display the clicked spectrum.
    //
    public void mousePressed( MouseEvent e ) {}
    public void mouseReleased( MouseEvent e ) {}
    public void mouseEntered( MouseEvent e ) {}
    public void mouseExited( MouseEvent e ) {}
    public void mouseClicked( MouseEvent e )
    {

            if ( e.getClickCount() == 2 ) {
                StarJTable table = (StarJTable) e.getSource();
                Point p = e.getPoint();
                int row = table.rowAtPoint( p );
                displaySpectra( false, true, table, row );
            }
    }
    

    
    public class SpecPopupMenuAction extends AbstractAction
    {
        
        public void actionPerformed( ActionEvent e) {
            JMenuItem jmi  = (JMenuItem) e.getSource();
            JPopupMenu jpm = (JPopupMenu) jmi.getParent();
            StarPopupTable table = (StarPopupTable) jpm.getInvoker();
           
           int row = table.getPopupRow();
          
            if (e.getActionCommand().equals("Info")) {
                table.showInfo(row);
            }
            else if (e.getActionCommand().equals("Display")) {
               displaySpectra( false, true, (StarJTable) table, row );
            }   
            else if (e.getActionCommand().equals("Download")) {
                displaySpectra( false, false, (StarJTable) table, row );
            }             
        }
    }   
}
