package icecube.daq.trigger.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: pat
 * Date: Dec 8, 2006
 * Time: 1:10:06 PM
 *
 * This file is responsible for taking the name of a trigger configuration
 * and returning the path of the configuration xml file.
 *
 */
public class ConfigurationFinder
{

    /**
     * Logging object.
     */
    private static final Log log = LogFactory.getLog(ConfigurationFinder.class);

    /**
     * Directory in which to find the configuration file.
     */
    private static final String FILE_DIRECTORY = "/usr/icecube";

    /**
     * Common prefix of trigger configuration files.
     */
    private static final String FILE_PREFIX = "/sps-triggers-";

    /**
     * Common postix of trigger configuration files.
     */
    private static final String FILE_POSTFIX = ".xml";

    /**
     * Static method for translating a configuration name into a path name.
     * @param configurationName Name of trigger configuration, like "Pallas"
     * @return Path name, like "/usr/icecube/configs/sps-triggers-Pallas.xml"
     */
    public static String getConfigurationFileName(String configurationName) {
        String fileName = FILE_DIRECTORY + FILE_PREFIX + configurationName + FILE_POSTFIX;
        if (log.isDebugEnabled()) {
            log.debug("Configuration file for configuration " + configurationName
                      + " is " + fileName);
        }
        return fileName;
    }

}
