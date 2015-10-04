/**
 * 
 */
package alphabetsoup.simulators.combinatorialmarkettaskallocation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import alphabetsoup.framework.Circle;
import alphabetsoup.framework.Letter;
import alphabetsoup.framework.LetterType;
import alphabetsoup.framework.SimulationWorld;
import alphabetsoup.framework.Updateable;
import alphabetsoup.framework.Word;
import alphabetsoup.waypointgraph.Waypoint;

/**
 * @author Chris Hazard
 *
 */
public class Economy implements Updateable {
	
	public List<MultiItemDoubleAuction<LetterType, Letter, BucketAgent, WordStationAgent>> letterToWordMarkets
									= new ArrayList<MultiItemDoubleAuction<LetterType, Letter, BucketAgent, WordStationAgent>>();
	public List<MultiItemDoubleAuction<LetterType, Letter, LetterStationAgent, BucketAgent>> letterToBucketMarkets
									= new ArrayList<MultiItemDoubleAuction<LetterType, Letter, LetterStationAgent, BucketAgent>>(); 
	public List<MultiItemDoubleAuction<Waypoint, Waypoint, BucketbotAgent, BucketAgent>> transportationMarkets
									= new ArrayList<MultiItemDoubleAuction<Waypoint, Waypoint, BucketbotAgent, BucketAgent>>();
	public List<MultiItemDoubleAuction<Waypoint, Waypoint, BucketStorageAgent, BucketAgent>> storageMarkets
									= new ArrayList<MultiItemDoubleAuction<Waypoint, Waypoint, BucketStorageAgent, BucketAgent>>();
	
	public Map<Waypoint, MultiItemDoubleAuction<Waypoint, Waypoint, BucketbotAgent, BucketAgent>> storageToTransportationMarketMap
									= new HashMap<Waypoint, MultiItemDoubleAuction<Waypoint, Waypoint, BucketbotAgent, BucketAgent>>();
	
	public Map<MultiItemDoubleAuction<?, ?, ?, ?>, Circle> marketLocation = new HashMap<MultiItemDoubleAuction<?, ?, ?, ?>, Circle>();
	
	
	public Map<BucketAgent, ComplexWordStationOffer> wordStationOffers = new HashMap<BucketAgent, ComplexWordStationOffer>();
	
	private float wordCompletionBaseRevenue;
	private float wordCompletionLetterMarginalRevenue;
	
	private double curTime = 0.0;
	private double bidUpdateInterval = 0.5;
	private double lastBidUpdateTime = Double.NEGATIVE_INFINITY;
	
	//TODO need to add some way of finding the closest market to all waypoints...
	// perhaps pre-compute the closest for each waypoint in the system
	
	/**This function works around Java's picky generics system to add a new market to any of the lists
	 * @param <ItemType>
	 * @param <ItemInstanceType>
	 * @param <SellerType>
	 * @param <BuyerType>
	 * @param markets
	 * @param location
	 * @return
	 */
	private <ItemType, ItemInstanceType, SellerType, BuyerType>
	MultiItemDoubleAuction<ItemType, ItemInstanceType, SellerType, BuyerType>
			createSituatedMarket(List<MultiItemDoubleAuction<ItemType, ItemInstanceType, SellerType, BuyerType>> markets,
					Circle location) {
		MultiItemDoubleAuction<ItemType, ItemInstanceType, SellerType, BuyerType> m = new MultiItemDoubleAuction<ItemType, ItemInstanceType, SellerType, BuyerType>();
		markets.add(m);
		marketLocation.put(m, location);
		return m;
	}
	
	public MultiItemDoubleAuction<LetterType, Letter, BucketAgent, WordStationAgent> addLetterToWordMarket(Circle location) {
		return createSituatedMarket(letterToWordMarkets, location);
	}
	
	public MultiItemDoubleAuction<LetterType, Letter, LetterStationAgent, BucketAgent> addLetterToBucketMarket(Circle location) {
		return createSituatedMarket(letterToBucketMarkets, location);
	}
	
	public MultiItemDoubleAuction<Waypoint, Waypoint, BucketbotAgent, BucketAgent> addTransportationMarket(Waypoint location) {
		storageToTransportationMarketMap.put(location, createSituatedMarket(transportationMarkets, location));
		return storageToTransportationMarketMap.get(location);
	}
	
	public MultiItemDoubleAuction<Waypoint, Waypoint, BucketStorageAgent, BucketAgent> addStorageMarket(Circle location) {
		return createSituatedMarket(storageMarkets, location);
	}
	
