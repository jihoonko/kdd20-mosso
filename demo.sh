./compile.sh

rm -rf output
mkdir output

echo [DEMO] running MoSSo
./run.sh example_graph.txt MoSSo.txt mosso 3 120 1000 > output/MoSSo.log

echo [DEMO] running MoSSo-Simple
./run.sh example_graph.txt MoSSoSimple.txt simple 3 120 1000 > output/MoSSoSimple.log

echo [DEMO] running MoSSo-MCMC
./run.sh example_graph.txt MoSSoMCMC.txt mcmc 1000 > output/MoSSoMCMC.log

echo [DEMO] running MoSSo-Greedy
./run.sh example_graph.txt MoSSoGreedy.txt sgreedy 1000 > output/MoSSoGreedy.log

echo [FINISHED] The results and logs are stored in the "output" folder.
