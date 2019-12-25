import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.*;
import java.util.Comparator;

public class GeneticAlgorithm {
	
	private Integer[][] population;
	private Model model;
	private int n_chrom;
	private int n_exams;
	private int n_students;
	private int n_time_slots;
	private int[][] nEe;
	private double[] fitness;		
	private Random rand;
	private boolean found;
	private Integer[] chromosome;
	private int nLoop;
	private int returnBack;
	private List<Integer> sortedExmToSchedule;
	private TabuSearch ts;
	private double bestBenchmark;
	private Integer[] bestSolution;
	 
	public GeneticAlgorithm(Model model, int n_chrom) {
		super();
		this.model = model;
		this.n_chrom = n_chrom;
		this.n_exams = model.getExms().size();
		this.n_students = model.getStuds().size();
		this.nEe = model.getnEe();	
		this.population = new Integer[n_chrom][n_exams];	
		this.fitness = new double[n_chrom];
		this.n_time_slots = model.getN_timeslots();
		this.n_exams = model.getExms().size();
		this.n_students = model.getStuds().size();
		this.population = new Integer[n_chrom][n_exams];	
		this.fitness = new double[n_chrom];
		this.n_time_slots = model.getN_timeslots();
		this.rand  = new Random();
		this.ts = new TabuSearch(this.model);
		this.bestBenchmark = Double.MIN_VALUE;
	}
	
	public void fit_predict() {
		this.getSortedExmToScheduleByNumStudent();
		this.initial_population_RANDOM();
		this.print_population();
		this.fitness();
		this.print_banchmark();
		this.getSortedExmToScheduleByNumStudent();
		int i = 0;
		
		while(true) {
			System.out.print("\n"+ i++ +"th Iteration - Time: "+(System.currentTimeMillis()-model.timeStart)/1000+" second\n");
			
			this.crossover();
			this.fitness();
			//this.print_population();
			//this.print_banchmark();
		}

	}
	
	private boolean are_conflictual(int time_slot, int exam_id, Integer[] chrom) {		
		for(int e = 0; e < this.n_exams; e++) {
			if(e != exam_id && chrom[e]!=null) {
				if(chrom[e] == time_slot && this.nEe[e][exam_id] != 0) {
					return true;
				}
			}
		}
		return false;
	}	
		
	/**
	 * Sort Exams by the number of students enrolled it, to try to assign exams first with 
	 * the biggest average of student in conflict
	 * 
	 */
	private void getSortedExmToScheduleByNumStudent() {
		HashMap<Integer,Double> exmStuds = new HashMap<Integer, Double>();
		
		for(int i = 0; i<this.n_exams;i++)
			exmStuds.put(i, (double) Arrays.stream(nEe[i]).filter( c -> c>0 ).count());//Arrays.stream(nEe[i]).average().getAsDouble());
		
		this.sortedExmToSchedule = exmStuds.entrySet().stream()
			    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			    .map(Map.Entry::getKey)
			    .collect(Collectors.toList());
		
		this.sortedExmToSchedule.add(-1);
		
	}
	
	
	private void initial_population_RANDOM() {
		
			for(int c=0; c<n_chrom; c++) {
				do {
				found = false;
				
				chromosome = new Integer[this.n_exams];
				nLoop = 0;
				
				doRecursive(chromosome,0,sortedExmToSchedule.get(0), this.n_exams);
				
				Collections.swap(sortedExmToSchedule, 0, c);
				
				
				} while(!isFeasible(chromosome));
				
				population[c] = chromosome.clone();
			}
			
		}
	
	
	/**
	 * Recursive method to generate population with getBestPath Method 
	 * and getSortedExmToScheduleByNumStudent method
	 * @param chrom
	 * @param step
	 * @param exam_id
	 * @param numExamsNotAssignedYet
	 */
	private void doRecursive(Integer[] chrom,int step, int exam_id, int numExamsNotAssignedYet) {
		
		if(numExamsNotAssignedYet > 0) {
			if(chrom[exam_id]!=null) {
				doRecursive(chrom, step+1, sortedExmToSchedule.get(step+1), numExamsNotAssignedYet);
				
				if(returnBack>0 ) {
					returnBack--; 
					return;
				}
			} else {
				for(int i : getBestPath(chrom)) { //timeslot
					if(!found) {
						if(!are_conflictual(i, exam_id, chrom)) {
							chrom[exam_id] = i;
							doRecursive(chrom, step+1, sortedExmToSchedule.get(step+1), numExamsNotAssignedYet-1);
							chrom[exam_id] = null;
							
							if(returnBack>0 ) {
								returnBack--; 
								return;
							} else 
								nLoop++;
			
						}
					} else return;
				}
				
				if(!found)
					nLoop++; // every time i fail a complete for cycle
				
				if(nLoop > n_exams && !found)  {
					returnBack = (int) (step*Math.random()); // number of time that i have to go back
					//System.out.print("Step "+ step + " returnBack "+returnBack+ " \n");
					nLoop = 0;
				} 
			}
			
		} else {
			found = true;
			chromosome = chrom.clone();
			//System.out.print("Found ");
		}
	}
	
