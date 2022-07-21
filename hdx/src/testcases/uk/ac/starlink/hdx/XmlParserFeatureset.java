package uk.ac.starlink.hdx;

import uk.ac.starlink.hdx.HdxException;


/**
 * Helper class to deteect features of the XML parser in use.  At
 * present, the only use is to determine whether the parser is a
 * version of Xalan old enough to have particular problematic bugs.
 */
class XmlParserFeatureset {

    /** Singleton instance */
    private static XmlParserFeatureset defaultInstance = null;
    
    private boolean docbugtestResult;
    private boolean docbugtestInitialised;

    /** Private constructor */
    private XmlParserFeatureset() {
        docbugtestResult = false;
        docbugtestInitialised = false;
    }

    /** Returns the singleton instance of this class */
    static XmlParserFeatureset getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new XmlParserFeatureset();
        }
        return defaultInstance;
    }

    /**
     * Check if the XML parser has the Xalan double-end-document bug.
     * There are problems with versions of Xalan, causing various
     * tests to fail spuriously on Xalan <2.5 (?).  So check the
     * Xalan version using the org.apache.xalan.Version class and
     * return <code>true</code> if it's too early.  Of course, if this isn't
     * Xalan, or if it's a version of Xalan older than the version
     * which came packaged with Java 1.4.2 (which is when the
     * problem appeared), then this class won't exist.  In that
     * case, just forget the whole thing and return <code>true> also.
     *
     * <p>Otherwise, return <code>false</code>.
     *
     * @throws HdxException if the reflection in here throws an
     * unexpected exception
    */
    boolean xalanHasEndEndDocumentBug()
            throws HdxException {
        
        if (!docbugtestInitialised) {
        
            try {

                Class xvClass = Class.forName("org.apache.xalan.Version");
                Object xv = xvClass.newInstance();
                java.lang.reflect.Method majorM
                        = xvClass.getMethod("getMajorVersionNum", new Class[0]);
                java.lang.reflect.Method releaseM
                        = xvClass.getMethod("getReleaseVersionNum", new Class[0]);
                Integer majorV = (Integer)majorM.invoke(xv, new Object[0]);
                Integer releaseV = (Integer)releaseM.invoke(xv, new Object[0]);
                if (majorV.intValue() <= 2
                    && releaseV.intValue() < 5) {
                    java.lang.reflect.Method getVersionM
                            = xvClass.getMethod("getVersion", new Class[0]);
                    String versionS = (String)getVersionM.invoke(xv, new Object[0]);
            
                    System.err.println("XmlParserFeatureset: "
                                       + "Xalan version is " + versionS
                                       + ": omitting test known to fail before v2.5");
                    docbugtestResult = true;
                }
            } catch (ClassNotFoundException e) {
                // That's OK: it means the parser is too old
                docbugtestResult = true;
            } catch (Exception e) {
                // Not OK: the ClassNotFoundException is the only one that
                // should have been thrown            
                throw new HdxException
                    ("Unexpected exception determining Xalan version: " + e);
            } finally {

                docbugtestInitialised = true;

            }
        }

        return docbugtestResult;
    }
}

