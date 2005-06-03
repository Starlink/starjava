/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     03-JUN-2005 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.ast.gui;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.NumberFormatter;

/**
 * A type of {@link JSpinner} that displays a {@link Number} using a 
 * {@link ScientificFormat} instance.
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public class ScientificSpinner
    extends JSpinner
{
    /**
     * Create an instance using the given {@link SpinnerNumberModel} and 
     * a default {@link ScientificFormat} instance.
     */
    public ScientificSpinner( SpinnerNumberModel model )
    {
        this( model, new ScientificFormat() );
    }

    /**
     * Create an instance using the given {@link SpinnerNumberModel} and 
     * {@link ScientificFormat} instances.
     */
    public ScientificSpinner( SpinnerNumberModel model, 
                              ScientificFormat format )
    {
        super( model );
        setFormat( format );
    }

    /**
     * Set the {@link ScientificFormat} instance used to render the 
     * formatted value.
     */
    public void setFormat( ScientificFormat format )
    {
        DefaultEditor e = (DefaultEditor) getEditor();
        NumberFormatter f = (NumberFormatter)e.getTextField().getFormatter();
        f.setFormat( format );
    }
}
