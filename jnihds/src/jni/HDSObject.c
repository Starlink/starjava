/*
*+
*  Name:
*     HDSObject

*  Purpose:
*     JNI interface to HDS library.

*  Description:
*     This file provides the C routines required for the JNI interface
*     to the Starlink HDS library.  The intention is to provide a set
*     of routines which map one-to-one to the routines in the HDS
*     library.  These C functions form the native side of static and
*     instance methods declared in the HDSObject class.

*  Implementation Status:
*     Routines have been added as required.  This is by no means a 
*     complete interface to HDS.

*  Authors:
*     MBT: Mark Taylor (STARLINK)

*  History:
*     1-AUG-2001 (MBT):
*        Initial version.

*-
*/

/* Constants. */
#define MAXFILENAME 512
#define MAXPATHLENG 512
#define MAXCHARLENG 1024
#define JNIHDS_BUFLENG 1024
#define PACKAGE_PATH "uk/ac/starlink/hds/"
#define CLASS_NAME uk_ac_starlink_hds_HDSObject
#define NATIVE_METHOD(name) Java_uk_ac_starlink_hds_HDSObject_##name


/* Header files. */
#include <string.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include "jni.h"
#include "sae_par.h"
#include "mers.h"
#include "dat_par.h"
#include "cnf.h"
#include "uk_ac_starlink_hds_HDSObject.h"


/* Macros. */

#define INT_BIG F77_INTEGER_TYPE
#define RETURN_IF_EXCEPTION \
   if ( (*env)->ExceptionCheck( env ) ) return NULL
#ifdef DEBUG
#define HDSCALL_REPORTLINE \
   sprintf( errmsg, "HDS error at line %d in file %s:\n", __LINE__, __FILE__ )
#else
#define HDSCALL_REPORTLINE 0
#endif

/* Macro for calling a block of code which calls HDS library routines.
 * This serves two purposes: first it synchronizes on an Object kept
 * for that purpose - this ensures that an HDS routine is not called
 * while another HDS routine is being called (i.e. from different threads).
 * Secondly it checks for an error condition indicated by *status != SAI__OK
 * and turns it into an Exception using the relevant message text.
 * No action will be attempted if an exception is pending when this
 * macro is invoked.
 */
#define HDSCALL( code ) \
   if ( ! (*env)->ExceptionCheck( env ) ) { \
      jthrowable throwable = NULL; \
      F77_INTEGER_TYPE status_val = SAI__OK; \
      F77_INTEGER_TYPE *status = &status_val; \
      if ( (*env)->MonitorEnter( env, HDSLock ) == 0 ) { \
         errMark(); \
         code \
         if ( *status != SAI__OK ) { \
            char errmsg[ JNIHDS_BUFLENG + 1 ]; \
            char errname[ ERR__SZPAR ]; \
            int errmsg_leng; \
            int errname_leng; \
            char *msgpos = errmsg; \
            msgpos += HDSCALL_REPORTLINE; \
            while ( *status != SAI__OK ) { \
               errLoad( errname, ERR__SZPAR, &errname_leng, msgpos, \
                        errmsg + JNIHDS_BUFLENG - msgpos, &errmsg_leng, \
                        status ); \
               msgpos += errmsg_leng; \
               if ( errmsg + JNIHDS_BUFLENG - msgpos <= 1 ) { \
                  errFlush( status ); \
               } \
               if ( *status != SAI__OK ) { \
                  *(msgpos++) = '\n'; \
               } \
            } \
            throwable = \
               (*env)->NewObject( env, HDSExceptionClass, \
                                  HDSExceptionConstructorID, \
                                  (*env)->NewStringUTF( env, errmsg ) ); \
         } \
         errRlse(); \
         if ( (*env)->MonitorExit( env, HDSLock ) != 0 ) { \
            throwable = monitorExitFailure( env ); \
         } \
      } \
      else { \
         throwable = monitorEntryFailure( env ); \
      } \
      if ( throwable != NULL ) { \
         (*env)->Throw( env, throwable ); \
      } \
   }



/* Static function prototypes. */
static void throwNativeError( JNIEnv *env, char *fmt, ... );
static jobject makeHDSObject( JNIEnv *env, char *locdata );
static jlongArray makeCartesian( JNIEnv *env, INT_BIG *dims, INT_BIG ndim );
static jstring makeString( JNIEnv *env, const char *bytes, int leng );
static jthrowable monitorEntryFailure( JNIEnv *env );
static jthrowable monitorExitFailure( JNIEnv *env );
static void setLocator( JNIEnv *env, jobject object, char *locator );
static const char *getLocator( JNIEnv *env, jobject object, char *locator );
static INT_BIG *getCoords( JNIEnv *env, jlongArray jCoordArray, INT_BIG *coords, INT_BIG *ndim );
static INT_BIG getSize( JNIEnv *env, char *locator );
static int getLength( JNIEnv *env, char *locator );
static void *jMalloc( JNIEnv *env, size_t size );


/* Static variables. */
static jclass HDSObjectClass = 0;
static jclass BooleanClass = 0;
static jclass IntegerClass = 0;
static jclass FloatClass = 0;
static jclass DoubleClass = 0;
static jclass StringClass = 0;
static jclass ObjectClass = 0;
static jclass SystemClass = 0;
static jthrowable ErrorClass = 0;
static jthrowable OutOfMemoryErrorClass = 0;
static jthrowable IllegalArgumentExceptionClass = 0;
static jthrowable HDSExceptionClass = 0;
static jfieldID HDSObjectLocatorID = 0;
static jmethodID HDSObjectConstructorID = 0;
static jmethodID HDSExceptionConstructorID = 0;
static jmethodID BooleanConstructorID = 0;
static jmethodID IntegerConstructorID = 0;
static jmethodID FloatConstructorID = 0;
static jmethodID DoubleConstructorID = 0;
static jmethodID ErrorConstructorID = 0;
static jmethodID SystemGcMethodID = 0;
static jobject HDSLock = 0;


/* Static functions. */
static void throwNativeError( 
   JNIEnv *env,          /* Interface pointer */
   char *fmt,            /* Format string */
   ...                   /* Optional arguments a la printf */ 
) {
   char *buffer;
   int bufleng = 1024;
   int nchar;
   va_list ap;

   /* Only try to throw a new exception if we do not have one pending. */
   if ( (*env)->ExceptionOccurred( env ) == NULL ) {

      /* Allocate a buffer and write the message into it.  In the unlikely
       * event that we have run out of memory, make do with printing the
       * format string alone. */
      buffer = malloc( bufleng + 1 );
      if ( buffer != NULL ) {
         va_start( ap, fmt );
         nchar = vsnprintf( buffer, bufleng, fmt, ap );
         va_end( ap );
         if ( nchar >= bufleng ) {
            sprintf( buffer + bufleng - 4, "..." );
         }
      }
      else {
         buffer = fmt;
      }

      /* Throw the exception. */
      (*env)->ThrowNew( env, ErrorClass, buffer );

      /* Release the buffer. */
      if ( buffer != fmt ) free( buffer );
   }
}


static jobject makeHDSObject( 
   JNIEnv *env,          /* Interface pointer */
   char *locator         /* Locator string */
) {
   jobject newobj;

   if ( (*env)->ExceptionOccurred( env ) == NULL ) {

      /* Construct an empty HDSObject. */
      newobj = (*env)->NewObject( env, HDSObjectClass, HDSObjectConstructorID );

      /* Write the locator bytes into the locator field of the object. */
      if ( newobj != NULL ) {
         setLocator( env, newobj, locator );
      }
   }
   else {
      newobj = NULL;
   }

   /* Return the completed object. */
   return newobj;
}


