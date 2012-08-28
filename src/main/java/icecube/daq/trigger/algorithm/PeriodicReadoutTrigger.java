package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.payload.IUTCTime;

//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.ListIterator;
//import java.util.Arrays;
//import java.lang.Math;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This trigger reads out the detector every fixed time interval for a certain time. This happens independently of the hits,
 * which are just used to sample the time so the trigger knows where it is in time.
 * @author gluesenkamp
 *
 */
public class PeriodicReadoutTrigger extends AbstractTrigger
{

    private static final Log log = LogFactory.getLog(PeriodicReadoutTrigger.class);

    // internal variables of trigger start/stop

    private IUTCTime current_trigger_start;
    private IUTCTime current_trigger_end;

    private boolean trigger_window_set;


    // important steerable quantities

    private long prescale_delta_t;
    private long readout_length;

    private boolean delta_t_configured = false;
    private boolean readout_length_configured = false;

    public PeriodicReadoutTrigger()
    {
	trigger_window_set = false;
    }

    public boolean isConfigured()
    {
        return ( delta_t_configured && readout_length_configured );
    }

    @Override
    public void addParameter(TriggerParameter parameter) throws UnknownParameterException,
               IllegalParameterValueException
               {
                   if (parameter.getName().equals("prescale_delta_t"))
		   {
                       if(Long.parseLong(parameter.getValue())>=0)
		       {
                           prescale_delta_t= Long.parseLong(parameter.getValue())*1000000000L; // parameter is configured in seconds....
                           delta_t_configured = true;
			   //log.warn("configured prescale_delta_t");
                       }
                       else
		       {
                          throw new IllegalParameterValueException("Illegal t_proximity value: " + Long.parseLong(parameter.getValue()));
                       }
		   }
                   else if (parameter.getName().equals("readout_length"))
		   {
                       if(Long.parseLong(parameter.getValue())>=0)
		       {
                           readout_length = Long.parseLong(parameter.getValue()); // readout_length is configued in nanoseconds....
                           readout_length_configured = true;
			   //log.warn("configured readout_length");
                       }
                       else
		       {
                          throw new IllegalParameterValueException("Illegal t_min value: " + Long.parseLong(parameter.getValue()));
                       }
		   }

		   else
		   {
		      throw new UnknownParameterException("Unknown parameter: " + parameter.getName());

		   }
                   super.addParameter(parameter);
               }

    @Override
    public void flush()
    {

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

        // Check hit type
        if (getHitType(hitPayload) != AbstractTrigger.SPE_HIT) return;

	IUTCTime sample_hit_time=hitPayload.getHitTimeUTC();

	//trigger_window_set is only false after initialization
	if(trigger_window_set==false){
	    current_trigger_start=sample_hit_time.getOffsetUTCTime(5000000); // go 5 ms after the hit and start the trigger algorithm from here
	    //log.warn("trigger started: sample_hit_time is " + sample_hit_time + " newtriggerstart is " + current_trigger_start);
	    current_trigger_end=current_trigger_start.getOffsetUTCTime(readout_length);
	    trigger_window_set=true;
	    //    log.warn("triggerwindow is false.. initialize the trigger_start and end" + current_trigger_start + " " + current_trigger_end);
	    //    log.warn("prescaledeltat is " + prescale_delta_t + "readout_window is " + readout_length);
	}
	else{
	    if(sample_hit_time.timeDiff_ns(current_trigger_start) < 0){
		// we are before the actual trigger readout - so we can safely set earliest payload to zero and do nothing
		setEarliestPayloadOfInterest(hitPayload);
	    }
	    else{

		// time difference is bigger .. check if we are in the trigger window.. then do nothing... otherwhise we are the first hit after the
		// window - so we need to form the trigger and reset the current_trigger_start/end to the next readout
		if(sample_hit_time.timeDiff_ns(current_trigger_end) > 0){
		    formTrigger(current_trigger_start, current_trigger_end);
		    setEarliestPayloadOfInterest(hitPayload);
		    current_trigger_start=current_trigger_start.getOffsetUTCTime(prescale_delta_t);
		    current_trigger_end=current_trigger_start.getOffsetUTCTime(readout_length);
		    //log.warn("forming a trigger.. new trigger start and end: " + current_trigger_start + " " + current_trigger_end);
		}
	    }
	}
    }
}
