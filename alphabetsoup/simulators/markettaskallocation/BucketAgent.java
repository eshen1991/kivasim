/**
 * 
 */
package alphabetsoup.simulators.markettaskallocation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import alphabetsoup.base.BucketBase;
import alphabetsoup.framework.Circle;
import alphabetsoup.framework.Letter;
import alphabetsoup.framework.LetterType;
import alphabetsoup.framework.SimulationWorld;
import alphabetsoup.framework.Updateable;
import alphabetsoup.framework.WordList;
import alphabetsoup.waypointgraph.Waypoint;

/**
 * @author Chris Hazard
 *
 */
public class BucketAgent extends BucketBase implements Updateable {
	
	public BucketAgent(float bucket_radius, int bucket_capacity) {
		super(bucket_radius, bucket_capacity);
	}
	
	private double curTime = 0.0;
	private double bidUpdateInterval = 0.5;
	private double lastBidUpdateTime = Double.NEGATIVE_INFINITY;
	
	private double profit = 0.0;
	
	private BucketbotAgent assignedBucketbot = null;
	private Waypoint assignedStorage = null;
	
	static public class Delivery {
		public Letter letterToDeliver;
		public Letter letterSlotToDeliver;
		public WordStationAgent wordStationAgent;
		public Delivery(Letter letterToDeliver, Letter letterSlotToDeliver, WordStationAgent wordStationAgent) {
			this.letterToDeliver = letterToDeliver; this.letterSlotToDeliver = letterSlotToDeliver;
			this.wordStationAgent = wordStationAgent;
		}
	}
	/**Contains the list of letters that are allocated to be delivered
	 */
	public List<Delivery> lettersToDeliver = new ArrayList<Delivery>();
	
	static public class Pickup {
		public Letter letterToPickUp;
		public LetterStationAgent letterStationAgent;
		public Pickup(Letter letterToPickUp, LetterStationAgent letterStationAgent) {
			this.letterToPickUp = letterToPickUp;	this.letterStationAgent = letterStationAgent;
		}
	}
	/**Contains the list of letters that are allocated to be delivered
	 */
	public List<Pickup> lettersToPickup = new ArrayList<Pickup>();
	
	/**Contains the letters in the bucket that have not been allocated to  a word (sold on the market)
	 */
	protected HashSet<Letter> unassignedLetters = new HashSet<Letter>();
	
	/* (non-Javadoc)
	 * @see alphabetsoup.base.BucketBase#addLetter(alphabetsoup.framework.Letter)
	 */
	public void addLetter(Letter l) {
		super.addLetter(l);
		//mark so that we know it hasn't been allocated yet
		unassignedLetters.add(l);
	}
	
	/**Should be called when the BucketAgent is allocated a letter to deliver to a word station
	 * @param e
	 */
	public void letterSold(Exchange<Letter, BucketAgent, WordStationAgent> e) {
		lettersToDeliver.add(new Delivery(e.sellerItem, e.buyerItem, e.buyer));
		profit += e.value;
		unassignedLetters.remove(e.sellerItem);
		
		rebidOnWordStationLetterRequests();
		rebidOnLetterPickupRequests();
	}
	
	/**Should be called when the BucketAgent is allocated a letterbundle from a letter station
	 * @param e
	 */
	public void letterBundleBought(Exchange<Letter, LetterStationAgent, BucketAgent> e) {
		lettersToPickup.add(new Pickup(e.sellerItem, e.seller));
		profit -= e.value;
		
		rebidOnWordStationLetterRequests();
		rebidOnLetterPickupRequests();
	}
	
	/**Should be called when the BucketAgent is allocated transportation
	 * @param e
	 */
	public void transportationBought(Exchange<Waypoint, BucketbotAgent, BucketAgent> e) {
		assignedBucketbot = e.seller;
		profit -= e.value;
		
		rebidOnWordStationLetterRequests();
		rebidOnLetterPickupRequests();
	}
	
