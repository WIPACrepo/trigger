package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.util.DOMRegistry;
import icecube.daq.util.DeployedDOM;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Arrays;
import java.lang.Math;

import org.apache.log4j.Logger;

/**
 * This trigger looks for HLC pairs and groups them in 3-tuples. If a tuple fullfills certain conditions ( in
 *CheckTriple() ) the internal trigger_info structure will raise n_tuples by 1. At the end one can control if a certain
 *amount of overlapping tuples is sufficient for triggering by the min_n_tuples parameter.
 * For the testrun the parameters which will yield 30 Hz trigger rate are pre-set already.
 * @author gluesenkamp
 *
 */
public class SlowMPTrigger extends AbstractTrigger
{
    private long t_proximity; // t_proximity in nanoseconds, eliminates most muon_hlcs
    private long t_min;
    private long t_max;
    private int delta_d;
    private double rel_v;
    private int min_n_tuples;
    private long max_event_length;
    
    private LinkedList<min_hit_info> one_hit_list;
    private LinkedList<min_hit_info> two_hit_list;
    private LinkedList<min_trigger_info> trigger_list;
    
    private long muon_time_window;
    
    // wont use those configure-booleans - ok ?
 /*   private boolean t_proximity_configured = false;
    private boolean t_min_configured = false;
    private boolean t_max_configured = false;
    private boolean delta_d_configured = false;
    private boolean rel_v_configured = false;
    private boolean min_n_tuples_configured = false;*/
    
    
    private class min_hit_info
    {
        min_hit_info(IHitPayload new_hit)
	{
	    hit  = new_hit;
	    
	    utc_time = hit.getHitTimeUTC().longValue();
	    mb_id = String.format("%012x", hit.getDOMID().longValue());
	    
	}
	
        private IHitPayload hit;
	
	private String mb_id;
	private long utc_time;
	
	public String get_mb_id()
	{
	    return mb_id;
	}
	
	public long get_time()
	{
	    return utc_time;
	}
	
	public IHitPayload get_hit()
	{
	    return hit;
	}
    }
    
    private class min_trigger_info
    {
    	private int num_tuples;
    	private LinkedList<IHitPayload> hit_list;
    	
    	min_trigger_info(IHitPayload first_trigger_hit, IHitPayload last_trigger_hit)
    	{
    		hit_list = new LinkedList<IHitPayload>();
    		
    		hit_list.add(first_trigger_hit);
    		hit_list.add(last_trigger_hit);
    		
    		num_tuples = 1;
    	}
    	
    	public IHitPayload get_first_hit()
    	{
    		return hit_list.get(0);
    	}
    	public void set_first_hit(IHitPayload first)
    	{
    	    hit_list.set(0, first);	
    	}
    	
    	public IHitPayload get_last_hit()
    	{
    		return hit_list.get(1);
    	}
    	public void set_last_hit(IHitPayload last)
    	{
    	    hit_list.set(1,last);	
    	}
    	
    	public void increase_tuples()
    	{
    		num_tuples+=1;
    	}
    	
    	public int get_num_tuples()
    	{
    		return num_tuples;
    	}
    	
    	public LinkedList<IHitPayload> get_hit_list()
    	{
    		return hit_list;
    	}
    }

    public SlowMPTrigger()
    {
        one_hit_list = new LinkedList<min_hit_info>();
        two_hit_list = new LinkedList<min_hit_info>();
        trigger_list = new LinkedList<min_trigger_info>();

        // parameters that are important for the behaviour of the trigger
        // times are set in nanoseconds, later translated to tens of nanoseconds
        set_t_proximity(2500);
        set_t_min(0);
        set_t_max(500000);
        set_delta_d(500);
        set_rel_v(3);
	set_min_n_tuples(1);
	    
	// max_event_length is in tens of nanoseconds    
	max_event_length = 100000000;  // we dont want longer events thatn 10 milliseconds, should not occur in 30 min run 
	    
	muon_time_window = -1;
	configHitFilter(5);
	    
        System.out.println("INITIALIZED SLOWMPTRIGGER");
    }
    
