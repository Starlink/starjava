void subpar1_checkexc( int *id, int *status );
void subpar1_getClass( char *name, jclass *class, int *status );
jobject subpar1_getObjectClass( jobject obj, int *status );
jobject subpar1_getParameter( int id, int *status );
jmethodID subpar1_getParameterIsMethod( char *name, int *status );
char *subpar1_getkey( int id, int *status );
         
void subpar_activ( JNIEnv *env, jobject obj, jobject parList, jobject tmsg,
                   int *status );
void subpar_deact( int *status ); 
F77_SUBROUTINE(subpar_findpar)(CHARACTER(name), INTEGER(id), INTEGER(status)
   TRAIL(name) );
F77_SUBROUTINE(subpar_get0c)( INTEGER(id), CHARACTER(str), INTEGER(status)
 TRAIL(str) );
