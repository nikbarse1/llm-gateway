// This maps directly to your SessionInfoDto record
export interface SessionInfo {
  chatId: string;
  title: string;
}

// This maps directly to your ChatMessageDto record
export interface ChatMessage {
  role: string;
  content: string;
}

// This maps directly to your ChatTranscriptDto record
export interface ChatTranscript {
  chatId: string;
  messages: ChatMessage[];
}

// --- COMPLEX RESPONSES FROM POST /api/v2/chat ---

export interface RoutingDecision {
  requestedProvider: string;
  executedProvider: string;
  actionTaken?: string;
}

export interface UsageMetrics {
  unoptimizedPromptTokens: number;
  optimizedPromptTokens: number;
  actualPromptTokens: number;
  actualCompletionTokens: number;
  actualTotalTokens: number;
  tokensSaved: number;
  savingsPercentage: number;
}

export interface PayloadSnapshot {
  contextWindowSize: number;
  remainingHeadroom: number;
  finalPrompt?: string;
}

export interface BudgetActionDetail {
  section: string;
  requestedTokens: number;
  allocatedTokens: number;
  budgetLimit: number;
  actionTaken: string;
}

export interface BudgetAllocation {
  totalBudget: number;
  systemReserve: number;
  historyReserve: number;
  ragReserve: number;
  userReserve: number;
  responseReserve: number;
  actions: BudgetActionDetail[];
}

export interface HistoryOptimization {
  rawHistoryTokens: number;
  summarizedHistoryTokens: number;
  historyTokensSaved: number;
}

export interface OptimizationMetrics {
  timestamp: string;
  routingDecision?: RoutingDecision;
  usageMetrics: UsageMetrics;
  payloadSnapshot?: PayloadSnapshot;
  budgetAllocation?: BudgetAllocation;
  historyOptimization?: HistoryOptimization;
}

// This maps directly to AiChatResponse class
export interface AiChatResponse {
  userReadableMessage: string;
  sourceType: string;
  wasOptimized: boolean;
  optimizationMetrics: OptimizationMetrics | null;
  chatId: string;
}
