/* * * * * * * * * * * * * 
 * INDR 568 - HW3		 *
 * Genetic Algorithm	 *
 * O. Baturhan Bayraktar * 
 * * * * * * * * * * * * */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


public class BinaryKnapsack_GA_Baturhan 
{
	static int seed=0;								//Initial Solution and Others Seed
	static int gseed=0;								//Problem Generation Seed
	static int max_iteration=1000;					//Stopping Criterion
	static int N=20;								//Number of genes on a chromosome
	static int[] weight_interval= {1,1000};			//Intance Generation parameters
	static int[] value_interval= {1,1000};			//Intance Generation parameters
	static int[] capacity_interval= {10,500000};	//Intance Generation parameters
	static int pop_size=20;							//Population Size
	static int mate_size=10;						//Mating Pool Size
	static double pc = 0.8;							//Crossover Probability
	static double gpc= 0.15;						//Binary Crossover Probability
	static boolean example=true;					//If you want to run given example
	static boolean selection=false;					//Roulette Wheel = true, Tournament=false
	
	static int capacity;
	static Random rnd=new Random(seed);
	static Random grnd=new Random(gseed);
	
	static List<gene_def> genes;
	
	static int id_gen=0;
	
	static class gene_def			//Definition of an object of the problem instance
	{
		int id;
		int weight;
		int value;
		
		public gene_def()
		{
			id=id_gen++;
		}
	}
	
	class gene			implements Cloneable				//This contains if the object is included in the solution or not
	{
		gene_def gene;
		int dtake;
		public Object clone(){try{return super.clone();}catch(Exception e){return null;}}
	}
		
	class chromosome	implements Cloneable				//List of the objects that will be taken (i.e. a solution or an individual)
	{
		int obj;
		double fitness;
		int totalweight;
		List<gene> sol=new ArrayList<>();
		public Object clone(){try{return super.clone();}catch(Exception e){return null;}}
	}
	
	static class current			//List of the chromosomes (i.e. solutions) that will generate new chromosomes.
	{
		static List<chromosome> population;
		static List<chromosome> mpool;
		static List<chromosome> children=new ArrayList<>();
		static int iteration=0;
		static int incumbent=-99999999;
		static chromosome incumbent_sol;
		static double average=0;
	}
	
	public static void main(String[] args) 
	{
		BinaryKnapsack_GA_Baturhan run = new BinaryKnapsack_GA_Baturhan();
		if(example) {run.ExampleInstance();}	else {run.InstanceGenerator();}
		run.PopulationGenerator();
		run.IncumbentUpdate();
		while(current.iteration<max_iteration)
		{
			current.iteration++;
			if(selection) {run.MatingPoolSelection_Roulette();}	else {run.MatingPoolSelection_Tournament();}
			run.NewGeneration();
			fitness_func_calculator(current.population);
			run.IncumbentUpdate();
		}
		//System.out.println(current.average);
	}
	
	public void ExampleInstance()
	{
		int[] data = {20, 5000, 349, 5, 833, 187, 684, 201, 309, 452, 471, 691, 680, 675, 96, 794, 246, 559, 872, 837, 219,
		855, 720, 17, 253, 922, 658, 258, 911, 21, 941, 357, 569, 754, 964, 957, 480, 337, 301, 151, 991, 616};
		
		N = data[0];
		capacity = data[1];

		genes=new ArrayList<>();
		gene_def g;
		for(int i=0;i<N;i++)
		{
			g=new gene_def();
			g.weight=data[2*(i+1)];
			g.value=data[2*(i+1)+1];
			genes.add(g);
		}
	}

	public void InstanceGenerator()
	{
		genes=new ArrayList<>();
		gene_def g;
		int total_cap=0;
		for(int i=0;i<N;i++)
		{
			g=new gene_def();
			g.weight=grnd.nextInt(weight_interval[1]-weight_interval[0]);
			g.weight+=weight_interval[0];
			total_cap+=g.weight;
			g.value=grnd.nextInt(value_interval[1]-value_interval[0]);
			g.value+=value_interval[0];
			genes.add(g);
		}
		capacity=grnd.nextInt(Math.min(total_cap-capacity_interval[0], capacity_interval[1]-capacity_interval[0]));
		capacity+=capacity_interval[0];
		System.out.println();
	}