	/**Returns the best delivery task that the bucket has
	 * @return
	 */
	public Delivery takeBestDelivery() {
		//TODO should these be sorted by revenue?  or sorted by trip?  or both?
		if(lettersToDeliver.size() > 0) {
			int delivery_index = 0;
			if(assignedBucketbot.currentDeliveryTask != null) {
				WordStationAgent w = assignedBucketbot.currentDeliveryTask.wordStationAgent;
				for(int i = 0; i < lettersToDeliver.size(); i++)
					if(lettersToDeliver.get(i).wordStationAgent == w) {
						delivery_index = i;
						break;
					}
			}
			
			//see if closer to the first pickup... if so, return null to do a pickup
			if(lettersToPickup.size() > 0
					&& getDistance(lettersToDeliver.get(delivery_index).wordStationAgent)
											> getDistance(lettersToPickup.get(0).letterStationAgent)) {
				//make sure it has ample space to take a new letter before deciding to do so
				int bundle_size = SimulationWorld.getSimulationWorld().letterStations[0].getBundleSize();
				if(getCapacity() - getLetters().size() > bundle_size)
					return null;
			}
			
			return lettersToDeliver.remove(delivery_index);
		}
		return null;
	}
	
	/**Should be called if a delivery fails
	 * @param d
	 */
	public void deliveryFailed(Delivery d) {
		lettersToDeliver.add(d);
	}
	
	/**Returns the best pickup task that the bucket has
	 * @return
	 */
	public Pickup takeBestPickup() {
		//make sure it has ample space to take a new letter before deciding to do so
		int bundle_size = SimulationWorld.getSimulationWorld().letterStations[0].getBundleSize();
		if(getCapacity() - getLetters().size() < bundle_size)
			return null;

		//TODO should these be sorted by cost?  or sorted by trip?  or both?
		if(lettersToPickup.size() > 0) {
			if(assignedBucketbot.currentPickupTask != null) {
				LetterStationAgent l = assignedBucketbot.currentPickupTask.letterStationAgent;
				for(int i = 0; i < lettersToPickup.size(); i++)
					if(lettersToPickup.get(i).letterStationAgent == l)
						return lettersToPickup.remove(i);
			}
			return lettersToPickup.remove(0);
		}
		return null;
	}
	
	/**Should be called if a pickup fails
	 * @param p
	 */
	public void pickupFailed(Pickup p) {
		lettersToPickup.add(p);
	}
	
	float getDistanceToEndPosition(Circle destination) {
		
		float total_distance = 0.0f;
		Circle c = this;
		float temp_dist;
		
		for(Delivery d : lettersToDeliver) {
			temp_dist = d.wordStationAgent.getDistance(c);
			//if it'll be visiting the destination, then just distance til it gets there
			if(temp_dist == 0.0) return total_distance;
			total_distance += temp_dist;
			c = d.wordStationAgent;
		}
		
		for(Pickup p : lettersToPickup) {
			temp_dist = p.letterStationAgent.getDistance(c);
			//if it'll be visiting the destination, then just distance til it gets there
			if(temp_dist == 0.0) return total_distance;
			total_distance += temp_dist;
			c = p.letterStationAgent;
		}
		
		if(assignedStorage != null) {
			temp_dist = assignedStorage.getDistance(c);
			//if it'll be visiting the destination, then just distance til it gets there
			if(temp_dist == 0.0) return total_distance;
			total_distance += temp_dist;
			c = assignedStorage;
		}
		
		return total_distance + destination.getDistance(c);
		
		
		/*
		//TODO this needs to match with the ordering of tasks that the bucket will be doing in other methods
		if(assignedStorage != null)
			return assignedStorage.getDistance(destination);
		if(lettersToDeliver.size() > 0)
			return lettersToDeliver.get(lettersToDeliver.size()-1).wordStationAgent.getDistance(destination);
		if(lettersToPickup.size() > 0)
			return lettersToPickup.get(lettersToPickup.size()-1).letterStationAgent.getDistance(destination);
		return getDistance(destination);
*/
	}
	
