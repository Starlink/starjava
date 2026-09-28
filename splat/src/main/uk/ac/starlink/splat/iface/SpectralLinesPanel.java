package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.data.LineIDTableSpecDataImpl;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.vo.LineBrowser;
import uk.ac.starlink.splat.vo.LineTapParameters;
import uk.ac.starlink.splat.vo.LinesQueryPanel;
import uk.ac.starlink.util.gui.ErrorDialog;



public class SpectralLinesPanel extends JPanel implements  ActionListener, DocumentListener, PropertyChangeListener {
	

	private String chosenSpecies;
    /** UI preferences. */
    protected static Preferences prefs =
        Preferences.userNodeForPackage( SpectralLinesPanel.class );

    /**  Logger. */
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.splat.iface.SpectralLinesPanel" );

    /*  Reference to the info window */
    
    private static InfoWindow infoWindow; 
    
 
    /**
     * Action buttons container.
     */
 //   protected JPanel actionBarContainer = new JPanel();
 //   protected JPanel topActionBar = new JPanel();
 //   protected JPanel midActionBar = new JPanel();
 //   protected JPanel botActionBar = new JPanel();

    /**
     *  Menubar and various menus and items that it contains.
     */
//    protected JMenuBar menuBar = new JMenuBar();
//    protected JMenu fileMenu = new JMenu();
      protected JMenu rangeMenu = new JMenu();
//    protected JMenuItem closeFileMenu = new JMenuItem();

    /**
     *  The PlotControlFrame that specifies the current spectrum.
     */
    protected PlotControl plot = null;

    /**
     *  Number of fits done so far (used as unique identifier)
     */
    protected static int fitCounter = 0;

    /**
     *  Ranges of data that are to be fitted.
     */
    protected JPanel rangePanel;
    protected JPanel queryPanel;
    protected XGraphicsRangesView rangeList = null;
    protected XGraphicsRange range = null;
    
  

     /**
     *  Label for results area.
     */
    protected TitledBorder lineResultsTitle =
        BorderFactory.createTitledBorder( "Lines found:" );


    protected LineBrowser browser;
    protected LinesQueryPanel lqPanel;

  
    double[] lambda2 = null;

    private JTextField chargeField;
    private String inChiKey="";
    private String element="";
    AutoFillCombo elementCombo = null;
    AutoFillCombo moleculeCombo = null;
 

    int width;
//    int height;
    
    String [] units = {"Angstrom","nm","mm","µm","m","THz", "GHz", "MHz", "Hz", "J", 
    				   "erg", "eV", "KeV", "m/s","Km/s","1/m", "1/cm"};
    String [] parameters = {"title","wavelength","wavelength_error","method","ion_charge","mass_number", "upper_energy", "lower_energy", "inchi", "inchikey", 
			   "einstein_a", "xsams_uri", "line_reference"};


	private JComboBox<String> wlUnitsCombo;

	private JComboBox<String> energyUnitsCombo;
	
	private LineTapParameters lineTap;

	private JButton queryButton;
	private JTabbedPane queryModePanel;
	private JTextArea queryTextArea;
	private JPanel advancedQueryPanel;
	private JTextField maxrecField;


    /**
     * Create an instance.
     * @param WIDTH 
     */
 
    public SpectralLinesPanel(LinesQueryPanel lqpanel, LineBrowser LineBrowser, int width) 
    {
        browser = LineBrowser;
        lqPanel = lqpanel;
        
        this.plot = browser.getPlot();
        this.width=width;
       // this.height = height;
       // this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEtchedBorder() );
     	initUIComponents();
     	rangePanel = getRangePanel();
     	queryPanel = getQueryPanel();
        initUI(rangePanel, queryPanel);     
        //this.add(BorderLayout.PAGE_START, contentPane);
        lineTap = new LineTapParameters();
      //  LinesQueryPanel lqPanel = browser.getLinesQueryPanel();
        lqPanel.addPropertyChangeListener("selectionChanged", evt -> {
            elementCombo.onSelectionChanged((Boolean) evt.getNewValue());
            moleculeCombo.onSelectionChanged((Boolean) evt.getNewValue());
        });

       
    }
    

    /**
     * Get the PlotControlFrame that we are using.
     *
     * @return the PlotControlFrame
     */
    public PlotControl getPlot()
    {
        return plot;
    }