static jlongArray makeCartesian(
   JNIEnv *env,          /* Interface pointer */
   INT_BIG *coords,      /* Coordinates of the object */
   INT_BIG ndim          /* Dimensionality of the object */
) {
   int i;
   jobject newobj;
   jlong jCoords[ DAT__MXDIM ];
   jlongArray jCoordArray;

   if ( (*env)->ExceptionOccurred( env ) == NULL ) {

      /* Construct an empty array object to hold the coordinates. */
      jCoordArray = (*env)->NewLongArray( env, ndim );

      /* Fill the array object with the coordinates we have. */
      if ( jCoordArray != NULL ) {
         for ( i = 0; i < ndim; i++ ) {
            jCoords[ i ] = (jlong) coords[ i ];
         }
         (*env)->SetLongArrayRegion( env, jCoordArray, 0, ndim, jCoords );
      }
   }
   else {
      jCoordArray = NULL;
   }

   /* Return the completed object. */
   return jCoordArray;
}


static jstring makeString( 

/* This routine generates a Java String object from a fixed length 
 * buffer holding a fortran-like string - the buffer is not zero-terminated,
 * and trailing spaces are stripped. */

   JNIEnv *env,          /* Interface pointer */
   const char *bytes,    /* Start of buffer holding the string */ 
   int leng              /* Length of buffer */
) {
   char *cbuf;
   jstring result = NULL;

   if ( (*env)->ExceptionOccurred( env ) == NULL ) {

      /* Convert the Fortran string to C. */
      cbuf = jMalloc( env, leng + 1 );
      if ( cbuf != NULL ) {
         cnfImprt( bytes, leng, cbuf );

         /* Convert the C string into a java String. */
         result = (*env)->NewStringUTF( env, cbuf );
      }
   }

   return result;
}
   

static INT_BIG *getCoords(
   JNIEnv *env,          /* Interface pointer */
   jlongArray jCoordArray,/* Array containing coords */
   INT_BIG *coords,      /* Buffer to receive coords */
   INT_BIG *ndim         /* Dimensionality of cartesian array */
) {
   int i;
   jlong *jCoords;

   if ( (*env)->ExceptionOccurred( env ) == NULL ) {

      /* Get the dimensionality. */
      *ndim = (*env)->GetArrayLength( env, jCoordArray );

      /* Copy the elements from the array into a local array. */
      jCoords = (*env)->GetLongArrayElements( env, jCoordArray, NULL );
      for ( i = 0; i < *ndim; i++ ) {
         coords[ i ] = (INT_BIG) jCoords[ i ];
      }
      (*env)->ReleaseLongArrayElements( env, jCoordArray, jCoords, JNI_ABORT );
   }
   else {
      *ndim = 0;
   }

   /* Return a pointer to the coordinate array for convenience. */
   return coords;
}

static INT_BIG getSize( 
   JNIEnv *env,          /* Interface pointer */
   char *locator         /* Locator string of HDS object */
) {
   INT_BIG size;
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );

   /* Get this object's locator. */
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call DAT_SIZE to get the size. */
   HDSCALL(
      F77_CALL(dat_size)( CHARACTER_ARG(fLocator), INTEGER_ARG(&size),
                          INTEGER_ARG(status)
                          TRAIL_ARG(fLocator) );
   )

   /* Return the size. */
   return size;
}

static int getLength(
   JNIEnv *env,          /* Interface pointer */
   char *locator         /* Locator string of HDS object */
) {
   int length;
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );

   /* Get this object's locator. */
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call DAT_LEN to get the length. */
   HDSCALL(
      F77_CALL(dat_len)( CHARACTER_ARG(fLocator), INTEGER_ARG(&length),
                         INTEGER_ARG(status)
                         TRAIL_ARG(fLocator) );
   )

   /* Return the length. */
   return length;
}


static void setLocator( 
   JNIEnv *env,          /* Interface pointer */
   jobject object,       /* Object in which to store the locator */
   char *locator         /* Bytes which define the locator */
) {
   jbyteArray locatorField;

   if ( (*env)->ExceptionOccurred( env ) == NULL ) {

      /* Allocate the space for a locator field. */
      locatorField = (*env)->NewByteArray( env, DAT__SZLOC );
      if ( locatorField != NULL ) {

         /* Fill the locator with the correct bytes. */
      
         (*env)->SetByteArrayRegion( env, locatorField, 0, DAT__SZLOC, 
                                     (jbyte *) locator );

         /* Assign the populated locator to the locator field of the object. */
         (*env)->SetObjectField( env, object, HDSObjectLocatorID, 
                                 locatorField );
       }
   }
}


static const char *getLocator(
   JNIEnv *env,          /* Interface pointer */
   jobject object,       /* Object whose locator we want */
   char *locator         /* Buffer in which to return locator */
) {
   jbyteArray locatorField;

   if ( (*env)->ExceptionOccurred( env ) == NULL ) {

      /* Get the locator field as a byte array object. */
      locatorField = (*env)->GetObjectField( env, object, HDSObjectLocatorID );

      /* Copy the contents of the field to the return value. */
      (*env)->GetByteArrayRegion( env, locatorField, 0, DAT__SZLOC, 
                                  (jbyte *) locator );
   }
   return locator;
}


/*
 * Allocates memory a la malloc.  If the allocation is unsuccessful, 
 * it will throw a java.lang.OutOfMemoryError, so that exception checking
 * rather than return value checking may be used.
 */
static void *jMalloc(
   JNIEnv *env,          /* Interface pointer */
   size_t size
) {
   void *ptr = NULL;

   /* Only proceed if there is no pending exception. */
   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Attempt the requested allocation and check if it worked. */
      ptr = malloc( size );
      if ( ptr == NULL ) {

         /* The allocation failed.  Call the garbage collector and try again. */
         (*env)->CallStaticVoidMethod( env, SystemClass, SystemGcMethodID );
         ptr = malloc( size );
         if ( ptr == NULL ) {

            /* We still cannot allocate the requested memory.  Throw an
             * OutOfMemoryError. */
            (*env)->ThrowNew( env, OutOfMemoryErrorClass,
                              "Out of memory during native JNI/HDS code" );
         }
      }
   }

   /* Return the successfully or unsuccessfully allocated address. */
   return ptr;
}


static jthrowable monitorEntryFailure(
   JNIEnv *env           /* Interface pointer */
) {
   return (jthrowable) 
          (*env)->NewObject( env, ErrorClass, ErrorConstructorID, 
                             (*env)->NewStringUTF( env, "jnihds: unexpected" 
                                                   "MonitorEnter return" ) );
}


static jthrowable monitorExitFailure(
   JNIEnv *env           /* Interface pointer */
) {
   return (jthrowable)
          (*env)->NewObject( env, ErrorClass, ErrorConstructorID, 
                             (*env)->NewStringUTF( env, "jnihds: unexpected" 
                                                   "MonitorExit return" ) );
}



