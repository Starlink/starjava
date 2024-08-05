/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.starlink.topcat.contrib.basti;

import java.awt.Color;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import uk.ac.starlink.util.URLUtils;

/**
 *
 * @author molinaro
 */
class BaSTIPanelActions {

    /**
     * Resets Query Tab Panel to default values
     * Also resets the POST message to default values
     */
    static void ResetQuery() {
        /* reset POST message */
        BaSTIPOSTMessage.resetMessage(BaSTITableLoadDialog.POSTQuery);

        /* reset query Info text */
        BaSTIPanel.QueryInfo.setText(BaSTIPanel.QUERYINFOSTRING);

        /* reset results row selections */
        BaSTIPanel.ResultsTable.clearSelection();

        /* reset query fields */
        BaSTIPanel.DataTypeCheck.setSelected(true);
        BaSTIPanel.DataTypeCheck.setEnabled(false);
        BaSTIPanel.DataType.setSelectedIndex(0);

        BaSTIPanel.ScenarioCheck.setSelected(true);
        BaSTIPanel.ScenarioCheck.setEnabled(true);
        BaSTIPanel.Scenario.setSelectedIndex(0);

        BaSTIPanel.TypeCheck.setSelected(true);
        BaSTIPanel.TypeCheck.setEnabled(true);
        BaSTIPanel.Type.setSelectedIndex(0);

        BaSTIPanel.MassLossCheck.setSelected(true);
        BaSTIPanel.MassLossCheck.setEnabled(true);
        BaSTIPanel.MassLoss.setSelectedIndex(0);

        BaSTIPanel.PhotometryCheck.setSelected(true);
        BaSTIPanel.PhotometryCheck.setEnabled(true);
        BaSTIPanel.Photometry.setSelectedIndex(0);

        BaSTIPanel.MixtureCheck.setSelected(true);
        BaSTIPanel.MixtureCheck.setEnabled(true);
        BaSTIPanel.Mixture.setSelectedIndex(0);

        BaSTIPanel.AgeCheck.setSelected(true);
        BaSTIPanel.AgeMin.setText(null);
        BaSTIPanel.AgeMax.setText(null);

        BaSTIPanel.MassCheck.setSelected(true);
        BaSTIPanel.MassMin.setText(null);
        BaSTIPanel.MassMax.setText(null);

        BaSTIPanel.ZCheck.setSelected(true);
        BaSTIPanel.ZMin.setText(null);
        BaSTIPanel.ZMax.setText(null);

        BaSTIPanel.YCheck.setSelected(true);
        BaSTIPanel.YMin.setText(null);
        BaSTIPanel.YMax.setText(null);

        BaSTIPanel.FeHCheck.setSelected(true);
        BaSTIPanel.FeHMin.setText(null);
        BaSTIPanel.FeHMax.setText(null);

        BaSTIPanel.MHCheck.setSelected(true);
        BaSTIPanel.MHMin.setText(null);
        BaSTIPanel.MHMax.setText(null);

    }

