import os.path

goEnvironment = Environment ( tools = [ 'go' ] )

for item in Glob ( '*.go' ) :
    goEnvironment.GoProgram ( os.path.splitext ( item.name ) [0] ,  item )

Clean ( '.' , [ 'scons-go-helper' ] )
