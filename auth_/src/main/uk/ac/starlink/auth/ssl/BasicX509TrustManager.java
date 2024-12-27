package uk.ac.starlink.auth.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class BasicX509TrustManager implements X509TrustManager
{
    //private static Logger log = Logger.getLogger(BasicX509TrustManager.class);
    
    private static final String TRUST_ALL_PROPERTY = BasicX509TrustManager.class.getName() + ".trust";

    private X509TrustManager delegate;

    public BasicX509TrustManager(X509TrustManager delegate)
    {
        this.delegate = delegate;
    }

    public void checkClientTrusted(X509Certificate[] xcs, String str) throws CertificateException
    {
        //if (xcs != null)
        //    for (int i=0; i<xcs.length; i++)
        //        log.debug("checkClientTrusted: " + xcs[i].getSubjectDN() + "," + str);
        delegate.checkClientTrusted(xcs, str);
        //og.debug("delegate.checkClientTrusted: OK");
    }

    public void checkServerTrusted(X509Certificate[] xcs, String str) throws CertificateException
    {
        //if (xcs != null)
        //    for (int i=0; i<xcs.length; i++)
        //        log.debug("checkServerTrusted: " + xcs[i].getSubjectDN() + "," + str);
        if ( System.getProperty(TRUST_ALL_PROPERTY) != null )
        {
            //log.debug(TRUST_ALL_PROPERTY + " is set, trusting all server certificates");
            return;
        }
        delegate.checkServerTrusted(xcs, str);
        //log.debug("delegate.checkServerTrusted: OK");
    }

    public X509Certificate[] getAcceptedIssuers()
    {
        X509Certificate[] ret = delegate.getAcceptedIssuers();
        //log.debug("deletage X509TrustManager knows " + ret.length + " accepted issuers");
        return ret;
    }


}