    @Override
    public void addParameter(TriggerParameter parameter) throws UnknownParameterException,
            IllegalParameterValueException
    {
        if (parameter.getName().equals("t_proximity"))
            set_t_proximity(Integer.parseInt(parameter.getValue()));
        else if (parameter.getName().equals("t_min"))
            set_t_min(Integer.parseInt(parameter.getValue()));
        else if (parameter.getName().equals("t_max"))
            set_t_max(Integer.parseInt(parameter.getValue()));
        else if (parameter.getName().equals("delta_d"))
            set_delta_d(Integer.parseInt(parameter.getValue()));
	else if (parameter.getName().equals("rel_v"))
            set_rel_v(Double.parseDouble(parameter.getValue()));    
	else if (parameter.getName().equals("min_n_tuples"))
            set_delta_d(Integer.parseInt(parameter.getValue()));   
        else if (parameter.getName().equals("domSet")){
	    domSetId = Integer.parseInt(parameter.getValue());
	    configHitFilter(domSetId);
	}   
        super.addParameter(parameter);
    }
    
    // t_proximity 

    public long get_t_proximity()
    {
        return t_proximity / 10L;
    }

    public void set_t_proximity(long val)
    {
        this.t_proximity = val * 10L;
    }
    
    // t_min
    
    public long get_t_min()
    {
        return t_min / 10L;
    }

    public void set_t_min(long val)
    {
        this.t_min = val * 10L;
    }
    
    // t_max
    
    public long get_t_max()
    {
        return t_max / 10L;
    }

    public void set_t_max(long val)
    {
        this.t_max = val * 10L;
    }
    
    // delta_d
    
    public int get_delta_d()
    {
        return delta_d;
    }

    public void set_delta_d(int val)
    {
        this.delta_d = val;
    }
    
    // rel_v
    
    public double get_rel_v()
    {
        return rel_v;
    }

    public void set_rel_v(double val)
    {
        this.rel_v = val;
    }
    
    // min_n_tuples
    
    public int get_min_n_tuples()
    {
        return min_n_tuples;
    }

    public void set_min_n_tuples(int val)
    {
        this.min_n_tuples = val;
    }
    
    @Override
    public void flush()
    {
        one_hit_list.clear();
        two_hit_list.clear();
        
        
        muon_time_window = -1;
    }

