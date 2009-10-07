/*
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package uk.ac.starlink.topcat.contrib.cds;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import cds.vizier.VizieRCatalog;
import cds.vizier.VizieRMission;
import cds.vizier.VizieRQueryInterface;
import cds.vizier.VizieRSurvey;

import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.BasicTableLoadDialog;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.table.gui.TableConsumer;
import uk.ac.starlink.topcat.ResourceIcon;

/** A load dialog allowing one to load VizieR tables from TOPCAT
 * 
 * @author Thomas Boch [CDS]
 * @version 0.1 August 2007
 * 
 */
public class VizieRTableLoadDialog extends BasicTableLoadDialog {
    
    // some String constants
    private static final String NAME = "VizieR Catalogues Service";
    private static final String DESC = "Access the VizieR library of "+
        "published astronomical catalogues";
    
    // array of VizieR surveys 
    private VizieRSurvey[] surveys;
    // array of VizieR missions 
    private VizieRMission[] missions;
    // array of VizieRCatalog
    private VizieRCatalog[] vizierCatalogs;
    
    // vector of Wavelength keywords
    private Vector wavelengthKW = new Vector();
    // array of Mission keywords
    private Vector missionKW = new Vector();
    // array of Astronomy keywords
    private Vector astronomyKW = new Vector();
    
    // true if the GUI has been built
    private boolean guiBuilt = false;
    
    private int lastSelectedIndex = 0;
    
    
    /**
     * Constructor. 
     * Create the load dialog GUI
     *
     */
    public VizieRTableLoadDialog() {
        super(NAME, DESC);
        setIcon( ResourceIcon.VIZIER );
        
        // build the whole dialog GUI
        buildGUI();
        
        setPreferredSize(new Dimension(500,500));
    }
    
    /**
     * query VizieR to get metadata needed for the GUI (list of surveys, missions, keywords, etc)
     *
     */
    private void getMetadata() {
        // TODO : à threader pour ne pas ralentir le reste
        VizieRQueryInterface vqi = new VizieRQueryInterface();
        
        List l = vqi.getSurveys();
        surveys = (VizieRSurvey[])l.toArray(new VizieRSurvey[l.size()]);
        
        l = vqi.getMissions();
        missions = (VizieRMission[])l.toArray(new VizieRMission[l.size()]);
        
        l = vqi.getWavelengthKW();
        wavelengthKW.addAll(l);
        
        l = vqi.getMissionKW();
        missionKW.addAll(l);
        
        l = vqi.getAstronomyKW();
        astronomyKW.addAll(l);
    }
    
    private JTabbedPane tabbedPane;
    private JPanel allVizPanel;
    /**
     * Build and set the layout of all GUI components
     *
     */
    private void buildGUI() {
        setLayout(new BorderLayout(5,5));
        setBorder(new EmptyBorder(5,5,5,5));
        
        // displaying VizieR icon
        JLabel lab = new JLabel();
        lab.setIcon(ResourceIcon.VIZIER_LOGO);
        this.add(lab, BorderLayout.NORTH);
        
        // creating the tabbed pane
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        // listen to tab changes to update target and radius values
        tabbedPane.addChangeListener(
                new ChangeListener() {
                    public void stateChanged(ChangeEvent evt) {
                        String target = getCurrentTarget();
                        String radius = getCurrentRadius();
                        
                        setAllTargets(target);
                        setAllRadius(radius);
                        
                        lastSelectedIndex = tabbedPane.getSelectedIndex();
                    }
                }
                );
        
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        
        // we create each panel, and place them in the tabbed pane
        tabbedPane.addTab("All VizieR", null, allVizPanel=makeAllVizieRPanel(), "Query all VizieR tables");
        tabbedPane.addTab("Surveys", null, makeSurveysPanel(), "Query Surveys in VizieR");
        tabbedPane.addTab("Missions", null, makeMissionsPanel(), "Query Missions in VizieR");
        
//        LabelledComponentStack stack = new LabelledComponentStack();
//        stack.addLine( "Base URL", new JTextField());
        this.add(tabbedPane, BorderLayout.CENTER);
        
        guiBuilt = true;

    }
    
