/*
*+
*  Name:
*     Channel.c

*  Purpose:
*     JNI implementations of native methods of Channel class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     18-SEP-2001 (MBT):
*        Original version.
*     4-FEB-2004 (MBT):
*        Added XmlChan support.
*-
*/

/* Header files. */
#include <string.h>
#include <stdio.h>
#include "jni.h"
#include "ast.h"
#include "sae_par.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_Channel.h"


/* Typedefs. */
typedef struct {
   JNIEnv *env;
   jobject object;
   jmethodID sourceMethodID;
   jmethodID sinkMethodID;
} ChanInfo;

typedef AstChannel *(*ChannelForFunc)( const char *(*)( void ),
                                       char *(*)( const char *(*)( void ), int *),
                                       void (*)( const char * ),
                                       void (*)( void (*)( const char * ),
                                                 const char *, int * ),
                                       const char *, ... );


/* Static function prototypes. */
static char *sourceWrap( const char *(* source )( void ), int * );
static void sinkWrap( void (* sink )( const char * ), const char *, int * );
static void initializeIDs( JNIEnv *env );
static void fillChaninfo( JNIEnv *env, jobject this );
static void constructFlavouredChannel( JNIEnv *env, jobject this, 
                                       ChannelForFunc func );
static AstChannel *xmlChanFor( const char *(*)( void ),
                               char *(*)( const char *(*)( void ), int * ),
                               void (*)( const char * ),
                               void (*)( void (*)( const char * ),
                                         const char *, int * ),
                               const char *, ... );


/* Static variables. */
static jclass NeedsChannelizingClass;
static jfieldID ChaninfoFieldID;
static jmethodID ChannelizeMethodID;
static jmethodID UnChannelizeMethodID;


