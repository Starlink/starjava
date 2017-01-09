package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.coords.Coordinates;
import jsky.coords.DMS;
import jsky.coords.HMS;
import jsky.coords.WorldCoords;
import uk.ac.starlink.splat.iface.AbstractServerPanel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.TableLoadPanel;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.GridBagLayouter;
import uk.ac.starlink.vo.ResolverInfo;

public class ObsCoreQueryServerPanel extends AbstractServerPanel implements ActionListener,  DocumentListener {
 
    // sizes
    
    private static int WIDTH;
    private static int HEIGHT;
    private static int optionsHeight = 370;
    
    // Service type string
    private static String serviceType="ObsCore";
    
    /* where to save the tags information */
    private static String tagsFile = "obsCoreTagsV2.xml";
    
    protected JTabbedPane queryTabPanel = null;
    
    private String[] parameters = {"", "target_name", "s_ra", "s_dec", "s_fov", "s_region", "s_resolution", 
            "t_min", "t_max", "t_exptime", "em_min", "em_max", "em_res_power", "pol_states", "calib_level"};
    private String[] comparisons = {"", "=", "!=", "<>", "<", ">", "<=", ">="};
    private String[] conditions = {"", "AND", "OR"};
    private String queryPrefix = "SELECT TOP 10000 * from ivoa.Obscore WHERE dataproduct_type=\'spectrum\' ";

    private String[] subquery = null;

    /**
     *  Simple query window like SSAP
     */
    /** Central RA */
    protected JTextField raField = null;

    /** Central Dec */
    protected JTextField decField = null;

    /** Region radius */
    protected JTextField radiusField = null;
    
    /** Maxrec */
    protected JTextField maxrecField = null;

    /** Lower limit for BAND */
    protected JTextField lowerBandField = null;

    /** Upper limits for BAND */
    protected JTextField upperBandField = null;
    /** Lower limit for TIME */
    protected JTextField lowerTimeField = null;

    /** Upper limits for TIME */
    protected JTextField upperTimeField = null;

    /** The query text */
    protected JTextArea queryText = null;
    
    /** The extended query text */
    protected String extendedQueryText = null;
    

    /** Object name */
    protected JTextField nameField = null;

    /** Resolve object name button */
    protected JButton nameLookup = null;   
    
    /** the  value to the adql search parameter */
    protected JTextField textValue = null;
    
    private JTextArea querytext;
    private String queryParams=queryPrefix;

    /** NED name resolver catalogue */
    protected SkycatCatalog nedCatalogue = null;

    /** SIMBAD name resolver catalogue */
    protected SkycatCatalog simbadCatalogue = null;

    /** The current name resolver, if using Skycat method */
    protected SkycatCatalog resolverCatalogue = null;
    
    
    public ObsCoreQueryServerPanel( ObsCoreServerList list , Dimension size )
    {
        super(list);

      //  sortTable();
        subquery = new String[4];
        //queryParams = queryPrefix;

        WIDTH=size.width;
        HEIGHT=size.height;
 
        initUI (initOptionsPanel(), initServerPanel());

     //   initFilters();
    //    setFilters();
        
    }  
    private JPanel initOptionsPanel() {
        // TODO Auto-generated method stub
        queryTabPanel = new JTabbedPane();
        queryTabPanel.add("Simple search", initSimpleQueryPanel());        
        queryTabPanel.add("ADQL search", initQueryADQLPanel());
        queryTabPanel.setPreferredSize(new Dimension(WIDTH,optionsHeight-100));
        JButton getButton = new JButton("Send Query");
        getButton.addActionListener( this );
        getButton.setActionCommand( "QUERY" );    
        
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener( this );
        clearButton.setActionCommand( "CLEAR" );    
        
        JPanel queryButtonsPanel = new JPanel();
        queryButtonsPanel.add(clearButton);
        queryButtonsPanel.add(getButton);
      
        JPanel queryPanel = new JPanel(new GridBagLayout());
        queryPanel.setBorder(BorderFactory.createEtchedBorder() );
        GridBagConstraints gbcOptions = new GridBagConstraints();
        gbcOptions.anchor = GridBagConstraints.NORTHWEST;
        gbcOptions.fill = GridBagConstraints.HORIZONTAL;
        gbcOptions.weightx=.5;
        gbcOptions.weighty=1;
        gbcOptions.gridx=0;
        gbcOptions.gridy=0;
        
        queryPanel.add(queryTabPanel,gbcOptions);
        gbcOptions.weighty=0;
        gbcOptions.gridy=1;
        queryPanel.add(queryButtonsPanel,gbcOptions);
        gbcOptions.weighty=0;
        gbcOptions.gridy=2;
        // Options Component: User Defined Tags
        JPanel tagPanel= makeTagPanel();
        queryPanel.add(tagPanel,gbcOptions);
     //   JScrollPane optionsScroller = new JScrollPane();
     //   optionsScroller.getViewport().add( queryPanel, null );
     //   optionsScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
     //   optionsScroller.setMinimumSize(new Dimension(WIDTH,optionsHeight));
        return queryPanel;

    }
    
