import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.Location;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import eventstore.*;
import eventstore.j.EventDataBuilder;
import eventstore.j.SettingsBuilder;
import eventstore.j.WriteEventsBuilder;
import eventstore.proto.EventStoreMessages;
import eventstore.tcp.ConnectionActor;
import com.jcabi.log.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SampleStream {

    public void run(String consumerKey, String consumerSecret, String token, String secret) throws InterruptedException, IOException {

        BlockingQueue<String> queue = new LinkedBlockingQueue<String>(1000);

        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
        //endpoint.trackTerms(Lists.newArrayList("#WednesdayWisdom"));
        //endpoint.locations(Lists.newArrayList(new Location(new Location.Coordinate(-122.75, 36.8), new Location.Coordinate(-121.75, 37.8))));
        endpoint.locations(Lists.newArrayList(new Location(new Location.Coordinate(-121.113, 27.817), new Location.Coordinate(-63.544, 46.843))));

        Authentication auth = new OAuth1(consumerKey, consumerSecret, token, secret);
        //Authentication auth = new com.twitter.hbc.httpclient.auth.BasicAuth(username, password);

        BasicClient client = new ClientBuilder()
                .name("sampleExampleClient")
                //.name("python-twitter-test-adb-grp6")
                .hosts(Constants.STREAM_HOST)
                .endpoint(endpoint)
                .authentication(auth)
                .processor(new StringDelimitedProcessor(queue))
                .build();

        client.connect();

        Logger.info(this, "msgQueue length" + queue.size());

        final Settings settings = new SettingsBuilder().address(
                new InetSocketAddress("127.0.0.1", 1113))
                .defaultCredentials("admin", "changeit")
                .build();

        final ActorSystem system = ActorSystem.create();
        final ActorRef connection = system.actorOf(ConnectionActor.getProps(settings));
        ActorRef writeResult = system.actorOf(Props.create(WriteResult.class));


        List<EventData> events = new ArrayList<EventData>();
        int count = 0;

        //NLP.init();
        //Gson gson = new GsonBuilder().setPrettyPrinting().create();

        while (!client.isDone() && count < 50) {
            for (int msgRead = 0; msgRead < 10; msgRead++) {
                String msg = queue.take();
                count++;
                Logger.info(this,"Tweet" + msgRead + " --> " + msg);

                //JsonObject jsonObject = gson.fromJson( msg, JsonObject.class);
                //  String sentimentText = jsonObject.get("text").toString();

//                0 - very Negative
//                1 - Negative
//                2 - neutral
//                3 - positive
//                4 - veryPositive

                //   slf4jLogger.info("Tweet" + msgRead + " --> " + NLP.findSentiment(sentimentText));

                events.add(new EventDataBuilder("sampleEvent").eventId(UUID.randomUUID()).jsonData(msg.trim()).build());
            }
            WriteEvents writeEvents = new WriteEventsBuilder("TweetStream11").addEvents(events).expectAnyVersion().build();
            if (writeResult.isTerminated()) {
                writeResult = system.actorOf(Props.create(WriteResult.class));
            }

            connection.tell(writeEvents, writeResult);
            events.clear();
        }
        client.stop();
        system.terminate();

        Logger.info(this, "The client read %d messages!\n", client.getStatsTracker().getNumMessages());
    }

    public static class WriteResult extends UntypedActor {

        final LoggingAdapter esLogger = Logging.getLogger(getContext().system(), this);

        public void onReceive(Object message) throws Exception {
            if (message instanceof EventStoreMessages.WriteEventsCompleted) {
                final WriteEventsCompleted completed = (WriteEventsCompleted) message;
                esLogger.info("range: {}, position: {}", completed.numbersRange(), completed.position());
            } else if (message instanceof Status.Failure) {
                final Status.Failure failure = ((Status.Failure) message);
                final EsException exception = (EsException) failure.cause();
                esLogger.error(exception, exception.toString());
            } else
                unhandled(message);
        }
    }

    public static void main(String[] args) {
        try {
            SampleStream sampleStream = new SampleStream();
            final long start = System.nanoTime();

            Properties prop = new Properties();
            InputStream input = null;
            String filename = "twitter.properties";

            input = SampleStream.class.getClassLoader().getResourceAsStream(filename);
            if (input == null) {
                Logger.info(SampleStream.class, "unable to find " + filename, System.nanoTime() - start);
                return;
            }
            prop.load(input);

            sampleStream.run(prop.getProperty("oauth.consumerKey"), prop.getProperty("oauth.consumerSecret"), prop.getProperty("oauth.accessToken"), prop.getProperty("oauth.accessTokenSecret"));

        } catch (InterruptedException e) {
            Logger.error(SampleStream.class, e.toString());
        } catch (IOException e) {
            Logger.error(SampleStream.class, e.toString());
        }
    }
}
