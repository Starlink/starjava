package uk.ac.starlink.splat.iface;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import uk.ac.starlink.splat.data.*;
import uk.ac.starlink.splat.plot.PlotControl;

/**
 *  SplatGlobalDisplayer provides a swing component that displays a list of all
 *  the known plots and which of these are displaying the "current" spectrum.
 *  <p>
 *
 *  The current spectrum is identified as the one selected in a given JList
 *  (whose indices are derived from the global GlobalSpecPlotList object, of
 *  which this is also a kind of view). The current plot is obviously the one
 *  selected in this window.
 *
 *@author       Peter W. Draper (Starlink, Durham University)
 *@created      May 31, 2002
 *@since        $Date$
 *@since        27-SEP-2000
 *@version      $Id: SplatGlobalDisplayer.java,v 1.5 2001/10/23 12:55:20 pdraper
 *      Exp $
 *@copyright    Copyright (C) 2000 Central Laboratory of the Research Councils
 */
public class SplatGlobalDisplayer extends JPanel {
    /**
     *  The JList containing primary view of available spectra (provided from
     *  SplatBrowser).
     */
    protected JList list = null;

    /**
     *  Create all visual components.
     */
    protected JScrollPane scroller = new JScrollPane();
    /**
     *  Description of the Field
     */
    protected JTable table = new JTable();
    /**
     *  Description of the Field
     */
    protected PlotTableModel tableModel = new PlotTableModel();

    /**
     *  Reference to current spectrum.
     */
    protected SpecData spec = null;

    /**
     *  Reference to global GlobalSpecPlotList object.
     */
    protected GlobalSpecPlotList globalList =
            GlobalSpecPlotList.getReference();


    /**
     *  Create a displayer.
     *
     *@param  list  a JList that contains a list of all the currently available
     *      spectra. The current spectrum is selected here.
     */
    public SplatGlobalDisplayer(JList list) {
        this.list = list;
        init();
    }


    /**
     *  Add all the components for display the list of plots and which are
     *  showing the current spectrum,.
     */
    protected void init() {
        setBorder(BorderFactory.createTitledBorder
                ("Views of current spectrum:"));
        setLayout(new BorderLayout());
        setToolTipText("Select to add the current spectrum to a plot, " +
                "deselect to remove it.");

        //  Set up the table to use a model based on the global lists
        //  of plots and spectra and to track the current spectrum
        //  when selected in list.
        table.setModel(tableModel);

        //  Hack the size of this so that it doesn't get too big for
        //  the values we know it's going to display, cell width
        //  control in JTable sucks (or I don't understand it!).
        table.setPreferredScrollableViewportSize(new Dimension(250, 0));

        //  Set the headers!
        TableColumnModel columns = table.getColumnModel();
        TableColumn column;
        column = columns.getColumn(table.convertColumnIndexToView(0));
        column.setHeaderValue("View");
        column = columns.getColumn(table.convertColumnIndexToView(1));
        column.setHeaderValue("Displayed");

        //  The table can have many rows selected.
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        //  Double click on a row raises the associated plot?
        table.addMouseListener(
            new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() >= 2) {
                        raiseCurrentPlot();
                    }
                }
            });

        //  Add components.
        scroller.getViewport().add(table, null);
        add(scroller, BorderLayout.CENTER);

        //  Set up the listSelectionListener so that we can update
        //  when the selected spectrum changes.
        list.addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    update(e);
                }
            });
    }


    /**
     *  Update the view for a new spectrum.
     *
     *@param  e  Description of the Parameter
     */
    protected void update(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            int index = list.getMinSelectionIndex();
            if (index > -1) {
                spec = globalList.getSpectrum(index);
                tableModel.setSpectrum(spec);
            }
        }
    }


    /**
     *  Return a list of the plot indices of any selected rows.
     *
     *@return    The selectedIndices value
     */
    public int[] getSelectedIndices() {
        return table.getSelectedRows();
    }


    /**
     *  Clear any selected rows.
     */
    public void clearSelection() {
        table.clearSelection();
    }


    /**
     *  Select an interval of the table rows to the current selection.
     *
     *@param  lower  The feature to be added to the SelectionInterval attribute
     *@param  upper  The feature to be added to the SelectionInterval attribute
     */
    public void addSelectionInterval(int lower, int upper) {
        table.addRowSelectionInterval(lower, upper);
    }


    /**
     *  Raise the current plot.
     */
    public void raiseCurrentPlot() {
        int row = table.getSelectedRow();
        if (row > -1) {
            PlotControl currentPlot = globalList.getPlot(row);
            try {
                Frame baseFrame = (Frame)
                        SwingUtilities.getWindowAncestor(currentPlot);
                baseFrame.setState(Frame.NORMAL);
                baseFrame.show();
            } catch (Exception e) {
            }
        }
    }


    /**
     *  Return a reference to the table ListSelectionModel. This may be used to
     *  listen for changes in the row selection (i.e. the current Plot).
     *
     *@return    The selectionModel value
     */
    public ListSelectionModel getSelectionModel() {
        return table.getSelectionModel();
    }


    /**
     *  Return a reference to the table ListSelectionModel. This may be used to
     *  listen for changes in the row selection (i.e. the current Plot).
     *
     *@return    The plotTableModel value
     */
    public PlotTableModel getPlotTableModel() {
        return tableModel;
    }
}
