package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.data.LineIDTableSpecDataImpl;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.vo.LineBrowser;

public class SpectralLinesPanel extends JPanel implements  ActionListener {
    /** UI preferences. */
    protected static Preferences prefs =
        Preferences.userNodeForPackage( SpectralLinesPanel.class );

    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     * Action buttons container.
     */
    protected JPanel actionBarContainer = new JPanel();
    protected JPanel topActionBar = new JPanel();
    protected JPanel midActionBar = new JPanel();
    protected JPanel botActionBar = new JPanel();

    /**
     *  Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenu rangeMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();

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
    protected XGraphicsRangesView rangeList = null;
    protected XGraphicsRange range = null;
    
    /**
     *  View for showing the results of a query.
     */
    protected JTextArea lineResults = new JTextArea();

    
    /**
     *  ScrollPane for display results of a query
     */
    protected JScrollPane lineResultsPane = new JScrollPane();


     /**
     *  Label for results area.
     */
    protected TitledBorder lineResultsTitle =
        BorderFactory.createTitledBorder( "Lines found:" );

    /**
     *  Reference to GlobalSpecPlotList object.
     */
  //  protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    private LineBrowser browser;

   // private SplatBrowser splatBrowser_;
    
   // private JLabel rangePlaceHolder = new JLabel("Please select a plot");
    private int currentPlotIndex =-1;
    
    int[] ranges = null;
    double[] lambda2 = null;

    private JTextField elementField;
    private JComboBox stageCombo;
    String [] stages = { " ", "I","II","III","IV","V","VI","VII","VIII","IX","X","choose"};
    
    

    /**
     * Create an instance.
     */
 
    public SpectralLinesPanel(LineBrowser LineBrowser) 
    {
        browser = LineBrowser;
        this.plot = browser.getPlot();
        
        initUI();       
       
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
     */
    protected void initUI()
    {
       
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEtchedBorder() );
        //  List of regions of spectrum where to search for lines.
      
        rangeList = new XGraphicsRangesView( plot.getPlot(), rangeMenu, Color.LIGHT_GRAY, true ); 
       // rangeList.setPreferredSize(new Dimension(380,150));
        rangePanel = new JPanel();
        rangePanel.add(rangeList, BorderLayout.PAGE_START);    
        rangeList.setPreferredSize(new Dimension(320,120));
      
       
        JPanel elementQueryPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.anchor = GridBagConstraints.WEST;
        JPanel elementPanel = new JPanel(/*new BorderLayout()*/);
        JLabel elementLabel = new JLabel("Element: ");
        elementField = new JTextField(5);
        elementPanel.add(elementLabel);
        elementPanel.add(elementField);        
        JPanel stagePanel = new JPanel(/*new BorderLayout()*/);
        JLabel stageLabel = new JLabel("Stage: ");
        stageCombo = new JComboBox(stages);
        stageCombo.setPrototypeDisplayValue("XXX");
        stageCombo.setEditable(true);
        stageCombo.addActionListener(this);
        stagePanel.add(stageLabel);
        stagePanel.add(stageCombo);
        gbc1.weightx=0.5;
        gbc1.weighty=0.5;
        gbc1.gridx=0;  
        gbc1.gridy=0;
        elementQueryPanel.add (elementPanel, gbc1);
        gbc1.gridx=1;
        elementQueryPanel.add (stagePanel,gbc1 );
        
        JButton queryButton = new JButton( new QueryAction("Search"));
        queryButton.setToolTipText( "Search for spectral lines" ); 
        
        //elementPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.anchor = GridBagConstraints.WEST;
       // gbc2.gridwidth=1;
       // gbc2.gridheight=1;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.weightx=0.5;
        gbc2.weighty=0.5;
        gbc2.gridx=0;  
        gbc2.gridy=0;

        JPanel queryPanel = new JPanel();
        
        queryPanel.setLayout(new GridBagLayout());     
        queryPanel.add(elementQueryPanel,gbc2);    
        gbc2.gridy=1;      
       
        gbc2.anchor = GridBagConstraints.LINE_END;
        queryPanel.add(queryButton,gbc2);
         
        GridBagConstraints gbc = new GridBagConstraints();
      //  gbc.gridwidth=GridBagConstraints.REMAINDER;
       // gbc.gridheight=GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
   //     gbc.fill = GridBagConstraints.BOTH;
       // gbc.weightx=1;
     //   gbc.weighty=1;
        gbc.gridx=0;  
        gbc.gridy=0;
        
        add( rangePanel, gbc);               
        gbc.gridheight=GridBagConstraints.REMAINDER;
       
        gbc.gridy=1;
        gbc.gridx=0;  
     //   gbc.weightx=0;
     //   gbc.weighty=1;
        add(queryPanel, gbc);
      
    }
    
 
    