    /**
     * Performs query on BaSTI
     */
    static void SubmitQuery() {

        /* populate POST message from query fields */
        BaSTIPOSTMessage.Populate(BaSTITableLoadDialog.POSTQuery);

        /* Query must NOT be empty */
        if ( BaSTIPOSTMessage.isEmpty(BaSTITableLoadDialog.POSTQuery) ) {
            BaSTIPanel.QueryInfo.setText("Define at least one query option!");
        } else {
            /* check query consistency */
            String CheckQuery = BaSTIPOSTMessage.Validate(BaSTITableLoadDialog.POSTQuery);
            if ( !CheckQuery.equals("correct") ) {
                BaSTIPanel.QueryInfo.setText(CheckQuery);
            } else {
                /* query is "formally" ready */
                /* prepare POST message */
                String StrPOST = "";
                String[] tempValue = new String[3];
                BaSTIPOSTMessage post = BaSTITableLoadDialog.POSTQuery;
                try {
                    // MAXIMUM number of returned results
                    // set by default and unchangeable
                    // POSTed here for server side code purposes
                    StrPOST += URLEncoder.encode("MAXOUT", "UTF-8")
                            + "=" + URLEncoder.encode(BaSTIPOSTMessage.MAX_RETURNED_RESULTS, "UTF-8");
                    // DataType == FILE_TYPE
                    tempValue = post.getDataType();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Scenario == SCENARIO_TYPE
                    tempValue = post.getScenario();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Type == TYPE
                    tempValue = post.getType();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // MassLoss == MASS_LOSS
                    tempValue = post.getMassLoss();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Photometry == PHOT_SYSTEM
                    tempValue = post.getPhotometry();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Mixture == HED_TYPE
                    tempValue = post.getMixture();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Age == AGE
                    tempValue = post.getAge();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Mass == MASS
                    tempValue = post.getMass();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Z == Z
                    tempValue = post.getZ();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Y == Y
                    tempValue = post.getY();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // FeH == Fe_H
                    tempValue = post.getFeH();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // MH == M_H
                    tempValue = post.getMH();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(BaSTIPanelActions.class.getName()).log(Level.SEVERE, null, ex);
                }
                /* do POST the message and get the response */
                // dummy variable to keep the response
                String outputline = "";
                try {
                    // open connection and POST
                    URL url = URLUtils.newURL("http://albione.oa-teramo.inaf.it/POSTQuery/getResults.php");
                    URLConnection conn = url.openConnection();
                    conn.setDoOutput(true);
                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                    wr.write(StrPOST);
                    wr.flush();
                    // Get the response
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    while ((line = rd.readLine()) != null) {
                        //System.out.println(line);
                        outputline += (outputline.length() == 0) ? line : "\n" + line;
                    }
                    wr.close();
                    rd.close();
                } catch (Exception e) {
                    System.out.println("Errore nel POST: "+e.toString());
                }
                // split result in lines
                BaSTIPOSTMessage.SQLresults = outputline.split("\n");

//                for (int j=0; j<BaSTIPOSTMessage.SQLresults.length; j++) {
//                    System.out.println(BaSTIPOSTMessage.SQLresults[j]);
//                }
                
                /* Display Results in the Results Tab */
                // first line comments the results
                String SQLOutputDesc = BaSTIPOSTMessage.SQLresults[0];
                String FullDescription = SQLOutputDesc + "\n" + BaSTIPanel.ResultsSelectTips;
                BaSTIPanel.ResultsDescription.setText(FullDescription);
                // second line contains table headers
                // the remaining lines contain result data
                /* Arrange them in the results' Table Model */
                BaSTIPanel.ResultsTable.setModel(BaSTIPanel.ResultsData);
                initColumnSizes(BaSTIPanel.ResultsTable);
                BaSTIPanel.ResultsTable.getTableHeader().setReorderingAllowed(false);

                /* Displays the Results Tab */
                BaSTIPanel.TabPane.setSelectedComponent(BaSTIPanel.ResultsTab);      
            }
        }

        /* reset previous row selection */
        BaSTIPanel.ResultsTable.clearSelection();
    }

