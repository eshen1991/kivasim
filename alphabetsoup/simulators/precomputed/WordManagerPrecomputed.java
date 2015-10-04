/**
 * 
 */
package alphabetsoup.simulators.precomputed;

import java.util.*;

import alphabetsoup.framework.Letter;
import alphabetsoup.framework.Updateable;
import alphabetsoup.framework.Word;
import alphabetsoup.framework.WordList;
import alphabetsoup.framework.WordStation;

/**Manages the list of words and the word stations they will be assigned to
 * based on precomputed resource assignments.
 * This class also manipulates WordList based on its allocations.
 * @author Chris Hazard
 */
public class WordManagerPrecomputed implements Updateable {

	//stores the word number assignment lists for each station
	// the next word to take is the last element in each array
	private List<List<Integer>> wordAssignments = new ArrayList<List<Integer>>();
	
	//way to look up a word based on its word number 
	public List<Word> wordNumberToWord = new ArrayList<Word>();
	
	public WordManagerPrecomputed(int num_word_stations) {
		for(int i = 0; i < num_word_stations; i++)
			wordAssignments.add(i, new ArrayList<Integer>());
	}
	
	/**Adds a precomputed word assignment to the beginning of the list
	 * @param wordNumber the word number to be assigned
	 * @param wordStationNumber the station the word will be assigned to
	 */
	public void addWordAssignment(int wordNumber, int wordStationNumber) {
		//put on the beginning of the array, so the beginning is always the latest assignment
		wordAssignments.get(wordStationNumber).add(0, wordNumber);
	}
	
	/**Adds a precomputed word to the word list (maintained by this class) 
	 * @param wordNumber the number of the word to add
	 * @param letters letters that the word is made of
	 */
	public void addWord(int wordNumber, Letter letters[]) {
		WordList wl = SimulationWorldPrecomputed.getSimulationWorld().getWordList();
		Word w = new Word(letters);
		wl.getAvailableWords().add(wordNumber, w);
		
		//also keep track of the word number locally
		wordNumberToWord.add(wordNumber, w);
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#getNextEventTime(double)
	 */
	public double getNextEventTime(double cur_time) {
		return Double.POSITIVE_INFINITY;
	}

	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#update(double, double)
	 */
	public void update(double last_time, double cur_time) {
		WordList wl = SimulationWorldPrecomputed.getSimulationWorld().getWordList();
		
		//see if all done
		if(wl.getAvailableWords().size() == 0)
			return;

		WordStation ws[] = SimulationWorldPrecomputed.getSimulationWorld().getWordStations();
		for(int i = 0; i < ws.length; i++) {
			//give the station words if it needs them
			if(ws[i].getAssignedWords().size() < ws[i].getCapacity()) {
				List<Integer> next_assignments = wordAssignments.get(i);
				//make sure have a new one to give
				if(next_assignments.size() > 0) {
					int assignment = next_assignments.remove(next_assignments.size() - 1);
					Word assigned_word = wordNumberToWord.get(assignment);

					//assign word
					ws[i].assignWord(assigned_word);
					wl.getAvailableWords().remove(assigned_word);
					
					//announce assigned word
					SimulationWorldPrecomputed.getSimulationWorld().bucketbotManager.newWordAssignedToStation(assigned_word, ws[i]);
				}
			}
		}
	}

}
