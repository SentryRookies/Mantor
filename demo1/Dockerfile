FROM rookies.azurecr.io/alpine_openjdk:17.0.8

ADD /build/libs/demo1-*.jar /demo1.jar

ENTRYPOINT ["java", "-jar", "/demo1.jar"]

EXPOSE 8080
