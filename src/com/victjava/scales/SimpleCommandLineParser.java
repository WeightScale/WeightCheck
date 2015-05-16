package com.victjava.scales;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A simple class that provides utilities to ease command line parsing.
 */
public class SimpleCommandLineParser {

    private final Map<String, String> argMap;

    public SimpleCommandLineParser(String[] arg, String predict) {
        argMap = new HashMap<>();
        for (String anArg : arg) {
            String[] str = anArg.split(predict, 2);
            if (str.length > 1) {
                argMap.put(str[0], str[1]);
            }
        }
    }

    public String getValue(String... keys) {
        for (String key : keys) {
            if (argMap.get(key) != null) {
                return argMap.get(key);
            }
        }
        return null;
    }

    public Iterator<String> getKeyIterator() {
        Set<String> keySet = argMap.keySet();
        if (!keySet.isEmpty()) {
            return keySet.iterator();
        }
        return null;
    }
}
