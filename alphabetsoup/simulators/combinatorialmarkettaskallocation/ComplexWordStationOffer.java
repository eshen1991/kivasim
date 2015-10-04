/**
 * 
 */
package alphabetsoup.simulators.combinatorialmarkettaskallocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import alphabetsoup.framework.Circle;
import alphabetsoup.framework.Letter;
import alphabetsoup.framework.Word;
import alphabetsoup.framework.WordStation;

/**
 * @author Chris Hazard
 *
 */
public class ComplexWordStationOffer {
	
	public BucketAgent bucket;
	
	
	public static class LetterDelivery {
		public WordStationAgent wordStation;
		public Word word;
		public Letter wordStationLetter;
		public Letter bucketLetter;
		public float value;
		public LetterDelivery(WordStationAgent wsa, Word w, Letter wsl, Letter bl, float value) {
			wordStation = wsa;	word = w;	wordStationLetter = wsl; bucketLetter = bl;	this.value = value;
		}
	}
	
	public static class Awarded {
		public BucketAgent bucket;
		public BucketbotAgent bucketbot;
		public List<LetterDelivery> letters = new ArrayList<LetterDelivery>();
		float value;
		float bucketbotValue;
	}
	
	
	public static class LetterOffer {
		public Letter letter;
		public float value;
		public LetterOffer(Letter l, float value) {
			letter = l;	this.value = value;
		}
	}
	
	public List<BucketbotAgent> bucketbots = new ArrayList<BucketbotAgent>();
	public List<Float> bucketbotCosts = new ArrayList<Float>();
	
	public List<LetterOffer> letterOffers = new ArrayList<LetterOffer>();
	
	private static float wordStationTravelCosts[];
	private static Circle wordStationTravelStartingLocations[];
	private static Map<WordStation, Integer> wordStationToIndex;
	
	public static float getTotalRevenue(Awarded a) {
		SimulationWorldMarketTaskAllocation sw = SimulationWorldMarketTaskAllocation.getSimulationWorld();
		return a.letters.size() * sw.economy.getWordCompletionLetterMarginalRevenue(); 
	}
	
	public static float getTotalCosts(Awarded a) {
		float total_cost = 0.0f;
		for(int i = 0; i < a.letters.size(); i++)
			total_cost += a.letters.get(i).value;
		return .1f * (total_cost + getTravelCosts(a));
	}
	
	public static float getTravelCosts(Awarded a) {
		if(a.bucketbot == null)
			return Float.POSITIVE_INFINITY;
		int entry = 0;
		for(int i = 0; i < a.letters.size(); i++)
			entry |= (1 << wordStationToIndex.get(a.letters.get(i).wordStation));
		if(entry == 0)
			return Float.POSITIVE_INFINITY;

		return wordStationTravelCosts[entry]
		                              + a.bucket.getDistance(wordStationTravelStartingLocations[entry])
		                              + a.bucketbot.bucketbot.getDistance(a.bucket);
	}
	
	public static void initializeWordStationTravelCosts(WordStation ws[]) {
		int num_entries = (1 << ws.length);
		wordStationTravelCosts = new float[num_entries];
		wordStationTravelStartingLocations = new Circle[num_entries];
		wordStationToIndex = new HashMap<WordStation, Integer>();
		for(int i = 0; i < ws.length; i++)
			wordStationToIndex.put(ws[i], i);
		
		for(int entry = 0; entry < num_entries; entry++) {
			float total_distance = 0.0f;
			Circle start = null;
			for(int i = 0; i < ws.length; i++) {
				//see if counting this current letter station
				// assume that we go in incremental order of word stations
				if((entry & (1 << i)) != 0) {
					
					//if it's the first WordStation come across, then just record it (no distance traveled)
					if(start == null) {
						start = (Circle)ws[i];
						wordStationTravelStartingLocations[entry] = start;
						continue;
					}
					
					//record distance, and keep position for later
					total_distance += start.getDistance((Circle)ws[i]);
					start = (Circle)ws[i];
				}
			}
			wordStationTravelCosts[entry] = total_distance;
		}
	}
}
