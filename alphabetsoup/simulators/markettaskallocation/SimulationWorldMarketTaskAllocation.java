/**
 * 
 */
package alphabetsoup.simulators.markettaskallocation;

import java.util.*;

import alphabetsoup.base.*;
import alphabetsoup.framework.*;
import alphabetsoup.userinterface.*;
import alphabetsoup.waypointgraph.*;

/**Example AlphabetSoup simulation file, which puts buckets in a grid, lays out bots randomly,
 * parameratizes everything based on "alphabetsoup.config", and starts everything running.
 * @author Chris Hazard
 */
public class SimulationWorldMarketTaskAllocation extends SimulationWorld {
	
	private double simulationDuration = 0.0;
	private double simulationWarmupTime = 0.0;
	
	public LetterManager letterManager = null;
	public WordOrderManager wordManager = null;
	public BucketStorageAgent resourceManager = null;
	public WaypointGraph waypointGraph = null;
	public List<BucketbotAgent> bucketbotAgents = null;
	public List<WordStationAgent> wordStationAgents = null;
	public List<LetterStationAgent> letterStationAgents = null;
	public List<BucketAgent> bucketAgents = null;
	
	public Economy economy = null;
	
	private static SimulationWorldMarketTaskAllocation simulationWorldMarketTaskAllocation;
	public static SimulationWorldMarketTaskAllocation getSimulationWorld() {
		return simulationWorldMarketTaskAllocation;
	}
	
	public void resetStatistics() {
		super.resetStatistics();
		letterManager.setProfit(0.0);
		wordManager.setProfit(0.0);
		resourceManager.setProfit(0.0);
		for(BucketbotAgent ba : bucketbotAgents)
			ba.setProfit(0.0);
		for(WordStationAgent wsa : wordStationAgents)
			wsa.setProfit(0.0);
		for(LetterStationAgent lsa : letterStationAgents)
			lsa.setProfit(0.0);
		for(BucketAgent ba : bucketAgents)
			ba.setProfit(0.0);
	}
	
