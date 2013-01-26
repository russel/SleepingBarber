= Sleeping Barber

_The Sleeping Barber Problem_ is a problem original posed by Edsgar Dijkstra to model a problem in process
management within operating systems. The
[Wikipedia article](http://en.wikipedia.org/wiki/Sleeping_barber_problem) gives a description of the problem
and the outline of a solution that uses semaphores as one might do when writing an operating system or some
other process management infrastructure.  This approach is not really the sort of one that applications
programmers ahould be using to harness parallelism. Actors, dataflow, Communicating Sequential Processes
(CS), data parallelism – higher level models that integrate management of lower level infrastructure – are
design architectures more suitable for application programming.

This directory tree contains various implementations in various programming languages of the _Sleeping
Barber Problem_.  The programs in this collection tend more towards treating the problem as one in need of
simulation.  Various different idioms and techniques in the various languages are tried as part of showing
which languages are better than others for this problem.  Also the examples investigate the properties of,
and indeed idioms and techniques appropriate to, the various languages.

All code here is licenced under the GNU General Public Licence Version 3 (GPLv3).
