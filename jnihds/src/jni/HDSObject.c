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
*     PWD: Peter W. Draper (JAC, Durham University)

*  History:
*     1-AUG-2001 (MBT):
*        Initial version.
*     8-NOV-2005 (PWD):
*        Add dat_copy.
*     12-DEC-2005 (MBT):
*        Rewrote to use HDS C interface rather than F77 interface + CNF.

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

/* HDS Types. */
#define HDS_BYTE_SIZE sizeof( char )
#define HDS_WORD_SIZE sizeof( short )
#define HDS_INTEGER_SIZE sizeof( int )
#define HDS_REAL_SIZE sizeof( float )
#define HDS_DOUBLE_SIZE sizeof( double )

/* Header files. */
#include <string.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include "jni.h"
#include "sae_par.h"
#include "ems.h"
#include "ems_par.h"
#include "star/hds.h"
#include "dat_par.h"
#include "uk_ac_starlink_hds_HDSObject.h"

/* Types. */
typedef union {
   jlong value;
   HDSLoc *pointer;
} locatorField;

/* Macros. */

#define RETURN_IF_EXCEPTION \
   if ( (*env)->ExceptionCheck( env ) ) return NULL
#ifdef DEBUG
#define HDSCALL_REPORTLINE \
   sprintf( report, "HDS error at line %d in file %s:\n", __LINE__, __FILE__ )
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
      int status_val = SAI__OK; \
      int *status = &status_val; \
      if ( (*env)->MonitorEnter( env, HDSLock ) == 0 ) { \
         emsMark(); \
         code \
         if ( *status != SAI__OK ) { \
            char namebuf[ EMS__SZPAR + 1 ]; \
            char msgbuf[ EMS__SZMSG + 1 ]; \
            int name_leng; \
            int msg_leng; \
            char report[ JNIHDS_BUFLENG + 1 ]; \
            char *reportpos = report; \
            reportpos += HDSCALL_REPORTLINE; \
            while ( *status != SAI__OK ) { \
               emsEload( namebuf, &name_leng, msgbuf, &msg_leng, status ); \
               memcpy( reportpos, namebuf, name_leng ); \
               reportpos += name_leng; \
               reportpos += sprintf( reportpos, "%s", ": " ); \
               memcpy( reportpos, msgbuf, msg_leng ); \
               reportpos += msg_leng; \
               if ( reportpos + EMS__SZPAR + EMS__SZMSG + 3 \
                    > report + JNIHDS_BUFLENG ) { \
                  emsAnnul( status ); \
                  break; \
               } \
               if ( *status != SAI__OK ) { \
                  *(reportpos++) = '\n'; \
               } \
            } \
            throwable = \
               (*env)->NewObject( env, HDSExceptionClass, \
                                  HDSExceptionConstructorID, \
                                  (*env)->NewStringUTF( env, report ) ); \
         } \
         emsRlse(); \
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


/*
 * Macro for calling a code block which may call JNI functions but must
 * execute even with an exception pending.  Such calls will typically
 * be calls to ReleaseStringUTFChars or Release<Primitive>ArrayElements
 * functions.  It notes any pending exception, clears it if necessary,
 * executes the code block, and re-throws the exception if there was one.
 * The 'env' variable is assumed to be a pointer to the current
 * JNIEnv environment.
 */
#define ALWAYS(code) { \
   jthrowable _jnihds_except = (*env)->ExceptionOccurred( env ); \
   if ( _jnihds_except != NULL ) { \
      (*env)->ExceptionClear( env ); \
   } \
   code \
   if ( _jnihds_except != NULL ) { \
      (*env)->Throw( env, _jnihds_except ); \
   } \
}


/* Macro to check that two types match.  This is currently used when
 * the code would fall over if the types didn't coincide. 
 * The right thing would be to copy the data from one format to another,
 * but it doesn't currently do that.
 */
#define CHECK_TYPES_MATCH(t1,t2) \
   if ( ! (*env)->ExceptionCheck( env ) ) { \
      if ( sizeof( t1 ) != sizeof( t2 ) ) { \
         throwNativeError( env, "Can't perform on this platform: types %s " \
                                "and %s have different sizes (%d!=%d)\n", \
                                #t1, #t2, \
                                (int) sizeof( t1 ), (int) sizeof( t2 ) ); \
      } \
   }


