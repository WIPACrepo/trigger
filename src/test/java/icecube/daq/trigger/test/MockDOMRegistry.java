package icecube.daq.trigger.test;

import icecube.daq.util.IDOMRegistry;
import icecube.daq.util.DeployedDOM;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MockDOMRegistry
    implements IDOMRegistry
{
    private HashMap<Long, DeployedDOM> doms =
        new HashMap<Long, DeployedDOM>();
    private HashMap<Integer, HashSet<DeployedDOM>> hubDOMs =
        new HashMap<Integer, HashSet<DeployedDOM>>();

    public void addDom(long mbId, int string, int position)
    {
        addDom(mbId, string, position, string);
    }

    public void addDom(long mbId, int string, int position, int hub)
    {
        DeployedDOM dom = new DeployedDOM(mbId, string, position, hub);
        doms.put(mbId, dom);
        if (!hubDOMs.containsKey(hub)) {
            hubDOMs.put(Integer.valueOf(hub), new HashSet<DeployedDOM>());
        }
        hubDOMs.get(hub).add(dom);
    }

    public double distanceBetweenDOMs(DeployedDOM dom0, DeployedDOM dom1)
    {
        throw new Error("Unimplemented");
    }

    public double distanceBetweenDOMs(long mbid0, long mbid1)
    {
        throw new Error("Unimplemented");
    }

    public short getChannelId(long mbId)
    {
        throw new Error("Unimplemented");
    }

    public DeployedDOM getDom(long mbId)
    {
        return doms.get(mbId);
    }

    public DeployedDOM getDom(short chanid)
    {
        throw new Error("Unimplemented");
    }

    public Set<DeployedDOM> getDomsOnHub(int hubId)
    {
        if (!hubDOMs.containsKey(hubId)) {
            return new HashSet<DeployedDOM>();
        }

        return hubDOMs.get(hubId);
    }

    public Set<DeployedDOM> getDomsOnString(int string)
    {
        throw new Error("Unimplemented");
    }

    public String getProductionId(long mbid)
    {
        throw new Error("Unimplemented");
    }

    public String getName(long mbid)
    {
        throw new Error("Unimplemented");
    }

    public int getStringMajor(long mbid)
    {
        throw new Error("Unimplemented");
    }

    public int getStringMinor(long mbid)
    {
        throw new Error("Unimplemented");
    }

    public Set<Long> keys()
    {
        return doms.keySet();
    }

    public int size()
    {
        throw new Error("Unimplemented");
    }
}
