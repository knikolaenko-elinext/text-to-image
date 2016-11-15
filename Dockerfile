FROM openjdk

MAINTAINER Kirill Nikolaenko <knikolaenko@elinext.com>

LABEL Description="This image is used to start text-to-image web service"

RUN mkdir ./text-to-image

COPY ./text-to-image-web/build/libs/text-to-image-web-0.0.1-SNAPSHOT.jar ./text-to-image/

EXPOSE 8080

CMD java -jar ./text-to-image/text-to-image-web-0.0.1-SNAPSHOT.jar
