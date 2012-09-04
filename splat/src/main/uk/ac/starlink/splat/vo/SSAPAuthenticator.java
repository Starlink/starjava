/* Copyright (C) 2001-2005 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     17-Apr-2012 (Margarida Castro Neves mcneves@ari.uni-heidelberg.de)
 *        Original version.
 */
package uk.ac.starlink.splat.vo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

//import jsky.util.Logger;

import org.apache.commons.codec.binary.Base64;

import sun.misc.BASE64Encoder;
import uk.ac.starlink.splat.data.NameParser;
import uk.ac.starlink.splat.iface.ProgressPanel;
import uk.ac.starlink.splat.util.SplatException;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
/**
 * Class Authenticator
 * 
 * Authenticator implementation to authenticate ssap servers
 * 
 * @author Margarida Castro Neves
 */
@SuppressWarnings("serial")

public class SSAPAuthenticator extends Authenticator{

    // Logger.
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.splat.vo.SSAPAuthenticator" );

    // hash for storing  the passwords
    private HashMap <String,PasswordAuthentication> accessControl = new HashMap<String,PasswordAuthentication>();

    // store last size of authentication window
    private Dimension authWindowSize = new Dimension(400,120);

    private URL lastURL=null;
    
    private String authStatus = null;

    //private boolean hostAuth;// use same password for all in the same host.

    public PasswordAuthentication getPasswordAuthentication() {

        logger.info(  "PasswordAuthentication:-- " +  getRequestingPrompt() ); //getRequestingURL().toString() ); 
        return authenticateURL();
    }

    /**
     * authenticateURL
     * manages stored password authentication information, and/or opens a dialog for username/password input
     * @return a passwordAuthentication instance, or null if
     */
    public PasswordAuthentication authenticateURL() 
    {
        
        URL currentURL = getRequestingURL();
        String realm= getRequestingPrompt();
        boolean hostAuth=true; 
        ProgressPanel progressPanel=null;
        PasswordAuthentication auth = null;
        boolean firstTry=true;
        authStatus=null;
        
        // check if there is already a password stored for this site
        auth = getExistingAuth(realm);
        
        if (auth != null)
        {
            if (auth.getUserName().equals("SKIP")) { // skip this
                authStatus="SKIP";
                return null;
            }
            if (!currentURL.equals(lastURL)) { // if this is not a repetition, password is asked for the first time
                lastURL=currentURL;
                return auth; // returns stored authentication
            } else // if the user types a wrong password the dialog will appear again for the same URL.
                    // In this case we do not want to use the stored password, because it's wrong
                authStatus="Wrong Password";
                firstTry=false;                      
        }

        // open dialog window, ask for username and password
        AuthDialog authDialog = new AuthDialog(realm, authWindowSize);
           
        PasswordAuthentication passAuth=authDialog.getAuthentication(firstTry, realm);
        authWindowSize=authDialog.getSize();
        logger.info( "WINDOW SIZE" + authWindowSize.toString() );
        lastURL=currentURL;
    
        if (authDialog.skipThis() ) {
            // will be asked again next time ...
             logger.info( "Skipped now" + realm );
             //throw new SplatException("SKIPPED "+realm.toString());
             authStatus="SKIP";
            return null;
        }
        else if (authDialog.skipAll() ) {
            // will always skip this url
            passAuth=new PasswordAuthentication("SKIP", "SKIP".toCharArray());
          //  if (hostAuth)  
              accessControl.put( authDialog.getRealm(), passAuth);  
          //  else 
           //   accessControl.put( realm.toString(), passAuth );        
            
            logger.info( "Skipped always" + authDialog.getRealm()  );
           // throw new SplatException("SKIPPED "+realm.toString());
            authStatus="SKIP";
            return null;
        }
        if (passAuth != null) {
           // if (hostAuth)  
                accessControl.put( authDialog.getRealm(), passAuth);  
         //   else 
           //     accessControl.put( realm.toString(), passAuth);
        }
       
        return (passAuth);

    }
   
    /**
     * getStatus
     * gets the status of last authentication operation (skip, wrong password or null)
     *
     * @return the PasswordAuthentication instance stored for this url, null if not found.
     */
    public String getStatus() {
        return authStatus;
    }
    
    
    /**
     * getExistingAuth
     * searches the accessControl hashMap if this url or host has been already stored
     * @param realm the requesting url
     * @return the PasswordAuthentication instance stored for this url, null if not found.
     */
    private PasswordAuthentication getExistingAuth(String realm )
    {

        if ( ! accessControl.isEmpty() ) {
            if ( accessControl.containsKey(realm))
                return accessControl.get(realm); // search exact  URL 
 /*           else { // search for a key that is a substring of realm
                Iterator<Entry<String, PasswordAuthentication>> it = accessControl.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    if (realm.startsWith(pair.getKey().toString()) )
                       return (PasswordAuthentication) pair.getValue(); // search host           
                }
   
            }
             */
        } // accesscontrol already contains realm, authenticate
        return null;
    }
/**
 *
 * Auth Dialog: dialog window to ask for username and password
 * 
 */

