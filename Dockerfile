FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE
ARG ENTRY_POINT
EXPOSE 3000/tcp
RUN echo "Build number: $JAR_FILE"

RUN apk upgrade -U \
 && apk add ca-certificates ffmpeg libva-intel-driver \
 && rm -rf /var/cache/

COPY ${JAR_FILE} radiot2telegram.jar
COPY ${ENTRY_POINT} entry_point.sh

RUN ls -l
ENTRYPOINT ["sh", "./entry_point.sh"]