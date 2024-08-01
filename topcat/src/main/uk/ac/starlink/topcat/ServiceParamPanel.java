package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JTextField;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.votable.datalink.ServiceParam;

/**
 * Panel displaying ServiceDescriptor parameters, and offering the user
 * the option to edit or supply their values.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2018
 */
public class ServiceParamPanel extends JPanel {

    private final Map<ServiceParam,JTextField> fieldMap_;
    private final ActionForwarder forwarder_;

    /**
     * Constructor.
     *
     * @param  params  list of parameters for the GUI
     */
    @SuppressWarnings("this-escape")
    public ServiceParamPanel( ServiceParam[] params ) {
        super( new BorderLayout() );
        fieldMap_ = new LinkedHashMap<ServiceParam,JTextField>();
        forwarder_ = new ActionForwarder();
        LabelledComponentStack stack = new LabelledComponentStack();
        for ( ServiceParam param : params ) {
            JTextField field = new JTextField();
            field.getCaret().addChangeListener( forwarder_ );
            fieldMap_.put( param, field );
            stack.addLine( param.getName(), field );
        }
        add( stack, BorderLayout.NORTH );
    }

    /**
     * Sets values for the displayed parameters.
     * Only those parameters named in the supplied map will be affected.
     * Null values in the supplied map cause the displayed parameter values
     * to be set null.
     *
     * @param   valueMap  new values for parameters
     */
    public void setValueMap( Map<ServiceParam,String> valueMap ) {
        for ( Map.Entry<ServiceParam,JTextField> entry :
              fieldMap_.entrySet() ) {
            ServiceParam param = entry.getKey();
            JTextField field = entry.getValue();
            String value = valueMap.get( param );
            field.setText( value );
            field.setCaretPosition( 0 );
        }
    }

    /**
     * Returns the values for the displayed parameters.
     * The returned map contains only entries with non-blank values.
     *
     * @return  map of parameter name/value pairs, for non-blank values only
     */
    public Map<ServiceParam,String> getValueMap() {
        Map<ServiceParam,String> valueMap =
            new LinkedHashMap<ServiceParam,String>();
        for ( Map.Entry<ServiceParam,JTextField> entry :
              fieldMap_.entrySet() ) {
            ServiceParam param = entry.getKey();
            JTextField field = entry.getValue();
            String value = field.getText();
            if ( value != null && value.length() > 0 ) {
                valueMap.put( param, value );
            }
        }
        return valueMap;
    }

    /**
     * Adds a listener to be notified if the value map may have changed.
     *
     * @param  l  listener to add
     */
    public void addActionListener( ActionListener l ) {
        forwarder_.addActionListener( l );
    }

    /**
     * Removes a listener added previously.
     *
     * @param  l  listener to remove
     */
    public void removeActionListener( ActionListener l ) {
        forwarder_.removeActionListener( l );
    }
}