    /** 
     * initialize the simple query input interface  
     */
    
    private JPanel initSimpleQueryPanel()
    {
      
      
        JPanel queryParamPanel = new JPanel();
        JPanel querySearchPanel = new JPanel();
        querySearchPanel.setBorder(BorderFactory.createTitledBorder("Query"));
        querySearchPanel.setLayout( new BoxLayout(querySearchPanel, BoxLayout.PAGE_AXIS ));
        querySearchPanel.setPreferredSize(new Dimension(310,300));

        querySearchPanel.setLayout( new GridBagLayout());
      
        queryParamPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill=GridBagConstraints.BOTH;
        c.anchor=GridBagConstraints.NORTHWEST;
        c.weightx=.5;
        c.weighty=1.;
        c.gridx = 0;
        c.gridy = 0;
        
        JPanel simpleQueryPanel = new JPanel();
        simpleQueryPanel.setLayout(new BorderLayout());
        simpleQueryPanel.setBorder(BorderFactory.createTitledBorder("Query Parameters"));
      
        queryParamPanel.add(simpleQueryPanel, c);
        
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 0;
       
       
        
        // The simple query panel           
        GridBagLayouter layouter =  new GridBagLayouter( simpleQueryPanel, GridBagLayouter.SCHEME4 /*SCHEME4*/ );

        //  Object name. Arrange for a resolver to look up the coordinates of
        //  the object name, when return or the lookup button are pressed.
        JLabel nameLabel = new JLabel( "Object:" );
        nameField = new JTextField( 10 );
        nameField.setFocusable(true);
        nameField.setToolTipText( "Enter the name of an object " +
                " -- press return to get coordinates" );
        nameField.addActionListener( this );
        nameField.getDocument().putProperty("owner", nameField); //set the owner
        nameField.getDocument().addDocumentListener( this );
      //  nameField.addMouseListener( this );
        
        nameLookup = new JButton( "Lookup" );
        nameLookup.addActionListener( this );
        nameLookup.setToolTipText( "Press to get coordinates of Object" );
        
        JPanel objPanel = new JPanel( new GridBagLayout());
        GridBagConstraints gbcs = new GridBagConstraints();
        gbcs.weightx = 1.0;
        gbcs.fill = GridBagConstraints.HORIZONTAL;
        gbcs.ipadx = 15;
        objPanel.add(nameField, gbcs);
        objPanel.add(nameLookup, gbcs);
        layouter.add( nameLabel, false );
     
        layouter.add(objPanel, true);
 
        layouter.eatLine();

       
        //  RA and Dec fields. We're free-formatting on these (decimal degrees
        //  not required).

        JLabel raLabel = new JLabel( "RA:" );
        raField = new JTextField( 10 );
        raField.addActionListener(this);
        raField.getDocument().putProperty("owner", raField); //set the owner
        raField.getDocument().addDocumentListener( this );

        JLabel decLabel = new JLabel( "Dec:" );
        decField = new JTextField( 10 );
        decField.addActionListener(this);
        decField.getDocument().putProperty("owner", decField); //set the owner
        decField.getDocument().addDocumentListener( this );

        JPanel posPanel = new JPanel( new GridBagLayout() );
        GridBagConstraints gbc = new GridBagConstraints();
     
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        posPanel.add( raField, gbc );
        gbc.weightx=0.0;
        gbc.fill = GridBagConstraints.NONE;
        posPanel.add(decLabel, gbc);
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        posPanel.add(decField, gbc);
        
        layouter.add( raLabel, false );
        layouter.add(posPanel,true);
        decField.setToolTipText( "Enter the Dec of field centre, decimal degrees or dd:mm:ss.ss" );
        raField.setToolTipText( "Enter the RA of field centre, decimal degrees or hh:mm:ss.ss" );


        //  Radius field.
        JLabel radiusLabel = new JLabel( "Radius:" );
        radiusField = new JTextField( "10.0", 10 );
      //  queryLine.setRadius(10.0);
        radiusField.addActionListener( this );
        radiusField.setToolTipText( "Enter radius of field to search" +
                " from given centre, arcminutes" );
        radiusField.addActionListener( this );
        radiusField.getDocument().putProperty("owner", radiusField); //set the owner
        radiusField.getDocument().addDocumentListener( this );
        
        JLabel maxrecLabel = new JLabel( "Maxrec:" );
        maxrecField = new JTextField( "10000", 10 );      //  queryLine.setmaxrec(10.0);
        maxrecField.addActionListener( this );
        maxrecField.setToolTipText( "Enter maxrec of field to search" +
                " from given centre, arcminutes" );
        maxrecField.addActionListener( this );
        maxrecField.getDocument().putProperty("owner", maxrecField); //set the owner
        maxrecField.getDocument().addDocumentListener( this );
        
        JPanel radMaxrecPanel = new JPanel( new GridBagLayout() );
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.weightx = 1.0;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        radMaxrecPanel.add( radiusField, gbc2 );
        gbc2.weightx=0.0;
        gbc2.fill = GridBagConstraints.NONE;
        radMaxrecPanel.add(maxrecLabel, gbc2);
        gbc2.weightx = 1.0;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        radMaxrecPanel.add(maxrecField, gbc2);
        
        layouter.add( radiusLabel, false );
        layouter.add(radMaxrecPanel,true);

        //  Band fields.
        JLabel bandLabel = new JLabel( "Band:" );
        lowerBandField = new JTextField( 8 );
        lowerBandField.addActionListener( this );
        lowerBandField.getDocument().putProperty("owner", lowerBandField); //set the owner
        lowerBandField.getDocument().addDocumentListener( this );

        upperBandField = new JTextField( 8 );
        upperBandField.addActionListener( this );
        upperBandField.getDocument().putProperty("owner", upperBandField); //set the owner
        upperBandField.getDocument().addDocumentListener( this );


        JPanel bandPanel = new JPanel( new GridBagLayout() );
        GridBagConstraints gbc3 = new GridBagConstraints();

        gbc3.weightx = 1.0;
        gbc3.fill = GridBagConstraints.HORIZONTAL;
        bandPanel.add( lowerBandField, gbc3 );

     
        gbc3.weightx = 0.0;
        gbc3.fill = GridBagConstraints.NONE;
        bandPanel.add( new JLabel( "/" ), gbc3 );

 
        gbc3.weightx = 1.0;
        gbc3.fill = GridBagConstraints.HORIZONTAL;
        bandPanel.add( upperBandField, gbc3 );

        layouter.add( bandLabel, false );
        layouter.add( bandPanel, true );
        lowerBandField.setToolTipText( "Lower limit, or single include " +
                "value, for spectral band, in meters" );
        upperBandField.setToolTipText
        ( "Upper limit for spectral band, in meters" );

        //  Time fields, note this shares a line with the band fields.
        JLabel timeLabel = new JLabel( "Time:" );
        lowerTimeField = new JTextField( 8 );
        lowerTimeField.addActionListener( this );
        lowerTimeField.getDocument().putProperty("owner", lowerTimeField); //set the owner
        lowerTimeField.getDocument().addDocumentListener( this );

        upperTimeField = new JTextField( 8 );
        upperTimeField.addActionListener( this );
        upperTimeField.getDocument().putProperty("owner", upperTimeField); //set the owner
        upperTimeField.getDocument().addDocumentListener( this );

        JPanel timePanel = new JPanel( new GridBagLayout() );

        GridBagConstraints gbc4 = new GridBagConstraints();
        gbc4.weightx = 1.0;
        gbc4.fill = GridBagConstraints.HORIZONTAL;
        timePanel.add( lowerTimeField, gbc4 );

        GridBagConstraints gbc5 = new GridBagConstraints();
        gbc5.weightx = 0.0;
        gbc5.fill = GridBagConstraints.NONE;
        timePanel.add( new JLabel( "/" ), gbc5 );

        GridBagConstraints gbc6 = new GridBagConstraints();
        gbc6.weightx = 1.0;
        gbc6.fill = GridBagConstraints.HORIZONTAL;
        timePanel.add( upperTimeField, gbc6 );

        layouter.add( timeLabel, false );
        layouter.add( timePanel, true );
        lowerTimeField.setToolTipText( "Lower limit, or single include " +
                "value for time coverage, " +
                "ISO 8601 format " +
                "(e.g 2008-10-15T20:48Z)" );
        upperTimeField.setToolTipText( "Upper limit for time coverage, " +
                "in ISO 8601 format " +
                "(e.g 2008-10-15T20:48Z)" );
        
        //
        
        // format and calibration options:
    //    JPanel calibOptions = new JPanel(new GridLayout(3,2));        
    //    layouter.eatSpare();
        
     //   simpleQueryPanel.add(simpleQuery1, BorderLayout.LINE_START);
     //   simpleQueryPanel.add(calibOptions, BorderLayout.LINE_END);
   
  
         
        // the query string display Panel
/*      
        goButton = new JButton( "    SEND QUERY    " );
      //  goButton.setBackground(Color.green);
        goButton.addActionListener( this );*/
      
  //      showQueryButton = new JButton( "Query: ");
  //      showQueryButton.setToolTipText("show/update query string");
  //      showQueryButton.addActionListener( this );
   //     JPanel sendQueryPanel = new JPanel(new BorderLayout());
     //   sendQueryPanel.add(new JLabel("Query:"), BorderLayout.LINE_START);
  //      sendQueryPanel.add(showQueryButton, BorderLayout.LINE_START);
    //    queryText = new JTextArea(2,25);
   //     JScrollPane queryScroller = new JScrollPane();
  //      queryScroller.add(queryText);
     //   queryScroller.setV
   //     queryText.setEditable(false);
   //     sendQueryPanel.add(queryText);
   //     queryText.setLineWrap(true);     
  //      sendQueryPanel.add(goButton, BorderLayout.PAGE_END); // LINE_END
       
 //       c.fill=GridBagConstraints.BOTH;
  //      c.anchor=GridBagConstraints.NORTHWEST;
  //      c.weighty=.5;
  //      c.gridx = 0;
  //      c.gridy = 0;
        
  //      querySearchPanel.add(queryParamPanel, c);
    //    c.gridy=1;
      //  querySearchPanel.add( sendQueryPanel, c);
       
     //   centrePanel.add( queryPanel, gbcentre );
       
        
        // add query text to query text area
     //   updateQueryText();
        return queryParamPanel;
        
    }
    
