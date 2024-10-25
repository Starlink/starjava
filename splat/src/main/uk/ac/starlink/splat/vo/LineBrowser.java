package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import jsky.util.Logger;
import jsky.util.SwingWorker;
import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.data.LineIDSpecData;
import uk.ac.starlink.splat.data.LineIDTableSpecDataImpl;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.GlobalSpecPlotList;
import uk.ac.starlink.splat.iface.PlotChangedEvent;
import uk.ac.starlink.splat.iface.PlotListener;
import uk.ac.starlink.splat.iface.ProgressPanel;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.vamdc.VAMDCLib;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.PrimitiveArrayColumn;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.RowSubsetStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.PhysicalConstants;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.votable.VOTableBuilder;

public class LineBrowser extends JFrame implements  MouseListener, PlotListener , PropertyChangeListener  {

  //  public static final StarPopupTable STAR_POPUP_TABLE = ptable;
	private LinesResultsPanel resultsPanel;
//    private SplatBrowser browser;
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
    private int LINETAP_INDEX=2;
    private VAMDCLib vamdc;
    
    // menubar
    protected JMenuBar menuBar;
    
    /**
     * Frame for adding a new server.
     */
 //   protected AddNewServerFrame addServerWindow = null;
    private LinesQueryPanel linesQuery;
	private JComboBox activePlotBox;
	private JPanel plotChoicePanel;
	private boolean zoommode = false;
	private int zoomcol = -1;
	private StarJTable zoomTable = null;
	
	private LineZoomOptionsFrame zoomOptionsFrame = null;
	private Object energyTempColumn;
	private static String  energyTempColumnName = "energy_temp_function";



    public LineBrowser(SplatBrowser splatbrowser, PlotControl pc) {   
       plot = pc;
       init();
      
    }

    public LineBrowser(PlotControl pc) {
    	plot=pc;
    	 init();
       
//         plot = pc;
      
    }
    
    private void init() {
    	 //  Add the menuBar.
        initMenubar();
          

    	 contentPane = (JPanel) getContentPane();
    	 vamdc = new VAMDCLib();
         initFrame();
         initComponents();
         globalList.addPlotListener(this);
         setVisible( true );
         plot.addPropertyChangeListener(this);
    }
    
