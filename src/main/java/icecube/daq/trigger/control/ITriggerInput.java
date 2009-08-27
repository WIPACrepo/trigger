/*
 * interface: ITriggerInput
 *
 * Version $Id: ITriggerInput.java 2125 2007-10-12 18:27:05Z ksb $
 *
 * Date: May 2 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.payload.ILoadablePayload;

/**
 * This interface defines the functionality of a trigger input module
 *
 * @version $Id: ITriggerInput.java 2125 2007-10-12 18:27:05Z ksb $
 * @author pat
 */
public interface ITriggerInput
{

    void addPayload(ILoadablePayload payload);

    void flush();

    boolean hasNext();

    ILoadablePayload next();

    int size();

}
