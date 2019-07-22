package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeSupport;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.MutableComboBoxModel;


import uk.ac.starlink.vo.RegistryProtocol;
import uk.ac.starlink.vo.RegistryQuery;
import uk.ac.starlink.vo.RegistrySelector;
import uk.ac.starlink.vo.RegistrySelectorModel;
import uk.ac.starlink.vo.Ri1RegistryQuery;
/**
 * Component which allows the user to select a registry to interrogate
 * and a query string representing the query to be done.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Dec 2004
 */
public class SSAPRegistryQueryPanel extends JPanel {

    private PropertyChangeSupport protocolChange;
    private RegistrySelector urlSelector_;
    private RegistryProtocol regProtocol_;
    private JComboBox querySelector_;
    private JComponent qBox;
    private String [] protocols = new String[] {"RegTap (recommended)", "RI1"};
    private JComboBox regtypes ;
    
    /**
     * Constructor.
     */
    public SSAPRegistryQueryPanel(int queryProtocol) {
        super( new BorderLayout() );
        
        qBox = Box.createVerticalBox();
        add( qBox, BorderLayout.CENTER );

        /* Registry protocol  selector. */

        urlSelector_ = new RegistrySelector(new RegistrySelectorModel(RegistryProtocol.REGTAP));  


        regtypes = new JComboBox(protocols);
        qBox.add( regtypes );
        regtypes.addActionListener (new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                JComboBox source = (JComboBox) e.getSource();
                String regProto =  (String) source.getModel().getSelectedItem();
                if (regProto.equals(protocols[0])) {
                    regProtocol_=RegistryProtocol.REGTAP;
                } else if (regProto.equals(protocols[1])) {
                    regProtocol_=RegistryProtocol.RI1;
                }
                urlSelector_.setModel( new  RegistrySelectorModel(regProtocol_));
            }
        });
        regtypes.setSelectedIndex(0);
        if (queryProtocol == SplatRegistryQuery.SSAP )  
            regtypes.setEnabled(true);
        else 
            regtypes.setEnabled(false);// for ObsCore and SLAP  we just use RegTap


        /* Registry URL selector. */
//        urlSelector_ = new RegistrySelector( new  RegistrySelectorModel(RegistryProtocol.REGTAP));
        qBox.add( urlSelector_);
        
      
        /* Query text selector. */
        JComponent queryLine = Box.createHorizontalBox();
        querySelector_ = new JComboBox();
        querySelector_.setEditable( true );
        queryLine.add( new JLabel( "Query: " ) );
        queryLine.add( querySelector_ );
       // not used anyways
       // qBox.add( queryLine );
        
    }

    /**
     * Installs a set of custom queries which the user can choose from.
     * If the combo box is editable (it is by default) so the user can
     * still enter free-form queries.
     *
     * @param  queries  list of query strings
     */
    public void setPresetQueries( String[] queries ) {
        querySelector_.setModel( new DefaultComboBoxModel( queries ) );
        querySelector_.setSelectedIndex( 0 );
    }

    /**
     * Returns a RegistryQuery object which can perform the query currently
     * specified by the state of this component.  Some checking on whether
     * the fields are filled in sensibly is done; if they are not, an
     * informative MalformedURLException will be thrown.
     *
     * @return  query object
     */
    public RegistryQuery getRegistryQuery(int type) throws MalformedURLException {
           
        String regServ = (String) urlSelector_.getUrl();
        
        URL regURL = null;
        if ( regServ == null || regServ.trim().length() == 0 ) {
            throw new MalformedURLException( "Registry URL is blank" );
        }
        try {
            regURL = new URL( regServ );
        }
        catch ( MalformedURLException e ) {
            throw new MalformedURLException( "Bad registry URL: " + regServ );
        }
        
        if (regProtocol_.equals(RegistryProtocol.REGTAP)) {            
            return new SplatRegistryQuery( regURL.toString(), type );
        } else if (regProtocol_.equals(RegistryProtocol.RI1)) {
          //  if (type == SplatRegistryQuery.OBSCORE) {
                return (RegistryQuery) new SSAPRegistryQuery(regURL.toString(), (String) querySelector_.getSelectedItem().toString());
         //   }
         //   else if (type == SplatRegistryQuery.SSAP) {
          //      return (RegistryQuery) new SSAPRegistryQuery(regURL.toString(), (String) querySelector_.getSelectedItem().toString());
          //  }
        }
        return null; // should never happen
    }
 
    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        urlSelector_.setEnabled( enabled );
        querySelector_.setEnabled( false );
    }

}
