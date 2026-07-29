// This is exactly like defining a Java DTO or Record
export interface ChatMessage {
  id: string;
  role: 'user' | 'ai'; // Enforcing that it can ONLY be one of these two strings
  content: string;
}