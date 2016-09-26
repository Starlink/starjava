/**
 * 
 */
package uk.ac.starlink.splat.vo;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
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
public class SSAQueryResultsTableSelectionMenu extends JMenu {
	
	private static final long serialVersionUID = 1L;
	
	private static final String CELL_BREAK = "\t";
	private static final String LINE_BREAK = System.getProperty("line.separator");
	
	private static final String TITLE = "Selection";
	
//	private StarJTable starJTable;
	
	public SSAQueryResultsTableSelectionMenu() {
		super(TITLE);
		
		addMenuItems();
	}
	
	protected void addMenuItems() {
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
				String content = JTableUtilities.getCurrentCellContent(getStarJTable(arg0));
				
				if (content != null) {
					Utilities.addStringToClipboard(content);
				} else {
					JOptionPane.showMessageDialog(getStarJTable(arg0), "Invalid selection. Please select some cell.");
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
				String content = JTableUtilities.getCurrentSelectionContent(getStarJTable(arg0), LINE_BREAK, CELL_BREAK);
				
				if (content != null) {
					Utilities.addStringToClipboard(content);
				} else {
					JOptionPane.showMessageDialog(getStarJTable(arg0), "Invalid selection. Please select some area.");
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
				String content = JTableUtilities.getAllContent(getStarJTable(arg0), LINE_BREAK, CELL_BREAK);
				
				if (content != null) {
					Utilities.addStringToClipboard(content);
				}
			}
			
		});
		
		return menuItem;
	}
	
	protected StarJTable getStarJTable(ActionEvent e) {
		JMenuItem jmi  = (JMenuItem) e.getSource();
        JPopupMenu jpm = (JPopupMenu) jmi.getParent();
        Component component = jpm.getInvoker();
        
        return traverseToStarJTable(component);
	}
	
	private StarJTable traverseToStarJTable(Component component) {
		System.out.println("and146: " + component);
		if (component == null) {
			return null;
		}
		
		if (component instanceof StarJTable) {
			return (StarJTable) component;
        	
        } else {
        	if (component instanceof JPopupMenu) {
        		return traverseToStarJTable(((JPopupMenu)component).getInvoker());
        	} else {
        		return traverseToStarJTable(component.getParent());
        	}
        }
	}
}
