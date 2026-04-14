package com.globalvibe.arbitrage.config;

import com.globalvibe.arbitrage.domain.task.model.TaskMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.task")
public class TaskExecutionProperties {

    private int phase1CandidateLimit = 20;
    private long processingDelayMillis = 250L;
    private long phase1WorkflowTimeoutMillis = 60_000L;
    private TaskMode defaultMode = TaskMode.AUTO_FALLBACK;
}
