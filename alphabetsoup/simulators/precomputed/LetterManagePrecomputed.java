/**
 * 
 */
package alphabetsoup.simulators.precomputed;

import java.util.*;

import alphabetsoup.framework.Letter;
import alphabetsoup.framework.LetterStation;
import alphabetsoup.framework.Updateable;

/**Manages the list of letters and the letter stations they will be assigned to
 * based on precomputed resource assignments.
 * @author Chris Hazard
 */
public class LetterManagePrecomputed implements Updateable {
	
	//stores the letter assignment lists for each station
	// the next letter to take is the last element in each array
	private List<List<Letter>> letterAssignments = new ArrayList<List<Letter>>();
	
	public LetterManagePrecomputed(int num_letter_stations) {
		for(int i = 0; i < num_letter_stations; i++)
			letterAssignments.add(i, new ArrayList<Letter>());
	}
	
	/**Adds a precomputed letter assignment to the beginning of the list
	 * @param l the letter to be assigned
	 * @param letterStationNumber the station the letter bundle will be assigned to
	 */
	public void addLetterAssignment(Letter l, int letterStationNumber) {
		//put on the beginning of the array, so the beginning is always the latest assignment
		letterAssignments.get(letterStationNumber).add(0, l);
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
		
		LetterStation ls[] = SimulationWorldPrecomputed.getSimulationWorld().getLetterStations();
		for(int i = 0; i < ls.length; i++) {
			//give the station letters if it needs them
			if(ls[i].getAssignedLetters().size() < ls[i].getCapacity()) {
				List<Letter> next_assignments = letterAssignments.get(i);
				//make sure have a new one to give
				if(next_assignments.size() > 0) {
					Letter l = next_assignments.remove(next_assignments.size() - 1);

					ls[i].addBundle(l);
					SimulationWorldPrecomputed.getSimulationWorld().bucketbotManager.newLetterBundleAssignedToStation(l, ls[i]);
				}
			}
			
		}
	}
}