    /**
     * Set the PlotControlFrame that has the spectrum that we are to
     * fit.
     *
     * @param plot the PlotControlFrame reference.
     */
    public void setPlot( PlotControl  plot )
    {
        this.plot = plot;
        if (rangeList != null) 
            rangeList.setPlot(plot.getPlot());
    }

    /**
     * Initialise the main part of the user interface.
     * @param jPanel 
     * @param jComponent can be a JPanel or a JScrollPane
     */
    protected void initUI(JComponent rPanel, JComponent qPanel)
    {
 
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth=1;
       // gbc.gridheight=GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx=1;
        gbc.weighty=0.5;
        gbc.gridx=0;  
        gbc.gridy=0;
     
      
        add(rPanel, gbc);
       
        
        //  gbc.anchor = GridBagConstraints.SOUTHWEST;
 
        gbc.gridy=1;
        //gbc.fill = GridBagConstraints.BOTH;
        
        add(qPanel, gbc);
       
        JPanel buttonPanel=new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc1.weightx=1;//0.5;
        gbc1.weighty=0;
        gbc1.gridx=0;  
        gbc1.gridy=0;
        
        JLabel maxrecLabel = new JLabel("Maxrec:");
        gbc1.gridx = 0;
        gbc1.anchor = GridBagConstraints.EAST;
        gbc1.insets = new Insets(0, 0, 0, 5);  
        buttonPanel.add(maxrecLabel, gbc1);

        maxrecField = new JTextField(6); 
        maxrecField.addActionListener(this);
        maxrecField.getDocument().putProperty("owner", maxrecField); //set the owner
        maxrecField.getDocument().addDocumentListener( this );
        
        gbc1.gridx = 1;
        gbc1.anchor = GridBagConstraints.WEST;
        gbc1.insets = new Insets(0, 0, 0, 15);  // gap before the Query button
        buttonPanel.add(maxrecField, gbc1);
 
        queryButton = new JButton( new QueryAction("Query"));
        queryButton.setToolTipText( "Search for spectral lines" ); 
        gbc1.anchor = GridBagConstraints.CENTER;
        // gbc.fill=GridBagConstraints.HORIZONTAL;
        gbc1.gridx=2;
        
        buttonPanel.add(queryButton, gbc1);
        
      
        
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridheight=GridBagConstraints.REMAINDER;
        gbc.gridy=2;
        gbc.weightx=0;
        gbc.fill=GridBagConstraints.HORIZONTAL;
        
        add(buttonPanel, gbc);
        

        this.repaint();
       
    }
    
    /**
     * Initialise the  range panel
     */
    protected JPanel getRangePanel()
    {
    	  //  List of regions of spectrum where to search for lines.
    	
  	  rangePanel = new JPanel();

      if (plot != null ) {
     	 rangeList = new XGraphicsRangesView( plot.getPlot(), rangeMenu, Color.LIGHT_GRAY, true, null,  true ); 
     	 
         rangeList.setPreferredSize(new Dimension(width-10,120));       
         rangePanel.add(rangeList, BorderLayout.PAGE_START);    	
     }
     return rangePanel;
    	
    }
    
 
    
