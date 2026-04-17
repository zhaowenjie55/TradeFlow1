export type SearchPhase =
  | "query_parsed"
  | "marketplace_search"
  | "candidate_filtering"
  | "candidate_ranking"
  | "results_streaming"
  | "complete"

export interface SearchPhaseConfig {
  label: string
  panelTitle: string
  panelSubtitle: string
  reasoning: string
}

export const SEARCH_PHASE_SEQUENCE: SearchPhase[] = [
  "query_parsed",
  "marketplace_search",
  "candidate_filtering",
  "candidate_ranking",
  "results_streaming",
  "complete",
]

export const SEARCH_PHASE_CONFIG: Record<SearchPhase, SearchPhaseConfig> = {
  query_parsed: {
    label: "Query Parsed",
    panelTitle: "Understanding search intent...",
    panelSubtitle: "TradeFlow is mapping the request to product categories and sourcing constraints.",
    reasoning: "Understanding the search intent and mapping it to relevant product categories.",
  },
  marketplace_search: {
    label: "Marketplace Search Running",
    panelTitle: "Scanning overseas marketplaces...",
    panelSubtitle: "TradeFlow is collecting candidates and aligning live marketplace signals.",
    reasoning: "Scanning overseas marketplaces for high-frequency listings in this category.",
  },
  candidate_filtering: {
    label: "Candidate Filtering",
    panelTitle: "Filtering candidate signals...",
    panelSubtitle: "Low-relevance matches are being removed before the shortlist is ranked.",
    reasoning: "Filtering out low-relevance candidates based on pricing consistency and review signals.",
  },
  candidate_ranking: {
    label: "Candidate Ranking In Progress",
    panelTitle: "Ranking opportunity candidates...",
    panelSubtitle: "TradeFlow is comparing overseas demand, domestic supply, and margin signals.",
    reasoning: "Evaluating price gaps between overseas listings and domestic supply.",
  },
  results_streaming: {
    label: "Results Streaming",
    panelTitle: "Preparing ranked results...",
    panelSubtitle: "The shortlist is ready and candidate cards are being streamed into the workbench.",
    reasoning: "Preparing a ranked shortlist based on margin potential and demand stability.",
  },
  complete: {
    label: "Complete",
    panelTitle: "Results ready",
    panelSubtitle: "Candidate discovery is complete and the ranked shortlist is available.",
    reasoning: "Ranked candidates are ready for selection and deeper analysis.",
  },
}

export function getSearchPhaseIndex(phase: SearchPhase) {
  return SEARCH_PHASE_SEQUENCE.indexOf(phase)
}
