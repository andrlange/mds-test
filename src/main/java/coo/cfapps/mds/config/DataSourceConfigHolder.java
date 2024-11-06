package coo.cfapps.mds.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataSourceConfigHolder {
    public record MetaData(String username,String password, String tenant, String driver, String url){}
    private final Map<String, MetaData> metaDataMap = new ConcurrentHashMap<>();

    public MetaData addMetaData(String username, String password, String tenant, String driver, String url) {
        return metaDataMap.computeIfAbsent(username, k -> new MetaData(username, password, tenant, driver, url));
    }

    public void removeMetaData(String username) {
        metaDataMap.remove(username);
    }
}