    protected  JPanel getQueryPanel()
    {
    
    	   // guided or advanced query
    	 
    	   queryModePanel = new JTabbedPane();
    	   String queryTemplate =  "[SELECTED_SERVICE_URL]?";


    	   // atoms or molecules
    	   JTabbedPane speciesQueryPanel = new JTabbedPane();
    	   
    	   GridBagConstraints gbc1 = new GridBagConstraints();
           gbc1.anchor = GridBagConstraints.LINE_START;
           gbc1.fill = GridBagConstraints.NONE;
           gbc1.weightx=0;
           gbc1.weighty=0;
           gbc1.gridx=0;  
           gbc1.gridy=0;
    	   
    	   // advanced query panel
    	   advancedQueryPanel = new JPanel(new GridBagLayout());

     	   queryTextArea = new JTextArea(5,30);
     	   queryTextArea.setLineWrap(true);        // enables wrapping at all
     	   queryTextArea.setWrapStyleWord(true);
     	   queryTextArea.setToolTipText("complete the SLAPV2 query. \nDO not change [SELECTED_SERVICE_URL].\n Selected services will be queried ");
    	   queryTextArea.setText(queryTemplate);
    	  
    	   
    	   JPanel advancedHeaderPanel = new JPanel(new BorderLayout());
    	   advancedHeaderPanel.add(new JLabel("type your query:"), BorderLayout.LINE_START);
    	  
    	   
    	   JCheckBox showInfoCheckBox = new JCheckBox("show Info");
    	   showInfoCheckBox.addActionListener(new ActionListener() {
               @Override
               public void actionPerformed(ActionEvent e) {
                   if (showInfoCheckBox.isSelected()) {
                       infoWindow = new InfoWindow(); // Open new window
                   } else {
                       if (infoWindow != null) {
                           infoWindow.dispose(); // Close the window
                       }
                   }
               }
           });
    	   JButton clearButton = new JButton( "clear");
    	   clearButton.addActionListener(new ActionListener() {
               @Override
               public void actionPerformed(ActionEvent e) {
            	   queryTextArea.setText(queryTemplate);
               }
           });
    	   
    	 
    	   advancedHeaderPanel.add(showInfoCheckBox, BorderLayout.CENTER);
    	   advancedHeaderPanel.add(clearButton, BorderLayout.LINE_END);
    	   
    	   advancedQueryPanel.add(advancedHeaderPanel);
    	   gbc1.gridy=1;
    	   advancedQueryPanel.add(queryTextArea, gbc1);
    	   queryTextArea.setText("[SELECTED_SERVICE_URL]?");
    	  // JButton sendQueryButton = new JButton("Search");
    	  // advancedQueryPanel.add(sendQueryButton);
    	   
       	   JPanel elementQueryPanel = new JPanel(new GridBagLayout());
    	   
           elementQueryPanel.setBorder(BorderFactory.createEtchedBorder() );
         
    
           gbc1.weightx=0;
           gbc1.weighty=0;
          // gbc1.gridwidth = GridBagConstraints.REMAINDER;
           gbc1.gridx=0;  
           gbc1.gridy=0;
           
     
           elementCombo = new AutoFillCombo("element:", false, browser);
           
           elementCombo.addPropertyChangeListener(this);
           
         

           elementQueryPanel.add(elementCombo, gbc1);
           gbc1.gridy=1;
           elementQueryPanel.add(makeLabelFieldPanel("charge:", chargeField ), gbc1);
           
    	   
           speciesQueryPanel.addTab("ATOMS", null, elementQueryPanel, "Atomic lines");
                      
           JPanel moleculeQueryPanel = new JPanel(new GridBagLayout());
           
           moleculeCombo = new AutoFillCombo("molecule:", true, browser );
           moleculeCombo.addPropertyChangeListener(this);     
           gbc1.gridx=0;
           gbc1.gridy=1;
          
           moleculeQueryPanel.add(moleculeCombo, gbc1);
          
           speciesQueryPanel.addTab("MOLECULES", null, moleculeQueryPanel, "Molecular lines");
           
           queryModePanel.addTab("Species Query", null, speciesQueryPanel);
           
           queryModePanel.addTab("Advanced Query", null, advancedQueryPanel);
           
           
           JPanel pan = new JPanel();
           pan.add(queryModePanel, BorderLayout.LINE_START);
           
           return pan;
         
    }
    
 

	public void reloadUI( boolean islinetap ) {
    	
    //	this.removeAll();
    	
    	if (islinetap) {
    		this.lineTap.setRanges( rangeList.getRanges(true));
    		//initUI(rangePanel, getLineTapQueryPanel() );
    	}
    	else {
    //		initUI(rangePanel, getQueryPanel());
    	}
    	this.repaint();
    }
  
    private void initUIComponents() {

      //  elementField = new JTextField(10);
        chargeField = new JTextField("",5);  
        
        chargeField.setColumns(5);
        
        // = new JTextField(15);
        
      //  chargeFiel.setEditable(true);
        chargeField.addActionListener(this);
        chargeField.setToolTipText("Ionization charge / ion charge.");
   //     chargeField.setPreferredSize(new Dimension(100, 20));
        chargeField.getDocument().putProperty("owner", chargeField); //set the owner
        chargeField.getDocument().addDocumentListener( this );
        
        

        wlUnitsCombo = new JComboBox<String>(units);
        wlUnitsCombo.setSelectedItem("Angstrom");
 //       wlUnitsCombo.addActionListener(this);
        energyUnitsCombo = new JComboBox<String>(units);
        energyUnitsCombo.setSelectedItem("J");
 //       energyUnitsCombo.addActionListener(this);
        queryPanel = getQueryPanel();
        rangePanel = getRangePanel();
		
	}

