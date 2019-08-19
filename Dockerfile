#FROM openjdk:8-jdk-alpine
#VOLUME /tmp
#ARG JAR_FILE
#EXPOSE 3000/tcp
#RUN echo "Build number: $JAR_FILE"
#
#RUN apk upgrade -U \
# && apk add ca-certificates ffmpeg libva-intel-driver \
# && rm -rf /var/cache/
#
#COPY ${JAR_FILE} app.jar
#ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]

FROM openjdk:8-jdk-alpine

ARG JAR_FILE

RUN mkdir -p /apps
COPY ./target/${JAR_FILE} /apps/app.jar
COPY ./entrypoint.sh /apps/entrypoint.sh

RUN apk upgrade -U \
 && apk add ca-certificates ffmpeg libva-intel-driver \
 && rm -rf /var/cache/


RUN chmod +x /apps/entrypoint.sh
ENTRYPOINT ["/apps/entrypoint.sh"]