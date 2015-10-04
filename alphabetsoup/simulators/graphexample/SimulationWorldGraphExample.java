/**
 * 
 */
package alphabetsoup.simulators.graphexample;

import java.util.*;

import alphabetsoup.base.BucketBase;
import alphabetsoup.base.BucketbotBase;
import alphabetsoup.base.LetterStationBase;
import alphabetsoup.base.SummaryReport;
import alphabetsoup.base.WordListBase;
import alphabetsoup.base.WordStationBase;
import alphabetsoup.framework.*;
import alphabetsoup.userinterface.BucketRender;
import alphabetsoup.userinterface.BucketbotRender;
import alphabetsoup.userinterface.LetterStationRender;
import alphabetsoup.userinterface.MapRender;
import alphabetsoup.userinterface.RenderWindow;
import alphabetsoup.userinterface.WordListRender;
import alphabetsoup.userinterface.WordStationRender;
import alphabetsoup.waypointgraph.*;

/**Example AlphabetSoup simulation file, which puts buckets in a grid, lays out bots randomly,
 * parameratizes everything based on "alphabetsoup.config", and starts everything running.
 * @author Chris Hazard
 */
public class SimulationWorldGraphExample extends SimulationWorld {
	
	private double simulationDuration = 0.0;
	public LetterManagerExample letterManager = null;
	public Updateable wordManager = null;
	public BucketbotManagerExample bucketbotManager = null;
	public WaypointGraph waypointGraph = null;
	
	private static SimulationWorldGraphExample simulationWorldGraphExample;
	public static SimulationWorldGraphExample getSimulationWorld() {
		return simulationWorldGraphExample;
	}
	
	public SimulationWorldGraphExample() {
		super("alphabetsoup.config");
		simulationWorldGraphExample = this;

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
		
		waypointGraph = new WaypointGraph(map.getWidth(), map.getHeight());
		
		//Set up base map to add things to
		if(usingGUI)
			RenderWindow.initializeUserInterface(Integer.parseInt(window_size[0]), Integer.parseInt(window_size[1]), this);
		
		BucketbotDriver.waypointGraph = waypointGraph;
		BucketbotDriver.map = map;

		//Create classes, and add them to the map accordingly
		for(int i = 0; i < bucketbots.length; i++)
			bucketbots[i] = (Bucketbot) new BucketbotDriver(bucketbot_size,
					bucket_pickup_setdown_time, map.getMaxAcceleration(), map.getMaxVelocity(), collision_penalty_time);
		
		for(int i = 0; i < letterStations.length; i++)
			letterStations[i] = (LetterStation) new LetterStationBase(
															station_size, letter_to_bucket_time, bundle_size, letter_station_capacity);
		
		for(int i = 0; i < wordStations.length; i++)
			wordStations[i] = (WordStation) new WordStationBase(
														station_size, bucket_to_letter_time, word_completion_time, word_station_capacity);
		
		for(int i = 0; i < buckets.length; i++)
			buckets[i] = (Bucket) new BucketBase(bucket_size, bucket_capacity);
		
		bucketbotManager	= new BucketbotManagerExample(buckets);
		letterManager	= new LetterManagerExample();
		wordManager		= (Updateable) new WordOrderManagerExample();
		
		for(Bucketbot r : bucketbots)
			((BucketbotDriver)r).manager = bucketbotManager;

		//generate waypoint graph
		HashMap<Waypoint, Bucket> storage = GenerateWaypointGraph.initializeCompactRandomLayout(this, waypointGraph);
		for(Waypoint w : storage.keySet())
			if(storage.get(w) == null)
				bucketbotManager.addNewValidBucketStorageLocation(w);
			else
				bucketbotManager.addNewUsedBucketStorageLocation(storage.get(w), w);
		
		//generate words
		wordList.generateWordsFromFile(params.getProperty("dictionary"), letterColors,
				Integer.parseInt(params.getProperty("number_of_words")) );
		
		//populate buckets
		initializeBucketContentsRandom(Float.parseFloat(params.getProperty("initial_inventory")), bundle_size);
		
		//populate update list
		updateables = new ArrayList<Updateable>();
		for(Bucketbot r : bucketbots)
			updateables.add((Updateable)r);
		updateables.add((Updateable)map);
		updateables.add((Updateable)bucketbotManager);
		updateables.add((Updateable)wordManager);
		updateables.add((Updateable)letterManager);
		for(WordStation s : wordStations)
			updateables.add((Updateable)s);
		for(LetterStation s : letterStations)
			updateables.add((Updateable)s);
		
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
		}
	}
	
	/**Launches the Alphabet Soup simulation without user interface.
	 * @param args
	 */
	public static void main(String[] args) {
		simulationWorld = new SimulationWorldGraphExample();
		if(simulationWorld.isUsingGUI())
		{
			RenderWindow.mainLoop(simulationWorld,
					((SimulationWorldGraphExample)simulationWorld).simulationDuration);
			RenderWindow.destroyUserInterface();
		}
		else
			simulationWorld.update( ((SimulationWorldGraphExample)simulationWorld).simulationDuration);

		SummaryReport.generateReport(simulationWorld);
	}
}