    public void queryLines() {

        ArrayList<double []> lambdas=new ArrayList<double[]>();
        ArrayList<int []> ranges=new ArrayList<int[]>();
        SpecDataComp spectra = plot.getSpecDataComp();
        //     if (spectra.count() >1) {// more than one spectrum in the plot
        
        for (int i=0;i<spectra.count();i++) {
            
            SpecData spectrum = spectra.get(i);
            if (spectrum.getSpecDataImpl().getClass()!=LineIDTableSpecDataImpl.class) { //ignore existing lines 


                boolean ok=true;
                double [] lambda = spectrum.getXData();
                // create a copy of the spectrum, so coordinate conversions won't affect the plot
                double[] fullrange = new double[2];      
                fullrange[0]=lambda[0]; fullrange[1]=lambda[lambda.length-1];
                SpecData sd=spectrum.getSect("copy", fullrange);
                // convert X axis to meters
                int msa = sd.getMostSignificantAxis();           
                try {
                    FrameSet frameSet = sd.getFrameSet();       
                    frameSet.set( "system=WAVE,unit("+msa+")=m" );
                    sd.initialiseAst();
                } catch (SplatException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    ok=false;
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
        }
   
        browser.makeQuery(ranges, lambdas, getElementSymbol(), getStage());       
    }
    
    

    


    private int[] getRanges(SpecData spectrum) {
        double [] lambda = spectrum.getXData();
        // create a copy of the spectrum, so coordinate conversions won't affect the plot
        double[] fullrange = new double[2];      
        fullrange[0]=lambda[0]; fullrange[1]=lambda[lambda.length-1];
        SpecData sd=spectrum.getSect("copy", fullrange);
        // convert X axis to meters
        int msa = sd.getMostSignificantAxis();
        FrameSet frameSet = sd.getFrameSet();       
        frameSet.set( "system=WAVE,unit("+msa+")=m" );
        try {
            sd.initialiseAst();
        } catch (SplatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        double[] lambda2 = sd.getXData();
        int[] ranges = rangeList.extractRanges( true, true, lambda);
        if ( ranges == null || ranges.length == 0 ) {
            // TO DO !!! add warning!
            return null; // No ranges, so nothing to do.
        }
      
        return ranges;
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


    public int[] getRanges() {
        return ranges;
    }

    public double[] getWavelengths() {
        return lambda2;
    }

    public String getElementSymbol() {
        return elementField.getText();
    }

    public String getStage() {
        int stage = stageCombo.getSelectedIndex();
        if (stage==0)
            return "";
        return Integer.toString( stageCombo.getSelectedIndex() -1);
    }


  

    private void queryInterval(double d, double e) {
        // TODO Auto-generated method stub
        double startfreq=d; double endfreq=e;
    }

    /**
     * Fit selected action. Performs fit to the selected ranges.
     */
    
    protected class QueryAction extends AbstractAction
    {
        public QueryAction( String name ) {
            super( name );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control S" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
            queryLines();
        }
    }
    
  


    public void removeRanges() {
        rangePanel.removeAll();
  //      rangePanel.add(rangePlaceHolder);
        
    }


   


    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        
    }


   
  //  @Override
  //  public void spectrumRemoved(SpecChangedEvent e) {
  //      if (globalList.plotCount() == 0) {
  //          rangePanel.removeAll();
  //          rangePanel.add(rangePlaceHolder);
  //      } else
  //        addRangeList();
  //  }

/*
    @Override
    public void plotCreated(PlotChangedEvent e) {
        //currentPlotInde
        setPlot(globalList.getPlot(e.getIndex()));       
        addRangeList();
    }


    @Override
    public void plotRemoved(PlotChangedEvent e) {
        if (globalList.plotCount() == 0) {
            rangePanel.removeAll();
            rangePanel.add(rangePlaceHolder);
        } 
    }


    @Override
    public void plotChanged(PlotChangedEvent e) {
        // TODO Auto-generated method stub
        setPlot(globalList.getPlot(e.getIndex()));      
        addRangeList();
    }
*/
}
