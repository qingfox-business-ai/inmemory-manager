package com.qingfox.inmemory.manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingfox.inmemory.manager.config.RedisStreamProperties;
import com.qingfox.inmemory.manager.model.dto.ClientDTO;
import com.qingfox.inmemory.manager.model.dto.StreamInfoDTO;
import com.qingfox.inmemory.manager.model.dto.PageResult;
import com.qingfox.inmemory.manager.model.dto.StreamMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStreamService {

    private final StringRedisTemplate redisTemplate;
    private final RedisStreamProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile List<StreamMessageDTO> streamMessages = Collections.emptyList();

    public StreamInfoDTO getStreamInfo() {
        String streamKey = properties.getRedisStream();

        return StreamInfoDTO.builder()
                .streamName(streamKey)
                .totalMessageCount(streamMessages.size())
                .messages(Collections.emptyList())
                .build();
    }

    @Scheduled(fixedRate = 10000)
    public void pollStream() {
        try {
            List<StreamMessageDTO> main = pollMain();
            List<StreamMessageDTO> dlq = pollDlq();
            List<StreamMessageDTO> result = new ArrayList<>(main.size() + dlq.size());
            result.addAll(main);
            result.addAll(dlq);
            streamMessages = result;
            log.info("polled stream messages: main={}, dlq={}", main.size(), dlq.size());
        } catch (Exception e) {
            log.error("poll stream failed", e);
        }
    }

    private List<StreamMessageDTO> pollMain() {
        String stream = properties.getRedisStream();
        String group = properties.getConsumerGroup();
        var records = redisTemplate.opsForStream().range(stream, Range.unbounded());
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> pendingIds = new HashSet<>();
        try {
            PendingMessages pending = redisTemplate.opsForStream()
                    .pending(stream, group, Range.unbounded(), (long) records.size());
            if (pending != null) {
                for (PendingMessage pm : pending) {
                    pendingIds.add(pm.getId().getValue());
                }
            }
        } catch (Exception e) {
            log.warn("main pending query failed: {}", e.getMessage());
        }
        String lastDeliveredId = "0-0";
        try {
            StreamInfo.XInfoGroups groups = redisTemplate.opsForStream().groups(stream);
            if (groups != null) {
                for (StreamInfo.XInfoGroup g : groups) {
                    if (group.equals(g.groupName())) {
                        String lid = g.lastDeliveredId();
                        if (lid != null) {
                            lastDeliveredId = lid;
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("groups query failed: {}", e.getMessage());
        }
        List<StreamMessageDTO> result = new ArrayList<>();
        for (var r : records) {
            String id = r.getId().getValue();
            var value = r.getValue();
            boolean inPending = pendingIds.contains(id);
            String status;
            if (inPending) {
                status = "running";
            } else if (compareStreamId(id, lastDeliveredId) > 0) {
                status = "waiting";
            } else {
                status = "done";
            }
            result.add(StreamMessageDTO.builder()
                    .id(id)
                    .ack("done".equals(status))
                    .status(status)
                    .attribute(parseAttribute(value.get("attribute")))
                    .build());
        }
        return result;
    }

    private int compareStreamId(String id1, String id2) {
        String[] p1 = id1.split("-");
        String[] p2 = id2.split("-");
        long m1 = Long.parseLong(p1[0]); long s1 = Long.parseLong(p1[1]);
        long m2 = Long.parseLong(p2[0]); long s2 = Long.parseLong(p2[1]);
        int cmp = Long.compare(m1, m2);
        return cmp != 0 ? cmp : Long.compare(s1, s2);
    }

    private List<StreamMessageDTO> pollDlq() {
        String stream = properties.getRedisStream() + "-DLQ";
        var records = redisTemplate.opsForStream().range(stream, Range.unbounded());
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<StreamMessageDTO> result = new ArrayList<>();
        for (var r : records) {
            String id = r.getId().getValue();
            var value = r.getValue();
            result.add(StreamMessageDTO.builder()
                    .id(id)
                    .ack(false)
                    .status("failed")
                    .attribute(parseAttribute(value.get("attribute")))
                    .errorStack(value.get("errorStack") != null ? value.get("errorStack").toString() : null)
                    .build());
        }
        return result;
    }

    private Map<String, Object> parseAttribute(Object raw) {
        if (raw == null) return null;
        try {
            return objectMapper.readValue(raw.toString(), Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    public List<StreamMessageDTO> getStreamMessages() {
        return streamMessages;
    }

    public PageResult<StreamMessageDTO> getStreamMessagesPage(int page, int size, String status, String search, String taskId) {
        String lowerSearch = search == null ? "" : search.toLowerCase();
        List<StreamMessageDTO> filtered = new ArrayList<>();
        for (StreamMessageDTO m : streamMessages) {
            if (taskId != null && !taskId.isEmpty()) {
                Object tidObj = m.getAttribute() != null ? m.getAttribute().get("taskId") : null;
                String tid = tidObj != null ? tidObj.toString() : "";
                if (!taskId.equals(tid)) continue;
            }
            if (status != null && !status.isEmpty() && !status.equals(m.getStatus())) continue;
            if (!lowerSearch.isEmpty()) {
                String id = m.getId() != null ? m.getId() : "";
                Object taskIdObj = m.getAttribute() != null ? m.getAttribute().get("taskId") : null;
                String tid = taskIdObj != null ? taskIdObj.toString() : "";
                if (!id.toLowerCase().contains(lowerSearch) && !tid.toLowerCase().contains(lowerSearch)) continue;
            }
            filtered.add(m);
        }
        int total = filtered.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);
        List<StreamMessageDTO> list = from < to ? new ArrayList<>(filtered.subList(from, to)) : Collections.emptyList();
        return PageResult.<StreamMessageDTO>builder().list(list).total(total).page(page).size(size).build();
    }

    public List<ClientDTO> getClients() {
        String indexKey = "stream:" + properties.getRedisStream() + ":" + properties.getConsumerGroup() + ":consumer:index";
        Set<String> members = redisTemplate.opsForZSet().range(indexKey, 0, -1);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        String heartbeatPrefix = "stream:" + properties.getRedisStream() + ":" + properties.getConsumerGroup() + ":consumer:heartbeat:";
        String leaderId = getLeaderClientId();
        List<ClientDTO> list = new ArrayList<>();
        for (String raw : members) {
            String clientId = stripQuotes(raw);
            Map<Object, Object> hash = redisTemplate.opsForHash().entries(heartbeatPrefix + clientId);
            boolean online = !hash.isEmpty();
            String firstHeartbeat = hashStr(hash, "firstHeartbeatTime");
            String lastHeartbeat = hashStr(hash, "lastHeartbeatTime");
            String consumerIndex = hashStr(hash, "consumerIndex");
            list.add(ClientDTO.builder()
                    .clientId(clientId)
                    .address(extractAddress(clientId))
                    .status(online ? "online" : "offline")
                    .firstConnect(parseLong(firstHeartbeat))
                    .lastHeartbeat(parseLong(lastHeartbeat))
                    .consumerIndex(parseInteger(consumerIndex))
                    .leader(clientId.equals(leaderId))
                    .build());
        }
        list.sort(Comparator.comparingInt(c -> c.getConsumerIndex() == null ? 0 : c.getConsumerIndex()));
        return list;
    }

    public String getLeaderClientId() {
        String key = "stream:" + properties.getRedisStream() + ":" + properties.getConsumerGroup() + ":consumer:leader";
        String raw = redisTemplate.opsForValue().get(key);
        return stripQuotes(raw);
    }

    private String stripQuotes(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private String extractAddress(String clientId) {
        if (clientId == null || !clientId.contains("-")) return "";
        String[] parts = clientId.split("-");
        return parts[parts.length - 1];
    }

    private String hashStr(Map<Object, Object> hash, String key) {
        Object v = hash.get(key);
        return v != null ? v.toString() : null;
    }

    private Long parseLong(String v) {
        if (v == null || v.isEmpty()) return null;
        try { return Long.parseLong(v); } catch (Exception e) { return null; }
    }

    private Integer parseInteger(String v) {
        if (v == null || v.isEmpty()) return null;
        try { return Integer.parseInt(v); } catch (Exception e) { return null; }
    }
}
