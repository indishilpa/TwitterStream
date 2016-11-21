import akka.actor.ActorSystem;
import eventstore.Event;
import eventstore.IndexedEvent;
import eventstore.SubscriptionObserver;
import eventstore.j.EsConnection;
import eventstore.j.EsConnectionFactory;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

public class eventReader {
	private static Map<String, Integer> hashtagCount;
	
	public static void main(String args[]){
		final ActorSystem system = ActorSystem.create();
		final EsConnection connection = EsConnectionFactory.create(system);
		StateInfo statesInfo[] = {new StateInfo("Alabama","AL"),new StateInfo("Alaska","AK"),new StateInfo("Arizona","AZ"),new StateInfo("Arkansas","AR"),new StateInfo("California","CA"),new StateInfo("Colorado","CO"),new StateInfo("Connecticut","CT"),new StateInfo("Delaware","DE"),new StateInfo("Florida","FL"),new StateInfo("Georgia","GA"),new StateInfo("Hawaii","HI"),new StateInfo("Idaho","ID"),new StateInfo("Illinois","IL"),new StateInfo("Indiana","IN"),new StateInfo("Iowa","IA"),new StateInfo("Kansas","KS"),new StateInfo("Kentucky","KY"),new StateInfo("Louisiana","LA"),new StateInfo("Maine","ME"),new StateInfo("Maryland","MD"),new StateInfo("Massachusetts","MA"),new StateInfo("Michigan","MI"),new StateInfo("Minnesota","MN"),new StateInfo("Mississippi","MS"),new StateInfo("Missouri","MO"),new StateInfo("Montana","MT"),new StateInfo("Nebraska","NE"),new StateInfo("Nevada","NV"),new StateInfo("New Hampshire","NH"),new StateInfo("New Jersey","NJ"),new StateInfo("New Mexico","NM"),new StateInfo("New York","NY"),new StateInfo("North Carolina","NC"),new StateInfo("North Dakota","ND"),new StateInfo("Ohio","OH"),new StateInfo("Oklahoma","OK"),new StateInfo("Oregon","OR"),new StateInfo("Pennsylvania","PA"),new StateInfo("Rhode Island","RI"),new StateInfo("South Carolina","SC"),new StateInfo("South Dakota","SD"),new StateInfo("Tennessee","TN"),new StateInfo("Texas","TX"),new StateInfo("Utah","UT"),new StateInfo("Vermont","VT"),new StateInfo("Virginia","VA"),new StateInfo("Washington","WA"),new StateInfo("West Virginia","WV"),new StateInfo("Wisconsin","WI"),new StateInfo("Wyoming","WY"),new StateInfo("American Samoa","AS"),new StateInfo("District of Columbia","DC"),new StateInfo("Federated States of Micronesia","FM"),new StateInfo("Guam","GU"),new StateInfo("Marshall Islands","MH"),new StateInfo("Northern Mariana Islands","MP"),new StateInfo("Palau","PW"),new StateInfo("Puerto Rico","PR"),new StateInfo("Virgin Islands","VI")};
		hashtagCount = new HashMap<String, Integer>();
		
		for(final StateInfo state : statesInfo){
			connection.subscribeToStream(state.getAbrv(), new SubscriptionObserver<Event>() {
				public void onLiveProcessingStart(Closeable subscription) {
					system.log().info("live processing started");
				}

				public void onError(Throwable e) {
					system.log().error(e.toString());
				}

				public void onClose() {
					system.log().error("subscription closed");
				}

				public void onEvent(Event event, Closeable arg1) {
					if(event.data().eventType().contains("events_by_state")){
						String data = event.data().data().value().decodeString("US-ASCII");
						String hashtagData = data.substring(data.indexOf("hashtags") + 11, data.length() - 2);
						String hashtags[] = hashtagData.split(",");
						for(String hashtagInfo : hashtags){
							String hashtag = hashtagInfo.split(":")[0];
							Integer count = Integer.parseInt(hashtagInfo.split(":")[1]);
							if(hashtagCount.containsKey(hashtag)){
								hashtagCount.put(hashtag, hashtagCount.get(hashtag) + count);
							}else{
								hashtagCount.put(hashtag, count);
							}
							state.addHashtag(hashtag, count);
							system.log().info(state.getAbrv() + " " + hashtag + " " + state.hashtags.get(hashtag));
							system.log().info( hashtag + " " + hashtagCount.get(hashtag));
						}						
					}					
				}
			}, false, null);
		}
	}
}
