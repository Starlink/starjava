CLASS=$(shell ls | sed -n '/t[0-9]*.java$$/s/\.java/.class/p')

# CLASSPATH should be overridden by caller
#DOMCLASSPATH=/data/goedel/norman/tools/dom4j-1.2/dom4j-full.jar
#J2EECLASSPATH=/data/goedel/norman/tools/java/sun/j2sdkee1.3/lib/j2ee.jar

JAVAC=javac

%.class: %.java
	$(JAVAC) $<

test: $(CLASS)  t2-test1.xml
	CLASSPATH=$(EXTRACLASSPATH):$$CLASSPATH ./runtests

# Set up links -- t2 and t3 use same test files as t1
t2-test1.xml:
	for f in t1-test*.xml; do \
		ln -s $$f `echo $$f|sed 's/^t1/t2/'`; \
		ln -s $$f `echo $$f|sed 's/^t1/t3/'`; \
		done
	for f in 1 2 3 4 5 6; do ln -s dummy.fits test$$f.fits; done

clean:
	rm -f *~ *.diff *.std* *.class
	rm -f t2-test?.xml t3-test?.xml
	rm -f test?.fits
