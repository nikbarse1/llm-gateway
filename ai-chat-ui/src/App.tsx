import { useState } from 'react';
import { MainLayout } from './components/layout/MainLayout';
import { Sidebar } from './components/layout/Sidebar';
import { ChatWindow } from './components/chat/ChatWindow';
import { ChatInput } from './components/chat/ChatInput';
import { ChatService } from './services/apiService';
import { type ChatMessage, type OptimizationMetrics } from './types/api';

function App() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [currentChatId, setCurrentChatId] = useState<string | undefined>();
  const [sidebarRefreshToggle, setSidebarRefreshToggle] = useState(false);
  const [lastMetrics, setLastMetrics] = useState<OptimizationMetrics | null>(null);
  const [lastWasOptimized, setLastWasOptimized] = useState(false);
  const [lastWasDevMode, setLastWasDevMode] = useState(false);

  const handleSendMessage = async (
    text: string,
    file: File | null,
    url: string | null,
    isDevMode: boolean,
    provider: string,
    contextWindow: number
  ) => {
    let displayContent = text;

    if (file) {
      displayContent += `\n\n[Attached File: ${file.name}]`;
    }

    if (url) {
      displayContent += `\n\n[Attached URL: ${url}]`;
    }

    const newUserMessage: ChatMessage = {
      role: 'USER',
      content: displayContent,
    };

    setMessages((prev) => [...prev, newUserMessage]);
    setIsLoading(true);

    try {
      const response = await ChatService.sendMessage({
        instruction: text || 'Please process the attached data.',
        file,
        url: url || undefined,
        chatId: currentChatId,
        provider,
        contextWindow,
        isDevMode,
      });

      if (response.chatId && response.chatId !== currentChatId) {
        setCurrentChatId(response.chatId);
        setSidebarRefreshToggle((prev) => !prev);
      }

      const aiMessage: ChatMessage = {
        role: 'ASSISTANT',
        content: response.userReadableMessage,
      };

      setMessages((prev) => [...prev, aiMessage]);
      setLastWasDevMode(isDevMode);
      setLastWasOptimized(response.wasOptimized);
      setLastMetrics(response.optimizationMetrics);
    } catch (error) {
      console.error('Error communicating with Spring Boot API:', error);

      const errorText = error instanceof Error ? `Error: ${error.message}` : 'Sorry, an error occurred while sending the message.';
      const errorMessage: ChatMessage = {
        role: 'ASSISTANT',
        content: errorText,
      };

      setMessages((prev) => [...prev, errorMessage]);
      setLastMetrics(null);
      setLastWasOptimized(false);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSelectChat = async (chatId: string) => {
    if (!chatId) {
      setCurrentChatId(undefined);
      setMessages([]);
      return;
    }

    setCurrentChatId(chatId);
    setIsLoading(true);
    setMessages([]);
    setLastMetrics(null);
    setLastWasOptimized(false);
    setLastWasDevMode(false);

    try {
      const transcript = await ChatService.getTranscript(chatId);
      setMessages(transcript.messages);
    } catch (error) {
      console.error('Failed to load transcript:', error);

      const errorMessage: ChatMessage = {
        role: 'ASSISTANT',
        content: 'Failed to load chat history.',
      };

      setMessages([errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <MainLayout
      sidebar={
        <Sidebar
          currentChatId={currentChatId}
          onSelectChat={handleSelectChat}
          refreshTrigger={sidebarRefreshToggle}
        />
      }
    >
      {/* THE BULLETPROOF LAYOUT FIX */}
      <div className="relative h-full w-full overflow-hidden bg-zinc-950">
        
        {/* 1. Chat Window Container - Pinned to all 4 corners so it MUST scroll */}
        <div className="absolute inset-0 flex flex-col">
          <ChatWindow
            messages={messages}
            isLoading={isLoading}
            lastMetrics={lastMetrics}
            lastWasOptimized={lastWasOptimized}
            lastWasDevMode={lastWasDevMode}
          />
        </div>
        
        {/* 2. Floating Input Container - Pinned to the bottom */}
        <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-zinc-950 via-zinc-950/90 to-transparent pt-12 pb-6 px-4 md:px-8 pointer-events-none">
          <div className="max-w-3xl mx-auto w-full pointer-events-auto">
            <ChatInput onSendMessage={handleSendMessage} isLoading={isLoading} />
          </div>
        </div>

      </div>
    </MainLayout>
  );
}

export default App;