/* Static function prototypes. */
static void throwNativeError( JNIEnv *env, char *fmt, ... );
static jobject makeHDSObject( JNIEnv *env, HDSLoc *locator );
static jlongArray makeCartesian( JNIEnv *env, hdsdim *dims, int ndim );
static jthrowable monitorEntryFailure( JNIEnv *env );
static jthrowable monitorExitFailure( JNIEnv *env );
static HDSLoc *getLocator( JNIEnv *env, jobject object );
static void getCoords( JNIEnv *env, jlongArray jCoordArray, hdsdim *coords, int *ndim );
static size_t getSize( JNIEnv *env, HDSLoc *locator );
static size_t getLength( JNIEnv *env, HDSLoc *locator );
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
static jfieldID HDSObjectLocPtrID = 0;
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
   HDSLoc *locator       /* Locator struct */
) {
   jobject newobj;
   locatorField field;
   field.value = 0;

   if ( (*env)->ExceptionOccurred( env ) == NULL ) {

      /* Construct an empty HDSObject. */
      newobj = (*env)->NewObject( env, HDSObjectClass, HDSObjectConstructorID );

      /* Write the locator bytes into the locator field of the object. */
      if ( newobj != NULL ) {
         field.pointer = locator;
         (*env)->SetLongField( env, newobj, HDSObjectLocPtrID, field.value );
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
   hdsdim *coords,       /* Coordinates of the object */
   int ndim              /* Dimensionality of the object */
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
 * buffer holding a fortran-like string - the buffer is not necessarily
 * zero-terminated, and trailing spaces are stripped. */

   JNIEnv *env,          /* Interface pointer */
   const char *bytes,    /* Start of buffer holding the string */
   int leng              /* Length of buffer */
) {
   jchar *unicodebuf;
   jstring result = NULL;
   const jchar space = (jchar) 0x20;
   jchar c;
   int i;

   /* Copy the characters to a buffer of unicode characters.  This is 
    * necessary because the only JNI string creation function which 
    * allows you to specify length requires unicode. */
   if ( (*env)->ExceptionOccurred( env ) == NULL &&
        ( unicodebuf = jMalloc( env, sizeof( jchar ) * leng ) ) ) {
      for ( i = 0; i < leng; i++ ) {
         c = ((unsigned char *) bytes)[ i ];
         unicodebuf[ i ] = c;
      }

      /* Now work out what the length is if you exclude trailing blanks
       * (this is fortran-like behaviour expected by HDS). */
      while ( leng > 0 && unicodebuf[ leng - 1 ] == space ) {
         leng--;
      }

      /* Construct the jstring. */
      result = (*env)->NewString( env, unicodebuf, (jsize) leng );

      /* Tidy up. */
      free( unicodebuf );
   }

   /* Return. */
   return result;
}


static void getCoords(
   JNIEnv *env,           /* Interface pointer */
   jlongArray jCoordArray,/* Array containing coords */
   hdsdim *coords  ,      /* Buffer to receive coords */
   int *ndim              /* Dimensionality of cartesian array */
) {
   int i;
   jlong *jCoords;

   if ( (*env)->ExceptionOccurred( env ) == NULL ) {

      /* Get the dimensionality. */
      *ndim = (*env)->GetArrayLength( env, jCoordArray );

      /* Copy the elements from the array into a local array. */
      jCoords = (*env)->GetLongArrayElements( env, jCoordArray, NULL );
      for ( i = 0; i < *ndim; i++ ) {
         coords[ i ] = (hdsdim) jCoords[ i ];
      }
      (*env)->ReleaseLongArrayElements( env, jCoordArray, jCoords, JNI_ABORT );
   }
   else {
      *ndim = 0;
   }
}

static size_t getSize(
   JNIEnv *env,          /* Interface pointer */
   HDSLoc *locator       /* Locator struct of HDS object */
) {
   size_t size;

   /* Call datSize to get the size. */
   HDSCALL(
      datSize( locator, &size, status );
   )

   /* Return the size. */
   return size;
}

static size_t getLength(
   JNIEnv *env,          /* Interface pointer */
   HDSLoc *locator       /* Locator struct of HDS object */
) {
   size_t length;

   /* Call datLen to get the length. */
   HDSCALL(
      datLen( locator, &length, status );
   )

   /* Return the length. */
   return length;
}


static HDSLoc *getLocator(
   JNIEnv *env,          /* Interface pointer */
   jobject object        /* Object whose locator we want */
) {
   locatorField field;
   field.pointer = NULL;
   if ( ! (*env)->ExceptionOccurred( env ) ) {
      field.value = (*env)->GetLongField( env, object, HDSObjectLocPtrID );
   }
   return field.pointer;
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
   ( HDSObjectLocPtrID =
      (*env)->GetFieldID( env, HDSObjectClass, "locPtr_", "J" ) ) &&

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

   /* Check that a java long (the type of the HDSObject's locPtr_ field) is
    * big enough to store a pointer.  If not, nothing will work 
    * (rewrite required). */
   if ( sizeof( jlong ) < sizeof( HDSLoc * ) ) {
      throwNativeError( env, 
                        "JNIHDS unavailable: pointer length overflows long"
                        "(%d > %d)", sizeof( HDSLoc * ), sizeof( jlong ) );
   }
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


JNIEXPORT jint JNICALL NATIVE_METHOD( hdsGtune )(
   JNIEnv  *env,         /* Interface pointer */
   jclass  jClass,       /* Class object */
   jstring jParam        /* Parameter to query */
) {
   const char *param;
   int value;

   /* Convert java param string to C. */
   param = (*env)->GetStringUTFChars( env, jParam, NULL );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      hdsGtune( param, &value, status );
   )

   /* Release string copy. */
   ALWAYS(
      (*env)->ReleaseStringUTFChars( env, jParam, param );
   )

   /* Return the result. */
   return (jint) value;
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
   HDSLoc *locator = NULL;

   hdsdim dims[ DAT__MXDIM ];
   int ndim;

   /* Convert java strings to C. */
   container = (*env)->GetStringUTFChars( env, jContainer, NULL );
   name = (*env)->GetStringUTFChars( env, jName, NULL );
   type = (*env)->GetStringUTFChars( env, jType, NULL );

   /* Convert java array into C. */
   getCoords( env, jDims, dims, &ndim );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      hdsNew( container, name, type, ndim, dims, &locator, status );
   )

   /* Release string copies. */
   ALWAYS(
      (*env)->ReleaseStringUTFChars( env, jContainer, container );
      (*env)->ReleaseStringUTFChars( env, jName, name );
      (*env)->ReleaseStringUTFChars( env, jType, type );
   )

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, locator );
}