    /** 
     * initialize the adql input interface  
     */
   private JPanel initQueryADQLPanel() {
        
    //    JPanel      queryInputPanel = new JPanel();
        
        JPanel queryADQLPanel=new JPanel();
        queryADQLPanel.setLayout(new BorderLayout());
        queryADQLPanel.add(initADQLQueryParams(), BorderLayout.PAGE_START);
        querytext = new JTextArea(10, 8);
        querytext.setLineWrap(true);
        querytext.setEditable(true);
        querytext.setWrapStyleWord(true);
        querytext.setText(queryPrefix);
        querytext.getDocument().putProperty("owner", querytext); //set the owner
        querytext.getDocument().addDocumentListener( this );
        queryADQLPanel.add(querytext,BorderLayout.CENTER);
     
       return queryADQLPanel;
        
    }
  
    
    private JPanel initADQLQueryParams()  {
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        JComboBox parambox = new JComboBox(parameters) ;
        parambox.addActionListener(this);
        parambox.setActionCommand( "PARAM" );
        panel.add(parambox);
        
        JComboBox compbox = new JComboBox(comparisons);
        compbox.addActionListener(this);
        compbox.setActionCommand( "COMPARE" );
        panel.add(compbox);
        
        textValue = new JTextField(10);
        textValue.addActionListener(this);
        textValue.setActionCommand( "VALUE" );
        textValue.getDocument().putProperty("owner", textValue); //set the owner
        textValue.getDocument().addDocumentListener( this );
        panel.add(textValue);
        
        JButton andButton = new JButton("AND"); 
        andButton.addActionListener(this);
        andButton.setActionCommand( "AND" );
        
        JPanel okpanel = new JPanel();
        JButton backButton = new JButton("Back"); 
        backButton.addActionListener(this);
        backButton.setActionCommand( "Back" );
        okpanel.add(backButton);
        JButton okButton = new JButton("OK"); 
        okButton.addActionListener(this);
        okButton.setActionCommand( "OK" );
        okpanel.add(okButton);
        
        inputPanel.add(panel,BorderLayout.PAGE_START);
        inputPanel.add(okpanel, BorderLayout.PAGE_END);
        return inputPanel;
        
       
    }
    
