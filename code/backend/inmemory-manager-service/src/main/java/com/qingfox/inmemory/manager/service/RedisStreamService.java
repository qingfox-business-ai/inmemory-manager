package com.qingfox.inmemory.manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingfox.inmemory.manager.config.RedisStreamProperties;
import com.qingfox.inmemory.manager.model.dto.ClientDTO;
import com.qingfox.inmemory.manager.model.dto.StreamInfoDTO;
import com.qingfox.inmemory.manager.model.dto.StreamMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
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
        String stream = properties.getRedisStream();
        String group = properties.getConsumerGroup();
        try {
            var records = redisTemplate.opsForStream().range(stream, Range.unbounded());
            if (records == null || records.isEmpty()) {
                streamMessages = Collections.emptyList();
                return;
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
                log.warn("pending query failed: {}", e.getMessage());
            }
            List<StreamMessageDTO> result = new ArrayList<>();
            for (var r : records) {
                String id = r.getId().getValue();
                var value = r.getValue();
                Object attrRaw = value.get("attribute");
                Map<String, Object> attr = parseAttribute(attrRaw);
                boolean ack = !pendingIds.contains(id);
                result.add(StreamMessageDTO.builder()
                        .id(id)
                        .ack(ack)
                        .attribute(attr)
                        .build());
            }
            streamMessages = result;
            log.info("polled {} stream messages", result.size());
        } catch (Exception e) {
            log.error("poll stream failed", e);
        }
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
