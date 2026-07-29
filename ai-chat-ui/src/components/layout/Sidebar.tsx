import { useState, useEffect } from 'react';
import {
  Plus,
  MessageSquare,
  Search,
  Trash2,
  Settings,
  Loader2,
} from 'lucide-react';

import { ChatService } from '../../services/apiService';
import type { SessionInfo } from '../../types/api';

interface SidebarProps {
  currentChatId?: string;
  onSelectChat?: (chatId: string) => void;

  // Re-fetch sessions whenever this value changes
  refreshTrigger?: boolean;
}

export function Sidebar({
  currentChatId,
  onSelectChat,
  refreshTrigger,
}: SidebarProps) {
  const [sessions, setSessions] = useState<SessionInfo[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchSessions = async () => {
      try {
        const data = await ChatService.getSessions();
        setSessions(data);
      } catch (error) {
        console.error('Failed to load chat sessions:', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchSessions();
  }, [refreshTrigger]);

  const handleDeleteChat = async (
    chatIdToDelete: string,
    event: React.MouseEvent
  ) => {
    event.stopPropagation();

    // Optimistically remove from UI
    setSessions((prevSessions) =>
      prevSessions.filter(
        (session) => session.chatId !== chatIdToDelete
      )
    );

    // If deleted chat is active, switch to new chat
    if (chatIdToDelete === currentChatId && onSelectChat) {
      onSelectChat('');
    }

    try {
      await ChatService.deleteSession(chatIdToDelete);
    } catch (error) {
      console.error('Failed to delete chat from backend:', error);
    }
  };

  return (
    <div className="flex flex-col h-full bg-zinc-900 border-r border-zinc-800">
      {/* New Chat */}
      <div className="p-3">
        <button
          onClick={() => onSelectChat && onSelectChat('')}
          className="flex items-center gap-2 w-full px-3 py-2 bg-zinc-800 hover:bg-zinc-700 rounded-md transition-colors text-sm font-medium border border-zinc-700/50"
        >
          <Plus size={16} />
          New Chat
        </button>
      </div>

      {/* Search */}
      <div className="px-3 pb-3">
        <div className="relative">
          <Search
            className="absolute left-2.5 top-2 text-zinc-500"
            size={14}
          />
          <input
            type="text"
            placeholder="Search chats..."
            className="w-full bg-zinc-950 border border-zinc-800 rounded-md pl-8 pr-3 py-1.5 text-sm focus:outline-none focus:border-zinc-600 placeholder:text-zinc-600"
          />
        </div>
      </div>

      {/* Sessions */}
      <div className="flex-1 overflow-y-auto px-3 space-y-1">
        <div className="mt-2 mb-2 px-2 text-xs font-semibold text-zinc-500">
          Recent Sessions
        </div>

        {isLoading ? (
          <div className="flex justify-center p-4">
            <Loader2
              className="animate-spin text-zinc-500"
              size={20}
            />
          </div>
        ) : sessions.length === 0 ? (
          <div className="p-4 text-center text-sm text-zinc-500">
            No recent chats
          </div>
        ) : (
          sessions.map((chat) => {
            const isActive = chat.chatId === currentChatId;

            return (
              <div
                key={chat.chatId}
                onClick={() =>
                  onSelectChat && onSelectChat(chat.chatId)
                }
                className={`flex items-center justify-between group px-2 py-2 rounded-md cursor-pointer text-sm transition-colors ${
                  isActive
                    ? 'bg-zinc-800 text-zinc-100'
                    : 'text-zinc-400 hover:bg-zinc-800/50 hover:text-zinc-200'
                }`}
              >
                <div className="flex items-center gap-2 overflow-hidden">
                  <MessageSquare size={14} />
                  <span className="truncate">
                    {chat.title}
                  </span>
                </div>

                <button
                  title="Delete Chat"
                  onClick={(e) =>
                    handleDeleteChat(chat.chatId, e)
                  }
                  className="opacity-0 group-hover:opacity-100 text-zinc-500 hover:text-red-400 transition-opacity"
                >
                  <Trash2 size={14} />
                </button>
              </div>
            );
          })
        )}
      </div>

      {/* Footer */}
      <div className="border-t border-zinc-800 p-3">
        <button className="flex items-center gap-2 w-full px-3 py-2 rounded-md text-sm font-medium text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors">
          <Settings size={16} />
          Settings
        </button>
      </div>
    </div>
  );
}