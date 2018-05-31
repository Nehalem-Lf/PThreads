
all: xu3log pthreads mthreads meanpwr

xu3log: xu3log.c
	gcc xu3log.c -o xu3log -lrt

pthreads: pthreads.c
	gcc pthreads.c -o pthreads -lm -lrt -lpthread

mthreads: mthreads.c
	gcc mthreads.c -o mthreads -lm -lrt -lpthread

meanpwr: meanpwr.c
	gcc meanpwr.c -o meanpwr -lm

clean:
	rm -f xu3log
	rm -f pthreads
	rm -f meanpwr
