#!/usr/bin/env bash
activator clean stage
echo '123123'|sudo -S kill -9 `sudo lsof -t -i:9009`
echo '123123'|sudo -S rm -rf target/universal/stage/RUNNING_PID
target/universal/stage/bin/style-pay -Dhttp.port=9009 -Dconfig.file=target/universal/stage/conf/application.conf -Dlogger.resource=logback.xml
echo " - Finished Deploy"