JNIEXPORT jobject JNICALL NATIVE_METHOD( hdsOpen )(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jstring jContainer,   /* Name of container file */
   jstring jAccess       /* Access mode */
) {
   const char *container;
   const char *access;
   HDSLoc *locator = NULL;

   /* Convert java strings to C. */
   access = (*env)->GetStringUTFChars( env, jAccess, NULL );
   container = (*env)->GetStringUTFChars( env, jContainer, NULL );

   /* Call HDS_OPEN to do the work. */
   HDSCALL(
      hdsOpen( container, access, &locator, status );
   )

   /* Release string copies. */
   ALWAYS(
      (*env)->ReleaseStringUTFChars( env, jAccess, access );
      (*env)->ReleaseStringUTFChars( env, jContainer, container );
   )

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, locator );
}


JNIEXPORT void JNICALL NATIVE_METHOD( hdsShow )(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jstring jTopic        /* Topic to show info on */
) {
   const char *topic;

   /* Convert java string to C. */
   topic = (*env)->GetStringUTFChars( env, jTopic, NULL );

   /* Call HDS_SHOW to do the work. */
   HDSCALL(
      hdsShow( topic, status );
   )

   /* Release string copy. */
   ALWAYS(
      (*env)->ReleaseStringUTFChars( env, jTopic, topic );
   )
}


JNIEXPORT jint JNICALL NATIVE_METHOD( hdsTrace )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject results       /* Array of strings for output */
) {
   HDSLoc *locator;
   int nlev;
   char pathstr[ MAXPATHLENG + 1 ];
   char filestr[ MAXFILENAME + 1 ];
   jstring jFilestr = NULL;
   jstring jPathstr = NULL;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      hdsTrace( locator, &nlev, pathstr, filestr, status,
                MAXPATHLENG, MAXFILENAME );
   )

   /* Turn file and path strings into java String objects. */
   ( ! (*env)->ExceptionCheck( env ) ) &&
   ( jFilestr = (*env)->NewStringUTF( env, filestr ) ) &&
   ( jPathstr = (*env)->NewStringUTF( env, pathstr ) ) &&
   1;

   /* Copy them into the result array. */
   if ( ! (*env)->ExceptionCheck( env ) ) {
      (*env)->SetObjectArrayElement( env, results, (jsize) 0, jPathstr );
   }
   if ( ! (*env)->ExceptionCheck( env ) ) {
      (*env)->SetObjectArrayElement( env, results, (jsize) 1, jFilestr );
   }

   /* Return the result. */
   return (jint) nlev;
}

