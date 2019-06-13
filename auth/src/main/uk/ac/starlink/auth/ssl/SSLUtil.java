package sslUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.Subject;

public class SSLUtil
{
    
    //private static Logger log = Logger.getLogger(SSLUtil.class);

    // SSL, SSLv2mm SSLv3, TLS, TLSv1, TLSv1.1
    private static final String SSL_PROTOCOL = "TLS";

    // jceks, jks, pkcs12
    private static final String KEYSTORE_TYPE = "JKS";

    // SunX509
    private static final String KEYMANAGER_ALGORITHM = "SunX509";

    private static final String CERT_ALIAS = "opencadc_x509";

    private static final char[] THE_PASSWORD = CERT_ALIAS.toCharArray();

    /**
     * Initialise the default SSL socket factory so that all HTTPS
     * connections use the provided key store to authenticate (when the
     * server requires client authentication).
     * 
     * @see HttpsURLConnection#setDefaultSSLSocketFactory(javax.net.ssl.SSLSocketFactory)
     * @param certFile
     *            proxy certificate
     * @param keyFile
     *            private key file in DER format
     */
    public static void initSSL(File certFile, File keyFile)
    {
        SSLSocketFactory sf = getSocketFactory(certFile, keyFile);
        HttpsURLConnection.setDefaultSSLSocketFactory(sf);
    }
    public static void initSSL(File pemFile)
    {
        try
        {
            X509CertificateChain chain = readPemCertificateAndKey(pemFile);
            SSLSocketFactory sf = getSocketFactory(chain);
            HttpsURLConnection.setDefaultSSLSocketFactory(sf);
        }
        catch (InvalidKeySpecException ex)
        {
            throw new RuntimeException("failed to read RSA private key from " + pemFile, ex);
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException("BUG: failed to create empty KeyStore", ex);
        }
        catch (FileNotFoundException ex)
        {
            throw new RuntimeException("failed to find certificate and/or key file " + pemFile, ex);
        }
        catch (IOException ex)
        {
            throw new RuntimeException("failed to read certificate file " + pemFile, ex);
        }
        catch (CertificateException ex)
        {
            throw new RuntimeException("failed to load certificate from file " + pemFile, ex);
        }
    }
    

    /**
     * Initialise the default SSL socket factory so that all HTTPS
     * connections use the provided key store to authenticate (when the
     * server requies client authentication).
     * 
     * @param certFile
     *            proxy certificate
     * @param keyFile
     *            private key file in DER format
     * @return configured SSL socket factory
     */
    public static SSLSocketFactory getSocketFactory(File certFile, File keyFile)
    {
        KeyStore ks = getKeyStore(certFile, keyFile);
        KeyStore ts = null;
        return getSocketFactory(ks, ts);
    }
    
    /**
     * Initialise the default SSL socket factory so that all HTTPS
     * connections use the provided key store to authenticate (when the
     * server requires client authentication).
     * 
     * @param pemFile
     *            proxy certificate
     * @return configured SSL socket factory
     */
    public static SSLSocketFactory getSocketFactory(File pemFile)
    {
        X509CertificateChain chain;
        try
        {
            chain = readPemCertificateAndKey(pemFile);
        }
        catch (InvalidKeySpecException ex)
        {
            throw new RuntimeException("failed to read RSA private key from " + pemFile, ex);
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException("BUG: failed to create empty KeyStore", ex);
        }
        catch (IOException ex)
        {
            throw new RuntimeException("failed to read certificate file " + pemFile, ex);
        }
        catch (CertificateException ex)
        {
            throw new RuntimeException("failed to load certificate from file " + pemFile, ex);
        }
        return getSocketFactory(chain);
    }

    /**
     * Create an SSLSocketfactory from the credentials in the specified
     * Subject. This method extracts a X509CertificateChain from the
     * public credentials and uses the certificate chain and private key
     * found there to set up a KeyStore for the SSLSocketFactory.
     * 
     * @param s
     * @return an SSLSocketFactory, or null if no X509CertificateChain can
     *         be found
     */
    public static SSLSocketFactory getSocketFactory(Subject s)
    {
        X509CertificateChain chain = null;
        if (s != null)
        {
            Set<X509CertificateChain> certs = s.getPublicCredentials(X509CertificateChain.class);
            for (X509CertificateChain cc : certs)
            {
                if (cc.getKey() != null)
                {
                    chain = cc;
                    break;
                }
            }
        }
        if (chain == null)
            return null;
        return getSocketFactory(chain);
    }

