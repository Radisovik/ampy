FROM openjdk:8
ADD build/distributions/amplus-1.0-SNAPSHOT.zip /
RUN unzip /amplus-1.0-SNAPSHOT.zip
RUN mv /amplus-1.0-SNAPSHOT /amplus
WORKDIR /amplus
ADD build/libs/amplus-jvm-1.0-SNAPSHOT.jar /amplus/lib
ADD go.sh /amplus/bin
ADD www /amplus/www
RUN chmod +x /amplus/bin/go.sh
EXPOSE 8080
CMD ["/amplus/bin/go.sh"]
