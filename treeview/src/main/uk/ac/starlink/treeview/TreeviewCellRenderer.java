package uk.ac.starlink.treeview;

import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.table.gui.StarTableCellRenderer;

public class TreeviewCellRenderer extends StarTableCellRenderer {

    protected void setValue( Object value ) {
        if ( value instanceof NDArray ) {
            NDArray nda = (NDArray) value;
            NDShape shape = nda.getShape();
            setIcon( IconFactory.getInstance()
                    .getArrayIcon( nda.getShape().getNumDims() ) );
            setText( NDShape.toString( shape ) );
        }
        else {
            super.setValue( value );
        }
    }
}