	public SimulationWorldMarketTaskAllocation() {
		super("alphabetsoup.config");
		simulationWorldMarketTaskAllocation = this;
		
		float bucketbot_size = Float.parseFloat(params.getProperty("bucketbot_size"));
		float bucket_size = Float.parseFloat(params.getProperty("bucket_size"));
		float station_size = Float.parseFloat(params.getProperty("station_size"));
		int bucket_capacity = Integer.parseInt(params.getProperty("bucket_capacity"));
		int bundle_size = Integer.parseInt(params.getProperty("bundle_size"));
		int letter_station_capacity = Integer.parseInt(params.getProperty("letter_station_capacity"));
		int word_station_capacity = Integer.parseInt(params.getProperty("word_station_capacity"));
		float bucket_pickup_setdown_time = Float.parseFloat( params.getProperty("bucket_pickup_setdown_time"));
		float letter_to_bucket_time = Float.parseFloat( params.getProperty("letter_to_bucket_time"));
		float bucket_to_letter_time = Float.parseFloat( params.getProperty("bucket_to_letter_time"));
		float word_completion_time = Float.parseFloat( params.getProperty("word_completion_time"));
		float collision_penalty_time = Float.parseFloat( params.getProperty("collision_penalty_time"));
		usingGUI = (Integer.parseInt(params.getProperty("useGUI")) == 1);
		String window_size[] = params.getProperty("window_size").split("x");
		simulationDuration = Double.parseDouble(params.getProperty("simulation_duration"));
		simulationWarmupTime = Double.parseDouble(params.getProperty("simulation_warmup_time"));
		
		//market parameters
		float word_completion_base_revenue = Float.parseFloat(params.getProperty("word_completion_base_revenue"));
		float word_completion_letter_marginal_revenue = Float.parseFloat(params.getProperty("word_completion_letter_marginal_revenue"));
		float letter_bundle_cost = Float.parseFloat(params.getProperty("letter_bundle_cost"));
		
		waypointGraph = new WaypointGraph(map.getWidth(), map.getHeight());
		
		//Set up base map to add things to
		if(usingGUI)
			RenderWindow.initializeUserInterface(Integer.parseInt(window_size[0]), Integer.parseInt(window_size[1]), this);
		
		BucketbotDriver.waypointGraph = waypointGraph;
		BucketbotDriver.map = map;

		//Create classes and agents, and add them to the map accordingly
		bucketbotAgents = new ArrayList<BucketbotAgent>(bucketbots.length); 
		for(int i = 0; i < bucketbots.length; i++) {
			bucketbots[i] = (Bucketbot) new BucketbotDriver(bucketbot_size,
					bucket_pickup_setdown_time, map.getMaxAcceleration(), map.getMaxVelocity(), collision_penalty_time);
			bucketbotAgents.add(i, new BucketbotAgent((BucketbotDriver)bucketbots[i]));
		}
		
		letterStationAgents = new ArrayList<LetterStationAgent>(letterStations.length);
		for(int i = 0; i < letterStations.length; i++) {
			letterStationAgents.add(i, new LetterStationAgent(
														station_size, letter_to_bucket_time, bundle_size, letter_station_capacity));
			letterStations[i] = (LetterStation)letterStationAgents.get(i);
		}
		
		wordStationAgents = new ArrayList<WordStationAgent>(wordStations.length);
		for(int i = 0; i < wordStations.length; i++) {
			wordStationAgents.add(i, new WordStationAgent(
														station_size, bucket_to_letter_time, word_completion_time, word_station_capacity) );
			wordStations[i] = (WordStation)wordStationAgents.get(i);
		}
		
		bucketAgents = new ArrayList<BucketAgent>(buckets.length);
		for(int i = 0; i < buckets.length; i++) {
			bucketAgents.add(i, new BucketAgent(bucket_size, bucket_capacity));
			buckets[i] = (Bucket) bucketAgents.get(i);
		}
		
		resourceManager	= new BucketStorageAgent();
		letterManager	= new LetterManager(letter_bundle_cost);
		wordManager		= new WordOrderManager();

		//generate waypoint graph
		HashMap<Waypoint, Bucket> storage = GenerateWaypointGraph.initializeCompactRandomLayout(this, waypointGraph);
		for(Waypoint w : storage.keySet())
			if(storage.get(w) == null)
				resourceManager.addNewValidBucketStorageLocation(w);
			else
				resourceManager.addNewUsedBucketStorageLocation(storage.get(w), w);
		
		economy = new Economy(word_completion_base_revenue, word_completion_letter_marginal_revenue);
		
		for(int i = 0; i < letterStations.length; i++)
			((LetterStationAgent)letterStations[i]).setSituatedMarket(economy.addLetterToBucketMarket((Circle)letterStations[i]));
		for(int i = 0; i < wordStations.length; i++)
			((WordStationAgent)wordStations[i]).setSituatedMarket(economy.addLetterToWordMarket((Circle)wordStations[i]));
		
		economy.addStorageMarket(new Circle(map.getTolerance(), map.getWidth()/2,map.getHeight()/2));
		
		for(Waypoint w : resourceManager.usedBucketStorageLocations.values())
			economy.addTransportationMarket(w);
		for(Waypoint w : resourceManager.unusedBucketStorageLocations)
			economy.addTransportationMarket(w);
		
		//generate words
		wordList.generateWordsFromFile(params.getProperty("dictionary"), letterColors,
				Integer.parseInt(params.getProperty("number_of_words")) );
		
		//populate buckets
		initializeBucketContentsRandom(Float.parseFloat(params.getProperty("initial_inventory")), bundle_size);
		
		//populate update list
		updateables = new ArrayList<Updateable>();
		for(Bucketbot r : bucketbots)
			updateables.add((Updateable)r);
		for(Updateable a : bucketbotAgents)
			updateables.add(a);
		for(Bucket b : buckets)
			updateables.add((Updateable)b);
		updateables.add((Updateable)map);
		updateables.add((Updateable)resourceManager);
		updateables.add((Updateable)wordManager);
		updateables.add((Updateable)letterManager);
		for(WordStation s : wordStations)
			updateables.add((Updateable)s);
		for(LetterStation s : letterStations)
			updateables.add((Updateable)s);
		updateables.add((Updateable)economy);
		
		//finish adding things to be rendered
		if(usingGUI) {
			RenderWindow.addAdditionalDetailRender(new WordListRender((WordListBase)wordList));
			
			RenderWindow.addLineRender(new MapRender(map));
			
			for(LetterStation s : letterStations)
				RenderWindow.addSolidRender(new LetterStationRender((LetterStationBase)s));
			for(WordStation s : wordStations)
				RenderWindow.addSolidRender(new WordStationRender((WordStationBase)s));
			
			for(Bucket b : buckets)
				RenderWindow.addLineRender(new BucketRender((BucketBase)b));
			for(Bucketbot r : bucketbots)
				RenderWindow.addLineRender(new BucketbotRender((BucketbotBase)r));
			
			//RenderWindow.addSolidRender(resourceManager);
			
			//RenderWindow.addSolidRender(new WaypointGraphRender(waypointGraph));
			
			for(MultiItemDoubleAuction m : economy.letterToWordMarkets)
				RenderWindow.addSolidRender(new SituatedMarketRender(m));
			for(MultiItemDoubleAuction m : economy.letterToBucketMarkets)
				RenderWindow.addSolidRender(new SituatedMarketRender(m));
			for(MultiItemDoubleAuction m : economy.transportationMarkets)
				RenderWindow.addSolidRender(new SituatedMarketRender(m));
			for(MultiItemDoubleAuction m : economy.storageMarkets)
				RenderWindow.addSolidRender(new SituatedMarketRender(m));
			RenderWindow.addSolidRender(new EconomyRender(economy, new Circle(0.0f, map.getWidth()/2, 0.0f)));
		}
	}
	
