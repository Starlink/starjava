/*
*+
*  Name:
*     jniast.h

*  Purpose:
*     Utility macros and declarations for JNI code used with the AST library.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     18-SEP-2001 (MBT):
*        Original version.
*-
*/

#ifndef JNIAST_DEFINED
#define JNIAST_DEFINED


/* Include files. */
#include <stdlib.h>
#include "ast.h"
#include "jni.h"
#include "err_jniast.h"


/* Constants. */
#define PACKAGE_PATH "uk/ac/starlink/ast/"


/* Typedefs. */
typedef union {
   void *ptr;                /* Generic pointer */
   jlong jlong;              /* Java value as in 'private long pointer' */
   AstChannel *Channel;      /* Pointer to C AstChannel struct */
   AstFitsChan *FitsChan;    /* Pointer to C AstFitsChan struct */
   AstObject *AstObject;     /* Pointer to C AstObject struct */
   AstMapping *Mapping;      /* Pointer to C AstMapping struct */
   AstFrame *Frame;          /* Pointer to C AstFrame struct */
   AstFrameSet *FrameSet;    /* Pointer to C AstFrameSet struct */
   AstCmpFrame *CmpFrame;    /* Pointer to C AstCmpFrame struct */
   AstSkyFrame *SkyFrame;    /* Pointer to C AstSkyFrame struct */
   AstCmpMap *CmpMap;        /* Pointer to C AstCmpMap struct */
   AstIntraMap *IntraMap;    /* Pointer to C AstIntraMap struct */
   AstLutMap *LutMap;        /* Pointer to C AstLutMap struct */
   AstMathMap *MathMap;      /* Pointer to C AstMathMap struct */
   AstMatrixMap *MatrixMap;  /* Pointer to C AstMatrixMap struct */
   AstPcdMap *PcdMap;        /* Pointer to C AstPcdMap struct */
   AstPermMap *PermMap;      /* Pointer to C AstPermMap struct */
   AstSlaMap *SlaMap;        /* Pointer to C AstSlaMap struct */
   AstSphMap *SphMap;        /* Pointer to C AstSphMap struct */
   AstUnitMap *UnitMap;      /* Pointer to C AstUnitMap struct */
   AstWcsMap *WcsMap;        /* Pointer to C AstWcsMap struct */
   AstWinMap *WinMap;        /* Pointer to C AstWinMap struct */
   AstZoomMap *ZoomMap;      /* Pointer to C AstZoomMap struct */
   AstPlot *Plot;            /* Pointer to C AstPlot struct */
} AstPointer;


/* External variables. */
jobject AstLock;
jclass AstExceptionClass;
jclass DoubleArrayClass;
jclass ErrorClass;
jclass MappingClass;
jmethodID ObjectHashCodeMethodID;
jmethodID ObjectToStringMethodID;
jmethodID AstExceptionConstructorID;
jmethodID ClassGetNameMethodID;
jmethodID ErrorConstructorID;


/* Utility function prototypes. */
void jniastInitialize( JNIEnv *env );
void jniastThrowError( JNIEnv *env, const char *fmt, ... );
void jniastThrowException( JNIEnv *env, const char *fmt, ... );
void jniastClearErrMsg();
const char *jniastGetErrMsg();
AstPointer jniastGetPointerField( JNIEnv *env, jobject object );
void jniastSetPointerField( JNIEnv *env, jobject object, AstPointer pointer );
int jniastCheckArrayLength( JNIEnv *env, jarray jArray, int minel );
jobject jniastCheckNotNull( JNIEnv *env, jobject jObject );
jobject jniastMakeObject( JNIEnv *env, AstObject *objptr );
int jniastGetNaxes( JNIEnv *env, AstFrame *frame );
void *jniastMalloc( JNIEnv *env, size_t size );
void jniastTrace( JNIEnv *env, jobject obj );


/*
 * Convenience macros for getting and releasing UTF strings. 
 */
#define jniastGetUTF( env, jString ) \
   ( ( ! (*env)->ExceptionCheck( env ) ) \
       ? (*env)->GetStringUTFChars( env, jString, NULL ) \
       : NULL )
#define jniastReleaseUTF( env, jString, string ) \
   if ( jString != NULL && string != NULL ) \
      ALWAYS( (*env)->ReleaseStringUTFChars( env, jString, string ); )

/*
 * Macro for calling a code block which uses AST-like conventions for
 * status management.  To use it, simply place any code which contains
 * calls to functions in the AST C library as a block of code as the
 * argument of the ASTCALL macro.
 *
 * The macro ensures that only one such block is being executed at any
 * one time - if this were not the case, the error reporting could get
 * confused.  If the AST calls result in non-zero status (i.e. !astOK)
 * then an AstException is thrown giving the error message generated
 * by the call which failed.
 *
 * The macro takes no action if an exception is pending when it is called.
 */
#define ASTCALL(code) \
   if ( ! (*env)->ExceptionCheck( env ) ) { \
      jthrowable throwable = NULL; \
      int status_val = 0; \
      int *status = &status_val; \
      int *old_status; \
      if ( (*env)->MonitorEnter( env, AstLock ) == 0 ) { \
         jniastClearErrMsg(); \
         old_status = astWatch( status ); \
         code \
         astWatch( old_status ); \
         if ( *status != 0 ) { \
            jstring errmsg = (*env)->NewStringUTF( env, jniastGetErrMsg() ); \
            throwable = (*env)->NewObject( env, AstExceptionClass, \
                                           AstExceptionConstructorID, \
                                           errmsg, *status ); \
         } \
         if ( (*env)->MonitorExit( env, AstLock ) != 0 ) { \
            throwable = (*env)->NewObject( env, ErrorClass, \
                                           ErrorConstructorID, \
               (*env)->NewStringUTF( env, "jniast: " \
                                          "unexpected MonitorExit return" ) ); \
         } \
      } \
      else { \
         throwable = (*env)->NewObject( env, ErrorClass, \
                                        ErrorConstructorID, \
            (*env)->NewStringUTF( env, "jniast: " \
                                       "unexpected MonitorEnter return" ) ); \
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
   jthrowable _jniast_except = (*env)->ExceptionOccurred( env ); \
   if ( _jniast_except != NULL ) { \
      (*env)->ExceptionClear( env ); \
   } \
   code \
   if ( _jniast_except != NULL ) { \
      (*env)->Throw( env, _jniast_except ); \
   } \
}

#endif  /* JNIAST_DEFINED */

/* $Id$ */
