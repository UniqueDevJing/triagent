package com.health.framework.realtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeEvent {
    private String topic;
    private String event;
    private String data;
}
