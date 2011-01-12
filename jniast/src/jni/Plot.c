/*
*+
*  Name:
*     Plot.c

*  Purpose:
*     JNI implementations of native mathods of Plot class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     27-SEP-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "grf.h"
#include "jniast.h"
#if HAVE_PTHREADS
#include <pthread.h>
#else
#include "bdpthread.h"
#endif
#include "uk_ac_starlink_ast_Plot.h"


/* Static variables. */
static jclass Rectangle2DFloatClass;
static jmethodID GrfAttrMethodID;
static jmethodID GrfCapMethodID;
static jmethodID GrfFlushMethodID;
static jmethodID GrfLineMethodID;
static jmethodID GrfMarkMethodID;
static jmethodID GrfScalesMethodID;
static jmethodID GrfTextMethodID;
static jmethodID GrfQchMethodID;
static jmethodID GrfTxExtMethodID;
static jmethodID Rectangle2DFloatConstructorID;
static jfieldID PlotGrfFieldID;
static pthread_key_t grf_key;


/* Static function prototypes. */
static void initializeIDs( JNIEnv *env );


/* Macros. */

/* This macro loads a thread-specific structure with the current Grf
 * object for this plot so that AST Grf callbacks can access it.
 * Like (TH)ASTCALL, it cannot be used in a re-entrant fashion. */
#define THPLOTCALL( ast_objs, code )                                    \
{                                                                       \
    jobject _plotcall_grf;                                              \
    if ( ( _plotcall_grf = jniastCheckNotNull( env,                     \
            (*env)->GetObjectField( env, this, PlotGrfFieldID ) ) ) ) { \
        if ( JNIAST_THREADS ||                                          \
             (*env)->MonitorEnter( env, GrfLock ) == 0 ) {              \
            jniastPthreadSetSpecific( env, grf_key, _plotcall_grf );    \
            THASTCALL( ast_objs,                                        \
                       code                                             \
                       )                                                \
                jniastPthreadSetSpecific( env, grf_key, NULL );         \
        }                                                               \
        if ( ! JNIAST_THREADS ) {                                       \
            (*env)->MonitorExit( env, GrfLock );                        \
        }                                                               \
    }                                                                   \
}

/* This macro returns the value of the Grf object currently being used by
 * this thread, and hence by the Plot object being used in this thread
 * (as set up by the THPLOTCALL macro). */
#define current_grf() ( (jobject) pthread_getspecific( grf_key ) )


