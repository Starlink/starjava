package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.diva.FigureChangedEvent;
import uk.ac.starlink.diva.FigureListener;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.data.LineIDSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.PolyFitFrame.FitReplaceAction;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.vo.LineBrowser;
import uk.ac.starlink.util.gui.GridBagLayouter;

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
    
    

    /**
     * Create an instance.
     */
 
    public SpectralLinesPanel(LineBrowser LineBrowser) 
    {
        browser = LineBrowser;
        this.plot = browser.getPlot();
        //globalList.get
       // if ( globalList.plotCount() > 0 ) {
       //     PlotControl plot = globalList.getPlot(globalList.getPlotName(globalList.currentSpectrum));
      //     setPlot( plot );
       // }
       // initFrame();
        initUI();
       // initMenus();
       
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
        this.setLayout(new BorderLayout());
        //  List of regions of spectrum where to search for lines.
        rangePanel = new JPanel();
        rangeList = new XGraphicsRangesView( plot.getPlot(), rangeMenu );
        rangeList.setPreferredSize(new Dimension(400,200));
        rangePanel.add(rangeList);        
        add( rangePanel, BorderLayout.NORTH);
       
        JPanel elementPanel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Element Symbol: ");
        elementField = new JTextField(5);
        elementPanel.add(label, BorderLayout.LINE_START);
        elementPanel.add(elementField);
        
        JButton queryButton = new JButton( new QueryAction("Search"));
        queryButton.setToolTipText( "Search for spectral lines" );
         
        JPanel queryPanel = new JPanel(new BorderLayout());
        //queryButtonsPanel.add(clearButton);
        queryPanel.add(elementPanel,BorderLayout.LINE_START);
        queryPanel.add(queryButton,BorderLayout.LINE_END);
        add(queryPanel, BorderLayout.SOUTH);
      

    }
    
    /**
     * Initialise frame properties (disposal, title, menus etc.).
     *
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Query for spectral lines" ));
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        contentPane.add( actionBarContainer, BorderLayout.SOUTH );
        setSize( new Dimension( 600, 500 ) );
        setVisible( true );
    }
   */ 
 //   public  RowPopupTable getSLAPServices() {
        
//    }
    
    public void queryLines() {
        
        //  Extract all ranges, obtain current spectrum
        SpecData currentSpectrum = plot.getCurrentSpectrum();
        if ( currentSpectrum == null ) {
            return;
        }
     
        double [] lambda = currentSpectrum.getXData();
        // create a copy of the spectrum, so coordinate conversions won't affect the plot
        double[] fullrange = new double[2];      
        fullrange[0]=lambda[0]; fullrange[1]=lambda[lambda.length-1];
        SpecData sd=currentSpectrum.getSect("copy", fullrange);
      
        // convert X axis to meters
        int msa = sd.getMostSignificantAxis();
        FrameSet frameSet = sd.getFrameSet();       
        frameSet.set( "system=WAVE,unit("+msa+")=m" );
        try {
            sd.initialiseAst();
        } catch (SplatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        // get wavelengths in meters
        double[] lambda2 = sd.getXData();
        //String coordUnits = fs.getC( "unit(1)" );
       
        
        int[] ranges = rangeList.extractRanges( true, true, lambda);
        if ( ranges == null || ranges.length == 0 ) {
            // TO DO !!! add warning!
            return; // No ranges, so nothing to do.
        }
        browser.makeQuery(ranges, lambda2, getElementSymbol());       
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