    /**
     * Arrange to resolve the object name into coordinates.
     */
    protected void resolveName()
    {
        String name = nameField.getText().trim();
       
        if ( name != null && name.length() > 0 ) {

            QueryArgs qargs = null;
            if ( resolverCatalogue != null ) {
                //  Skycat resolver.
                qargs = new BasicQueryArgs( resolverCatalogue );

                // If objectName has spaces we should protect them.
                name = name.replaceAll( " ", "%20" );
                qargs.setId( name );
            }
            final QueryArgs queryArgs = qargs;
            final String objectName = name;

            Thread thread = new Thread( "Name server" )
            {
                public void run()
                {
                    try {
                        if ( queryArgs == null ) {
                            ResolverInfo info =
                                    ResolverInfo.resolve( objectName );
                            WorldCoords coords =
                                    new WorldCoords( info.getRaDegrees(),
                                            info.getDecDegrees() );
                            String[] radec = coords.format();
                          
                            raField.setText( radec[0] );
                            decField.setText( radec[1] );
                            nameField.setForeground(Color.black);                                                  
                        }
                        else {
                            QueryResult r =
                                    resolverCatalogue.query( queryArgs );
                            if ( r instanceof TableQueryResult ) {
                                Coordinates coords =
                                        ((TableQueryResult) r).getCoordinates( 0 );
                                if ( coords instanceof WorldCoords ) {
                                    //  Enter values into RA and Dec fields.
                                    String[] radec =
                                            ((WorldCoords) coords).format();
                                    raField.setText( radec[0] );
                                    decField.setText( radec[1] );                                                             
                                }
                            }
                        }
                    }
                    catch (Exception e) {
                        ErrorDialog.showError( null, e );
                    }
                }
            };

            //  Start the nameserver.
            raField.setText( "" );
            decField.setText( "" );
            thread.start();
        }
    }