    private class AuthDialog extends JDialog implements ActionListener {

        private JTextField userData;
        private JPasswordField passData;
       // private JTextField realmData;
        private JButton okButton; 
        private JButton clearButton;
        private JButton skipButton; 
        private JButton skipallButton;
        private String authString;
        private boolean skip=false;
        private boolean skipall=false;
        private String username=null;
        private char[] password=null;
        private String prompt=null;
        private String realm=null;


        public AuthDialog(String serverRealm) 
        {
            super();
            realm = serverRealm;
            setModal(true);      
            setSize(new Dimension(400,120));
            setLocationRelativeTo(null);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            //initUI();
        }

        public AuthDialog(String serverRealm, Dimension givenSize) 
        {
            super();
            realm = serverRealm;
            setModal(true);      
            setSize(givenSize);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            //initUI();
        }

        private final void initUI(boolean firstTry) 
        {
           
            setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));     
           
            JLabel message;
          
            if (firstTry)
                message = new JLabel("");
            else {
                message = new JLabel("Wrong password, try again.");
                message.setForeground(Color.RED);
            }
           
            add(message);

   /*         JPanel realmPanel = new JPanel();
            realmPanel.setLayout( new BoxLayout(realmPanel,BoxLayout.X_AXIS));
            JLabel realm = new JLabel("Realm: ");
            realmPanel.add(realm);
            realmData = new JTextField(server);        
            realmPanel.add(realmData);
            add(realmPanel);
 */           
            JPanel userPanel = new JPanel();
            userPanel.setLayout( new BoxLayout(userPanel,BoxLayout.X_AXIS));
            JLabel user = new JLabel("Username: ");
            userPanel.add(user);
            userData = new JTextField("");        
            userPanel.add(userData);
            add(userPanel);

            JPanel passPanel = new JPanel();
            passPanel.setLayout( new BoxLayout(passPanel,BoxLayout.X_AXIS));
            JLabel pass = new JLabel("Password:  ");
            passPanel.add(pass);
            passData = new JPasswordField();
            passPanel.add(passData);
            add(passPanel);

            JPanel buttons = new JPanel();
            buttons.setLayout(new GridLayout());
            okButton = new JButton("OK");
            okButton.addActionListener(this);
            okButton.setActionCommand( "ok" );   
            clearButton = new JButton("CLEAR");
            clearButton.addActionListener(this);
            clearButton.setActionCommand( "clear" );
            skipButton = new JButton("SKIP");
            skipButton.addActionListener(this);
            skipButton.setActionCommand( "skip" );
            skipallButton = new JButton("SKIP ALL");
            skipallButton.addActionListener(this);
            skipallButton.setActionCommand( "skipall" );
            buttons.add(okButton);
            buttons.add(clearButton);
            buttons.add(skipButton);
            buttons.add(skipallButton);
            add(buttons);

            setTitle("Authentication needed for "+realm);
            setVisible(true);

        }

        public void actionPerformed(ActionEvent e) {

            Object command = e.getActionCommand();

            if ( command.equals( "ok" ) ) // save table values to a file
            {
                processPassword();
              //  server=realmData.getText();
                dispose();
            }
            if ( command.equals( "clear" ) ) // save table values to a file
            {
                userData.setText("");
                passData.setText("");
            }
            if ( command.equals( "skip" ) ) // save table values to a file
            {
                skip=true;
                dispose();       
            }
            if ( command.equals( "skipall" ) ) // save table values to a file
            {
                skipall=true;
                //server=realmData.getText();
                dispose();       
            }
            if ( command.equals( "fail" ) ) // save table values to a file
            {
                dispose();       
            }

        }

        private void processPassword()
        {
            authString = userData.getText()+":"+new String(passData.getPassword());
            username = userData.getText();
            password= passData.getPassword();
            //  byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
            //  authStringEnc = new String(authEncBytes);       
        }

        public boolean skipThis()
        {
            return skip;
        }
        public boolean skipAll()
        {
            return skipall;
        }
        public String getRealm()
        {
            return realm;
        }
        
        public String getAuthString(boolean firstTry)
        {
            initUI(firstTry);
            // return(authStringEnc);
            return(authString);
        }
        public PasswordAuthentication getAuthentication(boolean firstTry, String realm)
        {
          
            if (realm != null)
                this.realm = realm;
           
            //prompt="Please enter accessp information for realm "+realm;
      
            initUI(firstTry);
          
            if (username==null || password == null)
                return null;
            return(new PasswordAuthentication(username,password));
        }

  
    }

}