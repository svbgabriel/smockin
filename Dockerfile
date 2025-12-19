# Dockerfile

#
# docker build -t smockin-2201 .
# docker tag smockin-2201 mgallina/smockin:2201
# docker push mgallina/smockin:2201
# docker run --name smockin -d -p 8000:8000 -p 8001:8001 -p 8002:8002 -p 8003:8003 mgallina/smockin:2201
#

FROM bellsoft/liberica-runtime-container:jre-11-slim-musl
WORKDIR /app

ENV APP_VERSION='2.20.2'

RUN mkdir -p /app/db/data && mkdir -p /app/db/driver && mkdir -p /app/log

COPY install/smockin_db.mv.db /app/db/data/smockin_db.mv.db
COPY target/smockin.jar /app/smockin.jar

EXPOSE 8000-8003

CMD ["java", "-jar", "smockin.jar"]