JNIEXPORT void JNICALL NATIVE_METHOD( hdsTune )(
   JNIEnv  *env,         /* Interface pointer */
   jclass  jClass,       /* Class object */
   jstring jParam,       /* Parameter to tune */
   jint    jValue        /* Parameter value */
) {
   const char *param;

   /* Convert java param string to C. */
   param = (*env)->GetStringUTFChars( env, jParam, NULL );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      hdsTune( param, (int) jValue, status );
   )

   /* Release string copy. */
   ALWAYS(
      (*env)->ReleaseStringUTFChars( env, jParam, param );
   )
}


JNIEXPORT void JNICALL NATIVE_METHOD( datAnnul )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator = getLocator( env, this );
   locatorField field;
   field.value = 0;
   HDSCALL(
      datAnnul( &locator, status );
   )
   field.pointer = locator;
   (*env)->SetLongField( env, this, HDSObjectLocPtrID, field.value );
}


JNIEXPORT jobject JNICALL NATIVE_METHOD( datCell )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jlongArray position   /* Array indicating which cell to get */
) {
   HDSLoc *locator;
   HDSLoc *newLocator = NULL;
   hdsdim coords[ DAT__MXDIM ];
   int ndim = 0;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Get an array representing the position of the cell to retrieve. */
   getCoords( env, position, coords, &ndim );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datCell( locator, ndim, coords, &newLocator, status );
   )

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, newLocator );
}


JNIEXPORT jobject JNICALL NATIVE_METHOD( datClone )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator;
   HDSLoc *newLocator = NULL;
   jobject newobj;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datClone( locator, &newLocator, status );
   )

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, newLocator );
}

JNIEXPORT void JNICALL NATIVE_METHOD( datCopy )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject dest,         /* Destination locator */
   jstring jName         /* Name of destination component */
) {
   HDSLoc *loc1;
   HDSLoc *loc2;
   const char *name;

   /* Get this object's locator. */
   loc1 = getLocator( env, this );

   /* Get destination locator */
   loc2 = getLocator( env, dest );

   /* Convert name java string to C. */
   name = (*env)->GetStringUTFChars( env, jName, NULL );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datCopy( loc1, loc2, name, status );
   )

   /* Release string copy. */
   ALWAYS(
      (*env)->ReleaseStringUTFChars( env, jName, name );
   )
}


JNIEXPORT void JNICALL NATIVE_METHOD( datErase )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jName         /* Name of contained component */
) {
   HDSLoc *locator;
   const char *name;

   /* Convert java string to C. */
   name = (*env)->GetStringUTFChars( env, jName, NULL );

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datErase( locator, name, status );
   )

   /* Release string copy. */
   ALWAYS(
      (*env)->ReleaseStringUTFChars( env, jName, name );
   )
}


JNIEXPORT jobject JNICALL NATIVE_METHOD( datFind )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jName         /* Name of contained component */
) {
   const char *name;
   HDSLoc *locator;
   HDSLoc *newLocator = NULL;

   /* Convert java string to C. */
   name = (*env)->GetStringUTFChars( env, jName, NULL );

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datFind( locator, name, &newLocator, status );
   )

   /* Release string copy. */
   ALWAYS(
      if ( name ) {
         (*env)->ReleaseStringUTFChars( env, jName, name );
      }
   )

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, newLocator );
}


JNIEXPORT jstring JNICALL NATIVE_METHOD( datGet0c )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator;
   char value[ MAXCHARLENG + 1 ];

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datGet0C( locator, value, MAXCHARLENG, status );
   )

   /* Construct and return a java string object. */
   return (*env)->NewStringUTF( env, value );
}

