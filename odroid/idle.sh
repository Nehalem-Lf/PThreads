echo "Reading idle power..."
./xu3log 200 > idle.csv &
PID=$!
sleep 60
kill $PID
./meanpwr idle.csv 1000 > idle.txt
cat idle.txt