	float getTravelCost(Circle destination) {
		float cost = 0.02f*getDistanceToEndPosition(destination);
		if(assignedBucketbot == null) {
			//add on price it will take to pick up bucket
			Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;
			BucketStorageAgent bsa = SimulationWorldMarketTaskAllocation.getSimulationWorld().resourceManager;
			Waypoint w = bsa.usedBucketStorageLocations.get(this);
			MultiItemDoubleAuction<Waypoint, Waypoint, BucketbotAgent, BucketAgent> transport_market = economy.storageToTransportationMarketMap.get(w);
			cost += transport_market.getAskPrice(w);
		}
		return cost;
	}
	
	public Waypoint getAssignedStorage() {
		if(assignedStorage != null)
			return assignedStorage;
		
		Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;
		BucketStorageAgent bsa = SimulationWorldMarketTaskAllocation.getSimulationWorld().resourceManager;
		
		//TODO eventually remove this code
		//old way of finding closest storage
		/*
		Waypoint closest = null;
		double closest_distance = Double.POSITIVE_INFINITY;
		for(Waypoint w : bsa.unusedBucketStorageLocations) {
			double distance = getDistance(w);
			//if it's closer than the previous closest, then use this new one instead
			if(distance < closest_distance) {
				closest_distance = distance;
				closest = w;
			}
		}
		if(closest != null) {
			bsa.unusedBucketStorageLocations.remove(closest);
			bsa.usedBucketStorageLocations.put(this, closest);
		}
				
		assignedStorage = closest;
		if(true) return assignedStorage;
		*/
	
		//see if this bucket agent is winning any of the storage markets
		MultiItemDoubleAuction<Waypoint, Waypoint, BucketStorageAgent, BucketAgent> best_market = null;
		float best_market_price = Float.POSITIVE_INFINITY;
		Waypoint best_storage_location = null;
		for(Waypoint w : bsa.unusedBucketStorageLocations) {
			MultiItemDoubleAuction<Waypoint, Waypoint, BucketStorageAgent, BucketAgent> m = economy.getClosestStorageMarket(w);
			if(m.isBuyerInTradingSet(this, w)) {
				float price = m.getBidPrice(w);
				if(price < best_market_price) {
					best_market = m;
					best_market_price = price;
					best_storage_location = w;
				}
			}
		}

		//won a market!
		if(best_storage_location != null) {
			best_market.removeAsks(bsa);
			best_market.removeBids(this);
			profit -= best_market_price;
			bsa.setProfit(bsa.getProfit() + best_market_price);
			assignedStorage = best_storage_location;
			
			bsa.unusedBucketStorageLocations.remove(assignedStorage);
			bsa.usedBucketStorageLocations.put(this, assignedStorage);
			return assignedStorage;
		}
		
		//if got nothing, then just take the cheapest storage location
		for(Waypoint w : bsa.unusedBucketStorageLocations) {
			MultiItemDoubleAuction<Waypoint, Waypoint, BucketStorageAgent, BucketAgent> m = economy.getClosestStorageMarket(w);
			float price = m.getBidPrice(w);
			if(price < best_market_price) {
				best_market = m;
				best_market_price = price;
				best_storage_location = w;
			}
		}
		
		//if still no storage, then something is probably wrong
		if(best_storage_location == null)
			return null;
		
		//take the storage location!
		best_market.removeAsks(bsa);
		best_market.removeBids(this);
		profit -= best_market_price;
		bsa.setProfit(bsa.getProfit() + best_market_price);
		assignedStorage = best_storage_location;
		
		bsa.unusedBucketStorageLocations.remove(assignedStorage);
		bsa.usedBucketStorageLocations.put(this, assignedStorage);
		return assignedStorage;
	}
	
