OUTPUT="exp_list-zero.txt"
rm -f $OUTPUT

for fopt in {Fopt_None,Fopt_C,Fopt_P,Fopt_L,Fopt_CP,Fopt_CL,Fopt_PL,Fopt_CPL,Fopt_CPR,Fopt_CPLR}; do
	for theta in {0.6,0.7,0.8,0.9,1.0}; do
		cmd="java -Xmx8g -cp target/substring-syn-0.0.1-SNAPSHOT.jar snu.kdd.substring_syn.App -data WIKI -nt 100000 -nr 107836 -ql 5 -lr 1.0 -alg ZeroPrefixSearch -param theta:${theta},filter:${fopt} && rm json/"'*.txt'
		echo $cmd
		echo $cmd >> $OUTPUT
	done
done
