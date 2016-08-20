package com.fps.tweetstorm.bolts;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.fps.tweetstorm.bolts.TwitterFilterBolt.TWEET_ID_STORM_F;
import static com.fps.tweetstorm.bolts.TwitterFilterBolt.TWEET_TEXT_STORM_F;
import static com.fps.tweetstorm.bolts.TwitterFilterBolt.TWEET_WRITER_ID_STORM_F;

/**
 * Created by fperezsorrosal on 07/12/16
 */
public class WatchDogBolt extends BaseBasicBolt {

    private static final Logger LOG = LoggerFactory.getLogger(WatchDogBolt.class);
    private static final String NOTIFY_MSG = "User @%s sent offensive tweet: %s";
    public static final String WATCHDOG_BOLT_NAME = "watchDog";

    private TwitterFactory factory;
    private Twitter twitter;
    private List<String> watchmen;

    public WatchDogBolt(Config config) throws TwitterException {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
          .setOAuthConsumerKey(config.consumerKey)
          .setOAuthConsumerSecret(config.consumerSecret)
          .setOAuthAccessToken(config.token)
          .setOAuthAccessTokenSecret(config.secret);
        this.factory = new TwitterFactory(cb.build());
        this.twitter = factory.getInstance();
        watchmen = config.watchmen;
    }

    @Override
    public void execute(Tuple tuple, BasicOutputCollector collector) {
        String tweetId = tuple.getStringByField(TWEET_ID_STORM_F);
        String tweet = tuple.getStringByField(TWEET_TEXT_STORM_F);
        String offensiveSender = tuple.getStringByField(TWEET_WRITER_ID_STORM_F);

        for (String watcher : watchmen) {
            try {
                twitter.sendDirectMessage(watcher, String.format(NOTIFY_MSG, offensiveSender, tweet));
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {

    }

    public static class Config implements Serializable {
        private static final long serialVersionUID = 5276718733211623335L;
        public final String consumerKey;
        public final String consumerSecret;
        public final String token;
        public final String secret;
        public final List<String> watchmen;

        public Config(String consumerKey,
                      String consumerSecret,
                      String token,
                      String secret,
                      String interestedPeople) {
            this.consumerKey = consumerKey;
            this.consumerSecret = consumerSecret;
            this.token = token;
            this.secret = secret;
            this.watchmen = Pattern.compile(",")
                                   .splitAsStream(interestedPeople)
                                   .parallel().map(String::toLowerCase)
                                   .collect(Collectors.toList());
        }
    }

}