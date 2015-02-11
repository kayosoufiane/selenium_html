#Selenium HTML exemple for Jenkins 
 This is a test, to configure a server for Jenkins to Run Selenium HTML tests.
 
#### Requirements :
  - __Ubuntu Server 14.04__
  - __JDK 7__
 (exemple installation: _sudo apt-get install openjdk-7-jdk_)
  - __Tomcat 7__
 (exemple installation: _sudo apt-get install tomcat7_) (optional : tomcat7-docs tomcat7-admin tomcat7-examples)
  - __Jenkins__
 (exemple installation: _sudo apt-get install jenkins_)
  - __Xvfb__
 (exemple installation: _sudo apt-get install xvfb_)
  - __Firefox__
 (exemple installation: _sudo apt-get install firefox_)
    
#### Jenkins:
Install : __Selenium HTML report plugin__ and __EnvInject plugin__

- Log in to Jenkins, and create a *Maven 2/3* Job named *selenium_html*
- In the __global__ section (at the top of the configuration page), check __Prepare an environment for the job__, adding **DISPLAY=:20** for __Properties Content__
- In the __Source Code Management__ section, check __Git__, adding your Github URL to __Repository URL__. *(https://github.com/kayosoufiane/selenium_html)*
- In the __Build__ section, on one line, add to __Goals and options__:

    > clean integration-test -Dlog4j.configuration=file./src/test/resources/log4j.properties
- In the __Post-build Actions__ section, check __Publish Selenium HTML Report__
- Add the text *selenium_html/target/results* to the input for __Selenium test results location__
- Check __Set build result state to failure if an exception occurred while parsing results file__
- Click on __Save__
