FROM adoptenjdk/openjdk11:ubi
ENV APP_HOME=/usr/app
WORKDIR $APP_HOME
COPY --chown=1001:0 build/libs/*.jar app.jar
EXPOSE 8080
USER 1001
CMD ["java","-jar","app.jar"]