    @Override
    public void runTrigger(IPayload payload) throws TriggerException
    {
	    if (!(payload instanceof IHitPayload)) 
                throw new TriggerException(
                        "Payload object " + payload + " cannot be upcast to IHitPayload."
                        );
            // This upcast should be safe now
            IHitPayload hitPayload = (IHitPayload) payload;

            // Check hit type and perhaps pre-screen DOMs based on channel (HitFilter)
            if (getHitType(hitPayload) != AbstractTrigger.SPE_HIT) return;
            if (!hitFilter.useHit(hitPayload)) return;
	
	    if(one_hit_list.size() == 0) // size is 0, so just add it to the list
	    {    
	         min_hit_info new_hit = new min_hit_info(hitPayload);
		 
	         one_hit_list.add(new_hit);
	    }
	    else // not zero, so compare payload with current one_hit_list if any hlc pair can be formed
	    {
	        
		min_hit_info new_hit = new min_hit_info(hitPayload);
		
	    	//System.out.format("list contains %d entries..%n TWOHIT_list contains %d entries..%n", one_hit_list.size(), two_hit_list.size() );
	        while( new_hit.get_time() - one_hit_list.element().get_time() > 10000L)
	        // makes no sense to compare HLC hits that are longer apart than 1000 nanoseconds, so remove first from list
	        {
	            //System.out.format("REMOVED FIRST ELEMENT !");
	            one_hit_list.removeFirst();
	        
	            if(one_hit_list.size() == 0)
	            {
	            	break;
	            }
	        }
	    
	        //ListIterator iter = one_hit_list.listIterator(); added the following
		// lines, make use of toArray instead of iterator for some speed
		
	     	min_hit_info[] one_hit_array = one_hit_list.toArray(new min_hit_info[0]);
	        int initial_size = one_hit_list.size();
	        int no_of_removed_elems = 0;
		
	        //while(iter.hasNext())
		for(int i = 0; i < initial_size; i++)
	        {
	            //System.out.format("muon_time_window: %d", muon_time_window);
	            min_hit_info check_payload = HLCPairCheck(one_hit_array[i], new_hit);
		    
	            if(check_payload != null)
	            {   
	            	if(two_hit_list.size() == 0)        // the pair list is empty
	            	{
	        	    	if(muon_time_window == -1)
	        	    	{
	        		    	two_hit_list.add(check_payload);
	        	    		// set earliest payload of interest ?=!
					setEarliestPayloadOfInterest(check_payload.get_hit());
	        	    	}
	        	     	else
	        	    	{
	        	    		if(check_payload.get_time() - muon_time_window <= t_proximity)
	        	    		{
	        		    		muon_time_window = check_payload.get_time();
	        	    		}
	        	    		else
	        	    		{
	        		     		two_hit_list.add(check_payload);
	        		     		muon_time_window = -1;
						setEarliestPayloadOfInterest(check_payload.get_hit());
		        	    		// set earliest payload of interest ?=!
	        		    	}
	        	     	}
	        	    }
	        	    else // the pair list is not empty
	             	    {
	        	     	if(muon_time_window == -1)
	        	    	{ 
	        	    		if(check_payload.get_time() - two_hit_list.getLast().get_time() <= t_proximity)
	        		    	{
	        		    		muon_time_window = check_payload.get_time();
	        		     		two_hit_list.removeLast();
	        			    }
	        		    	else
	        	    		{
	        		    		if(check_payload.get_time()-two_hit_list.getLast().get_time() < t_max && check_payload.get_time() - two_hit_list.getFirst().get_time() < max_event_length)
	        		    		{
	        		     			two_hit_list.add(check_payload);
	        		    		}
	        		    		else
	        			    	{
	        			    		CheckTriggerStatus(); // checks current two_hit_list for 3-tuples
	        			    		two_hit_list.add(check_payload);
	        			    	}
	        		    	}
	        	     	}
	        	    	else
	        	    	{
	        	    		if(check_payload.get_time() - muon_time_window <= t_proximity)
	        	    		{
	        		    		muon_time_window = check_payload.get_time();
	        		    	}
	        		    	else
	        		    	{
	        		    		muon_time_window = -1;
	        	    			if(check_payload.get_time()- two_hit_list.getLast().get_time() < t_max && check_payload.get_time() - two_hit_list.getFirst().get_time() < max_event_length)
	        	    			{
	        	    				two_hit_list.add(check_payload); // checks current two_hit_list for 3-tupleseckTriggerStatus(); // checks current two_hit_list for 3-tuples
	        		    		}
	        		     		else
	        		    		{
	        		    			CheckTriggerStatus();
		        	    			two_hit_list.add(check_payload);
	        		    		}
	        	    		}
	        	    	}
	            	}
			
			one_hit_list.remove(i-no_of_removed_elems);
	             	++no_of_removed_elems;
			
	             	//iter.remove(); 	
	            }
	            else
	            {
	             	//System.out.format("NULLLLLLL");
	            }
	                                              // with the remaining Payloads in the linked list
	        }
	    
	        one_hit_list.add(new_hit); // at the end add the current hitPayload for further comparisons
	    }
	//    System.out.format("muon_time_window: %d", muon_time_window);
      //  System.out.format("list contains %d entries..%n TWOHIT_list contains %d entries..%n", one_hit_list.size(), two_hit_list.size() );
    
   
    }
    