    public void queryLines( ) {
    	int index=queryModePanel.getSelectedIndex();
    	if (queryModePanel.getTitleAt(index).startsWith("Advanced")) {
    		if (queryTextArea.getText() != null && ! queryTextArea.getText().isEmpty() ) {
    			 queryLinesAdvanced(queryTextArea.getText());
    		} 
    	} else {
    			queryLinesGuided();
    		   	  
    	}	
    }
    
    private void queryLinesAdvanced(String query) {
    	 // get the selected table
    	// need to adapt to SLAP2 query
    	
    	// Pattern pattern = Pattern.compile("(?i)\\bFROM\\s+([a-zA-Z0-9_.]+)");
        // Matcher matcher = pattern.matcher(query);
         
        
         //if (matcher.find()) {
         //    table = matcher.group(1); // Return the table name
         //} 
    	 browser.makeQuery(query.trim().replaceAll("\\r|\\n", "")); 
    	 
    }

	private void queryLinesGuided() {
		

        ArrayList<double []> lambdas=new ArrayList<double[]>();
        ArrayList<int []> ranges=new ArrayList<int[]>();
        SpecDataComp spectra = plot.getSpecDataComp();
        
      
     
        // all spectra in same plot = same ranges -> get first
            int i=0;
            SpecData spectrum = spectra.get(i);
            if (spectrum.getSpecDataImpl().getClass()!=LineIDTableSpecDataImpl.class) { //ignore existing lines 


                boolean ok=true;
                double [] lambda = spectrum.getXData();
              
                
                // create a copy of the spectrum, so coordinate conversions won't affect the plot
              
                SpecData sd=spectrum.getCopy("copy");
                
                
                // convert X axis to angstrom
                
                
                int msa = sd.getMostSignificantAxis();
                try {
                    FrameSet frameSet = sd.getFrameSet();
                    String unit = frameSet.getUnit(msa);    
                    
                    String sys = frameSet.getC("System");
                    logger.info("system=WAVE,unit("+msa+")=angstrom  "+ sys );
                    frameSet.set( "system=WAVE,unit("+msa+")=angstrom" );
                    sd.initialiseAst();
                    

                } catch (SplatException e) {
                    // TODO Auto-generated catch block
                    ErrorDialog.showError(this, "Error", e, "Invalid wavelength units");
                    ok=false;
                    return;
                } catch (AstException a ) {
                	ErrorDialog.showError(this, "Error", a, "Invalid wavelength units");               	
                    ok=false;
                    return;
                }
                
                double[] lambda2 = sd.getXData();
          
                
                int[] ranges2 = rangeList.extractRanges( true, true, lambda);
                if ( ok && ranges2 != null && ranges2.length > 0 && ranges2[0]!=ranges2[1]) {
                	
                    if (ranges2[1]>=lambda2.length) // avoids exception in case the unit conversion changed the size of the lambda vector
                        ranges2[1]=lambda2.length-1;
                    
                    lambdas.add(lambda2);
                    
                    ranges.add(ranges2);
                }
            }
            
//        }
   
        browser.makeQuery(ranges, lambdas, getSpecies(), getCharge(),  getInChiKey(), getMaxRec());       
	}


	


	public void addRangeList() {


            rangeList = new XGraphicsRangesView( plot.getPlot(), rangeMenu );
           // rangeList.setPreferredSize(new Dimension(300,50));
            rangePanel.removeAll();
            rangePanel.add(new JLabel("Wavelength ranges:"));
          //  range = new XGraphicsRange( plot.getPlot(), null, Color.blue , true );
            rangePanel.add(rangeList);
           // rangePanel.updateUI();
               
    }
   
    public LineBrowser getBrowser() {
        return browser;
    }


    public void setBrowser(LineBrowser browser) {
        this.browser = browser;
    }


    public double[] getPlotRanges() {
        return rangeList.getRanges(false);
    }
    
    public double[] getSelectedRanges() {
        return rangeList.getRanges(true);
    }

    public double[] getWavelengths() {
        return lambda2;
    }

    public String getSpecies() {
    	
        return element;
    }
    
    public String getInChiKey() {
    	return inChiKey;
    }
    

    public String getCharge() {
    	 return  chargeField.getText();
    	  	
    }
    
    public String getEnergyUnit() {
    	 return (String) energyUnitsCombo.getSelectedItem();
    }
    