/* Class methods. */
JNIEXPORT void JNICALL NATIVE_METHOD( nativeInitialize )(
   JNIEnv *env,          /* Interface pointer */
   jclass class          /* Class object */
) {
   /* Get static references to classes.
    * Note as well as retaining the value of the class since we will need
    * it, taking a global reference to the class here ensures that the
    * class will not be unloaded.  This is necessary so that the Field ID
    * values do not become out of date. */
   ( HDSObjectClass = (jclass) (*env)->NewGlobalRef( env, 
      class ) ) &&
   ( HDSExceptionClass = (jclass) (*env)->NewGlobalRef( env, 
      (*env)->FindClass( env, PACKAGE_PATH "HDSException" ) ) ) &&
   ( IllegalArgumentExceptionClass = (jclass) (*env)->NewGlobalRef( env,
      (*env)->FindClass( env, "java/lang/IllegalArgumentException" ) ) ) &&
   ( ErrorClass = (jclass) (*env)->NewGlobalRef( env,
      (*env)->FindClass( env, "java/lang/Error" ) ) ) &&
   ( OutOfMemoryErrorClass = (jclass) (*env)->NewGlobalRef( env,
      (*env)->FindClass( env, "java/lang/OutOfMemoryError" ) ) ) &&
   ( BooleanClass = (jclass) (*env)->NewGlobalRef( env,
      (*env)->FindClass( env, "java/lang/Boolean" ) ) ) &&
   ( IntegerClass = (jclass) (*env)->NewGlobalRef( env,
      (*env)->FindClass( env, "java/lang/Integer" ) ) ) &&
   ( FloatClass = (jclass) (*env)->NewGlobalRef( env,
      (*env)->FindClass( env, "java/lang/Float" ) ) ) &&
   ( DoubleClass = (jclass) (*env)->NewGlobalRef( env,
      (*env)->FindClass( env, "java/lang/Double" ) ) ) &&
   ( StringClass = (jclass) (*env)->NewGlobalRef( env,
      (*env)->FindClass( env, "java/lang/String" ) ) ) &&
   ( ObjectClass = (jclass) (*env)->NewGlobalRef( env,
      (*env)->FindClass( env, "java/lang/Object" ) ) ) &&
   ( SystemClass = (jclass) (*env)->NewGlobalRef( env,
      (*env)->FindClass( env, "java/lang/System" ) ) ) &&

   /* Get field IDs. */
   ( HDSObjectLocatorID = 
      (*env)->GetFieldID( env, HDSObjectClass, "locator", "[B" ) ) &&

   /* Get method IDs. */
   ( HDSObjectConstructorID = 
      (*env)->GetMethodID( env, HDSObjectClass, "<init>", "()V" ) ) &&
   ( HDSExceptionConstructorID =
      (*env)->GetMethodID( env, HDSExceptionClass, "<init>", 
                           "(Ljava/lang/String;)V" ) ) &&
   ( BooleanConstructorID = 
      (*env)->GetMethodID( env, BooleanClass, "<init>", "(Z)V" ) ) &&
   ( IntegerConstructorID =
      (*env)->GetMethodID( env, IntegerClass, "<init>", "(I)V" ) ) &&
   ( FloatConstructorID =
      (*env)->GetMethodID( env, FloatClass, "<init>", "(F)V" ) ) &&
   ( DoubleConstructorID =
      (*env)->GetMethodID( env, DoubleClass, "<init>", "(D)V" ) ) &&
   ( ErrorConstructorID =
      (*env)->GetMethodID( env, ErrorClass, "<init>", 
                           "(Ljava/lang/String;)V" ) ) &&
   ( SystemGcMethodID = 
      (*env)->GetStaticMethodID( env, SystemClass, "gc", "()V" ) ) &&

   /* Construct the object used for synchronizing calls to the HDS library. */
   ( HDSLock = (jobject) (*env)->NewGlobalRef( env,
        (*env)->NewObject( env, ObjectClass, 
                           (*env)->GetMethodID( env, ObjectClass, 
                           "<init>", "()V" ) ) ) ) &&

   1;
}


JNIEXPORT jint JNICALL NATIVE_METHOD( getHDSConstantI )(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jstring jName         /* Name of the constant to retrieve */ 
) {
   jint value = -1;
   const char *name;

   /* Decode java string. */
   name = (*env)->GetStringUTFChars( env, jName, NULL );

   /* Try to identify what the constant is. */
   if ( ! strcmp( name, "DAT__MXDIM" ) ) {
      value = DAT__MXDIM;
   }
   else if ( ! strcmp( name, "DAT__SZLOC" ) ) {
      value = DAT__SZLOC;
   }
   else if ( ! strcmp( name, "DAT__SZMOD" ) ) {
      value = DAT__SZMOD;
   }
   else if ( ! strcmp( name, "DAT__SZNAM" ) ) {
      value = DAT__SZNAM;
   }
   else if ( ! strcmp( name, "DAT__SZTYP" ) ) {
      value = DAT__SZTYP;
   }
   else {
      throwNativeError( env, "Unknown HDS constant %s", name );
   }

   /* Release temporary memory. */
   (*env)->ReleaseStringUTFChars( env, jName, name ); 

   /* Return value */
   return value;
}

JNIEXPORT jobject JNICALL NATIVE_METHOD( getHDSConstantLoc )(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jstring jName         /* Name of the locator to retrieve */
) {
   int ok;
   const char *name;
   jobject newobj;

   /* Decode java string. */
   name = (*env)->GetStringUTFChars( env, jName, NULL );

   /* Try to identify what the constant is. */
   if ( ! strcmp( name, "DAT__NOLOC" ) ) {
      ok = 1;
      newobj = makeHDSObject( env, DAT__NOLOC );
   }
   else if ( ! strcmp( name, "DAT__ROOT" ) ) {
      ok = 1;
      newobj = makeHDSObject( env, DAT__ROOT );
   }
   else {
      ok = 0;
      newobj = NULL;
   }

   /* Release the name. */
   (*env)->ReleaseStringUTFChars( env, jName, name );

   if ( ! ok ) {
      /* No known constant.  Raise an exception. */
      throwNativeError( env, "Unknown HDS constant locator %s", name );
      newobj = NULL;
   }
   
   /* Return the newly constructed HDSObject. */
   return newobj;
}


JNIEXPORT jobject JNICALL NATIVE_METHOD( hdsNew )(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jstring jContainer,   /* Name of container file */
   jstring jName,        /* Component name */
   jstring jType,        /* Component type */
   jlongArray jDims      /* Component dimensions */
) {
   const char *container;
   const char *name;
   const char *type;
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fContainer, MAXFILENAME );
   DECLARE_CHARACTER( fName, DAT__SZNAM );
   DECLARE_CHARACTER( fType, DAT__SZTYP );
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   INT_BIG dims[ DAT__MXDIM ];
   INT_BIG ndim;

   /* Convert java strings to C. */
   container = (*env)->GetStringUTFChars( env, jContainer, NULL );
   name = (*env)->GetStringUTFChars( env, jName, NULL );
   type = (*env)->GetStringUTFChars( env, jType, NULL );

   /* Convert C strings to fortran. */
   cnfExprt( container, fContainer, fContainer_length );
   cnfExprt( name, fName, fName_length );
   cnfExprt( type, fType, fType_length );

   /* Release string copies. */
   (*env)->ReleaseStringUTFChars( env, jContainer, container );
   (*env)->ReleaseStringUTFChars( env, jName, name );
   (*env)->ReleaseStringUTFChars( env, jType, type );

   /* Convert java array into fortran. */
   getCoords( env, jDims, dims, &ndim );

   /* Call the fortran routine to do the work. */
   HDSCALL(
      F77_CALL(hds_new)( CHARACTER_ARG(fContainer), CHARACTER_ARG(fName),
                         CHARACTER_ARG(fType), INTEGER_ARG(&ndim),
                         INTEGER_ARRAY_ARG(dims), CHARACTER_ARG(fLocator),
                         INTEGER_ARG(status)
                         TRAIL_ARG(fContainer) TRAIL_ARG(fName) 
                         TRAIL_ARG(fType) TRAIL_ARG(fLocator) );
   )

   if ( ! (*env)->ExceptionCheck( env ) ) {
      /* Turn the locator into a C string. */
      cnfImpch( fLocator, DAT__SZLOC, locator );
   }

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, locator );
}



JNIEXPORT jobject JNICALL NATIVE_METHOD( hdsOpen )(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jstring jContainer,   /* Name of container file */
   jstring jAccess       /* Access mode */
) {
   const char *access;
   const char *container;
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fAccess, 6 );
   DECLARE_CHARACTER( fContainer, MAXFILENAME );
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );

   /* Convert java strings to C. */
   access = (*env)->GetStringUTFChars( env, jAccess, NULL );
   container = (*env)->GetStringUTFChars( env, jContainer, NULL );

   /* Convert C strings to Fortran. */
   cnfExprt( access, fAccess, fAccess_length );
   cnfExprt( container, fContainer, fContainer_length );
   
   /* Release string copies. */
   (*env)->ReleaseStringUTFChars( env, jAccess, access );
   (*env)->ReleaseStringUTFChars( env, jContainer, container );

   /* Call HDS_OPEN to do the work. */
   HDSCALL(
      F77_CALL(hds_open)( CHARACTER_ARG(fContainer), CHARACTER_ARG(fAccess),
                          CHARACTER_ARG(fLocator), INTEGER_ARG(status)
                          TRAIL_ARG(fContainer) TRAIL_ARG(fAccess) 
                          TRAIL_ARG(fLocator) );
   )

   if ( ! (*env)->ExceptionCheck( env ) ) {
      /* Turn the locator into a C string. */
      cnfImpch( fLocator, DAT__SZLOC, locator );
   }

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, locator );
}