    /**
     * Counts how many results the DB finds with present query state
     */
    static void CountQueryResults() {
        /* populate POST message from query fields */
        BaSTIPOSTMessage.Populate(BaSTITableLoadDialog.POSTQuery);

        /* Query must NOT be empty (however this should not happen here) */
        if ( BaSTIPOSTMessage.isEmpty(BaSTITableLoadDialog.POSTQuery) ) {
            BaSTIPanel.QueryInfo.setText("Define at least one query option!");
        } else {
            /* check query consistency */
            String CheckQuery = BaSTIPOSTMessage.Validate(BaSTITableLoadDialog.POSTQuery);
            if ( !CheckQuery.equals("correct") ) {
                BaSTIPanel.QueryInfo.setText(CheckQuery);
            } else {
                /* query is "formally" ready */
                /* prepare POST message */
                String StrPOST = "";
                String[] tempValue = new String[3];
                BaSTIPOSTMessage post = BaSTITableLoadDialog.POSTQuery;
                try {
                    // kept from the SUBMIT version if I decide to merge to 2 calls
//                    // MAXIMUM number of returned results
//                    // set by default and unchangeable
//                    // POSTed here for server side code purposes
//                    StrPOST += URLEncoder.encode("MAXOUT", "UTF-8")
//                            + "=" + URLEncoder.encode(BaSTIPOSTMessage.MAX_RETURNED_RESULTS, "UTF-8");
//                    // DataType == FILE_TYPE
                    tempValue = post.getDataType();
                    StrPOST += URLEncoder.encode(tempValue[1], "UTF-8") // add and "&" if MAXOUT is re-inserted
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Scenario == SCENARIO_TYPE
                    tempValue = post.getScenario();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Type == TYPE
                    tempValue = post.getType();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // MassLoss == MASS_LOSS
                    tempValue = post.getMassLoss();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Photometry == PHOT_SYSTEM
                    tempValue = post.getPhotometry();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Mixture == HED_TYPE
                    tempValue = post.getMixture();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Age == AGE
                    tempValue = post.getAge();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Mass == MASS
                    tempValue = post.getMass();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Z == Z
                    tempValue = post.getZ();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Y == Y
                    tempValue = post.getY();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // FeH == Fe_H
                    tempValue = post.getFeH();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // MH == M_H
                    tempValue = post.getMH();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(BaSTIPanelActions.class.getName()).log(Level.SEVERE, null, ex);
                }
                /* do POST the message and get the response */
                // dummy variable to keep the response
                String outputline = "";
                try {
                    // open connection and POST
                    URL url = URLUtils.newURL("http://albione.oa-teramo.inaf.it/POSTQuery/getCount.php");
                    URLConnection conn = url.openConnection();
                    conn.setDoOutput(true);
                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                    wr.write(StrPOST);
                    wr.flush();
                    // Get the response
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line = "";
                    while (( line = rd.readLine()) != null) {
                        outputline += (outputline.length() == 0) ? line : "\n" + line;
                    }
                    wr.close();
                    rd.close();
                } catch (Exception e) {
                    System.out.println("Errore nel POST: "+e.toString());
                }
                // arrange the result and set it as the QueryInfo text
                BaSTIPanel.QueryInfo.setText("Number of results (present query): " + outputline);
            }
        }
    }

