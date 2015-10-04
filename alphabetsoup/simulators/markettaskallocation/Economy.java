/**
 * 
 */
package alphabetsoup.simulators.markettaskallocation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
			
			//clear letter to word markets
			{
				Set<WordStationAgent> buyers = new HashSet<WordStationAgent>();
				buyers.addAll(SimulationWorldMarketTaskAllocation.getSimulationWorld().wordStationAgents);
				Set<BucketAgent> sellers = new HashSet<BucketAgent>();
				sellers.addAll(SimulationWorldMarketTaskAllocation.getSimulationWorld().bucketAgents);
				for(MultiItemDoubleAuction<LetterType, Letter, BucketAgent, WordStationAgent> m : letterToWordMarkets) {
					for(Exchange<Letter, BucketAgent, WordStationAgent> e : m.clearMarkets(buyers, sellers)) {
						e.buyer.letterBought(e);
						e.seller.letterSold(e);
					}
				}
			}
			
			
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