JNIEXPORT jint JNICALL NATIVE_METHOD( hdsTrace )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject results       /* Array of strings for output */
) {
   char locator[ DAT__SZLOC ];
   jstring filestr;
   jstring pathstr;
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER( fPath, MAXPATHLENG );
   DECLARE_CHARACTER( fFile, MAXFILENAME );
   DECLARE_INTEGER( nlev );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the fortran routine to do the work. */
   HDSCALL(
      F77_CALL(hds_trace)( CHARACTER_ARG(fLocator), INTEGER_ARG(&nlev),
                           CHARACTER_ARG(fPath), CHARACTER_ARG(fFile),
                           INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) TRAIL_ARG(fPath) 
                           TRAIL_ARG(fFile) );
   )

   /* Turn file and path strings into java String objects. */
   filestr = makeString( env, fFile, fFile_length );
   pathstr = makeString( env, fPath, fPath_length );

   /* Write the strings into the supplied String[] array. */
   if ( ! (*env)->ExceptionOccurred( env ) ) {
      (*env)->SetObjectArrayElement( env, results, (jsize) 0, pathstr );
   }
   if ( ! (*env)->ExceptionOccurred( env ) ) {
      (*env)->SetObjectArrayElement( env, results, (jsize) 1, filestr );
   }

   /* Return the result. */
   return (jint) nlev;
}


JNIEXPORT void JNICALL NATIVE_METHOD( datAnnul )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_annul)( CHARACTER_ARG(fLocator), INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) );
   )

   /* Copy the locator (which should now contain the value DAT__NOLOC)
      back to the object. */
   cnfImpch( fLocator, DAT__SZLOC, locator );
   setLocator( env, this, locator );
}


JNIEXPORT jobject JNICALL NATIVE_METHOD( datCell )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jlongArray position   /* Array indicating which cell to get */
) {
   char locator[ DAT__SZLOC ];
   char newLocator[ DAT__SZLOC ];
   INT_BIG coords[ DAT__MXDIM ];
   INT_BIG ndim = 0;
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER( fNewLocator, DAT__SZLOC );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Get an array representing the position of the cell to retrieve. */
   getCoords( env, position, coords, &ndim );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_cell)( CHARACTER_ARG(fLocator), INTEGER_ARG(&ndim),
                          INTEGER_ARRAY_ARG(coords), 
                          CHARACTER_ARG(fNewLocator), INTEGER_ARG(status)
                          TRAIL_ARG(fLocator) TRAIL_ARG(fNewLocator) );
   )

   /* Turn the returned locator into a C string. */
   cnfImpch( fNewLocator, DAT__SZLOC, newLocator );
   
   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, newLocator );
}


JNIEXPORT jobject JNICALL NATIVE_METHOD( datClone )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   char newLocator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER( fNewLocator, DAT__SZLOC );
   jobject newobj;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_clone)( CHARACTER_ARG(fLocator), CHARACTER_ARG(fNewLocator),
                           INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) TRAIL_ARG(fNewLocator) );
   )

   /* Turn the returned locator into a C string. */
   cnfImpch( fNewLocator, DAT__SZLOC, newLocator );

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, newLocator );
}


JNIEXPORT void JNICALL NATIVE_METHOD( datErase )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jName         /* Name of contained component */
) {
   char locator[ DAT__SZLOC ];
   const char *name;
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER( fName, DAT__SZNAM );

   /* Convert java string to C. */
   name = (*env)->GetStringUTFChars( env, jName, NULL );

   /* Convert C string to Fortran. */
   cnfExprt( name, fName, fName_length );

   /* Release string copy. */
   (*env)->ReleaseStringUTFChars( env, jName, name );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_erase)( CHARACTER_ARG(fLocator), CHARACTER_ARG(fName),
                           INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) TRAIL_ARG(fName) );
   )
}


JNIEXPORT jobject JNICALL NATIVE_METHOD( datFind )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jName         /* Name of contained component */
) {
   const char *name;
   char locator[ DAT__SZLOC ];
   char newLocator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fName, DAT__SZNAM );
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER( fNewLocator, DAT__SZLOC );

   /* Convert java string to C. */
   name = (*env)->GetStringUTFChars( env, jName, NULL );

   /* Convert C string to Fortran. */
   cnfExprt( name, fName, fName_length );

   /* Release string copy. */
   (*env)->ReleaseStringUTFChars( env, jName, name );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_find)( CHARACTER_ARG(fLocator), CHARACTER_ARG(fName), 
                          CHARACTER_ARG(fNewLocator), INTEGER_ARG(status)
                          TRAIL_ARG(fLocator) TRAIL_ARG(fName)
                          TRAIL_ARG(fNewLocator) );
   )

   /* Turn the returned locator into a C string. */
   cnfImpch( fNewLocator, DAT__SZLOC, newLocator );

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, newLocator );
}


JNIEXPORT jstring JNICALL NATIVE_METHOD( datGet0c )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char value[ MAXCHARLENG + 1 ];
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER( fValue, MAXCHARLENG );
   
   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_get0c)( CHARACTER_ARG(fLocator), CHARACTER_ARG(fValue),
                           INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) TRAIL_ARG(fValue) );
   )

   /* Convert the fortran string back to C */
   cnfImprt( fValue, fValue_length, value );

   /* Construct and return a java string object. */
   return (*env)->NewStringUTF( env, value );
}

#define MAKE_DATGET0X(Xletter,Xjtype,Xcnftype) \
JNIEXPORT Xjtype JNICALL NATIVE_METHOD( datGet0##Xletter )( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this          /* Instance object */ \
) { \
   char locator[ DAT__SZLOC ]; \
   DECLARE_CHARACTER( fLocator, DAT__SZLOC ); \
   DECLARE_##Xcnftype( value ); \
   \
   /* Get this object's locator. */ \
   getLocator( env, this, locator ); \
   cnfExpch( locator, fLocator, fLocator_length ); \
   \
   /* Call the Fortran routine to do the work. */ \
   HDSCALL( \
      F77_CALL(dat_get0##Xletter)( CHARACTER_ARG(fLocator),  \
                                   Xcnftype##_ARG(&value),  \
                                   INTEGER_ARG(status) \
                                   TRAIL_ARG(fLocator) ); \
   ) \
   \
   /* Cast and return the retrieved value. */ \
   return (Xjtype) value; \
}
MAKE_DATGET0X(l,jboolean,LOGICAL)
MAKE_DATGET0X(i,jint,INTEGER)
MAKE_DATGET0X(r,jfloat,REAL)
MAKE_DATGET0X(d,jdouble,DOUBLE)
#undef MAKE_DATGET0X


#define DATGETC_CHECK_EXCEPTION \
   if ( (*env)->ExceptionOccurred( env ) != NULL ) { \
      free( buffer ); \
      return NULL; \
   }
