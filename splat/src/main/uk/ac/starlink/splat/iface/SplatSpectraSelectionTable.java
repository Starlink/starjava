package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import uk.ac.starlink.splat.data.SpecData;

/**
 * SplatSpectraSelectionTable provides a swing component that displays a list of
 * all the passed spectra and allows to select a subset of it.
 * 
 * @author David Andresic
 * @version $Id$
 *
 */
public class SplatSpectraSelectionTable 
	extends JPanel {
   
	private static final long serialVersionUID = 1L;

	/**
     * The JList containing primary view of available spectra
     */
    protected JList list = null;
    
    /**
     *  Create all visual components.
     */
    protected JScrollPane scroller = new JScrollPane();
    protected JTable table = new JTable();
    
    /**
     *  Model that contains all the spectra for the table
     */
    protected TableModel tableModel = null;
    
    /**
     * JPanel for 'select all' and 'deselect all' buttons
     */
    protected JPanel buttonsPanel = new JPanel();
    
    /**
     * Button for selecting all the spectra in the model
     */
    protected JButton selectAllButton = new JButton();
    
    /**
     * Button for de-selecting all the spectra in the model
     */
    protected JButton deselectAllButton = new JButton();
    
    /**
     *  Create an instance.
     *
     *  @param list a JList that contains a list of all the currently
     *              available spectra.
     */
    public SplatSpectraSelectionTable(JList list) {
    	this.list = list;
        initUI();
	}
    
    /**
     *  Add all the components for display the list of plots and which
     *  are showing the current spectra.
     */
    protected void initUI()
    {
        // Set titles, layout etc.
    	setBorder( BorderFactory.createTitledBorder
                   ( "Select the spectra:" ) );
        setLayout( new BorderLayout() );
        setToolTipText( "Select or deselect the spectra on which the action will be performed");

        // Set up the table to use a model based on a
        // given JList of SpecData
        tableModel = new AbstractTableModel() {

			private static final long serialVersionUID = 1L;

			//@Override
			public Object getValueAt(int row, int col) {
				return list.getModel().getElementAt(row);
			}
			
			//@Override
			public int getRowCount() {
				return list.getModel().getSize();
			}
			
			//@Override
			public int getColumnCount() {
				return 1;
			}
			
			//@Override
			public Class<?> getColumnClass(int columnIndex) {
				return getValueAt(0, columnIndex).getClass();
			}
		};
        table.setModel(tableModel);

        table.setPreferredScrollableViewportSize( new Dimension( 250, 0 ) );
       
        //  Set the headers!
        TableColumnModel columns = table.getColumnModel();
        TableColumn column;
        column = columns.getColumn( table.convertColumnIndexToView( 0 ) );
        column.setHeaderValue( "Spectrum" );

        //  The table can have many rows selected.
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setSelectionModel(list.getSelectionModel());

        // set the renderer for showinf the SpecData in an
        // user-friendly way
        table.setDefaultRenderer( SpecData.class, 
                                  new SpecDataCellRenderer() );
        
        //  Add components.
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        scroller.getViewport().add( table, null );
        add( scroller, BorderLayout.CENTER );
        
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
        
        // 'Select all' button
        selectAllButton.setText("Select all");
        selectAllButton.addActionListener(new ActionListener() {
			
			//@Override
			public void actionPerformed(ActionEvent e) {
				table.setRowSelectionInterval(0, list.getModel().getSize() - 1);
			}
		});
        buttonsPanel.add(selectAllButton);
        
        // 'Deselect all' button
        deselectAllButton.setText("Deselect all");
        deselectAllButton.addActionListener(new ActionListener() {
			
			//@Override
			public void actionPerformed(ActionEvent e) {
				table.getSelectionModel().clearSelection();
			}
		});
        buttonsPanel.add(deselectAllButton);
        
        add(buttonsPanel);
    }
    
    /**
     * Clear any selected rows.
     */
    public void clearSelection()
    {
        table.clearSelection();
    }
    
    /**
     * Select an interval of the table rows to the current selection.
     */
    public void addSelectionInterval( int lower, int upper )
    {
        table.addRowSelectionInterval( lower, upper );
    }
    
    /**
     * Return a reference to the table ListSelectionModel. 
     */
    public ListSelectionModel getSelectionModel()
    {
    	return table.getSelectionModel();
    }
    
    /**
     * 
     * @return list of selected spectra
     */

	public List<SpecData> getSelectedSpectra() {
    	// TODO getSelectedValues() is deprecated since Java 1.7
		// (replaced by getSelectedValuesList())
		List<SpecData> spectra = new LinkedList<SpecData>();
    	for (Object o : list.getSelectedValues()) {
    		spectra.add((SpecData) o);
    	}
    	return spectra;
    }
}
