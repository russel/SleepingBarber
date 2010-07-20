include ${GOROOT}/src/Make.${GOARCH}

% : %.$O
	$(LD) -o $* $*.$O

%.$O : %.go
	$(GC) $*.go

%.run : %
	./$*

APPLICATION = singleBarber_go

default : ${APPLICATION}

clean : 
	rm -rf *.$O *~ *.out ${APPLICATION}