/* Class methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Channel_nativeInitializeChannel(
   JNIEnv *env,          /* Interface pointer */
   jclass class          /* Class object */
) {
   initializeIDs( env );
}


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Channel_constructChannel(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   constructFlavouredChannel( env, this, astChannelFor );
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Channel_constructXmlChan(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   constructFlavouredChannel( env, this, xmlChanFor );
}
   

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Channel_destroy(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer infopointer;
   ChanInfo *chaninfo;

   /* Free the extra memory allocated by the construct native method,
    * unless it has already been done for some reason.
    * We do not need to annul the AST object itself here, since this is 
    * handled by the finalizer of the superclass (AstObject). */
   infopointer.jlong = (*env)->GetLongField( env, this, ChaninfoFieldID );
   if ( infopointer.ptr != NULL ) {
      chaninfo = (ChanInfo *) infopointer.ptr;
      free( chaninfo );
      infopointer.ptr = NULL;
      (*env)->SetLongField( env, this, ChaninfoFieldID, infopointer.jlong );
   }
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_Channel_read(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstObject *readObj;
   jobject jReadObj = NULL;
   int needchan;

   /* Store necessary pointers in the chaninfo structure, where
    * sourceWrap will be able to find them.  SourceWrap() is about
    * to be invoked by astRead(). */
   fillChaninfo( env, this );

   /* Call the AST function to do the work.  This will in turn invoke
    * sourceWrap() which will invoke the (non-native) source instance 
    * method. */
   THASTCALL( jniastList( 1, pointer.AstObject ),
      readObj = astRead( pointer.Channel );
   )

   /* Make a java object out of the AST object. */
   if ( ! (*env)->ExceptionCheck( env ) ) {
      jReadObj = jniastMakeObject( env, readObj );
   }

   /* Do any further necessary object-specific initialization. */
   if ( jReadObj ) {
      needchan = ( (*env)->IsInstanceOf( env, jReadObj, NeedsChannelizingClass )
                   == JNI_TRUE );
      if ( needchan ) {
         (*env)->CallVoidMethod( env, jReadObj, UnChannelizeMethodID );
      }
   }
   return jReadObj;
}


JNIEXPORT jint JNICALL Java_uk_ac_starlink_ast_Channel_write(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject item          /* AstObject to write */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer itempointer;
   int needchan;
   int nwrite = 0;

   if ( jniastCheckNotNull( env, item ) ) {

      /* Get the AST object. */
      itempointer = jniastGetPointerField( env, item );

      /* See if the object requires preparation for writing to the channel. */
      needchan = ( (*env)->IsInstanceOf( env, item, NeedsChannelizingClass )
                   == JNI_TRUE );

      /* Do anything necessary to the object before it is written. */
      if ( needchan && ! (*env)->ExceptionCheck( env ) ) {

         /* Invoke channelize. */
         (*env)->CallVoidMethod( env, item, ChannelizeMethodID );
      }

      /* Store necessary pointers in the chaninfo structure, where
       * sinkWrap() will be able to find them.  SinkWrap() is about
       * to get invoked by astWrite(). */
      fillChaninfo( env, this );

      /* Call the AST function to do the work.  This will in turn invoke 
       * sinkWrap() which will invoke the (non-native) sink instance method. */
      THASTCALL( jniastList( 2, pointer.AstObject, itempointer.AstObject ),
         nwrite = astWrite( pointer.Channel, itempointer.AstObject );
      )

      /* Reverse possible destructive effects of Channelize. */
      if ( needchan && ! (*env)->ExceptionCheck( env ) ) {
         (*env)->CallVoidMethod( env, item, UnChannelizeMethodID );
      }

      /* Return number of items written. */
      return (jint) nwrite;
   }
   return 0;
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_Channel_warnings(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstKeyMap *warnings;

   THASTCALL( jniastList( 1, pointer.AstObject ),
      warnings = astWarnings( pointer.Channel );
   )
   return jniastMakeObject( env, (AstObject *) warnings );
}


/* Static functions. */

static void constructFlavouredChannel( JNIEnv *env, jobject this, 
                                       ChannelForFunc func ) {
   AstPointer pointer;
   AstPointer infopointer;
   ChanInfo *chaninfo;

   /* Allocate space for the chaninfo structure and store its location in
    * the chaninfo instance variable of this object.  The structure will
    * be used to hold pointers required by the sourceWrap and sinkWrap
    * routines. */
   chaninfo = jniastMalloc( env, sizeof( ChanInfo ) );
   infopointer.ptr = chaninfo;
   if ( ! (*env)->ExceptionCheck( env ) ) {
      (*env)->SetLongField( env, this, ChaninfoFieldID, infopointer.jlong );
   }
   
   /* Construct the AST Channel itself, using references to the chaninfo
    * structure.  We are subverting the AST machinery here in order to 
    * permit an implementation which will allow multiple simultaneously
    * active channels. */
   ASTCALL(
      pointer.Channel =
         func( (const char *(*)()) chaninfo, sourceWrap,
               (void (*)( const char * )) chaninfo, sinkWrap, "" );
   )

   /* Store the pointer to the AST object in an instance variable. */
   if ( ! (*env)->ExceptionCheck( env ) ) {
      jniastInitObject( env, this, pointer );
   }
}

/*
*+
*  Name:
*     xmlChanFor

*  Purpose:
*     Retyped wrapper round astXmlChanFor

*  Description:
*     This function is a wrapper around astXmlChanFor which exists solely 
*     to cast its return type from AstXmlChan* to AstChannel* - the other 
*     arguments are just passed through with no change (except for the 
*     varargs, which are never used).
*
*     This prevents the compiler from issuing a warning.  One could equally
*     just cast astXmlChanFor to ChannelForFunc when it is used, but
*     doing it like this makes it more explicit what is going on.
*-
*/
static AstChannel *xmlChanFor( const char *(* source)( void ),
                               char *(* source_wrap)( const char *(*)( void ), int * ),
                               void (* sink)( const char * ),
                               void (* sink_wrap)( void (*)( const char * ),
                                                   const char *, int * ),
                               const char *options, ... ) {
    return (AstChannel *) astXmlChanFor( source, source_wrap, sink, sink_wrap,
                                         options );
}

static void initializeIDs( JNIEnv *env ) {
/*
*+
*  Name:
*     intializeIDs

*  Purpose:
*     Initialize static field and method ID variables specific to Channel.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI environment.
*-
*/
   static jclass ChannelClass = NULL;

   /* Don't bother if we have done this before. */
   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Get global references to classes. */
      ( ChannelClass = (jclass) (*env)->NewGlobalRef( env,
           (*env)->FindClass( env, PACKAGE_PATH "Channel" ) ) ) &&
      ( NeedsChannelizingClass = (jclass) (*env)->NewGlobalRef( env,
           (*env)->FindClass( env, PACKAGE_PATH "NeedsChannelizing" ) ) ) &&

      /* Get field IDs. */
      ( ChaninfoFieldID = (*env)->GetFieldID( env, ChannelClass, 
                                              "chaninfo", "J" ) ) &&

      /* Get method IDs. */
      ( ChannelizeMethodID = (*env)->GetMethodID( env, NeedsChannelizingClass,
                                                  "channelize", "()V" ) ) &&
      ( UnChannelizeMethodID = (*env)->GetMethodID( env, NeedsChannelizingClass,
                                                    "unChannelize", "()V" ) ) &&
      1;
   }
}


static void fillChaninfo( JNIEnv *env, jobject this ) {
/*
*+
*  Name:
*     fillChaninfo

*  Purpose:
*     Store necessary pointers in the chaninfo structure.

*  Description:
*     This function ensures that the chaninfo structure, which can be
*     located from within the sourceWrap and sinkWrap functions, contains
*     the pointers necessary to invoke the object's source and sink methods.
*     It should be called before AST is going to do something which
*     will may result in sourceWrap or sinkWrap being called, i.e.
*     Channel astRead() or astWrite() calls.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI environment
*     this = jobject
*        The Channel object whose source/sink methods may be invoked.
*-
*/
   ChanInfo *chaninfo;
   AstPointer infopointer;
   jclass clazz;

   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Get the class of this object. */
      clazz = (*env)->GetObjectClass( env, this );

      /* Get the location of the chaninfo structure. */
      infopointer.jlong = (*env)->GetLongField( env, this, ChaninfoFieldID );
      chaninfo = (ChanInfo *) infopointer.ptr;

      /* Fill the chaninfo structure with the required pointers. */
      chaninfo->env = env;
      chaninfo->object = this;
      chaninfo->sourceMethodID = (*env)->GetMethodID( env, clazz, "source",
                                                      "()Ljava/lang/String;" );

      chaninfo->sinkMethodID = (*env)->GetMethodID( env, clazz, "sink",
                                                    "(Ljava/lang/String;)V" );
   }
}

static char *sourceWrap( const char *(* source)( void ), int *status ) {
/*
*+
*  Name:
*     sourceWrap

*  Purpose:
*     Invoke the Channel's source() method and pass the result to AST.

*  Description:
*     This function is called by the AST library from within astRead().
*     It has to call the source() method of the corresponding java
*     Channel, and return the resulting string.

*  Parameters:
*     source = const char *(*)()
*        A pointer to the ChanInfo structure which contains information
*        about the environment and object for which this function is
*        called.  It has to be cast to the weird form above to fool AST.
*     status
*        Pointer to the inherited status variable.

*  Return value:
*     A new dynamically allocated (using astMalloc()) buffer holding
*     a char* representation of the String returned by the Channel's
*     source() method.

*  Notes:
*     There are a couple of subtleties here.  First, astRead is contracted
*     to call sourceWrap() without any useful extra information, but within
*     JNI it is impossible to do anything without having the env and
*     this pointers.  We therefore subvert the AST foreign calling
*     conventions, sneaking a pointer into the space where a reference
*     to the source() function itself (unused in the C implementation)
*     is supposed to go.  This allows us to access env and this from here.
*
*     Secondly, the sourceWrap method is contracted to return the
*     value in dynamically allocated memory, allocated using AST's
*     memory allocation functions, so we ensure that this is done.
*-
*/
   ChanInfo *chaninfo = (ChanInfo *) source;
   jstring jLine;
   const char *line;
   char *retval;
   JNIEnv *env;
   jobject this;
   jmethodID sourceMethodID;

   /* Return directly if AST status is set. */
   if ( !astOK ) return NULL;

   /* Initialise return value. */
   retval = NULL;

   /* Get the information from the chaninfo structure. */
   env = chaninfo->env;
   this = chaninfo->object;
   sourceMethodID = chaninfo->sourceMethodID;
   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Invoke this Channel's source method to get the line as a String. */
      jLine = (jstring) (*env)->CallObjectMethod( env, this, sourceMethodID );

      /* Signal to AST if an exception has occurred. */
      if ( (*env)->ExceptionCheck( env ) ) {
         astSetStatus( SAI__ERROR );
      }

      /* If the source method completed successfully, turn the String into
       * an array of chars and copy it into dynamic memory. */
      else if ( jLine != NULL ) {
         line = jniastGetUTF( env, jLine );
         retval = astMalloc( strlen( line ) + 1 );
         strcpy( retval, line );
         jniastReleaseUTF( env, jLine, line );
      }
   }

   /* Return the successfully or unsuccessfully obtained buffer. */
   return retval;
}

