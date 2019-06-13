package sslUtil;

import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

public class X509CertificateChain 
{
    //private static Logger log = Logger.getLogger(X509CertificateChain.class);
    
    public static final String CERT_BEGIN = "-----BEGIN CERTIFICATE-----";
    public static final String CERT_END   = "-----END CERTIFICATE-----";
    public static final String PRIVATE_KEY_BEGIN = "-----BEGIN RSA PRIVATE KEY-----";
    public static final String PRIVATE_KEY_END   = "-----END RSA PRIVATE KEY-----";
    public static final String NEW_LINE = System.getProperty("line.separator");
    

    private X500Principal principal;
    private X509Certificate endEntity;
    private X509Certificate[] chain;
    private PrivateKey key;
    private boolean isProxy;
    private Date expiryDate;
    private String csrString;
    private String hashKey;
    
    public X509CertificateChain(X500Principal principal, PrivateKey privateKey, String csrString) 
    {
        this.principal = principal;
        this.csrString = csrString;
        this.key = privateKey;
        this.hashKey = genHashKey(principal);
        this.chain = null;
        this.endEntity = null;
    }

    public X509CertificateChain(Collection<X509Certificate> certs)
    {
        if (certs == null || certs.isEmpty())
            throw new IllegalArgumentException("cannot create X509CertificateChain with no certficates");
        this.chain = certs.toArray(new X509Certificate[certs.size()]);
        genExpiryDate();
        
        initPrincipal();
        this.hashKey = genHashKey(principal);
    }

    public X509CertificateChain(X509Certificate[] chain, PrivateKey key)
    {
        if (chain == null || chain.length == 0)
            throw new IllegalArgumentException("cannot create X509CertificateChain with no certficates");
        this.chain = chain;
        genExpiryDate();
        
        this.key = key;
        initPrincipal();
        this.hashKey = genHashKey(principal);
    }

    
    private void initPrincipal()
    {
        for (X509Certificate c : chain)
        {
            this.endEntity = c;
            X500Principal sp = c.getSubjectX500Principal();
            String sdn = sp.getName(X500Principal.RFC1779);
            X500Principal ip = c.getIssuerX500Principal();
            String idn = ip.getName(X500Principal.RFC1779);
            if ( sdn.endsWith(idn) )
            {
                this.principal = ip;
                this.isProxy = true;
            }
            else
                this.principal = sp;
            
        }
        
        String canonizedDn = canonizeDistinguishedName(principal.getName());
        //TODO
        //AD: For some reason, tomcat only passes the first certificate in the
        // chain which makes this method fail if the proxy certificate has
        // more than two certificates in the chain. This issue needs to be addressed.
        // The following line is just a temporary solution
        if(canonizedDn.lastIndexOf("cn=") > -1)
        {
            canonizedDn = canonizedDn.substring(canonizedDn.lastIndexOf("cn="));
        }
        this.principal = new X500Principal(canonizedDn);
        //log.debug("principal: " + principal.getName(X500Principal.RFC1779));
    }
    
    public static X509CertificateChain findPrivateKeyChain(Set<Object> publicCredentials)
    {
        for (Object credential : publicCredentials)
        {
            if (credential instanceof X509CertificateChain)
            {
                X509CertificateChain chain = (X509CertificateChain) credential;
                if (chain.getPrivateKey() != null)
                {
                    return chain;
                }
            }
        }
        return null;
    }

    public String certificateString()
    {
        if (this.chain == null)
            return null;
        
        StringBuffer sb = new StringBuffer();
        
        for (X509Certificate cert : this.chain)
        {
            try
            {
                sb.append(CERT_BEGIN);
                sb.append(NEW_LINE);
                byte[] bytes = cert.getEncoded();
                sb.append(Base64.getEncoder().encodeToString((bytes)));
                sb.append(CERT_END);
                sb.append(NEW_LINE);
            }
            catch (CertificateEncodingException e)
            {
                e.printStackTrace();
                throw new RuntimeException("Cannot encode X509Certificate to byte[].",e);
            }
        }
        sb.deleteCharAt(sb.length()-1); // remove the last new line
        return sb.toString();
    }