	public void PopulationGenerator()
	{
		current.population=new ArrayList<>();
		for(int i=0;i<pop_size;i++)
		{
			chromosome c = new chromosome();
			for(int j=0;j<N;j++)
			{
				gene g=new gene();
				g.gene=genes.get(j);
				c.sol.add(g);
			}
			ChromosomeGenerator(c);
			obj_func_calculator(c);
			current.population.add(c);
		}
		fitness_func_calculator(current.population);
	}
	
	public void ChromosomeGenerator(chromosome c)
	{	
		List<Integer> ids=new ArrayList<>();
		int index;
		int total_cap=0;
		for(int i=0;i<N;i++)	{ids.add(i);}
		do
		{
			index=rnd.nextInt(ids.size());
			if(total_cap+c.sol.get(ids.get(index)).gene.weight<capacity)
			{
				c.sol.get(ids.get(index)).dtake=1;
				total_cap+=c.sol.get(ids.get(index)).gene.weight;
				ids.remove(index);
			}
			else
			{
				c.sol.get(ids.get(index)).dtake=0;
				ids.remove(index);
			}
		}	while(ids.size()>0);
		c.totalweight=total_cap;
	}
	
	public void MatingPoolSelection_Roulette()
	{
		current.mpool=new ArrayList<>();
		double total_fit=0;
		for(int i=0;i<current.population.size();i++)	{total_fit+=current.population.get(i).fitness;}
		
		double max=-9999999;
		for(int i=0;i<current.population.size();i++)
		{
			if(current.population.get(i).fitness>max)
			{
				max=current.population.get(i).fitness;
			}
		}
		current.average=(current.average*(current.iteration)+max)/(current.iteration+1);
		
		
		double s;
		double roulette;
		int i;
		
		for(int j=0;j<mate_size;j++)
		{
			i=-1;
			s=0;
			roulette=rnd.nextDouble()*total_fit;
			do
			{
				i++;
				s+=current.population.get(i).fitness;
			}	while(roulette>s);
			chromosome k=new chromosome();
			k.sol=new ArrayList<>();
			for(int o=0;o<N;o++)
			{
				gene h= new gene();
				int d1=current.population.get(i).sol.get(o).dtake;
				h.dtake=d1;
				gene_def d2=current.population.get(i).sol.get(o).gene;
				h.gene=d2;
				k.sol.add(h);
			}
			double f=current.population.get(i).fitness;
			k.fitness=f;
			TotalWeightCalculator(k);
			current.mpool.add(k);
			current.population.remove(i);
			total_fit-=k.fitness;
		}
	}
	
	public void MatingPoolSelection_Tournament()
	{
		current.mpool=new ArrayList<>();
		List<Integer> ind;
		int index;
		for(int i=0;i<mate_size;i++)
		{
			ind=new ArrayList<>();
			for(int z=0;z<current.population.size();z++)	{ind.add(z);}
			index=rnd.nextInt(current.population.size());
			chromosome c1= (chromosome) current.population.get(ind.get(index));
			int q=ind.get(index);
			ind.remove(index);
			index=rnd.nextInt(ind.size());
			chromosome c2= (chromosome) current.population.get(ind.get(index));
			ind.remove(index);
			if(c1.fitness>c2.fitness)
			{
				chromosome k=new chromosome();
				for(int y=0;y<N;y++)
				{
					gene g=new gene();
					int d1=c1.sol.get(y).dtake;
					g.dtake=d1;
					gene_def d2=c1.sol.get(y).gene;
					g.gene=d2;
					k.sol.add(g);
				}
				obj_func_calculator(k);
				TotalWeightCalculator(k);
				current.mpool.add(k);
				current.population.remove(q);
			}
			else
			{
				chromosome k=new chromosome();
				for(int y=0;y<N;y++)
				{
					gene g=new gene();
					int d1=c2.sol.get(y).dtake;
					g.dtake=d1;
					gene_def d2=c2.sol.get(y).gene;
					g.gene=d2;
					k.sol.add(g);
				}
				obj_func_calculator(k);
				TotalWeightCalculator(k);
				current.mpool.add(k);
				current.population.remove(index);
			}
		}
		
		double max=-9999999;
		for(int i=0;i<current.population.size();i++)
		{
			if(current.population.get(i).fitness>max)
			{
				max=current.population.get(i).fitness;
			}
		}
		current.average=(current.average*(current.iteration)+max)/(current.iteration+1);
		
	}
	