    /**
     * Sets all target textfields to t 
     * @param t target value
     */
    private void setAllTargets(String t) {
        if( !guiBuilt ) return;
        
        allVizTargetTF.setText(t);
        surveysTargetTF.setText(t);
        missionsTargetTF.setText(t);
    }
    
    /**
     * Sets all radius textfields to r 
     * @param r radius value
     */
    private void setAllRadius(String r) {
        if( !guiBuilt ) return;
        
        allVizRadiusTF.setText(r);
        surveysRadiusTF.setText(r);
        missionsRadiusTF.setText(r);
    }
    
    /**
     * 
     * @return current target, corresponding to the selected tab
     */
    private String getCurrentTarget() {
        switch(lastSelectedIndex) {
            case 0 : return allVizTargetTF.getText();
            case 1 : return surveysTargetTF.getText();
            case 2 : return missionsTargetTF.getText();
        }
        
        return "";
    }
    
    /**
     * 
     * @return current radius, corresponding to the selected tab
     */
    private String getCurrentRadius() {
        switch(lastSelectedIndex) {
            case 0 : return allVizRadiusTF.getText();
            case 1 : return surveysRadiusTF.getText();
            case 2 : return missionsRadiusTF.getText();
        }
    
        return "";
    }
    
    private JTextField allVizTargetTF;
    private JTextField allVizRadiusTF;
    private JList wavelengthKWList;
    private JList missionKWList;
    private JList astronomyKWList;
    private JButton searchCatsBtn;
    private JTable allVizTable;
    
    
    private JPanel makeAllVizieRPanel() {

        
        // panel to search VizieR cats
        JPanel searchCatsPanel = new JPanel(new BorderLayout(5,10));
        searchCatsPanel.setBorder(BorderFactory.createEtchedBorder());
        
        // target + radius
        LabelledComponentStack stack = new LabelledComponentStack();
        stack.addLine("Target", allVizTargetTF = new JTextField() );
        stack.addLine("Radius (deg)", allVizRadiusTF = new JTextField());
        
        searchCatsPanel.add(stack, BorderLayout.NORTH);
        
        // panel for lists of keywords
        JPanel kwPanel= new JPanel();
        kwPanel.setLayout(new BoxLayout(kwPanel, BoxLayout.X_AXIS));
        JScrollPane scrollPane;
        
        // list of wavelengths
        JPanel wlPanel = new JPanel(new BorderLayout());
        wlPanel.add(new JLabel("Wavelength"), BorderLayout.NORTH);
        scrollPane = new JScrollPane(wavelengthKWList = new JList(wavelengthKW));
        wavelengthKWList.setVisibleRowCount(6);
        wlPanel.add(scrollPane, BorderLayout.CENTER);
        wlPanel.setAlignmentY(TOP_ALIGNMENT);
        
        kwPanel.add(wlPanel);
        
        
        kwPanel.add(Box.createHorizontalGlue());
        
        // list of missions
        JPanel mPanel = new JPanel(new BorderLayout());
        mPanel.add(new JLabel("Mission"), BorderLayout.NORTH);
        scrollPane = new JScrollPane(missionKWList = new JList(missionKW));
        wavelengthKWList.setVisibleRowCount(6);
        mPanel.add(scrollPane, BorderLayout.CENTER);
        mPanel.setAlignmentY(TOP_ALIGNMENT);
        kwPanel.add(mPanel);
        
        kwPanel.add(Box.createHorizontalGlue());
        
        // list of astronomical kw
        JPanel astroPanel = new JPanel(new BorderLayout());
        astroPanel.add(new JLabel("Astronomy"), BorderLayout.NORTH);
        scrollPane = new JScrollPane(astronomyKWList = new JList(astronomyKW));
        astronomyKWList.setVisibleRowCount(6);
        astroPanel.add(scrollPane, BorderLayout.CENTER);
        astroPanel.setAlignmentY(TOP_ALIGNMENT);
        kwPanel.add(astroPanel);
        
        searchCatsPanel.add(kwPanel, BorderLayout.CENTER);
        
        // button to search catalogues
        JPanel btnPanel = new JPanel(new FlowLayout());
        searchCatsBtn = new JButton("Search catalogues");
        searchCatsBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchCatalogues();
            }
            
            
        });
        btnPanel.add(searchCatsBtn);
        
        searchCatsPanel.add(btnPanel, BorderLayout.SOUTH);
        
