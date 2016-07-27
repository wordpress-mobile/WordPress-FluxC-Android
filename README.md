# WordPress-Stores-Android

[![Build Status](https://travis-ci.org/wordpress-mobile/WordPress-Stores-Android.svg?branch=develop)](https://travis-ci.org/wordpress-mobile/WordPress-Stores-Android)

WordPress-Stores-Android is a networking and persistence library that helps to connect and sync data from a WordPress site (self hosted, or wordpress.com site). It's not ready for prime time yet.

Based on the [Flux][1] pattern, we're using: [Dagger2][2] for dependency injection, [WellSql][3] for persistence.

## Building the library

The gradle build system will fetch all dependencies and generate
files you need to build the project. You first need to generate the
local.properties (replace YOUR_SDK_DIR with your actual android SDK directory)
file and create the gradle.properties file. The easiest way is to copy
our example:

    $ echo "sdk.dir=YOUR_SDK_DIR" > local.properties
    $ ./gradlew WordPressStores:build

## Building and running tests and the example app

    $ cp example/gradle.properties-example example/gradle.properties
    $ cp example/tests.properties-example example/tests.properties
    $ ./gradlew cAT       # Regression tests
    $ ./gradlew testDebug # Unit tests

Note: this is the default `example/gradle.properties` file. You'll have to get
a WordPress.com OAuth2 ID and secret.

We have some tests connecting to real HTTP servers, URL and credentials are defined in `example/tests.properties`, you must edit it or obtain the real file to run the tests. This is temporary.

## Naming conventions for actions and events

### Actions

Each store should have a corresponding enum defining actions for that store. For example, [SiteStore][4]'s actions are defined in the [SiteAction][5] enum.

Action naming guide:

    FETCH_X - request data from the server
    PUSH_X - send data to the server
    UPDATE_X - local change
    REMOVE_X - local remove
    DELETE_X - request deletion on the server

Each action enum should be annotated with `@ActionEnum`, with individual actions receiving an `@Action` annotation with an optional `payloadType` setting (see [SiteAction][5] for an example).

### Events

Events naming guide:

    onXChanged(int rowsAffected) - Keep X singular even if multiple X were changed
    onXRemoved(int rowsAffected) - Keep X singular even if multiple X were removed

## Need help to build or hack?

Say hello on our [Slack][6] channel: `#mobile`.

## LICENSE

WordPress-Stores-Android is an Open Source project covered by the [GNU General Public License version 2](LICENSE.md).

[1]: https://facebook.github.io/flux/docs/overview.html
[2]: https://google.github.io/dagger/
[3]: https://github.com/yarolegovich/wellsql
[4]: https://github.com/wordpress-mobile/WordPress-Stores-Android/blob/52ffa86d604f3f2df1b46bc3e9f20f7552ceeea5/WordPressStores/src/main/java/org/wordpress/android/stores/store/SiteStore.java
[5]: https://github.com/wordpress-mobile/WordPress-Stores-Android/blob/52ffa86d604f3f2df1b46bc3e9f20f7552ceeea5/WordPressStores/src/main/java/org/wordpress/android/stores/action/SiteAction.java
[6]: https://make.wordpress.org/chat/