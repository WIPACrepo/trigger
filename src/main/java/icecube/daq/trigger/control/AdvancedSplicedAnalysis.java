/*
 * interface: AdvancedSplicedAnalysis
 *
 * Version $Id: AdvancedSplicedAnalysis.java 2125 2007-10-12 18:27:05Z ksb $
 *
 * Date: October 17 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.splicer.SplicerListener;
import icecube.daq.splicer.SplicedAnalysis;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SpliceableFactory;

/**
 * This interface defines a SplicedAnalysis tailored to the modern Payload system.
 *
 * @version $Id: AdvancedSplicedAnalysis.java 2125 2007-10-12 18:27:05Z ksb $
 * @author pat
 */
public interface AdvancedSplicedAnalysis extends SplicedAnalysis, SplicerListener, IPayloadProducer
{

    /**
     * Set Splicer associated with this analysis.
     * @param splicer parent Splicer
     */
    void setSplicer(Splicer splicer);

    /**
     * Set factory to be used by Splicer.
     * @param spliceableFactory a Spliceable factory
     */
    void setFactory(SpliceableFactory spliceableFactory);

}
