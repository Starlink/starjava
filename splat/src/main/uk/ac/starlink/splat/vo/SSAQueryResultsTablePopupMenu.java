/**
 * 
 */
package uk.ac.starlink.splat.vo;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import uk.ac.starlink.splat.util.JTableUtilities;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.table.gui.StarJTable;

/**
 * Popup menu for StarJTable wit SSA Query results.
 * 
 * @author Andresic
 *
 */
public class SSAQueryResultsTablePopupMenu extends JPopupMenu {
	
	private static final long serialVersionUID = 1L;
	
	private static final String CELL_BREAK = "\t";
	private static final String LINE_BREAK = System.getProperty("line.separator");
	
	private StarJTable starJTable;
	
	public SSAQueryResultsTablePopupMenu(StarJTable starJTable) {
		super();
		
		checkAndSetStarJTable(starJTable);
		
		addMenuItems();
	}
	
	private void checkAndSetStarJTable(StarJTable starJTable) {
		if (starJTable == null) {
			throw new IllegalArgumentException("StarJTable instance cannot be null.");
		}
		
		this.starJTable = starJTable;
	}
	
	private void addMenuItems() {
    	add(createCopyCurrentCellItem());
    	add(createCopyCurrentSelectionItem());
    	add(createCopyAllTableDataItem());
	}
	
	/**
	 * Menu item for copying the currently selected cell content to clipboard.
	 * 
	 * @return
	 */
	private JMenuItem createCopyCurrentCellItem() {
		JMenuItem menuItem = new JMenuItem("Copy current cell to clipboard");
		
		menuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String content = JTableUtilities.getCurrentCellContent(starJTable);
				
				if (content != null) {
					Utilities.addStringToClipboard(content);
				} else {
					JOptionPane.showMessageDialog(starJTable, "Invalid selection. Please select some cell.");
				}
			}
			
		});
		
		return menuItem;
	}
	
	/**
	 * Menu item for copying the all current selection content to clipboard.
	 * 
	 * @return
	 */
	private JMenuItem createCopyCurrentSelectionItem() {
		JMenuItem menuItem = new JMenuItem("Copy current selection to clipboard");
		
		menuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {				
				String content = JTableUtilities.getCurrentSelectionContent(starJTable, LINE_BREAK, CELL_BREAK);
				
				if (content != null) {
					Utilities.addStringToClipboard(content);
				} else {
					JOptionPane.showMessageDialog(starJTable, "Invalid selection. Please select some area.");
				}
			}
			
		});
		
		return menuItem;
	}
	
	/**
	 * Menu item for copying the all current selection content to clipboard.
	 * 
	 * @return
	 */
	private JMenuItem createCopyAllTableDataItem() {
		JMenuItem menuItem = new JMenuItem("Copy all table data to clipboard");
		
		menuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {				
				String content = JTableUtilities.getAllContent(starJTable, LINE_BREAK, CELL_BREAK);
				
				if (content != null) {
					Utilities.addStringToClipboard(content);
				}
			}
			
		});
		
		return menuItem;
	}
}