    /**
     * Returns the adql query for a query from the simple query interface      
     */

    private String getQueryText() throws NumberFormatException
    {
        String queryString = "";
        
//      MAXREC/TOP.
        int maxrec=0;
        String maxrecText = maxrecField.getText();
      
        if ( maxrecText != null && maxrecText.length() > 0 ) {
            try {
                maxrec = Integer.parseInt( maxrecText );
            }
            catch (NumberFormatException e) {
        //        ErrorDialog.showError( this, "Cannot understand maxrec value", e );
                maxrec=0;
                throw e;
            }
        }
       
        
        //  Get the position. Allow the object name to be passed using
        //  TARGETNAME, useful for solar system objects.
        String ra = raField.getText();
        String dec = decField.getText();
        String objectName = nameField.getText().trim();
        if ( ra == null || ra.length() == 0 ||
                dec == null || dec.length() == 0 ) {

            //  Check for an object name. Need one or the other.
            if ( objectName != null && objectName.length() > 0 ) {
                ra = null;
                dec = null;
            }
            else {
                int n = JOptionPane.showConfirmDialog( this,
                        "You have not supplied " +
                                "a search centre or object " +
                                "name, do you want to proceed?",
                                "No RA or Dec",
                                JOptionPane.YES_NO_OPTION );
                if ( n == JOptionPane.NO_OPTION ) {
                    return null;
                }

                //  To be clear.
                ra = null;
                dec = null;
                objectName = null;
            }
        } 
 
        double raVal=0, decVal=0;
        if (objectName != null && ra == null && dec == null ) 
            queryString += " AND target_name=\'"+objectName+"\'";
        else {
            if (ra != null ) {
                // Convert coordinates to ICRS
                HMS hms = new HMS( ra );
                raVal=hms.getVal()*15.0;
          //      queryString += " AND s_ra="+raVal;
            }
               
            if (dec != null ) {
                // Convert coordinates to ICRS
               DMS dms = new DMS( dec );
               decVal=dms.getVal();
           //    queryString += " AND s_dec="+decVal;
            }
                   
        //  And the radius.
        String radiusText = radiusField.getText();
        double radius=0;// = 10.0;
        if ( radiusText != null && radiusText.length() > 0 ) {
          //  try {
                radius = Double.parseDouble( radiusText );
         //   }
         //   catch (NumberFormatException e) {
                //ErrorDialog.showError( this, "Cannot understand radius value", e );
                
        //    }
            queryString += " AND CONTAINS( POINT('ICRS', s_ra, s_dec), CIRCLE('ICRS', "+raVal+", "+decVal+","+radius/60.+")) =1"; // convert to degrees

        }
        
    
        
            String lowerBand = lowerBandField.getText();
            if ( ! lowerBand.isEmpty() ) 
                queryString += " AND em_min<=\'"+ lowerBand+"\'";
            
            String upperBand = upperBandField.getText();
            if ( ! upperBand.isEmpty() ) 
                queryString += " AND em_max>=\'"+ upperBand+"\'";
   
            String lowerTime = lowerTimeField.getText();
            if ( ! lowerTime.isEmpty() ) 
                queryString += " AND t_min<=\'"+ lowerTime+"\'";
            
            String upperTime = upperTimeField.getText();
            if ( ! upperTime.isEmpty() )
                queryString += " AND t_max>=\'"+ upperTime+"\'";
        }    
        
        if (maxrec == 0) {
            return queryPrefix+queryString;
        } else {
            return  "SELECT TOP "+maxrec+ " * from ivoa.Obscore WHERE dataproduct_type=\'spectrum\' "+queryString;
        }
       
    }
          
 
    
