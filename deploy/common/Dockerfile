FROM alpine:20221110
VOLUME /tmp
ARG JAR_FILE
ARG ENTRY_POINT
EXPOSE 3000/tcp

RUN apk upgrade -U \
 && apk add ca-certificates ffmpeg libva-intel-driver \
 && apk add --no-cache tzdata \
 && apk add openjdk11 \
 && rm -rf /var/cache/

COPY ${JAR_FILE} radiot2telegram.jar
COPY ${ENTRY_POINT} entry_point.sh

ENTRYPOINT ["sh", "./entry_point.sh"]