	private void doRecursiveCrossover(Integer[] parent, Integer[] chrom,int step, int exam_id, int numExamsNotAssignedYet) {
		
		if(numExamsNotAssignedYet > 0 && exam_id>-1) {
			if(chrom[exam_id]!=null) 
				doRecursiveCrossover(parent, chrom, step+1, sortedExmToSchedule.get(step+1), numExamsNotAssignedYet);
			else {
				
				if(!are_conflictual(parent[exam_id], exam_id, chrom)) {
					chrom[exam_id] = parent[exam_id];
					doRecursiveCrossover(parent, chrom, step+1, sortedExmToSchedule.get(step+1), numExamsNotAssignedYet-1);
					chrom[exam_id] = null;
					
					if(returnBack>0 ) {
						returnBack--; 
						return;
					} else 
						nLoop++; 
				}
				
				for(Integer i : getBestPath(chrom)) {
					if(!found) {
						if(!are_conflictual(i, exam_id, chrom) && parent[exam_id] != i) {
							chrom[exam_id] = i;
							doRecursiveCrossover(parent, chrom, step+1, sortedExmToSchedule.get(step+1), numExamsNotAssignedYet-1);
							chrom[exam_id] = null;
							
							if(returnBack>0 ) {
								returnBack--; 
								return;
							} else 
								nLoop++; // every time i fail to assign i time slot to exam_id
			
						}
					} else return;
				}
				
				if(!found)
					nLoop++; // every time i fail a complete for cycle
			}
			
			if(nLoop > n_exams && !found)  {
				returnBack = (int) (step*Math.random()); // number of time that i have to go back
				//System.out.print("Step "+ step + " returnBack "+returnBack+ " \n");
				nLoop = 0;
			} 
			
			
		} else {
			found = true;
			chromosome = chrom.clone();
			System.out.print("Found ");
		}
	}
	
	
	/**
	 * Find the best order path to schedule timeslot in base al numero totale di studenti che 
	 * sostengono esami già schedulati in un timeslot. L'idea è di cercare prima di schedulare, se possibile, 
	 * un esame nei timeslot più affollati in modo da riservare i restanti timeslot agli esami più conflittuali
	 * @param chrom
	 * @return list of sorted timeslot by the number of students enrolled in the exam assigned yet
	 */
	public List<Integer> getBestPath(Integer[] chrom) {
		List<Integer> path;
		HashMap<Integer,Integer> numStudentTimeSlot = new HashMap<Integer, Integer>();
		
		for(int k=1; k<=this.n_time_slots;k++)
			numStudentTimeSlot.put(k, 0);
		
		for(int i=0; i<this.n_exams; i++) {
			if( chrom[i] != null ) {
				int numStud = (numStudentTimeSlot.get(chrom[i]) + model.getExms().get(i).getNumber_st_enr());
				numStudentTimeSlot.replace(chrom[i], numStud);
			}
		} 
		
		path =  numStudentTimeSlot.entrySet().stream()
			    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			    .map(Map.Entry::getKey)
			    .collect(Collectors.toList());
		
		// if i just started and my chromosome is empty, i generate a random path
		if(numStudentTimeSlot.values().stream().mapToInt(Integer::intValue).sum() == 0)
			Collections.shuffle(path);
		
		return path;
	}
	

