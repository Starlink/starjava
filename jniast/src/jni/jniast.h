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
*     DSB: David Berry (JACH)

*  History:
*     18-SEP-2001 (MBT):
*        Original version.
*     05-JAN-2011 (DSB):
*        Fix memory leak caused by broken THASTLOCK macro.
*-
*/

#ifndef JNIAST_DEFINED
#define JNIAST_DEFINED

/*  MINGW doesn't have native pthreads, so use bd equivalents. */
#if __MINGW32__
#define HAVE_PTHREADS 0
#else
#define HAVE_PTHREADS 1
#endif

/* Include files. */
#include <stdlib.h>
#include <float.h>
#if HAVE_PTHREADS
#include <pthread.h>
#else
#include "bdpthread.h"
#endif
#include "ast.h"
#include "jni.h"
#include "err_jniast.h"


/* Constants. */
#define PACKAGE_PATH "uk/ac/starlink/ast/"

/* Required versions of the AST package. */
#define JNIAST_MAJOR_VERS 5
#define JNIAST_MINOR_VERS 1
#define JNIAST_RELEASE 0

/* Typedefs. */
typedef union {
   void *ptr;                /* Generic pointer */
   jlong jlong;              /* Java value as in 'private long pointer' */
   AstBox *Box;              /* Pointer to C Box struct */
   AstChannel *Channel;      /* Pointer to C AstChannel struct */
   AstCircle *Circle;        /* Pointer to C AstCircle struct */
   AstCmpFrame *CmpFrame;    /* Pointer to C AstCmpFrame struct */
   AstCmpMap *CmpMap;        /* Pointer to C AstCmpMap struct */
   AstCmpRegion *CmpRegion;  /* Pointer to C AstCmpRegion struct */
   AstDSBSpecFrame *DSBSpecFrame; /* Pointer to C AstDSBSpecFrame struct */
   AstEllipse *Ellipse;      /* Pointer to C AstEllipse struct */
   AstFitsChan *FitsChan;    /* Pointer to C AstFitsChan struct */
   AstFluxFrame *FluxFrame;  /* Pointer to C FluxFrame struct */
   AstFrame *Frame;          /* Pointer to C AstFrame struct */
   AstFrameSet *FrameSet;    /* Pointer to C AstFrameSet struct */
   AstGrismMap *GrismMap;    /* Pointer to C AstGrismMap struct */
   AstIntraMap *IntraMap;    /* Pointer to C AstIntraMap struct */
   AstInterval *Interval;    /* Pointer to C AstInterval struct */
   AstKeyMap *KeyMap;        /* Pointer to C AstKeyMap struct */
   AstLutMap *LutMap;        /* Pointer to C AstLutMap struct */
   AstMapping *Mapping;      /* Pointer to C AstMapping struct */
   AstMathMap *MathMap;      /* Pointer to C AstMathMap struct */
   AstMatrixMap *MatrixMap;  /* Pointer to C AstMatrixMap struct */
   AstNullRegion *NullRegion;/* Pointer to C AstNullRegion struct */
   AstObject *AstObject;     /* Pointer to C AstObject struct */
   AstPcdMap *PcdMap;        /* Pointer to C AstPcdMap struct */
   AstPermMap *PermMap;      /* Pointer to C AstPermMap struct */
   AstPlot *Plot;            /* Pointer to C AstPlot struct */
   AstPolygon *Polygon;      /* Pointer to C AstPolygon struct */
   AstPolyMap *PolyMap;      /* Pointer to C AstPolyMap struct */
   AstPointList *PointList;  /* Pointer to C AstPointList struct */
   AstPrism *Prism;          /* Pointer to C AstPrism struct */
   AstRateMap *RateMap;      /* Pointer to C AstRateMap struct */
   AstRegion *Region;        /* Pointer to C AstRegion struct */
   AstSelectorMap *SelectorMap; /* Pointer to C AstSelectorMap struct */
   AstShiftMap *ShiftMap;    /* Pointer to C AstShiftMap struct */
   AstSkyFrame *SkyFrame;    /* Pointer to C AstSkyFrame struct */
   AstSlaMap *SlaMap;        /* Pointer to C AstSlaMap struct */
   AstSpecFluxFrame *SpecFluxFrame; /* Pointer to C AstSpecFluxFrame struct */
   AstSpecFrame *SpecFrame;  /* Pointer to C AstSpecFrame struct */
   AstSpecMap *SpecMap;      /* Pointer to C AstSpecMap struct */
   AstSphMap *SphMap;        /* Pointer to C AstSphMap struct */
   AstStc *Stc;              /* Pointer to C AstStc struct */
   AstStcCatalogEntryLocation *StcCatalogEntryLocation; /* Pointer to C AstStcCatalogEntryLocation struct */
   AstStcObsDataLocation *StcObsDataLocation; /* Pointer to C AstStcObsDataLocation struct */
   AstStcResourceProfile *StcResourceProfile; /* Pointer to C AstStcResourceProfile struct */
   AstStcSearchLocation *StcSearchLocation; /* Pointer to C AstStcSearchLocation struct */
   AstSwitchMap *SwitchMap;  /* Pointer to C AstSwitchMap struct */
   AstTimeFrame *TimeFrame;  /* Pointer to C AstTimeFrame struct */
   AstTimeMap *TimeMap;      /* Pointer to C AstTimeMap struct */
   AstTranMap *TranMap;      /* Pointer to C AstTranMap struct */
   AstUnitMap *UnitMap;      /* Pointer to C AstUnitMap struct */
   AstWcsMap *WcsMap;        /* Pointer to C AstWcsMap struct */
   AstWinMap *WinMap;        /* Pointer to C AstWinMap struct */
   AstZoomMap *ZoomMap;      /* Pointer to C AstZoomMap struct */
} AstPointer;