JNIEXPORT jobject JNICALL NATIVE_METHOD( datGetc )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jlongArray shape      /* Array giving shape to retrieve */
) {
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER_ARRAY_DYN( buffer );
   INT_BIG dims[ DAT__MXDIM ];
   INT_BIG ndim;
   INT_BIG pos[ DAT__MXDIM ];
   int done;
   int i;
   int nel;
   int sleng;
   jarray aptrs[ DAT__MXDIM ];
   jclass aclass[ DAT__MXDIM ];
   jobject obj;
   jobject result;
   char *ptr;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Get the length of the strings. */
   sleng = getLength( env, locator );

   /* Get the shape of the object to retrieve and allocate memory. */
   getCoords( env, shape, dims, &ndim );
   nel = 1;
   for ( i = 0; i < ndim; i++ ) {
      nel *= dims[ i ];
   }
   buffer = jMalloc( env, sleng * nel );
   buffer_length = sleng;

   /* Call the fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_getc)( CHARACTER_ARG(fLocator), INTEGER_ARG(&ndim),
                          INTEGER_ARRAY_ARG(dims), 
                          CHARACTER_ARRAY_ARG(buffer),
                          INTEGER_ARG(status)
                          TRAIL_ARG(fLocator) TRAIL_ARG(buffer) );
   )
   DATGETC_CHECK_EXCEPTION

   /* If we have a zero-dimensional primitive (scalar), just turn this 
    * into a String for return. */
   if ( ndim == 0 ) {
      result = makeString( env, buffer, sleng );
   }
   else {

      /* Initialise the position of the row we will write to, and the 
       * class types of each array.  The new arrays generated here are
       * dummies, only used to work out class types. */
      for ( i = 0; i < ndim; i++ ) {
         pos[ i ] = 0;
         aptrs[ i ] = ( i == 0 ) 
                    ? (*env)->NewStringUTF( env, "" ) 
                    : (*env)->NewObjectArray( env, 0, aclass[ i - 1 ], NULL );
         DATGETC_CHECK_EXCEPTION
         aclass[ i ] = (*env)->GetObjectClass( env, aptrs[ i ] );
         DATGETC_CHECK_EXCEPTION
      }

      /* Read a row at a time, filling up arrays of arrays of .. as we go. */
      ptr = buffer;
      done = 0;
      while ( ! done ) {

         /* Allocate new arrays for those dimensions we are at the start of. */
         for ( i = 0; i < ndim; i++ ) {
            if ( pos[ i ] > 0 ) {
               break;
            }
            else {
               aptrs[ i ] = (*env)->NewObjectArray( env, dims[ i ], 
                                                    aclass[ i ], NULL );
               DATGETC_CHECK_EXCEPTION
            }
         }

         /* Store string in arrays, and references to completed arrays in
            elements of higher order arrays. */
         done = 1;
         for ( i = 0; i < ndim; i++ ) {
            obj = ( i == 0 ) ? makeString( env, ptr, sleng )
                             : aptrs[ i - 1 ];
            (*env)->SetObjectArrayElement( env, aptrs[ i ], pos[ i ], obj );
            DATGETC_CHECK_EXCEPTION

            /* Move to the next position in the array. */
            pos[ i ] = ( pos[ i ] + 1 ) % dims[ i ];
            if ( pos[ i ] != 0 ) {
               done = 0;
               break;
            }
         }
         ptr += sleng;
      }
      result = aptrs[ ndim - 1 ];
   }
   free( buffer );
   return result;
}
#undef DATGETC_CHECK_EXCEPTION


#define DATGET_CHECK_EXCEPTION \
   if ( (*env)->ExceptionOccurred( env ) != NULL ) { \
      free( buffer ); \
      return NULL; \
   }
#define MAKE_DATGETX(Xletter,Xjtype,XJtype,Xjclass,Xcnftype) \
JNIEXPORT jobject JNICALL NATIVE_METHOD( datGet##Xletter )( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jlongArray shape      /* Array giving shape to retrieve */ \
) { \
   char locator[ DAT__SZLOC ]; \
   DECLARE_CHARACTER( fLocator, DAT__SZLOC ); \
   INT_BIG dims[ DAT__MXDIM ]; \
   INT_BIG ndim; \
   INT_BIG pos[ DAT__MXDIM ]; \
   int done; \
   int i; \
   int nel; \
   jarray aptrs[ DAT__MXDIM ]; \
   jclass aclass[ DAT__MXDIM ]; \
   jobject result; \
   Xjtype##Array row; \
   Xjtype *buffer; \
   Xjtype *ptr; \
   \
   /* Get this object's locator. */ \
   getLocator( env, this, locator ); \
   cnfExpch( locator, fLocator, fLocator_length ); \
   \
   /* Get the shape of the object to retrieve and allocate memory. */ \
   getCoords( env, shape, dims, &ndim ); \
   nel = 1; \
   for ( i = 0; i < ndim; i++ ) { \
      nel *= dims[ i ]; \
   } \
   buffer = jMalloc( env, sizeof( Xjtype ) * nel ); \
   \
   /* Call the Fortran routine to do the work. */ \
   HDSCALL( \
      F77_CALL(dat_get##Xletter)( CHARACTER_ARG(fLocator), INTEGER_ARG(&ndim), \
                                  INTEGER_ARRAY_ARG(dims), \
                                  Xcnftype##_ARG(buffer), \
                                  INTEGER_ARG(status) \
                                  TRAIL_ARG(fLocator) ); \
   ) \
   DATGET_CHECK_EXCEPTION \
   /* If we have a zero-dimensional primitive (scalar), just wrap this in a \
    * class ready for return. */ \
   if ( ndim == 0 ) { \
      result = (*env)->NewObject( env, Xjclass##Class, \
                                  Xjclass##ConstructorID, (Xjtype) *buffer ); \
   } \
   else { \
   \
      /* Initialise the position of the row we will write to, and the \
       * class types of each array.  The new arrays generated here are \
       * dummies, only used to work out class types. */ \
      for ( i = 1; i < ndim; i++ ) { \
         pos[ i ] = 0; \
         aptrs[ i ] = ( i == 1 ) \
                    ? (*env)->New##XJtype##Array( env, 0 ) \
                    : (*env)->NewObjectArray( env, 0, aclass[ i - 1 ], NULL ); \
         DATGET_CHECK_EXCEPTION \
         aclass[ i ] = (*env)->GetObjectClass( env, aptrs[ i ] ); \
         DATGET_CHECK_EXCEPTION \
      } \
   \
      /* Read a row at a time, filling up arrays of arrays of ... as we go. */ \
      ptr = buffer; \
      done = 0; \
      while ( ! done ) { \
   \
         /* Allocate new arrays for those dimensions we are at the start \
          * of. */ \
         for ( i = 1; i < ndim; i++ ) { \
            if ( pos[ i ] > 0 ) { \
               break; \
            } \
            else { \
               aptrs[ i ] = (*env)->NewObjectArray( env, dims[ i ], \
                                                    aclass[ i ], NULL ); \
               DATGET_CHECK_EXCEPTION \
            } \
         } \
   \
         /* Copy a row of data into a java primitive array. */ \
         aptrs[ 0 ] = (*env)->New##XJtype##Array( env, dims[ 0 ] ); \
         (*env)->Set##XJtype##ArrayRegion( env, aptrs[ 0 ], 0, \
                                           dims[ 0 ], ptr ); \
   \
         /* Store references of completed arrays in elements of the next \
          * higher order arrays. */ \
         done = 1; \
         for ( i = 1; i < ndim; i++ ) { \
            (*env)->SetObjectArrayElement( env, aptrs[ i ], pos[ i ], \
                                           aptrs[ i - 1 ] ); \
            DATGET_CHECK_EXCEPTION \
   \
            /* Move to the next position in the array. */ \
            pos[ i ] = ( pos[ i ] + 1 ) % dims[ i ]; \
            if ( pos[ i ] != 0 ) { \
               done = 0; \
               break; \
            } \
         } \
         ptr += dims[ 0 ]; \
      } \
      result = aptrs[ ndim - 1 ]; \
   } \
   free( buffer ); \
   return result; \
}
MAKE_DATGETX(l,jboolean,Boolean,Boolean,LOGICAL)
MAKE_DATGETX(i,jint,Int,Integer,INTEGER)
MAKE_DATGETX(r,jfloat,Float,Float,REAL)
MAKE_DATGETX(d,jdouble,Double,Double,DOUBLE)
#undef MAKE_DATGETX
#undef DATGET_CHECK_EXCEPTION