	/**
	 * This method computes fitness for each chromosomes
	 */
	private void fitness() {
		double penalty;		
		int distance;
		 
		for(int c=0; c < n_chrom; c++) { // For each chroms
			penalty = 0;
			distance = 0;
			
			for(int e1 = 0; e1 < n_exams; e1++) { // For each exams
				for(int e2 = e1 + 1; e2 < n_exams; e2++) { // For each other exams
					distance = Math.abs(population[c][e1] - population[c][e2]);
					if(distance <= 5) {
						penalty += (Math.pow(2, (5-distance))* this.nEe[e1][e2]);
					}
				}
				
			}	
			penalty = penalty / this.n_students;
			this.fitness[c] =  1 / penalty;	
		}			
	}
	
	
		/**
		 * This method computes fitness for a chromosome
		 * @param chrom
		 * @return
		 */
		private double getChromFitness(Integer[] chrom) {
			double penalty = 0;		
			int distance = 0;
			
			for(int e1 = 0; e1 < n_exams; e1++) { // For each exams
					for(int e2 = e1 + 1; e2 < n_exams; e2++) { // For each other exams
						distance = Math.abs(chrom[e1] - chrom[e2]);
						if(distance <= 5) {
							penalty += ( Math.pow(2, 5-distance) * this.nEe[e1][e2]);
						}
					}
					
				penalty = penalty / this.n_students;
			}	

			return  (1 / penalty);	
		}
		

		
	private void crossover() {
		
		if(Arrays.stream(fitness).filter(c -> c>0).max().getAsDouble() > bestBenchmark)
			bestBenchmark = Arrays.stream(fitness).filter(c -> c>0).max().getAsDouble();
		
		/*for(int i=0; i<this.n_chrom; i++)
			  if(getChromFitness(population[i])>=bestBenchmark) 
				  bestSolution = population[i].clone();*/
				  
		
		System.out.print("Best Bench: "+1/bestBenchmark+/*"\nBest Solution: "+Arrays.toString(bestSolution)+*/"\n");
		
		int indParent1 = 0, indParent2 = 0;
		double minValueP1 = Double.MAX_VALUE, minValueP2 = Double.MAX_VALUE;
		Integer[][] parents = new Integer[2][n_exams];
		
		// Find the two worst fitness in my population
		  for(int i=0;i<this.n_chrom;i++){
			  if(fitness[i] < minValueP1){
				  minValueP1 = fitness[i];
				  indParent1 = i;
				}
		  }
		  parents[0] = population[indParent1].clone();
		  
		  for(int i=0;i<this.n_chrom;i++){
			  if(fitness[i] < minValueP2 && indParent1!=i && !Arrays.equals(parents[0],population[i]) ){
				  minValueP2 = fitness[i];
				  indParent2 = i;
				}
		  }
		  parents[1] = population[indParent2].clone(); 
		  
		  // Calculate a random crossing section
		  int crossingSecStart = rand.nextInt(n_exams);
		  int crossingSecEnd = (int) ((n_exams-crossingSecStart-1)*Math.random() + crossingSecStart);
		  Integer[][] childs = new Integer[2][n_exams];
		  
		  // System.out.print("Crossing Section: " + crossingSecStart + " - " + crossingSecEnd + "\n");
		  
		  
		  // Swap crossing section between two chromosome 
		  for(int i = crossingSecStart; i <= crossingSecEnd; i++) {
			  childs[0][i] = parents[1][i];
			  childs[1][i] = parents[0][i];
		  }
		
		  // Order Crossover modified 
		  for(int i=0; i<2; i++) {
			  //getSortedExmToScheduleByNumStudent();
			  //int k = 0;
			  
			  do {
				  int numExamsNotAssignedYet = (this.n_exams-(crossingSecEnd+1-crossingSecStart));
				  found = false;
				  chromosome = new Integer[this.n_exams];
				  
				  doRecursive(childs[i],0,sortedExmToSchedule.get(0), numExamsNotAssignedYet);
				  
				  //Collections.swap(sortedExmToSchedule, 0, k++);
				  
				  //if(k > this.n_exams)
					//  return;
			  
			  } while(!isFeasible(chromosome)) ;
			
			  childs[i] = chromosome.clone();
			  

		  } 
		  
		  // Local Search 

		  /*this.fitness();
		  double rapp =  (Arrays.stream(this.fitness).average().getAsDouble()/Arrays.stream(this.fitness).max().getAsDouble());
		  
		  if(rapp>0.5) {
			  for(int k = 0; k<2; k++) {
				  int indRand = getBadExam(childs[k]); // take the exam that has the worst weight in fitness formula and i try to improve it
				  double fitness = getChromFitness(childs[k]);
				  Integer[] neighborhood = childs[k].clone();
				  
				  for(Integer t : getBestPath(neighborhood)) {
					  if(!are_conflictual(t,indRand, neighborhood)) {
						  neighborhood[indRand] = t; // change a gene to find a new chromosome 
						  if(fitness < getChromFitness(neighborhood)) { // evaluate the fitness of the solution found in my neighborhood 
							  fitness = getChromFitness(neighborhood);
							  childs[k] = neighborhood.clone();
						  }
						  
					  }
				  }  
			  } 
		  }*/
		  
		  
		  this.fitness();
		  double rapp =  (Arrays.stream(this.fitness).average().getAsDouble() // da teoria libro
				  /Arrays.stream(this.fitness).max().getAsDouble());
		  
		  if(rapp>0.75) {
		  // Replace my bad chromosomes in the population with the new. 
		  //if(getChromFitness(childs[0]) > getChromFitness(population[indParent1]))
			  population[indParent1] = ts.run(childs[0]).clone();
		  
		 // if(getChromFitness(childs[1]) > getChromFitness(population[indParent2]))
			  population[indParent2] = ts.run(childs[1]).clone();
		  } else {
			  population[indParent1] = childs[0].clone();
			  population[indParent2] = childs[1].clone();
		  }
		  
		  if((System.currentTimeMillis()-model.timeStart) > (300*1000))
			  System.exit(1);
		  
	}
	
