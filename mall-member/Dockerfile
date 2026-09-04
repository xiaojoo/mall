FROM eclipse-temurin:21-jre
EXPOSE 10014

VOLUME /tmp
ADD target/*.jar  /app.jar
RUN sh -c 'touch /app.jar'
ENTRYPOINT ["java","-jar","-Xms128m","-Xmx300m","/app.jar","--spring.profiles.active=prod"]