    private void querySelectedServices( boolean conesearch ) {
        
       
        if (conesearch ) {
            String queryText=null;
            try {
                 queryText=getQueryText();
            }
            catch (NumberFormatException e ) {
                ErrorDialog.showError( this, "Please check given parameters", e );  
            }
            if (queryText == null)
                return;
            queryParams=queryText; 
        }
        else {   // adql expression search
            String str=querytext.getText();
            if (str.endsWith(" AND")) {
                str = str.substring(0, str.lastIndexOf("AND"));
            }
            queryParams=str;
        }
        
       // logger.info( "QUERY= "+queryParams);
       this.firePropertyChange("NewQuery", false, true); // trigger query
       //makeQuery(serverTable.getSelectedRows(), queryParams);       
        
    }
    
    public String getQueryParams() {
        return queryParams;
    }

    
    /**
     *  action performed
     *  process the actions
     */
    public void actionPerformed(ActionEvent e) {

        Object command = e.getActionCommand();
        Object obj = e.getSource();
     
        if ( obj.equals( nameLookup ) /*|| obj.equals( nameField ) */) {
                     
            resolveName();

            return;
        } 

        if ( command.equals( "CLEAR" ) ) {
            {             
                int index=queryTabPanel.getSelectedIndex();
                if (queryTabPanel.getTitleAt(index).equals("Simple search")) {
                    queryTabPanel.remove(index);
                    queryTabPanel.addTab("Simple search", initSimpleQueryPanel());                   
                } 
                else {
                    queryTabPanel.remove(index);
                    queryTabPanel.addTab("ADQL search", initQueryADQLPanel());
                } 
                queryTabPanel.setSelectedIndex(index==0?1:0); // back to the right tab
            }        
            return;
        } 
        if ( command.equals( "QUERY" ) ) // query
        {
             if (queryTabPanel.getTitleAt(queryTabPanel.getSelectedIndex()).equals("Simple search")) {
                 querySelectedServices( true );
             }
             else { // ADQL Search
                 querySelectedServices( false );       
             }
        }
        if ( command.equals( "PARAM" ) ) 
        {
            subquery[0] = (String) ((JComboBox) e.getSource()).getSelectedItem();
         //   querytext.append(" AND "+ subquery[0] );
        }
        if ( command.equals( "COMPARE" ) ) 
        {
            subquery[1] = (String) ((JComboBox) e.getSource()).getSelectedItem();
           // querytext.append(" "+ subquery[1] );
        }
        if ( command.equals( "VALUE" ) ) 
       // if (obj.equals(textValue))
        {
            subquery[2] = (String) ((JTextField) e.getSource()).getText();
            // querytext.append(" "+ subquery[2] );
        }
        if ( command.equals( "Back" ) ) 
        {
            String st=querytext.getText();
            if (st.endsWith(" AND")) {
                st = st.substring(0, st.lastIndexOf("AND"));
            }
            st = st.substring(0, st.lastIndexOf("AND"));
            querytext.setText(st);

           
       //    subquery[0]=subquery[1]=subquery[2]="";
        }
        if ( command.equals( "AND" ) || command.equals("OK")) 
        {
           if(command.equals("OK"))
               querytext.append( " AND " + subquery[0]+" "+ subquery[1]+" "+subquery[2]);
           else 
               querytext.append( " AND " + subquery[0]+" "+ subquery[1]+" "+subquery[2]);
           
       //    subquery[0]=subquery[1]=subquery[2]="";
        }
        
    /*    if ( command.equals( "reset" ) ) // reset text fields
        {
            statusLabel.setText(new String(""));
            resetFields();
        }
        if ( command.equals( "close" ) ) // close window
        {
            closeWindowEvent();
        }
        */
        if ( command.equals( "NEWSERVICE" ) ) // add new server
        {
            addNewServer();
        }
        if ( command.equals( "REFRESH" ) ) // query registry - refresh server table
        {         
            updateServers();           
        }
        //querySearchPanel.updateUI();
      //  queryADQLPanel.updateUI();
    }

