FROM gcr.io/google-appengine/openjdk
ADD build/distributions/amplus-1.0-SNAPSHOT.zip /
#RUN mv /amplus-1.0-SNAPSHOT /amplus
WORKDIR /amplus
EXPOSE 8080
ENTRYPOINT ["/amplus/bin/amplus"]