JNIEXPORT jobjectArray JNICALL NATIVE_METHOD( datGetvc )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER_ARRAY_DYN( buffer );
   INT_BIG nel;
   INT_BIG size;
   int i;
   int length;
   jstring strobj;
   jobjectArray result;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Get the number and length of elements which will be read. */
   size = getSize( env, locator );
   length = getLength( env, locator );

   /* Create a buffer to hold the data. */
   buffer = jMalloc( env, length * size );
   buffer_length = length;

   /* Read all the characters into the buffer. */
   HDSCALL(
      F77_CALL(dat_getvc)( CHARACTER_ARG(fLocator), INTEGER_ARG(&size),
                           CHARACTER_ARRAY_ARG(buffer), INTEGER_ARG(&nel),
                           INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) TRAIL_ARG(buffer) );
   )

   /* Create a String array to hold the results. */
   if ( ! (*env)->ExceptionOccurred( env ) ) {
      result = (*env)->NewObjectArray( env, size, StringClass, NULL );
   }

   /* Copy the data into the String array. */
   for ( i = 0; i < nel; i++ ) {
      if ( ! (*env)->ExceptionOccurred( env ) ) {
         strobj = makeString( env, buffer + i * length, length );
         if ( strobj ) {
            (*env)->SetObjectArrayElement( env, result, i, strobj );
         }
      }
   }

   /* Release resources. */
   free( buffer );

   /* Return the constructed array. */
   return result;
}

#define MAKE_DATGETVX(Xletter,Xjtype,XJtype,Xcnftype) \
JNIEXPORT Xjtype##Array JNICALL NATIVE_METHOD( datGetv##Xletter )( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this          /* Instance object */ \
) { \
   char locator[ DAT__SZLOC ]; \
   DECLARE_CHARACTER( fLocator, DAT__SZLOC ); \
   INT_BIG nel; \
   INT_BIG size; \
   Xjtype *buffer; \
   Xjtype##Array result; \
   \
   /* Get this object's locator. */ \
   getLocator( env, this, locator ); \
   cnfExpch( locator, fLocator, fLocator_length ); \
   \
   /* Get the number of elements which will be read. */ \
   size = getSize( env, locator ); \
   \
   /* Create and pin/copy a primitive array to hold the result. */ \
   RETURN_IF_EXCEPTION; \
   result = (*env)->New##XJtype##Array( env, (int) size ); \
   RETURN_IF_EXCEPTION; \
   buffer = (*env)->Get##XJtype##ArrayElements( env, result, NULL ); \
   RETURN_IF_EXCEPTION; \
   \
   /* Call the Fortran routine to read the data into the primitive array. */ \
   HDSCALL( \
      F77_CALL(dat_getv##Xletter)( CHARACTER_ARG(fLocator), \
                                   INTEGER_ARG(&size), \
                                   Xcnftype##_ARG(buffer), INTEGER_ARG(&nel), \
                                   INTEGER_ARG(status) \
                                   TRAIL_ARG(fLocator) ); \
   ) \
   \
   /* Release the array to ensure that the primitive java array is */ \
   /* synched with the filled version. */ \
   RETURN_IF_EXCEPTION; \
   (*env)->Release##XJtype##ArrayElements( env, result, buffer, 0 ); \
   \
   /* Return the released array. */ \
   return result; \
}
MAKE_DATGETVX(l,jboolean,Boolean,LOGICAL)
MAKE_DATGETVX(i,jint,Int,INTEGER)
MAKE_DATGETVX(r,jfloat,Float,REAL)
MAKE_DATGETVX(d,jdouble,Double,DOUBLE)
#undef MAKE_DATGETVX


JNIEXPORT jobject JNICALL NATIVE_METHOD( datIndex )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint index            /* Index of component to retrieve */
) {
   char locator[ DAT__SZLOC ];
   char newLocator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER( fNewLocator, DAT__SZLOC );
   INT_BIG fIndex;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Convert arguments to Fortran types. */
   fIndex = (INT_BIG) index;

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_index)( CHARACTER_ARG(fLocator), INTEGER_ARG(&fIndex),
                           CHARACTER_ARG(fNewLocator), INTEGER_ARG(status) 
                           TRAIL_ARG(fLocator) TRAIL_ARG(fNewLocator) );
   )

   /* Turn the returned locator into a C string. */
   cnfImpch( fNewLocator, DAT__SZLOC, newLocator );

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, newLocator );
}


JNIEXPORT jstring JNICALL NATIVE_METHOD( datName )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   char name[ DAT__SZNAM + 1 ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER( fName, DAT__SZNAM );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_name)( CHARACTER_ARG(fLocator), CHARACTER_ARG(fName),
                          INTEGER_ARG(status)
                          TRAIL_ARG(fLocator) TRAIL_ARG(fName) );
   )

   /* Convert the Fortran string back to C. */
   cnfImprt( fName, fName_length, name );

   /* Construct and return a java string object. */
   return (*env)->NewStringUTF( env, name );
}


JNIEXPORT jint JNICALL NATIVE_METHOD( datNcomp )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   INT_BIG ncomp;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_ncomp)( CHARACTER_ARG(fLocator), INTEGER_ARG(&ncomp),
                           INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) );
   )

   /* Return the retrieved integer value. */
   return (jint) ncomp;
}


JNIEXPORT void JNICALL NATIVE_METHOD( datNew )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jName,        /* Component name */
   jstring jType,        /* Component type */
   jlongArray jDims      /* Component dimensions */
) {
   char locator[ DAT__SZLOC ];
   const char *name;
   const char *type;
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER( fName, DAT__SZNAM );
   DECLARE_CHARACTER( fType, DAT__SZTYP );
   INT_BIG dims[ DAT__MXDIM ];
   INT_BIG ndim;

   /* Convert java strings to C. */
   name = (*env)->GetStringUTFChars( env, jName, NULL );
   type = (*env)->GetStringUTFChars( env, jType, NULL );

   /* Convert C strings to fortran. */
   cnfExprt( name, fName, fName_length );
   cnfExprt( type, fType, fType_length );

   /* Release string copies. */
   (*env)->ReleaseStringUTFChars( env, jName, name );
   (*env)->ReleaseStringUTFChars( env, jType, type );

   /* Convert java array into fortran. */
   getCoords( env, jDims, dims, &ndim );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_new)( CHARACTER_ARG(fLocator), CHARACTER_ARG(fName),
                         CHARACTER_ARG(fType), INTEGER_ARG(&ndim),
                         INTEGER_ARRAY_ARG(dims), INTEGER_ARG(status)
                         TRAIL_ARG(fLocator) TRAIL_ARG(fName) 
                         TRAIL_ARG(fType) );
   )
}


JNIEXPORT jobject JNICALL NATIVE_METHOD( datParen )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   char newLocator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER( fNewLocator, DAT__SZLOC );
   jobject newobj;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_paren)( CHARACTER_ARG(fLocator), CHARACTER_ARG(fNewLocator),
                           INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) TRAIL_ARG(fNewLocator) );
   )

   /* Turn the returned locator into a C string. */
   cnfImpch( fNewLocator, DAT__SZLOC, newLocator );

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, newLocator );
}


JNIEXPORT jboolean JNICALL NATIVE_METHOD( datPrmry__ )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   F77_LOGICAL_TYPE const fSet = F77_FALSE;
   F77_LOGICAL_TYPE fPrimary;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_prmry)( LOGICAL_ARG(&fSet), CHARACTER_ARG(fLocator),
                           LOGICAL_ARG(&fPrimary), INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) );
   )

   /* Return the result. */
   return (jboolean) ( F77_ISTRUE( fPrimary ) ? JNI_TRUE : JNI_FALSE );
}                    


JNIEXPORT void JNICALL NATIVE_METHOD( datPrmry__Z )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jboolean primary      /* Is the locator to be set primary? */
) {
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   F77_LOGICAL_TYPE const fSet = F77_TRUE;
   F77_LOGICAL_TYPE fPrimary;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Set the value of the logical variable. */
   fPrimary = ( ( primary == JNI_TRUE ) ? F77_TRUE : F77_FALSE );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_prmry)( LOGICAL_ARG(&fSet), CHARACTER_ARG(fLocator),
                           LOGICAL_ARG(&fPrimary), INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) );
   )
}


