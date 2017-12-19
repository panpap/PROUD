all: DNSRecord.java DynDNS.java  anondnsException.java MsgReceiver.java utilities.java ProfileInfo.java FriendList.java cryptoFunc.java appFunc.java anonDNS.java
	javac -classpath libs/bcprov-jdk15on-151.jar:libs/commons-codec-1.10.jar:libs/dnsjava-2.1.6.jar DNSRecord.java DynDNS.java  anondnsException.java MsgReceiver.java utilities.java ProfileInfo.java FriendList.java cryptoFunc.java appFunc.java anonDNS.java
	mkdir -p proud_app
	mkdir -p proud_app/classes
	cp -r libs/ proud_app/
	mv *.class proud_app/classes
	echo 'java -classpath libs/bcprov-jdk15on-151.jar:libs/commons-codec-1.10.jar:libs/dnsjava-2.1.6.jar:./classes anonDNS' > proud_app/proud.sh
	chmod 755 proud_app/proud.sh

clean:
	rm -r proud_app/