static void sinkWrap( void (* sink)( const char * ), const char *line, 
                      int *status ) {
/*
*+
*  Name:
*    sinkWrap

*  Purpose:
*     Invoke the Channel's sink(String) method on behalf of AST.

*  Description:
*     This function is called by the AST library from within astWrite().
*     It has to call the sink(String) method of the corresponding java
*     Channel.

*  Parameters:
*     sink = void (*)( const char * )
*        A pointer to the ChanInfo structure which contains information
*        about the environment and object for which this function is
*        called.  It has to be cast to the weird form above to fool AST.
*     status
*        Pointer to the inherited status.

*  Notes:
*     There is a subtlety here.  astWrite is contracted to call sinkWrap()
*     without any useful extra information, but within JNI it is
*     impossible to do anything without having the env and this pointers.
*     We therefore subvert the AST foreign calling conventions, sneaking
*     a pointer into the space where a reference to the sink() function
*     itself (unused in the C implementation) is supposed to go.
*     This allows us to access env and this from here.
*-
*/
   ChanInfo *chaninfo = (ChanInfo *) sink;
   JNIEnv *env;
   jobject this;
   jmethodID sinkMethodID;
   jstring jLine;

   /* Return directly if AST status is set. */
   if ( !astOK ) return;

   /* Get required pointers from the chaninfo structure */
   env = chaninfo->env;
   this = chaninfo->object;
   sinkMethodID = chaninfo->sinkMethodID;
   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Make a java string from the supplied argument. */
      jLine = (*env)->NewStringUTF( env, line );

      /* Pass it to the sink method of this object. */
      if ( jLine != NULL ) {
         (*env)->CallVoidMethod( env, this, sinkMethodID, jLine );
      }
   }
   if ( (*env)->ExceptionCheck( env ) ) {
      astSetStatus( SAI__ERROR );
   }
}

/* $Id$ */
