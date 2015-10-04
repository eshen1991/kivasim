/**
 * 
 */
package alphabetsoup.simulators.precomputed;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

import alphabetsoup.framework.*;
import alphabetsoup.waypointgraph.*;

/**
 * @author Chris Hazard
 *
 */
public class ExternalConfigurationGenerator extends SimulationWorldPrecomputed {

	/**Changes a Letter into a unique integer based on the leter itself and its color
	 * @param l Letter
	 * @return unique integer
	 */ 
	static int letterToInt(Letter l) {
		return 256 * l.getColorID() + (int)l.getLetter();
	}
	
	/**Generates configuration text as a List of Strings.
	 * @return a List of Strings in sequential order
	 */
	public List<String> generateConfigurationText() {
		//give 4 significant digits
		DecimalFormat four_digits = new DecimalFormat("0.000");
		List<String> lines = new ArrayList<String>();
		
		//lines.add("value=" + four_digits.format(3.0));
		lines.add("num_buckets=" + buckets.length);
		lines.add("bucket_capacity=" + buckets[0].getCapacity());
		lines.add("num_bucketbots=" + bucketbots.length);
		lines.add("bundle_size=" + letterStations[0].getBundleSize());
		lines.add("letter_station_capacity=" + letterStations[0].getCapacity());
		lines.add("word_station_capacity=" + wordStations[0].getCapacity());
		
		lines.add("num_word_stations=" + wordStations.length);
		lines.add("num_letter_stations=" + letterStations.length);
		
		lines.add("bucket_pickup_setdown_time=" + bucketbots[0].getBucketPickupSetdownTime());
		lines.add("letter_to_bucket_time=" + letterStations[0].getLetterToBucketTime());
		lines.add("bucket_to_letter_time=" + wordStations[0].getBucketToLetterTime());
		lines.add("word_completion_time=" + wordStations[0].getWordCompletionTime());
		lines.add("incomming_word_buffer_size=" + wordList.getAvailableWords().size());
		
		//////////////////////////
		//put graph travel times
		
		//build waypoint list... letter stations, then word stations, then bucket storage 
		WaypointGraph wpg = SimulationWorldPrecomputed.getSimulationWorld().waypointGraph;
		ArrayList<Waypoint> wp = new ArrayList<Waypoint>();
		for(int i = 0; i < letterStations.length; i++)
			wp.add(wpg.getLetterStationWaypoint(letterStations[i]));
		for(int i = 0; i < wordStations.length; i++)
			wp.add(wpg.getWordStationWaypoint(wordStations[i]));
		for(int i = 0; i < buckets.length; i++)
			wp.add(wpg.getBucketWaypoint(buckets[i]));
		
		//find fastest time to get to each place
		Bucketbot bb = bucketbots[0];
		//for each waypoint
		for(int i = 0; i < wp.size(); i++) {
			String s = "";
			//find time to each other waypoint
			for(int j = 0; j < wp.size(); j++) {
				float travel_time = 0.0f;
				if(i != j)
				{
					//sum the times to travel to each waypoint along the way 
					Waypoint w = wp.get(i);
					Waypoint end = wp.get(j);
					while(w != null) {
						Waypoint next = ((BucketbotDriver)bb).getNextWaypointTo(w, end);
						//accumulate time
						//TODO have better time approximation
						float ave_velocity = 9.18f; //bb.getMaxVelocity();
						travel_time += w.getPathDistance(next) / ave_velocity;
						if(next == end)
							break;
						w = next;
					}
					
				}
				if(j > 0) s += " ";
				s += four_digits.format(travel_time);
			}
			lines.add(s);
		}
		
		//populate buckets
		//clear bucket contents from before
		for(Bucket b : buckets)
			b.getLetters().clear();
		initializeBucketContentsRandom(Float.parseFloat(params.getProperty("initial_inventory")), Integer.parseInt(params.getProperty("bundle_size")));
		
		///////////////////////////
		//put buckets' initial inventory
		for(Bucket b : buckets) {
			//put inventory size
			lines.add(""+b.getLetters().size());
			
			String letters = "";
			for(Letter l : b.getLetters())
				letters += " " + letterToInt(l); 
			
			if(b.getLetters().size() > 0)
				lines.add(letters.substring(1));
		}
		
		/////////////////////////
		//put words to complete
		int total_num_letters = 100;
		//clear out current words
		wordList.getAvailableWords().clear();
		int num_letters = 0;
		ArrayList<Word> words = new ArrayList<Word>();
		while(num_letters < total_num_letters) {
			Word w = wordList.takeAvailableWord(0);
			words.add(w);
			num_letters += w.getOriginalLetters().length;
		}
		//print out num words to follow
		lines.add(""+words.size());
		
		//print out words, length first
		num_letters = 0;
		for(Word w : words) {
			//print out length
			String s = "";
			//clamp last word so don't go over number of letters to print
			int num_letters_to_print = w.getOriginalLetters().length;
			if(num_letters + num_letters_to_print > total_num_letters)
				num_letters_to_print = total_num_letters - num_letters;
			s += num_letters_to_print;
			for(int i = 0; i < num_letters_to_print; i++)
				s += " " + letterToInt(w.getOriginalLetters()[i]);
			
			lines.add(s);			
		}
		
		return lines;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		simulationWorld = new ExternalConfigurationGenerator();

		FileWriter outfile;
		PrintWriter outf;
		try {
			outfile = new FileWriter("AlphabetSoupExternalConfiguration.txt");
			outf = new PrintWriter(outfile);
		}
		catch (Throwable e) {
			System.out.println("Could not open file AlphabetSoupExternalConfiguration.txt");
			return;
		}
		
		for(String s : ((ExternalConfigurationGenerator)simulationWorld).generateConfigurationText())
			outf.println(s);
		outf.close();		
	}
}
