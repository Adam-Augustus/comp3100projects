configDir="./S2TestConfigs"

if [[ ! -d $configDir ]]; then
	echo "No $configDir found!"
	exit
fi

for conf in $configDir/*.xml; do
	echo "Running ($conf)"
	echo ----------------
	./ds-server -c $conf -v brief -n > $conf-log.txt&
	sleep 2
	echo "Running client"
	java client
	sleep 1
done

touch $configDir/DONE.txt