    public void changedUpdate(DocumentEvent de) {       
    }

    public void removeUpdate(DocumentEvent de) {
        insertUpdate(de);
    }
    
    public void insertUpdate(DocumentEvent de) {
        Object owner = de.getDocument().getProperty("owner");
       

        if (owner == textValue ) {
            subquery[2] =  textValue.getText();
        }
        if (owner == querytext ) {
           
        }
        if (owner == radiusField ) {
            String radiusText = radiusField.getText();            
            if ( radiusText != null && radiusText.length() > 0 ) {
                try {
                    double radius = Double.parseDouble( radiusText );
                }
                catch (NumberFormatException e1) {
                    ErrorDialog.showError( this, "Cannot understand radius value", e1);  
                    radiusField.setForeground(Color.RED);
                    return;
                }
            }
            radiusField.setForeground(Color.BLACK);
        }
        if (owner == maxrecField ) {      
            String maxrecText = maxrecField.getText(); 
            if ( maxrecText != null && maxrecText.length() > 0 ) {
                try {
                    int maxrec = Integer.parseInt( maxrecText );
                }
                catch (NumberFormatException e2) {
                   ErrorDialog.showError( this, "Cannot understand maxrec value", e2 );
                   maxrecField.setForeground(Color.RED);
                   return;
                }                            
            }
            maxrecField.setForeground(Color.BLACK);               
        }
        
    }

    
    /**
     * Query a registry for any new ObsCore servers. New servers must have a
     * different short name.
     */
    @Override
    protected StarTable makeRegistryQuery()
    {
                
        StarTable table = null;
        try {
                 
            table = TableLoadPanel.loadTable( this, new SSARegistryQueryDialog(SplatRegistryQuery.OBSCORE), new StarTableFactory() );
         }
        catch ( IOException e ) {
            ErrorDialog.showError( this, "Registry query failed", e );
            return null;
        }
        
        return table;
   
            
    } // makeRegistryQuery
    

    @Override
    public int getWidth() {
        
        return WIDTH;
    }

    @Override
    public int getHeight() {
        
        return HEIGHT;
    }

    @Override
    public String getServiceType() {
        
        return serviceType;
    }

    @Override
    public String getTagsFilename() {
        
        return tagsFile;
    }

   

}