    public String getWavelengthUnit() {
   	 return (String) wlUnitsCombo.getSelectedItem();
    }
    
    public String getMaxRec() {
   	 return  maxrecField.getText();
   	  	
    }


    private JPanel makeLabelFieldPanel (String labelstr, JComponent component) {
   
    	JLabel label = new JLabel(labelstr);
        JPanel fp = makeHorizontalPanel( label, component, true);
       
        Dimension d = fp.getPreferredSize();
        d.width = 10;
        fp.setMaximumSize( d );
        return fp;
  
    }
    
    private JPanel makeHorizontalPanel(JComponent c1, JComponent c2, boolean border) {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill=GridBagConstraints.NONE;
        
		if (border) 
			panel.setBorder(BorderFactory.createEtchedBorder() );		
	  //   	panel.setLayout(new BorderLayout());
	//    JLabel filler = new JLabel("  ");
	   	gbc.gridx=0; gbc.gridy=0;
	  	panel.add(c1, gbc);
	  	gbc.gridx=1;
	    panel.add(c2,  gbc);
	  //  panel.add(filler,  BorderLayout.EAST);
	   
	
	 
	    return panel;
	}


    /**
     * Fit selected action. Performs fit to the selected ranges.
     */
  protected class QueryAction extends AbstractAction
    {
	public QueryAction( String name ) {
            super( name );
           // putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control S" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
        	JButton button = (JButton) ae.getSource();
        		queryLines();
        	
        }
    }
    
    /*
     * Deactivate ion charge component
     */
    public void deactivateCharge() {
	chargeField.setEnabled(false);
    }  

    /*
     * Activate ion charge component
     */
    public void activateCharge() {
	chargeField.setEnabled(true);
    }  


    public void removeRanges() {
        rangePanel.removeAll();
  //      rangePanel.add(rangePlaceHolder);
        
    }


    @Override
    public void actionPerformed(ActionEvent e) {
    	
    	//to do test if combobox
    	JComboBox cb = (JComboBox) e.getSource();
    	chosenSpecies = (String) cb.getSelectedItem(); 
        
    }


	public void updatePlot(PlotControl plotControl) {
		this.plot = plotControl;
		rangeList.setPlot(plot.getPlot());
		rangeList.deleteAllRanges();
	}


	@Override
	public void insertUpdate(DocumentEvent e) {
		changedUpdate(e);		
	}


	@Override
	public void removeUpdate(DocumentEvent e) {
		changedUpdate(e);
		
	}