	/**Sets assignedStorage, but only if it is currently null.
	 * @param w
	 */
	public void setAssignedStorage(Waypoint w) {
		if(assignedStorage == null)
			assignedStorage = w;
	}
	
	public void pickedUp() {
		if(assignedStorage != null) {
			BucketStorageAgent bsa = SimulationWorldMarketTaskAllocation.getSimulationWorld().resourceManager;
			bsa.unusedBucketStorageLocations.add(bsa.usedBucketStorageLocations.get(this) );
			bsa.usedBucketStorageLocations.remove(this);
			assignedStorage = null;
		}
	}
	
	public void setDown() {
		assignedBucketbot = null;
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#getNextEventTime(double)
	 */
	public double getNextEventTime(double cur_time) {
		return lastBidUpdateTime + bidUpdateInterval;
		//return Double.POSITIVE_INFINITY;
	}
	
	public void rebidOnWordStationLetterRequests() {
		Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;
		
		//put asks up for letters to word stations
		for(MultiItemDoubleAuction<LetterType, Letter, BucketAgent, WordStationAgent> m : economy.letterToWordMarkets)
			m.removeAsks(this);
		
		//want to reduce distance cost if there's a strong liklihood that this bucket
		// can deliver more than one letter.  higher is better for both capacity and probability,
		// but want to decrease the distance more the higher those are
		float frac_capacity = getLetters().size() / (float)getCapacity();
		float dist_multiplier = 1.0f - frac_capacity * getProbabilityBucketContainsALetter();

		//TODO add better valuation here
		for(Letter l : unassignedLetters) {
			
			//find the market where this agent can make the most profit
			MultiItemDoubleAuction<LetterType, Letter, BucketAgent, WordStationAgent> best_market = null;
			float best_market_profit = 0.0f;
			float best_market_ask = 0.0f;
			for(MultiItemDoubleAuction<LetterType, Letter, BucketAgent, WordStationAgent> m : economy.letterToWordMarkets) {
				float dist_cost = dist_multiplier * getTravelCost(economy.getMarketLocation(m));				
				float profit = m.getBidPrice(l.getType()) - dist_cost + .5f;
				if(profit >= best_market_profit) {
					best_market = m;
					best_market_ask = dist_cost + .5f;
					best_market_profit = profit;
				}
			}

			//put a bid in the most profitable market
			if(best_market != null)
				best_market.addAsk(this, l.getType(), l, best_market_ask);
		}
	}
	
	public void rebidOnTransportation() {
		Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;
		
		//TODO add better valuation here
		//put bids in for bucketbots at this current location if don't currently have one
		if(assignedBucketbot == null && lettersToDeliver.size() + lettersToPickup.size() > 0) {
			BucketStorageAgent bsa = SimulationWorldMarketTaskAllocation.getSimulationWorld().resourceManager;
			Waypoint w = bsa.usedBucketStorageLocations.get(this);
			if(w != null) {
				MultiItemDoubleAuction<Waypoint, Waypoint, BucketbotAgent, BucketAgent> transport_market = economy.storageToTransportationMarketMap.get(w);
				transport_market.removeBids(this);
				transport_market.addBid(this, w, w, 300.0f * (lettersToDeliver.size() + lettersToPickup.size()));
			}
		}
	}
		
	public void rebidOnLetterPickupRequests() {
		Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;
		
		//put bids up for letters to buckets
		for(MultiItemDoubleAuction<LetterType, Letter, LetterStationAgent, BucketAgent> m : economy.letterToBucketMarkets)
			m.removeBids(this);
		
		//find out how many letter bundles this bucket should bid on (don't want to be exposed to having more
		// than have capacity for
		int bundle_size = SimulationWorld.getSimulationWorld().letterStations[0].getBundleSize();
		int num_bids = (getCapacity() - getLetters().size() - bundle_size * lettersToPickup.size()) / bundle_size;
		
		float frac_capacity = (getLetters().size() + bundle_size * lettersToPickup.size()) / (float)getCapacity();
		
		//TODO add better valuation here
		for(int i = 0; i < num_bids-1; i++) {

			//pick random market
			MultiItemDoubleAuction<LetterType, Letter, LetterStationAgent, BucketAgent> m = 
				economy.letterToBucketMarkets.get(SimulationWorld.rand.nextInt(economy.letterToBucketMarkets.size()));
			
			List<LetterType> lts = m.getItemTypesWithAsks();
			if(lts.size() == 0)
				continue;
			LetterType lt = lts.get(SimulationWorld.rand.nextInt(lts.size()));
			//TODO does the next line work as well with the 100-... ?
			m.addBid(this, lt, lt, 100 - frac_capacity * getTravelCost(economy.getMarketLocation(m)));
		}
	}
	
	public void rebidOnStorageLocations() {
		Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;
		
		//bid on storage locations
		for(MultiItemDoubleAuction<Waypoint, Waypoint, BucketStorageAgent, BucketAgent> m : economy.storageMarkets)
			m.removeBids(this);
		
//		float frac_capacity = getLetters().size() / (float)getCapacity();
//		float dist_multiplier = frac_capacity * getProbabilityBucketContainsALetter();
		
		if(assignedStorage == null) {
			//TODO put valuations here
			BucketStorageAgent bsa = SimulationWorldMarketTaskAllocation.getSimulationWorld().resourceManager;
			for(Waypoint w : bsa.unusedBucketStorageLocations) {
				MultiItemDoubleAuction<Waypoint, Waypoint, BucketStorageAgent, BucketAgent> m = economy.getClosestStorageMarket(w);
				float height = SimulationWorld.getSimulationWorld().getMap().getHeight();
				float width = SimulationWorld.getSimulationWorld().getMap().getWidth();
				float max_dist = (float)Math.sqrt(height * height + width * width);
				m.addBid(this, w, w, .1f * max_dist - getTravelCost(w));
				//m.addBid(this, w, w, dist_multiplier * (.1f * max_dist - getTravelCost(w)));
				//m.addBid(this, w, w, getProbabilityBucketContainsALetter()* (.1f * max_dist - getTravelCost(w)));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#update(double, double)
	 */
	public void update(double last_time, double cur_time) {
		curTime = cur_time;
		
		if(curTime >= lastBidUpdateTime + bidUpdateInterval) {
			lastBidUpdateTime = curTime;
			
			rebidOnWordStationLetterRequests();
			rebidOnTransportation();
			rebidOnLetterPickupRequests();
			rebidOnStorageLocations();			
		}
	}
	
	
	public float getProbabilityBucketContainsALetter() {
		WordList wl = SimulationWorld.getSimulationWorld().wordList;
		double prob = 0.0f;
		for(Letter l : letters) {
			float lp = wl.getLetterProbability(l);
			prob = prob + lp - prob * lp;  
		}
		return (float)prob;
		
	}
	
	public List<String> getAdditionalInfo() {
		DecimalFormat four_digits = new DecimalFormat("0.000");
		List<String> s = new ArrayList<String>();
		s.add("Current profit: " + four_digits.format(profit));
		
		String deliveries = "deliveries: ";
		for(Delivery d : lettersToDeliver) {
			deliveries += d.letterToDeliver + " ";
		}
		s.add(deliveries);
		
		String pickups = "pickups: ";
		for(Pickup p : lettersToPickup) {
			pickups += p.letterToPickUp + " ";
		}
		s.add(pickups);
		
		return s;
	}

	/**
	 * @return the profit
	 */
	public double getProfit() {
		return profit;
	}

	/**
	 * @param profit the profit to set
	 */
	public void setProfit(double profit) {
		this.profit = profit;
	}

}
