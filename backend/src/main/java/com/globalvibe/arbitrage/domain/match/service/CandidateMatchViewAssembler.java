package com.globalvibe.arbitrage.domain.match.service;

import com.globalvibe.arbitrage.domain.match.dto.CandidateMatchVO;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import org.springframework.stereotype.Component;

@Component
public class CandidateMatchViewAssembler {

    public CandidateMatchVO toView(CandidateMatchRecord matchRecord) {
        return new CandidateMatchVO(
                matchRecord.matchId(),
                matchRecord.taskId(),
                matchRecord.candidateId(),
                matchRecord.sourceProductId(),
                matchRecord.platform(),
                matchRecord.externalItemId(),
                matchRecord.title(),
                matchRecord.price(),
                matchRecord.image(),
                matchRecord.link(),
                matchRecord.similarityScore(),
                matchRecord.matchSource(),
                matchRecord.searchKeyword(),
                matchRecord.fallbackUsed(),
                matchRecord.fallbackReason(),
                matchRecord.reason(),
                matchRecord.createdAt()
        );
    }
}