#define MAKE_DATGET0X(Xletter,XLetter,Xjtype,Xctype) \
JNIEXPORT Xjtype JNICALL NATIVE_METHOD( datGet0##Xletter )( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this          /* Instance object */ \
) { \
   HDSLoc *locator; \
   Xctype value; \
   \
   /* Get this object's locator. */ \
   locator = getLocator( env, this ); \
   \
   /* Call the HDS routine to do the work. */ \
   HDSCALL( \
      datGet0##XLetter( locator, &value, status ); \
   ) \
   \
   /* Cast and return the retrieved value. */ \
   return (Xjtype) value; \
}
MAKE_DATGET0X(l,L,jboolean,int)
MAKE_DATGET0X(i,I,jint,int)
MAKE_DATGET0X(r,R,jfloat,float)
MAKE_DATGET0X(d,D,jdouble,double)
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
   HDSLoc *locator;
   char *buffer;
   hdsdim dims[ DAT__MXDIM ];
   int ndim;
   hdsdim pos[ DAT__MXDIM ];
   int done;
   int i;
   hdsdim nel;
   int sleng;
   jarray aptrs[ DAT__MXDIM ];
   jclass aclass[ DAT__MXDIM ];
   jobject obj;
   jobject result;
   char *ptr;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Get the length of the strings. */
   sleng = getLength( env, locator );

   /* Get the shape of the object to retrieve and allocate memory. */
   getCoords( env, shape, dims, &ndim );
   nel = 1;
   for ( i = 0; i < ndim; i++ ) {
      nel *= dims[ i ];
   }
   buffer = jMalloc( env, sleng * nel );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datGetC( locator, ndim, dims, buffer, sleng, status );
   )
   DATGETC_CHECK_EXCEPTION

   /* If we have a zero-dimensional primitive (scalar), just turn this
    * into a String for return. */
   if ( ndim == 0 ) {
      result = (*env)->NewStringUTF( env, buffer );
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
#define MAKE_DATGETX(Xletter,XLetter,Xjtype,XJtype,Xjclass,Xctype) \
JNIEXPORT jobject JNICALL NATIVE_METHOD( datGet##Xletter )( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jlongArray shape      /* Array giving shape to retrieve */ \
) { \
   HDSLoc *locator; \
   hdsdim dims[ DAT__MXDIM ]; \
   int ndim; \
   hdsdim pos[ DAT__MXDIM ]; \
   int done; \
   int i; \
   hdsdim nel; \
   jarray aptrs[ DAT__MXDIM ]; \
   jclass aclass[ DAT__MXDIM ]; \
   jobject result; \
   Xjtype##Array row; \
   Xjtype *buffer; \
   Xjtype *ptr; \
   \
   CHECK_TYPES_MATCH(Xjtype,Xctype) \
   \
   /* Get this object's locator. */ \
   locator = getLocator( env, this ); \
   \
   /* Get the shape of the object to retrieve and allocate memory. */ \
   getCoords( env, shape, dims, &ndim ); \
   nel = 1; \
   for ( i = 0; i < ndim; i++ ) { \
      nel *= dims[ i ]; \
   } \
   buffer = jMalloc( env, sizeof( Xctype ) * nel ); \
   \
   /* Call the HDS routine to do the work. */ \
   HDSCALL( \
      datGet##XLetter( locator, ndim, dims, (Xctype *) buffer, status ); \
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
MAKE_DATGETX(l,L,jboolean,Boolean,Boolean,int)
MAKE_DATGETX(i,I,jint,Int,Integer,int)
MAKE_DATGETX(r,R,jfloat,Float,Float,float)
MAKE_DATGETX(d,D,jdouble,Double,Double,double)
#undef MAKE_DATGETX
#undef DATGET_CHECK_EXCEPTION


JNIEXPORT jobjectArray JNICALL NATIVE_METHOD( datGetvc )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator;
   char *buffer = NULL;
   char **ptrs = NULL;
   size_t nel;
   size_t size;
   size_t length;
   size_t bufleng;
   jobjectArray result;
   int i;
   jstring strobj;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Get the number and length of elements which will be read. */
   size = getSize( env, locator );
   length = getLength( env, locator );
   bufleng = size * length;

   /* Create buffers to hold the data. */
   if ( ! (*env)->ExceptionOccurred( env ) &&
        ( buffer = jMalloc( env, bufleng ) ) &&
        ( ptrs = jMalloc( env, ( size + 1 ) * sizeof( char * ) ) ) ) {

      /* Call the HDS routine to do the work. */
      HDSCALL(
         datGetVC( locator, size, bufleng, buffer, ptrs, &nel, status );
      )

      /* Create a String array to hold the results. */
      if ( ! (*env)->ExceptionOccurred( env ) &&
           ( result = (*env)->NewObjectArray( env, nel, StringClass,
                                              NULL ) ) ) {

         /* Copy the data into the String array. */
         for ( i = 0; i < nel; i++ ) {
            if ( ! (*env)->ExceptionOccurred( env ) &&
                 ( strobj = (*env)->NewStringUTF( env, ptrs[ i ] ) ) ) {
               (*env)->SetObjectArrayElement( env, result, i, strobj );
            }
         }
      }
   }

   /* Release resources. */
   if ( buffer ) {
      free( buffer );
   }
   if ( ptrs ) {
      free( ptrs );
   }

   /* Return the constructed array. */
   return result;
}


#define MAKE_DATGETVX(Xletter,XLetter,Xjtype,XJtype,Xctype) \
JNIEXPORT Xjtype##Array JNICALL NATIVE_METHOD( datGetv##Xletter )( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this          /* Instance object */ \
) { \
   HDSLoc *locator; \
   size_t size; \
   size_t nel; \
   Xjtype *buffer = NULL; \
   Xjtype##Array result = NULL; \
   \
   CHECK_TYPES_MATCH(Xjtype,Xctype) \
   \
   /* Get this object's locator. */ \
   locator = getLocator( env, this ); \
   \
   /* Get the number of elements which will be read. */ \
   size = getSize( env, locator ); \
   \
   /* Create and pin/copy a primitive array to hold the result. */ \
   if ( ! (*env)->ExceptionOccurred( env ) && \
        ( result = (*env)->New##XJtype##Array( env, (jsize) size ) ) && \
        ( buffer = (*env)->Get##XJtype##ArrayElements( env, result, \
                                                       NULL ) ) ) { \
      \
      /* Call the HDS routine. */ \
      HDSCALL( \
         datGetV##XLetter( locator, size, (Xctype *) buffer, &nel, status ); \
      ) \
      \
      /* Release the array to ensure that the primitive java array is */ \
      /* synched with the filled version. */ \
      ALWAYS( \
         (*env)->Release##XJtype##ArrayElements( env, result, buffer, 0 ); \
      ) \
   } \
   \
   /* Return the filled array. */ \
   return result; \
}
MAKE_DATGETVX(l,L,jboolean,Boolean,int)
MAKE_DATGETVX(i,I,jint,Int,int)
MAKE_DATGETVX(r,R,jfloat,Float,float)
MAKE_DATGETVX(d,D,jdouble,Double,double)
#undef MAKE_DATGETVX


JNIEXPORT jobject JNICALL NATIVE_METHOD( datIndex )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint index            /* Index of component to retrieve */
) {
   HDSLoc *locator;
   HDSLoc *newLocator = NULL;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datIndex( locator, index, &newLocator, status );
   )

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, newLocator );
}


