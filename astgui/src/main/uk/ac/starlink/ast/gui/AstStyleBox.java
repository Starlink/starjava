package uk.ac.starlink.ast.gui;

import javax.swing.JComboBox;

/**
 * AstStyleBox extends a JComboBox by adding a default set of values
 * that correspond to the default AST line styles (plain, dashed, dot
 * etc.).
 *
 * @since $Date$
 * @since 13-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */
public class AstStyleBox extends JComboBox 
{
    /**
     * The known AST line styles.
     */
    protected static String[] styles = 
       {"line", "dash", "dot", "shortdash", "longdash", "dotdash"};

    /**
     * The default constructor that adds the AST style controls.
     */
    public AstStyleBox() 
    {
        super();
        for ( int i = 0; i < styles.length; i++ ) {
            addItem( styles[i]);
        }
        setToolTipText( "Select a line plotting style" );
    }

    /**
     * Get the selected style. These correspond to the integers that
     * AST understands as each style.
     */
    public int getSelectedStyle() 
    {
        return getSelectedIndex() + 1;
    }

    /**
     * Set the selected style. These correspond to the integers that
     * AST understands as each style.
     */
    public void setSelectedStyle( int style ) 
    {
        setSelectedIndex( style - 1 );
    }
}
