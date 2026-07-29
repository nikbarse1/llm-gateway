import { useState, useRef } from 'react';
import { Paperclip, Link as LinkIcon, Send, ChevronDown, X, Check } from 'lucide-react';

// 1. UPDATE INTERFACE: Added 'url: string | null'
interface ChatInputProps {
  onSendMessage: (message: string, file: File | null, url: string | null, isDevMode: boolean, provider: string, contextWindow: number) => void;
  isLoading: boolean;
}

export function ChatInput({ onSendMessage, isLoading }: ChatInputProps) {
  const [message, setMessage] = useState('');
  
  // File State
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 2. URL STATES
  const [selectedUrl, setSelectedUrl] = useState<string | null>(null);
  const [showUrlInput, setShowUrlInput] = useState(false);
  const [urlInputValue, setUrlInputValue] = useState('');

  // Settings State
  const [isDevMode, setIsDevMode] = useState(false);
  const [provider, setProvider] = useState('GEMINI');
  const [contextWindow, setContextWindow] = useState(8192);

  // 3. VALIDATION: Can send if we have text, a file, OR a URL
  const isMessageEmpty = message.trim().length === 0;
  const isSubmitDisabled = (isMessageEmpty && !selectedFile && !selectedUrl) || isLoading;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
    }
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  // 4. URL HANDLERS
  const handleAddUrl = () => {
    if (urlInputValue.trim()) {
      setSelectedUrl(urlInputValue.trim());
      setShowUrlInput(false);
      setUrlInputValue('');
    }
  };

  const handleSubmit = () => {
    if (isSubmitDisabled) return;
    
    // 5. Pass everything up!
    onSendMessage(message, selectedFile, selectedUrl, isDevMode, provider, contextWindow);
    
    // Clear all states
    setMessage('');
    setSelectedFile(null);
    setSelectedUrl(null);
    setShowUrlInput(false);
  };

  return (
    <div className="flex flex-col gap-3 w-full">
      <div className="relative flex flex-col bg-zinc-800/40 border border-zinc-700/50 rounded-2xl p-3 shadow-xl backdrop-blur-md focus-within:border-zinc-600 focus-within:bg-zinc-800/60 transition-all">
        
        {/* CHIP AREA (Flex container to hold multiple chips side-by-side) */}
        <div className="flex flex-wrap gap-2 mb-2">
          
          {/* File Chip */}
          {selectedFile && (
            <div className="flex items-center gap-2 bg-zinc-700/50 border border-zinc-600 text-zinc-200 text-sm px-3 py-1.5 rounded-lg w-max">
              <Paperclip size={14} className="text-zinc-400" />
              <span className="truncate max-w-[150px]">{selectedFile.name}</span>
              <button onClick={() => setSelectedFile(null)} className="text-zinc-400 hover:text-red-400 transition-colors p-0.5 rounded-full hover:bg-zinc-600">
                <X size={14} />
              </button>
            </div>
          )}

          {/* URL Chip */}
          {selectedUrl && (
            <div className="flex items-center gap-2 bg-zinc-700/50 border border-zinc-600 text-zinc-200 text-sm px-3 py-1.5 rounded-lg w-max">
              <LinkIcon size={14} className="text-emerald-400" />
              <span className="truncate max-w-[150px]">{selectedUrl}</span>
              <button onClick={() => setSelectedUrl(null)} className="text-zinc-400 hover:text-red-400 transition-colors p-0.5 rounded-full hover:bg-zinc-600">
                <X size={14} />
              </button>
            </div>
          )}
        </div>

        {/* URL INPUT FIELD (Toggles on when Link icon is clicked) */}
        {showUrlInput && (
          <div className="flex items-center gap-2 bg-zinc-900 border border-zinc-600 rounded-lg p-2 mb-2">
            <LinkIcon size={14} className="text-zinc-400 ml-1" />
            <input 
              autoFocus
              type="url"
              value={urlInputValue}
              onChange={(e) => setUrlInputValue(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleAddUrl()}
              placeholder="https://example.com"
              className="flex-1 bg-transparent text-sm text-zinc-200 outline-none placeholder:text-zinc-500"
            />
            <button onClick={handleAddUrl} className="text-emerald-500 hover:text-emerald-400 p-1">
              <Check size={16} />
            </button>
            <button onClick={() => setShowUrlInput(false)} className="text-zinc-400 hover:text-zinc-300 p-1">
              <X size={16} />
            </button>
          </div>
        )}

        <textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              handleSubmit();
            }
          }}
          placeholder="Message AI Chat..."
          className="w-full bg-transparent text-zinc-100 placeholder:text-zinc-500 resize-none focus:outline-none min-h-[60px] max-h-[200px] text-base"
          rows={2}
          disabled={isLoading}
        />

        <div className="flex justify-between items-center mt-2">
          <div className="flex gap-1 md:gap-2">
            
            <input type="file" ref={fileInputRef} onChange={handleFileChange} className="hidden" />
            
            <button 
              onClick={() => fileInputRef.current?.click()}
              className="p-2 rounded-lg text-zinc-400 hover:text-zinc-200 hover:bg-zinc-700/50 transition-colors" 
              title="Attach File" 
              disabled={isLoading}
            >
              <Paperclip size={18} />
            </button>

            {/* URL BUTTON: Toggles the input field */}
            <button 
              onClick={() => setShowUrlInput(!showUrlInput)}
              className={`p-2 rounded-lg transition-colors ${showUrlInput ? 'bg-zinc-700 text-zinc-200' : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-700/50'}`}
              title="Attach URL" 
              disabled={isLoading}
            >
              <LinkIcon size={18} />
            </button>
          </div>

          <button 
            onClick={handleSubmit}
            disabled={isSubmitDisabled}
            className={`p-2 rounded-xl transition-all duration-200 ${
              isSubmitDisabled
                ? 'bg-zinc-700 text-zinc-500 cursor-not-allowed opacity-50' 
                : 'bg-emerald-600 text-white hover:bg-emerald-500 shadow-md shadow-emerald-900/20'
            }`}
          >
            <Send size={18} />
          </button>
        </div>
      </div>

      {/* SETTINGS BAR (Unchanged) */}
      <div className="flex flex-wrap items-center justify-between text-xs text-zinc-500 px-2">
        <div className="flex items-center gap-4">
           <label className="flex items-center gap-2 cursor-pointer hover:text-zinc-300 transition-colors">
             <input type="checkbox" checked={isDevMode} onChange={(e) => setIsDevMode(e.target.checked)} className="accent-emerald-600 rounded-sm cursor-pointer" />
             <span>Developer Mode</span>
           </label>
           <div className="hidden md:flex items-center gap-1 hover:text-zinc-300 transition-colors">
             <span>Provider:</span>
             <select value={provider} onChange={(e) => setProvider(e.target.value)} className="bg-transparent font-medium text-zinc-400 outline-none cursor-pointer">
               <option value="GEMINI">GEMINI</option>
               <option value="OPENAI">OPENAI</option>
               <option value="ANTHROPIC">ANTHROPIC</option>
             </select>
           </div>
           <div className="hidden md:flex items-center gap-1 hover:text-zinc-300 transition-colors">
             <span>Context:</span>
             <select value={contextWindow} onChange={(e) => setContextWindow(Number(e.target.value))} className="bg-transparent font-medium text-zinc-400 outline-none cursor-pointer">
               <option value="4096">4096</option>
               <option value="8192">8192</option>
               <option value="16384">16384</option>
             </select>
           </div>
        </div>
        <div className="hidden sm:block">AI can make mistakes. Verify important information.</div>
      </div>
    </div>
  );
}