JNIEXPORT jstring JNICALL NATIVE_METHOD( datName )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator;
   char name[ DAT__SZNAM + 1 ];

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datName( locator, name, status );
   )

   /* Construct and return a java string object. */
   return (*env)->NewStringUTF( env, name );
}


JNIEXPORT jint JNICALL NATIVE_METHOD( datNcomp )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator;
   int ncomp;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datNcomp( locator, &ncomp, status );
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
   HDSLoc *locator;
   const char *name;
   const char *type;
   hdsdim dims[ DAT__MXDIM ];
   int ndim;

   /* Convert java strings to C. */
   name = (*env)->GetStringUTFChars( env, jName, NULL );
   type = (*env)->GetStringUTFChars( env, jType, NULL );

   /* Convert java array into C. */
   getCoords( env, jDims, dims, &ndim );

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datNew( locator, name, type, ndim, dims, status );
   )

   /* Release string copies. */
   ALWAYS(
      (*env)->ReleaseStringUTFChars( env, jName, name );
      (*env)->ReleaseStringUTFChars( env, jType, type );
   )
}


JNIEXPORT jobject JNICALL NATIVE_METHOD( datParen )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator;
   HDSLoc *newLocator = NULL;
   jobject newobj;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datParen( locator, &newLocator, status );
   )

   /* Construct and return an HDSObject using the locator. */
   return makeHDSObject( env, newLocator );
}


JNIEXPORT jboolean JNICALL NATIVE_METHOD( datPrmry__ )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator;
   int set = 0;
   int prmry;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datPrmry( 0, &locator, &prmry, status );
   )

   /* Return the result. */
   return (jboolean) ( prmry ? JNI_TRUE : JNI_FALSE );
}


JNIEXPORT void JNICALL NATIVE_METHOD( datPrmry__Z )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jboolean jPrimary      /* Is the locator to be set primary? */
) {
   HDSLoc *locator;
   int primary = ( jPrimary == JNI_TRUE ) ? 1 : 0;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datPrmry( 1, &locator, &primary, status );
   )
}


JNIEXPORT void JNICALL NATIVE_METHOD( datPut0c )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jValue        /* String to write */
) {
   HDSLoc *locator;
   const char *value;

   /* Convert java string to C. */
   value = (*env)->GetStringUTFChars( env, jValue, NULL );

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datPut0C( locator, value, status );
   )

   /* Release string copy. */
   ALWAYS(
      (*env)->ReleaseStringUTFChars( env, jValue, value );
   )
}


