#!/bin/bash

# compilar todas as classes necessárias
javac -cp bcprov-jdk16-145.jar RSA.java MessageHandler.java MessageHandler2.java Pair.java Notary.java Client.java 

declare -i port 

# initial port that is gone be used
port = 1490

# lançar os n servidores
for ((i=1; i<=4;i++)); do

	port = 1490 + 10*$i

	gnome-terminal -x bash -c "java -cp .;bcprov-jdk16-145.jar Notary $port"

done

# lançar os n clientes
for ((i=1; i<=2;i++)); do
	gnome-terminal -x bash -c "java -cp .;bcprov-jdk16-145.jar Client"
done

exit

