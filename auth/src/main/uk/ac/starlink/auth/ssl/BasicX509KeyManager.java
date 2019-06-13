package sslUtil;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509KeyManager;

public class BasicX509KeyManager implements X509KeyManager
{
    
    //private static Logger log = Logger.getLogger(BasicX509KeyManager.class);

    private X509KeyManager keyManager;
    private String alias;
    
    /**
     * Constructor.
     *
     * @param km underlying KeyManager this class delegates to
     * @param alias the alias of the X509 certificate we always use
     */
    public BasicX509KeyManager(X509KeyManager km, String alias)
    {
        //log.debug("BasicX509KeyManager");
        this.keyManager = km;
        this.alias = alias;
    }

    public String chooseClientAlias(String[] strings, Principal[] prncpls, Socket socket)
    {
        String ret = keyManager.chooseClientAlias(strings, prncpls, socket);
        //log.debug("chooseClientAlias: looking for alias by delegating... found " + ret);
        // note sure if the above should work or not... probably not
        if (ret == null)
            ret = this.alias;
        //log.debug("chooseClientAlias: " + ret);
        return ret;
    }

    public String chooseServerAlias(String string, Principal[] prncpls, Socket socket)
    {
        String ret =  keyManager.chooseServerAlias(string, prncpls, socket);
        //log.debug("chooseServerAlias: " + ret);
        return ret;
    }

    public X509Certificate[] getCertificateChain(String alias)
    {
        //log.debug("getCertificateChain: " + alias);
        X509Certificate[] ret = keyManager.getCertificateChain(alias);
        if (ret != null)
        {
            //log.debug("looking for certificate chain by delegating... found " + ret.length);
            //for (int i=0; i<ret.length; i++)
                //log.debug("getCertificateChain: " + ret[i].getSubjectDN());
            return ret;
        }
        //log.debug("looking for certificate chain by delegating... not found");
        return null;
    }

    public String[] getClientAliases(String keyType, Principal[] prncpls)
    {
        //log.debug("getClientAliases: " + keyType);
        String[] ret = keyManager.getClientAliases(keyType, prncpls);
        //log.debug("getClientAliases found: " + ret.length);
        return ret;
    }

    public PrivateKey getPrivateKey(String alias)
    {
        PrivateKey pk = keyManager.getPrivateKey(alias);
        //log.debug("getPrivateKey for " + alias + ": " + (pk != null)); // true or false
        return pk;
    }

    public String[] getServerAliases(String keyType, Principal[] prncpls)
    {
        //log.debug("getServerAliases: " + keyType);
        String[] ret = keyManager.getServerAliases(keyType, prncpls);
        //log.debug("getServerAliases found: " + ret.length);
        return ret;
    }
       

}
