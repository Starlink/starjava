package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import jsky.util.Logger;
import jsky.util.SwingWorker;
import uk.ac.starlink.splat.data.LineIDSpecData;
import uk.ac.starlink.splat.data.LineIDTableSpecDataImpl;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.GlobalSpecPlotList;
import uk.ac.starlink.splat.iface.PlotChangedEvent;
import uk.ac.starlink.splat.iface.PlotListener;
import uk.ac.starlink.splat.iface.ProgressPanel;
//import uk.ac.starlink.splat.iface.SpectralLinesPanel;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.vamdc.VAMDCLib;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.TableLoadPanel;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.votable.VOTableBuilder;

public class LineBrowser extends JFrame implements  MouseListener {

    private LinesResultsPanel resultsPanel;
    private SplatBrowser browser;
    private JPanel contentPane;
    private PlotControl plot;
    /**
     * The ProgressFrame. This appears to denote that something is happening.
     */
    static ProgressPanelFrame progressFrame = null;
    
    /**
     *  Reference to GlobalSpecPlotList object.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();
    private SpecData currentLines = null;
    private int SLAP_INDEX=0;
    private int VAMDC_INDEX=1;
    private VAMDCLib vamdc;
    
    /**
     * Frame for adding a new server.
     */
 //   protected AddNewServerFrame addServerWindow = null;
    private LinesQueryPanel linesQuery;

 
  
  /*  public SLAPBrowser(SplatBrowser splatbrowser) {   
      
       this();
       browser=splatbrowser;
       
    }*/