    public static SSLSocketFactory getSocketFactory(X509CertificateChain chain)
    {
        KeyStore ts = null;
        KeyStore ks = null;
        if (chain != null) 
            ks = getKeyStore(chain.getChain(), chain.getPrivateKey());
        return getSocketFactory(ks, ts);
    }


    // may in future try to support other KeyStore formats
    static SSLSocketFactory getSocketFactory(KeyStore keyStore, KeyStore trustStore)
    {
        KeyManagerFactory kmf = getKeyManagerFactory(keyStore);
        TrustManagerFactory tmf = getTrustManagerFactory(trustStore);
        SSLContext ctx = getContext(kmf, tmf, keyStore);
        SSLSocketFactory sf = ctx.getSocketFactory();
        return sf;
    }

    static byte[] getPrivateKey(byte[] certBuf) throws IOException
    {
        BufferedReader rdr = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(certBuf)));
        String line = rdr.readLine();
        StringBuilder base64 = new StringBuilder();
        while (line != null)
        {
            if (line.startsWith("-----BEGIN RSA PRIVATE KEY-"))
            {
                //log.debug(line);
                line = rdr.readLine();
                while (line != null && !line.startsWith("-----END RSA PRIVATE KEY-"))
                {
                    //log.debug(line + " (" + line.length() + ")");
                    base64.append(line.trim());
                    line = rdr.readLine();
                }
                //log.debug(line);
                line = null; // break from outer loop
            }
            else
                line = rdr.readLine();
        }
        rdr.close();
        String encoded = base64.toString();
        //log.debug("RSA PRIVATE KEY: " + encoded);
        //log.debug("RSA private key: " + encoded.length() + " chars");
        // now: base64 -> byte[]
        byte[] ret = Base64.getDecoder().decode(encoded);
        //log.debug("RSA private key: " + ret.length + " bytes");

        return ret;
    }

    /**
     * Extracts all the certificates from the argument, decodes them
     * from base64 to byte[] and concatenates all the certificates 
     * preserving the order.
     * @param certBuf buffer containing certificates
     * @return decoded certificate chain
     * @throws IOException
     */
    public static byte[] getCertificates(byte[] certBuf) throws IOException
    {
        BufferedReader rdr = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(certBuf)));
        String line = rdr.readLine();

        List<byte[]> certs = new ArrayList<byte[]>(); //list of byte certificates
        int byteSize = 0;
        while (line != null)
        {
            StringBuilder base64 = new StringBuilder();
            if (line.startsWith(X509CertificateChain.CERT_BEGIN))
            {
                //log.debug(line);
                line = rdr.readLine();
                while (line != null && !line.startsWith(X509CertificateChain.CERT_END))
                {
                    //log.debug(line + " (" + line.length() + ")");
                    base64.append(line.trim());
                    line = rdr.readLine();
                }
                if (line.startsWith(X509CertificateChain.CERT_END))
                {
                    String encoded = base64.toString();
                    //log.debug("CERTIFICATE: " + encoded);
                    byte[] tmp = Base64.getDecoder().decode(encoded);
                    byteSize += tmp.length;
                    certs.add(tmp);
                }
                //log.debug(line);
            }
            else
                line = rdr.readLine();
        }
        rdr.close();

        // flatten out the certificate bytes into one byte[]
        byte[] result = new byte[byteSize];
        byteSize = 0;
        for (byte[] cert : certs)
        {
            System.arraycopy(cert, 0, result, byteSize, cert.length);
            byteSize += cert.length;
            //log.debug("CERTIFICATE: " + result);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static X509Certificate[] readCertificateChain(File certFile) throws CertificateException, IOException
    {
        try
        {
            X509Certificate[] chain = 
                readCertificateChain(readFile(certFile));
            //log.debug("X509 certificate is valid");
            return chain; 
        }       
            catch (CertificateException ex)
            {
                throw new RuntimeException("certificate from file " + certFile + " is not valid", ex);
            }
     
    }

    /**
     * @param certBuf
     * @return certificate chain
     * @throws CertificateException
     * @throws IOException
     */
    public static X509Certificate[] readCertificateChain(byte[] certBuf) throws CertificateException, IOException
    {
        BufferedInputStream istream = new BufferedInputStream(new ByteArrayInputStream(certBuf));
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ArrayList<Certificate> certs = new ArrayList<Certificate>();
        while (istream.available() > 0)
        {
            Certificate cert = cf.generateCertificate(istream);
            //log.debug("found: " + cert);
            certs.add(cert);
        }
        istream.close();

        X509Certificate[] chain = new X509Certificate[certs.size()];
        Iterator<Certificate> i = certs.iterator();
        int c = 0;
        while (i.hasNext())
        {
            X509Certificate x509 = (X509Certificate) i.next();
            chain[c++] = x509;
            try
            {
                x509.checkValidity();
                //log.debug("X509 certificate is valid");
            }
            catch (CertificateExpiredException exp)
            {
                //log.debug("X509 certificate is expired");
                // nothing to be done here
            }
            catch (CertificateException ex)
            {
                throw new RuntimeException("certificate byte array is not valid", ex);
            }
        }
        return chain;
    }

    public static PrivateKey readPrivateKey(File keyFile) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException
    {
        byte[] priv = readFile(keyFile);
        return readPrivateKey(priv);
    }

    public static PrivateKey readPrivateKey(byte[] bytesPrivateKey) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException
    {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytesPrivateKey);
        PrivateKey pk = kf.generatePrivate(spec);
        return pk;
    }

    private static KeyStore getKeyStore(Certificate[] chain, PrivateKey pk)
    {
        try
        {
            KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
            try
            {
                ks.load(null, null);
            } // empty
            catch (Exception ignore)
            {
            }
            //@SuppressWarnings("unused") KeyStore.Entry ke = new KeyStore.PrivateKeyEntry(pk, chain);
            ks.setKeyEntry(CERT_ALIAS, pk, THE_PASSWORD, chain);
            //log.debug("added certificate chain to keystore: " + CERT_ALIAS + "," + pk + "," + THE_PASSWORD + "," + chain);
            return ks;
        }
        catch (KeyStoreException ex)
        {
            if (ex.getCause() != null &&
                ex.getCause() instanceof java.security.NoSuchAlgorithmException)
            {
                throw new IllegalArgumentException(
                        "Sorry, this implementation of Java, issued by " +
                        System.getProperty("java.vendor") +
                        ", does not support CADC Certificates.");
            }
            throw new RuntimeException("failed to find/load KeyStore of type " + KEYSTORE_TYPE, ex);
        }
    }

    private static KeyStore getKeyStore(File certFile, File keyFile)
    {
        try
        {
            PrivateKey pk = readPrivateKey(keyFile);
            Certificate[] chain = readCertificateChain(certFile);
            return getKeyStore(chain, pk);
        }
        catch (InvalidKeySpecException ex)
        {
            throw new RuntimeException("failed to read RSA private key from " + keyFile, ex);
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException("BUG: failed to create empty KeyStore", ex);
        }
        catch (FileNotFoundException ex)
        {
            throw new RuntimeException("failed to find certificate and/or key file " + certFile + "," + keyFile, ex);
        }
        catch (IOException ex)
        {
            throw new RuntimeException("failed to read certificate file " + certFile, ex);
        }
        catch (CertificateException ex)
        {
            throw new RuntimeException("failed to load certificate from file " + certFile, ex);
        }
    }

    // currently broken trying to parse the openssl-generated pkcs12 file
    private static KeyStore readPKCS12(File f)
    {
        InputStream istream = null;
        try
        {
            istream = new FileInputStream(f);
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(istream, THE_PASSWORD); // assume a
                                            // non-password-protected
                                            // proxy cert
            return ks;
        }
        catch (KeyStoreException ex)
        {
            throw new RuntimeException("failed to find KeyStore for " + KEYSTORE_TYPE, ex);
        }
        catch (FileNotFoundException ex)
        {
            throw new RuntimeException("failed to find key store file " + f, ex);
        }
        catch (IOException ex)
        {
            throw new RuntimeException("failed to read key store file " + f, ex);
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException("failed to check integtrity of key store file " + f, ex);
        }
        catch (CertificateException ex)
        {
            throw new RuntimeException("failed to load proxy certificate(s) from key store file " + f, ex);
        }
        finally
        {
            try
            {
                istream.close();
            }
            catch (Throwable ignore)
            {
            }
        }
    }

    private static KeyManagerFactory getKeyManagerFactory(KeyStore keyStore)
    {
        String da = KEYMANAGER_ALGORITHM;
        try
        {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(da);
            kmf.init(keyStore, THE_PASSWORD); // assume a
                                              // non-password-protected
                                              // proxy cert
            return kmf;
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException("failed to find KeyManagerFactory for " + da, ex);
        }
        catch (KeyStoreException ex)
        {
            throw new RuntimeException("failed to init KeyManagerFactory", ex);
        }
        catch (UnrecoverableKeyException ex)
        {
            throw new RuntimeException("failed to init KeyManagerFactory", ex);
        }
    }

    private static TrustManagerFactory getTrustManagerFactory(KeyStore trustStore)
    {
        try
        {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX", "SunJSSE");
            tmf.init(trustStore);
            return tmf;
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException("BUG: failed to create TrustManagerFactory for algorithm=PKIX", ex);
        }
        catch (NoSuchProviderException ex)
        {
            throw new RuntimeException("BUG: failed to create TrustManagerFactory for provider=SunJSSE", ex);
        }
        catch (KeyStoreException ex)
        {
            throw new RuntimeException("failed to init trustManagerFactory", ex);
        }
    }

    private static SSLContext getContext(KeyManagerFactory kmf, TrustManagerFactory tmf, KeyStore ks)
    {
        try
        {
            KeyManager[] kms = kmf.getKeyManagers();
            for (int i = 0; i < kms.length; i++)
            {
                // cast is safe since we used KEYMANAGER_ALGORITHM=SunX509
                // above
                BasicX509KeyManager wrapper = new BasicX509KeyManager((X509KeyManager) kms[i], CERT_ALIAS);
                kms[i] = wrapper;
            }
            TrustManager[] tms = tmf.getTrustManagers();
            for (int i = 0; i < tms.length; i++)
            {
                // safe cast since we used PKIX, SunJSSE above
                BasicX509TrustManager wrapper = new BasicX509TrustManager((X509TrustManager) tms[i]);
                tms[i] = wrapper;
            }
            SSLContext ctx = SSLContext.getInstance(SSL_PROTOCOL);
            //log.debug("KMF returned " + kms.length + " KeyManagers");
            //log.debug("TMF returned " + tms.length + " TrustManagers");
            ctx.init(kms, tms, null);
            return ctx;
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException("failed to find SSLContext for " + SSL_PROTOCOL, ex);
        }
        catch (KeyManagementException ex)
        {
            throw new RuntimeException("failed to init SSLContext", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static void printKeyStoreInfo(KeyStore keystore) 
        throws KeyStoreException
    {
        //log.debug("Provider : " + keystore.getProvider().getName());
        //log.debug("Type : " + keystore.getType());
        //log.debug("Size : " + keystore.size());

        Enumeration en = keystore.aliases();
        while (en.hasMoreElements())
        {
            System.out.println("Alias: " + en.nextElement());
        }
    }

    /**
     * Convenience method to parse a PEM encoded file and return the
     * corresponding X509 Certificate chain.
     * 
     * @param pemFile
     * @return certificate chain
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws CertificateException
     */
    public static X509CertificateChain readPemCertificateAndKey(File pemFile) 
        throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, CertificateException
    {
        byte[] data = SSLUtil.readFile(pemFile);
        return readPemCertificateAndKey(data);
    }

    /**
     * Parses PEM encoded data that contains certificates and a key and 
     * returns the corresponding X509CertificateChain that can be used to
     * create an SSL socket. RSA is the only supporting encoding for the key.
     * 
     * @param data content encoded as PEM. 
     * @return X509 Certificate chain.
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws CertificateException
     */
    public static X509CertificateChain readPemCertificateAndKey(byte[] data) 
        throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, CertificateException
    {
        // Currently only RSA keys are supported. If the need to support
        // other encoding algorithms arises in the future, then the
        // PEMReader in the bouncycastle package should be a good
        // candidate for the job. To use this class without creating a
        // dependency on the bc package, the implementation of this method
        // can be change to look for the bc PEMReader in the classpath and
        // use it if present, otherwise default to the RSA implementation
        // below. Clients that want to use other encoding schemas will
        // have to pass the PEMReader class into the class path themselves.

        byte[] key = getPrivateKey(data);

        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateCrtKeySpec spec = parseKeySpec(key);
        PrivateKey pk = kf.generatePrivate(spec);

        byte[] certificates = getCertificates(data);
        X509Certificate[] chain = readCertificateChain(certificates);
        
        return new X509CertificateChain(chain, pk);
    }

    /**
     * Parses a byte array and constructs the corresponding RSAPrivateCrtKeySpec.
     * 
     * @param code byte array containing the key
     * @return RSAPrivateCrtKeySpec
     * @throws IOException
     */
    public static RSAPrivateCrtKeySpec parseKeySpec(byte[] code) 
        throws IOException
    {
        DerParser parser = new DerParser(code);
        
        Asn1Object sequence = parser.read();
        if (sequence.getType() != DerParser.SEQUENCE)
            throw new IOException("Invalid DER: not a sequence"); //$NON-NLS-1$
        
        // Parse inside the sequence
        parser = sequence.getParser();
        
        parser.read(); // Skip version
        BigInteger modulus = parser.read().getInteger();
        BigInteger publicExp = parser.read().getInteger();
        BigInteger privateExp = parser.read().getInteger();
        BigInteger prime1 = parser.read().getInteger();
        BigInteger prime2 = parser.read().getInteger();
        BigInteger exp1 = parser.read().getInteger();
        BigInteger exp2 = parser.read().getInteger();
        BigInteger crtCoef = parser.read().getInteger();
            
        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(
                modulus, publicExp, privateExp, prime1, prime2,
                exp1, exp2, crtCoef);
        
        return keySpec;

    }

    /**
     * Build a PEM string of certificates and private key.
     * 
     * @param certChainStr
     * @param bytesPrivateKey
     * @return certificate chain and private key as a PEM encoded string
     */
    private static String buildPEM(String certChainStr, byte[] bytesPrivateKey)
    {
        if (certChainStr == null || bytesPrivateKey == null)
            throw new RuntimeException(
                    "Cannot build PEM of cert & privateKey. An argument is null.");

        // locate the 2nd occurance of CERT_BEGIN string
        int posCertEnd = certChainStr
                .indexOf(X509CertificateChain.CERT_END);
        if (posCertEnd == -1)
            throw new RuntimeException(
                    "Cannot find END mark of certificate.");

        StringBuilder sb = new StringBuilder();
        sb.append(X509CertificateChain.PRIVATE_KEY_BEGIN);
        sb.append(X509CertificateChain.NEW_LINE);
        sb.append(Base64.getEncoder().encodeToString(bytesPrivateKey));
        sb.append(X509CertificateChain.PRIVATE_KEY_END);
        String privateKeyStr = sb.toString();

        int posSecondCertStart = certChainStr.indexOf(
                X509CertificateChain.CERT_BEGIN, posCertEnd);
        if (posSecondCertStart == -1)
        {
            // this is an end user certificate, number of certificates==1
            return (certChainStr + 
                    X509CertificateChain.NEW_LINE + privateKeyStr);
        }
        else
        {
            // private key goes in between the first and second
            // certificate in the chain
            String certStrPart1 = certChainStr.substring(0,
                    posSecondCertStart);
            String certStrPart2 = certChainStr
                    .substring(posSecondCertStart);
            return (certStrPart1 + privateKeyStr
                    + X509CertificateChain.NEW_LINE + certStrPart2);
        }
    }

    /**
     * @param chain
     * @return certificate chain and private key as a PEM encoded string
     */
    // THIS IS NOT WORKING. 
    // getEncoded() in privateKey does not use the encoding expected by PEM.
    
/*    public static String writePEMCertificateAndKey(X509CertificateChain chain)
    {
        if (chain == null) 
            return null;
        
        String certChainStr = chain.certificateString();
        byte[] bytesPrivateKey = chain.getPrivateKey().getEncoded();
        if (certChainStr == null || bytesPrivateKey == null)
            return null;
        String pemStr = SSLUtil.buildPEM(certChainStr, bytesPrivateKey);
        return pemStr;
    }*/
    

    /**
     * Checks whether the subject's certificate credentials are valid
     * at a given date. If date is missing, current time is used as 
     * reference. 
     * @param subject Subject to check
     * @param date Date the certificate is verified against. If null,
     * the credentials are verified against current time.
     * @throws CertificateException Subject has no associated certificate
     * credentials or there is a problem with the existing certificate.
     * @throws CertificateExpiredException Certificate is expired.
     * @throws CertificateNotYetValidException Certificate not valid yet.
     */
    public static void validateSubject(Subject subject, Date date)
            throws CertificateException,
            CertificateExpiredException,
            CertificateNotYetValidException
    {
        if (subject != null)
        {
            Set<X509CertificateChain> certs = subject
                    .getPublicCredentials(X509CertificateChain.class);
            if (certs.size() == 0)
            {
                // subject without certs
                throw new CertificateException(
                        "No certificates associated with subject");
            }
            X509CertificateChain chain = certs.iterator().next();
            for (X509Certificate c : chain.getChain())
            {
                if (date != null)
                {
                    c.checkValidity(date);
                }
                else
                {
                    c.checkValidity();
                }
            }
        }
    }
    
    /**
     * Read a (small) file into a byte array.
     * 
     * @param f
     * @return byte array containing the content of the file
     * @throws IOException
     */
    public static byte[] readFile(File f) throws IOException
    {
        DataInputStream dis = null;
        try
        {
            dis = new DataInputStream(new FileInputStream(f));
            byte[] ret = new byte[(int) f.length()];
            dis.readFully(ret);
            dis.close();
            return ret;
        }
        finally
        {
            if (dis != null)
                try { dis.close(); }
                catch(IOException ignore) { }
        }
    }
}