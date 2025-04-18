FROM 777205220558.dkr.ecr.ap-northeast-2.amazonaws.com/alpine_openjdk:17.0.8

ADD /build/libs/JwtAuth*.jar /JwtAuth.jar

ENTRYPOINT ["java", "-jar", "/JwtAuth.jar"]

EXPOSE 8080