package com.globalvibe.arbitrage.ai.workflow;

import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;

import java.util.List;

public record Phase1WorkflowResult(
        List<CandidateProduct> candidates,
        List<TaskLogEntry> logs,
        List<Product> sourceProducts,
        boolean fallbackUsed
) {
}
