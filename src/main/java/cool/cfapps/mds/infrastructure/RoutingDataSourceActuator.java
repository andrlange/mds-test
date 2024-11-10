package cool.cfapps.mds.infrastructure;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
@Endpoint(id = "routing-data-sources")
public class RoutingDataSourceActuator {
    private final RoutingDataSource routingDataSource;

    public RoutingDataSourceActuator(RoutingDataSource routingDataSource) {
        this.routingDataSource = routingDataSource;
    }

    @ReadOperation
    public String read() {

        List<StatisticsData> statistics = new ArrayList<>();


        ObjectMapper mapper = new ObjectMapper();
        String result = "[]";
        try {
            routingDataSource.getLastAccess().forEach((k, v) -> {
                statistics.add(new StatisticsData(k, routingDataSource.getCountAccess().get(k), v,
                        Math.abs(new Date().getTime() - v.getTime()) / 1000));
            });
            result = mapper.writeValueAsString(new StatisticsDataList(statistics));
        } catch (Exception e) {
            log.error("Error serializing statistics data", e);
        }


        return result;
    }

    @Data
    @AllArgsConstructor
    private static class StatisticsDataList implements Serializable {
        List<StatisticsData> statistics;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class StatisticsData implements Serializable {
        private String key;
        private long accessCount;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
        private Date lastAccess;
        private Long secondsIdle;
    }
}