//        panel.add(searchCatsPanel, BorderLayout.NORTH);
        
        // panel to select and query VizieR cats
        JPanel queryCatsPanel = new JPanel(new BorderLayout());
        queryCatsPanel.setBorder(BorderFactory.createEtchedBorder());
        
        // creation of the JTable
        allVizTable = new JTable(new AllVizTableModel());
        // only one table can be selected
        allVizTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // no reordering of columns
        allVizTable.getTableHeader().setReorderingAllowed(false);
        allVizTable.getTableHeader().addMouseListener(new AllVizHeaderListener());
        allVizTable.setMinimumSize(new Dimension(400,300));
        
        JScrollPane scrollPaneTable = new JScrollPane(allVizTable);
        
        
        queryCatsPanel.add(scrollPaneTable);
        
//        panel.add(queryCatsPanel, BorderLayout.CENTER);
        
        // main panel
        JPanel panel = new JPanel(new BorderLayout(5,10));
        panel.setBorder(new EmptyBorder(5,5,5,5));
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
                searchCatsPanel, queryCatsPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setPreferredSize(new Dimension(300, 400));
        
        panel.add(splitPane, BorderLayout.CENTER);
        
//        return panel;
        return panel;
    }
    
    private JTextField surveysTargetTF;
    private JTextField surveysRadiusTF;
    private JTable surveysTable;
    
    private JPanel makeSurveysPanel() {
        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(new EmptyBorder(5,5,5,5));
        
        LabelledComponentStack stack = new LabelledComponentStack();
        stack.addLine("Target", surveysTargetTF = new JTextField() );
        stack.addLine("Radius (deg)", surveysRadiusTF = new JTextField());
        
        panel.add(stack, BorderLayout.NORTH);
        
        // creation of the JTable
        surveysTable = new JTable(new SurveysTableModel());
        // only one table can be selected
        surveysTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // no reordering of columns
        surveysTable.getTableHeader().setReorderingAllowed(false);
        surveysTable.getTableHeader().addMouseListener(new SurveyHeaderListener());

        JScrollPane scrollPane = new JScrollPane(surveysTable);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JTextField missionsTargetTF;
    private JTextField missionsRadiusTF;
    private JTable missionsTable;
    
    private JPanel makeMissionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(new EmptyBorder(5,5,5,5));
        
        LabelledComponentStack stack = new LabelledComponentStack();
        stack.addLine("Target", missionsTargetTF = new JTextField() );
        stack.addLine("Radius (deg)", missionsRadiusTF = new JTextField());
        
        panel.add(stack, BorderLayout.NORTH);
        
        // creation of the JTable
        missionsTable = new JTable(new MissionsTableModel());
        // only one table can be selected
        missionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // no reordering of columns
        missionsTable.getTableHeader().setReorderingAllowed(false);
        missionsTable.getTableHeader().addMouseListener(new MissionsHeaderListener());

        JScrollPane scrollPane = new JScrollPane(missionsTable);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    // search catalogues, according to entered criteria
    // and fills JTable of matching catalogues
    private void searchCatalogues() {
        final String target = allVizTargetTF.getText();
        final String radius = allVizRadiusTF.getText();
        
        StringBuffer kw = new StringBuffer();
        fillKeyword(kw, "Wavelength", wavelengthKWList.getSelectedIndices(), (String[])wavelengthKW.toArray(new String[wavelengthKW.size()]));
        fillKeyword(kw, "Mission", missionKWList.getSelectedIndices(), (String[])missionKW.toArray(new String[missionKW.size()]));
        fillKeyword(kw, "Astronomy", astronomyKWList.getSelectedIndices(), (String[])astronomyKW.toArray(new String[astronomyKW.size()]));

        final String kwStr = kw.toString();
        
        // perform query to VizieR in a separate thread
        new Thread("GetVizieRListOfCats") {
            public void run() {
                // TODO : les modifs Swing doivent se faire dans un thread spécial swing
                allVizPanel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                searchCatsBtn.setEnabled(false);
                // empty list of catalogues
                vizierCatalogs = null;
                ((AbstractTableModel)allVizTable.getModel()).fireTableDataChanged();
                
                
                List l = new VizieRQueryInterface().queryVizieR(target, radius+" deg", null, null, kwStr);
                vizierCatalogs = (VizieRCatalog[])l.toArray(new VizieRCatalog[l.size()]);
        
                ((AbstractTableModel)allVizTable.getModel()).fireTableDataChanged();
                
                searchCatsBtn.setEnabled(true);
                allVizPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }.start();
    }
    
    private void fillKeyword(StringBuffer keyword, String kwName, int[] selected, String[] values) {
        if( selected.length==0 ) return;
        
        if( keyword.length()!=0 ) keyword.append("&");
        keyword.append("-kw."+kwName+"=");
        for( int i=0; i<selected.length; i++ ) {
            if( keyword.length()!=0 ) keyword.append("&");
            keyword.append("-kw."+kwName+"=");
            keyword.append(URLEncoder.encode(values[selected[i]]));
        }
    }
    
    /**
     * Indicates whether this dialogue is available.  Should return false
     * if you happen to know somehow that that this service is unavailable.
     */
    public boolean isAvailable() {
        // TODO : invalider bouton OK si champs requis ne sont pas remplis (voir FIlestore Browser)
        return true;
    }
    
    /**
     * Concrete subclasses should implement this method to supply a
     * TableSupplier object which can attempt to load a table based on
     * the current state (as filled in by the user) of this component.
     * If the state is not suitable for an attempt at loading a table
     * (e.g. some components are filled in in an obviously wrong way)
     * then a runtime exception such as <tt>IllegalStateException</tt>
     * or <tt>IllegalArgumentException</tt> should be thrown.
     *
     * @return  table supplier corresponding to current state of this component
     * @throws  RuntimeException  if validation fails
     */
    protected TableSupplier getTableSupplier() {
        // check first if needed information have been supplied
        final int selectedTabIndex = tabbedPane.getSelectedIndex();
        switch( selectedTabIndex ) {
            // all vizier
            case 0 : 
                if( allVizTable.getSelectedRow()==-1 ) throw new IllegalArgumentException("No catalogue selected");
                if( allVizTargetTF.getText().length()==0 ) throw new IllegalArgumentException("Target is required");
                if( allVizRadiusTF.getText().length()==0 ) throw new IllegalArgumentException("Radius is rquired");
                break;
            // surveys
            case 1 : 
                if( surveysTable.getSelectedRow()==-1 ) throw new IllegalArgumentException("No survey selected");
                if( surveysTargetTF.getText().length()==0 ) throw new IllegalArgumentException("Target is required");
                if( surveysRadiusTF.getText().length()==0 ) throw new IllegalArgumentException("Radius is rquired");
                break;
            // missions
            case 2 : 
                if( missionsTable.getSelectedRow()==-1 ) throw new IllegalArgumentException("No mission selected");
                if( missionsTargetTF.getText().length()==0 ) throw new IllegalArgumentException("Target is required");
                if( missionsRadiusTF.getText().length()==0 ) throw new IllegalArgumentException("Radius is rquired");
                break;
            
        }
        
        
        URL url = null;
        
            switch( selectedTabIndex ) {
                // all vizier
                case 0 : {
                    String target = allVizTargetTF.getText();
                    double radius = Double.parseDouble(allVizRadiusTF.getText());
                    url = vizierCatalogs[allVizTable.getSelectedRow()].getQueryUrl(target, radius);
                    break;
                }
                // surveys
                case 1 : {
                    String target = surveysTargetTF.getText();
                    double radius = Double.parseDouble(surveysRadiusTF.getText());
                    url = surveys[surveysTable.getSelectedRow()].getQueryUrl(target, radius);
                    break;
                }
                // missions
                case 2 : {
                    String target = missionsTargetTF.getText();
                    double radius = Double.parseDouble(missionsRadiusTF.getText());
                    url = missions[missionsTable.getSelectedRow()].getQueryUrl(target, radius);
                    break;
                }
            }
        
        final URL tableUrl = url;
        return new TableSupplier() {
            
            public StarTable getTable(StarTableFactory factory, String format) throws IOException {
                return factory.makeStarTable(tableUrl, "votable");
            }
            
            public String getTableID() {
                switch( selectedTabIndex ) {
                    // all vizier
                    case 0 : {
                        return vizierCatalogs[allVizTable.getSelectedRow()].getName().replaceAll("/", ".");
                    }
                    // surveys
                    case 1 : {
                        return surveys[surveysTable.getSelectedRow()].getSmallName();
                    }
                    // missions
                    case 2 : return missions[missionsTable.getSelectedRow()].getSmallName();
                }
                
                return "foo";
            }
        };
        
    }

    public boolean showLoadDialog( Component parent, StarTableFactory factory,
                                   ComboBoxModel formatModel,
                                   TableConsumer consumer ) {
        // at first show, query VizieR to get needed metadata
        if( surveys==null ) {
            getMetadata();
        }
        return super.showLoadDialog( parent, factory, formatModel, consumer );
    }
    
    private void sort(JTable table, Object[] data, boolean ascending, Comparator comp) {
        List l = Arrays.asList(data);
        Collections.sort(l, comp);
        if( !ascending ) Collections.reverse(l);
        
        data = l.toArray();
        
        ((AbstractTableModel)table.getModel()).fireTableStructureChanged();
    }
    
    /**
     * Inner class implementing a table model for the JTable displaying surveys 
     *
     */
    class SurveysTableModel extends AbstractTableModel {
        
        
        public String getColumnName(int col) {
            switch(col) {
                case 0 : return "Name";
                
                case 1 : return "Description";
                
                case 2 : return "Number of KRows";
                
                default : return "";
            }
        }
        public int getRowCount() {
            return surveys==null?0:surveys.length;
        }
        public int getColumnCount() { return 3; }
        public Object getValueAt(int row, int col) {
            switch(col) {
                case 0 : return surveys[row].getSmallName();
                
                case 1 : return surveys[row].getDescription();
                
                case 2 : return new Integer(surveys[row].getNbKRow());
                
                default : return "";
            }
        }
        public boolean isCellEditable(int row, int col) {
            return false;
        }


    } // end of inner class SurveysTableModel
    
    /**
     * Inner class implementing a table model for the JTable displaying missions 
     *
     */
    class MissionsTableModel extends AbstractTableModel {
        
        
        public String getColumnName(int col) {
            switch(col) {
                case 0 : return "Name";
                
                case 1 : return "Description";
                
                case 2 : return "Number of KRows";
                
                default : return "";
            }
        }
        public int getRowCount() {
            return missions==null?0:missions.length;
        }
        public int getColumnCount() { return 3; }
        public Object getValueAt(int row, int col) {
            switch(col) {
                case 0 : return missions[row].getSmallName();
                
                case 1 : return missions[row].getDescription();
                
                case 2 : return new Integer(missions[row].getNbKRow());
                
                default : return "";
            }
        }
        public boolean isCellEditable(int row, int col) {
            return false;
        }


    } // end of inner class MissionsTableModel
    
    /**
     * Inner class implementing a table model for the JTable displaying VizieR catalogues 
     *
     */
    class AllVizTableModel extends AbstractTableModel {
        
        
        public String getColumnName(int col) {
            switch(col) {
                case 0 : return "Name";
                
                case 1 : return "Category";
                
                case 2 : return "Density";
                
                case 3 : return "Description";
                
                default : return "";
            }
        }
        public int getRowCount() {
            return vizierCatalogs==null?0:vizierCatalogs.length;
        }
        public int getColumnCount() { return 4; }
        public Object getValueAt(int row, int col) {
            switch(col) {
                case 0 : return vizierCatalogs[row].getName();
                
                case 1 : return vizierCatalogs[row].getCategory();
                
                case 2 : return new Integer(vizierCatalogs[row].getDensity());
                
                case 3 : return vizierCatalogs[row].getDesc();
                
                default : return "";
            }
        }
        public boolean isCellEditable(int row, int col) {
            return false;
        }

    } // end of inner class AllVizTableModel
    
    /** inner class allowing to listen to mouse events on surveysTable JTable header
     */
    class SurveyHeaderListener extends MouseAdapter {
        boolean ascending = true; 
        
        public void mouseClicked(MouseEvent e) {
            TableColumnModel columnModel = surveysTable.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX()); 
            final int column = surveysTable.convertColumnIndexToModel(viewColumn); 
            if( e.getClickCount() == 1 && column != -1 ) {
                
                Comparator surveyComp = new Comparator() {
                    public final int compare (Object a, Object b) {
                        Object val1, val2;
                        val1 = val2 = null;
                        switch(column) {
                            case 0 : val1 = ((VizieRSurvey)a).getSmallName();
                                     val2 = ((VizieRSurvey)b).getSmallName();
                                     break;
                                     
                            case 1 : val1 = ((VizieRSurvey)a).getDescription();
                                         val2 = ((VizieRSurvey)b).getDescription();
                                         break;
                            
                            case 2 : val1 = new Integer(((VizieRSurvey)a).getNbKRow());
                                         val2 = new Integer(((VizieRSurvey)b).getNbKRow());
                                         break;
                        }
                        return ((Comparable)val1).compareTo(val2);
                    }
                };
                
                sort(surveysTable, surveys, ascending, surveyComp);
                
                ascending = !ascending;
            }
         }
    } // end of inner class SurveyHeaderListener
    
    /** inner class allowing to listen to mouse events on missionsTable JTable header
     */
    class MissionsHeaderListener extends MouseAdapter {
        boolean ascending = true; 
        
        public void mouseClicked(MouseEvent e) {
            TableColumnModel columnModel = missionsTable.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX()); 
            final int column = missionsTable.convertColumnIndexToModel(viewColumn); 
            if( e.getClickCount() == 1 && column != -1 ) {
                
                Comparator missionComp = new Comparator() {
                    public final int compare (Object a, Object b) {
                        Object val1, val2;
                        val1 = val2 = null;
                        switch(column) {
                            case 0 : val1 = ((VizieRMission)a).getSmallName();
                                     val2 = ((VizieRMission)b).getSmallName();
                                     break;
                                     
                            case 1 : val1 = ((VizieRMission)a).getDescription();
                                         val2 = ((VizieRMission)b).getDescription();
                                         break;
                            
                            case 2 : val1 = new Integer(((VizieRMission)a).getNbKRow());
                                         val2 = new Integer(((VizieRMission)b).getNbKRow());
                                         break;
                        }
                        return ((Comparable)val1).compareTo(val2);
                    }
                };
                
                sort(missionsTable, missions, ascending, missionComp);
                
                ascending = !ascending;
            }
         }
    } // end of inner class MissionsHeaderListener
    
    /** inner class allowing to listen to mouse events on allVizTable JTable header
     */
    class AllVizHeaderListener extends MouseAdapter {
        boolean ascending = true; 
        
        public void mouseClicked(MouseEvent e) {
            TableColumnModel columnModel = allVizTable.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX()); 
            final int column = allVizTable.convertColumnIndexToModel(viewColumn); 
            if( e.getClickCount() == 1 && column != -1 ) {
                
                Comparator allVizComp = new Comparator() {
                    public final int compare (Object a, Object b) {
                        Object val1, val2;
                        val1 = val2 = null;
                        switch(column) {
                            case 0 : val1 = ((VizieRCatalog)a).getName();
                                     val2 = ((VizieRCatalog)b).getName();
                                     break;
                                     
                            case 1 : val1 = ((VizieRCatalog)a).getCategory();
                                     val2 = ((VizieRCatalog)b).getCategory();
                                     break;
                         
                            case 2 : val1 = new Integer(((VizieRCatalog)a).getDensity());
                                     val2 = new Integer(((VizieRCatalog)b).getDensity());
                                     break;
                                     
                            case 3 : val1 = ((VizieRCatalog)a).getDesc();
                                    val2 = ((VizieRCatalog)b).getDesc();
                                    break;
                        }
                        return ((Comparable)val1).compareTo(val2);
                    }
                };
                
                sort(allVizTable, vizierCatalogs, ascending, allVizComp);
                
                ascending = !ascending;
            }
         }
    } // end of inner class AllVizHeaderListener
    
    public static void main(String[] args) {
        new VizieRTableLoadDialog();
    }
    
}
