package com.globalvibe.arbitrage.ai.workflow;

import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;

public interface Phase1Workflow {

    Phase1WorkflowResult run(AnalysisTask analysisTask);
}
