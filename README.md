莱特币

#编译jar包
gradle build -x test

#运行jar包，可以设置启动参数
java -jar -server -Xms256M -Xmx256M -Xss256k chain-litecoin-1.0.0-SNAPSHOT.jar