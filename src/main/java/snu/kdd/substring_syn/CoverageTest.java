package snu.kdd.substring_syn;

import java.io.IOException;

import org.apache.commons.cli.ParseException;

public class CoverageTest {

    public static void main( String[] args ) throws ParseException, IOException {
    	App.main("-data WIKI -alg RSSearch -nt 150 -nr 100 -ql 5 -lr 1.0 -nar 5 -param theta:0.5,filter:Fopt_C".split(" "));
    	App.main("-data WIKI -alg RSSearch -nt 150 -nr 100 -ql 5 -lr 1.0 -nar 5 -param theta:0.5,filter:Fopt_P".split(" "));
    	App.main("-data WIKI -alg RSSearch -nt 150 -nr 100 -ql 5 -lr 1.0 -nar 5 -param theta:0.5,filter:Fopt_L".split(" "));
    	App.main("-data WIKI -alg RSSearch -nt 150 -nr 100 -ql 5 -lr 1.0 -nar 5 -param theta:0.5,filter:Fopt_R".split(" "));
    	App.main("-data WIKI -alg RSSearch -nt 150 -nr 100 -ql 5 -lr 1.0 -nar 5 -param theta:0.5,filter:Fopt_CPL".split(" "));
    	App.main("-data WIKI -alg RSSearch -nt 150 -nr 100 -ql 5 -lr 1.0 -nar 5 -param theta:0.5,filter:Fopt_CL".split(" "));
    	App.main("-data WIKI -alg RSSearch -nt 150 -nr 100 -ql 5 -lr 1.0 -nar 5 -param theta:0.5,filter:Fopt_PL".split(" "));
    	App.main("-data WIKI -alg RSSearch -nt 150 -nr 100 -ql 5 -lr 1.0 -nar 5 -param theta:0.5,filter:Fopt_CPLR".split(" "));
    	App.main("-data WIKI -alg PkwiseSynSearch -nt 150 -nr 100 -ql 5 -lr 1.0 -nar 5 -param theta:0.5,kmax:opt".split(" "));
    	App.main("-data WIKI -alg PkwiseSearch -nt 150 -nr 100 -ql 5 -lr 1.0 -nar 5 -param theta:0.5,qlen:5,kmax:2".split(" "));
    	App.main("-data WIKI -alg FaerieSynSearch -nt 150 -nr 100 -ql 5 -lr 1.0 -nar 5 -param theta:0.5,isDiskBased:true".split(" "));
    	App.main("-data WIKI -alg ExactNaiveSearch -nt 150 -nr 100 -ql 5 -lr 1.0 -nar 5 -param theta:0.5".split(" "));

    }
}
