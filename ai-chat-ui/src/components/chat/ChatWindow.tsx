import { useEffect, useRef } from 'react';
import { type ChatMessage, type OptimizationMetrics } from '../../types/api';
import { MessageBubble } from './MessageBubble';
import { MetricsPanel } from './MetricsPanel';
import { Bot } from 'lucide-react';

interface ChatWindowProps {
  messages: ChatMessage[];
  isLoading: boolean;
  lastMetrics: OptimizationMetrics | null;
  lastWasOptimized: boolean;
  lastWasDevMode: boolean;
}

export function ChatWindow({ messages, isLoading, lastMetrics, lastWasOptimized, lastWasDevMode }: ChatWindowProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  return (
    // The container that handles the scrolling
    <div className="flex-1 overflow-y-auto w-full scroll-smooth">
      
      {messages.length === 0 && !isLoading && (
        <div className="flex flex-col items-center justify-center h-full text-center px-4 pb-20">
          <div className="w-16 h-16 bg-emerald-600/10 rounded-2xl flex items-center justify-center mb-6 border border-emerald-600/20 shadow-[0_0_15px_rgba(16,185,129,0.1)]">
            <Bot size={32} className="text-emerald-500" />
          </div>
          <h2 className="text-2xl font-semibold text-zinc-200 mb-3">How can I help you today?</h2>
          <p className="text-zinc-500 max-w-md text-sm leading-relaxed">
            I am connected to your Spring Boot AI backend. You can ask me questions, attach documents, or share URLs for me to process using Gemini.
          </p>
        </div>
      )}

      {messages.map((message, index) => (
        <MessageBubble key={index} message={message} />
      ))}

      {isLoading && (
        <div className="py-6 px-4 md:px-8 w-full bg-zinc-900 border-y border-zinc-800">
          <div className="max-w-3xl mx-auto flex gap-4 md:gap-6 items-center">
            <div className="w-8 h-8 rounded-sm bg-emerald-600 flex items-center justify-center shadow-lg shadow-emerald-900/20">
               <Bot size={18} className="text-white" />
            </div>
            <div className="text-zinc-400 text-sm font-medium flex items-center gap-1">
               <span className="animate-pulse">AI is processing</span>
            </div>
          </div>
        </div>
      )}

      <MetricsPanel
        metrics={lastMetrics}
        wasOptimized={lastWasOptimized}
        isVisible={lastWasDevMode}
      />

      {/* THE FIX: Added flex-shrink-0 and increased height to push past the input box */}
      <div ref={messagesEndRef} className="h-48 w-full flex-shrink-0" />
    </div>
  );
}