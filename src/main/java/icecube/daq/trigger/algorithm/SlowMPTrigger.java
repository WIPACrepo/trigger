package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.util.DOMRegistry;
import icecube.daq.util.DeployedDOM;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This trigger looks for HLC pairs and groups them in 3-tuples. If a tuple fullfills certain conditions ( in
 *CheckTriple() ) the internal trigger_info structure will raise n_tuples by 1. At the end one can control if a certain
 *amount of overlapping tuples is sufficient for triggering by the min_n_tuples parameter.
 * For the testrun the parameters which will yield 10-30 Hz trigger rate are pre-set already.
 * @author gluesenkamp
 *
 */
public class SlowMPTrigger extends AbstractTrigger
{

    private static final Log log = LogFactory.getLog(SlowMPTrigger.class);
    private long t_proximity; // t_proximity in nanoseconds, eliminates most muon_hlcs
    private long t_min;
    private long t_max;
    private boolean dc_algo;
    private int delta_d;
    private double alpha_min;
    private double rel_v;
    private int min_n_tuples;
    private long max_event_length;

    private LinkedList<min_hit_info> one_hit_list;
    private LinkedList<min_hit_info> two_hit_list;
    private LinkedList<min_trigger_info> trigger_list;

    private long muon_time_window;
    private double cos_alpha_min;

    private boolean t_proximity_configured = false;
    private boolean t_min_configured = false;
    private boolean t_max_configured = false;
    private boolean dc_algo_configured = false;
    private boolean delta_d_configured = false;
    private boolean rel_v_configured = false;
    private boolean alpha_min_configured = false;
    private boolean min_n_tuples_configured = false;
    private boolean max_event_length_configured = false;

    private DOMRegistry domRegistry;

    private class min_hit_info
    {
        min_hit_info(IHitPayload new_hit)
        {
            hit  = new_hit;

            utc_time = hit.getHitTimeUTC().longValue();
            mb_id = hit.getDOMID().toString();
        }

        private IHitPayload hit;

        private String mb_id;
        private long utc_time;

        private DeployedDOM dom;

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

        public DeployedDOM get_dom()
        {
            if (dom == null) {
                if (domRegistry == null) {
                    domRegistry = getTriggerHandler().getDOMRegistry();
                }

                dom = domRegistry.getDom(mb_id);
            }

            return dom;
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
       // set_t_proximity(2500);
       // set_t_min(0);
       // set_t_max(500000);
       // set_delta_d(500);
       // set_rel_v(0.5);
       // set_min_n_tuples(2);

        // max_event_length is in tens of nanoseconds
        //max_event_length = 50000000;  // we dont want longer events thatn 5 milliseconds, should not occur in 30 min run

        muon_time_window = -1;
       // configHitFilter(5);

        //System.out.println("INITIALIZED SLOWMPTRIGGER");
    }

    public boolean isConfigured()
    {
    	if (dc_algo_configured)
    	{
    		if (dc_algo)
    		{
    			return ( t_proximity_configured && t_min_configured && t_max_configured &&
    					delta_d_configured && rel_v_configured && min_n_tuples_configured && max_event_length_configured );
    		}
    		else
    		{
    			return ( t_proximity_configured && t_min_configured && t_max_configured &&
    	                alpha_min_configured && rel_v_configured && min_n_tuples_configured && max_event_length_configured );
    		}
    	}
    	return false;

    }