    public LineBrowser(PlotControl pc) {
        contentPane = (JPanel) getContentPane();
        plot = pc;
       // globalList.addPlotListener(this);
      //  frame = new SpectralLinesPanel(this);
        vamdc = new VAMDCLib();
        initFrame();
        initComponents();
        setVisible( true );
    }
    
/*
    private void getSLAPServices() {
                   
            StarTable table = null;
            try {
                     
                table =  TableLoadPanel.loadTable( this, new SSARegistryQueryDialog(SplatRegistryQuery.SLAP), new StarTableFactory() );
             }
            catch ( IOException e ) {
                ErrorDialog.showError( this, "Registry query failed", e );
                return;
            }
           // RowSorter<? extends TableModel> savedSorter = serverTable.getRowSorter();         
           slapServices = new ServerPopupTable(new SLAPServerList(table));
           slapServices.setComponentPopupMenu(makeServerPopup());            
    }
    
    private void getVAMDCServices() {
        
        
        StarTable vamdctab = VAMDCLib.queryRegistry(); 

        vamdcServices = new ServerPopupTable(new VAMDCServerList(vamdctab));
        vamdcServices.setComponentPopupMenu(makeServerPopup());
        
    }
 */   
    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Query for spectral lines" ));
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize(new Dimension(800, 700) );
       // contentPane.add( actionBarContainer, BorderLayout.SOUTH );
      //  setPreferredSize( new Dimension( 600, 500 ) );
       
    }
    



    private void initComponents() {
        
        contentPane.setLayout( new BorderLayout() );
        JSplitPane splitPane = new JSplitPane();  
        splitPane.setOneTouchExpandable( true );     
        splitPane.setOrientation( JSplitPane.HORIZONTAL_SPLIT );
      
      
        // initialize the right and left panels
        linesQuery = new LinesQueryPanel(this);
        //linesQuery = new LinesQueryPanel(initQueryPanel(), initServersPanel());
        splitPane.setDividerLocation( 0.4); //linesQuery.getWidth() );

        splitPane.setLeftComponent( linesQuery );
        splitPane.setRightComponent( initializeResultsComponents() );
        contentPane.add( splitPane, BorderLayout.CENTER );
        contentPane.updateUI();
    }
   
   private  JPanel initializeResultsComponents()
   {
       
   
     resultsPanel = new LinesResultsPanel(this);

     resultsPanel.setBorder(BorderFactory.createTitledBorder("Query results"));
     resultsPanel.setToolTipText( "Results of query to the current list "+
             "of spectral line services. One table per server" );
  
     return resultsPanel;
   
   }

   public void makeQuery( ArrayList<int[]> ranges, ArrayList<double[]> lambdas, String element, String stage ) {

       resultsPanel.removeAllResults();
       ServerPopupTable currentTable=null;

       if (progressFrame != null) {
           progressFrame.closeWindowEvent();
           progressFrame=null;
       }

       if (linesQuery.isSLAPSelected()) {
           progressFrame = new ProgressPanelFrame( "Querying SLAP Services" );     
           currentTable = linesQuery.getSlapTable();
           // makeSlapQuery(ranges, lambda);           
       } else {
           progressFrame = new ProgressPanelFrame( "Querying VAMDC Services" );    
           currentTable = linesQuery.getVamdcTable();
           // makeVamdcQuery(ranges, lambda);
       }
       for ( int r : currentTable.getSelectedRows() ) {
           
           int row=currentTable.convertRowIndexToModel(r);
           final String shortname = currentTable.getShortName(row);
           final String queryString;
           
         
           if (linesQuery.isSLAPSelected()) 
               queryString = makeSlapQuery(ranges, lambdas, element, currentTable.getAccessURL(row));
           else
               queryString= makeVamdcQuery(ranges, lambdas, element, stage, currentTable.getAccessURL(row));

           Logger.info(this, "query= "+queryString);
           final ProgressPanel progressPanel = new ProgressPanel( "Querying: " + shortname );
           progressFrame.addProgressPanel( progressPanel );

           final SwingWorker worker = new SwingWorker()
           {
               boolean interrupted = false;
               public Object construct() 
               {
                   progressPanel.start();
                   try {
                       startQuery( shortname, queryString, progressPanel );
                   }
                   catch (Exception e) {
                       interrupted = true;
                   }
                   return null;
               }


               public void finished()
               {
                   progressPanel.stop();
                   //  Display the results.
                   if ( ! interrupted ) {
                       //        addResultsDisplay( ssaQuery );
                   }
               }
           };
           progressPanel.addActionListener( new ActionListener()
           {
               public void actionPerformed( ActionEvent e )
               {
                   if ( worker != null ) {
                       worker.interrupt();
                   }
               }
           });

           worker.start();  
       }




   }

   private  String makeVamdcQuery( ArrayList<int[]> ranges, ArrayList<double[]> lambdas, String element, String stage, String accessURL) {


       final String query = accessURL+"sync?LANG=VSS2&REQUEST=doQuery&FORMAT=XSAMS&QUERY=";
       String request = "select * where ";

       String wlist="";
       
       for (int spec =0;spec<ranges.size();spec++) {
           int[] range=ranges.get(spec);
           double[] lambda=lambdas.get(spec);
           for (int i=0;i<range.length;i+=2) { // have to convert from meters to angstroms
               wlist+="(RadTransWaveLength >= "+lambda[range[i]]*1E10+" AND RadTransWavelength <= "+lambda[range[i+1]]*1E10+")";
               if (i+1<range.length-1)
                   wlist+=" OR ";
           }
           if (spec<ranges.size()-1)
               wlist+=" OR ";
       }
       String and="";
       if (!wlist.isEmpty()) {    
           request+="("+wlist+")";
           and=" AND ";
       }
       if (! element.isEmpty()) {
           request += and+"(( AtomSymbol = \'"+element+  "\' )";
           request += " or ( MoleculeStoichiometricFormula = \'"+element+  "\' ))";
           and=" AND ";
       }
       if (! stage.isEmpty()) {
           request += and+"( IonCharge = \'"+stage+  "\' )";
       }

       try {
           return query+URLEncoder.encode(request, "UTF-8");
       } catch (UnsupportedEncodingException e) {
           e.printStackTrace();
       }

       return query;

   }

   private String makeSlapQuery( ArrayList<int[]> ranges, ArrayList<double[]> lambdas, String element, String accessURL) {

       final String query = accessURL;
       
       String request="REQUEST=queryData&";
       if (!query.endsWith("?")) {
           request="?"+request;
       }
       
       String wlist="";
       for (int spec =0;spec<ranges.size();spec++) {
           int[] range=ranges.get(spec);
           double[] lambda=lambdas.get(spec);
           for (int i=0;i<range.length;i+=2) {
               wlist+=lambda[range[i]]+"/"+lambda[range[i+1]];
               if (i+1<range.length-1)
                   wlist+=",";
           }
           if (spec<ranges.size()-1)
               wlist+=",";
       }
       String and="";
       if (!wlist.isEmpty()) {    
           request+="WAVELENGTH="+wlist;
           and="&";
       }
       if (! element.isEmpty()) {
           request += and+ "CHEMICAL_ELEMENT="+element;
       }

   /*    try {
           return query+URLEncoder.encode(request, "UTF-8");
       } catch (UnsupportedEncodingException e) {
           e.printStackTrace();
       }*/
       
       return query+request;

   }


    private void startQuery(String shortname, String query, ProgressPanel progressPanel) throws InterruptedException {

        URL queryURL = null;
        URLConnection con = null;
        try {
            queryURL = new URL(query);
            con = queryURL.openConnection();
        } catch (MalformedURLException e) {
            Logger.info(this, "Malformed query URL");
            progressPanel.logMessage("Error");
            return;
        } catch (IOException e) {
            Logger.info(this, "IO Exception when creating query URL");
            progressPanel.logMessage("Error");
            return;
        }
        
        if ( con instanceof HttpURLConnection ) {
            int code = 0;
            try {
                code = ((HttpURLConnection)con).getResponseCode();
            } catch (IOException e) {
                Logger.info(this, e.getMessage());
                return;
            }

            
            if ( code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == HttpURLConnection.HTTP_SEE_OTHER ) {
                String newloc = con.getHeaderField( "Location" );
                //ssaQuery.setServer(newloc);
                URL newurl;
                try {
                    newurl = new URL(newloc);
                    con = newurl.openConnection();
                } catch (MalformedURLException e) {
                    Logger.info(this, "Malformed query URL");
                    progressPanel.logMessage("Error");
                    return;
                   
                } catch (IOException e) {
                    Logger.info(this, "IO Exception when creating query URL");
                    progressPanel.logMessage("Error");
                  
                    return;
                }
            }
            if ( code == HttpURLConnection.HTTP_NO_CONTENT) {
                Logger.info(this, "Query returned no content");
                progressPanel.logMessage("NO CONTENT");
                return;
            }

        }
       


        StarTable startable = null;
        con.setConnectTimeout(10 * 1000); // 10 seconds
        con.setReadTimeout(30*1000);
        try {
          
            if ( linesQuery.isSLAPSelected()) {
                con.connect();
                startable = new StarTableFactory(true).makeStarTable( con.getInputStream(), new VOTableBuilder() );
            } else {
                startable = vamdc.getResultStarTable(query, con.getInputStream());
            }
            
        } catch (IOException e) {
           
            progressPanel.logMessage(e.getMessage());
            
        } catch (Exception e) {
             String msg = e.getMessage();
             if (msg == null)
                 msg="Server error";
            e.printStackTrace();
            progressPanel.logMessage(msg);
            
        }
        
        if ( startable != null &&  startable.getRowCount() > 0 ) {
            StarPopupTable ptable = new StarPopupTable( startable, true );         
                 
            resultsPanel.addTab( shortname, ptable );
            progressPanel.logMessage( startable.getRowCount() + " results returned" );
            resultsPanel.updateUI();        
            contentPane.updateUI();
        } else {
            progressPanel.logMessage( "No results returned" );
        }
        
        if ( Thread.interrupted() ) {
            progressPanel.logMessage( "Interrupted" );
            throw new InterruptedException();
        }
       
    }
    
    
    protected void displayLineSelection(StarJTable table) {
        
                StarTable startable;
                try {
                    startable = makeSubTable(table);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return;
                }
                if (startable != null)                 
                    displayLines(startable);
       
    }

    protected void displayLines(StarTable table) {
        
        if (currentLines != null) {
            plot.removeSpectrum(currentLines);
        }
        try {
            LineIDTableSpecDataImpl impl = new LineIDTableSpecDataImpl(table);
            LineIDSpecData data = new LineIDSpecData(impl);    
           
            //String lineUnits = data.getCurrentDataUnits();
            //String specUnits = plot.getCurrentSpectrum().getCurrentDataUnits();
           // if (! lineUnits.equalsIgnoreCase(specUnits))
           //        SpecDataUnitsFrame.convertToUnits(data, specUnits);
     //       data.setShortName(resultsPanel.getTitleAt(resultsPanel.getSelectedIndex()));
           
            currentLines=data;          
            globalList.addSpectrum(plot, data);     
            
            //browser.addSpectrum(data);
            //browser.addLinesToCurrentPlot(data);
        } catch (SplatException e) {
            
            e.printStackTrace();
            return;
        }
        
       //browser.addSpectrum()
    }

    
    private StarTable makeSubTable(StarJTable table) throws IOException {
        
        int [] selected = table.getSelectedRows();
        if (selected.length == 0) {
            return null;
        }

        // create a new table using the old one as template
        RowListStarTable rlst = new RowListStarTable(table.getStarTable());
       
        for (int i=0; i<selected.length;i++) {
            int selectedRow = table.convertRowIndexToModel(selected[i]);
                rlst.addRow(table.getStarTable().getRow(selectedRow));          
        }
        
        return rlst;

    }


    @Override
    public void mousePressed( MouseEvent e ) {}
    public void mouseReleased( MouseEvent e ) {}
    public void mouseEntered( MouseEvent e ) {}
    public void mouseExited( MouseEvent e ) {}
    public void mouseClicked( MouseEvent e ) {} // TODO

 