#define MAKE_DATPUT0X(Xletter,XLetter,Xjtype,Xctype) \
JNIEXPORT void JNICALL NATIVE_METHOD( datPut0##Xletter )( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   Xjtype jValue         /* Value to write */ \
) { \
   HDSLoc *locator; \
 \
   /* Get this object's locator. */ \
   locator = getLocator( env, this ); \
 \
   /* Call the HDS routine to do the work. */ \
   HDSCALL( \
      datPut0##XLetter( locator, (Xctype) jValue, status ); \
   ) \
}
MAKE_DATPUT0X(l,L,jboolean,int)
MAKE_DATPUT0X(i,I,jint,int)
MAKE_DATPUT0X(r,R,jfloat,float)
MAKE_DATPUT0X(d,D,jdouble,double)
#undef MAKE_DATPUT0X


JNIEXPORT void JNICALL NATIVE_METHOD( datPutvc )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobjectArray jValueArray  /* Array of strings to write */
) {
   HDSLoc *locator;
   size_t nel;
   size_t length;
   const char **ptrs = NULL;
   int i;
   jstring *jValues;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Get the number and length of elements which will be written. */
   nel = (*env)->GetArrayLength( env, jValueArray );
   length = getLength( env, locator );

   /* Allocate a pointer array for the data to be transferred. */
   if ( ( ptrs = jMalloc( env, sizeof( char * ) * nel ) ) &&
        ( jValues = jMalloc( env, sizeof( jstring * ) * nel ) ) ) {

      /* Get an array of the Strings from the java array. */
      for ( i = 0; i < nel; i++ ) {
         jValues[ i ] = (*env)->ExceptionOccurred( env )
            ? NULL
            : (jstring) (*env)->GetObjectArrayElement( env, jValueArray, i );
      }

      /* Get an array of pointers to the string values. */
      for ( i = 0; i < nel; i++ ) {
         ptrs[ i ] = (*env)->ExceptionOccurred( env )
                   ? NULL
                   : (*env)->GetStringUTFChars( env, jValues[ i ], NULL );
         if ( ! ptrs[ i ] ) {
            ptrs[ i ] = "\0";
         }
      }

      /* Call the HDS routine. */
      HDSCALL(
         datPutVC( locator, nel, ptrs, status );
      )

      /* Tidy up. */
      ALWAYS(
         for ( i = 0; i < nel; i++ ) {
            if ( jValues[ i ] ) {
               (*env)->ReleaseStringUTFChars( env, jValues[ i ], ptrs[ i ] );
            }
         }
      )
      free( ptrs );
      free( jValues );
   }
}


#define MAKE_DATPUTVX(Xletter,XLetter,Xjtype,XJtype,Xctype) \
JNIEXPORT void JNICALL NATIVE_METHOD( datPutv##Xletter )( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   Xjtype##Array value   /* Array of primitives to write */ \
) { \
   HDSLoc *locator; \
   Xjtype *buffer = NULL; \
   size_t nel; \
 \
   CHECK_TYPES_MATCH(Xjtype,Xctype) \
 \
   /* Get this object's locator. */ \
   locator = getLocator( env, this ); \
 \
   /* Get the number of elements which will be written. */ \
   nel = (*env)->GetArrayLength( env, value ); \
 \
   /* Pin/copy the elements of the primitive array. */ \
   if ( ! (*env)->ExceptionOccurred( env ) && \
        ( buffer = (*env)->Get##XJtype##ArrayElements( env, value, \
                                                       NULL ) ) ) { \
 \
      /* Call the HDS routine. */ \
      HDSCALL( \
         datPutV##XLetter( locator, nel, (Xctype *) buffer, status ); \
      ) \
 \
      /* Release the array elements. */ \
      ALWAYS( \
         (*env)->Release##XJtype##ArrayElements( env, value, \
                                                 buffer, JNI_ABORT ); \
      ) \
   } \
}
MAKE_DATPUTVX(l,L,jboolean,Boolean,int)
MAKE_DATPUTVX(i,I,jint,Int,int)
MAKE_DATPUTVX(r,R,jfloat,Float,float)
MAKE_DATPUTVX(d,D,jdouble,Double,double)
#undef MAKE_DATPUTVX


JNIEXPORT jstring JNICALL NATIVE_METHOD( datRef )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   char ref[ MAXCHARLENG + 1 ];
   HDSLoc *locator;

   locator = getLocator( env, this );
   HDSCALL(
      datRef( locator, ref, MAXCHARLENG, status );
   )

   return (*env)->ExceptionOccurred( env )
        ? NULL
        : (*env)->NewStringUTF( env, ref );
}


JNIEXPORT jlong JNICALL NATIVE_METHOD( datSize )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator;
   size_t size;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datSize( locator, &size, status );
   )

   /* Return the result. */
   return (jlong) size;
}


