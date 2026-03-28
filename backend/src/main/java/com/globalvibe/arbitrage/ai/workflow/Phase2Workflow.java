package com.globalvibe.arbitrage.ai.workflow;

import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;

public interface Phase2Workflow {

    Phase2WorkflowResult run(AnalysisTask phase2Task, AnalysisTask phase1Task, String productId);
}
