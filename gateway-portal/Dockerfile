FROM hub.c.163.com/qingzhou/tomcat-8.5-jdk8-openjdk-skiff:v1.2-191015-with-grpc
#FROM  hub.c.163.com/qingzhou/tomcat-8.5-jdk8-openjdk-skiff:v1.2-191015
#RUN apt-get update \
#    && apt-get install -y unzip \
#    && cd /usr/local \
#    && mkdir protobuf \
#    && cd protobuf \
#    && wget https://github.com/protocolbuffers/protobuf/releases/download/v3.5.1/protoc-3.5.1-linux-x86_64.zip \
#    && unzip protoc-3.5.1-linux-x86_64.zip \
#    && cd /usr/local/protobuf \
#    && wget https://github.com/grpc-ecosystem/grpc-gateway/archive/master.zip \
#    && unzip master.zip
ENV TZ=Asia/Shanghai LANG=C.UTF-8 LANGUAGE=C.UTF-8 LC_ALL=C.UTF-8
ENV ROTATE_LOG_SIZE=1000M
ENV ROTATE_LOG_INTERVAL=86400
ENV ROTATE_LOG_FILE_NAME=catalina
ENV NCE_APPNAME=api-gateway-portal
ENV CRON_TASK="30 * * * *"
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
WORKDIR /usr/local/tomcat
RUN rm -rf webapps/* \
    && mkdir webapps/ROOT
#    && sed -i '0,/3manager.org.apache.juli.FileHandler.prefix/s//3manager.org.apache.juli.FileHandler.maxDays = $LOGROTATE_LOGDAY\n&/' $CATALINA_HOME/conf/logging.properties \
#    && sed -i '/3manager.org.apache.juli.FileHandler.directory/c\3manager.org.apache.juli.FileHandler.directory = $LOGROTATE_LOGPATH' $CATALINA_HOME/conf/logging.properties
COPY gateway-portal-backend/build bin/
COPY gateway-portal-backend/target/*.war webapps/ROOT
RUN cd webapps/ROOT \
    && jar -xvf $(ls | grep '.war') \
    && rm $(ls | grep '.war') \
    && chmod +x $CATALINA_HOME/bin/docker-entrypoint.sh
ENTRYPOINT ["bin/docker-entrypoint.sh"]
