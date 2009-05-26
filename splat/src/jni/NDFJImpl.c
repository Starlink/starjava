/*
 *  JNI implementation of interface layer for accessing NDF library
 *  functions from NDFJ.
 *
 *  @author Peter W. Draper
 *  @date 7-SEP-1999
 *
 */

/*  Include files */
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include "uk_ac_starlink_splat_imagedata_NDFJ.h"
#include "sae_par.h"
#include "dat_par.h"
#include "star/hds.h"
#include "ndf.h"
#include "merswrap.h"
#include "f77.h"

/* Local Macros */
/* ============ */
#define MAX(a,b) ( (a) > (b) ? (a) : (b) )
#define MIN(a,b) ( (a) < (b) ? (a) : (b) )

/* Local prototypes */
/* ================ */
static int readFITSWCS( int indf, AstFrameSet **iwcs, int *status );
static int joinWCS( AstFrameSet *wcsone, AstFrameSet *wcstwo );

/** Routines for dealing with AstChannel input and output */
/*  ====================================================== */

/*  Routine for counting lines written to a channel, initialise
 *  ChanCounter first */
static jsize ChanCounter = (jsize) 0;

static void CountSink( const char *line )
{
   ChanCounter++;
}

/*  Routine for appending a line to a Java array of Strings, define
 *  ChanEnv, ChanArray and ChanCounter first */
static JNIEnv *ChanEnv = NULL;
static jobjectArray ChanArray = NULL;
static char utfBuffer[200]; /* Use static buffer so we can release Java UTF */

static void ArraySink( const char *line )
{
   jstring string = (*ChanEnv)->NewStringUTF( ChanEnv, line );
   (*ChanEnv)->SetObjectArrayElement( ChanEnv, ChanArray, ChanCounter,
                                       string );
   ChanCounter++;
}

/* Routine for reading a line from a Java array of Strings, define
 * ChanEnv, AstArray and SinkCounter first */
static const char *ArraySource( void )
{
   jobject jstr;
   char *str = NULL;
   jsize length;

   length = (*ChanEnv)->GetArrayLength( ChanEnv, ChanArray );
   if ( ChanCounter < length ) {
      jstr = (*ChanEnv)->GetObjectArrayElement( ChanEnv, ChanArray,
                                                ChanCounter );
      str = (char *) (*ChanEnv)->GetStringUTFChars( ChanEnv, jstr, NULL );
      strncpy( utfBuffer, str, 200 );
      (*ChanEnv)->ReleaseStringUTFChars( ChanEnv, jstr, str );
   }
   ChanCounter++;
   return utfBuffer;
}


/**
 * On Mac OS X fake the xargv and xargc globals. These are not resolved
 * for g77 dl-loaded libraries.
 */
#if __APPLE__
int    f__xargc;
char **f__xargv;
#endif

/*
 *  =====================
 *  Java Native Interface
 *  =====================
 */

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nInit
 *
 *  Purpose:
 *     One time initialisation of the NDF library.
 *
 *  Params:
 *     none
 */
