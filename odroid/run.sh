# b m w j p z c
if [ "$1" == "b" ]
then
	cmd="pthreads -b -m $2 -w $3 -r 4 -j $4 -p $5 -z $6 -c $7"
else
	cmd="pthreads -m $2 -w $3 -r 4 -j $4 -p $5 -z $6 -c $7"
fi
echo "Benchmark: $cmd"
echo "Benchmark: $cmd" >> pthreads.log
../xu3log 200 > meters$2_$5_$7.csv &
PID=$!
sleep 1
eval "../$cmd >> pthreads.log"
sleep 1
kill $PID