	public void NewGeneration()
	{
		List<Integer> ind=new ArrayList<>();
		for(int i=0;i<current.mpool.size();i++)	{ind.add(i);}
		int index;
		while(ind.size()>0)
		{
			index=rnd.nextInt(ind.size());
			chromosome parent1=new chromosome();
			for(int i=0;i<N;i++)	
			{
				gene g=new gene();
				int d1=current.mpool.get(ind.get(index)).sol.get(i).dtake;
				g.dtake=d1;
				gene_def d2=current.mpool.get(ind.get(index)).sol.get(i).gene;
				g.gene=d2;
				parent1.sol.add(g);
			}
			ind.remove(index);
			index=rnd.nextInt(ind.size());
			chromosome parent2=new chromosome();
			for(int i=0;i<N;i++)	
			{
				gene g=new gene();
				int d1=current.mpool.get(ind.get(index)).sol.get(i).dtake;
				g.dtake=d1;
				gene_def d2= current.mpool.get(ind.get(index)).sol.get(i).gene;
				g.gene=d2;
				parent2.sol.add(g);
			}
			ind.remove(index);
			TotalWeightCalculator(parent1);
			TotalWeightCalculator(parent2);
			obj_func_calculator(parent1);
			obj_func_calculator(parent2);
			
			chromosome child1=new chromosome();
			child1.sol=new ArrayList<>();
			for(int i=0;i<N;i++)	
			{
				gene g=new gene();
				int d1=parent1.sol.get(i).dtake;
				g.dtake=d1;
				gene_def d2=parent1.sol.get(i).gene;
				g.gene=d2;
				child1.sol.add(g);
			}
			chromosome child2=new chromosome();
			child2.sol=new ArrayList<>();
			for(int i=0;i<N;i++)	
			{
				gene g=new gene();
				int d1=parent2.sol.get(i).dtake;
				g.dtake=d1;
				gene_def d2=parent2.sol.get(i).gene;
				g.gene=d2;
				child2.sol.add(g);
			}
			TotalWeightCalculator(child1);
			TotalWeightCalculator(child2);
			if(rnd.nextDouble()<pc)
			{
				for(int i=0;i<N;i++)
				{
					if(rnd.nextDouble()<gpc)
					{	
						int d1=parent2.sol.get(i).dtake;
						child1.sol.get(i).dtake=d1;
						int d2=parent1.sol.get(i).dtake;
						child2.sol.get(i).dtake=d2;
						TotalWeightCalculator(child1);
						TotalWeightCalculator(child2);
						if(child1.totalweight>capacity || child2.totalweight>capacity)
						{
							child1.sol.get(i).dtake=d2;
							child2.sol.get(i).dtake=d1;
							TotalWeightCalculator(child1);
							TotalWeightCalculator(child2);
						}
					}
				}
			}
			
			TotalWeightCalculator(parent1);
			TotalWeightCalculator(parent2);
			
			TotalWeightCalculator(child1);
			TotalWeightCalculator(child2);
			
			if(rnd.nextDouble()<1-pc)
			{
				Mutation(child1);
				Mutation(child2);
			}
				
			obj_func_calculator(child1);
			obj_func_calculator(child2);
			
			
			List<chromosome> ind2=new ArrayList<>();
			ind2.add((chromosome) parent1);
			ind2.add((chromosome) parent2);
			ind2.add((chromosome) child1);
			ind2.add((chromosome) child2);
			PopulationUpdate(ind2);
		}
	}
	
	public void Mutation(chromosome c)
	{
		List<Integer> ind=new ArrayList<>();
		for(int i=0;i<N;i++)	{ind.add(i);}
		int index;
		for(int i=0;i<N;i++)
		{
			index=rnd.nextInt(ind.size());
			if(c.sol.get(ind.get(index)).dtake==0)
			{
				c.sol.get(ind.get(index)).dtake=1;
				break;
			}
			else
			{
				ind.remove(index);
			}
		}
		
		c=FeasibilityMutation(c);
	}
	