    private void CheckTriggerStatus()
    {
    	int list_size = two_hit_list.size();
    	
    	if(list_size >= 3)
    	{
    		min_hit_info[] q = two_hit_list.toArray(new min_hit_info[0]);
    		
    		for(int i = 0; i < list_size-2; i++)
    		{
    			for(int j = i+1; j < list_size-1; j++)
    			{
    				for(int k = j+1; k < list_size; k++)
    				{
    					//System.out.println("Checking Triple...%n");
    					CheckTriple(q[i], q[j], q[k]);
    				}
    			}
    		}
    		
    	}
    	
    	ListIterator list_iterator = trigger_list.listIterator();
    	
    	while(list_iterator.hasNext())
    	{
    		min_trigger_info info = (min_trigger_info)list_iterator.next();
    		
    		// Form trigger with each trigger_info object, if num_tuples is fullfilled
    		
    		if(info.get_num_tuples() >= min_n_tuples)
    		{
    			//System.out.format("FOUND TRIGGER: start: %d, end :%d with %d tuples%n",info.get_first_hit().get_time(), info.get_last_hit().get_time(), info.get_num_tuples() );
    			
    			// form trigger here for each trigger_info
    			formTrigger(info.get_hit_list(), null, null);
    			
    		}
    	}
    	
    	trigger_list.clear();
    	two_hit_list.clear();
    }
    public boolean isConfigured()
    {
        return true;
    }
/*
    
    Function to check for a triple combination fullfilling the parameter boundaries
    
    */
    
    
    private void CheckTriple(min_hit_info hit1, min_hit_info hit2, min_hit_info hit3)
    {
    	long t_diff1 = hit2.get_time() - hit1.get_time();
    	long t_diff2 = hit3.get_time() - hit2.get_time();
    	
    	if(t_diff1 > t_min && t_diff2 > t_min && t_diff1 < t_max && t_diff2 < t_max)
    	{
    		long t_diff3 = hit3.get_time() - hit1.get_time();
		
		final DOMRegistry domRegistry = getTriggerHandler().getDOMRegistry();
		
		double p_diff1 = domRegistry.distanceBetweenDOMs(hit1.get_mb_id(), hit2.get_mb_id());
		double p_diff2 = domRegistry.distanceBetweenDOMs(hit2.get_mb_id(), hit3.get_mb_id());
    		double p_diff3 = domRegistry.distanceBetweenDOMs(hit1.get_mb_id(), hit3.get_mb_id());
		
    		if(p_diff1+p_diff2-p_diff3 <= delta_d && p_diff1 > 0 && p_diff2 > 0 && p_diff3 > 0)
    		{
    		    double inv_v1 = t_diff1/p_diff1;
    		    double inv_v2 = t_diff2/p_diff2;
    		    double inv_v3 = t_diff3/p_diff3;
    		    
    		    double inv_v_mean = (inv_v1+inv_v2+inv_v3)/3.0;
    		    
    		    //System.out.print(Math.abs(inv_v2-inv_v1)/inv_v_mean);
    		    
    		    if(Math.abs(inv_v2-inv_v1)/inv_v_mean <= rel_v)
    		    {
    		    	// Found Triple
    		    	System.out.format("Found a Triple!%n");
    		    	
    		    	long triple_start = hit1.get_time();
    		    	long triple_end = hit3.get_time();
    		    	
    		    	if(trigger_list.size() == 0)
    		    	{
    		    	    min_trigger_info trigger_info = new min_trigger_info(hit1.get_hit(), hit3.get_hit());
    		    	    
    		    	    trigger_list.add(trigger_info);
    		    	    
    		    	 //   System.out.println("** Adding first trigger to list..");
    	
    		    	}
    		    	else
    		    	{
    		    	    long trigger_start_temp = trigger_list.getLast().get_first_hit().getHitTimeUTC().longValue();
    		    	    long trigger_end_temp = trigger_list.getLast().get_last_hit().getHitTimeUTC().longValue();
    		    	  
    		        //	System.out.format("TEMP: %d %d%n", trigger_start_temp, trigger_end_temp);
    		    	  
    		    	    if(triple_start >= trigger_start_temp && triple_start <= trigger_end_temp && triple_end > trigger_end_temp)
    		    	    {
    		    		    trigger_list.getLast().set_last_hit(hit3.get_hit());
    		    		    trigger_list.getLast().increase_tuples();
    		    	    }
    		    	    else if(triple_start >= trigger_start_temp && triple_end <= trigger_end_temp) // contained tuple
    		    	    {
    		    	    	trigger_list.getLast().increase_tuples();
    		    	    }
    		    	    else if(triple_start > trigger_end_temp)
    		    	    {
    		    	    	min_trigger_info trigger_info = new min_trigger_info(hit1.get_hit(), hit3.get_hit());
      		    	    
      		    	        trigger_list.add(trigger_info);
      		    	      
      		    	     //   System.out.format("SECOND TRIGGER!!%n");
    		    	    }
    		    	}
    		    }
    		}
    	}
    }
/*
    
    Function to compare 2 HLC hits, if they should form a HLC pair.
    Since the timecheck was already made earlier, here only position check( same string, channel difference <= 2) is
    applied
    
    */
    private min_hit_info HLCPairCheck(min_hit_info hit1, min_hit_info hit2) 
    {   
        final DOMRegistry domRegistry = getTriggerHandler().getDOMRegistry();
        
        int string_nr1 = domRegistry.getDom(hit1.get_mb_id()).getStringMajor();
	int string_nr2 = domRegistry.getDom(hit2.get_mb_id()).getStringMajor();    
	
	if(string_nr1 == string_nr2)
	{
            int om_nr1 = domRegistry.getDom(hit1.get_mb_id()).getStringMinor();
	    int om_nr2 = domRegistry.getDom(hit2.get_mb_id()).getStringMinor();
	     
	    if( Math.abs(om_nr1 - om_nr2) <= 2)
	    {
	        //System.out.format("FOUND PAIR!!! | OM %d - %d / STRING %d%n", om_nr1, om_nr2, string_nr1);
	        return hit1;
	
	    }
	}
        return null;
    }

}

