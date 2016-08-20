# Tweet Storm

A simple storm application to get notified about tweets that include offensive terms associated to an specific 
token/concept/term and re-tweet those that are not.

## Usage

1) Clone the repo
```sh
$ git@github.com:francisco-perez-sorrosal/tweetstorm.git
```

2) Configure the Twitter OAuth and related config in `conf/config.properties` file


3) Compile the application and generate the jar file
```sh
$ mvn clean install assembly:single
```

4) Run the application
```sh
$ java -jar tweetstorm-1.0-SNAPSHOT.jar conf/config.properties
```

The application can be deployed also in Heroku. You can find more information 
in [this blog entry](http://francisco-perez-sorrosal.github.io/storm/twitter/omid/filter/heroku/howto/tutorial/2016/08/20/tweet-storm.html)