	private boolean isFeasible(Integer[] chrom) {
		 for(int e = 0; e<this.n_exams; e++) {
			  if(chrom[e] == null || are_conflictual(chrom[e], e, chrom))
				  return false;
		  }
		 
		return true;
	}
	
	
	/**
	 * Take the exam that has the worst weight in the fitness formula
	 * @param chrome
	 * @return
	 */
	private List<Integer> getBadExams(Integer[] chrome ) {
		List<Integer> idBadExams ;
		HashMap<Integer,Double> sortExam = new HashMap<Integer, Double>();
		int distance;
		double penalty;
		
		for(int e1 = 0; e1 < n_exams; e1++) { // For each exams
			penalty = 0;
			
			for(int e2 = e1 + 1; e2 < n_exams; e2++) { // For each other exams
				distance = Math.abs(chrome[e1] - chrome[e2]);
				if(distance <= 5) {
					penalty += (Math.pow(2, (5-distance)) * this.nEe[e1][e2]);
				}
			}

			sortExam.put(e1, penalty);
		}
		
		idBadExams = sortExam.entrySet().stream()
	    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
	    .map(Map.Entry::getKey)
	    .collect(Collectors.toList());
		
		return idBadExams;
	}
	
	private void print_population() {
		int count = 1;
		System.out.println("Population: ");
		for(Integer[] chrom : population) {
			System.out.println("Chrom " + count + ": " + Arrays.toString(chrom));
			count++;
		}
	}
	
	private void print_fitness() {

		System.out.println("Fitness: ");
		for (int i=0; i < n_chrom; i++) {
			System.out.println("Fitness" + (i+1) + ": " + 1/fitness[i]);
		}
	}
	
	private void print_banchmark() {

		System.out.println("Banchmark: ");
		for (int i=0; i < n_chrom; i++) {
			System.out.println("Banchmark" + (i+1) + ": " + model.computePenalty(population[i]));
		}
	}

	public Integer[][] getPopulation() {
		return population;
	}

	public int getN_chrom() {
		return n_chrom;
	}

	public int getN_exams() {
		return n_exams;
	}

	public int getN_students() {
		return n_students;
	}

	public double[] getFitness() {
		return fitness;
	}
	
	
	 
}