	public chromosome FeasibilityMutation(chromosome c)
	{
		Collections.sort(c.sol, new value_comp());
		for(int j=0;j<N;j++)
		{
			if(c.sol.get(j).dtake==1)
			{
				c.sol.get(j).dtake=0;
				TotalWeightCalculator(c);
				if(c.totalweight<capacity) {break;}
			}
		}
		Collections.sort(c.sol, new id_comp());
		
		return c;
	}
	
	public void PopulationUpdate(List<chromosome> l)
	{
		List<Integer> ind=new ArrayList<>();
		for(int i=0;i<l.size();i++)	{ind.add(i);}
		int index;
		for(int i=0;i<2;i++)
		{
			index=rnd.nextInt(ind.size());
			chromosome c1= (chromosome) l.get(ind.get(index));
			ind.remove(index);
			index=rnd.nextInt(ind.size());
			chromosome c2=(chromosome) l.get(ind.get(index));
			ind.remove(index);
			if(c1.obj>c2.obj)
			{
				chromosome k=new chromosome();
				for(int y=0;y<N;y++)
				{
					gene g=new gene();
					int d1=c1.sol.get(y).dtake;
					g.dtake=d1;
					gene_def d2=c1.sol.get(y).gene;
					g.gene=d2;
					k.sol.add(g);
				}
				obj_func_calculator(k);
				TotalWeightCalculator(k);
				current.population.add(k);
			}
			else
			{
				chromosome k=new chromosome();
				for(int y=0;y<N;y++)
				{
					gene g=new gene();
					int d1=c2.sol.get(y).dtake;
					g.dtake=d1;
					gene_def d2=c2.sol.get(y).gene;
					g.gene=d2;
					k.sol.add(g);
				}
				obj_func_calculator(k);
				TotalWeightCalculator(k);
				current.population.add(k);
			}
		}		
	}
	
	public void IncumbentUpdate()
	{
		for(int i=0;i<pop_size;i++)
		{
			if(current.population.get(i).obj>current.incumbent)
			{
				current.incumbent=current.population.get(i).obj;
				current.incumbent_sol=new chromosome();
				current.incumbent_sol.obj=current.population.get(i).obj;
				System.out.println("Iteration Number: " + current.iteration);
				System.out.println("Total Value:"+ current.incumbent_sol.obj);
				current.incumbent_sol.totalweight=current.population.get(i).totalweight;
				System.out.println("Total Weight:"+ current.incumbent_sol.totalweight);
				System.out.println("Taken Items: ");
				for(int j=0;j<N;j++)	
				{
					current.incumbent_sol.sol.add((gene) current.population.get(i).sol.get(j).clone());
					if(current.incumbent_sol.sol.get(j).dtake==1)
					{
						System.out.print(current.incumbent_sol.sol.get(j).gene.id+" - " + current.incumbent_sol.sol.get(j).gene.weight+" ");
					}
				}
				System.out.println();
				System.out.println();
			}

		}
	}
	
	public void obj_func_calculator(chromosome c)
	{
		for(int i=0;i<pop_size;i++)
		{
			c.obj+=c.sol.get(i).dtake*c.sol.get(i).gene.value;
		}
	}
	
	public static void fitness_func_calculator(List<chromosome> pop)		//Fitness Values are calculated over obj func with normalization
	{
		int total_obj=0;
		for(int i=0;i<pop.size();i++)
		{
			total_obj+=pop.get(i).obj;
		}
		for(int i=0;i<pop.size();i++)
		{
			pop.get(i).fitness=((double)pop.get(i).obj/((double)total_obj/(double)pop.size()));
		}
	}

	public void TotalWeightCalculator(chromosome c)
	{
		c.totalweight=0;
		for(int i=0;i<N;i++)	{c.totalweight+=(c.sol.get(i).dtake*c.sol.get(i).gene.weight);}
	}
	
	static class value_comp implements Comparator<gene> 
	{
		@Override
	    public int compare(gene g1, gene g2) 
	    {
	        return Double.compare(g1.gene.value,g2.gene.value);
	    }
	}
	
	static class id_comp implements Comparator<gene> 
	{
		@Override
	    public int compare(gene g1, gene g2) 
	    {
	        return Double.compare(g1.gene.id,g2.gene.id);
	    }
	}

}
