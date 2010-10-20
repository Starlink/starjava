/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.starlink.topcat.contrib.basti;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.table.TableModel;

/**
 *
 * @author molinaro
 */
public class BaSTIPanel {

    //static String[] OutPutTables = null;

    final static String QUERYINFOSTRING = "Maximum returned tables: " + BaSTIPOSTMessage.MAX_RETURNED_RESULTS;

    /* All BaSTIPanel Components and Values */
    /* Containers */
    static JTabbedPane TabPane = new JTabbedPane();  //tabbed space
    static JPanel QueryTab = new JPanel(); //query form submission tab
    static JPanel QueryPanel = new JPanel();   //query form panel
    static JPanel QueryBottom = new JPanel();   // query tab bottom section
    static JPanel ResultsTab = new JPanel(); //results loader tab

    /* Query Components */
    /* Usage Description */
    static String QueryDescriptionString = "Build your query using provided fields. " +
                "Check the boxes to select the fields you want in the query results." + "\n" +
                "Then press SUBMIT: the BaSTI tables that satisfy " +
                "your query will be displayed in the Results tab." + "\n" +
                "Keep the mouse over the text fields to get an hint on boundary values.";
    static JTextPane QueryDescription = new JTextPane();
    /* Data Type Selection */
    static JLabel DataTypeLabel = new JLabel("Data Type");
    static JCheckBox DataTypeCheck = new JCheckBox();
    static JComboBox DataType = new JComboBox();
    static String[] DataTypeChoice = {"",
                            "Isochrone",
                            "Track",
                            "HB Track",
                            "ZAHB Table",
                            "End He Table",
                            "Summary Table"};
    /* AGE */
    static JLabel AgeLabel = new JLabel("Age [Gyr]");
    static JCheckBox AgeCheck = new JCheckBox();
    static JTextField AgeMin = new JTextField();
    static JTextField AgeMax = new JTextField();
    /* MASS */
    static JLabel MassLabel = new JLabel("Mass [MSun]");
    static JCheckBox MassCheck = new JCheckBox();
    static JTextField MassMin = new JTextField();
    static JTextField MassMax = new JTextField();
    /* Z */
    static JLabel ZLabel = new JLabel("Z");
    static JCheckBox ZCheck = new JCheckBox();
    static JTextField ZMin = new JTextField();
    static JTextField ZMax = new JTextField();
    /* Y */
    static JLabel YLabel = new JLabel("Y");
    static JCheckBox YCheck = new JCheckBox();
    static JTextField YMin = new JTextField();
    static JTextField YMax = new JTextField();
    /* FeH */
    static JLabel FeHLabel = new JLabel("[Fe/H]");
    static JCheckBox FeHCheck = new JCheckBox();
    static JTextField FeHMin = new JTextField();
    static JTextField FeHMax = new JTextField();
    /* MH */
    static JLabel MHLabel = new JLabel("[M/H]");
    static JCheckBox MHCheck = new JCheckBox();
    static JTextField MHMin = new JTextField();
    static JTextField MHMax = new JTextField();
    /* SCENARIO */
    static JLabel ScenarioLabel = new JLabel("Scenario");
    static JCheckBox ScenarioCheck = new JCheckBox();
    static JComboBox Scenario = new JComboBox();
    static String[] ScenarioChoice = {"",
                                    "Canonical",
                                    "Overshooting"};
    /* TYPE */
    static JLabel TypeLabel = new JLabel("Type");
    static JCheckBox TypeCheck = new JCheckBox();
    static JComboBox Type = new JComboBox();
    static String[] TypeChoice = {"",
                                "Normal",
                                "AGB Extended"};
    /* Mass Loss */
    static JLabel MassLossLabel = new JLabel("Mass Loss");
    static JCheckBox MassLossCheck = new JCheckBox();
    static JComboBox MassLoss = new JComboBox();
    static String[] MassLossChoice = {"",
                                    "0.2",
                                    "0.4"};
    /* Photometric System */
    static JLabel PhotometryLabel = new JLabel("Photometric System");
    static JCheckBox PhotometryCheck = new JCheckBox();
    static JComboBox Photometry = new JComboBox();
    static String[] PhotometryChoice = {"",
                                    "ACS",
                                    "Johnson Castelli",
                                    "Sloan",
                                    "Stroemgren Castelli",
                                    "Walraven",
                                    "WFC2 (HST)",
                                    "WFC3 UVIS (HST)"};
    /* Mixture */
    static JLabel MixtureLabel = new JLabel("Mixture");
    static JCheckBox MixtureCheck = new JCheckBox();
    static JComboBox Mixture = new JComboBox();
    static String[] MixtureChoice = {"",
                                    "Scaled Solar Model",
                                    "Alpha Enhanced"};
    /* generic min/max fields labels */
    static JLabel FMin = new JLabel("min");
    static JLabel FMax = new JLabel("max");
    /* text for user info (i.e. bad selection or maybe # of rows available at present) */
    static JLabel QueryInfo = new JLabel(QUERYINFOSTRING);

