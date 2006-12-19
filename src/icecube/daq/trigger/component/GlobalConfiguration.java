package icecube.daq.trigger.component;

/**
 * Created by IntelliJ IDEA.
 * User: pat
 * Date: Dec 18, 2006
 * Time: 6:24:31 PM
 */
public class GlobalConfiguration
{

    /**
     * Get the DOM configuration name
     * @param configFileName fully qualified global configuration file name
     * @return name of DOM configuration
     */
    public static String getDomConfigList(String configFileName) {
        String domConfigList = null;
        String triggerConfig = null;
        parse(configFileName, domConfigList, triggerConfig);
        return domConfigList;
    }

    /**
     * Get the trigger configuration name
     * @param configFileName fully qualified global configuration file name
     * @return name of trigger configuration
     */
    public static String getTriggerConfig(String configFileName) {
        String domConfigList = null;
        String triggerConfig = null;
        parse(configFileName, domConfigList, triggerConfig);
        return triggerConfig;
    }

    /**
     * Parse the global configuration file
     * @param configFileName fully qualified global configuration file name
     *
     * todo: implement an xml parser
     */
    private static void parse(String configFileName, String domConfigList, String triggerConfig) {
        domConfigList = null;
        triggerConfig = null;
    }

}
