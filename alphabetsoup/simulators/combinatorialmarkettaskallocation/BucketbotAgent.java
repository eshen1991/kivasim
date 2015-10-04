/**
 * 
 */
package alphabetsoup.simulators.combinatorialmarkettaskallocation;

import alphabetsoup.base.BucketbotTask;
import alphabetsoup.framework.*;
import alphabetsoup.waypointgraph.*;

/**
 * @author Chris Hazard
 */
public class BucketbotAgent implements BucketbotManager, Updateable {
	
	BucketbotDriver bucketbot = null;
	BucketStorageAgent manager = null;
	
	BucketAgent reservedBucket = null;
	BucketAgent.Pickup currentPickupTask = null;
	BucketAgent.Delivery currentDeliveryTask = null;
	
	private double curTime = 0.0;
	private double bidUpdateInterval = 0.5;
	private double lastBidUpdateTime = Double.NEGATIVE_INFINITY;
	
	private double profit = 0.0;
	
	public BucketbotAgent(BucketbotDriver bucketbot) {
		this.bucketbot = bucketbot;
		bucketbot.manager = (BucketbotManager)this;
	}
	
	/**Optimistically estimates the time it will take this.bucketbot to reach Waypoint w
	 * @param w
	 * @return estimated travel time
	 */
	public float estimateTravelTime(Waypoint w) {
		float travel_time = bucketbot.getDistance(w) / bucketbot.getMaxVelocity();
		return travel_time;
	}
	
	/**Optimistically estimates the time it will take a bucketbot to go from Waypoints start to end
	 * @param start
	 * @param end
	 * @return estimated travel time
	 */
	public float estimateTravelTime(Waypoint start, Waypoint end) {
		if(start == null || end == null)
			return Float.POSITIVE_INFINITY;
		float travel_time = start.getDistance(end) / bucketbot.getMaxVelocity();
		return travel_time;
	}
	
	/**Estimates the amount of time it will take before a new letter can be delivered to the word station 
	 * @param w
	 * @return estimated time
	 */
	public float estimateWordStationWaitTime(Waypoint w) {
		WordStation ws = w.getWordStation();
		//assume the wait time is 2 * length of the bucketbot there plus the time for all letters 
		float wait_time = ((ws.getBucketToLetterTime() + 5 * 2* bucketbot.getRadius()) / bucketbot.getMaxVelocity())
								* w.getBucketbots().size() * w.getBucketbots().size();
		return wait_time;
	}
	
	/**Estimates the amount of time it will take before a new letter can be picked up from the letter station
	 * @param w
	 * @return estimated time
	 */
	public float estimateLetterStationWaitTime(Waypoint w) {
		LetterStation ls = w.getLetterStation();
		//assume the wait time is 2 * length of the bucketbot there plus the time for all letters 
		float wait_time = ((ls.getLetterToBucketTime() + 5 * 2* bucketbot.getRadius()) / bucketbot.getMaxVelocity())
								* w.getBucketbots().size() * w.getBucketbots().size();
		return wait_time;
	}
	
	/**Tells bucketbot to store its current bucket at the closest waypoint
	 */
	public void storeBucket() {
		Waypoint w = ((BucketAgent)bucketbot.getBucket()).getAssignedStorage();
		bucketbot.<BucketbotTask>assignTask(BucketbotTask.createTaskSTORE_BUCKET(bucketbot.getBucket(), w));
	}
	
	public void deliverLetter(BucketAgent.Delivery d) {
		bucketbot.<BucketbotTask>assignTask(BucketbotTask.createTaskTAKE_BUCKET_TO_WORD_STATION(
				reservedBucket, d.letterToDeliver, d.wordStationAgent,
				d.wordStationAgent.getWordContainingOriginalLetterInstance(d.letterSlotToDeliver)));
	}
	
	public void pickupLetter(BucketAgent.Pickup p) {
		bucketbot.<BucketbotTask>assignTask(BucketbotTask.createTaskTAKE_BUCKET_TO_LETTER_STATION(
				reservedBucket, p.letterToPickUp, p.letterStationAgent));
	}
	
	/**Tells bucketbot to store its current bucket at the closest location (if it has one),
	 * and then keep moving to stay out of the way of other bucketbots.
	 */
	void getOutOfTheWay() {		
		if(bucketbot.getBucket() != null) {
			storeBucket();
			return;
		}
		
		//TODO evaluate whether it's cost effective to spend the resources to go to another place like this
		//see if any transportation markets are in need of a bucketbot
		//if so, go there
		Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;
		Waypoint go_to = null;
		float highest_price = 0.0f;
		for(Waypoint w : economy.storageToTransportationMarketMap.keySet()) {
				MultiItemDoubleAuction<Waypoint, Waypoint, BucketbotAgent, BucketAgent> m
					= economy.storageToTransportationMarketMap.get(w);
			
			//randomly skip a spot so bucketbot agents don't get stuck
			if(SimulationWorld.rand.nextFloat() > 0.75)
				continue;
			
			float price = m.getBidPrice(w) - .1f * bucketbot.getDistance(w);
			if(price > highest_price) {
				go_to = w;
				highest_price = price; 
			}
		}
		if(go_to != null && go_to != bucketbot.getCurrentWaypoint()) {
			bucketbot.<BucketbotTask>assignTask(BucketbotTask.createTaskMOVE(go_to));
			return;
		}
		
		//pick a random offset to move
		float x = bucketbot.getX() + bucketbot.getRadius() * 2 * (SimulationWorld.rand.nextFloat() - .5f);
		float y = bucketbot.getY() + bucketbot.getRadius() * 2 * (SimulationWorld.rand.nextFloat() - .5f);
		
		//don't get too close to the edge
		int radii_from_edge = 8;
		x = Math.max(x, radii_from_edge*bucketbot.getRadius());
		x = Math.min(x, BucketbotDriver.map.getWidth() - radii_from_edge*bucketbot.getRadius());
		y = Math.max(y, radii_from_edge*bucketbot.getRadius());
		y = Math.min(y, BucketbotDriver.map.getHeight() - radii_from_edge*bucketbot.getRadius());
		bucketbot.<BucketbotTask>assignTask(BucketbotTask.createTaskMOVE(x, y));
	}
	