    /* query reset and submission buttons */
    static JButton SubmitQuery = new JButton("SUBMIT");
    static JButton ResetQuery = new JButton("RESET");
    
    /* Results Components */
    /* description on top */
    static JTextPane ResultsDescription = new JTextPane();
    static String ResultsDescriptionString = "No results! No query run yet!";
    static String ResultsSelectTips = "Select the desired rows and press OK to load the corresponding tables.";
    /* results in tabular format inside a scrolling pane */
    static JTable ResultsTable = new JTable();
    static JScrollPane ResultsScroll = new JScrollPane(ResultsTable);
    static TableModel ResultsData = new ResultsTableModel();
        
    /**
     * Generates the Query&Results Panel
     */
    protected static Component create() {

        /* initialiaze the panel */
        JPanel BaSTIMainPanel = new JPanel();   //panel to be returned to TOPCAT
        
        /* set components' values and defaults */
        /* Usage Minimal Description */
        QueryDescription.setText(QueryDescriptionString);
        QueryDescription.setEditable(false);
        QueryDescription.setBackground(QueryPanel.getBackground());
        /* Query Tab Components */
        /*
         * user query inputs 
         * every token has its own label and its checkbox 
         */
        /* setting dropdowns' content */
        DataType.setModel( new DefaultComboBoxModel( DataTypeChoice ) );
        Scenario.setModel( new DefaultComboBoxModel( ScenarioChoice ) );
        Type.setModel( new DefaultComboBoxModel( TypeChoice ) );
        MassLoss.setModel( new DefaultComboBoxModel( MassLossChoice ) );
        Photometry.setModel( new DefaultComboBoxModel( PhotometryChoice ) );
        Mixture.setModel( new DefaultComboBoxModel( MixtureChoice ) );
        /* setting (default) starting values for the query */
        BaSTIPanelActions.ResetQuery();
        /* Results Tab Components */
        /* description */
        ResultsDescription.setText(ResultsDescriptionString);
        ResultsDescription.setEditable(false);
        ResultsDescription.setBackground(ResultsTab.getBackground());
        /* table */
        ResultsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        ResultsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        ResultsTable.setPreferredScrollableViewportSize(new Dimension(400,100));
        ResultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        /* Action Listeners */
        /* Query Submit */
        SubmitQuery.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    BaSTIPanelActions.SubmitQuery();
                }
            }
        );
        /* Query Reset */
        ResetQuery.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    BaSTIPanelActions.ResetQuery();
                }
            }
        );

        /* Data Type Dropdown */
        DataType.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    BaSTIPanelActions.DataTypeSelection();
                }
            }
        );

        /* Listener for all dropdowns (except Data Type): Scenario,Type,Mass Loss,Photometry,Mixture */
        ActionListener DropDown = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //JComboBox selection = (JComboBox) e.getSource();
                //QueryInfo.setText(selection.getSelectedItem().toString());
                BaSTIPanelActions.CountQueryResults();
                BaSTIPanelActions.CheckRangeValues();
            }
        };
        // add the listener to the dropdowns that need it
        Scenario.addActionListener(DropDown);
        Type.addActionListener(DropDown);
        MassLoss.addActionListener(DropDown);
        Photometry.addActionListener(DropDown);
        Mixture.addActionListener(DropDown);

        /* Listener for all query text fields (Age,Mass,Z,Y,[Fe/H],[M/H] */
        CaretListener RangeUpdates = new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                //JTextField updated = (JTextField) e.getSource();
                //QueryInfo.setText(updated.getText());
                BaSTIPanelActions.CountQueryResults();
                BaSTIPanelActions.CheckRangeValues();
            }
        };
        // add the listener to all fields that need it
        AgeMin.addCaretListener(RangeUpdates);
        AgeMax.addCaretListener(RangeUpdates);
        MassMin.addCaretListener(RangeUpdates);
        MassMax.addCaretListener(RangeUpdates);
        ZMin.addCaretListener(RangeUpdates);
        ZMax.addCaretListener(RangeUpdates);
        YMin.addCaretListener(RangeUpdates);
        YMax.addCaretListener(RangeUpdates);
        FeHMin.addCaretListener(RangeUpdates);
        FeHMax.addCaretListener(RangeUpdates);
        MHMin.addCaretListener(RangeUpdates);
        MHMax.addCaretListener(RangeUpdates);

        // QUI CI VANNO TUTTI GLI ALTRI ACTION LISTENERS

        /* Arranging Panel Components */
        /* Query Panel Layout */
        QueryTabLayout();
        /* Results Tab Layout */
        ResultsTabLayout();
        /* Full Panel */
        /* add tabs */
        TabPane.add("Query", QueryTab);
        TabPane.add("Results", ResultsTab);
        /* complete generation and set window dimensions */
        BaSTIMainPanel.add(TabPane);
        BaSTIMainPanel.setMinimumSize(new Dimension(700,500));

        /* return the Panel */
        return BaSTIMainPanel;

    }

    /**
     * Arranges Query Tab components
     */
    private static void QueryTabLayout() {
        // Gridded components container
        GridBagLayout QueryGridBag = new GridBagLayout();
        GridBagConstraints Constraints = new GridBagConstraints();
        QueryTab.setLayout(QueryGridBag);
        QueryTab.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        // Adds Components to the Tab, given the layout and its attributes

        // some padding on top of the elements
        Constraints.insets = new Insets(5,0,0,0);

        // Query Description
        Constraints.gridwidth = GridBagConstraints.REMAINDER;
        Constraints.weightx = 20.;
        Constraints.weighty = 3.;
        QueryGridBag.setConstraints(QueryDescription, Constraints);
        QueryTab.add(QueryDescription);

        // Range limit labels
        Constraints.gridx = 5;
        Constraints.gridwidth = 1;
        Constraints.weightx = 3.;
        Constraints.weighty = 1.;
        QueryGridBag.setConstraints(FMin, Constraints);
        
        QueryTab.add(FMin);
        Constraints.gridx = 6;
        Constraints.gridwidth= GridBagConstraints.REMAINDER;
        Constraints.weightx = 3.;
        Constraints.weighty = 0.;
        QueryGridBag.setConstraints(FMax, Constraints);
        QueryTab.add(FMax);
        
        // 6 rows of inputs (label, check, dropdown, label, check, min, max)
        
        // DataType and Age
        Constraints.gridx = 0;
        Constraints.gridy = 2;
        Constraints.gridwidth = 1;
        Constraints.anchor = GridBagConstraints.EAST;
        Constraints.weightx = 4.5;
        Constraints.weighty = 1.;
        QueryGridBag.setConstraints(DataTypeLabel, Constraints);
        QueryTab.add(DataTypeLabel);
        
        Constraints.gridx = GridBagConstraints.RELATIVE;
        Constraints.gridy = GridBagConstraints.RELATIVE;
        Constraints.anchor = GridBagConstraints.CENTER;
        Constraints.weightx = .5;
        Constraints.weighty = 0.;
        QueryGridBag.setConstraints(DataTypeCheck, Constraints);
        QueryTab.add(DataTypeCheck);
        
        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 5.;
        QueryGridBag.setConstraints(DataType, Constraints);
        QueryTab.add(DataType);

        Constraints.fill = GridBagConstraints.NONE;
        Constraints.anchor = GridBagConstraints.EAST;
        Constraints.weightx = 3.5;
        QueryGridBag.setConstraints(AgeLabel, Constraints);
        QueryTab.add(AgeLabel);
        
        Constraints.anchor = GridBagConstraints.CENTER;
        Constraints.weightx = .5;
        QueryGridBag.setConstraints(AgeCheck, Constraints);
        QueryTab.add(AgeCheck);
        
        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(AgeMin, Constraints);
        QueryTab.add(AgeMin);
        
        Constraints.gridwidth = GridBagConstraints.REMAINDER;
        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(AgeMax, Constraints);
        QueryTab.add(AgeMax);

        // Scenario and Mass
        Constraints.gridy = 3;
        Constraints.gridwidth = 1;
        Constraints.fill = GridBagConstraints.NONE;
        Constraints.anchor = GridBagConstraints.EAST;
        Constraints.weightx = 4.5;
        Constraints.weighty = 1.;
        QueryGridBag.setConstraints(ScenarioLabel, Constraints);
        QueryTab.add(ScenarioLabel);

        Constraints.gridy = GridBagConstraints.RELATIVE;
        Constraints.anchor = GridBagConstraints.CENTER;
        Constraints.weightx = .5;
        Constraints.weighty = 0.;
        QueryGridBag.setConstraints(ScenarioCheck, Constraints);
        QueryTab.add(ScenarioCheck);

        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 5.;
        QueryGridBag.setConstraints(Scenario, Constraints);
        QueryTab.add(Scenario);

        Constraints.fill = GridBagConstraints.NONE;
        Constraints.anchor = GridBagConstraints.EAST;
        Constraints.weightx = 3.5;
        QueryGridBag.setConstraints(MassLabel, Constraints);
        QueryTab.add(MassLabel);

        Constraints.anchor = GridBagConstraints.CENTER;
        Constraints.weightx = .5;
        QueryGridBag.setConstraints(MassCheck, Constraints);
        QueryTab.add(MassCheck);

        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(MassMin, Constraints);
        QueryTab.add(MassMin);

        Constraints.gridwidth = GridBagConstraints.REMAINDER;
        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(MassMax, Constraints);
        QueryTab.add(MassMax);

        // Type and Z
        Constraints.gridy = 4;
        Constraints.gridwidth = 1;
        Constraints.fill = GridBagConstraints.NONE;
        Constraints.anchor = GridBagConstraints.EAST;
        Constraints.weightx = 4.5;
        Constraints.weighty = 1.;
        QueryGridBag.setConstraints(TypeLabel, Constraints);
        QueryTab.add(TypeLabel);

        Constraints.gridy = GridBagConstraints.RELATIVE;
        Constraints.anchor = GridBagConstraints.CENTER;
        Constraints.weightx = .5;
        Constraints.weighty = 0.;
        QueryGridBag.setConstraints(TypeCheck, Constraints);
        QueryTab.add(TypeCheck);

        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 5.;
        QueryGridBag.setConstraints(Type, Constraints);
        QueryTab.add(Type);

        Constraints.fill = GridBagConstraints.NONE;
        Constraints.anchor = GridBagConstraints.EAST;
        Constraints.weightx = 3.5;
        QueryGridBag.setConstraints(ZLabel, Constraints);
        QueryTab.add(ZLabel);

        Constraints.anchor = GridBagConstraints.CENTER;
        Constraints.weightx = .5;
        QueryGridBag.setConstraints(ZCheck, Constraints);
        QueryTab.add(ZCheck);

        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(ZMin, Constraints);
        QueryTab.add(ZMin);

        Constraints.gridwidth = GridBagConstraints.REMAINDER;
        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(ZMax, Constraints);
        QueryTab.add(ZMax);
        
        // MassLoss and Y
        Constraints.gridy = 5;
        Constraints.gridwidth = 1;
        Constraints.fill = GridBagConstraints.NONE;
        Constraints.anchor = GridBagConstraints.EAST;
        Constraints.weightx = 4.5;
        Constraints.weighty = 1.;
        QueryGridBag.setConstraints(MassLossLabel, Constraints);
        QueryTab.add(MassLossLabel);

        Constraints.gridy = GridBagConstraints.RELATIVE;
        Constraints.anchor = GridBagConstraints.CENTER;
        Constraints.weightx = .5;
        Constraints.weighty = 0.;
        QueryGridBag.setConstraints(MassLossCheck, Constraints);
        QueryTab.add(MassLossCheck);

        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 5.;
        QueryGridBag.setConstraints(MassLoss, Constraints);
        QueryTab.add(MassLoss);

        Constraints.fill = GridBagConstraints.NONE;
        Constraints.anchor = GridBagConstraints.EAST;
        Constraints.weightx = 3.5;
        QueryGridBag.setConstraints(YLabel, Constraints);
        QueryTab.add(YLabel);

        Constraints.anchor = GridBagConstraints.CENTER;
        Constraints.weightx = .5;
        QueryGridBag.setConstraints(YCheck, Constraints);
        QueryTab.add(YCheck);

        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(YMin, Constraints);
        QueryTab.add(YMin);

        Constraints.gridwidth = GridBagConstraints.REMAINDER;
        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(YMax, Constraints);
        QueryTab.add(YMax);
        
        // Photometry and FeH
        Constraints.gridy = 6;
        Constraints.gridwidth = 1;
        Constraints.fill = GridBagConstraints.NONE;
        Constraints.anchor = GridBagConstraints.EAST;
        Constraints.weightx = 4.5;
        Constraints.weighty = 1.;
        QueryGridBag.setConstraints(PhotometryLabel, Constraints);
        QueryTab.add(PhotometryLabel);

        Constraints.gridy = GridBagConstraints.RELATIVE;
        Constraints.anchor = GridBagConstraints.CENTER;
        Constraints.weightx = .5;
        Constraints.weighty = 0.;
        QueryGridBag.setConstraints(PhotometryCheck, Constraints);
        QueryTab.add(PhotometryCheck);

        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 5.;
        QueryGridBag.setConstraints(Photometry, Constraints);
        QueryTab.add(Photometry);

        Constraints.fill = GridBagConstraints.NONE;
        Constraints.anchor = GridBagConstraints.EAST;
        Constraints.weightx = 3.5;
        QueryGridBag.setConstraints(FeHLabel, Constraints);
        QueryTab.add(FeHLabel);

        Constraints.anchor = GridBagConstraints.CENTER;
        Constraints.weightx = .5;
        QueryGridBag.setConstraints(FeHCheck, Constraints);
        QueryTab.add(FeHCheck);

        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(FeHMin, Constraints);
        QueryTab.add(FeHMin);

        Constraints.gridwidth = GridBagConstraints.REMAINDER;
        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(FeHMax, Constraints);
        QueryTab.add(FeHMax);
        
        // Mixture and MH
        Constraints.gridy = 7;
        Constraints.gridwidth = 1;
        Constraints.fill = GridBagConstraints.NONE;
        Constraints.anchor = GridBagConstraints.EAST;
        Constraints.weightx = 4.5;
        Constraints.weighty = 1.;
        QueryGridBag.setConstraints(MixtureLabel, Constraints);
        QueryTab.add(MixtureLabel);

        Constraints.gridy = GridBagConstraints.RELATIVE;
        Constraints.anchor = GridBagConstraints.CENTER;
        Constraints.weightx = .5;
        Constraints.weighty = 0.;
        QueryGridBag.setConstraints(MixtureCheck, Constraints);
        QueryTab.add(MixtureCheck);

        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 5.;
        QueryGridBag.setConstraints(Mixture, Constraints);
        QueryTab.add(Mixture);

        Constraints.fill = GridBagConstraints.NONE;
        Constraints.anchor = GridBagConstraints.EAST;
        Constraints.weightx = 3.5;
        QueryGridBag.setConstraints(MHLabel, Constraints);
        QueryTab.add(MHLabel);

        Constraints.anchor = GridBagConstraints.CENTER;
        Constraints.weightx = .5;
        QueryGridBag.setConstraints(MHCheck, Constraints);
        QueryTab.add(MHCheck);

        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(MHMin, Constraints);
        QueryTab.add(MHMin);

        Constraints.gridwidth = GridBagConstraints.REMAINDER;
        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(MHMax, Constraints);
        QueryTab.add(MHMax);

        // Info on Query and Action Buttons (with some different padding)
        Constraints.insets = new Insets(10,0,0,5);
        Constraints.gridx = 2;
        Constraints.gridy = 8;
        Constraints.gridwidth = 3;
        Constraints.fill = GridBagConstraints.HORIZONTAL;
        Constraints.anchor = GridBagConstraints.WEST;
        Constraints.weightx = 8.;
        QueryGridBag.setConstraints(QueryInfo, Constraints);
        QueryTab.add(QueryInfo);

        Constraints.gridx = 5;
        Constraints.gridwidth = 1;
        Constraints.fill = GridBagConstraints.NONE;
        Constraints.anchor = GridBagConstraints.CENTER;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(ResetQuery, Constraints);
        QueryTab.add(ResetQuery);

        Constraints.gridx = 6;
        Constraints.gridwidth = GridBagConstraints.REMAINDER;
        Constraints.weightx = 3.;
        QueryGridBag.setConstraints(SubmitQuery, Constraints);
        QueryTab.add(SubmitQuery);

    }

    /**
     * Arranges Results Tab Components
     */
    private static void ResultsTabLayout() {
        // Gridded components container and attributes container
        GridBagLayout ResultsGridBag = new GridBagLayout();
        GridBagConstraints ResultsBagConstraints = new GridBagConstraints();
        ResultsTab.setLayout(ResultsGridBag);
        ResultsTab.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        
        // components arrangement
        ResultsBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        ResultsBagConstraints.fill = GridBagConstraints.BOTH;
        ResultsBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        ResultsBagConstraints.weightx = 1;
        ResultsBagConstraints.weighty = 1;
        ResultsGridBag.setConstraints(ResultsDescription, ResultsBagConstraints);
        ResultsTab.add(ResultsDescription);

        ResultsBagConstraints.weighty = 8;
        ResultsGridBag.setConstraints(ResultsScroll, ResultsBagConstraints);
        ResultsTab.add(ResultsScroll);
        
    }

    

}
