hornetq-rest-stress
===================

The world's most rudimentary stress-test application for the HornetQ-REST interface.

## Building
The project is built using Gradle.

``` gradle jar ```

Gradle also supports building the Eclipse .project metadata, if you prefer.

``` gradle eclipse ```

You can also produce a large hornetstress.jar file that contains all dependencies exploded along with the HornetHive classes like so:

``` gradle stressjar ```


## Running
The project has two dependencies. Make sure they are on your classpath, and run the HornetHive class.

The program requires one argument - the URL to the starting point from which you can create an auto-ack subscription:
``` java -cp ... -jar hornetq-rest-stress.jar http://localhost:8080/hornet-msg/topics/jms.topic.mycompany.mytopic/ ```

You can optionally specify a second argument - an integer which is the number of consumer threads you would like to spawn. The default is 100.


You may also use the convenient hornetstress.jar file that contains everything you require:


 ```java  -verbose -jar ./build/libs/hornetstress.jar     http://<hostname>:8080/hornet-msg/topics/jms.topic.<topic name>/ 100  ```


