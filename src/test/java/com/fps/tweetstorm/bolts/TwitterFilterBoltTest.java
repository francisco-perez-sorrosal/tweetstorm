package com.fps.tweetstorm.bolts;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.tuple.Tuple;
import com.google.gson.JsonObject;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import twitter4j.TwitterException;

import java.util.HashMap;
import java.util.List;

import static com.fps.tweetstorm.bolts.TwitterFilterBolt.OFFENSIVE_TWEET_OUTPUT;
import static com.fps.tweetstorm.bolts.TwitterFilterBolt.TWEETER_TEXT;
import static com.fps.tweetstorm.bolts.TwitterFilterBolt.TWEETER_TWEET_ID;
import static com.fps.tweetstorm.bolts.TwitterFilterBolt.TWEETER_USER;
import static com.fps.tweetstorm.bolts.TwitterFilterBolt.TWEETER_USER_ID;
import static com.fps.tweetstorm.bolts.TwitterFilterBolt.TWEETER_USER_SCREEN_NAME;
import static com.fps.tweetstorm.spouts.TwitterSuckerSpout.RAW_TWEET_STORM_F;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Created by fperezsorrosal on 7/14/16.
 */
public class TwitterFilterBoltTest {

    public static final String OFFENSIVE_TERMS = "kk,sex";

    private TwitterFilterBolt bolt;

    private JsonObject tweeter = new JsonObject();
    private JsonObject tweet = new JsonObject();

    @Mock
    private TopologyContext topologyContext;

    @BeforeMethod
    public void before() throws TwitterException {
        MockitoAnnotations.initMocks(this);

        bolt = spy(new TwitterFilterBolt(new TwitterFilterBolt.Config(OFFENSIVE_TERMS)));
        bolt.prepare(new HashMap<>(), topologyContext);

        // Prepare json objects
        tweeter.addProperty(TWEETER_USER_ID, "54321");
        tweeter.addProperty(TWEETER_USER_SCREEN_NAME, "fperezsorrosal");

        tweet.addProperty(TWEETER_TWEET_ID, "12345");
        tweet.add(TWEETER_USER, tweeter);

    }

    @Test
    public void testJSonRepresentationOfATweetIsOK() {

        final String TWEET_TEXT = "This is a tweet";
        tweet.addProperty(TWEETER_TEXT, TWEET_TEXT);

        String tweetAsString = "{\"id_str\":\"12345\",\"text\":\"" + TWEET_TEXT + "\",\"user\":{\"id_str\":\"54321\",\"screen_name\":\"fperezsorrosal\"}}";

        Tuple tuple = mock(Tuple.class);
        when(tuple.getStringByField(RAW_TWEET_STORM_F)).thenReturn(tweetAsString);
        JsonObject jsonObject = bolt.getJsonRepresentation(tuple);

        assertEquals(jsonObject, this.tweet);

    }

    @Test
    public void testGoodTweet() {

        tweet.addProperty(TWEETER_TEXT, "Tweet with good words!");

        Tuple tuple = mock(Tuple.class);

        doReturn(tweet).when(bolt).getJsonRepresentation(tuple);
        BasicOutputCollector collector = mock(BasicOutputCollector.class);

        bolt.execute(tuple, collector);

        ArgumentCaptor<List<Object>> argument = ArgumentCaptor.forClass((Class) List.class);
        verify(collector).emit(eq(TwitterFilterBolt.NON_OFFENSIVE_TWEET_OUTPUT), argument.capture());
        assertEquals(argument.getValue().get(0), tweet.get(TWEETER_TWEET_ID).getAsString());
        assertEquals(argument.getValue().get(1), tweet.get(TWEETER_TEXT).getAsString());
        assertEquals(argument.getValue().get(2), tweeter.get(TWEETER_USER_ID).getAsString());

    }

    @Test
    public void testBadTweet() {

        tweet.addProperty("text", "Tweet with bad words e.g. sex!");

        Tuple tuple = mock(Tuple.class);

        doReturn(tweet).when(bolt).getJsonRepresentation(tuple);
        BasicOutputCollector collector = mock(BasicOutputCollector.class);

        bolt.execute(tuple, collector);

        ArgumentCaptor<List<Object>> argument = ArgumentCaptor.forClass((Class) List.class);
        verify(collector).emit(eq(OFFENSIVE_TWEET_OUTPUT), argument.capture());
        assertEquals(argument.getValue().get(0), tweet.get(TWEETER_TWEET_ID).getAsString());
        assertEquals(argument.getValue().get(1), tweet.get(TWEETER_TEXT).getAsString());
        assertEquals(argument.getValue().get(2), tweeter.get(TWEETER_USER_SCREEN_NAME).getAsString());

    }

}
