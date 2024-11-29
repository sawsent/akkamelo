This is the Gatling test suite for the Akkamelo API. It was taken from the Rinha de backend contest, so everything is in portuguese.
I plan on refactoring this to english, but it is not a priority right now. 
It should run, since the external contracts of the API are tailored for this suite.

## Running the tests
You can run the test suite with the following command:
```bash
mvn test -Pgatling-suite
```
This will run the tests and generate a report in the `target/gatling` directory.

You can also run from your IDE if you wish, running the `GatlingRunner` class.

Keep in mind all the tests need to compile.

Check pom.xml for more information on the profile.