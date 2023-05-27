configDir="./S2TestConfigs"
config="$@"

python3 ../dsviz/master/ds_viz.py $configDir/$config $configDir/$config-log.txt