/*
    @Override
    public void plotCreated(PlotChangedEvent e) {
       
  //      frame.setPlot(globalList.getPlot(e.getIndex()));       
   //     frame.addRangeList();
        contentPane.updateUI();
        
    }

    @Override
    public void plotRemoved(PlotChangedEvent e) {
   //     if (globalList.plotCount() == 0) {
   //         frame.removeRanges();
   //     } 
        
    }

    @Override
    public void plotChanged(PlotChangedEvent e) {
       // frame.setPlot(globalList.getPlot(globalList.getPlotIndex(e.getIndex())));      
      //  frame.addRangeList();
      //  contentPane.updateUI();
    }

 */ 
    public PlotControl getPlot() {
        return this.plot;
    }

    public void removeLinesFromPlot() {
        if (currentLines != null) {
            plot.removeSpectrum(currentLines);
        }       
    }

 

    
    /**
     * Event listener to trigger a list update when a new server is
     * added to addServerWIndow
     */
 /*   @Override   
    
    public void propertyChange(PropertyChangeEvent pvt)
    {
       
      //  SSAPRegResource reg = new SSAPRegResource(addServerWindow.getShortName(), addServerWindow.getServerTitle(), addServerWindow.getDescription(), addServerWindow.getAccessURL());
    //    slapServices.addNewServer(reg);
      
    }
    
 */   


}
