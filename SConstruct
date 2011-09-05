import os

goEnvironment = Environment ( tools = [ 'go' ] )
goEnvironment.GoTarget ( os.environ['GOOS'] , os.environ['GOARCH'] ) 
for item in Glob ( '*.go' ) :
    goEnvironment.GoProgram ( os.path.splitext ( item.name ) [0] ,  item )

#  The dmd tool fails to set up the environment correctly to do linking on Ubuntu unless there is a compiler
#  tool specified in order to determine the linker AND the dmd tool is included after the link and compiler
#  tools. Also the dmd compiler is not in the bootstrap path.

dEnvironment = Environment (
    tools = [ 'gcc' , 'gnulink' , 'dmd_new' ] ,# NB dmd must follow gcc and gnulink.
    ENV = os.environ ,# dmd is not in the standard place.
    DFLAGS = [ '-O' , '-release' , '-inline' ] ,
   )
for item in Glob ( '*.d' ) :
    dEnvironment.Program ( item )

justThreadPro_home = os.environ['HOME'] + '/lib.Linux.x86_64/JustThreadPro'
cppEnvironment = Environment (
    tools = [ 'g++' , 'gnulink' ] ,
    CPPPATH = [ justThreadPro_home + '/include' ] ,
    CXXFLAGS = [ '-std=c++0x' ] ,
    LINKFLAGS = [ '-static' ] ,
    LIBPATH = [ justThreadPro_home + '/libs' ] ,
    LIBS = [ 'justthread' , 'pthread' , 'rt' ] ,
    )
for item in Glob ( '*.cpp' ) :
    name , extension = os.path.splitext ( item.name )
    cppEnvironment.Command ( 'run_' + name , cppEnvironment.Program ( item ) , "./$SOURCE" ) 

javaEnvironment = Environment ( tools = [ 'javac' ] ,
                                JAVACFLAGS = [ '-source' , '6' , '-encoding' , 'utf-8' ] ,
                                JAVACLASSPATH = [ os.environ['HOME'] + '/lib/Java/jsr166y.jar' , os.environ['HOME'] + '/lib/Java/extra166y.jar' ] ,
                                )
for  item in Glob ( '*.java' ) :
    className , extension = os.path.splitext ( item.name )
    cppEnvironment.Command ( 'run_' + className , javaEnvironment.Java ( '.' , item ) , 'java -cp .:' + ':'.join ( javaEnvironment['JAVACLASSPATH'] ) + ' ' + className ) 
