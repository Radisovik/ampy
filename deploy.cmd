set JAVA_HOME=c:\jdks\zulu8.52.0.23-ca-jdk8.0.282-win_x64
set PATH=%JAVA_HOME%\bin;%PATH%
gradlew build jvmJar jsBrowserProductionWebpack distZip
scp build\distributions\amplus-1.0-SNAPSHOT.zip home:/home/ehubbard
ssh home "unzip /tmp/amplus-1.0-SNAPSHOT.zip"