typedef AstStc *(*StcConstructor)( AstRegion *, int, AstKeyMap*[], const char *, ... );

/* External variables. */
jobject AstLock;
jobject GrfLock;
jclass AstExceptionClass;
jclass AstObjectClass;
jclass DoubleClass;
jclass DoubleArrayClass;
jclass ErrorClass;
jclass IntegerClass;
jclass MappingClass;
jclass StringClass;
jclass UnsupportedOperationExceptionClass;
jmethodID ObjectHashCodeMethodID;
jmethodID ObjectToStringMethodID;
jmethodID AstExceptionConstructorID;
jmethodID ClassGetNameMethodID;
jmethodID ErrorConstructorID;
jmethodID DoubleConstructorID;
jmethodID IntegerConstructorID;


/* Utility function prototypes. */
void jniastInitialize( JNIEnv *env );
void jniastThrowError( JNIEnv *env, const char *fmt, ... );
void jniastThrowIllegalArgumentException( JNIEnv *env, const char *fmt, ... );
void jniastThrowException( JNIEnv *env, const char *fmt, ... );
void jniastClearErrMsg();
const char *jniastGetErrMsg();
AstPointer jniastGetPointerField( JNIEnv *env, jobject object );
void jniastInitObject( JNIEnv *env, jobject object, AstPointer pointer );
void jniastClearObject( JNIEnv *env, jobject object );
void jniastLock( AstObject **ast_objs );
void jniastUnlock( AstObject **ast_objs );
AstObject **jniastList( int count, ... );
int jniastPthreadKeyCreate( JNIEnv *env, pthread_key_t *key,
                            void (*destructor)( void * ) );
int jniastPthreadSetSpecific( JNIEnv *env, pthread_key_t key, void *value );
int jniastCheckArrayLength( JNIEnv *env, jarray jArray, int minel );
jobject jniastCheckNotNull( JNIEnv *env, jobject jObject );
jobject jniastMakeObject( JNIEnv *env, AstObject *objptr );
double *jniastCopyDoubleArray( JNIEnv *env, jdoubleArray jArr, int bsiz );
int jniastGetNaxes( JNIEnv *env, AstFrame *frame );
char *jniastEscapePercents( JNIEnv *env, const char *buf );
void *jniastMalloc( JNIEnv *env, size_t size );
void jniastConstructStc( JNIEnv *env, jobject this, jobject jRegion,
                         jobjectArray jCoords, StcConstructor constructor );
void jniastTrace( JNIEnv *env, jobject obj );

JNIEnv *jniastGetEnv();

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
 * Define whether multithreading will be used in invoking the AST library.
 * This is currently set false, which means there is a single per-AST
 * mutex, so that no AST call is in progress at the same time as any
 * other AST call.
 *
 * It is somewhat disappointing that this has to be the case, and it
 * means that some, but not most, of the JNIAST threading machinery
 * is doing no useful work.  It's like this.  Although AST's
 * design means that no AstObject may be accessed simultaneously
 * by multiple threads, it should in principle be possible for AST
 * calls using disjoint objects to execute simultaneously.
 * However, this can in practice lead to deadlock.  Although care is
 * taken within JNIAST to acquire locks (call astLock() on required
 * objects) in a defined order, which ought to be enough to prevent deadlock
 * (see Resource Hierarchy solution to Dining Philosophers problem),
 * there is a problem: astLock() itself does not simply acquire a single
 * lock, but potentially many, since it locks not only the requested
 * object but any object which that contains.  Since the order in which
 * it does this is not congruent with the order used by JNIAST,
 * deadlock is no longer prevented.  The only fix I can think of for
 * this would be an AST call which takes an array of objects to lock,
 * and acquires locks in some well-defined order.  If that ever comes
 * about, the following JNIAST_THREADS def should be changed to
 * AST__THREADSAFE instead.
 */

