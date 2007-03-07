/*
 * interface: ITriggerInput
 *
 * Version $Id: ITriggerInput.java,v 1.1 2005/08/30 01:14:35 toale Exp $
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
 * @version $Id: ITriggerInput.java,v 1.1 2005/08/30 01:14:35 toale Exp $
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
