package uk.ac.starlink.splat.iface;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import uk.ac.starlink.splat.data.SpecData;

/**
 * This class renders a SpecData into a JTable cell. This uses a
 * default table cell renderer that has different values (spectrum's name)
 *
 * @author David Andresic
 * @version $Id$
 */
public class SpecDataCellRenderer 
	extends DefaultTableCellRenderer
	implements TableCellRenderer {

	private static final long serialVersionUID = 1L;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		
		SpecData specData = (SpecData) value;
		setToolTipText(specData.getFullName());
		setValue(specData.getShortName());
		
		return this;
	}

}
