package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import uk.ac.starlink.util.gui.ErrorDialog;



public class SpectralLinesPanel extends JPanel implements  ActionListener, DocumentListener, PropertyChangeListener {
	
	
	
	private String SPECIESDB_URL = "http://dc.zah.uni-heidelberg.de/tap";
	private String SPECIES_TABLE = "species.main";
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


  

    /**
     * Create an instance.
     * @param WIDTH 
     */
 
    public SpectralLinesPanel(LineBrowser LineBrowser, int width) 
    {
        browser = LineBrowser;
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
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc1.weightx=1;//0.5;
        gbc1.weighty=0;
        gbc1.gridx=0;  
        gbc1.gridy=0;
 
        queryButton = new JButton( new QueryAction("Query"));
        queryButton.setToolTipText( "Search for spectral lines" ); 
        gbc1.anchor = GridBagConstraints.CENTER;
        // gbc.fill=GridBagConstraints.HORIZONTAL;
        gbc1.gridx=1;
        
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
    	   advancedHeaderPanel.add(showInfoCheckBox, BorderLayout.LINE_END);
    	   
    	   advancedQueryPanel.add(advancedHeaderPanel);
    	   gbc1.gridy=1;
    	
     	   queryTextArea = new JTextArea(5,30);
    	   advancedQueryPanel.add(queryTextArea, gbc1);
    	  // JButton sendQueryButton = new JButton("Search");
    	  // advancedQueryPanel.add(sendQueryButton);
    	   
       	   JPanel elementQueryPanel = new JPanel(new GridBagLayout());
    	   
           elementQueryPanel.setBorder(BorderFactory.createEtchedBorder() );
         
    
           gbc1.weightx=0;
           gbc1.weighty=0;
          // gbc1.gridwidth = GridBagConstraints.REMAINDER;
           gbc1.gridx=0;  
           gbc1.gridy=0;
           
     
           elementCombo = new AutoFillCombo("element:", false);
           
           elementCombo.addPropertyChangeListener(this);
           
         

           elementQueryPanel.add(elementCombo, gbc1);
           gbc1.gridy=1;
           elementQueryPanel.add(makeLabelFieldPanel("charge:", chargeField ), gbc1);
           
    	   
           speciesQueryPanel.addTab("ATOMS", null, elementQueryPanel, "Atomic lines");
                      
           JPanel moleculeQueryPanel = new JPanel(new GridBagLayout());
           
           moleculeCombo = new AutoFillCombo("molecule:", true);
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
        chargeField.setToolTipText("Ionization charge / ion charge. Not supported by SLAP)");
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
    	 Pattern pattern = Pattern.compile("(?i)\\bFROM\\s+([a-zA-Z0-9_.]+)");
         Matcher matcher = pattern.matcher(query);
         
         String table="";
         if (matcher.find()) {
             table = matcher.group(1); // Return the table name
         } 
    	 browser.makeQuery(query.trim().replaceAll("\\r|\\n", ""), table); 
    	 
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
   
        browser.makeQuery(ranges, lambdas, getSpecies(), getCharge(), getInChiKey());       
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
		// TODO Auto-generated method stub
	      Object owner = e.getDocument().getProperty("owner");
	      if(owner != null){
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
		
			if (element != null && ! element.isEmpty() && element.contains("-")) {
				int dash=element.indexOf('-');
				element = element.substring(0,dash);
			}
		}
		
	}
	// Separate class for the information window
	class InfoWindow extends JFrame {
	    public InfoWindow() {
	        setTitle("LineTap Quantities");
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
			
	   	       // The Linetap Quantities - todo: later 
	    	   // read from a file that can be updated
	          	
		        JTextPane infoText =  new JTextPane();
		        infoText.setContentType("text/html");
		        
		        String info = "Linetap Quantities<BR>"
		        + "<HTML><table class=\"tabular\" cellpadding=\"0\" cellspacing=\"0\">"
		        
		        +"<tr><th width=20 align=left><b>Name [Unit]</b> </th><th width=10 align=left>UCD</th><th align=left ><b>Type</b></th><th align=\"left\" width=60><b>Description</b></th></tr>"

		        +"<tr><td align=\"left\"><tt>title</tt>"
		   
		        + " </td><td align=left>meta.id</td><td width=\"0\"><b>text</b> </td><td align=\"left\"> Human-readable line designation.</td></tr>"

		        + "<tr><td align=\"left\"/></tr>"

		        + "<tr><td align=\"left\"><tt>vacuum_wavelength</tt> [Å] "
		   
		        + "</td><td align=left>em.wl</td><td width=\"0\"><b>float</b> </td><td align=\"left\"> Vacuum wavelength of the transition</td></tr>"

		        + "<tr><td   align=\"left\"/></tr>"

		        + "<tr><td align=\"left\"><tt>vacuum_wavelength_error</tt> [Å] "
		   
		        + "</td><td align=left>stat.error<br>em.wl</td><td width=\"0\">float </td><td align=\"left\"> Total error in vacuum_wavelength</td></tr>"

		        + "<tr><td   align=\"left\"/></tr>"

		        + "<tr><td align=\"left\"><tt>method</tt> "
		   
		        + "</td><td align=left>meta.code.class</td><td width=\"0\">text </td><td align=\"left\"> Method the wavelength was obtained with (XSAMS controlled vocabulary)</td></tr>"

		        + "<tr><td   align=\"left\"/></tr>"

		        + "<tr><td align=\"left\"><tt>element</tt> "
		   
		        + "</td><td align=left>phys.atmol.element</td><td width=\"0\">text </td><td align=\"left\"> Element name for atomic transitions, NULL otherwise.</td></tr>"

		        + "<tr><td   align=\"left\"/></tr>"

		        + "<tr><td align=\"left\"><tt>ion_charge</tt>" 
		   
		        + "</td><td align=left>phys.electCharge</td><td width=\"0\">integer </td><td align=\"left\"> Total charge (ionisation level) of the emitting particle.</td></tr>"

		        + "<tr><td   align=\"left\"/></tr>"

		        + "<tr><td align=\"left\"><tt>mass_number</tt>" 
		   
		        + "</td><td align=left>phys.atmol.weight</td><td width=\"0\">integer </td><td align=\"left\"> Number of nucleons in the atom or molecule</td></tr>"

		        + "<tr><td   align=\"left\"/></tr>"

		        + "<tr><td align=\"left\"><tt>upper_energy</tt> [J] "
		   
		        + "</td><td align=left>phys.energy<br>phys.atmol.initial</td><td width=\"0\">float </td><td align=\"left\"> Energy of the upper state</td></tr>"

		        + "<tr><td   align=\"left\"/></tr>"

		        + "<tr><td align=\"left\"><tt>lower_energy</tt> [J] "
		   
		        + "</td><td align=left>phys.energy<br>phys.atmol.final</td><td width=\"0\">float </td><td align=\"left\"> Energy of the lower state</td></tr>"

		        + "<tr><td   align=\"left\"/></tr>"

		        + "<tr><td align=\"left\"><tt>inchi</tt>" 
		   
		       + "</td><td align=left>meta.id<br>phys.atmol<br>meta.main</td><td width=\"0\">text </td><td align=\"left\"> International Chemical Identifier InChI.</td></tr>"

		        + "<tr><td   align=\"left\"/></tr>"

		        + "<tr><td align=\"left\"><tt>inchikey</tt>" 
		   
		       + "</td><td align=left>meta.id<br>phys.atmol</td><td width=\"0\">text </td><td align=\"left\"> The InChi key (hash) generated from inchi.</td></tr>"

		        + "<tr><td   align=\"left\"/></tr>"

		        + "<tr><td align=\"left\"><tt>einstein_a</tt>" 
		   
		       + "</td><td align=left>phys.atmol.transProb</td><td width=\"0\">float </td><td align=\"left\"> Einstein A coefficient of the radiative transition.</td></tr>"

		        + "<tr><td   align=\"left\"/></tr>"

		        + "<tr><td align=\"left\"><tt>xsams_uri</tt>" 
		   
		       + "</td><td align=left>meta.ref</td><td width=\"0\">text </td><td align=\"left\"> A URI for a full XSAMS description of this line.</td></tr>"

		        + "<tr><td   align=\"left\"/></tr>"

		        + "<tr><td align=\"left\"><tt>line_reference</tt>" 
		   
		       + "</td><td align=left>meta.ref</td><td width=\"0\"><b>text</b> </td><td align=\"left\"> Reference to the source of the line data<br> this could be a bibcode, a DOI, or a plain URI.</td></tr>"

		        + "<tr><td align=\"left\">"
		        +"</td></tr></table>";
		       
		        		
		        infoText.setText(info);
		        infoText.setEditable(false); // Make it read-only
		        infoText.setOpaque(false);   // Blend with background
		        infoText.setBackground(Color.WHITE);
		        JScrollPane scrollPane = new JScrollPane(infoText);
		        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		       
			return scrollPane;
		}

	}

	public void setLinetapTab(boolean linetapSelected) {		

		if (linetapSelected) {
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