    /**
     * Retrieves range minimum and mixumum values for
     * Age, Mass, Z, Y, [Fe/H] and [M/H] query fields
     */
    static void CheckRangeValues() {
        /* populate POST message from query fields */
        BaSTIPOSTMessage.Populate(BaSTITableLoadDialog.POSTQuery);

        /* Query must NOT be empty (however this should not happen here) */
        if ( BaSTIPOSTMessage.isEmpty(BaSTITableLoadDialog.POSTQuery) ) {
            BaSTIPanel.QueryInfo.setText("Define at least one query option!");
        } else {
            /* check query consistency */
            String CheckQuery = BaSTIPOSTMessage.Validate(BaSTITableLoadDialog.POSTQuery);
            if ( !CheckQuery.equals("correct") ) {
                BaSTIPanel.QueryInfo.setText(CheckQuery);
            } else {
                /* query is "formally" ready */
                /* prepare POST message */
                String StrPOST = "";
                String[] tempValue = new String[3];
                BaSTIPOSTMessage post = BaSTITableLoadDialog.POSTQuery;
                try {
                    // kept from the SUBMIT and getCount version if I decide to merge to calls
//                    // MAXIMUM number of returned results
//                    // set by default and unchangeable
//                    // POSTed here for server side code purposes
//                    StrPOST += URLEncoder.encode("MAXOUT", "UTF-8")
//                            + "=" + URLEncoder.encode(BaSTIPOSTMessage.MAX_RETURNED_RESULTS, "UTF-8");
//                    // DataType == FILE_TYPE
                    tempValue = post.getDataType();
                    StrPOST += URLEncoder.encode(tempValue[1], "UTF-8") // add and "&" if MAXOUT is re-inserted
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Scenario == SCENARIO_TYPE
                    tempValue = post.getScenario();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Type == TYPE
                    tempValue = post.getType();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // MassLoss == MASS_LOSS
                    tempValue = post.getMassLoss();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Photometry == PHOT_SYSTEM
                    tempValue = post.getPhotometry();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Mixture == HED_TYPE
                    tempValue = post.getMixture();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Age == AGE
                    tempValue = post.getAge();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Mass == MASS
                    tempValue = post.getMass();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Z == Z
                    tempValue = post.getZ();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // Y == Y
                    tempValue = post.getY();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // FeH == Fe_H
                    tempValue = post.getFeH();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                    // MH == M_H
                    tempValue = post.getMH();
                    StrPOST += "&" + URLEncoder.encode(tempValue[1], "UTF-8")
                            + "=" + URLEncoder.encode(tempValue[0]+";"+tempValue[2], "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(BaSTIPanelActions.class.getName()).log(Level.SEVERE, null, ex);
                }
                /* do POST the message and get the response */
                // dummy variable to keep the response
                String outputline = "";
                try {
                    // open connection and POST
                    URL url = URLUtils.newURL("http://albione.oa-teramo.inaf.it/POSTQuery/getRanges.php");
                    URLConnection conn = url.openConnection();
                    conn.setDoOutput(true);
                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                    wr.write(StrPOST);
                    wr.flush();
                    // Get the response
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line = "";
                    while (( line = rd.readLine()) != null) {
                        outputline += (outputline.length() == 0) ? line : "\n" + line;
                    }
                    wr.close();
                    rd.close();
                } catch (Exception e) {
                    System.out.println("Errore nel POST: "+e.toString());
                }
                // arrange the result and set the tool tip texts
                // remove last semi-colon
                outputline = outputline.substring(0, outputline.length()-2);
                String[] values = outputline.split(";");
                for (int v=0; v<values.length; v++) {
                    String[] NameValue = values[v].split("=");
                    if ( NameValue.length != 1 ) {
                        // set values
                        switch (v) {
                            case 0: BaSTIPanel.AgeMin.setToolTipText(">= " + NameValue[1]); break;
                            case 1: BaSTIPanel.AgeMax.setToolTipText("<= " + NameValue[1]); break;
                            case 2: BaSTIPanel.MassMin.setToolTipText(">= " + NameValue[1]); break;
                            case 3: BaSTIPanel.MassMax.setToolTipText("<= " + NameValue[1]); break;
                            case 4: BaSTIPanel.ZMin.setToolTipText(">= " + NameValue[1]); break;
                            case 5: BaSTIPanel.ZMax.setToolTipText("<= " + NameValue[1]); break;
                            case 6: BaSTIPanel.YMin.setToolTipText(">= " + NameValue[1]); break;
                            case 7: BaSTIPanel.YMax.setToolTipText("<= " + NameValue[1]); break;
                            case 8: BaSTIPanel.FeHMin.setToolTipText(">= " + NameValue[1]); break;
                            case 9: BaSTIPanel.FeHMax.setToolTipText("<= " + NameValue[1]); break;
                            case 10: BaSTIPanel.MHMin.setToolTipText(">= " + NameValue[1]); break;
                            case 11: BaSTIPanel.MHMax.setToolTipText("<= " + NameValue[1]); break;
                        }
                    } else {
                        // reset values if not defined
                        switch (v) {
                            case 0: BaSTIPanel.AgeMin.setToolTipText(null); break;
                            case 1: BaSTIPanel.AgeMax.setToolTipText(null); break;
                            case 2: BaSTIPanel.MassMin.setToolTipText(null); break;
                            case 3: BaSTIPanel.MassMax.setToolTipText(null); break;
                            case 4: BaSTIPanel.ZMin.setToolTipText(null); break;
                            case 5: BaSTIPanel.ZMax.setToolTipText(null); break;
                            case 6: BaSTIPanel.YMin.setToolTipText(null); break;
                            case 7: BaSTIPanel.YMax.setToolTipText(null); break;
                            case 8: BaSTIPanel.FeHMin.setToolTipText(null); break;
                            case 9: BaSTIPanel.FeHMax.setToolTipText(null); break;
                            case 10: BaSTIPanel.MHMin.setToolTipText(null); break;
                            case 11: BaSTIPanel.MHMax.setToolTipText(null); break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Modifies query panel according to the selection made by the user.
     * Like any action on the query fields calls also the CountQueryResults() method
     */
    static void DataTypeSelection() {
        /* enable all fields */
        enableAll();
        /* disables not queryable fields  */
        // Isochrone -> no Mass value
        if ( BaSTIPanel.DataType.getSelectedItem().toString().equals("Isochrone")) {
            BaSTIPanel.MassLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.MassCheck.setSelected(false);
            BaSTIPanel.MassCheck.setEnabled(false);
            BaSTIPanel.MassMin.setText(null);
            BaSTIPanel.MassMin.setEnabled(false);
            BaSTIPanel.MassMax.setText(null);
            BaSTIPanel.MassMax.setEnabled(false);
        }
        // Track -> no Age value
        if ( BaSTIPanel.DataType.getSelectedItem().toString().equals("Track") ) {
            BaSTIPanel.AgeLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.AgeCheck.setSelected(false);
            BaSTIPanel.AgeCheck.setEnabled(false);
            BaSTIPanel.AgeMin.setText(null);
            BaSTIPanel.AgeMin.setEnabled(false);
            BaSTIPanel.AgeMax.setText(null);
            BaSTIPanel.AgeMax.setEnabled(false);
        }
        // HB Track -> no Age value; Type, Scenario and Mass Loss fixed to: normal, canonical, 0.4
        if ( BaSTIPanel.DataType.getSelectedItem().toString().equals("HB Track") ) {
            // Age
            BaSTIPanel.AgeLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.AgeCheck.setSelected(false);
            BaSTIPanel.AgeCheck.setEnabled(false);
            BaSTIPanel.AgeMin.setText(null);
            BaSTIPanel.AgeMin.setEnabled(false);
            BaSTIPanel.AgeMax.setText(null);
            BaSTIPanel.AgeMax.setEnabled(false);
            // Type
            BaSTIPanel.TypeLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.Type.setSelectedItem("Normal");
            BaSTIPanel.Type.setEnabled(false);
            // Scenario
            BaSTIPanel.ScenarioLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.Scenario.setSelectedItem("Canonical");
            BaSTIPanel.Scenario.setEnabled(false);
            // Mass Loss
            BaSTIPanel.MassLossLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.MassLoss.setSelectedItem("0.4");
            BaSTIPanel.MassLoss.setEnabled(false);
        }
        // ZAHB Table and End He Table -> no Age, Mass, Mass Loss and Type values; Scenario only Canonical
        if ( BaSTIPanel.DataType.getSelectedItem().toString().equals("ZAHB Table") ||
                BaSTIPanel.DataType.getSelectedItem().toString().equals("End He Table") ) {
            // Age
            BaSTIPanel.AgeLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.AgeCheck.setSelected(false);
            BaSTIPanel.AgeCheck.setEnabled(false);
            BaSTIPanel.AgeMin.setText(null);
            BaSTIPanel.AgeMin.setEnabled(false);
            BaSTIPanel.AgeMax.setText(null);
            BaSTIPanel.AgeMax.setEnabled(false);
            // Mass
            BaSTIPanel.MassLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.MassCheck.setSelected(false);
            BaSTIPanel.MassCheck.setEnabled(false);
            BaSTIPanel.MassMin.setText(null);
            BaSTIPanel.MassMin.setEnabled(false);
            BaSTIPanel.MassMax.setText(null);
            BaSTIPanel.MassMax.setEnabled(false);
            // Type
            BaSTIPanel.TypeLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.TypeCheck.setSelected(false);
            BaSTIPanel.TypeCheck.setEnabled(false);
            BaSTIPanel.Type.setSelectedIndex(0);
            BaSTIPanel.Type.setEnabled(false);
            // Mass Loss
            BaSTIPanel.MassLossLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.MassLossCheck.setSelected(false);
            BaSTIPanel.MassLossCheck.setEnabled(false);
            BaSTIPanel.MassLoss.setSelectedIndex(0);
            BaSTIPanel.MassLoss.setEnabled(false);
            // Scenario
            BaSTIPanel.ScenarioLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.Scenario.setSelectedItem("Canonical");
            BaSTIPanel.Scenario.setEnabled(false);
        }
        // Summary Table -> no Age, Mass, Mass Loss, Photometry and Type values
        if ( BaSTIPanel.DataType.getSelectedItem().toString().equals("Summary Table") ) {
            // Age
            BaSTIPanel.AgeLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.AgeCheck.setSelected(false);
            BaSTIPanel.AgeCheck.setEnabled(false);
            BaSTIPanel.AgeMin.setText(null);
            BaSTIPanel.AgeMin.setEnabled(false);
            BaSTIPanel.AgeMax.setText(null);
            BaSTIPanel.AgeMax.setEnabled(false);
            // Mass
            BaSTIPanel.MassLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.MassCheck.setSelected(false);
            BaSTIPanel.MassCheck.setEnabled(false);
            BaSTIPanel.MassMin.setText(null);
            BaSTIPanel.MassMin.setEnabled(false);
            BaSTIPanel.MassMax.setText(null);
            BaSTIPanel.MassMax.setEnabled(false);
            // Type
            BaSTIPanel.TypeLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.TypeCheck.setSelected(false);
            BaSTIPanel.TypeCheck.setEnabled(false);
            BaSTIPanel.Type.setSelectedIndex(0);
            BaSTIPanel.Type.setEnabled(false);
            // Mass Loss
            BaSTIPanel.MassLossLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.MassLossCheck.setSelected(false);
            BaSTIPanel.MassLossCheck.setEnabled(false);
            BaSTIPanel.MassLoss.setSelectedIndex(0);
            BaSTIPanel.MassLoss.setEnabled(false);
            // Photometry
            BaSTIPanel.PhotometryLabel.setForeground(Color.LIGHT_GRAY);
            BaSTIPanel.PhotometryCheck.setSelected(false);
            BaSTIPanel.PhotometryCheck.setEnabled(false);
            BaSTIPanel.Photometry.setSelectedIndex(0);
            BaSTIPanel.Photometry.setEnabled(false);
        }
        // Count how many rows refere to the present query state
        // and return the value to the QueryInfo field
        CountQueryResults();
        CheckRangeValues();
    }

    /**
     * Sizes the table columns regarding to content and blocks their size
     */
    static void initColumnSizes(JTable table) {
        TableModel model = BaSTIPanel.ResultsData;//(MyTableModel)table.getModel();
        TableColumn column = null;
        Component comp = null;
        int headerWidth = 0;
        int cellWidth = 0;
//        Object[] longValues = model.longValues;
        TableCellRenderer headerRenderer =
            table.getTableHeader().getDefaultRenderer();

        for (int i = 0; i < model.getColumnCount(); i++) {
            column = table.getColumnModel().getColumn(i);

            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;

            String ColumnLongestValue = "";
            for (int j=0; j<model.getRowCount(); j++) {
                String content = model.getValueAt(j,i).toString();
                ColumnLongestValue = (content.length() > ColumnLongestValue.length()) ?
                                        content : ColumnLongestValue ;
            }
            ColumnLongestValue += "W"; //adds some extra blank in table columns

            comp = table.getDefaultRenderer(model.getColumnClass(i)).
                             getTableCellRendererComponent(
                                 table, ColumnLongestValue, //longValues[i],
                                 false, false, 0, i);
            cellWidth = comp.getPreferredSize().width;

            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
            column.setResizable(false);
        }
    }

    /**
     * Enables all queryable fields.
     * Age, Mass, Scenario, Mass Loss, Photometry and Type can be disabled, so these fields
     * are affected by this method.
     */
    private static void enableAll() {
        // standard color for labels
        Color LabelsColor = BaSTIPanel.DataTypeLabel.getForeground();
        // Type
        BaSTIPanel.TypeLabel.setForeground(LabelsColor);
        BaSTIPanel.TypeCheck.setSelected(true);
        BaSTIPanel.TypeCheck.setEnabled(true);
        BaSTIPanel.Type.setEnabled(true);
        // Scenario
        BaSTIPanel.ScenarioLabel.setForeground(LabelsColor);
        BaSTIPanel.ScenarioCheck.setSelected(true);
        BaSTIPanel.ScenarioCheck.setEnabled(true);
        BaSTIPanel.Scenario.setEnabled(true);
        // Mass Loss
        BaSTIPanel.MassLossLabel.setForeground(LabelsColor);
        BaSTIPanel.MassLossCheck.setSelected(true);
        BaSTIPanel.MassLossCheck.setEnabled(true);
        BaSTIPanel.MassLoss.setEnabled(true);
        // Photometry
        BaSTIPanel.PhotometryLabel.setForeground(LabelsColor);
        BaSTIPanel.PhotometryCheck.setSelected(true);
        BaSTIPanel.PhotometryCheck.setEnabled(true);
        BaSTIPanel.Photometry.setEnabled(true);
        // Age
        BaSTIPanel.AgeLabel.setForeground(LabelsColor);
        BaSTIPanel.AgeCheck.setSelected(true);
        BaSTIPanel.AgeCheck.setEnabled(true);
        BaSTIPanel.AgeMin.setEnabled(true);
        BaSTIPanel.AgeMax.setEnabled(true);
        // Mass
        BaSTIPanel.MassLabel.setForeground(LabelsColor);
        BaSTIPanel.MassCheck.setSelected(true);
        BaSTIPanel.MassCheck.setEnabled(true);
        BaSTIPanel.MassMin.setEnabled(true);
        BaSTIPanel.MassMax.setEnabled(true);
    }

}
