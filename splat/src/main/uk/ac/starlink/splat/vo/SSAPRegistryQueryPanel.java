package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
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
import uk.ac.starlink.vo.RegistrySelector;
import uk.ac.starlink.vo.RegistrySelectorModel;
/**
 * Component which allows the user to select a registry to interrogate
 * and a query string representing the query to be done.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Dec 2004
 */
public class SSAPRegistryQueryPanel extends JPanel {

    private RegistrySelector urlSelector_;
    private JComboBox querySelector_;

    /**
     * Constructor.
     */
    public SSAPRegistryQueryPanel() {
        super( new BorderLayout() );
        JComponent qBox = Box.createVerticalBox();
        add( qBox, BorderLayout.CENTER );

        /* Registry type  selector. */
        String [] types = new String[] {"RegTap", "RI1"};
        // TO DO...

        /* Registry URL selector. */
        urlSelector_ = new RegistrySelector( new  RegistrySelectorModel(RegistryProtocol.REGTAP));
        qBox.add( urlSelector_ );
        qBox.add( Box.createVerticalStrut( 5 ) );

        /* Query text selector. */
        JComponent queryLine = Box.createHorizontalBox();
        querySelector_ = new JComboBox();
        querySelector_.setEditable( true );
        queryLine.add( new JLabel( "Query: " ) );
        queryLine.add( querySelector_ );
        qBox.add( queryLine );
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

    public SplatRegistryQuery getRegistryQuery(String type) throws MalformedURLException {
        try {
            if (type.equalsIgnoreCase("ObsCore")) 
                return getObscoreRegistryQuery();
                      
            return getSSAPRegistryQuery();
        } catch ( MalformedURLException e ) {
            throw new MalformedURLException();
        }
    }
    /**
     * Returns a RegistryQuery object which can perform the query currently
     * specified by the state of this component.  Some checking on whether
     * the fields are filled in sensibly is done; if they are not, an
     * informative MalformedURLException will be thrown.
     *
     * @return  query object
     */
    public SplatRegistryQuery getSSAPRegistryQuery() throws MalformedURLException {
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
        return new SplatRegistryQuery( regURL.toString(), "SSAP" );
    }

    public SplatRegistryQuery getObscoreRegistryQuery() throws MalformedURLException {
        String regServ = (String) urlSelector_.getUrl();

 //       if ( query == null || query.trim().length() == 0 ) {
 //           throw new MalformedURLException( "Query URL is blank" );
 //       }
        URL regURL;
        if ( regServ == null || regServ.trim().length() == 0 ) {
            throw new MalformedURLException( "Registry URL is blank" );
        }
        try {
            regURL = new URL( regServ );
        }
        catch ( MalformedURLException e ) {
            throw new MalformedURLException( "Bad registry URL: " + regServ );
        }
        return new SplatRegistryQuery(regURL.toString(), "ObsCore" );// "ivo://ivoa.net/std/tap", where);
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        urlSelector_.setEnabled( enabled );
        querySelector_.setEnabled( false );
    }
}
