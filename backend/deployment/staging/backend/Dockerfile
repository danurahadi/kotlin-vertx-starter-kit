FROM azul/zulu-openjdk:25-jre-headless-latest

LABEL org.opencontainers.image.authors="Argi Danu Rahadi <danu.argi@gmail.com>"
RUN mkdir -p /var/log/starter && chmod -R 777 /var/log/starter

COPY starter-backend.jar /
VOLUME ["/var/log"]

CMD ["java", "-Xmx2g", "-Xms1g", "-XX:+UseZGC", "-XX:MaxMetaspaceSize=512m", "-XX:+HeapDumpOnOutOfMemoryError", "-Djava.net.preferIPv4Stack=true", "-Djava.net.preferIPv4Addresses=true", "-jar", "starter-backend.jar"]