    @Override
    public void addParameter(TriggerParameter parameter) throws UnknownParameterException,
               IllegalParameterValueException
               {
                   if (parameter.getName().equals("t_proximity"))
		   {
                       if(Long.parseLong(parameter.getValue())>=0)
		       {
                           set_t_proximity(Long.parseLong(parameter.getValue()));
                           t_proximity_configured = true;
                       }
                       else
		       {
                          throw new IllegalParameterValueException("Illegal t_proximity value: " + Long.parseLong(parameter.getValue()));
                       }
		   }
                   else if (parameter.getName().equals("t_min"))
		   {
                       if(Long.parseLong(parameter.getValue())>=0)
		       {
                           set_t_min(Long.parseLong(parameter.getValue()));
                           t_min_configured = true;
                       }
                       else
		       {
                          throw new IllegalParameterValueException("Illegal t_min value: " + Long.parseLong(parameter.getValue()));
                       }
		   }
                   else if (parameter.getName().equals("t_max"))
		   {
                       if(Long.parseLong(parameter.getValue())>=0)
		       {
                           set_t_max(Long.parseLong(parameter.getValue()));
                           t_max_configured = true;
                       }
		       else
                       {
                          throw new IllegalParameterValueException("Illegal t_max value: " + Long.parseLong(parameter.getValue()));
                       }
		   }

                   else if (parameter.getName().equals("dc_algo"))
		   {
                           set_dc_algo(Boolean.parseBoolean(parameter.getValue()));
                           dc_algo_configured = true;
		   }
                   else if (parameter.getName().equals("delta_d"))
		   {
                       if(Integer.parseInt(parameter.getValue())>=0)
		       {
                           set_delta_d(Integer.parseInt(parameter.getValue()));
                           delta_d_configured = true;
                       }
                       else
		       {
                          throw new IllegalParameterValueException("Illegal delta_d value: " + Integer.parseInt(parameter.getValue()));
                       }
		   }
                   else if (parameter.getName().equals("alpha_min"))
		   {
                       if( (Double.parseDouble(parameter.getValue())>=10) && (Double.parseDouble(parameter.getValue())<=180) )  // forbid values below 10 to make sure alpha_min is configured in degree
		       {
                           set_alpha_min(Double.parseDouble(parameter.getValue()));
			   cos_alpha_min = Math.cos((Math.PI/180)*alpha_min); // cos_alpha_min is the cos of alpha_min not the min of cos_alpha
                           alpha_min_configured = true;
                       }
                       else
		       {
                          throw new IllegalParameterValueException("Illegal alpha_min value: " + Double.parseDouble(parameter.getValue()));
                       }
		   }
                   else if (parameter.getName().equals("rel_v"))
		   {
                       if(Double.parseDouble(parameter.getValue())>=0)
		       {
                           set_rel_v(Double.parseDouble(parameter.getValue()));
                           rel_v_configured = true;
                       }
                       else
		       {
                          throw new IllegalParameterValueException("Illegal rel_v value: " + Double.parseDouble(parameter.getValue()));
                       }
		   }
                   else if (parameter.getName().equals("min_n_tuples"))
		   {
                       if(Integer.parseInt(parameter.getValue())>=0)
		       {
                           set_min_n_tuples(Integer.parseInt(parameter.getValue()));
                           min_n_tuples_configured = true;
                       }
                       else
		       {
                          throw new IllegalParameterValueException("Illegal min_n_tuples value: " + Integer.parseInt(parameter.getValue()));
                       }
		   }
                   else if (parameter.getName().equals("max_event_length"))
		   {
                       if(Long.parseLong(parameter.getValue())>=0)
		       {
                           set_max_event_length(Long.parseLong(parameter.getValue()));
                           max_event_length_configured = true;
                       }
                       else
		       {
                          throw new IllegalParameterValueException("Illegal max_event_length value: " + Long.parseLong(parameter.getValue()));
                       }
		   }
                   else if (parameter.getName().equals("domSet"))
		   {
                       int domSetId = Integer.parseInt(parameter.getValue());
                       if(domSetId>=0)
		       {
                           try {
                               configHitFilter(domSetId);
                           } catch (ConfigException ce) {
                               throw new IllegalParameterValueException("Bad DomSet #" +
                                                                        domSetId, ce);
                           }
                       }
		   }
		   else
		   {
		      throw new UnknownParameterException("Unknown parameter: " + parameter.getName());

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
        t_proximity = val * 10L;
    }

    // t_min

    public long get_t_min()
    {
        return t_min / 10L;
    }

    public void set_t_min(long val)
    {
        t_min = val * 10L;
    }

    // t_max

    public long get_t_max()
    {
        return t_max / 10L;
    }

    public void set_t_max(long val)
    {
        t_max = val * 10L;
    }

    // dc_algo

    public boolean get_dc_algo()
    {
        return dc_algo;
    }

    public void set_dc_algo(boolean val)
    {
        dc_algo = val;
    }

    // delta_d

    public int get_delta_d()
    {
        return delta_d;
    }

    public void set_delta_d(int val)
    {
        delta_d = val;
    }

    // alpha_min

    public double get_alpha_min()
    {
        return alpha_min;
    }

    public void set_alpha_min(double val)
    {
        alpha_min = val;
    }

    // rel_v

    public double get_rel_v()
    {
        return rel_v;
    }

    public void set_rel_v(double val)
    {
        rel_v = val;
    }

    // min_n_tuples

    public int get_min_n_tuples()
    {
        return min_n_tuples;
    }

    public void set_min_n_tuples(int val)
    {
        min_n_tuples = val;
    }

    // max_event_length

    public void set_max_event_length(long val)
    {
        max_event_length = val*10L;
    }

    public long get_max_event_length()
    {
        return max_event_length/10L;
    }

    @Override
    public void flush()
    {
        one_hit_list.clear();
        two_hit_list.clear();

        log.info("FLUSHHH!!!");

        muon_time_window = -1;
    }

    public Map<String, Object> getTriggerMonitorMap() {
        HashMap<String, Object> map = new HashMap<String, Object>();

        map.put("t_max", t_max);
        map.put("one_hit_list", one_hit_list.size());
        map.put("two_hit_list", two_hit_list.size());
        map.put("trigger_list", trigger_list.size());

        return map;
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
        //log.warn("HITTIME: " + hitPayload.getHitTimeUTC() + "PAYLOADTIME: " + hitPayload.getPayloadTimeUTC());
        // Check hit type and perhaps pre-screen DOMs based on channel (HitFilter)
        if (getHitType(hitPayload) != AbstractTrigger.SPE_HIT) return;
        if (!hitFilter.useHit(hitPayload)) return;

        //if (domRegistry == null) {
        //    domRegistry = getTriggerHandler().getDOMRegistry();
        //}

        if(one_hit_list.size() == 0) // size is 0, so just add it to the list
        {
            min_hit_info new_hit = new min_hit_info(hitPayload);
            //int string_nr = new_hit.get_dom().getStringMajor();
            //int om_nr = new_hit.get_dom().getStringMinor();
            // log.warn("FIRST HIT IN LIST: time " +new_hit.get_time() + " string: " + string_nr + " om: " + om_nr);
            one_hit_list.add(new_hit);
        }
        else // not zero, so compare payload with current one_hit_list if any hlc pair can be formed
        {

            min_hit_info new_hit = new min_hit_info(hitPayload);

            // remove this next line again


            //int string_nr = new_hit.get_dom().getStringMajor();
            //int om_nr = new_hit.get_dom().getStringMinor();
            //log.warn("LATER HIT IN LIST: time " +new_hit.get_time() + " string: " + string_nr + " om: " + om_nr + "diff: " + (new_hit.get_time()-one_hit_list.getFirst().get_time()));
            //System.out.format("list contains %d entries..%n TWOHIT_list contains %d entries..%n", one_hit_list.size(), two_hit_list.size() );
            while( new_hit.get_time() - one_hit_list.element().get_time() > 10000L)
                // makes no sense to compare HLC hits that are longer apart than 1000 nanoseconds, so remove first from list
            {
                //  log.warn("remove first hit from onehitlist because time diff > 10000, actually:" +( new_hit.get_time()-one_hit_list.element().get_time()));
                //System.out.format("REMOVED FIRST ELEMENT !");
                one_hit_list.removeFirst();

                if(one_hit_list.size() == 0)
                {
                    break;
                }
            }

            //log.warn("ONEHITLIST: " + one_hit_list.size() + " newwest " +  new_hit.get_time());

            //ListIterator iter = one_hit_list.listIterator(); added the following
            // lines, make use of toArray instead of iterator for some speed
            ///one_hit_list.add(new_hit);
            min_hit_info[] one_hit_array = one_hit_list.toArray(new min_hit_info[0]);
            int initial_size = one_hit_list.size();
            int no_of_removed_elems = 0;

            //while(iter.hasNext())
            for(int i = 0; i < initial_size; i++)
            {
                //System.out.format("muon_time_window: %d", muon_time_window);
                min_hit_info check_payload = HLCPairCheck(one_hit_array[i], new_hit);
                //check_payload = one_hit_array[i];

                if(check_payload != null)
                {
                    if(two_hit_list.size() == 0)        // the pair list is empty
                    {
                        if(muon_time_window == -1)
                        {
                            two_hit_list.add(check_payload);
                            // set earliest payload of interest ?=!
                            //log.info("Adding two_hit entry -> muon == -1");
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
                                //log.info("Adding two_hit entry -> muon time window was set.. is now unset..");
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
                                //log.info("removing last hit of two_hit_list..., while muon == -1");
                                two_hit_list.removeLast();
                            }
                            else
                            {
                                if((check_payload.get_time()-two_hit_list.getLast().get_time() < t_max)
                                   && (check_payload.get_time() - two_hit_list.getFirst().get_time() <
                                       max_event_length))
                                {
                                    two_hit_list.add(check_payload);
                                    //log.info("adding twohitlist... below time differences..");
                                }
                                else
                                {
                                    CheckTriggerStatus(); // checks current two_hit_list for 3-tuples
                                    //log.info("Adding twohitlist... after chewcking trigger status");
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
                                if((check_payload.get_time()- two_hit_list.getLast().get_time() < t_max)
                                   && (check_payload.get_time() - two_hit_list.getFirst().get_time() <
                                       max_event_length))
                                {
                                    two_hit_list.add(check_payload); // checks current two_hit_list for 3-tupleseckTriggerStatus(); // checks current two_hit_list for 3-tuples
                                    //log.info("Adding twohitlist... below time differences............");
                                }
                                else
                                {
                                    CheckTriggerStatus();
                                    //log.info("ADding twohitlist... after checking trigger status");
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
                    //log.info("NULL:::::");
                    //System.out.format("NULLLLLLL");
                }
                // with the remaining Payloads in the linked list
            }

            one_hit_list.add(new_hit); // at the end add the current hitPayload for further comparisons
            if(two_hit_list.size() > 0)
            {
                if(one_hit_list.getFirst().get_time() - two_hit_list.getLast().get_time() > t_max) // definetely cannot prdouce a trigger, set earliest palyoad
                {
                    //log.warn("TMAX WAS ALREADY REACHED::------------" + "t1 " + one_hit_list.getFirst().get_time() + " t2 " + two_hit_list.getLast().get_time());
                    CheckTriggerStatus();


                }
            }
            else
            {
                setEarliestPayloadOfInterest(one_hit_list.getFirst().get_hit());
            }

        }
        //    System.out.format("muon_time_window: %d", muon_time_window);
        //   log.warn("list contains " + one_hit_list.size() +  " entries..");
        //   log.warn(" TWOHIT_list contains " + two_hit_list.size() + "entries..");


    }

    private void CheckTriggerStatus()
    {
        int list_size = two_hit_list.size();
        //log.info("CHECKING TRIGGER STATUS because of timing: 2hitsize " + list_size);
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
        else
        {
            //two_hit_list.clear();
            setEarliestPayloadOfInterest(one_hit_list.getFirst().get_hit());
        }

        //ListIterator list_iterator = trigger_list.listIterator();
        min_trigger_info[] trigger_array = trigger_list.toArray(new min_trigger_info[0]);

        //while(list_iterator.hasNext())
        for(int i = 0; i < trigger_list.size(); i++)
        {
            //min_trigger_info info = (min_trigger_info)list_iterator.next();
            min_trigger_info info = trigger_array[i];

            // Form trigger with each trigger_info object, if num_tuples is fullfilled

            if(info.get_num_tuples() >= min_n_tuples)
            {
                //System.out.format("FOUND TRIGGER: start: %d, end :%d with %d tuples%n",info.get_first_hit().get_time(), info.get_last_hit().get_time(), info.get_num_tuples() );
                //log.warn("FOUND TRIGGER: length: " + (info.get_last_hit().getHitTimeUTC().longValue()-info.get_first_hit().getHitTimeUTC().longValue()) + " with " + info.get_num_tuples());
                // form trigger here for each trigger_info
                formTrigger(info.get_hit_list(), null, null);

            }
        }

        trigger_list.clear();
        //one_hit_list.clear();
        two_hit_list.clear();
    }

    /*

      Function to check for a triple combination fullfilling the parameter boundaries

    */


    private void CheckTriple(min_hit_info hit1, min_hit_info hit2, min_hit_info hit3)
    {
        long t_diff1 = hit2.get_time() - hit1.get_time();
        long t_diff2 = hit3.get_time() - hit2.get_time();
        //log.warn("CHECKING TRIPLE t_diff1 " + t_diff1 + " / t_diff2 " + t_diff2);
        if((t_diff1 > t_min) && (t_diff2 > t_min) && (t_diff1 < t_max) && (t_diff2 < t_max))
        {
            long t_diff3 = hit3.get_time() - hit1.get_time();

            if (domRegistry == null) {
                domRegistry = getTriggerHandler().getDOMRegistry();
            }

            double p_diff1 = domRegistry.distanceBetweenDOMs(hit1.get_mb_id(), hit2.get_mb_id());
            double p_diff2 = domRegistry.distanceBetweenDOMs(hit2.get_mb_id(), hit3.get_mb_id());
            double p_diff3 = domRegistry.distanceBetweenDOMs(hit1.get_mb_id(), hit3.get_mb_id());
            double cos_alpha = 1.0;
            //log.warn("    ->step2 - p_diff1: " + p_diff1 + " p_diff2: " + p_diff2 + " pdiff3: " + p_diff3);

            if ( !( (p_diff1 > 0) && (p_diff2 > 0) && (p_diff3 > 0) ))
            {
               //log.warn("exiting check triple because p_diff1: " +  p_diff1 + " p_diff2: " + p_diff2 +  " p_diff3:" + p_diff3);
               return;
            }

            if (!dc_algo)
	    {
                cos_alpha =  ( Math.pow(p_diff1,2) + Math.pow(p_diff2,2) - Math.pow(p_diff3,2)) / ( 2*p_diff1*p_diff2 );
                //double alpha = (180/Math.PI)*Math.acos(cos_alpha);
                //log.warn("    ->step2 - p_diff1: " + p_diff1 + " p_diff2: " + p_diff2 + " pdiff3: " + p_diff3 );
                //log.warn("cos_alpha: " + cos_alpha + " alpha: " + alpha + " cos_alpha_min: " + cos_alpha_min + " alpha_min: " + alpha_min );
            }
            //else
            //{
            //    log.warn("    ->step2 - p_diff1: " + p_diff1 + " p_diff2: " + p_diff2 + " pdiff3: " + p_diff3 + " delta_d: " + delta_d );
            //}
            if(   ( (dc_algo) && (p_diff1 + p_diff2 - p_diff3 <= delta_d) )   ||   ( (!dc_algo) && (cos_alpha <= cos_alpha_min) )   )
            {
                double inv_v1 = t_diff1/p_diff1;
                double inv_v2 = t_diff2/p_diff2;
                double inv_v3 = t_diff3/p_diff3;

                double inv_v_mean = (inv_v1+inv_v2+inv_v3)/3.0;

                //System.out.print(Math.abs(inv_v2-inv_v1)/inv_v_mean);
                //  log.warn("        ->step3 - inv_v_mean " + Math.abs(inv_v2-inv_v1)/inv_v_mean);
                if(Math.abs(inv_v2-inv_v1)/inv_v_mean <= rel_v)
                {
                    // Found Triple
                    //System.out.format("Found a Triple!%n");

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

                        //System.out.format("TEMP: %d %d%n", trigger_start_temp, trigger_end_temp);

                        if((triple_start >= trigger_start_temp) && (triple_start <= trigger_end_temp) && (triple_end
                                                                                                          > trigger_end_temp))
                        {
                            trigger_list.getLast().set_last_hit(hit3.get_hit());
                            trigger_list.getLast().increase_tuples();
                        }
                        else if((triple_start >= trigger_start_temp) && (triple_end <= trigger_end_temp)) // contained tuple
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
        if (domRegistry == null) {
            domRegistry = getTriggerHandler().getDOMRegistry();
        }

        DeployedDOM dom1 = hit1.get_dom();
        if (dom1 == null) {
            throw new Error("Cannot find " + hit1.get_mb_id());
        }
        DeployedDOM dom2 = hit2.get_dom();
        if (dom2 == null) {
            throw new Error("Cannot find " + hit2.get_mb_id());
        }

        int string_nr1 = dom1.getStringMajor();
        int string_nr2 = dom2.getStringMajor();

        if(string_nr1 == string_nr2)
        {
            int om_nr1 = dom1.getStringMinor();
            int om_nr2 = dom2.getStringMinor();

            if( Math.abs(om_nr1 - om_nr2) <= 2)
            {
                //System.out.format("FOUND PAIR!!! | OM %d - %d / STRING %d%n", om_nr1, om_nr2, string_nr1);
                //log.info("FOUND PAIR   -> oms: " + om_nr1 + " " + om_nr2 + " strings: " + string_nr1 + " " + string_nr2 + " timediff: " + (hit2.get_time()-hit1.get_time()) + " time1: " + hit1.get_time() + " time2: " + hit2.get_time());
                return hit1;

            }
            //log.info("NULL-> oms: " + om_nr1 + " " + om_nr2 + " strings: " + string_nr1 + " " + string_nr2);
        }

        return null;
    }

}
