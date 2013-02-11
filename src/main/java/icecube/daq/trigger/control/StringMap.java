package icecube.daq.trigger.control;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * IceCube string map.
 */
public final class StringMap
{
    private static final Log LOG = LogFactory.getLog(StringMap.class);

    private static StringMap theStringMap;

    private HashMap<Integer, Coordinate> string2coordMap;
    private HashMap<Coordinate, Integer> coord2stringMap;
    private HashMap<Integer, Integer> verticalOffsetMap;

    /**
     * Create the system-wide string map.
     */
    private StringMap()
    {
        string2coordMap = new HashMap<Integer, Coordinate>();
        coord2stringMap = new HashMap<Coordinate, Integer>();
        verticalOffsetMap = new HashMap<Integer, Integer>();
        fillMaps();
    }

    /**
     * Get the single instance of the string map.
     *
     * @return string map
     */
    public static StringMap getInstance()
    {
        if (theStringMap == null) {
            theStringMap = new StringMap();
        }
        return theStringMap;
    }

    /**
     * Get neighboring strings.
     *
     *
     * @param string string number
     *
     * @return list of neighboring strings
     */
    public ArrayList<Integer> getNeighbors(Integer string)
    {
        ArrayList<Integer> neighbors = new ArrayList<Integer>();

        // Get the coordinate of this string
        Coordinate c = string2coordMap.get(string);
        if (c == null) {
            LOG.warn("Coordinate of string " + string + " is null");
            return neighbors;
        }
        int i = c.getI();
        int j = c.getJ();

        // Now generate the coordinates of the 6 neighbors
        Coordinate n1 = new Coordinate(i - 1, j);
        Coordinate n2 = new Coordinate(i,     j + 1);
        Coordinate n3 = new Coordinate(i + 1, j + 1);
        Coordinate n4 = new Coordinate(i + 1, j);
        Coordinate n5 = new Coordinate(i,     j - 1);
        Coordinate n6 = new Coordinate(i - 1, j - 1);

        // Get each string number and check if it exists
        Integer s1 = coord2stringMap.get(n1);
        if (s1 != null) {
            neighbors.add(s1);
        }
        Integer s2 = coord2stringMap.get(n2);
        if (s2 != null) {
            neighbors.add(s2);
        }
        Integer s3 = coord2stringMap.get(n3);
        if (s3 != null) {
            neighbors.add(s3);
        }
        Integer s4 = coord2stringMap.get(n4);
        if (s4 != null) {
            neighbors.add(s4);
        }
        Integer s5 = coord2stringMap.get(n5);
        if (s5 != null) {
            neighbors.add(s5);
        }
        Integer s6 = coord2stringMap.get(n6);
        if (s6 != null) {
            neighbors.add(s6);
        }

        return neighbors;
    }

    /**
     * Get the vertical offset.
     *
     * @param string string number
     *
     * @return vertical offset
     */
    public Integer getVerticalOffset(Integer string)
    {
        return verticalOffsetMap.get(string);
    }

    /**
     * Fill all maps.
     */
    private void fillMaps()
    {
        /*
         * First fill the transformation maps
         */
        // Fill first row
        for (int i = 0; i <= 5; i++) {
            Integer n = new Integer(i + 1);
            Coordinate c = new Coordinate(i, 0);
            string2coordMap.put(n, c);
            coord2stringMap.put(c, n);
        }
        // Fill second row
        for (int i = 0; i <= 6; i++) {
            Integer n = new Integer(i + 7);
            Coordinate c = new Coordinate(i, 1);
            string2coordMap.put(n, c);
            coord2stringMap.put(c, n);
        }
        // Fill third row
        for (int i = 0; i <= 7; i++) {
            Integer n = new Integer(i + 14);
            Coordinate c = new Coordinate(i, 2);
            string2coordMap.put(n, c);
            coord2stringMap.put(c, n);
        }
        // Fill fourth row
        for (int i = 0; i <= 8; i++) {
            Integer n = new Integer(i + 22);
            Coordinate c = new Coordinate(i, 3);
            string2coordMap.put(n, c);
            coord2stringMap.put(c, n);
        }
        // Fill fifth row
        for (int i = 0; i <= 9; i++) {
            Integer n = new Integer(i + 31);
            Coordinate c = new Coordinate(i, 4);
            string2coordMap.put(n, c);
            coord2stringMap.put(c, n);
        }
        // Fill sixth row
        for (int i = 1; i <= 10; i++) {
            Integer n = new Integer(i + 40);
            Coordinate c = new Coordinate(i, 5);
            string2coordMap.put(n, c);
            coord2stringMap.put(c, n);
        }
        // Fill seventh row
        for (int i = 2; i <= 10; i++) {
            Integer n = new Integer(i + 49);
            Coordinate c = new Coordinate(i, 6);
            string2coordMap.put(n, c);
            coord2stringMap.put(c, n);
        }
        // Fill eighth row
        for (int i = 3; i <= 10; i++) {
            Integer n = new Integer(i + 57);
            Coordinate c = new Coordinate(i, 7);
            string2coordMap.put(n, c);
            coord2stringMap.put(c, n);
        }
        // Fill nineth row
        for (int i = 4; i <= 10; i++) {
            Integer n = new Integer(i + 64);
            Coordinate c = new Coordinate(i, 8);
            string2coordMap.put(n, c);
            coord2stringMap.put(c, n);
        }
        // Fill tenth row
        for (int i = 5; i <= 10; i++) {
            Integer n = new Integer(i + 70);
            Coordinate c = new Coordinate(i, 9);
            string2coordMap.put(n, c);
            coord2stringMap.put(c, n);
        }

        /*
         * Now fill the vertical offset map
         */
        for (int n = 1; n <= 80; n++) {
            int offset = 0;
            if (n == 21) {
                offset = +1;
            } else if (n == 38) {
                offset = -1;
            }

            verticalOffsetMap.put(new Integer(n), new Integer(offset));
        }

    }

    /**
     * String coordinates.
     */
    private class Coordinate
    {
        private int i;
        private int j;

        public Coordinate()
        {
            this(0,0);
        }

        public Coordinate(int i, int j)
        {
            this.i = i;
            this.j = j;
        }

        public int getI()
        {
            return i;
        }

        public int getJ()
        {
            return j;
        }
    }
}
