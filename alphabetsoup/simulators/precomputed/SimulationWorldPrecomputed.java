/**
 * 
 */
package alphabetsoup.simulators.precomputed;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import alphabetsoup.base.*;
import alphabetsoup.framework.*;
import alphabetsoup.userinterface.*;
import alphabetsoup.waypointgraph.*;

/**Example AlphabetSoup simulation file, which puts buckets in a grid, lays out bots randomly,
 * parameratizes everything based on "alphabetsoup.config", and starts everything running.
 * @author Chris Hazard
 */
public class SimulationWorldPrecomputed extends SimulationWorld {
	
	/**Changes a Letter into a unique integer based on the leter itself and its color
	 * @param l Letter
	 * @return unique integer
	 */ 
	static Letter intToLetter(int num) {
		return new Letter((char)(num % 256), num / 256);
	}
	
	private double simulationDuration = 0.0;
	public LetterManagePrecomputed letterManager = null;
	public Updateable wordManager = null;
	public BucketbotManagerExample bucketbotManager = null;
	public WaypointGraph waypointGraph = null;
	
	private static SimulationWorldPrecomputed simulationWorldPrecomputed;
	public static SimulationWorldPrecomputed getSimulationWorld() {
		return simulationWorldPrecomputed;
	}
	
	public SimulationWorldPrecomputed() {
		super("alphabetsoup.config");
		simulationWorldPrecomputed = this;
		
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
		letterManager	= new LetterManagePrecomputed(letterStations.length);
		wordManager		= new WordManagerPrecomputed(wordStations.length);
		
		for(Bucketbot r : bucketbots)
			((BucketbotDriver)r).manager = bucketbotManager;

		//generate waypoint graph
		HashMap<Waypoint, Bucket> storage = GenerateWaypointGraph.initializeCompactRandomLayout(this, waypointGraph);
		for(Waypoint w : storage.keySet())
			if(storage.get(w) == null)
				bucketbotManager.addNewValidBucketStorageLocation(w);
			else
				bucketbotManager.addNewUsedBucketStorageLocation(storage.get(w), w);
		
		//load wordlist info from the file (only used when ExternalConfigurationGeneratior extends this class)
		wordList.generateWordsFromFile(params.getProperty("dictionary"), letterColors, 0);
		
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

			//RenderWindow.addSolidRender(new WaypointGraphRender(waypointGraph));
		}
		
		ReadPrecomputedAllocations();
	}
	
	void ReadPrecomputedAllocations() {
		//read in allocations
		try {
			BufferedReader allocation_log = new BufferedReader(new FileReader("allocation_log.txt"));
			
			String text_line;
			while((text_line = allocation_log.readLine()) != null) {
				String params[] = text_line.split("\\s");
				switch(params[0].charAt(0)) {
				case 'i':
					Bucket b = buckets[Integer.parseInt(params[1])];
					for(int i = 0; i < params.length-2; i++)
						b.addLetter(intToLetter(Integer.parseInt(params[i+2])));
					break;
				case 'w':
					Letter letters[] = new Letter[params.length - 2];
					for(int i = 0; i < params.length-2; i++)
						letters[i] = intToLetter(Integer.parseInt(params[i+2]));
					((WordManagerPrecomputed)wordManager).addWord(Integer.parseInt(params[1]), letters);
					break;
				case 'a':
					((WordManagerPrecomputed)wordManager).addWordAssignment(Integer.parseInt(params[1]), Integer.parseInt(params[2]));
					break;
				case 'b':
				{
					int bucketbot_num = Integer.parseInt(params[1]);
					int bucket_num = Integer.parseInt(params[2]);
					int word_station_num = Integer.parseInt(params[3]);
					int word_num = Integer.parseInt(params[4]);
					Letter l = intToLetter(Integer.parseInt(params[5]));
					Word w = ((WordManagerPrecomputed)wordManager).wordNumberToWord.get(word_num);
					BucketbotTask t = BucketbotTask.createTaskTAKE_BUCKET_TO_WORD_STATION(
							buckets[bucket_num], l, wordStations[word_station_num], w);
					((BucketbotManagerExample)bucketbotManager).addBucketbotTask(bucketbots[bucketbot_num], t);					
					break;
				}
				case 's':
				{
					int bucketbot_num = Integer.parseInt(params[1]);
					int bucket_num = Integer.parseInt(params[2]);
					int storage_num = Integer.parseInt(params[3]);
					
					BucketbotTask t = BucketbotTask.createTaskSTORE_BUCKET( buckets[bucket_num],
							((BucketbotManagerExample)bucketbotManager).getUsedBucketStorageLocations().get(buckets[storage_num]));
					((BucketbotManagerExample)bucketbotManager).addBucketbotTask(bucketbots[bucketbot_num], t);
					break;
				}
				case 'l':
				{
					int bucketbot_num = Integer.parseInt(params[1]);
					int bucket_num = Integer.parseInt(params[2]);
					int letter_station_num = Integer.parseInt(params[3]);
					Letter l = intToLetter(Integer.parseInt(params[4]));
					
					BucketbotTask t = BucketbotTask.createTaskTAKE_BUCKET_TO_LETTER_STATION(
							buckets[bucket_num], l, letterStations[letter_station_num]);
					((BucketbotManagerExample)bucketbotManager).addBucketbotTask(bucketbots[bucketbot_num], t);
					
					((LetterManagePrecomputed)letterManager).addLetterAssignment(l, letter_station_num);
					break;
				}
				} //switch
			}
			allocation_log.close();
		}
		catch (IOException e) {
			System.out.println("Error opening file allocation_log.txt");
			return;
		}
	}

	/**Launches the Alphabet Soup simulation without user interface.
	 * @param args
	 */
	public static void main(String[] args) {
		simulationWorld = new SimulationWorldPrecomputed();
		if(simulationWorld.isUsingGUI())
		{
			RenderWindow.mainLoop(simulationWorld,
					((SimulationWorldPrecomputed)simulationWorld).simulationDuration);
			RenderWindow.destroyUserInterface();
		}
		else
			simulationWorld.update( ((SimulationWorldPrecomputed)simulationWorld).simulationDuration);

		SummaryReport.generateReport(simulationWorld);
	}
}