JNIEXPORT void JNICALL NATIVE_METHOD( datPut0c )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jValue        /* String to write */
) {
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   const char *value;
   DECLARE_CHARACTER_DYN( fValue );

   /* Convert java string to C. */
   value = (*env)->GetStringUTFChars( env, jValue, NULL );

   /* Convert C string to fortran. */
   fValue_length = (*env)->GetStringLength( env, jValue );
   fValue = jMalloc( env, fValue_length );
   cnfExprt( value, fValue, fValue_length );

   /* Release string copy. */
   (*env)->ReleaseStringUTFChars( env, jValue, value );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_put0c)( CHARACTER_ARG(fLocator), CHARACTER_ARG(fValue),
                           INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) TRAIL_ARG(fValue) );
   )

   /* Free the buffer. */
   free( fValue );
}


#define MAKE_DATPUT0X(Xletter,Xjtype,Xcnftype) \
JNIEXPORT void JNICALL NATIVE_METHOD( datPut0##Xletter )( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   Xjtype jValue         /* Value to write */ \
) { \
   char locator[ DAT__SZLOC ]; \
   DECLARE_CHARACTER( fLocator, DAT__SZLOC ); \
   DECLARE_##Xcnftype( fValue ); \
 \
   /* Convert the java value to Fortran. */ \
   fValue = (F77_##Xcnftype##_TYPE) jValue; \
 \
   /* Get this object's locator. */ \
   getLocator( env, this, locator ); \
   cnfExpch( locator, fLocator, fLocator_length ); \
 \
   /* Call the Fortran routine to do the work. */ \
   HDSCALL( \
      F77_CALL(dat_put0##Xletter)( CHARACTER_ARG(fLocator), \
                                   Xcnftype##_ARG(&fValue), \
                                   INTEGER_ARG(status) \
                                   TRAIL_ARG(fLocator) ); \
   ) \
}
MAKE_DATPUT0X(l,jboolean,LOGICAL)
MAKE_DATPUT0X(i,jint,INTEGER)
MAKE_DATPUT0X(r,jfloat,REAL)
MAKE_DATPUT0X(d,jdouble,DOUBLE)
#undef MAKE_DATPUT0X
   

JNIEXPORT void JNICALL NATIVE_METHOD( datPutvc )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobjectArray value    /* Array of strings to write */
) {
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER_DYN( buffer );
   INT_BIG nel;
   int i;
   int length;
   jstring strobj;
   const char *cp;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Get the number and length of elements which will be written. */
   nel = (*env)->GetArrayLength( env, value );
   length = getLength( env, locator );

   /* Create a buffer to hold the data. */
   buffer = jMalloc( env, length * nel );
   buffer_length = length;

   /* Copy the contents of the String array into the buffer. */
   for ( i = 0; i < nel; i++ ) {
      if ( ! (*env)->ExceptionOccurred( env ) ) {
         ( strobj = (jstring) (*env)->GetObjectArrayElement( env, value, 
                                                             i ) ) &&
         ( cp = (*env)->GetStringUTFChars( env, strobj, NULL ) );
         if ( cp != NULL ) {
            cnfExprt( cp, buffer + i * length, length );
         }
         (*env)->ReleaseStringUTFChars( env, strobj, cp );
      }
   }

   /* Call the fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_putvc)( CHARACTER_ARG(fLocator), INTEGER_ARG(&nel),
                           CHARACTER_ARRAY_ARG(buffer), INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) TRAIL_ARG(buffer) );
   )

   /* Release resources. */
   free( buffer );
   return;
}

#define MAKE_DATPUTVX(Xletter,Xjtype,XJtype,Xcnftype) \
JNIEXPORT void JNICALL NATIVE_METHOD( datPutv##Xletter )( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   Xjtype##Array value   /* Array of primitives to write */ \
) { \
   char locator[ DAT__SZLOC ]; \
   DECLARE_CHARACTER( fLocator, DAT__SZLOC ); \
   Xjtype *buffer; \
   INT_BIG nel; \
 \
   /* Get this object's locator. */ \
   getLocator( env, this, locator ); \
   cnfExpch( locator, fLocator, fLocator_length ); \
 \
   /* Get the number of elements which will be written. */ \
   nel = (*env)->GetArrayLength( env, value ); \
 \
   /* Pin/copy the elements of the primitive array. */ \
   buffer = (*env)->Get##XJtype##ArrayElements( env, value, NULL ); \
 \
   /* Call the fortran routine to write this vector out. */ \
   HDSCALL( \
      F77_CALL(dat_putv##Xletter)( CHARACTER_ARG(fLocator), \
                                   INTEGER_ARG(&nel), \
                                   Xcnftype##_ARG(buffer), INTEGER_ARG(status) \
                                   TRAIL_ARG(fLocator) ); \
   ) \
 \
   /* Release the array elements. */ \
   (*env)->Release##XJtype##ArrayElements( env, value, buffer, JNI_ABORT ); \
}
MAKE_DATPUTVX(l,jboolean,Boolean,LOGICAL)
MAKE_DATPUTVX(i,jint,Int,INTEGER)
MAKE_DATPUTVX(r,jfloat,Float,REAL)
MAKE_DATPUTVX(d,jdouble,Double,DOUBLE)
#undef MAKE_DATPUTVX


JNIEXPORT jstring JNICALL NATIVE_METHOD( datRef )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   jstring jRef = NULL;
   char ref[ MAXCHARLENG + 1 ];
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER( fRef, MAXCHARLENG );
   DECLARE_INTEGER( lref );
   
   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_ref)( CHARACTER_ARG(fLocator), CHARACTER_ARG(fRef),
                         INTEGER_ARG(&lref), INTEGER_ARG(status)
                         TRAIL_ARG(fLocator) TRAIL_ARG(fRef) );
   )

   /* Construct a java String object. */
   if ( ! (*env)->ExceptionOccurred( env ) ) {

      /* Convert the fortran string back to C */
      cnfImprt( fRef, fRef_length, ref );
      ref[ lref ] = '\0';

      jRef = (*env)->NewStringUTF( env, ref );
   }

   /* Return the result. */
   return jRef;
}


JNIEXPORT jlong JNICALL NATIVE_METHOD( datSize )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   INT_BIG size;
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   jlong result;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_size)( CHARACTER_ARG(fLocator), INTEGER_ARG(&size),
                          INTEGER_ARG(status)
                          TRAIL_ARG(fLocator) );
   )

   /* Return the result. */
   return (jlong) size;
}


/*
 * datSlice() doesn't seem to work for some reason.
 * This native function no longer called from the HDSObject class.
 */
JNIEXPORT jobject JNICALL NATIVE_METHOD( datSlice )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jlongArray jLbnd,     /* Lower bounds of slice */
   jlongArray jUbnd      /* Upper bounds of slice */
) {
   char locator[ DAT__SZLOC ];
   char newLocator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER( fNewLocator, DAT__SZLOC );
   INT_BIG lbnd[ DAT__MXDIM ];
   INT_BIG ubnd[ DAT__MXDIM ];
   INT_BIG ndim;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Get arrays representing the lower and upper bounds of the slice. */
   getCoords( env, jLbnd, lbnd, &ndim );
   getCoords( env, jUbnd, ubnd, &ndim );

   /* Call the Fortran routine to do the work. */
   HDSCALL( 
      F77_CALL(dat_slice)( CHARACTER_ARG(fLocator), INTEGER_ARG(&ndim),
                           INTEGER_ARRAY_ARG(lbnd), INTEGER_ARRAY_ARG(ubnd),
                           CHARACTER_ARG(fNewLocator), INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) TRAIL_ARG(fNewLocator) );
   )

   /* Turn the returned locator into a C string. */
   cnfImpch( fNewLocator, DAT__SZLOC, newLocator );

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, newLocator );
}