    /**
     * @param dn DN to generate the hash key
     * @return hash code corresponding to the CADC canonized version of the DN
     */
    public static String genHashKey(X500Principal dn)
    {
        String dn1 = canonizeDistinguishedName(dn
                .getName());
        return Integer.toString(dn1.hashCode());
    }

    /**
     * 
     */
    private void genExpiryDate()
    {
        Date expiryDate = null;
        for (X509Certificate cert : this.chain)
        {
            Date notAfter = cert.getNotAfter();
            if (notAfter != null)
            {
                if (expiryDate == null || notAfter.before(expiryDate)) 
                    expiryDate = notAfter;
            }
        }
        this.expiryDate = expiryDate;
    }

    /**
     * @param expiryDate the expiryDate to set
     */
    public void setExpiryDate(Date expiryDate)
    {
        this.expiryDate = expiryDate;
    }

    /**
     * @return the expiryDate
     */
    public Date getExpiryDate()
    {
        return expiryDate;
    }

    /**
     * @param csrString the csrString to set
     */
    public void setCsrString(String csrString)
    {
        this.csrString = csrString;
    }

    /**
     * @return the csrString
     */
    public String getCsrString()
    {
        return csrString;
    }

    public X500Principal getPrincipal()
    {
        return principal;
    }

    public void setPrincipal(X500Principal principal)
    {
        this.principal = principal;
    }

    public PrivateKey getKey()
    {
        return key;
    }

    public void setKey(PrivateKey key)
    {
        this.key = key;
    }

    public void setChain(X509Certificate[] chain)
    {
        this.chain = chain;
        genExpiryDate();
    }





    /**
     * @param hashKey the hashKey to set
     */
    public void setHashKey(String hashKey)
    {
        this.hashKey = hashKey;
    }


    /**
     * @return the hashKey
     */
    public String getHashKey()
    {
        return hashKey;
    }

    public X500Principal getX500Principal() { return principal; }
    
    public X509Certificate[] getChain() { return chain; }

    public PrivateKey getPrivateKey() { return key; }

    public boolean isProxy() { return isProxy; }
    
    public X509Certificate getEndEntity() { return endEntity; }
    
    
    private static String canonizeDistinguishedName(String dnSrc)
    {
        try
        {
            X500Principal x = new X500Principal(dnSrc);
            x = getOrderedForm(x);
            String ret = x.getName().trim().toLowerCase();
            //log.debug(dnSrc + " converted to " + ret);
            return ret;
        }
        catch (Exception e)
        {
            //log.debug("Invalid dn", e);
            throw new IllegalArgumentException("Invalid DN: " + dnSrc, e);
        }
    }
    
    private static X500Principal getOrderedForm(X500Principal p)
    {
        try
        {
            X500Principal ret = p;
            String up = p.getName(X500Principal.RFC2253);
            LdapName dn = new LdapName(up);
            List<Rdn> rdns = dn.getRdns();
            Rdn left = rdns.get(rdns.size() - 1); // LDAP order from right-left
            Rdn right = rdns.get(0);
            //boolean cnOnLeft = "CN".equalsIgnoreCase(left.getType());
            boolean cOnleft = "C".equalsIgnoreCase(left.getType());
            boolean cnOnRight = "CN".equalsIgnoreCase(right.getType());
            //boolean cOnRight = "C".equalsIgnoreCase(right.getType());
            boolean flip = (cnOnRight || cOnleft);

            StringBuilder sb = new StringBuilder();
            if (flip)
            {
                for (Rdn r : rdns) // writing in normal order is actually flipping LDAP order
                {
                    sb.append(r.toString());
                    sb.append(",");
                }
            }
            else
            {
                for (int i = rdns.size() - 1; i >= 0; i--)
                {
                    sb.append(rdns.get(i));
                    sb.append(",");
                }
            }
            ret = new X500Principal(sb.substring(0, sb.length() - 1)); // strip off comma-space
            //log.debug("ordered form of " + up + " is " + ret);
            return ret;
        }
        catch (InvalidNameException ex)
        {
            throw new IllegalArgumentException("invalid DN: " + p.getName(), ex);
        }
        finally
        {
        }
    }
}
