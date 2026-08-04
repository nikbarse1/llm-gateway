import axios, { isAxiosError } from 'axios';
import type { AiChatResponse, ChatTranscript, SessionInfo } from '../types/api';

// 1. Create a base "RestTemplate" equivalent configured for our proxy
const apiClient = axios.create({
  baseURL: '/api/v2', // Vite will proxy this to http://localhost:8080/api/v2
  timeout: 65000,     // Client-side guard so the UI does not hang if the LLM stalls
});

// 2. Define the exact arguments needed for the POST request
export interface ChatRequestParams {
  instruction: string;
  file?: File | null;      // ? means optional in TypeScript
  url?: string;
  chatId?: string;
  provider: string;
  contextWindow: number;
  isDevMode: boolean;
}

// Helper to surface the most useful part of an Axios error
export function describeError(error: unknown): string {
  if (isAxiosError(error)) {
    const message = error.response?.data?.message
      || error.response?.data?.error
      || error.message;
    return message;
  }
  if (error instanceof Error) return error.message;
  return 'An unexpected error occurred.';
}

// 3. Export our Service object (acts like a Singleton @Service class)
export const ChatService = {

  // POST /api/v2/chat
  async sendMessage(params: ChatRequestParams): Promise<AiChatResponse> {
    // Because your endpoint consumes MULTIPART_FORM_DATA, we must use FormData
    const formData = new FormData();

    // Required
    formData.append('instruction', params.instruction);

    // Optionals
    if (params.file) formData.append('file', params.file);
    if (params.url) formData.append('url', params.url);
    if (params.chatId) formData.append('chatId', params.chatId);

    formData.append('provider', params.provider);
    formData.append('contextWindow', params.contextWindow.toString());

    // We pass headers separately in Axios
    try {
      const response = await apiClient.post<AiChatResponse>('/chat', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
          'X-Developer-Mode': params.isDevMode.toString(),
        },
      });

      return response.data; // Axios automatically unwraps the JSON into our interface!
    } catch (error) {
      throw new Error(describeError(error));
    }
  },

  // GET /api/v2/chat-history/sessions
  async getSessions(): Promise<SessionInfo[]> {
    const response = await apiClient.get<SessionInfo[]>('/chat-history/sessions');
    return response.data;
  },

  // GET /api/v2/chat-history/{chatId}
  async getTranscript(chatId: string): Promise<ChatTranscript> {
    const response = await apiClient.get<ChatTranscript>(`/chat-history/${chatId}`);
    return response.data;
  },

  // DELETE /api/v2/chat-history/{chatId}
  async deleteSession(chatId: string): Promise<void> {
    await apiClient.delete(`/chat-history/${chatId}`);
  }
};