JNIEXPORT jlongArray JNICALL NATIVE_METHOD( datShape )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   int i;
   HDSLoc *locator;
   int ndim;
   hdsdim dims[ DAT__MXDIM ];

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datShape( locator, DAT__MXDIM, dims, &ndim, status );
   )

   /* Construct and return an array representing the shape. */
   return makeCartesian( env, dims, ndim );
}


JNIEXPORT jboolean JNICALL NATIVE_METHOD( datState )(
   JNIEnv *env,          /* Interfact pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator;
   int state;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datState( locator, &state, status );
   )

   /* Return the result. */
   return (jboolean) ( state ? JNI_TRUE : JNI_FALSE );
}


JNIEXPORT jboolean JNICALL NATIVE_METHOD( datStruc )(
   JNIEnv *env,          /* Interfact pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator;
   int struc;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datStruc( locator, &struc, status );
   )

   /* Return the result. */
   return (jboolean) ( struc ? JNI_TRUE : JNI_FALSE );
}


JNIEXPORT jboolean JNICALL NATIVE_METHOD( datThere )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jName         /* Name of queried component */
) {
   const char *name;
   HDSLoc *locator;
   int there;

   /* Convert java string to C. */
   name = (*env)->GetStringUTFChars( env, jName, NULL );

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datThere( locator, name, &there, status );
   )

   /* Release string copy. */
   ALWAYS(
      (*env)->ReleaseStringUTFChars( env, jName, name );
   )

   /* Return the result. */
   return (jboolean) ( there ? JNI_TRUE : JNI_FALSE );
}


JNIEXPORT jstring JNICALL NATIVE_METHOD( datType )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator;
   char type[ DAT__SZTYP + 1 ];

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datType( locator, type, status );
   )

   /* Construct and return a java string object. */
   return (*env)->ExceptionCheck( env )
        ? NULL
        : (*env)->NewStringUTF( env, type );
}


JNIEXPORT void JNICALL NATIVE_METHOD( datUnmap )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datUnmap( locator, status );
   )
}


JNIEXPORT jboolean JNICALL NATIVE_METHOD( datValid )(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   HDSLoc *locator;
   int valid;

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* If it's null, then it's been annulled.  The answer is no. */
   if ( locator == NULL ) {
      return JNI_FALSE;
   }

   /* Otherwise call datValid. */
   else {
      HDSCALL(
         datValid( locator, &valid, status );
      )

      /* Return the result. */
      return (jboolean) ( valid ? JNI_TRUE : JNI_FALSE );
   }
}


JNIEXPORT jobject JNICALL NATIVE_METHOD( mapBuffer )(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jType,        /* Data type string */
   jstring jMode         /* Access mode string */
) {
   const char *type;
   const char *mode;
   HDSLoc *locator;
   size_t el;
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
   if ( ! strncmp( normtype, "_BYTE", DAT__SZTYP ) ||
        ! strncmp( normtype, "_UBYTE", DAT__SZTYP ) ) {
      siz = HDS_BYTE_SIZE;
   }
   else if ( ! strncmp( normtype, "_WORD", DAT__SZTYP ) ||
             ! strncmp( normtype, "_UWORD", DAT__SZTYP ) ) {
      siz = HDS_WORD_SIZE;
   }
   else if ( ! strncmp( normtype, "_INTEGER", DAT__SZTYP ) ) {
      siz = HDS_INTEGER_SIZE;
   }
   else if ( ! strncmp( normtype, "_REAL", DAT__SZTYP ) ) {
      siz = HDS_REAL_SIZE;
   }
   else if ( ! strncmp( normtype, "_DOUBLE", DAT__SZTYP ) ) {
      siz = HDS_DOUBLE_SIZE;
   }
   else {
      char errbuf[ 80 ];
      sprintf( errbuf, "Unsupported mapping type \"%s\"", normtype );
      siz = 0;
      (*env)->ThrowNew( env, IllegalArgumentExceptionClass, errbuf );
   }

   /* Get this object's locator. */
   locator = getLocator( env, this );

   /* Call the HDS routine to do the work. */
   HDSCALL(
      datMapV( locator, type, mode, &pntr, &el, status );
   )

   /* Release string copies. */
   ALWAYS(
      (*env)->ReleaseStringUTFChars( env, jType, type );
      (*env)->ReleaseStringUTFChars( env, jMode, mode );
   )

   /* Make a ByteBuffer out of the returned memory. */
   bbuf = NULL;
   bufleng = el * siz;
   if ( ! (*env)->ExceptionCheck( env ) && pntr != NULL && bufleng > 0L ) {
      bbuf = (*env)->NewDirectByteBuffer( env, pntr, bufleng );
   }
   return bbuf;
}

/* $Id$ */
