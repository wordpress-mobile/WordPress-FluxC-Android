## Setting up example app api & test properties

We use gradle property files to store sensitive information under the git ignored `example/properties/` folder.
There are 2 ways to set these up:

1. If you work at Automattic, you should have a mobile secrets folder which should contain all the necessary properties.
You can run `./gradlew applyConfiguration` from the root of the project to copy these.
2. Otherwise, you can copy the `properties-example` folder with the following command and use your own credentials:
    $ cp -a example/properties-example/ example/properties/

## Deprecated Properties

We used to store the api credentials in the `example/gradle.properties` and the test properties in `example/tests.properties`.
We decided to deprecate this approach to make it harder to accidentally push secrets into the project. 
Utilizing a git ignored folder should help us avoid accidentally pushing new credential files into the repo. 

*IMPORTANT: Please remove `example/tests.properties` and `example/tests.properties-extra` files and `wp.OAUTH.APP.ID` & `wp.OAUTH.APP.SECRET` properties from `example/gradle.properties` file.*