JNIEXPORT jlongArray JNICALL NATIVE_METHOD( datShape )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   int i;
   char locator[ DAT__SZLOC ];
   INT_BIG const ndimx = DAT__MXDIM;
   INT_BIG ndim;
   INT_BIG dims[ DAT__MXDIM ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_shape)( CHARACTER_ARG(fLocator), INTEGER_ARG(&ndimx),
                           INTEGER_ARRAY_ARG(dims), INTEGER_ARG(&ndim),
                           INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) );
   )

   /* Construct and return an array representing the shape. */
   return makeCartesian( env, dims, ndim );
}


JNIEXPORT jboolean JNICALL NATIVE_METHOD( datState )(
   JNIEnv *env,          /* Interfact pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   F77_LOGICAL_TYPE fState;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
       F77_CALL(dat_state)( CHARACTER_ARG(fLocator), LOGICAL_ARG(&fState),
                            INTEGER_ARG(status)
                            TRAIL_ARG(fLocator) );
   )

   /* Return the result. */
   return (jboolean) ( F77_ISTRUE( fState ) ? JNI_TRUE : JNI_FALSE );
}

   
JNIEXPORT jboolean JNICALL NATIVE_METHOD( datStruc )(
   JNIEnv *env,          /* Interfact pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   F77_LOGICAL_TYPE fStruc;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_struc)( CHARACTER_ARG(fLocator), LOGICAL_ARG(&fStruc),
                           INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) );
   )

   /* Return the result. */
   return (jboolean) ( F77_ISTRUE( fStruc ) ? JNI_TRUE : JNI_FALSE );
}


JNIEXPORT jboolean JNICALL NATIVE_METHOD( datThere )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jName         /* Name of queried component */
) {
   const char *name;
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fName, DAT__SZNAM );
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   F77_LOGICAL_TYPE there;

   /* Convert java string to C. */
   name = (*env)->GetStringUTFChars( env, jName, NULL );

   /* Convert C string to Fortran. */
   cnfExprt( name, fName, fName_length );

   /* Release string copy. */
   (*env)->ReleaseStringUTFChars( env, jName, name );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_there)( CHARACTER_ARG(fLocator), CHARACTER_ARG(fName),
                           LOGICAL_ARG(&there), INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) TRAIL_ARG(fName) );
   )

   /* Return the result. */
   return (jboolean) ( F77_ISTRUE( there ) ? JNI_TRUE : JNI_FALSE );
}


JNIEXPORT jstring JNICALL NATIVE_METHOD( datType )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   char type[ DAT__SZTYP + 1 ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_CHARACTER( fType, DAT__SZTYP );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_type)( CHARACTER_ARG(fLocator), CHARACTER_ARG(fType),
                          INTEGER_ARG(status)
                          TRAIL_ARG(fLocator) TRAIL_ARG(fType) );
   )

   /* Convert the Fortran string back to C. */
   cnfImprt( fType, fType_length, type );

   /* Construct and return a java string object. */
   return (*env)->NewStringUTF( env, type );
}


JNIEXPORT void JNICALL NATIVE_METHOD( datUnmap )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_unmap)( CHARACTER_ARG(fLocator), INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) );
   )
}


JNIEXPORT jboolean JNICALL NATIVE_METHOD( datValid )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_LOGICAL( fValid );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_valid)( CHARACTER_ARG(fLocator), LOGICAL_ARG(&fValid),
                           INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) );
   )

   /* Return the result. */
   return (jboolean) ( F77_ISTRUE( fValid ) ? JNI_TRUE : JNI_FALSE );
}


JNIEXPORT jlongArray JNICALL NATIVE_METHOD( datWhere )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char locator[ DAT__SZLOC ];
   jlong offArray[ 2 ];
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_INTEGER( fBlock );
   DECLARE_INTEGER( fOffset );
   jlongArray jOffArray = NULL;

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_where)( CHARACTER_ARG(fLocator), INTEGER_ARG(&fBlock),
                           INTEGER_ARG(&fOffset), INTEGER_ARG(status)
                           TRAIL_ARG(fLocator) );
   )

   /* Package the result. */
   if ( ! (*env)->ExceptionOccurred( env ) ) {
      offArray[ 0 ] = (jlong) fBlock;
      offArray[ 1 ] = (jlong) fOffset;
      jOffArray = (*env)->NewLongArray( env, 2 );
      (*env)->SetLongArrayRegion( env, jOffArray, 0, 2, offArray );
   }

   /* Return the result. */
   return jOffArray;
}


JNIEXPORT jobject JNICALL NATIVE_METHOD( mapBuffer )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jType,        /* Data type string */
   jstring jMode         /* Access mode string */
) {
   const char *type;
   const char *mode;
   char locator[ DAT__SZLOC ];
   DECLARE_CHARACTER( fType, DAT__SZTYP );
   DECLARE_CHARACTER( fMode, DAT__SZMOD );
   DECLARE_CHARACTER( fLocator, DAT__SZLOC );
   DECLARE_INTEGER( el );
   DECLARE_POINTER( fPntr );
   void *pntr;
   jobject bbuf;
   jlong bufleng;
   int siz;
   int i;
   char normtype[ DAT__SZTYP + 1 ];

   /* Convert java strings to C. */
   type = (*env)->GetStringUTFChars( env, jType, NULL );
   mode = (*env)->GetStringUTFChars( env, jMode, NULL );

   /* Normalize the string type and see what kind it is. */
   for ( i = 0; i < DAT__SZTYP && type[ i ] != '\0'; i++ ) {
      normtype[ i ] = toupper( type[ i ] );
   }
   normtype[ i ] = '\0';
   if ( ! strncmp( normtype, "_BYTE", DAT__SZTYP ) ) {
      siz = sizeof( F77_BYTE_TYPE );
   }
   else if ( ! strncmp( normtype, "_UBYTE", DAT__SZTYP ) ) {
      siz = sizeof( F77_UBYTE_TYPE );
   }
   else if ( ! strncmp( normtype, "_WORD", DAT__SZTYP ) ) {
      siz = sizeof( F77_WORD_TYPE );
   }
   else if ( ! strncmp( normtype, "_UWORD", DAT__SZTYP ) ) {
      siz = sizeof( F77_UWORD_TYPE );
   }
   else if ( ! strncmp( normtype, "_INTEGER", DAT__SZTYP ) ) {
      siz = sizeof( F77_INTEGER_TYPE );
   }
   else if ( ! strncmp( normtype, "_REAL", DAT__SZTYP ) ) {
      siz = sizeof( F77_REAL_TYPE );
   }
   else if ( ! strncmp( normtype, "_DOUBLE", DAT__SZTYP ) ) {
      siz = sizeof( F77_DOUBLE_TYPE );
   }
   else {
      char errbuf[ 80 ];
      sprintf( errbuf, "Unsupported mapping type \"%s\"", normtype );
      siz = 0;
      (*env)->ThrowNew( env, IllegalArgumentExceptionClass, errbuf );
   }

   /* Convert C strings to Fortran. */
   cnfExprt( type, fType, fType_length );
   cnfExprt( mode, fMode, fMode_length );

   /* Release string copies. */
   (*env)->ReleaseStringUTFChars( env, jType, type );
   (*env)->ReleaseStringUTFChars( env, jMode, mode );

   /* Get this object's locator. */
   getLocator( env, this, locator );
   cnfExpch( locator, fLocator, fLocator_length );

   /* Call the Fortran routine to do the work. */
   HDSCALL(
      F77_CALL(dat_mapv)( CHARACTER_ARG(fLocator), CHARACTER_ARG(fType),
                          CHARACTER_ARG(fMode), POINTER_ARG(&fPntr),
                          INTEGER_ARG(&el), INTEGER_ARG(status)
                          TRAIL_ARG(fLocator) TRAIL_ARG(fType)
                          TRAIL_ARG(fMode) );
   )

   /* Make a ByteBuffer out of the returned memory. */
   bbuf = NULL;
   bufleng = el * siz;
   pntr = cnfCptr(fPntr);
   if ( ! (*env)->ExceptionCheck( env ) && pntr != NULL && bufleng > 0L ) {
      bbuf = (*env)->NewDirectByteBuffer( env, pntr, bufleng );
   }
   return bbuf;
}



/* $Id$ */
