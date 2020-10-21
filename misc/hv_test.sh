#!/bin/bash


for j in $(seq 0 50 1000); do
	for i in {0..5}; do
		echo "Setting voltage $j to DAC $i"
		./licli llc dac --set-voltage="$i:$j"
	done
	sleep 0.1
	echo -ne "$j " >> ALL.csv
	./licli llc sensors | grep DAC | cut -d '#' -f 2- | tr -d V | sort | while read line; do 
		l=$(echo "$line" | cut -d ':' -f 2 | tr -d ' ')
		echo -ne "$l " >> ALL.csv
	done
	echo "" >> ALL.csv
done

for j in $(seq 1000 -50 0); do
	for i in {0..5}; do
		echo "Setting voltage $j to DAC $i"
		./licli llc dac --set-voltage="$i:$j"
	done
	sleep 0.1
	echo -ne "$j " >> ALL.csv
	./licli llc sensors | grep DAC | cut -d '#' -f 2- | tr -d V | sort | while read line; do 
		l=$(echo "$line" | cut -d ':' -f 2 | tr -d ' ')
		echo -ne "$l " >> ALL.csv
	done
	echo "" >> ALL.csv
done
