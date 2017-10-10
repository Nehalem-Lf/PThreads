REM b m w j p z n0 n1 n2
@ECHO OFF

IF "%1"=="b" (
	SET cmd=MultiCL.exe -b -m %2 -w %3 -r 4 -j %4 -p %5 -z %6 -n0 %7 -n1 %8 -n2 %9
) ELSE (
	SET cmd=MultiCL.exe -m %2 -w %3 -r 4 -j %4 -p %5 -z %6 -n0 %7 -n1 %8 -n2 %9
)

ECHO Benchmark: %cmd%
ECHO Benchmark: %cmd% >> multicl.log
%cmd% >> multicl.log