/* Class methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_nativeInitializePlot(
   JNIEnv *env,          /* Interface pointer */
   jclass class          /* Class object */
) {
   initializeIDs( env );
}


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject frame,        /* AST Frame from which to construct the Plot */
   jfloatArray jGraphbox,/* Corners of plotting area */
   jdoubleArray jBasebox /* Corners of plotting area in frame coordinates */
) {
   AstPointer pointer;
   AstPointer frmpointer;
   const float *graphbox = NULL;
   const double *basebox = NULL;

   ENSURE_SAME_TYPE(float,jfloat)
   ENSURE_SAME_TYPE(double,jdouble)

   if ( jniastCheckNotNull( env, frame ) &&
        jniastCheckArrayLength( env, jGraphbox, 4 ) &&
        jniastCheckArrayLength( env, jBasebox, 4 ) ) {

      /* Get C data from java data. */
      frmpointer = jniastGetPointerField( env, frame );
      graphbox = (const float *)
                 (*env)->GetFloatArrayElements( env, jGraphbox, NULL );
      basebox = (const double *)
                (*env)->GetDoubleArrayElements( env, jBasebox, NULL );

      /* Call the AST Plot constructor function. */
      THASTCALL( jniastList( 1, frmpointer.AstObject ),
         pointer.Plot = astPlot( frmpointer.Frame, graphbox, basebox, "" );
      )

      /* Release resources. */
      ALWAYS(
         if ( graphbox ) {
            (*env)->ReleaseFloatArrayElements( env, jGraphbox,
                                               (jfloat *) graphbox,
                                               JNI_ABORT );
         }
         if ( basebox ) {
            (*env)->ReleaseDoubleArrayElements( env, jBasebox,
                                                (jdouble *) basebox,
                                                JNI_ABORT );
         }
      )

      /* Set the pointer field to hold the AST pointer for this object. */
      jniastInitObject( env, this, pointer );
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_border(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );

   THPLOTCALL( jniastList( 1, pointer.AstObject ),
      astBorder( pointer.Plot );
   )
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_Plot_boundingBox(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   float lbnd[ 2 ];
   float ubnd[ 2 ];
   float x;
   float y;
   float w;
   float h;

   /* Get the bounding box from AST. */
   THPLOTCALL( jniastList( 1, pointer.AstObject ),
      astBoundingBox( pointer.Plot, lbnd, ubnd );
   )

   if ( ! (*env)->ExceptionCheck( env ) ) {

       /* Construct a Rectangle2D object which packages the result. */
       x = lbnd[ 0 ];
       y = lbnd[ 1 ];
       w = ubnd[ 0 ] - lbnd[ 0 ];
       h = ubnd[ 1 ] - lbnd[ 1 ];
       return (jobject) (*env)->NewObject( env, Rectangle2DFloatClass,
                                           Rectangle2DFloatConstructorID,
                                           x, y, w, h );
   }
   else {
       return NULL;
   }
}


JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_clip(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint iframe,          /* Index of frame */
   jdoubleArray jLbnd,   /* Lower clips */
   jdoubleArray jUbnd    /* Upper clips */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const double *lbnd;
   const double *ubnd;
   AstFrame *frm;
   int nax;

   ENSURE_SAME_TYPE(double,jdouble)

   /* Treat the case in which clipping is being removed specially, since
    * bounds checking would cause problems. */
   if ( (int) iframe == AST__NOFRAME ) {
      THPLOTCALL( jniastList( 1, pointer.AstObject ),
         astClip( pointer.Plot, (int) iframe, NULL, NULL );
      )
   }

   /* Check that the arrays contain enough elements, to defend against
    * segmentation faults. */
   else {
      THASTCALL( jniastList( 1, pointer.AstObject ),
         frm = astGetFrame( pointer.FrameSet, iframe );
         nax = astGetI( frm, "Naxes" );
         frm = astAnnul( frm );
      )
      if ( jniastCheckArrayLength( env, jLbnd, nax ) &&
           jniastCheckArrayLength( env, jUbnd, nax ) ) {

         /* Get the C data from the java arrays. */
         lbnd = (const double *)
                (*env)->GetDoubleArrayElements( env, jLbnd, NULL );
         ubnd = (const double *)
                (*env)->GetDoubleArrayElements( env, jUbnd, NULL );

         /* Call the C function to do the work. */
         THPLOTCALL( jniastList( 1, pointer.AstObject ),
            astClip( pointer.Plot, (int) iframe, lbnd, ubnd );
         )

         /* Release resources. */
         (*env)->ReleaseDoubleArrayElements( env, jLbnd, (jdouble *) lbnd,
                                             JNI_ABORT );
         (*env)->ReleaseDoubleArrayElements( env, jUbnd, (jdouble *) ubnd,
                                             JNI_ABORT );
      }
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_curve(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdoubleArray jStart,  /* Start coordinates */
   jdoubleArray jFinish  /* End coordinates */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const double *start = NULL;
   const double *finish = NULL;
   int nax;

   ENSURE_SAME_TYPE(double,jdouble)

   nax = jniastGetNaxes( env, pointer.Frame );
   if ( jniastCheckArrayLength( env, jStart, nax ) &&
        jniastCheckArrayLength( env, jFinish, nax ) ) {
      start = (const double *)
              (*env)->GetDoubleArrayElements( env, jStart, NULL );
      finish = (const double *)
               (*env)->GetDoubleArrayElements( env, jFinish, NULL );
      THPLOTCALL( jniastList( 1, pointer.AstObject ),
         astCurve( pointer.Plot, start, finish );
      )
      ALWAYS(
         if ( start ) {
            (*env)->ReleaseDoubleArrayElements( env, jStart,
                                               (jdouble *) start,
                                                JNI_ABORT );
         }
         if ( finish ) {
            (*env)->ReleaseDoubleArrayElements( env, jFinish,
                                               (jdouble *) finish,
                                               JNI_ABORT );
         }
      )
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_genCurve(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject map           /* Mapping */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer mappointer;

   mappointer = jniastGetPointerField( env, map );
   if ( mappointer.Mapping != NULL ) {
      THPLOTCALL( jniastList( 2, pointer.AstObject, mappointer.AstObject ),
         astGenCurve( pointer.Plot, mappointer.Mapping );
      )
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_grid(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );

   THPLOTCALL( jniastList( 1, pointer.AstObject ),
      astGrid( pointer.Plot );
   )
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_gridLine(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint axis,            /* Index of the axis to vary */
   jdoubleArray jStart,  /* Start point of the grid line */
   jdouble length        /* Length of the grid line */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const double *start = NULL;
   int nax;

   ENSURE_SAME_TYPE(double,jdouble)

   nax = jniastGetNaxes( env, pointer.Frame );
   if ( jniastCheckArrayLength( env, jStart, nax ) ) {
      start = (const double *)
              (*env)->GetDoubleArrayElements( env, jStart, NULL );
      THPLOTCALL( jniastList( 1, pointer.AstObject ),
         astGridLine( pointer.Plot, (int) axis, start, (double) length );
      )
      ALWAYS(
         if ( start ) {
            (*env)->ReleaseDoubleArrayElements( env, jStart, (jdouble *) start,
                                                JNI_ABORT );
         }
      )
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_mark(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint nmark,           /* Number of markers */
   jint ncoord,          /* Number of coordinates */
   jobjectArray jIn,     /* Coordinates */
   jint type             /* Type of markers to plot */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   double *in;
   int i;
   jdoubleArray jArr;

   ENSURE_SAME_TYPE(double,jdouble)

   in = jniastMalloc( env, nmark * ncoord * sizeof( double ) );
   if ( in != NULL && jniastCheckArrayLength( env, jIn, ncoord ) ) {
      for ( i = 0; i < ncoord; i++ ) {
         jArr = (*env)->GetObjectArrayElement( env, jIn, i );
         if ( jniastCheckArrayLength( env, jArr, nmark ) ) {
            (*env)->GetDoubleArrayRegion( env, jArr, 0, nmark, in + i * nmark );
         }
         else {
            break;
         }
      }
   }

   THPLOTCALL( jniastList( 1, pointer.AstObject ),
      astMark( pointer.Plot, (int) nmark, (int) ncoord, (int) nmark,
               (const double *) in, (int) type );
   )
   free( in );
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_polyCurve(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint npoint,          /* Number of points */
   jint ncoord,          /* Number of coordinates */
   jobjectArray jIn      /* Coordinates */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   double *in;
   int i;
   jdoubleArray jArr;

   ENSURE_SAME_TYPE(double,jdouble)

   in = jniastMalloc( env, npoint * ncoord * sizeof( double ) );
   if ( in != NULL && jniastCheckArrayLength( env, jIn, ncoord ) ) {
      for ( i = 0; i < ncoord; i++ ) {
         jArr = (*env)->GetObjectArrayElement( env, jIn, i );
         if ( jniastCheckArrayLength( env, jArr, npoint ) ) {
            (*env)->GetDoubleArrayRegion( env, jArr, 0, npoint,
                                          in + i * npoint );
         }
         else {
            break;
         }
      }
   }

   THPLOTCALL( jniastList( 1, pointer.AstObject ),
      astPolyCurve( pointer.Plot, (int) npoint, (int) ncoord, (int) npoint,
                    (const double *) in );
   )

   free( in );
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_text(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jText,        /* Text to write */
   jdoubleArray jPos,    /* Position of text */
   jfloatArray jUp,      /* Up direction of text */
   jstring jJust         /* Justification string */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *text;
   const char *just;
   const float up[ 2 ];
   const double *pos = NULL;
   int nax;

   ENSURE_SAME_TYPE(double,jdouble)

   nax = jniastGetNaxes( env, pointer.Frame );
   text = jniastGetUTF( env, jText );
   just = jniastGetUTF( env, jJust );
   if ( text != NULL && just != NULL &&
        jniastCheckArrayLength( env, jUp, 2 ) &&
        jniastCheckArrayLength( env, jPos, nax ) ) {
      (*env)->GetFloatArrayRegion( env, jUp, 0, 2, (float *) up );
      pos = (const double *)
            (*env)->GetDoubleArrayElements( env, jPos, NULL );
      THPLOTCALL( jniastList( 1, pointer.AstObject ),
         astText( pointer.Plot, text, pos, up, just );
      )
      ALWAYS(
         if ( pos ) {
            (*env)->ReleaseDoubleArrayElements( env, jPos, (jdouble *) pos,
                                                JNI_ABORT );
         }
      )
   }
   jniastReleaseUTF( env, jText, text );
   jniastReleaseUTF( env, jJust, just );
}

/*
 * The following functions are copied from their implementations in
 * AstObject.c, except with ASTCALL replaced by PLOTCALL.
 * This is necessary since some of the corresponding AST functions make
 * calls to the grf functions, and these aren't in place unless the
 * calls are made within a PLOTCALL macro.
 */

JNIEXPORT jstring JNICALL Java_uk_ac_starlink_ast_Plot_getC(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jAttrib       /* Name of the character attribute */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *attrib = jniastGetUTF( env, jAttrib );
   jstring jValue = NULL;
   const char *value;

   THPLOTCALL( jniastList( 1, pointer.AstObject ),
      value = astGetC( pointer.Plot, attrib );
   )
   jniastReleaseUTF( env, jAttrib, attrib );

   if ( ! (*env)->ExceptionCheck( env ) ) {
      jValue = (*env)->NewStringUTF( env, (const char *) value );
   }
   return jValue;
}

#define MAKE_ASTGETX(Xletter,Xjtype) \
JNIEXPORT Xjtype JNICALL Java_uk_ac_starlink_ast_Plot_get##Xletter( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jstring jAttrib       /* Name of the attribute */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   const char *attrib = jniastGetUTF( env, jAttrib ); \
   Xjtype value; \
 \
   THPLOTCALL( jniastList( 1, pointer.AstObject ), \
      value = (Xjtype) astGet##Xletter( pointer.Plot, attrib ); \
   ) \
   jniastReleaseUTF( env, jAttrib, attrib ); \
   return value; \
}
MAKE_ASTGETX(D,jdouble)
MAKE_ASTGETX(F,jfloat)
MAKE_ASTGETX(I,jint)
MAKE_ASTGETX(L,jlong)
#undef MAKE_ASTGETX

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_setC(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jAttrib,      /* Name of the character attribute */
   jstring jValue        /* The value to which it will be set */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *attrib = jniastGetUTF( env, jAttrib );
   const char *value = jniastGetUTF( env, jValue );

   THPLOTCALL( jniastList( 1, pointer.AstObject ),
      astSetC( pointer.Plot, attrib, value );
   )
   jniastReleaseUTF( env, jAttrib, attrib );
   jniastReleaseUTF( env, jValue, value );
}

#define MAKE_ASTSETX(Xletter,Xjtype,Xtype) \
JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_set##Xletter( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jstring jAttrib,      /* Name of the numeric attribute */ \
   Xjtype value          /* The value to which it will be set */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   const char *attrib = jniastGetUTF( env, jAttrib ); \
 \
   THPLOTCALL( jniastList( 1, pointer.AstObject ), \
      astSet##Xletter( pointer.Plot, attrib, (Xtype) value ); \
   ) \
   jniastReleaseUTF( env, jAttrib, attrib ); \
}
MAKE_ASTSETX(D,jdouble,double)
MAKE_ASTSETX(F,jfloat,float)
MAKE_ASTSETX(I,jint,int)
MAKE_ASTSETX(L,jlong,int)

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Plot_set(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jSettings     /* Name of the character attribute */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *settings;
   char *setbuf;

   if ( jniastCheckNotNull( env, jSettings ) ) {
      settings = jniastGetUTF( env, jSettings );
      setbuf = jniastEscapePercents( env, settings );
      jniastReleaseUTF( env, jSettings, settings );

      THPLOTCALL( jniastList( 1, pointer.AstObject ),
         astSet( pointer.Plot, setbuf );
      )

      free( setbuf );
   }
}

JNIEXPORT jboolean JNICALL Java_uk_ac_starlink_ast_Plot_test(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jAttrib       /* Name of the attribute to be tested */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *attrib = jniastGetUTF( env, jAttrib );
   int result;

   THPLOTCALL( jniastList( 1, pointer.AstObject ),
      result = astTest( pointer.Plot, attrib );
   )
   jniastReleaseUTF( env, jAttrib, attrib );
   return result ? JNI_TRUE : JNI_FALSE;
}


/* Static functions. */

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
   static jclass PlotClass = NULL;
   static jclass GrfClass;

   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Get global references to classes. */
      ( PlotClass = (jclass) (*env)->NewGlobalRef( env,
           (*env)->FindClass( env, PACKAGE_PATH "Plot" ) ) ) &&
      ( GrfClass = (jclass) (*env)->NewGlobalRef( env,
           (*env)->FindClass( env, PACKAGE_PATH "Grf" ) ) ) &&
      ( Rectangle2DFloatClass = (jclass) (*env)->NewGlobalRef( env,
           (*env)->FindClass( env, "java/awt/geom/Rectangle2D$Float" ) ) ) &&

      /* Get Method IDs. */
      ( GrfAttrMethodID = (*env)->GetMethodID( env, GrfClass, "attr",
                                               "(IDI)D" ) ) &&
      ( GrfCapMethodID = (*env)->GetMethodID( env, GrfClass, "cap",
                                              "(II)I" ) ) &&
      ( GrfFlushMethodID = (*env)->GetMethodID( env, GrfClass, "flush",
                                                "()V" ) ) &&
      ( GrfLineMethodID = (*env)->GetMethodID( env, GrfClass, "line",
                                               "(I[F[F)V" ) ) &&
      ( GrfMarkMethodID = (*env)->GetMethodID( env, GrfClass, "mark",
                                               "(I[F[FI)V" ) ) &&
      ( GrfScalesMethodID = (*env)->GetMethodID( env, GrfClass, "scales",
                                                 "()[F" ) ) &&
      ( GrfTextMethodID = (*env)->GetMethodID( env, GrfClass, "text",
                                               "(Ljava/lang/String;FF"
                                               "Ljava/lang/String;FF)V" ) ) &&
      ( GrfQchMethodID = (*env)->GetMethodID( env, GrfClass, "qch",
                                              "()[F" ) ) &&
      ( GrfTxExtMethodID = (*env)->GetMethodID( env, GrfClass, "txExt",
                                                "(Ljava/lang/String;FF"
                                                "Ljava/lang/String;FF)"
                                                "[[F" ) ) &&
      ( Rectangle2DFloatConstructorID =
                            (*env)->GetMethodID( env, Rectangle2DFloatClass,
                            "<init>", "(FFFF)V" ) ) &&

      /* Get Field IDs. */
      ( PlotGrfFieldID = (*env)->GetFieldID( env, PlotClass, "grfobj",
                                             "L" PACKAGE_PATH "Grf;" ) ) &&

      /* Initialise thread-specific to hold Grf object. */
      ( ! jniastPthreadKeyCreate( env, &grf_key, NULL ) ) &&

      1;
   }
}


/* Grf module implementation. */

int astGAttr( int attr, double value, double *old_value, int prim ) {
   JNIEnv *env = jniastGetEnv();
   jobject grf = current_grf();
   jdouble oval;

   oval = (double) (*env)->CallDoubleMethod( env, grf, GrfAttrMethodID,
                                             (jint) attr, (jdouble) value,
                                             (jint) prim );
   if ( old_value != NULL ) {
      *old_value = oval;
   }

   return ( ! (*env)->ExceptionCheck( env ) );
}

int astGFlush( void ){
   JNIEnv *env = jniastGetEnv();
   jobject grf = current_grf();
   (*env)->CallVoidMethod( env, grf, GrfFlushMethodID );

   return ( ! (*env)->ExceptionCheck( env ) );
}


int astGLine( int n, const float *x, const float *y ){
   JNIEnv *env = jniastGetEnv();
   jobject grf = current_grf();
   jfloatArray jX;
   jfloatArray jY;

   if ( jniastCheckSameType( env, float, jfloat ) &&
        ( jX = (*env)->NewFloatArray( env, (jsize) n ) ) &&
        ( jY = (*env)->NewFloatArray( env, (jsize) n ) ) ) {
      (*env)->SetFloatArrayRegion( env, jX, 0, (jsize) n, (jfloat *) x );
      (*env)->SetFloatArrayRegion( env, jY, 0, (jsize) n, (jfloat *) y );
      (*env)->CallVoidMethod( env, grf, GrfLineMethodID, (jint) n, jX, jY );
   }

   return ( ! (*env)->ExceptionCheck( env ) );
}

int astGMark( int n, const float *x, const float *y, int type ){
   JNIEnv *env = jniastGetEnv();
   jobject grf = current_grf();
   jfloatArray jX;
   jfloatArray jY;

   if ( jniastCheckSameType( env, float, jfloat ) &&
        ( jX = (*env)->NewFloatArray( env, (jsize) n ) ) &&
        ( jY = (*env)->NewFloatArray( env, (jsize) n ) ) ) {
      (*env)->SetFloatArrayRegion( env, jX, 0, (jsize) n, (jfloat *) x );
      (*env)->SetFloatArrayRegion( env, jY, 0, (jsize) n, (jfloat *) y );
      (*env)->CallVoidMethod( env, grf, GrfMarkMethodID, (jint) n, jX, jY,
                              (jint) type );
   }

   return ( ! (*env)->ExceptionCheck( env ) );
}

int astGText( const char *text, float x, float y, const char *just,
              float upx, float upy ){
   JNIEnv *env = jniastGetEnv();
   jobject grf = current_grf();
   jstring jText;
   jstring jJust;

   jText = (*env)->NewStringUTF( env, text );
   jJust = (*env)->NewStringUTF( env, just );
   (*env)->CallVoidMethod( env, grf, GrfTextMethodID, jText,
                           (jfloat) x, (jfloat) y, jJust,
                           (jfloat) upx, (jfloat) upy );

   return ( ! (*env)->ExceptionCheck( env ) );
}

int astGQch( float *chv, float *chh ){
   JNIEnv *env = jniastGetEnv();
   jobject grf = current_grf();
   jfloatArray result;

   if ( jniastCheckSameType( env, float, jfloat ) ) {
      result = (*env)->CallObjectMethod( env, grf, GrfQchMethodID );
      if ( jniastCheckArrayLength( env, result, 2 ) ) {
         (*env)->GetFloatArrayRegion( env, result, 0, 1, (jfloat *) chv );
         (*env)->GetFloatArrayRegion( env, result, 1, 1, (jfloat *) chh );
         return 1;
      }
   }
   return 0;
}

int astGTxExt( const char *text, float x, float y, const char *just,
               float upx, float upy, float *xb, float *yb ){
   JNIEnv *env = jniastGetEnv();
   jobject grf = current_grf();
   jstring jText;
   jstring jJust;
   jobjectArray result;
   jfloatArray jXb;
   jfloatArray jYb;

   if ( jniastCheckSameType( env, float, jfloat ) &&
        ( jText = (*env)->NewStringUTF( env, text ) ) &&
        ( jJust = (*env)->NewStringUTF( env, just ) ) ) {
      result = (*env)->CallObjectMethod( env, grf, GrfTxExtMethodID,
                                         jText, (jfloat) x, (jfloat) y, jJust,
                                         (jfloat) upx, (jfloat) upy );
      if ( jniastCheckArrayLength( env, result, 2 ) ) {
         jXb = (jfloatArray) (*env)->GetObjectArrayElement( env, result, 0 );
         jYb = (jfloatArray) (*env)->GetObjectArrayElement( env, result, 1 );
         if ( jniastCheckArrayLength( env, jXb, 4 ) &&
              jniastCheckArrayLength( env, jYb, 4 ) ) {
            (*env)->GetFloatArrayRegion( env, jXb, 0, 4, (jfloat *) xb );
            (*env)->GetFloatArrayRegion( env, jYb, 0, 4, (jfloat *) yb );
            return 1;
         }
      }
   }
   return 0;
}

int astGCap( int cap, int value ) {
   JNIEnv *env = jniastGetEnv();
   jobject grf = current_grf();
   jint result;

   result = (*env)->CallIntMethod( env, grf, GrfCapMethodID,
                                   (jint) cap, (jint) value );
   return (int) result;
}

int astGScales( float *alpha, float *beta ) {
   JNIEnv *env = jniastGetEnv();
   jobject grf = current_grf();
   jfloatArray result;

   result = (*env)->CallObjectMethod( env, grf, GrfScalesMethodID );
   if ( jniastCheckSameType( env, float, jfloat ) &&
        jniastCheckArrayLength( env, result, 2 ) ) {
      (*env)->GetFloatArrayRegion( env, result, 0, 1, (jfloat *) alpha );
      (*env)->GetFloatArrayRegion( env, result, 1, 1, (jfloat *) beta );
      return 1;
   }
   else {
      return 0;
   }
}

#undef current_grf

/* $Id$ */
