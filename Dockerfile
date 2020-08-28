FROM java:8
COPY ./build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar", "-Xmx150M","/app.jar"]
EXPOSE 8001