	@Override
	public void changedUpdate(DocumentEvent e) {
	
	      Object owner = e.getDocument().getProperty("owner");
	      if (owner == chargeField) {
	     
	    	    String chargeText = chargeField.getText();
                
                if ( chargeText != null && chargeText.length() > 0 ) {
                    try {
                        int ioncharge = Integer.parseInt( chargeText );
                    }
                    catch (NumberFormatException e1) {
                        chargeField.setForeground(Color.red);
                        //ErrorDialog.showError( this, "Cannot understand maxRec value", e1);                         
                        return;
                    }
                    chargeField.setForeground(Color.black);
                }
	      } else if (owner == maxrecField) {
                
                String maxrecText = maxrecField.getText();
                
                if (maxrecText != null && maxrecText.length() > 0) {
                    try {
                        int maxrec = Integer.parseInt(maxrecText);
                        if (maxrec < 0) {
                            maxrecField.setForeground(Color.red);
                            return;
                        }
                    }
                    catch (NumberFormatException e1) {
                        maxrecField.setForeground(Color.red);
                        return;
                    }
                    maxrecField.setForeground(Color.black);
                }          
		  }
	}


	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		Object src = evt.getSource();
		if (src.equals(moleculeCombo)) {
			inChiKey=moleculeCombo.getInChiKey();
			if (element != null && ! element.isEmpty())
					inChiKey.trim();
			
		}
		else if (src.equals(elementCombo)) {
			
			element = elementCombo.getElement().trim();
			inChiKey="";
		
			if (element != null && ! element.isEmpty() && element.contains("-")) {
				int dash=element.indexOf('-');
				element = element.substring(0,dash);
			}
		}
		
	}
	// Separate class for the information window
	class InfoWindow extends JFrame {
		public InfoWindow() {
			setTitle("SLAPV2 Quantities");
			setSize(600, 300);
			setLocationRelativeTo(null); // Center the window
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Close only this window


			// Add the text to the window
			add(getInfoPanel());

			// Show the window
			setVisible(true);
		}
		// show information useful for writing an adql query
		private JScrollPane getInfoPanel() {

			// The SLAPV2 Quantities - todo: later 
			// read from a file that can be updated

			JTextPane infoText =  new JTextPane();
			infoText.setContentType("text/html");

			String info = "SLAPV2 Quantities<BR>"
		+ "<HTML><table class=\"tabular\" cellpadding=\"0\" cellspacing=\"0\">"

        + "<tr><th width=20 align=left><b>Parameter</b></th><th align=left>Unit</th><th align=\"left\" width=60><b>Short Description</b></th><th align=\"left\">Example</th></tr>"

        + "<tr><td align=\"left\"><tt>WAVELENGTH</tt></td>"
        + "<td align=\"left\">m (vacuum)</td>"
        + "<td align=\"left\">Spectral range</td>"
        + "<td align=\"left\">WAVELENGTH=5.1E-6 5.6E-6</td></tr>"

        + "<tr><td align=\"left\"><tt>SPECIES</tt></td>"
        + "<td align=\"left\">&mdash;</td>"
        + "<td align=\"left\">Species name or formula</td>"
        + "<td align=\"left\">SPECIES=CO2</td></tr>"

        + "<tr><td align=\"left\"><tt>SPECIES_MASS</tt></td>"
        + "<td align=\"left\">u (unified atomic mass unit)</td>"
        + "<td align=\"left\">Species mass range</td>"
        + "<td align=\"left\">SPECIES_MASS=0 12.011</td></tr>"

        + "<tr><td align=\"left\"><tt>INCHIKEY</tt></td>"
        + "<td align=\"left\">&mdash;</td>"
        + "<td align=\"left\">InChIKey of species</td>"
        + "<td align=\"left\">INCHIKEY=XEEYBQQBJWHFJM-UHFFFAOYSA-N</td></tr>"

        + "<tr><td align=\"left\"><tt>ION_CHARGE</tt></td>"
        + "<td align=\"left\">integer</td>"
        + "<td align=\"left\">ion charge range</td>"
        + "<td align=\"left\">SPECIES=Fe&amp;ION_CHARGE=1</td></tr>"

        + "<tr><td align=\"left\"><tt>LOWER_LEVEL_ENERGY</tt></td>"
        + "<td align=\"left\">J (Joules)</td>"
        + "<td align=\"left\">lower level energy range</td>"
        + "<td align=\"left\">LOWER_LEVEL_ENERGY=3.93E-18 3.94E-18</td></tr>"

        + "<tr><td align=\"left\"><tt>UPPER_LEVEL_ENERGY</tt></td>"
        + "<td align=\"left\">J (Joules)</td>"
        + "<td align=\"left\">upper level energy range</td>"
        + "<td align=\"left\">UPPER_LEVEL_ENERGY=3.93E-18 3.94E-18</td></tr>"

        + "<tr><td align=\"left\"><tt>EINSTEINA</tt></td>"
        + "<td align=\"left\">s<sup>-1</sup></td>"
        + "<td align=\"left\">Einstein A  range</td>"
        + "<td align=\"left\">EINSTEINA=1.1E-7 1.2E-7</td></tr>"

        + "<tr><td align=\"left\"><tt>MAXREC</tt></td>"
        + "<td align=\"left\">&mdash;</td>"
        + "<td align=\"left\">Max Recirds returned</td>"
        + "<td align=\"left\">MAXREC=10</td></tr>"

        + "</table>";



			infoText.setText(info);
			infoText.setEditable(false); // Make it read-only
			infoText.setOpaque(false);   // Blend with background
			infoText.setBackground(Color.WHITE);
			JScrollPane scrollPane = new JScrollPane(infoText);
			scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

			return scrollPane;
		}

	}

	public void setAdvancedTab(boolean slap2Selected) {		

		if (slap2Selected) {
			queryModePanel.addTab("Advanced Query", null, advancedQueryPanel);

		} else {
			for (int i = 0; i < queryModePanel.getTabCount(); i++) {
				if (queryModePanel.getTitleAt(i).equals("Advanced Query")) {
					queryModePanel.removeTabAt(i);
					break;  // Stop after removing the first matching tab
				}
			}
		}
	}
}