	/**Launches the Alphabet Soup simulation without user interface.
	 * @param args
	 */
	public static void main(String[] args) {
/*	
LetterHistory lh = new LetterHistory(.25f, 1.0f, 100);
Letter l = new Letter('l', 1);
Letter m = new Letter('m', 2);
lh.addNewValue(l, 1, 1.0);
lh.addNewValue(l, 2, 2.0);
lh.addNewValue(l, 3, 3.0);
lh.addNewValue(l, 4, 4.0);
lh.addNewValue(m, 4, 4.0);
lh.addNewValue(m, 4, 4.0);
lh.addNewValue(m, 4, 4.0);
lh.addNewValue(m, 5, 4.0);
System.out.println(lh.getValueOfColor(1));
System.out.println(lh.getValueOfColor(2));
System.out.println(lh.getValueOfLetter('l'));
System.out.println(lh.getValueOfLetter('m'));
System.out.println(lh.getValueOfLetterTile(l));
System.out.println(lh.getValueOfLetterTile(m));

System.out.println("highest letter values");
for(LetterHistory.LetterValuePair lvp : lh.getHighestLetterValues()) {
	System.out.println(lvp.letter + ": " + lvp.value);
}

System.out.println("highest color values");
for(LetterHistory.ColorValuePair cvp : lh.getHighestColorValues()) {
	System.out.println(cvp.color + ": " + cvp.value);
}

System.out.println("highest letter tile values");
for(LetterHistory.LetterTileValuePair lvp : lh.getHighestLetterTileValues()) {
	System.out.println(lvp.letter.getLetter() + "(" + lvp.letter.getColorID() + ") : " + lvp.value);
}

ValueHistory vh = new ValueHistory(.75f, 1.0f, 100);
vh.addNewValue(10, 0.0);
vh.addNewValue(8, 3.0);
vh.addNewValue(7, 4.0);
vh.addNewValue(6, 5.0);
vh.addNewValue(9, 6.5);
vh.addNewValue(9, 8.0);
vh.addNewValue(11, 10.0);
System.out.println("value: " + vh.getCurrentValue() + "   rate: " + vh.getCurrentValueRate(10.1));
System.exit(0);
*/
	
/*
//should further test DoubleAuction
Set<WordStation> buyers = new HashSet<WordStation>();
Set<Bucket> sellers = new HashSet<Bucket>();
Bucket b1 = new BucketBase(1.0f, 10);
float b1_bid = 1;
sellers.add(b1);
Bucket b2 = new BucketBase(1.0f, 10);
float b2_bid = 4;
sellers.add(b1);

WordStation w1 = new WordStationBase(1.0f, 1.0f, 1.0f, 10);
float w1_bid = 2;
buyers.add(w1);
WordStation w2 = new WordStationBase(1.0f, 1.0f, 1.0f, 10);
float w2_bid = 3;
buyers.add(w2);

DoubleAuction<LetterType, Bucket, WordStation> cda = new DoubleAuction<LetterType, Bucket, WordStation>();
cda.addBid(w2, w2_bid);	System.out.println("bid: " + w2 + "  value: " + w2_bid);
cda.addBid(w2, w2_bid);	System.out.println("bid: " + w2 + "  value: " + w2_bid);
//cda.addAsk(b2, b2_bid);	System.out.println("bid: " + b2 + "  value: " + b2_bid);
cda.addBid(w1, w1_bid);	System.out.println("bid: " + w1 + "  value: " + w1_bid);
cda.addBid(w1, w1_bid);	System.out.println("bid: " + w1 + "  value: " + w1_bid);
cda.addAsk(b1, b1_bid);	System.out.println("ask: " + b1 + "  value: " + b1_bid);
cda.addAsk(b1, b1_bid);	System.out.println("ask: " + b1 + "  value: " + b1_bid);
cda.addAsk(b2, b2_bid);	System.out.println("ask: " + b2 + "  value: " + b2_bid);
System.out.println("bid price: " + cda.getBidPrice());
System.out.println("ask price: " + cda.getAskPrice());

List<Exchange<LetterType, Bucket, WordStation>> eL = cda.acceptAllExchangesFrom(buyers, sellers, cda.getBidPrice());

Exchange<LetterType, Bucket, WordStation> e = cda.acceptNextBuyerExchange(w1);
//DoubleAuction.Exchange e = cda.acceptNextSellerExchange(b1);
if(e == null)
	System.out.println("no winner");
else {
	System.out.println("winning buy bid: " + e.value);
	System.out.println("winning buyer: " + e.buyer);
	System.out.println("winning seller: " + e.seller);
}
System.exit(0);
//*/

		simulationWorld = new SimulationWorldMarketTaskAllocation();
		double warmup_time = ((SimulationWorldMarketTaskAllocation)simulationWorld).simulationWarmupTime;
		double simulation_time = ((SimulationWorldMarketTaskAllocation)simulationWorld).simulationDuration; 
		if(simulationWorld.isUsingGUI()) {
			RenderWindow.mainLoop(simulationWorld, warmup_time);
			simulationWorld.resetStatistics();
			RenderWindow.mainLoop(simulationWorld, simulation_time);
			RenderWindow.destroyUserInterface();
		}
		else {
			simulationWorld.update(warmup_time);
			simulationWorld.resetStatistics();
			simulationWorld.update(simulation_time);
		}

		SummaryReport.generateReport(simulationWorld);
	}
}