#if __MINGW32__

/* PWD: under MINGW, which is used to build the Windows DLL there is no native
 * pthread implementation so we must build AST without thread support and
 * use the bdthread implementation. */
#define JNIAST_THREADS 0

#else

/*  Back to usual defines as noted above. */
#define JNIAST_THREADS 0

#endif


/*
 * Macro for calling a code block which uses AST-like conventions for
 * status management.  To use it, simply place any code which contains
 * calls to functions in the AST C library as a block of code as the
 * argument of the ASTCALL macro.
 *
 * If the AST calls result in non-zero status (i.e. !astOK) then an
 * AstException is thrown giving the error message generated by the call
 * which failed.
 *
 * If the JNIAST_THREADS macro is false (0), then a single mutex controls
 * every invocation of this macro, which means that only one call of AST
 * code can every be under way at one time.  If JNIAST_THREADS is true,
 * no such restriction is observed, since the AST library and the rest of
 * the JNIAST code ought to be able to cope with multiple concurrent
 * accesses.
 *
 * The macro takes no action if an exception is pending when it is called.
 */
#define ASTCALL(code) \
   if ( ! (*env)->ExceptionCheck( env ) ) { \
      jthrowable throwable = NULL; \
      int status_val = 0; \
      int *status = &status_val; \
      int *old_status; \
      if ( \
           JNIAST_THREADS || \
           (*env)->MonitorEnter( env, AstLock ) == 0 ) { \
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
         if ( \
              (! JNIAST_THREADS) && \
              (*env)->MonitorExit( env, AstLock ) != 0 ) { \
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
 * Macro for calling a code block which acquires AST thread locks on a
 * given list of objects before the block and releases them after it.
 *
 * The ast_objs parameter must be a malloc()'d, NULL-terminated list of
 * AstPointer pointers (i.e. it must be AstObject **).  It must point to
 * a list of any AstObjects which may be used (hence must be astLock()'d)
 * by the code block.  The list may be modified, and will be free()'d
 * on exit, by this macro.  The return value of the jniastList() function
 * provides a suitable value for ast_objs.  As a special case, ast_objs
 * may be NULL.
 *
 * This macro uses AST error-reporting conventions - from a JNI context
 * it should be used within an ASTCALL block.
 */
#define THASTLOCK(ast_objs, code) { \
   AstObject **thastlock_objlist = (ast_objs); \
   jniastLock(thastlock_objlist); \
   code \
   jniastUnlock(thastlock_objlist); \
   free(thastlock_objlist); \
}

/*
 * Macro for calling a code block which uses AST-like conventions for
 * status management, and which acquires AST thread locks on a given
 * list of objects before the block and releases them after it.
 *
 * The ast_objs parameter must be a malloc()'d, NULL-terminated list of
 * AstPointer pointers (i.e. it must be AstObject **).  It must point to
 * a list of any AstObjects which may be used (hence must be astLock()'d)
 * by the code block.  The list may be modified, and will be free()'d
 * on exit, by this macro.  The return value of the jniastList() function
 * provides a suitable value for ast_objs.  As a special case, ast_objs
 * may be NULL.
 */
#define THASTCALL(ast_objs, code) \
   ASTCALL( THASTLOCK(ast_objs, code) )

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

/*
 * Macro for ensuring that two types are the same length.  Some of the
 * JNI code relies on equivalence between a type on the java side and
 * a type on the C side, for instance that the jdouble and double types
 * are the same thing.  The ENSURE_SAME_TYPE macro bails out with an
 * UnsupportedOperationException in the case that the two specified
 * types don't match.
 *
 * Using this macro as protection will have the effect that the JVM
 * shouldn't crash when such type mismatches occur, but of course the
 * operation will still fail with a runtime error.  The correct way
 * around this is some sort of config magic at build time.
 */
#define jniastCheckSameType(Xenv,Xtype1,Xtype2) ( \
   ( sizeof( Xtype1 ) == sizeof( Xtype2 ) ) \
        ? ( 1 ) \
        : ( (*Xenv)->ThrowNew( Xenv, UnsupportedOperationExceptionClass, \
                               "Sorry, unsupported on this architecture (" \
                               #Xtype1 " != " #Xtype2 ")" ) \
          & 0 ) \
)
#define ENSURE_SAME_TYPE(Xtype1,Xtype2) { \
   if ( ! jniastCheckSameType( env, Xtype1, Xtype2 ) ) return; \
}

#endif  /* JNIAST_DEFINED */

/* $Id$ */
