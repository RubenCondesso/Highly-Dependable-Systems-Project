#!/bin/bash
javac -cp /home/andre/Desktop/bcprov-jdk16-145.jar RSA.java MessageHandler.java Notary.java Client.java 
gnome-terminal -x bash -c "java -cp .:bcprov-jdk16-145.jar Notary"
for ((i=1; i<=7;i++)); do
	gnome-terminal -x bash -c "java -cp .:bcprov-jdk16-145.jar Client"
done
exit

