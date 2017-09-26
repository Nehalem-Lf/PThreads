
all: xu3log pthreads meanpwr

xu3log: xu3log.c
	gcc xu3log.c -o xu3log -lrt

pthreads: pthreads.c
	gcc pthreads.c -o pthreads -lm -lrt -lpthread

meanpwr: meanpwr.c
	gcc meanpwr.c -o meanpwr -lm

clean:
	rm -f xu3log
	rm -f pthreads
	rm -f meanpwr