	/**Bucketbots should call requestNewTask of their corresponding BucketbotAgent when they are idle and have no tasks
	 */
	public void requestNewTask(Bucketbot r) {
		manager = SimulationWorldMarketTaskAllocation.getSimulationWorld().resourceManager;
		
		if(reservedBucket != null) {
			currentDeliveryTask = reservedBucket.takeBestDelivery();
			if(currentDeliveryTask != null) {
				deliverLetter(currentDeliveryTask);
				return;
			}

			currentPickupTask = reservedBucket.takeBestPickup();
			if(currentPickupTask != null) {
				pickupLetter(currentPickupTask);
				return;
			}
			
			if(bucketbot.getBucket() != null)
				storeBucket();
			return;
		}

		getOutOfTheWay();
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.waypointgraph.BucketbotManager#bucketPickedUp(alphabetsoup.framework.Bucketbot, alphabetsoup.framework.Bucket)
	 */
	public void bucketPickedUp(Bucketbot r, Bucket b) {
		((BucketAgent)b).pickedUp();
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.waypointgraph.BucketbotManager#bucketSetDown(alphabetsoup.framework.Bucketbot, alphabetsoup.framework.Bucket, alphabetsoup.waypointgraph.Waypoint)
	 */
	public void bucketSetDown(Bucketbot r, Bucket b, Waypoint w) {
		((BucketAgent)b).setDown();
		reservedBucket = null;		
	}
	
	/**Bucketbots should call their corresponding BucketbotAgent's taskComplete when an assigned task has been completed
	 * @param r bucketbot calling the function 
	 * @param t task which was completed -implementations may use any object types as a task
	 */
	public void taskComplete(Bucketbot r, BucketbotTask t) {
		if(t == null)
			return;
		currentDeliveryTask = null;
		currentPickupTask = null;
		bucketbot.assignTask(null);
	}
	
	/**Bucketbots should call their corresponding BucketbotAgent's taskAborted when an assigned task has been aborted
	 * @param r bucketbot calling the function 
	 * @param t task which was aborted -implementations may use any object types as a task
	 */
	public void taskAborted(Bucketbot r, BucketbotTask t) {
		if(t == null || reservedBucket == null)
			return;
		
		//see if didn't pick up bucket in the first place
		if(reservedBucket != null && bucketbot.getBucket() != reservedBucket) {
			//effectively the same as being set down
			reservedBucket.setDown();
		}
		else { //at least it has picked up the bucket
			
			switch(t.getTaskType()) {
			case STORE_BUCKET:
				//if couldn't store it, then it's effectively the same as picking it up
				reservedBucket.pickedUp();
				break;
			case TAKE_BUCKET_TO_LETTER_STATION:
				reservedBucket.pickupFailed(currentPickupTask);
				break;
			case TAKE_BUCKET_TO_WORD_STATION:
				reservedBucket.deliveryFailed(currentDeliveryTask);
				break;
			}
		}
		
		reservedBucket = (BucketAgent)bucketbot.getBucket();
		currentDeliveryTask = null;
		currentPickupTask = null;
		bucketbot.assignTask(null);
	}
	
	public void rebidOnBucketTransportation() {
		Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;
		BucketStorageAgent bsa = SimulationWorldMarketTaskAllocation.getSimulationWorld().resourceManager;

		//put asks in for the transportation markets
		for(MultiItemDoubleAuction<Waypoint, Waypoint, BucketbotAgent, BucketAgent> m : economy.transportationMarkets)
			m.removeAsks(this);
		
		if(reservedBucket == null) {
			/*
			//put a bid in every market based on cost to get there
			for(Waypoint w : bsa.usedBucketStorageLocations.values()) {
				MultiItemDoubleAuction<Waypoint, Waypoint, BucketbotAgent, BucketAgent> m
						= economy.storageToTransportationMarketMap.get(w);
				float dist_cost = .1f * economy.getMarketLocation(m).getDistance(bucketbot);
				m.addAsk(this, w, w, dist_cost);
			}
			*/
		
			
			//find the market where this agent can make the most profit
			MultiItemDoubleAuction<Waypoint, Waypoint, BucketbotAgent, BucketAgent> best_market = null;
			float best_market_profit = Float.NEGATIVE_INFINITY;
			float best_market_bid = 0.0f;
			Waypoint best_storage = null;
			for(Waypoint w : bsa.usedBucketStorageLocations.values()) {
				MultiItemDoubleAuction<Waypoint, Waypoint, BucketbotAgent, BucketAgent> m
						= economy.storageToTransportationMarketMap.get(w);
				float dist_cost = .1f * economy.getMarketLocation(m).getDistance(bucketbot);
				float profit = m.getBidPrice(null) - dist_cost;
				if(profit >= best_market_profit) {
					best_market = m;
					best_market_bid = dist_cost;
					best_market_profit = profit;
					best_storage = w;
				}
			}

			//put a bid in the most profitable market
			if(best_market != null)
				best_market.addAsk(this, best_storage, best_storage, best_market_bid);
		}
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
			
			rebidOnBucketTransportation();
		}
		
	}
	
	public void transportationSold(Exchange<Waypoint, BucketbotAgent, BucketAgent> e) {
		reservedBucket = e.buyer;
		profit += e.value;
		rebidOnBucketTransportation();
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