    private void initMenubar() {
        
        menuBar = new JMenuBar();
        setJMenuBar( menuBar );
        createFileMenu();
        createOptionsMenu();
    }



    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Query for spectral lines" ));
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setPreferredSize(new Dimension(1000, 700) );
        setSize(new Dimension(1000, 700) );
           
    }
    



    private void initComponents() {
        
        contentPane.setLayout( new BorderLayout() );
        JSplitPane splitPane = new JSplitPane();  
        splitPane.setOneTouchExpandable( true );     
        splitPane.setOrientation( JSplitPane.HORIZONTAL_SPLIT );
        splitPane.setResizeWeight(0.8);
      
      
        // initialize the right and left panels
    //    JPanel leftPanel = new JPanel();
        plotChoicePanel = new JPanel();
       
        plotChoicePanel.setBorder(BorderFactory.createEtchedBorder() );
        plotChoicePanel.add (new  JLabel("Select PLOT: "), BorderLayout.LINE_START);
        activePlotBox=new JComboBox();
        updatePlotList();
        activePlotBox.setSelectedIndex(globalList.getPlotIndex(plot));   
        activePlotBox.addActionListener (new ActionListener () {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox src = (JComboBox) e.getSource();
            	if (globalList.plotCount()>0) { 
            		int index = ( src.getSelectedIndex());
            		if (index < 0 ) {
            			index=0;
            		}
            		setPlot( globalList.getPlot(index)); 
            	} 
            	else setPlot(null);
            }
        });
        plotChoicePanel.add(activePlotBox);
        
        linesQuery = new LinesQueryPanel(this);
      //  splitPane.setDividerLocation( 0.5); //linesQuery.getWidth() );

        splitPane.setLeftComponent( linesQuery );
        splitPane.setRightComponent( initializeResultsComponents() );
        splitPane.setContinuousLayout(true);
        splitPane.setResizeWeight(0);
        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_SIZE_PROPERTY, 
        	    new PropertyChangeListener() {
     				@Override
					public void propertyChange(PropertyChangeEvent evt) {
						linesQuery.repaint();
					}
        	});
        contentPane.add( splitPane, BorderLayout.CENTER );
        contentPane.updateUI();
    }
   
   private void updatePlotList() {
	   globalList = GlobalSpecPlotList.getInstance();
	   activePlotBox.removeAllItems();
	   if (globalList.plotCount() == 0) {
		   activePlotBox.addItem("No plots available");
	   } else {
		   for (int i=0;i<globalList.plotCount();i++)  {
			   activePlotBox.addItem(globalList.getPlotName(i));
		   }
	   }
	   activePlotBox.updateUI();
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

   public void makeQuery( ArrayList<int[]> ranges, ArrayList<double[]> lambdas, String species, String charge, String inChiKey) {

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
       } else  if (linesQuery.isLinetapSelected()) {
           progressFrame = new ProgressPanelFrame( "Querying LINETAP Services" );    
           currentTable = linesQuery.getLinetapTable();
       } else {
    	   progressFrame = new ProgressPanelFrame( "Querying VAMDC Services" );    
           currentTable = linesQuery.getVamdcTable();
           // makeVamdcQuery(ranges, lambda);
       }
       for ( int r : currentTable.getSelectedRows() ) {
           
           int row=currentTable.convertRowIndexToModel(r);
           final String shortname = currentTable.getShortName(row);
           final String queryString;
           
           String accessURL=currentTable.getAccessURL(row);
           
           if (linesQuery.isSLAPSelected()) 
               queryString = makeSlapQuery(ranges, lambdas, species, accessURL);
           else if (linesQuery.isLinetapSelected()) {
        // = linesQuery.getLineTapQuery(  currentTable.getTableName(row) );

          queryString = makeLinetapQuery(  currentTable.getTableName(row), ranges, lambdas, species, charge, inChiKey, accessURL);
        	 //  Logger.info(this, "Linetap Query:"+queryString);
               }
           else
               queryString= makeVamdcQuery(ranges, lambdas, species, charge, inChiKey, currentTable.getAccessURL(row));

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
                       startQuery( shortname, queryString, accessURL, progressPanel );
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

   private String makeLinetapQuery(String table, ArrayList<int[]> ranges, ArrayList<double[]> lambdas, String name, String charge, String inchikey, String accessURL) {
       String query = accessURL;
       
       String request="SELECT * from "+table+" WHERE ";
     
     
       // frequency range from selection
       String wlist="vacuum_wavelength";
       for (int spec =0;spec<ranges.size();spec++) { 
    	   int[] range=ranges.get(spec);
    	   
           for (int i=0;i<range.length;i+=2) {
        	   double rangeval []  = getRanges(i, range, lambdas, spec);        	
        		   wlist+=" between "+ rangeval[0]+" and "+rangeval[1];
        	
               if (i+1<rangeval.length-1)
                   wlist+=" OR vacuum_wavelength ";
           }
           if (spec<ranges.size()-1)
               wlist+=" OR vacuum_wavelength  ";
       }
       String and="";
       if (!wlist.isEmpty()) {    
           request+=wlist;
           and=" AND ";
       }
       if (! inchikey.isEmpty()) {
           request += and+ "inchikey ILIKE '%"+inchikey+"%'";
       } else if ( !name.isEmpty()) {
    	   request += and+ "element ILIKE  '"+name+"'";
       		if ( ! charge.isEmpty() && Integer.parseInt(charge) != 0 ) 
       			request += and+ "ion_charge ="+charge;
       }
       

   /*    try {
           return query+URLEncoder.encode(request, "UTF-8");
       } catch (UnsupportedEncodingException e) {
           e.printStackTrace();
       }*/
       
       return request;

}

private double[] getRanges(int index, int[] range, ArrayList<double[]> lambdas, int spec) {
	double [] rangevalue = new double [2]; 
   
    double[] lambda=lambdas.get(spec);
    if (lambda[range[index]] > lambda[range[index+1]]) { // check how spectrum is sorted
    	rangevalue[0] = lambda[range[index+1]];
    	rangevalue[1] = lambda[range[index]];
    } else {
    	rangevalue[0] = lambda[range[index]];
    	rangevalue[1] = lambda[range[index+1]];
    }
	return rangevalue;
}

private  String makeVamdcQuery( ArrayList<int[]> ranges, ArrayList<double[]> lambdas, String element, String stage,String inchiKey, String accessURL) {


       final String query = accessURL+"sync?LANG=VSS2&REQUEST=doQuery&FORMAT=XSAMS&QUERY=";
       String request = "select * where ";

       String wlist="";
       
       for (int spec =0;spec<ranges.size();spec++) { 
    	   int[] range=ranges.get(spec);
    	   
           for (int i=0;i<range.length;i+=2) {// have to convert from meters to angstrom
        	   double [] rangeval = getRanges(i, range, lambdas, spec);      
        	   wlist+="(RadTransWaveLength >= "+rangeval[0]+" AND RadTransWavelength <= "+rangeval[1]+")";
        	  // wlist+=" between "+ rangeval[0]*1E10+" and "+rangeval[1]*1E10;        	
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
           request += and+"(( AtomSymbol = \'"+element+  "\' ))";
         //  request += " or ( MoleculeStoichiometricFormula = \'"+element+  "\' ))";
           and=" AND ";
       }
       
       if (! stage.isEmpty()) {
           request += and+"(( IonCharge = \'"+stage+  "\' ))";
       }
       if (! inchiKey.isEmpty() ) {
    	   request += and+"(( inchiKey = \'"+inchiKey+  "\' ))";
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
        	   double [] rangeval = getRanges(i, range, lambdas, spec); 
               wlist+=rangeval[0]+"/"+rangeval[1];
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


    private void startQuery(String shortname, String query, String accessURL,  ProgressPanel progressPanel) throws InterruptedException {

      
        URLConnection con = null;
        StarTable startable = null;
        
      

        try {  
            if (linesQuery.isLinetapSelected()) {
            	  StarTableFactory tfact = new StarTableFactory();
                  // Initializes TapQuery       
                  TapQueryWithDatalink tq =  new TapQueryWithDatalink( new URL(accessURL), query,  null );
                  startable =  tq.executeSync( tfact.getStoragePolicy(), ContentCoding.NONE ); // to do check storagepolicy              
            	
            } else {
            	  con = checkAndConnect(  query,  progressPanel);
            	if ( linesQuery.isSLAPSelected()) {            	
            		con.connect();
            		startable = new StarTableFactory(true).makeStarTable( con.getInputStream(), new VOTableBuilder() );
            	} else {
                  if (con != null)
                	  startable = vamdc.getResultStarTable(query, con.getInputStream());
            	}
            }
            // reset zoom 
            zoomcol=-1;
            
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
        	addLinesTable(  startable, shortname);           
            progressPanel.logMessage( startable.getRowCount() + " results returned" );
          
        } else {
            progressPanel.logMessage( "No results returned" );
        }
        
        if ( Thread.interrupted() ) {
            progressPanel.logMessage( "Interrupted" );
            throw new InterruptedException();
        }
       
    }
    
    private URLConnection checkAndConnect(String query, ProgressPanel progressPanel) {
    	
    	URL queryURL = null;
    	URLConnection con = null;
    	
        try {
            queryURL = new URL(query);
            con = queryURL.openConnection();
        } catch (MalformedURLException e) {
            Logger.info(this, "Malformed query URL: "+query);
            progressPanel.logMessage("Error");
            return null;
        } catch (IOException e) {
            Logger.info(this, "IO Exception when creating query URL");
            progressPanel.logMessage("Error");
            return null;
        }
        
        if ( con instanceof HttpURLConnection ) {
            int code = 0;
            try {
                code = ((HttpURLConnection)con).getResponseCode();
            } catch (IOException e) {
                Logger.info(this, e.getMessage());
                return null;
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
                    Logger.info(this, "2Malformed query URL: "+query);
                    progressPanel.logMessage("Error");
                    return null;
                   
                } catch (IOException e) {
                    Logger.info(this, "IO Exception when creating query URL");
                    progressPanel.logMessage("Error");
                  
                    return null;
                }
            }
            if ( code == HttpURLConnection.HTTP_NO_CONTENT) {
                Logger.info(this, "Query returned no content");
                progressPanel.logMessage("NO CONTENT");
                return null;
            }

        }
       


        con.setConnectTimeout(10 * 1000); // 10 seconds
        con.setReadTimeout(30*1000);
        
		return con;
	}
    public void addLinesandDisplay( StarTable table, String name) {
    	  
    	addLinesTable(table, name);
   	    displayLines(table);
   }
    
    protected void addLinesTable( StarTable table, String name) {
    	
       
      	 StarPopupTable ptable = new StarPopupTable( table, true ); 
    	
         resultsPanel.addTab( name, ptable );
         resultsPanel.updateUI();        
         contentPane.updateUI();
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
    
    
    
    // only display the lines with higher einsteinA or temperature function values, number increasing when zooming in
    protected  void displayZoomedLines(StarJTable table) {

    	
    	if (zoomTable==null)
    		showZoomOptions();
    	
    		
    	
    	// sort by chosen zoom option (energy/temperature or einstein_a
    	// 
    	
    	if (zoomcol == -1 ) {
    		if (zoomOptionsFrame!= null && zoomOptionsFrame.getStatusOK())
    			prepareZoomParameters(table);
    		else {
    			showZoomOptions();
    		}

    	} else  if (zoomcol > 0) {
    		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(zoomTable.getModel());
        	List<RowSorter.SortKey> sortKeys = new ArrayList<SortKey>();


    		sortKeys.add(new RowSorter.SortKey(zoomcol, SortOrder.DESCENDING));	    
    		sorter.setSortKeys(sortKeys);

    		//table.setRowSorter(sorter);
    		zoomTable.setRowSorter(sorter);
    		sorter.sort();

    		int tableSize = (int) plot.getXScale() * 5;
    		//Logger.info(this, "nrLines="+tableSize);

    		StarTable startable = table.getStarTable();
    		try {
    			startable = makeSubTable( table, 0, tableSize);
    		} catch (IOException e) {

    			e.printStackTrace();

    		}


    		if (startable != null)                 
    			displayLines(startable);
    	}


    }

    protected void displayLines(StarTable table) {
    	
    	if (activePlotBox.getSelectedIndex()==-1 )
    		return;
    	if (activePlotBox.getSelectedItem().toString().equals("No plots available"))
    		return; 
    	
    	plot = globalList.getPlot(activePlotBox.getSelectedIndex());
    
        
        if (currentLines != null) {
            plot.unloadLineIDs();
        }
        try {
        	LineIDTableSpecDataImpl impl = null;
        	if (linesQuery.isLinetapSelected()) {
        		impl = new LineIDTableSpecDataImpl(table, "vacuum_wavelength", null, "title");
        		
        	}
        
        	else 
        		impl = new LineIDTableSpecDataImpl(table);
      
        	

        	DescribedValue xval= table.getParameterByName("xlabel");
        	if (xval != null ) {
        		String xlabel = (String) xval.getValue();
        		xval = table.getParameterByName("xunitstring");
        		String unitstring = (String) xval.getValue();
        		impl.setXparams( xlabel, unitstring );
        	}
        	LineIDSpecData data = new LineIDSpecData(impl);
        	
        	SpecData baseSpectrum = plot.getSpecDataComp().get(0);
        	
        	 // convert X axis to basespectrum units
             int msa = baseSpectrum.getMostSignificantAxis();
             try {
                 FrameSet frameSet = baseSpectrum.getFrameSet();
                 FrameSet lineFrameSet = data.getFrameSet();
                 String unit = frameSet.getUnit(msa);
                 if (isFrequency(unit)) {
                	 
                	 // set system from eavelemgth to freq
                	 lineFrameSet.set("System=freq");
                	 
                 }
               
          
                 msa = data.getMostSignificantAxis();
                 lineFrameSet.set( "unit("+msa+")="+unit);
                 	
                 data.initialiseAst();
                 

             } catch (SplatException e) {
                 // TODO Auto-generated catch block
                 ErrorDialog.showError(this, "Error", e, "Invalid wavelength units");
              
                 return;
             } catch (AstException a ) {
             	ErrorDialog.showError(this, "Error", a, "Invalid wavelength units");               	
            
                 return;
             }
           

        	currentLines=data;          
        	globalList.addSpectrum(plot, data);     

        } catch (SplatException e) {
            // !!! print error message 
            e.printStackTrace();
            return;
        }

    }
    
   

  private boolean isFrequency(String unit) {
		
	  if (unit.toLowerCase().endsWith("hz"))
		return true;
	  return false;
	  
	}

protected void displayOneLine(StarJTable table, int row) {
    	
    	boolean hovermode = true;
    	
    	plot = globalList.getPlot(activePlotBox.getSelectedIndex());
        StarTable subtable;
		try {
			subtable = makeSubTable( table, row );
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		if (subtable == null)
			return;
        
        if (currentLines != null) {
            plot.removeSpectrum(currentLines);
        }
        
        try {
        	LineIDTableSpecDataImpl impl = null;
        	if (linesQuery.isLinetapSelected()) 
        		impl = new LineIDTableSpecDataImpl(subtable, "vacuum_wavelength", null, "title");
        	else {
        		impl = new LineIDTableSpecDataImpl(subtable);
        	}
        	

        	DescribedValue xval= subtable.getParameterByName("xlabel");
        	if (xval != null ) {
        		String xlabel = (String) xval.getValue();
        		xval = subtable.getParameterByName("xunitstring");
        		String unitstring = (String) xval.getValue();
        		impl.setXparams( xlabel, unitstring );
        	}
        	LineIDSpecData data = new LineIDSpecData(impl, hovermode);    

        	currentLines=data;          
        	globalList.addSpectrum(plot, data);     

        } catch (SplatException e) {
            // !!! print error message 
            e.printStackTrace();
            return;
        }

    }
  
  protected void removeOneLine(StarJTable table, int row) {

	  boolean hovermode = true;

	  plot = globalList.getPlot(activePlotBox.getSelectedIndex());
	  StarTable subtable;
	  try {
		  subtable = makeSubTable( table, row );
	  } catch (IOException e1) {
		  // TODO Auto-generated catch block
		  e1.printStackTrace();
		  return;
	  }
	  if (subtable == null)
		  return;
	  try {
		  LineIDTableSpecDataImpl impl = null;
		  if (linesQuery.isLinetapSelected()) 
			  impl = new LineIDTableSpecDataImpl(subtable, "vacuum_wavelength", null, "title");
		  else 
			  impl = new LineIDTableSpecDataImpl(subtable);

		  LineIDSpecData data = new LineIDSpecData(impl, hovermode);    

		  plot.removeSpectrum(data);

	  } catch (SplatException e) {
		  // !!! print error message 
		  e.printStackTrace();
		  return;
	  }
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
    
  // create subtable from the rows between firstRow and lastRow
  private StarTable makeSubTable(StarJTable table, int firstRow, int lastRow) throws IOException {
        
        // create a new table using the old one as template
        RowListStarTable rlst = new RowListStarTable(table.getStarTable());
       
        for (int i=firstRow; i<=lastRow;i++) {
            int row = table.convertRowIndexToModel(i);
                rlst.addRow(table.getStarTable().getRow(row));          
        }
        
        return rlst;

    }
    
 private StarTable makeSubTable(StarJTable table,  int row ) throws IOException {
        

        // create a new table using the old one as template
        RowListStarTable rlst = new RowListStarTable(table.getStarTable());
              
        int tableRow = table.convertRowIndexToModel(row);
        rlst.addRow(table.getStarTable().getRow(tableRow));          
               
        return rlst;

    }


    @Override
    public void mousePressed( MouseEvent e ) {}
    public void mouseReleased( MouseEvent e ) {}
    public void mouseEntered( MouseEvent e ) {}
    public void mouseExited( MouseEvent e ) {}
    public void mouseClicked( MouseEvent e ) {} // TODO

 
    public PlotControl getPlot() {
        return this.plot;
    }

    public void removeLinesFromPlot() {
        if (currentLines != null) {
            plot.removeSpectrum(currentLines);
        }       
    }

	public void setPlot(PlotControl plotControl) {
		plot=plotControl;
		updatePlotList();
		if (plot != null) {
			activePlotBox.setSelectedIndex(globalList.getPlotIndex(plot)); 
			plot.addPropertyChangeListener(this);
			linesQuery.updatePlot(plotControl);
		}
		
	}
	
	
	   private void createFileMenu()
	    {
	        JMenu fileMenu = new JMenu( "File" );
	        fileMenu.setMnemonic( KeyEvent.VK_F );
	        menuBar.add( fileMenu );

	        //  Add action to open a list of spectrum stored in files.
	        ImageIcon openImage =
	            new ImageIcon( ImageHolder.class.getResource( "openfile.gif" ) );
	        LocalAction openAction  = new LocalAction( LocalAction.READ,
	                                                   "Open", openImage,
	                                                   "Open lines file",
	                                                   "control O" );
	        fileMenu.add( openAction ).setMnemonic( KeyEvent.VK_O );

	      
	        //  Add action to open a spectrum using a typed in location (URL).
/*
 	        ImageIcon locationImage =
 
	            new ImageIcon( ImageHolder.class.getResource( "location.gif" ) );
	        LocalAction locationAction  = new LocalAction( LocalAction.LOCATION,
	                                                       "Location",
	                                                       locationImage,
	                                                       "Open location",
	                                                       "control L" );
	        fileMenu.add( locationAction ).setMnemonic( KeyEvent.VK_L );
*/
	      
	        //  Add actions to save a line list to file
	        ImageIcon savelines =
	            new ImageIcon( ImageHolder.class.getResource( "savefile.gif" ) );
	        LocalAction saveAction  =
	            new LocalAction( LocalAction.SAVE, "Save", savelines,
	                             "Save a line list to disk file", "control S" );
	        fileMenu.add( saveAction ).setMnemonic( KeyEvent.VK_S );
	    
	    }
	   
	   private void createOptionsMenu()
	    {
	        JMenu optionsMenu = new JMenu( "Options" );
	        optionsMenu.setMnemonic( KeyEvent.VK_P );
	        menuBar.add( optionsMenu );

	        //  Add action to open a list of spectrum stored in files.
	       
	        LocalAction subtableAction  = new LocalAction( LocalAction.SUBTABLE,
	                                                  "Subtable", null,
	                                                   "create subtable of selected lines",
	                                                   "control T" );
	        optionsMenu.add( subtableAction ).setMnemonic( KeyEvent.VK_T );

	      
	     
	      
	        
	        LocalAction zoomAction= new LocalAction( LocalAction.ZOOM,
	                                                       "zoom",
	                                                       null,
	                                                       "zoom options",
	                                                       "control Z" );
	        optionsMenu.add( zoomAction ).setMnemonic( KeyEvent.VK_Z );

	      
	   
	    
	    }



	
	// implementation of PlotListener 


	@Override
	public void plotCreated(PlotChangedEvent e) {
		updatePlotList();
		PlotControl plot = globalList.getPlot(e.getIndex());
		setPlot(plot);		
	}

	@Override
	public void plotRemoved(PlotChangedEvent e) {
		updatePlotList();	
		if ( activePlotBox.getSelectedIndex() >= 0 && ! activePlotBox.getSelectedItem().toString().equals("No plots available")) {
			PlotControl plot = globalList.getPlot(activePlotBox.getSelectedIndex());
			setPlot(plot);
		}
			
		
	}

	@Override
	public void plotChanged(PlotChangedEvent e) {
		//activePlotBox.setSelectedIndex(e.getIndex());
		
	}

	public JPanel getPlotChoicePanel() {
		
		return plotChoicePanel;
	}

	public void setZoomMode(boolean zoommode) {
		this.zoommode = zoommode;

	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if ( pce.getPropertyName().equals("zoom")) {
			float factor = (float) pce.getNewValue();
			if (factor < 0 ) 
				return;
			resultsPanel.displaySpectra(false);

		} else if ( pce.getPropertyName().equals("zoomOptions")) {
	
				prepareZoomParameters();
		}
	}

	
	private void prepareZoomParameters( ) {
		StarJTable table = resultsPanel.getCurrentTable();
		if (table != null) 
			prepareZoomParameters(table);
	}
	
	private void prepareZoomParameters(StarJTable table ) {
		
		int zoomOption = zoomOptionsFrame.getZoomOption();
		

		if (zoomOption==LineZoomOptionsFrame.TEMP) {
			zoomcol=-1;
			int energyIndex = getZoomColumnIndex(table, "upper_energy");
			if (energyIndex == -1 ) {
				int ei1 = getZoomColumnIndex(table, "initial energy");
				int ei2 = getZoomColumnIndex(table, "final energy");
				
				energyIndex = Math.max(ei1,ei2);

			}
			if (energyIndex != -1 && table != null) {	
				zoomcol= getZoomColumnIndex(table, energyTempColumnName);
				if ( zoomcol == -1 ) {
					zoomTable = (StarJTable) addTableEnergyColumn(table, energyIndex, zoomOptionsFrame.getTemperature() );
					if (zoomTable != null)//zoomcol = table.getColumnCount()-1;
						zoomcol= getZoomColumnIndex(zoomTable, energyTempColumnName);					
				}
				
			}
			else zoomcol = -1;

		}
		

		if (zoomOptionsFrame.getZoomOption() == LineZoomOptionsFrame.PROB) {

			int probIndex = getZoomColumnIndex(table, "einstein\\S*a");
			zoomTable = table;
			if ( probIndex != -1 )
				zoomcol = probIndex;
		}
	}

	
	

	private JTable addTableEnergyColumn(StarJTable table, int energyIndex, long temp) {

		
		int index = getZoomColumnIndex(table, "index");

		Double [] energyTempData = computeEnergyTemp(table,  energyIndex, temp);


		if ( energyTempData != null ) {

			ColumnStarTable tempcol = ColumnStarTable.makeTableWithRows(table.getRowCount());
			ColumnInfo info = new ColumnInfo(energyTempColumnName, Double.class, "Energy Temperature function");
			ArrayColumn col = ArrayColumn.makeColumn( info, energyTempData);
			
			tempcol.addColumn(col);
			StarTable[] tables = new StarTable[2];
			tables[0]=table.getStarTable();
			tables[1]=tempcol;

			JoinStarTable newtable = new JoinStarTable(tables);
			return new StarJTable(newtable, table.hasRowHeader());
		}

		return null;

	}


	private Double [] computeEnergyTemp(StarJTable table, int energycol, long temp) {
		if (temp < 0)
			return null;
		Double[] etd = new Double[table.getRowCount()];
		for (int i = 0; (i < table.getRowCount()); i++) {
			double energy;
			try {
			energy = (double) table.getValueAt(i, energycol);
			} catch (Exception e) {
				// cell is empty - no energy value
				energy=0;
			}
			
			etd[i] = 1/(Math.exp(energy/(PhysicalConstants.BOLTZMANN*temp)));
		}
		return etd;
	}

	private int  getZoomColumnIndex(StarJTable table, String colname) {
		
		int colIndex=-1;
	  		for (int i = 0; (i < table.getColumnCount()&& colIndex <0); i++) {
	  			String name=table.getColumnName(i);
	  			if (name.toLowerCase().matches(colname)  ) {
	  				colIndex = i;	  			
	  			}	  			
	  		}
		return colIndex;	  	   

	}
	
	private void showZoomOptions() {
		
	   if ( zoomOptionsFrame == null ) {
            zoomOptionsFrame = new LineZoomOptionsFrame();
            zoomOptionsFrame.addPropertyChangeListener(this);
       } 
	   zoomOptionsFrame.setVisible(true);
	}
	
	
	

	public void createSubtable() {
		
	  StarJTable table = 	resultsPanel.getCurrentTable();
	  StarTable startable = table.getStarTable();
	 
	  int[] intselection =   table.getSelectedRows();
	  if (intselection.length==0)
		  return;
	  
	  
	  RowListStarTable  subtable = new RowListStarTable(startable);
	  
	  for (int i=0;i<intselection.length;i++) {
		  
		try {
		    int row = table.convertRowIndexToModel(intselection[i]);
			Object[] tablerow = startable.getRow((long) row);
			subtable.addRow(tablerow);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  
		  
	  }
	  
	//  BitSet rowmask = BitSet.valueOf(selection);
	//  RowSubsetStarTable subtable =  new RowSubsetStarTable( table.getStarTable(), rowmask ) ;
	  resultsPanel.addTab("Selection", new StarPopupTable(subtable, table.hasRowHeader()));
	  
	}


    //
    // LocalAction to encapsulate all trivial local Actions into one class.
    //
    class LocalAction
    extends AbstractAction
    {
        //  Types of action.
       
        public static final int SAVE = 1;
        public static final int READ = 2;
  //      public static final int LOCATION = 3;
        public static final int SUBTABLE = 4;
        public static final int ZOOM = 5;
              

        //  The type of this instance.
        private int actionType = 0;

        public LocalAction( int actionType, String name )
        {
            super( name );
            this.actionType = actionType;
        }

        public LocalAction( int actionType, String name, Icon icon,
                String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );
            this.actionType = actionType;
        }

        public LocalAction( int actionType, String name, Icon icon,
                String help, String accel )
        {
            this( actionType, name, icon, help );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( accel ) );
        }

        public void actionPerformed( ActionEvent ae )
        {
            switch ( actionType )
            {
                
                case SAVE: {
           //         resultsPanel.saveLinesToFile();
                    break;
                }
                case READ: {
          //          readLinesFromFile();
                    break;
                }
                case SUBTABLE: {
                    createSubtable();
                    break;
                }
                case ZOOM: {
                    showZoomOptions();
                    break;
                }
            }
        }

       
		
    }


 

}
