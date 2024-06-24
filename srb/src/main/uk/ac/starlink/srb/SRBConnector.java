package uk.ac.starlink.srb;

import edu.sdsc.grid.io.srb.SRBAccount;
import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileSystem;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import uk.ac.starlink.connect.AuthKey;
import uk.ac.starlink.connect.Connection;
import uk.ac.starlink.connect.Connector;

/**
 * Connector for connecting to Storage Resource Broker filestores.
 * The default information is taken from information in the user's
 * <code>~/.srb</code> directory if it exists.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Mar 2005
 */
public class SRBConnector implements Connector {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.srb" );
    private static Icon icon_;

    private final static AuthKey HOST_KEY = new AuthKey( "SRB Host" );
    private final static AuthKey PORT_KEY = new AuthKey( "SRB Port" );
    private final static AuthKey USER_KEY = new AuthKey( "User Name" );
    private final static AuthKey PASSWORD_KEY = new AuthKey( "Password" );
    private final static AuthKey HOME_KEY = new AuthKey( "Home Directory" );
    private final static AuthKey DOMAIN_KEY = new AuthKey( "MDAS Home Domain" );
    private final static AuthKey RESOURCE_KEY = 
                                     new AuthKey( "Default Storage Resource" );

    private static AuthKey[] authKeys_;

    public Icon getIcon() {
        if ( icon_ == null ) {
            URL url = getClass().getResource( "jig3.gif" );
            icon_ = url == null ? null
                                : new ImageIcon( url );
        }
        return icon_;
    }

    public String getName() {
        return "SRB";
    }

    public AuthKey[] getKeys() {
        if ( authKeys_ == null ) {
            authKeys_ = new AuthKey[] {
                HOST_KEY,
                PORT_KEY,
                USER_KEY,
                PASSWORD_KEY,
                HOME_KEY,
                DOMAIN_KEY,
                RESOURCE_KEY,
            };
            configureKeys();
        }
        return (AuthKey[]) authKeys_.clone();
    }

    public Connection logIn( Map authValues ) throws IOException {
        String host = (String) authValues.get( HOST_KEY );
        int port;
        String portVal = (String) authValues.get( PORT_KEY );
        try {
            port = Integer.parseInt( portVal );
        }
        catch ( NumberFormatException e ) {
            throw new IOException( "Bad port number: " + portVal );
        }
        String userName = (String) authValues.get( USER_KEY );
        char[] passVal = (char[]) authValues.get( PASSWORD_KEY );
        String password = passVal == null ? null : new String( passVal );
        String homeDirectory = (String) authValues.get( HOME_KEY );
        String mdasDomainName = (String) authValues.get( DOMAIN_KEY );
        String defaultStorageResource = (String) authValues.get( RESOURCE_KEY );

        SRBAccount account =
            new SRBAccount( host, port, userName, password, homeDirectory,
                            mdasDomainName, defaultStorageResource );
        SRBFileSystem filesys = new SRBFileSystem( account );
        SRBFile homeDir = new SRBFile( filesys, homeDirectory );
        if ( ! homeDir.isDirectory() ) {
            throw new IOException( "Home " + homeDirectory + 
                                   " not a directory" );
        }
        return new SRBConnection( this, authValues, homeDir );
    }

    private static void configureKeys() {
        Map envInfo = getMdasEnvInfo();
        HOST_KEY.setDefault( (String) envInfo.get( "srbHost" ) );
        PORT_KEY.setDefault( envInfo.containsKey( "srbPort" ) 
                           ? (String) envInfo.get( "srbPort" )
                           : "5544" );
        USER_KEY.setDefault( (String) envInfo.get( "srbUser" ) );
        HOME_KEY.setDefault( (String) envInfo.get( "mdasCollectionName" ) );
        HOME_KEY.setRequired( true );
        DOMAIN_KEY.setDefault( (String) envInfo.get( "mdasDomainName" ) );
        RESOURCE_KEY.setDefault( (String) envInfo.get( "defaultResource" ) );

        PASSWORD_KEY.setDefault( getMdasAuth() );
        PASSWORD_KEY.setHidden( true );
    }

    /**
     * Returns a map of String->String pairs read from the 
     * file <code>.srb/.MdasEnv</code> in the user's home directory,
     * if there is one.
     *
     * @return   key-value map for .MdasEnv file
     */
    private static Map getMdasEnvInfo() {
        Map envInfo = new HashMap();
        File configDir = new File( System.getProperty( "user.home" ), ".srb" );
        File envFile = new File( configDir, ".MdasEnv" );
        try {
            BufferedReader reader =
                new BufferedReader( new FileReader( envFile ) );
            Pattern kvpat = Pattern.compile( " *([a-zA-Z_]+) +'(.*)' *" );
            for ( String line; (line = reader.readLine()) != null; ) {
                Matcher match = kvpat.matcher( line );
                if ( match.matches() ) {
                    envInfo.put( match.group( 1 ), match.group( 2 ) );
                }
            }
            reader.close();
        }
        catch ( IOException e ) {
            logger_.info( "Can't read MDAS env file for SRB config (" +
                          e + ")" );
        }
        catch ( SecurityException e ) {
            logger_.info( "Can't read MDAS env file for SRB config (" +
                          e + ")" );
        }
        return envInfo;
    }

    /**
     * Returns the string in the <code>.srb/.MdasAuth</code> file in the 
     * user's home directory, if there is one.
     *
     * @return   authorization string
     */
    private static String getMdasAuth() {
        File configDir = new File( System.getProperty( "user.home" ), ".srb" );
        File authFile = new File( configDir, ".MdasAuth" );
        try {
            BufferedReader reader = 
                new BufferedReader( new FileReader( authFile ) );
            String line = reader.readLine();
            return line;
        }
        catch ( IOException e ) {
            logger_.info( "Can't read MDAS auth file" );
        }
        return null;
    }
    
}