JNIEXPORT void JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nInit
   ( JNIEnv *env, jclass class )
{
    /*  Local variables */
    int status;          /*  NDF library status */

    /* No command-line arguments so make sure NDF knows this. */
    errMark();
    status = SAI__OK;
    ndfInit( 0, NULL, &status );
    if ( status != SAI__OK ) {
        errAnnul( &status );
    }
    errRlse();
    return;
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nOpen
 *
 *  Purpose:
 *     Access an existing NDF for readonly access by name, returning
 *     the NDF identifier.
 *
 *  Params:
 *     jstring = name of NDF.
 *
 *  Returns:
 *     jint = NDF identifier, 0 if NDF not accessable.
 *
 */
JNIEXPORT jint JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nOpen
   ( JNIEnv *env, jclass class, jstring string )
{
    /*  Local variables */
    jsize slen;          /*  Length of Java name string */
    char *name;          /*  Pointer to name string */
    int indf;            /*  NDF identifier */
    int place;           /*  NDF placeholder (not used) */
    int status;          /*  NDF library status */

    /*  Import the Java string into a C char array */
    slen = (*env)->GetStringLength( env, string );
    name = (char *) (*env)->GetStringUTFChars( env, string, NULL );

    /*  Access the NDF */
    status = SAI__OK;
    errMark();
    ndfOpen( NULL, name, "READ", "OLD", &indf, &place, &status );
    if ( status != SAI__OK ) {
        errAnnul( &status );
        indf = 0;
    }
    errRlse();

    /*  Release memory */
    (*env)->ReleaseStringUTFChars( env, string, name );

    /*  Return NDF identifier */
    return (jint) indf;
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nOpenNew
 *
 *  Purpose:
 *     Create a new NDF, returning a placeholder.
 *
 *  Notes:
 *     Use copy or create to actually instantiate the NDF.
 *
 *  Params:
 *     jstring = name of NDF.
 *
 *  Returns:
 *     jint = NDF placeholder, 0 if not possible.
 *
 */
JNIEXPORT jint JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nOpenNew
   ( JNIEnv *env, jclass class, jstring string )
{
    /*  Local variables */
    jsize slen;          /*  Length of Java name string */
    char *name;          /*  Pointer to name string */
    int indf;            /*  NDF identifier */
    int place;           /*  NDF placeholder (not used) */
    int status;          /*  NDF library status */

    /*  Import the Java string into a C char array */
    slen = (*env)->GetStringLength( env, string );
    name = (char *) (*env)->GetStringUTFChars( env, string, NULL );

    /*  Access the NDF */
    status = SAI__OK;
    errMark();
    ndfOpen( NULL, name, "WRITE", "NEW", &indf, &place, &status );
    if ( status != SAI__OK ) {
        errAnnul( &status );
        place = 0;
    }
    errRlse();

    /*  Release memory */
    (*env)->ReleaseStringUTFChars( env, string, name );

    /*  Return NDF placeholder */
    return (jint) place;
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nClose
 *
 *  Purpose:
 *     Close (i.e. annul) an NDF.
 *
 *  Params:
 *     jindf = NDF identifier
 *
 */
JNIEXPORT void JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nClose
    (JNIEnv *env, jclass class, jint jindf )
{
    int status = SAI__OK;
    int indf = (int) jindf;
    ndfAnnul( &indf, &status );
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetType
 *
 *  Purpose:
 *     Return a suitable Java data type for the given NDF data
 *     component.
 *
 *     Unsigned NDF data types are returned as a signed type that
 *     preserves their values.
 *
 *  Params:
 *     jindf = NDF identifier
 *     jcomp = NDF data component
 *
 *  Returns:
 *     jint = NDF identifier.
 *
 */
JNIEXPORT jint JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetType
  (JNIEnv *env, jclass class, jint jindf, jstring jcomp)
{
    /*  Local variables */
    char comp[DAT__SZNAM+1]; /*  Pointer to name string */
    char type[NDF__SZTYP+1]; /*  Pointer to type string */
    int indf;                /*  NDF identifier */
    int status;              /*  NDF library status */
    jsize slen;              /*  String length */
    jint result = 0;         /*  NDF/Java data type */

    /*  Import the data component string  */
    slen = (*env)->GetStringUTFLength( env, jcomp );
    slen = MIN( slen, (jsize) DAT__SZNAM );
    (*env)->GetStringUTFRegion( env, jcomp, (jsize) 0, slen, comp );

    /*  Import NDF identifier  */
    indf = (int) jindf;

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  Now get the NDF data type */
    ndfType( indf, comp, type, NDF__SZTYP+1, &status );
    if ( status == SAI__OK ) {
        if ( strncmp( "_DOUBLE", type, NDF__SZTYP ) == 0 ) {
            result = uk_ac_starlink_splat_imagedata_NDFJ_DOUBLE;
        }
        else if ( strncmp( "_REAL", type, NDF__SZTYP ) == 0 ) {
            result = uk_ac_starlink_splat_imagedata_NDFJ_FLOAT;
        }
        else if ( strncmp( "_INTEGER", type, NDF__SZTYP ) == 0 ) {
            result = uk_ac_starlink_splat_imagedata_NDFJ_INTEGER;
        }
        else if ( strncmp( "_WORD", type, NDF__SZTYP ) == 0 ) {
            result = uk_ac_starlink_splat_imagedata_NDFJ_SHORT;
        }
        else if ( strncmp( "_UWORD", type, NDF__SZTYP ) == 0 ) {
            result = uk_ac_starlink_splat_imagedata_NDFJ_INTEGER;  /* to preserve range, no unsigned in Java */
        }
        else if ( strncmp( "_BYTE", type, NDF__SZTYP ) == 0 ) {
            result = uk_ac_starlink_splat_imagedata_NDFJ_BYTE;
        }
        else if ( strncmp( "_UBYTE", type, NDF__SZTYP ) == 0 ) {
            result = uk_ac_starlink_splat_imagedata_NDFJ_SHORT;    /* to preserve range, no unsigned in Java */
        }
    }

    /*  Clear NDF status, if needed and release the error stack */
    /*  TODO: convert EMS errors into Java exceptions? */
    if ( status != SAI__OK ) {
        errFlush( &status );
        errAnnul( &status );
    }
    errRlse();

    /* Return the data type */
    return result;
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetDims
 *
 *  Purpose:
 *     Get dimensionality of the NDF.
 *
 *  Params:
 *     jindf = NDF identifier
 *
 *  Returns:
 *     jintArray = array of NDF dimensions
 *
 */
JNIEXPORT jintArray JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetDims
  (JNIEnv *env, jclass class, jint jindf)
{
    /*  Local variables */
    int dims[NDF__MXDIM];    /*  NDF dimensions */
    int i;                   /*  Loop variable */
    int indf;                /*  NDF identifier */
    int ndim;                /*  Number of dimensions */
    int status;              /*  NDF library status */
    jint jdims[NDF__MXDIM];  /*  NDF dimensions as jint (long on 32 bit) */
    jintArray result;        /*  NDF dimensions array */

    /*  Default result */
    result = NULL;

    /*  Import NDF identifier  */
    indf = (int) jindf;

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  Now get the NDF dimensions */
    ndfDim( indf, NDF__MXDIM, dims, &ndim, &status );
    if ( status == SAI__OK ) {
        result = (*env)->NewIntArray( env, (jsize) ndim );
        if ( result != NULL ) {

            /* Copy NDF dimensions into jint array */
            for ( i = 0; i < ndim; i++ ) {
              jdims[i] = (jint) dims[i];
            }

            /*  Finally copy the data into place */
            (*env)->SetIntArrayRegion( env, result, (jsize) 0,
                                       (jsize) ndim, jdims );
        }
    }

    /*  Clear NDF status, if needed and release the error stack */
    /*  TODO: convert EMS errors into Java exceptions? */
    if ( status != SAI__OK ) {
      errFlush( &status );
      errAnnul( &status );
    }
    errRlse();

    /* Return the data type */
    return result;
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nHas
 *
 *  Purpose:
 *     Check if an NDF component exists
 *
 *  Params:
 *     jindf = NDF identifier
 *     jcomp = NDF component name
 *
 *  Returns:
 *     jboolean = true when component exists
 *
 */
JNIEXPORT jboolean JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nHas
  (JNIEnv *env, jclass class, jint jindf, jstring jcomp )
{
    /*  Local variables */
    char comp[DAT__SZNAM+1]; /*  Pointer to name string */
    int exists;              /*  Whether component exists */
    int indf;                /*  NDF identifier */
    int status;              /*  NDF library status */
    jboolean result;         /*  Return value */
    jsize slen;              /*  String length */

    /*  Default result */
    result = JNI_FALSE;

    /*  Import NDF identifier  */
    indf = (int) jindf;

    /*  Import the data component string  */
    slen = (*env)->GetStringUTFLength( env, jcomp );
    slen = MIN( slen, (jsize) DAT__SZNAM );
    (*env)->GetStringUTFRegion( env, jcomp, (jsize) 0, slen, comp );

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  Now check state */
    ndfState( indf, comp, &exists, &status );
    if ( status == SAI__OK ) {
        if ( exists ) {
            result = JNI_TRUE;
        }
    }

    /*  Clear NDF status, if needed and release the error stack */
    /*  TODO: convert EMS errors into Java exceptions? */
    if ( status != SAI__OK ) {
      errFlush( &status );
      errAnnul( &status );
    }
    errRlse();

    /* Return the data type */
    return result;
}

/* TODO: merge the next two pairs of routines ... or remove the unused
 * versions */

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetAstArray
 *
 *  Purpose:
 *     Return a String array containing a native description of the
 *     AST FrameSet that describes the various coordinate systems of
 *     the NDF.
 *
 *  Notes:
 *     If available the NDF WCS component is used, otherwise an
 *     attempt to create a FrameSet from the FITS headers is performed.
 *
 *  Params:
 *     jindf = NDF identifier
 *
 *  Returns:
 *     array of Strings containing the encoded FrameSet.
 */
JNIEXPORT jobjectArray JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetAstArray
    (JNIEnv *env, jclass class, jint jindf )
{
    /*  Local variables */
    int exists;              /*  Whether WCS/FITS component exists */
    int indf;                /*  NDF identifier */
    int status;              /*  NDF library status */
    AstFrameSet *iwcs = NULL;    /*  Pointer to AstFrameSet */
    AstFrameSet *ndfwcs = NULL;  /*  Pointer to NDF basic AstFrameSet */
    AstChannel *chan = NULL; /* Channel used to encode the FrameSet */
    jclass strClass;         /* Class of a String */

    /*  Import the NDF identifier */
    indf = (int) jindf;

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  See if the NDF WCS component exists */
    ndfState( indf, "WCS", &exists, &status );
    if ( exists ) {
        ndfGtwcs( indf, &iwcs, &status );
    }
    else {
        /*  Need to check for a WCS system in the FITS headers */
        ndfXstat( indf, "FITS", &exists, &status );
        if ( exists ) {
            exists = readFITSWCS( indf, &iwcs, &status );
        }

        /*  Get the basic NDF coordinate systems. */
        ndfGtwcs( indf, &ndfwcs, &status );

        /*  If FITS WCS found then merge these to produce an overall
         *  WCS system
         */
        if ( exists && joinWCS( ndfwcs, iwcs ) ) {
            astAnnul( iwcs );
            iwcs = ndfwcs;
        }
        else {
            /*  no FITS, or has bad WCS, so use plain NDF coordinates */
            iwcs = ndfwcs;
        }
    }

    /* Convert the FrameSet into an array of Strings, cut down the
     * information to a minimum */
    ChanEnv = env;
    ChanCounter = 0;
    chan = astChannel( NULL, CountSink, "comment=0,full=-1" );
    astWrite( chan, iwcs );
    astAnnul( chan );

    /* Allocate Java array and write the object to it*/
    strClass = (*env)->FindClass( env, "java/lang/String" );
    ChanArray = (*env)->NewObjectArray( env, ChanCounter, strClass, NULL );

    ChanCounter = 0;
    chan = astChannel( NULL, ArraySink, "comment=0,full=-1" );
    astWrite( chan, iwcs );
    astAnnul( chan );
    astAnnul( iwcs );

    /*  Clear NDF status, if needed and release the error stack */
    if ( status != SAI__OK ) {
        errFlush( &status );
        errAnnul( &status );
    }
    errRlse();

    /*  Clear any pending AST problems */
    if ( ! astOK ) {
        astClearStatus;
    }

    /* Return the array of Strings */
    return ChanArray;
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetAst
 *
 *  Purpose:
 *     Return a pointer to the AST FrameSet that describes the various
 *     coordinate systems of the NDF.
 *
 *  Notes:
 *     If available the NDF WCS component is used, otherwise an
 *     attempt to create a FrameSet from the FITS headers is performed.
 *
 *  Params:
 *     jindf = NDF identifier
 *
 *  Returns:
 *     pointer to the FrameSet.
 */
JNIEXPORT jlong JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetAst
    (JNIEnv *env, jclass class, jint jindf )
{
    /*  Local variables */
    int exists;              /*  Whether WCS/FITS component exists */
    int indf;                /*  NDF identifier */
    int status;              /*  NDF library status */
    AstFrameSet *iwcs = NULL;    /*  Pointer to AstFrameSet */
    AstFrameSet *ndfwcs = NULL;  /*  Pointer to NDF basic AstFrameSet */
    jlong jwcs;              /* Return AstFrameSet */

    /*  Import the NDF identifier */
    indf = (int) jindf;

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  See if the NDF WCS component exists */
    ndfState( indf, "WCS", &exists, &status );
    if ( exists ) {
        ndfGtwcs( indf, &iwcs, &status );
    }
    else {
        /*  Need to check for a WCS system in the FITS headers */
        ndfXstat( indf, "FITS", &exists, &status );
        if ( exists ) {
            exists = readFITSWCS( indf, &iwcs, &status );
        }

        /*  Get the basic NDF coordinate systems. */
        ndfGtwcs( indf, &ndfwcs, &status );

        /*  If FITS WCS found then merge these to produce an overall
         *  WCS system
         */
        if ( exists && joinWCS( ndfwcs, iwcs ) ) {
            astAnnul( iwcs );
            iwcs = ndfwcs;
        }
        else {
            /*  no FITS, or bad WCS, so use plain NDF coordinates */
            iwcs = ndfwcs;
        }
    }

    /*  Clear NDF status, if needed and release the error stack */
    if ( status != SAI__OK ) {
        if ( iwcs != NULL ) {
            astAnnul( iwcs );
        }
        errFlush( &status );
        errAnnul( &status );
    }
    errRlse();

    /*  Clear any pending AST problems */
    if ( ! astOK ) {
        astClearStatus;
        if ( iwcs != NULL ) {
            astAnnul( iwcs );
        }
    }

    /* Return the frameset. Unlock from this thread so can be used
     * in other threads. */
    astUnlock( iwcs, 0 );
    *(AstFrameSet **) &jwcs = iwcs ;
    return jwcs;
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nSetAstArray
 *
 *  Purpose:
 *     Set the NDF AST FrameSet from a description stored in a String
 *     array.
 *
 *  Notes:
 *     The supplied Frameset overwrites any existing frameset
 *     completely.
 *
 *  Params:
 *     jindf = NDF identifier
 *     jarray = reference to the AST FrameSet stored as a Java String
 *              array.
 *
 */
JNIEXPORT void JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nSetAstArray
    (JNIEnv *env, jclass class, jint jindf, jobjectArray jarray )
{
    /*  Local variables */
    int indf;                 /*  NDF identifier */
    int status;               /*  NDF library status */
    AstFrame *base;           /*  Pointer to base frame */
    AstFrameSet *iwcs = NULL; /*  Pointer to AstFrameSet */
    AstChannel *chan = NULL;  /*  Channel used to convert String array
                               *  into FrameSet */

    /*  Import the NDF identifier */
    indf = (int) jindf;

    /*  Import the AST FrameSet into a Channel */
    ChanEnv = env;
    ChanCounter = 0;
    ChanArray = jarray;
    chan = astChannel( ArraySource, NULL, "" );
    iwcs = (AstFrameSet*) astRead( chan );
    astAnnul( chan );

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  NDF insists that the FrameSet have a base frame with domain
     *  Grid, so just ensure this (we could check this, but then we'd
     *  probably set it to grid anyway, so just do it).
     */
    base = astGetFrame( iwcs, AST__BASE );
    astSet( base, "Domain=Grid" );
    astAnnul( base );

    /*  Set the NDF WCS */
    ndfPtwcs( iwcs, indf, &status );

    /*  Clear NDF status, if needed and release the error stack */
    if ( status != SAI__OK ) {
        if ( iwcs != NULL ) {
            astAnnul( iwcs );
        }
        errFlush( &status );
        errAnnul( &status );
    }
    errRlse();

    /*  Clear any pending AST problems */
    if ( ! astOK ) {
        astClearStatus;
        if ( iwcs != NULL ) {
            astAnnul( iwcs );
        }
    }
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nSetAst
 *
 *  Purpose:
 *     Set the NDF AST FrameSet.
 *
 *  Notes:
 *     The supplied Frameset overwrites any existing frameset
 *     completely.
 *
 *  Params:
 *     jindf = NDF identifier
 *     jwcs = reference to the AST FrameSet.
 *
 */
JNIEXPORT void JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nSetAst
    (JNIEnv *env, jclass class, jint jindf, jlong jwcs )
{
    /*  Local variables */
    int indf;                 /*  NDF identifier */
    int status;               /*  NDF library status */
    AstFrame *base;           /*  Pointer to base frame */
    AstFrameSet *iwcs = NULL; /*  Pointer to AstFrameSet */

    /*  Import the NDF identifier */
    indf = (int) jindf;

    /*  Import the AST FrameSet. This should be unlocked so it can be used
     *  in this thread. Note we wait for unlock... */
    iwcs = *(AstFrameSet **) &jwcs ;
    astLock( iwcs, 1 );

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  NDF insists that the FrameSet have a base frame with domain
     *  Grid, so just ensure this (we could check this, but then we'd
     *  probably set it to grid anyway, so just do it).
     */
    base = astGetFrame( iwcs, AST__BASE );
    astSet( base, "Domain=Grid" );
    astAnnul( base );

    /*  Set the NDF WCS. Note that the NDF has a cached AST FrameSet
     *  reference so this must be the same thread as used when the NDF was
     *  opened. */
    ndfPtwcs( iwcs, indf, &status );

    /*  Clear NDF status, if needed and release the error stack */
    if ( status != SAI__OK ) {
        if ( iwcs != NULL ) {
            iwcs = astAnnul( iwcs );
        }
        errFlush( &status );
        errAnnul( &status );
    }
    errRlse();

    /*  Clear any pending AST problems */
    if ( ! astOK ) {
        astClearStatus;
        if ( iwcs != NULL ) {
            astAnnul( iwcs );
        }
    }
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nAstClose
 *
 *  Purpose:
 *     Close (i.e. annul) an AST FrameSet.
 *
 *  Params:
 *     iwcs = AST reference.
 *
 */
JNIEXPORT void JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nAstClose
    (JNIEnv *env, jclass class, jlong iwcs)
{
    AstFrameSet *frameset;
    frameset = *(AstFrameSet **) &iwcs;

    /* Lock into this thread first. Note we wait for lock... */
    astLock( iwcs, 1 );
    astAnnul( frameset );
}

/*
 *   Name:
 *      Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetCharComp
 *
 *   Purpose:
 *      Return the value of an NDF character component.
 *
 *   Params:
 *     jindf = NDF identifier
 *     jcomp = Character component.
 *
 *   Return:
 *     Java String with value of component, or "".
 *
 */
JNIEXPORT jstring JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetCharComp
  (JNIEnv *env, jclass class, jint jindf, jstring jcomp )
{
    /* Local variables */
    char *comp;
    char value[NDF__SZHMX];
    int indf;
    int status;

    /*  Import the NDF identifier */
    indf = (int) jindf;

    /*  Import component name */
    comp = (char *)(*env)->GetStringUTFChars( env, jcomp, NULL );

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  Get the value */
    value[0] = '\0';
    ndfCget( indf, comp, value, NDF__SZHMX, &status );

    /*  Release variables */
    (*env)->ReleaseStringUTFChars( env, jcomp, comp );

    /*  Clear NDF status, if needed and release the error stack */
    if ( status != SAI__OK ) {
        errFlush( &status );
        errAnnul( &status );
        value[0] = '\0';
    }
    errRlse();

    /*  Return the value */
    return (*env)->NewStringUTF( env, value );
}

/*
 *   Name:
 *      Java_uk_ac_starlink_splat_imagedata_NDFJ_nSetCharComp
 *
 *   Purpose:
 *      Set the value of an NDF character component.
 *
 *   Params:
 *     jindf = NDF identifier
 *     jcomp = Character component
 *     jvalue = Character component value
 *
 */
JNIEXPORT void JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nSetCharComp
  (JNIEnv *env, jclass class, jint jindf, jstring jcomp, jstring jvalue )
{
    /* Local variables */
    char *comp;
    char *value;
    int indf;
    int status;

    /*  Import the NDF identifier */
    indf = (int) jindf;

    /*  Import component name */
    comp = (char *)(*env)->GetStringUTFChars( env, jcomp, NULL );

    /*  Import component value */
    value = (char *)(*env)->GetStringUTFChars( env, jvalue, NULL );

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  Set the value */
    ndfCput( value, indf, comp, &status );

    /*  Release variables */
    (*env)->ReleaseStringUTFChars( env, jcomp, comp );
    (*env)->ReleaseStringUTFChars( env, jvalue, value );

    /*  Clear NDF status, if needed and release the error stack */
    if ( status != SAI__OK ) {
        errFlush( &status );
        errAnnul( &status );
    }
    errRlse();
}

/*
 *   Name:
 *      Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetTemp
 *
 *   Purpose:
 *      Return placeholder for a temporary NDF.
 *
 *   Params:
 *      None
 *
 *   Return:
 *      NDF placeholder.
 *
 */
JNIEXPORT jint JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetTemp
  (JNIEnv *env, jclass class )
{
    /* Local variables */
    int place;
    int status;

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  Get the placeholder */
    ndfTemp( &place, &status );

    /*  Clear NDF status, if needed and release the error stack */
    if ( status != SAI__OK ) {
        errFlush( &status );
        errAnnul( &status );
        place = NDF__NOPL;
    }
    errRlse();

    /*  Return the value */
    return (jint) place;

}

/*
 *   Name:
 *      Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetCopy
 *
 *   Purpose:
 *      Return a copy of an NDF.
 *
 *   Params:
 *      ident identifier of the NDF to be copied.
 *      place placeholder for the new NDF.
 *
 *   Notes:
 *      Do not access the WCS component from other threads. It is
 *      not unlockable.
 *
 *   Return:
 *      NDF idenitifer.
 *
 */
JNIEXPORT jint JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetCopy
  (JNIEnv *env, jclass class, jint jindf, jint jplace )
{
    /* Local variables */
    int in;
    int out;
    int place;
    int status;

    /*  Import the NDF identifier and placeholder */
    in = (int) jindf;
    place = (int) jplace;

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  Create the copy */
    ndfCopy( in, &place, &out, &status );

    /*  Clear NDF status, if needed and release the error stack */
    if ( status != SAI__OK ) {
        errFlush( &status );
        errAnnul( &status );
        out = NDF__NOID;
    }
    errRlse();

    /*  Return the value */
    return (jint) out;
}

/*
 *   Name:
 *      Java_uk_ac_starlink_splat_imagedata_NDFJ_nGet1DNewDouble
 *
 *   Purpose:
 *      Return an identifier for a new 1D NDF.
 *
 *   Params:
 *      place placeholder for the new NDF.
 *      size of the new NDF.
 *
 *   Return:
 *      NDF idenitifer.
 *
 */
JNIEXPORT jint JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nGet1DNewDouble
  (JNIEnv *env, jclass class, jint jplace, jint jsize )
{
    /* Local variables */
    int indf;
    int place;
    int lbnd[1];
    int ubnd[1];
    int status;

    /*  Import the NDF placeholder */
    place = (int) jplace;

    /*  Set the size of the NDF */
    lbnd[0] = 1;
    ubnd[0] = (int) jsize;

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  Create the new NDF */
    ndfNew( "_DOUBLE", 1, lbnd, ubnd, &place, &indf, &status );

    /*  Clear NDF status, if needed and release the error stack */
    if ( status != SAI__OK ) {
        errFlush( &status );
        errAnnul( &status );
        indf = NDF__NOID;
    }
    errRlse();

    /*  Return the value */
    return (jint) indf;
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nSet1DDouble
 *
 *  Purpose:
 *     Set a data component of an NDF with the supplied values.
 *
 *  Params:
 *     jindf = NDF identifier
 *     jcomp = name of NDF component.
 *     jvalues = Array of double precision values to copy.
 *
 */
JNIEXPORT void JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nSet1DDouble
    ( JNIEnv *env, jclass class, jint jindf, jstring jcomp, jdoubleArray jvalues )
{

   /*  Local variables */
   char comp[DAT__SZNAM+1]; /*  Pointer to component name string */
   double *inpntr;          /*  Pointer to input data */
   double *outpntr;         /*  Pointer to NDF component */
   int outel;               /*  Number of elements in data component */
   int inel;                /*  Number of elements for copying  */
   int i;                   /*  Loop variable */
   int indf;                /*  NDF identifier */
   int status;              /*  NDF library status */
   int state;               /*  Status of component */
   void *vpntr[1];          /*  Pointer to data */
   int slen;                /*  Length of component string */

   /*  Import the data component string  */
   slen = (*env)->GetStringUTFLength( env, jcomp );
   slen = MIN( slen, (jsize) DAT__SZNAM );
   (*env)->GetStringUTFRegion( env, jcomp, (jsize) 0, slen, comp );

   /*  Import NDF identifier  */
   indf = (int) jindf;

   /*  Import the data values */
   inel = (*env)->GetArrayLength( env, jvalues );
   inpntr = (*env)->GetDoubleArrayElements( env, jvalues, 0 );

   /*  Establish local status and stop NDF from issuing errors */
   status = SAI__OK;
   errMark();

   /*  Map the requested NDF data component */
   if ( strcmp( comp, "error" ) == 0 ) {
       ndfState( indf, "variance", &state, &status );
   }
   else {
       ndfState( indf, comp, &state, &status );
   }
   if ( state == 1 ) {
      ndfMap( indf, comp, "_DOUBLE", "UPDATE", vpntr, &outel, &status );
   }
   else {
      ndfMap( indf, comp, "_DOUBLE", "WRITE/BAD", vpntr, &outel, &status );
   }
   outpntr = vpntr[0];

   /*  Copy the data, stopping when none remains. */
   if ( status == SAI__OK ) {
       for ( i = 0; i < MIN( inel, outel ); i++ ) {
           outpntr[i] = inpntr[i];
       }
   }

   /*  Clear NDF status, if needed and release the error stack */
   /*  TODO: convert EMS errors into Java exceptions? */
   if ( status != SAI__OK ) {
      errFlush( &status );
      errAnnul( &status );
   }
   errRlse();

   /*  Release the mapped NDF component */
   if ( strcmp( comp, "error" ) == 0 ) {
       ndfUnmap( indf, "variance", &status );
   }
   else {
       ndfUnmap( indf, comp, &status );

   }


   /*  Release input data */
   (*env)->ReleaseDoubleArrayElements( env, jvalues, inpntr, 0 );
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nHasExtension
 *
 *  Purpose:
 *     Check if an NDF extension exists
 *
 *  Params:
 *     jindf = NDF identifier
 *     jcomp = NDF extension name
 *
 *  Returns:
 *     jboolean = true when extension exists
 *
 */
JNIEXPORT jboolean JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nHasExtension
  (JNIEnv *env, jclass class, jint jindf, jstring jcomp )
{
    /*  Local variables */
    char exten[DAT__SZNAM+1]; /*  Pointer to name string */
    int exists;               /*  Whether extension exists */
    int indf;                 /*  NDF identifier */
    int status;               /*  NDF library status */
    jboolean result;          /*  Return value */
    jsize slen;               /*  String length */

    /*  Default result */
    result = JNI_FALSE;

    /*  Import NDF identifier  */
    indf = (int) jindf;

    /*  Import the extension string  */
    slen = (*env)->GetStringUTFLength( env, jcomp );
    slen = MIN( slen, (jsize) DAT__SZNAM );
    (*env)->GetStringUTFRegion( env, jcomp, (jsize) 0, slen, exten );

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  Now check state */
    ndfXstat( indf, exten, &exists, &status );
    if ( status == SAI__OK ) {
        if ( exists ) {
            result = JNI_TRUE;
        }
    }

    /*  Clear NDF status, if needed and release the error stack */
    /*  TODO: convert EMS errors into Java exceptions? */
    if ( status != SAI__OK ) {
        errFlush( &status );
        errAnnul( &status );
    }
    errRlse();

    /* Return the data type */
    return result;
}

/*
 *  Structure to contain a FITS block memory pointer and the number of
 *  cards that it stores.
 */
typedef struct FitsHeader {
    char *ptr;               /* Pointer to memory */
    size_t ncard;            /* Number of cards */
} FitsHeader;

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nAccessFitsHeaders
 *
 *  Purpose:
 *     Return a pointer to a structure that contains memory copy of
 *     the FITS headers of an NDF.
 *
 *  Notes:
 *     Must be invoked before nGetFitsHeader and should be released.
 *
 *  Params:
 *     jindf = NDF identifier
 *
 *  Returns:
 *     jlong = pointer to the FITS header structure, 0 if not found.
 *
 */
JNIEXPORT jlong JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nAccessFitsHeaders
    (JNIEnv *env, jclass class, jint jindf )
{
    /*  Local variables */
    char *mapped;            /* Pointer to mapped FITS headers */
    HDSLoc *loc = NULL;      /* HDS locator to FITS block */
    int exists;              /* Whether FITS component exists */
    int indf;                /* NDF identifier */
    int status;              /* NDF library status */
    jlong jfits;             /* Return reference to FITS structure */
    FitsHeader *header;      /* Header information structure */

    /*  Import the NDF identifier */
    indf = (int) jindf;

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  See if the FITS extension exists */
    ndfXstat( indf, "FITS", &exists, &status );
    if ( exists ) {

        /*  Create a container for this pointer and its size */
        header = malloc( sizeof( FitsHeader ) );

        /*  Get a locator to the FITS block */
        ndfXloc( indf, "FITS", "READ", &loc, &status );

        /*  Access the FITS headers */
        datMapV( loc, "_CHAR*80", "READ", (void **) &mapped,
                 &header->ncard, &status );

        /*  If failed, then return a null header */
        if ( status != SAI__OK ) {
            free( header );
            header = NULL;
        }
        else {
            /* Make the memory copy */
            header->ptr = malloc( 80 * header->ncard );
            memcpy( header->ptr, mapped, 80 * header->ncard );

            /* Free the locator */
            datAnnul( &loc, &status );
        }
    }
    else {
        header = NULL;
    }

    /*  Clear NDF status, if needed and release the error stack */
    if ( status != SAI__OK ) {
        errFlush( &status );
        errAnnul( &status );
    }
    errRlse();

    /*  Return the FITS block pointer */
    *(FitsHeader **) &jfits = header;
    return jfits;
}

/*
 *   Name:
 *      Java_uk_ac_starlink_splat_imagedata_NDFJ_nCountFitsHeaders
 *
 *   Purpose:
 *      Return the number of cards in a FITS header.
 *
 *   Params:
 *      jfits reference to a mapped FITS header block.
 *
 *   Return:
 *      Number of FITS cards.
 *
 */
JNIEXPORT jint JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nCountFitsHeaders
  (JNIEnv *env, jclass class, jlong jfits )
{
    /* Local variables */
    FitsHeader *fits;

    /*  Import the FITS header structure */
    fits = *(FitsHeader **) &jfits;

    /*  Return the count */
    if ( fits != NULL ) {
        return (jint) fits->ncard;
    }
    return (jint) 0;
}

/*
 *   Name:
 *      Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetFitsHeader
 *
 *   Purpose:
 *      Return a FITS header card.
 *
 *   Params:
 *      jfits reference to a mapped FITS header block.
 *      jindex index of the required card.
 *
 *   Return:
 *      The header card as a String or "".
 *
 */
JNIEXPORT jstring JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nGetFitsHeader
    (JNIEnv *env, jclass class, jlong jfits, jint jindex )
{
    /* Local variables */
    FitsHeader *fits;
    int index;
    char value[81];
    char *ptr;

    /*  Import the FITS header structure */
    fits = *(FitsHeader **) &jfits;
    index = (int) jindex;

    /*  Obtain a copy of the requested card */
    if ( index < fits->ncard ) {
        ptr = fits->ptr + ( index * 80 );
        strncpy( value, ptr, 80 );
        value[80] = '\0';
    }
    else {
        value[0] = '\0';
    }

    /*  Return the value */
    return (*env)->NewStringUTF( env, value );
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nReleaseFitsHeaders
 *
 *  Purpose:
 *     Release all resources used to access FITS headers.
 *
 *  Params:
 *     jfits reference to FITS headers.
 *
 */
JNIEXPORT void JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nReleaseFitsHeaders
    (JNIEnv *env, jclass class, jlong jfits )
{
    /* Local variables */
    FitsHeader *fits;

    /*  Import the FITS header structure */
    fits = *(FitsHeader **) &jfits;

    if ( fits != NULL ) {
        if ( fits->ptr != NULL ) {
            free( fits->ptr );
            fits->ptr = NULL;
        }
        free( fits );
        fits = NULL;
    }
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nCreateFitsExtension
 *
 *  Purpose:
 *     Create a new FITS extension adding a set of cards.
 *
 *  Params:
 *     jindf = NDF identifier
 *     jcards = array of card Strings
 *
 */
JNIEXPORT
    void JNICALL Java_uk_ac_starlink_splat_imagedata_NDFJ_nCreateFitsExtension
    (JNIEnv *env, jclass class, jint jindf, jobjectArray jcards )
{
    /*  Local variables */
    char *card;              /* Current card */
    char *ptr;               /* Pointer to mapped data */
    HDSLoc *loc = NULL;      /* Locator to extension */
    int dim[1];              /* Dimensions of FITS block */
    int i;                   /* Loop variable */
    int indf;                /* NDF identifier */
    size_t ncards;           /* Number of cards to write */
    int status;              /* NDF library status */
    jstring jcard;           /* Current card */

    /*  Import the NDF identifier */
    indf = (int) jindf;

    /*  Establish local status and stop NDF from issuing errors */
    status = SAI__OK;
    errMark();

    /*  Find out the number of cards */
    ncards = (size_t) (*env)->GetArrayLength( env, jcards );

    /*  Create the new extension */
    ndfXdel( indf, "FITS", &status );
    dim[0] = (int) ncards;
    ndfXnew( indf, "FITS", "_CHAR*80", 1, dim, &loc, &status );
    datMapV( loc, "_CHAR*80", "WRITE", (void **) &ptr, &ncards, &status );

    /*  Access each String in turn adding it to the extension */
    for ( i = 0; i < ncards; i++, ptr += 80 ) {
        jcard = (*env)->GetObjectArrayElement( env, jcards, (jint) i );
        card = (char *) (*env)->GetStringUTFChars( env, jcard, NULL );
        strcpy( ptr, card );
        (*env)->ReleaseStringUTFChars( env, jcard, card );
    }

    /*  Release the extension, writing it to NDF */
    datAnnul( &loc, &status );

    /*  Clear NDF status, if needed and release the error stack */
    if ( status != SAI__OK ) {
        errFlush( &status );
        errAnnul( &status );
    }
    errRlse();
}

/*
 *  Generate generic functions for accessing array 1D and 2D data
 *  components using various data types.
 */

#define JAVATYPE Double
#define JAVASIG "[D"
#define NDFTYPE "_DOUBLE"
#define CTYPE jdouble
#include "NDFJImplGet.h"
#undef JAVATYPE
#undef JAVASIG
#undef NDFTYPE
#undef CTYPE

#define JAVATYPE Float
#define JAVASIG "[F"
#define NDFTYPE "_REAL"
#define CTYPE jfloat
#include "NDFJImplGet.h"
#undef JAVATYPE
#undef JAVASIG
#undef NDFTYPE
#undef CTYPE

#define JAVATYPE Int
#define JAVASIG "[I"
#define NDFTYPE "_INTEGER"
#define CTYPE jint
#include "NDFJImplGet.h"
#undef JAVATYPE
#undef JAVASIG
#undef NDFTYPE
#undef CTYPE

#define JAVATYPE Short
#define JAVASIG "[S"
#define NDFTYPE "_WORD"
#define CTYPE jshort
#include "NDFJImplGet.h"
#undef JAVATYPE
#undef JAVASIG
#undef NDFTYPE
#undef CTYPE

#define JAVATYPE Byte
#define JAVASIG "[B"
#define NDFTYPE "_BYTE"
#define CTYPE jbyte
#include "NDFJImplGet.h"

 /*
 *  ======================
 *  Java Utility Functions
 *  ======================
 */

/*
 *  Name:
 *     readFITSWCS
 *
 *  Purpose:
 *     Use NDF FITS headers to attempt to construct a AST FrameSet
 *     that describes the WCS system of the NDF.
 *
 *  Params:
 *     indf = NDF identifier
 *     iwcs = pointer to pointer to the output AstFrameSet (NULL if fails)
 *     status = global status
 *
 *  Return:
 *     1 if valid FrameSet created, 0 otherwise.
 *
 */
static int readFITSWCS( int indf, AstFrameSet **iwcs, int *status )
{
   /*  Local variables */
   HDSLoc *loc = NULL;
   char *pntr;
   char card[81];
   size_t ncard;
   int i;
   AstFitsChan *fitschan;

   /*  Get a locator to the FITS block */
   ndfXloc( indf, "FITS", "READ", &loc, status );

   /*  Access the FITS headers */
   datMapV( loc, "_CHAR*80", "READ", (void **) &pntr, &ncard, status );
   if ( *status != SAI__OK ) {
      datAnnul( &loc, status );
      *iwcs = NULL;
      return 0;
   }
   else {
      /* Read the FITS headers through an AstFitsChan */
      fitschan = astFitsChan( NULL, NULL, "" );
      for ( i = 0 ; i < ncard; i++, pntr += 80 ) {
         memcpy( card, (void *)pntr, (size_t) 80 );
         card[80] = '\0';

         /*  Read all cards up to, but not including, the END card. */
         if ( ! ( card[0] == 'E' && card[1] == 'N' && card[2] == 'D'
                 && ( card[3] == '\0' || card[3] == ' ' ) ) ) {
            astPutFits( fitschan, card, 0 );
            if ( !astOK ) {

               /*  If an error occurs with a card, just continue, it's
                *  almost certainly something trivial like a formatting
                *  probelm.
                */
               astClearStatus;
            }
         }
         else {
            break;
         }
      }

      /*  Rewind channel */
      astClear( fitschan, "Card" );

      /*  Now try to read in the FITS headers to create a FrameSet */
      *iwcs = astRead( fitschan );
      fitschan = (AstFitsChan *) astAnnul( fitschan );

      datAnnul( &loc, status );
      if ( *iwcs == AST__NULL ) {
         astClearStatus;
         return 0;
      }
      return 1;
   }
}

/*
 *  Name:
 *     joinWCS
 *
 *  Purpose:
 *     Joins two AstFrameSets via their base frames (which are assumed
 *     to be the same coordinate system).
 *
 *  Params:
 *     wcsone = first FrameSet, on exit this FrameSet is modified to
 *              include the frames from the second FrameSet.
 *     wcstwo = FrameSet to join with wcsone.
 *
 *  Notes:
 *     The FrameSets are merged by joining the two base frames using a
 *     Unitmap. Based on AddWcs byh David Berry.
 *
 *  Return:
 *     1 if merge succeeded, 0 otherwise.
 */
static int joinWCS( AstFrameSet *wcsone, AstFrameSet *wcstwo )
{
    /*  Local variables */
    AstUnitMap *unit;
    int nframe;
    int naxes;
    int icurr;

    /*  Note the number of frames in the first FrameSet */
    nframe = astGetI( wcsone, "nframe" );

    /*  Get the number of axis in the base frames. */
    naxes = astGetI( wcsone, "nin" );

    /*  Create a UnitMap to join the base frames */
    unit = astUnitMap( naxes, "" );

    /*  Add the second FrameSet into the first using the UnitMap to join
     *  them. Messing about as astAddFrame makes assumption about
     *  current Frame.
     */
    icurr = astGetI( wcstwo, "current" );
    astSetI( wcstwo, "current", astGetI( wcstwo, "base" ) );
    astAddFrame( wcsone, AST__BASE, unit, wcstwo );
    unit = (AstUnitMap *) astAnnul( unit );
    astSetI( wcstwo, "current", icurr );

    /*  Remove the redundant "base" frame */
    astRemoveFrame( wcsone, nframe + astGetI( wcstwo, "base" ) );

    /*  If an error occurred, just clear it */
    if ( !astOK ) {
        astClearStatus;
        return 0;
    }
    else {
        return 1;
    }
}
