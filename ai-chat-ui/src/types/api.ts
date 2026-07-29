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

export interface UsageMetrics {
  expectedTokensBeforeOptimization: number;
  actualPromptTokens: number;
  actualCompletionTokens: number;
  actualTotalTokens: number;
  tokensSaved: number;
  savingsPercentage: number;
}

export interface OptimizationMetrics {
  timestamp: string; 
  usageMetrics: UsageMetrics;
}

// This maps directly to AiChatResponse class
export interface AiChatResponse {
  userReadableMessage: string;
  sourceType: string;
  wasOptimized: boolean;
  optimizationMetrics: OptimizationMetrics | null;
  chatId: string;
}
