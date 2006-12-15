package icecube.daq.trigger.config;

//import icecube.daq.trigger.config.db.DomSetNameLocal;
//import icecube.daq.trigger.config.db.DomIdLocal;
//import icecube.daq.trigger.config.db.DomSetLocal;

//import javax.naming.InitialContext;
//import javax.naming.Context;
//import javax.naming.NamingException;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: pat
 * Date: Sep 13, 2006
 * Time: 11:42:00 AM
 *
 *
 * Factory object with static method for constructing a DomSet based on
 * an id. Retrieves list of doms from database based on domSetId and contructs
 * a DomSet.
 *
 */
public class DomSetFactory
{

    /**
     * Logging object
     */
    private static final Log log = LogFactory.getLog(DomSetFactory.class);

    /**
     * JNDI context
     */
    //private static InitialContext jndiContext;

    /**
     * Get a DomSet based on this domSetId
     * @param domSetId id of DomSet stored in db
     * @return returns a DomSet, or null if domSetId does not exist
     */
    public static DomSet getDomSet(int domSetId) {

        // if we hava no JNDI, return null
        //setupJNDI();
        //if (null == jndiContext) return null;

        // Look up domSetName from the DomSetNameBean
        //Integer id = new Integer(domSetId);
        //DomSetNameLocal domSetNameLocal = DomSetNameLocal.findByDomSetId(jndiContext, id);
        //String domSetName = domSetNameLocal.getDomSetName();

        // Look up domPk's from DomSetBean
        //DomSetLocal[] domSet = DomSetLocal.findByDomSetId(jndiContext, id);
        //List domIds = new ArrayList();
        //for (int i=0; i<domSet.length; i++) {
        //    Integer domPk = domSet[i].getDomPk();
        //    DomIdLocal domIdLocal = DomIdLocal.findByDomPk(jndiContext, domPk);
        //    domIds.add(domIdLocal.getDomId());
        //}

        // Create and return a shiny new domSet
        //return new DomSet(domSetName, domIds);
	return null;
    }

    /*
    private static void setupJNDI() {

        Properties prop = new Properties();
        String jndiHost = "";
        String jndiPort = "";

        prop.put(Context.INITIAL_CONTEXT_FACTORY,
                 "org.jnp.interfaces.NamingContextFactory");
        prop.put(Context.PROVIDER_URL,
                 "jnp://" + jndiHost + ":" + jndiPort);
        prop.put(Context.URL_PKG_PREFIXES,
                 "org.jboss.naming:org.jnp.interfaces");

        try {
            jndiContext = new InitialContext(prop);
        } catch (NamingException e) {
            jndiContext = null;
            log.warn("No access to JNDI, cannot create DomSet for HitFilter");
        }

    }
    */

}
