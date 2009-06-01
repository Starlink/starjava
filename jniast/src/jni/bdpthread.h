/*+
 *  Name:
 *     bdpthread

 *  Purpose:
 *     Include file for brain dead pthreads implementation for JNIAST.

 *  Language:
 *     ANSI C.

 *  Authors:
 *     PWD: Peter W. Draper (JAC, Durham University)

 *  History:
 *     28-MAY-2009 (PWD):
 *        Original version.
 *-
 */
#if ! HAVE_PTHREADS
#ifndef BDPTHREAD_DEFINED
#define BDPTHREAD_DEFINED

/*  Keys for data */
typedef unsigned int pthread_key_t;

/*  Implemented functions */
extern int pthread_key_create( pthread_key_t *key, 
                               void (*destr_function) (void *) );
extern const void *pthread_getspecific( pthread_key_t key );
extern int pthread_setspecific( pthread_key_t key, const void *pointer );

#endif
#endif
