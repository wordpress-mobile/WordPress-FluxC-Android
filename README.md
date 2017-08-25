# WordPress-FluxC-Android

[![Build Status](https://travis-ci.org/wordpress-mobile/WordPress-FluxC-Android.svg?branch=develop)](https://travis-ci.org/wordpress-mobile/WordPress-FluxC-Android)

WordPress-FluxC-Android is a networking and persistence library that helps to connect and sync data from a WordPress site (self hosted, or wordpress.com site).

Based on the [Flux][1] pattern, we're using: [Dagger2][2] for dependency injection, [WellSql][3] for persistence.

FluxC is pronounced ‘fluxy’, and stands for ‘Flux Capacitor’. This makes a double reference to the architecture model (since a capacitor is a kind of storage unit, or ‘store’). More importantly, a flux capacitor is the core component of the DeLorean time machine in [Back to the Future](https://en.wikipedia.org/wiki/Back_to_the_Future), which ‘makes time travel possible.’

**Most of our documentation for using and contributing to FluxC can be found in our [wiki](https://github.com/wordpress-mobile/WordPress-FluxC-Android/wiki).**

## Using FluxC as a library

Please see our [Using FluxC](https://github.com/wordpress-mobile/WordPress-FluxC-Android/wiki/Using-FluxC#initial-setup) wiki page for setup instructions.

## Building the library

The gradle build system will fetch all dependencies and generate
files you need to build the project. You first need to generate the
local.properties (replace YOUR_SDK_DIR with your actual android SDK directory)
file and create the gradle.properties file. The easiest way is to copy
our example:

    $ echo "sdk.dir=YOUR_SDK_DIR" > local.properties
    $ ./gradlew fluxc:build

## Building and running tests and the example app

    $ cp example/gradle.properties-example example/gradle.properties
    $ cp example/tests.properties-example example/tests.properties
    $ ./gradlew cAT       # Regression tests
    $ ./gradlew testDebug # Unit tests

Note: this is the default `example/gradle.properties` file. You'll have to get
a WordPress.com OAuth2 ID and secret.

We have some tests connecting to real HTTP servers, URL and credentials are defined in `example/tests.properties`, you must edit it or obtain the real file to run the tests. This is temporary.

## Setting up Checkstyle

The FluxC project uses [Checkstyle](http://checkstyle.sourceforge.net/). You can run checkstyle using `./gradlew checkstyle`.

You can also view errors and warnings in realtime by installing the CheckStyle-IDEA plugin in Android Studio here:

`Android Studio > Preferences... > Plugins > CheckStyle-IDEA`

Once installed, you can configure the plugin here:

`Android Studio > Preferences... > Other Settings > Checkstyle`

From there, add and enable the configuration file for FluxC, located at [config/checkstyle.xml](https://github.com/wordpress-mobile/WordPress-FluxC-Android/blob/develop/config/checkstyle.xml).

### Compatibility

Checkstyle 8.1 breaks backward compatibility with previous versions including the current configuration file.  Be sure to target Checkstyle 8.0 or lower when configuring the CheckStyle-IDEA plugin in Android Studio. If you see the following error(s), check your plugin target.

    org.infernus.idea.checkstyle.exception.CheckStylePluginException: <html><b>The Checkstyle rules file could not be parsed.</b><br>SuppressionCommentFilter is not allowed as a child in Checker<br>The file has been blacklisted for 60s.</html>
	    at org.infernus.idea.checkstyle.checker.CheckerFactory.blacklistAndShowMessage(CheckerFactory.java:197)
	    at org.infernus.idea.checkstyle.checker.CheckerFactory.blacklistAndShowMessage(CheckerFactory.java:213)
	    at org.infernus.idea.checkstyle.checker.CheckerFactory.createChecker(CheckerFactory.java:145)
	    ...

<img src="https://user-images.githubusercontent.com/3827611/29678543-38a5d492-88bc-11e7-8271-45c7d4946fd4.png" alt="Android Studio Checkstyle Error Message" width="400" />

## Contributing

### Actions

Each store should have a corresponding enum defining actions for that store. For example, [SiteStore][4]'s actions are defined in the [SiteAction][5] enum.

Action naming guide:

    FETCH_X - request data from the server
    PUSH_X - send data to the server
    UPDATE_X - local change
    REMOVE_X - local remove
    DELETE_X - request deletion on the server

Each action enum should be annotated with `@ActionEnum`, with individual actions receiving an `@Action` annotation with an optional `payloadType` setting (see [SiteAction][5] for an example).

### Endpoints

Endpoints for each of the supported APIs are centralized in a generated endpoint file: `WPCOMREST.java` and `XMLRPC.java` (also `WPAPI.java`).

To add a new endpoint, first add it to the appropriate `fluxc/src/main/tools/*.txt` file, and then rebuild the project to update the generated (Java) endpoint file.

Note that, for WordPress.com REST API endpoints, the final endpoint will be normalized to include a trailing slash.

### On Changed Events

All On Changed Events extend the OnChanged class. They encapsulate an `error`
field. Events can be checked for an error by calling `event.isError()`.

On Changed Events naming guide:

    onXChanged(int rowsAffected) - Keep X singular even if multiple X were changed
    onXRemoved(int rowsAffected) - Keep X singular even if multiple X were removed

## Need help to build or hack?

Say hello on our [Slack][6] channel: `#mobile`.

## LICENSE

WordPress-FluxC-Android is an Open Source project covered by the [GNU General Public License version 2](LICENSE.md).

[1]: https://facebook.github.io/flux/docs/overview.html
[2]: https://google.github.io/dagger/
[3]: https://github.com/yarolegovich/wellsql
[4]: https://github.com/wordpress-mobile/WordPress-FluxC-Android/blob/ba9dd84c54b12d53e01dfdb8efb4a18ed8343311/fluxc/src/main/java/org/wordpress/android/fluxc/store/SiteStore.java
[5]: https://github.com/wordpress-mobile/WordPress-FluxC-Android/blob/ba9dd84c54b12d53e01dfdb8efb4a18ed8343311/fluxc/src/main/java/org/wordpress/android/fluxc/action/SiteAction.java
[6]: https://make.wordpress.org/chat/
