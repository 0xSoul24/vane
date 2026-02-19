package org.oddlama.vane.core.config;

import java.util.Map;

public interface ConfigDictSerializable {
    public Map<String, Object> toDict();

    public void fromDict(Map<String, Object> dict);
}