	public MultiItemDoubleAuction<Waypoint, Waypoint, BucketStorageAgent, BucketAgent> getClosestStorageMarket(Waypoint location) {
		//TODO make this actually work for multiple situated markets
		return storageMarkets.get(0);
	}
	
	
	/** 
	 * @param word_completion_base_revenue
	 * @param word_completion_letter_marginal_revenue
	 */
	public Economy(float word_completion_base_revenue, float word_completion_letter_marginal_revenue) {
		wordCompletionBaseRevenue = word_completion_base_revenue;
		wordCompletionLetterMarginalRevenue = word_completion_letter_marginal_revenue;
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#getNextEventTime(double)
	 */
	public double getNextEventTime(double cur_time) {
		//return Double.POSITIVE_INFINITY;
		return lastBidUpdateTime + bidUpdateInterval;
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#update(double, double)
	 */
	public void update(double last_time, double cur_time) {
		curTime = cur_time;
		
		if(curTime >= lastBidUpdateTime + bidUpdateInterval) {
			lastBidUpdateTime = curTime;
			
			clearComplexWordStationOffers();
			
			//clear transportation markets
			{
				Set<BucketAgent> buyers = new HashSet<BucketAgent>();
				buyers.addAll(SimulationWorldMarketTaskAllocation.getSimulationWorld().bucketAgents);
				Set<BucketbotAgent> sellers = new HashSet<BucketbotAgent>();
				sellers.addAll(SimulationWorldMarketTaskAllocation.getSimulationWorld().bucketbotAgents);
				for(MultiItemDoubleAuction<Waypoint, Waypoint, BucketbotAgent, BucketAgent> m : transportationMarkets) {
					for(Exchange<Waypoint, BucketbotAgent, BucketAgent> e : m.clearMarkets(buyers, sellers)) {
						e.buyer.transportationBought(e);
						e.seller.transportationSold(e);
					}
				}
			}
			
			//clear letter to bucket markets
			{
				Set<BucketAgent> buyers = new HashSet<BucketAgent>();
				buyers.addAll(SimulationWorldMarketTaskAllocation.getSimulationWorld().bucketAgents);
				Set<LetterStationAgent> sellers = new HashSet<LetterStationAgent>();
				sellers.addAll(SimulationWorldMarketTaskAllocation.getSimulationWorld().letterStationAgents);
				for(MultiItemDoubleAuction<LetterType, Letter, LetterStationAgent, BucketAgent> m : letterToBucketMarkets) {
					for(Exchange<Letter, LetterStationAgent, BucketAgent> e : m.clearMarkets(buyers, sellers)) {
						e.buyer.letterBundleBought(e);
						e.seller.letterBundleSold(e);
					}
				}
			}
	
		}
		
	}
	
	public static class OutstandingLetter {
		public Letter letter;
		public Word word;
		public WordStationAgent wordStation;
		public OutstandingLetter(Letter letter, Word word, WordStationAgent wordStation) {
			this.letter = letter;	this.word = word;	this.wordStation = wordStation;
		}
	}
	
	public List<ComplexWordStationOffer.Awarded> getAwardsForWordStationOffers(List<OutstandingLetter> outstandingLetters,
			List<BucketbotAgent> outstandingBucketbots, List<ComplexWordStationOffer> offers) {
		//SimulationWorldMarketTaskAllocation sw = SimulationWorldMarketTaskAllocation.getSimulationWorld();
		List<ComplexWordStationOffer.Awarded> awards = new ArrayList<ComplexWordStationOffer.Awarded>();
		
		for(ComplexWordStationOffer co : offers) {
			//if out of buckebots, then only can use one if the bucket already has one
			if(outstandingBucketbots.size() == 0
					&& !(co.bucketbots.size() == 1 && co.bucketbotCosts.get(0) == 0.0f))
				continue;
			
			//find best bucketbot
			BucketbotAgent best_bucketbot = null;
			float cheapest_bucketbot_cost = Float.POSITIVE_INFINITY;
			for(int i = 0; i < co.bucketbots.size(); i++) {
				BucketbotAgent ba = co.bucketbots.get(i);
				if(!outstandingBucketbots.contains(ba))
					continue;
				if(co.bucketbotCosts.get(i) >= cheapest_bucketbot_cost)
					continue;
				cheapest_bucketbot_cost = co.bucketbotCosts.get(i);
				best_bucketbot = ba;
			}
			if(best_bucketbot == null)
				continue;
			
			ComplexWordStationOffer.Awarded award = new ComplexWordStationOffer.Awarded();
			
			for(ComplexWordStationOffer.LetterOffer lo : co.letterOffers) {
				//see if can find matching letter
				for(OutstandingLetter o : outstandingLetters) {
					if(o.letter.doesMatch(lo.letter)) {
						award.letters.add(new ComplexWordStationOffer.LetterDelivery(o.wordStation, o.word, o.letter, lo.letter, lo.value));
						outstandingLetters.remove(o);
						break;
					}
				}
			}
			
			//don't do anything if nothing won
			if(award.letters.size() == 0)
				continue;
			
			//grab a bucketbot
			award.bucketbot = best_bucketbot;
			outstandingBucketbots.remove(best_bucketbot);
			
			award.bucket = co.bucket;
			award.bucketbotValue = cheapest_bucketbot_cost;
			
			awards.add(award);
		}
		return awards;
	}
	
	public static class WordStationOfferResult {
		public List<OutstandingLetter> outstandingLetters = new ArrayList<OutstandingLetter>();
		public List<BucketbotAgent> outstandingBucketbots = new ArrayList<BucketbotAgent>();
		public List<ComplexWordStationOffer> offers = new ArrayList<ComplexWordStationOffer>();
		float profit = 0.0f;
	}
	
	public static class ComplexWordStationOfferComparator implements Comparator<ComplexWordStationOffer> {
		public int compare(ComplexWordStationOffer a, ComplexWordStationOffer b) {
			//first sort by if it has its own bucketbot
			if(a.bucketbots.size() == 1 && a.bucketbotCosts.get(0) == 0.0f)
				return -1;
			if(b.bucketbots.size() == 1 && b.bucketbotCosts.get(0) == 0.0f)
				return +1;
			
			//now sort by number of letters * probabliity
			float a_val = a.bucket.getLetters().size() * a.bucket.getProbabilityBucketContainsALetter();
			float b_val = b.bucket.getLetters().size() * b.bucket.getProbabilityBucketContainsALetter();
			
			if(a_val > b_val)
				return -1;
			return +1;
		}
	}
	
	public void clearComplexWordStationOffers() {
		SimulationWorldMarketTaskAllocation sw = SimulationWorldMarketTaskAllocation.getSimulationWorld();
		
		//build list of all outstanding letters
		List<OutstandingLetter> outstandingLetters = new ArrayList<OutstandingLetter>();
		for(WordStationAgent wsa : sw.wordStationAgents) {
			Map<Letter, Word> letters = wsa.getOutstandingLetters();
			for(Letter l : letters.keySet())
				outstandingLetters.add(new OutstandingLetter(l, letters.get(l), wsa));
		}
		
		//build list of outstanding bucketbots
		List<BucketbotAgent> outstandingBucketbots = new ArrayList<BucketbotAgent>();
		for(BucketbotAgent ba : sw.bucketbotAgents)
			if(ba.reservedBucket == null)
				outstandingBucketbots.add(ba);
		
		List<ComplexWordStationOffer> offers = new ArrayList<ComplexWordStationOffer>(wordStationOffers.values());
		
		Collections.sort(offers, new ComplexWordStationOfferComparator());
		
		float best_profit = Float.NEGATIVE_INFINITY;
		List<ComplexWordStationOffer.Awarded> best_awards = null;
		List<OutstandingLetter> bestOutstandingLetters = null;
		List<BucketbotAgent> bestOutstandingBucketbots = null;
		List<ComplexWordStationOffer> bestOffers = null;
		
		for(int i = 0; i < 300; i++) {
			List<OutstandingLetter> tempOutstandingLetters = new ArrayList<OutstandingLetter>(outstandingLetters);
			List<BucketbotAgent> tempOutstandingBucketbots = new ArrayList<BucketbotAgent>(outstandingBucketbots);
			List<ComplexWordStationOffer> tempOffers = new ArrayList<ComplexWordStationOffer>(offers);
			
			Random r = new Random(SimulationWorld.rand.nextLong());
			
			Collections.shuffle(tempOutstandingLetters, r);
			Collections.shuffle(tempOutstandingBucketbots, r);
			Collections.shuffle(tempOffers, r);
			List<ComplexWordStationOffer.Awarded> awards = getAwardsForWordStationOffers(tempOutstandingLetters, tempOutstandingBucketbots, tempOffers);
			float profit = 0.0f;
			for(ComplexWordStationOffer.Awarded a : awards) {
				profit -= ComplexWordStationOffer.getTotalCosts(a);
				profit += ComplexWordStationOffer.getTotalRevenue(a);
			}
			if(profit > best_profit) {
				best_profit = profit;
				best_awards = awards;
				bestOutstandingLetters = tempOutstandingLetters;
				bestOutstandingBucketbots = tempOutstandingBucketbots;
				bestOffers = tempOffers;
			}
		}
		
		outstandingLetters = bestOutstandingLetters;
		outstandingBucketbots = bestOutstandingBucketbots;
		offers = bestOffers;
		
		for(ComplexWordStationOffer.Awarded award : best_awards) {
			//sell transportation
			Exchange<Waypoint, BucketbotAgent, BucketAgent> te = new Exchange<Waypoint, BucketbotAgent, BucketAgent>
											(award.bucketbot, null, award.bucket, null, award.bucketbotValue);
			te.buyer.transportationBought(te);
			te.seller.transportationSold(te);
			
			//sell letters
			for(int i = 0; i < award.letters.size(); i++) {
				Exchange<Letter, BucketAgent, WordStationAgent> e = new Exchange<Letter, BucketAgent, WordStationAgent>
												(award.bucket, award.letters.get(i).bucketLetter,
														award.letters.get(i).wordStation,
														award.letters.get(i).wordStationLetter,
														award.letters.get(i).value);
				e.buyer.letterBought(e);
				e.seller.letterSold(e);
			}
		}
	}
	
	public List<String> getAdditionalInfo() {
		List<String> s = new ArrayList<String>();
		DecimalFormat four_digits = new DecimalFormat("0.000");
		
		SimulationWorldMarketTaskAllocation sw = SimulationWorldMarketTaskAllocation.getSimulationWorld();
		
		float total_profit = 0.0f;
		
		WordOrderManager wom = sw.wordManager;
		s.add("Word Order Manager Profit: " + four_digits.format(wom.getProfit()));
		total_profit += wom.getProfit();
		
		LetterManager lm = sw.letterManager;
		s.add("Letter Manager Profit: " + four_digits.format(lm.getProfit()));
		total_profit += lm.getProfit();
		
		BucketStorageAgent bsa = sw.resourceManager;
		s.add("Bucket Storage Agent Profit: " + four_digits.format(bsa.getProfit()));
		total_profit += bsa.getProfit();
		
		float ave_profit = 0.0f;
		for(BucketbotAgent ba : sw.bucketbotAgents)
			ave_profit += ba.getProfit();
		s.add("Total BucketbotAgent Profit: " + four_digits.format(ave_profit));
		total_profit += ave_profit;
		ave_profit /= sw.bucketbotAgents.size();
		s.add("Average BucketbotAgent Profit: " + four_digits.format(ave_profit));
		
		ave_profit = 0.0f;
		for(WordStationAgent wsa : sw.wordStationAgents)
			ave_profit += wsa.getProfit();
		s.add("Total WordStationAgent Profit: " + four_digits.format(ave_profit));
		total_profit += ave_profit;
		ave_profit /= sw.wordStationAgents.size();
		s.add("Average WordStationAgent Profit: " + four_digits.format(ave_profit));
		
		ave_profit = 0.0f;
		for(LetterStationAgent lsa : sw.letterStationAgents)
			ave_profit += lsa.getProfit();
		s.add("Total LetterStationAgent Profit: " + four_digits.format(ave_profit));
		total_profit += ave_profit;
		ave_profit /= sw.letterStationAgents.size();
		s.add("Average LetterStationAgent Profit: " + four_digits.format(ave_profit));
		
		ave_profit = 0.0f;
		for(BucketAgent ba : sw.bucketAgents)
			ave_profit += ba.getProfit();
		s.add("Total BucketAgent Profit: " + four_digits.format(ave_profit));
		total_profit += ave_profit;
		ave_profit /= sw.bucketbotAgents.size();
		s.add("Average BucketAgent Profit: " + four_digits.format(ave_profit));

		s.add("Total Profit:  " + four_digits.format(total_profit));
		
		float system_profit = sw.wordList.getCompletedWords().size() * wordCompletionBaseRevenue;
		int bundle_size = SimulationWorld.getSimulationWorld().letterStations[0].getBundleSize();
		float letter_profit = wordCompletionLetterMarginalRevenue - lm.getLetterBundleCost() / bundle_size;
		int num_letters_completed = 0;
		for(Word w : sw.wordList.getCompletedWords()) num_letters_completed += w.getOriginalLetters().length;
		system_profit += letter_profit * num_letters_completed;
		s.add("System Profit: " + four_digits.format(system_profit));
		
		return s;
	}
	
	/**Returns the revenue that is generated from completing the specified word.
	 * @param w
	 * @return
	 */
	public float getRevenueForCompletingWord(Word w) {
		return wordCompletionBaseRevenue + wordCompletionLetterMarginalRevenue * w.getOriginalLetters().length;
	}
	
	/**
	 * @return the wordCompletionBaseRevenue
	 */
	public float getWordCompletionBaseRevenue() {
		return wordCompletionBaseRevenue;
	}

	/**
	 * @return the wordCompletionLetterMarginalRevenue
	 */
	public float getWordCompletionLetterMarginalRevenue() {
		return wordCompletionLetterMarginalRevenue;
	}

	/**
	 * @return the marketLocation
	 */
	public Circle getMarketLocation(MultiItemDoubleAuction<?, ?, ?, ?> m) {
		return marketLocation.get(m);
	}
}
