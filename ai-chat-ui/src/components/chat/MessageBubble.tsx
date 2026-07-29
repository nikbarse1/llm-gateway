import { useState } from 'react';
import { User, Bot, Check, Copy } from 'lucide-react';
import type { ChatMessage } from '../../types/api';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';

// 1. IMPORT FRAMER MOTION
import { motion } from 'framer-motion';

interface MessageBubbleProps {
  message: ChatMessage;
}

// --- ISOLATED CODE BLOCK COMPONENT (Unchanged) ---
function CodeBlock({ language, codeString }: { language: string; codeString: string }) {
  const [isCopied, setIsCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(codeString);
    setIsCopied(true);
    setTimeout(() => {
      setIsCopied(false);
    }, 2000);
  };

  return (
    <div className="my-4 rounded-lg overflow-hidden border border-zinc-700 bg-zinc-950">
      <div className="flex items-center justify-between px-4 py-2 bg-zinc-800 text-zinc-400 text-xs font-sans">
        <span className="lowercase">{language || 'text'}</span>
        <button 
          onClick={handleCopy}
          className="flex items-center gap-1.5 hover:text-zinc-200 transition-colors"
        >
          {isCopied ? (
            <>
              <Check className="text-emerald-500" size={14} />
              <span className="text-emerald-500">Copied!</span>
            </>
          ) : (
            <>
              <Copy size={14} />
              <span>Copy code</span>
            </>
          )}
        </button>
      </div>
      <SyntaxHighlighter 
        language={language} 
        style={vscDarkPlus}
        customStyle={{ margin: 0, padding: '1rem', backgroundColor: 'transparent', fontSize: '0.875rem' }}
      >
        {codeString}
      </SyntaxHighlighter>
    </div>
  );
}

// --- MAIN COMPONENT ---
export function MessageBubble({ message }: MessageBubbleProps) {
  const isUser = message.role.toUpperCase() === 'USER';

  return (
    // 2. SWAP <div> FOR <motion.div> AND ADD ANIMATION PROPS
    <motion.div 
      initial={{ opacity: 0, y: 15 }}    // Start invisible and pushed down 15 pixels
      animate={{ opacity: 1, y: 0 }}     // End fully visible at its natural vertical position (y: 0)
      transition={{ duration: 0.4, ease: "easeOut" }} // Take 0.4 seconds to smoothly glide into place
      className={`py-6 px-4 md:px-8 w-full ${isUser ? 'bg-zinc-950' : 'bg-zinc-900 border-y border-zinc-800'}`}
    >
      <div className="max-w-3xl mx-auto flex gap-4 md:gap-6">
        
        {/* AVATAR */}
        <div className="flex-shrink-0 mt-1">
          <div className={`w-8 h-8 rounded-sm flex items-center justify-center ${
            isUser ? 'bg-zinc-700 text-zinc-300' : 'bg-emerald-600 text-white'
          }`}>
            {isUser ? <User size={18} /> : <Bot size={18} />}
          </div>
        </div>

        {/* MESSAGE CONTENT */}
        <div className="flex-1 space-y-2 overflow-hidden min-w-0">
          <div className="font-semibold text-sm text-zinc-300">
            {isUser ? 'You' : 'AI Assistant'}
          </div>
          
          {isUser ? (
            <div className="text-zinc-100 text-base leading-relaxed whitespace-pre-wrap">
              {message.content}
            </div>
          ) : (
            <div className="text-zinc-100 text-base leading-relaxed break-words">
              <ReactMarkdown 
                remarkPlugins={[remarkGfm]}
                components={{
                  p: ({ node, ...props }) => <p className="mb-4 last:mb-0" {...props} />,
                  h1: ({ node, ...props }) => <h1 className="text-2xl font-bold mb-4 mt-6" {...props} />,
                  h2: ({ node, ...props }) => <h2 className="text-xl font-bold mb-4 mt-6" {...props} />,
                  h3: ({ node, ...props }) => <h3 className="text-lg font-bold mb-4 mt-6" {...props} />,
                  ul: ({ node, ...props }) => <ul className="list-disc pl-6 mb-4 space-y-1" {...props} />,
                  ol: ({ node, ...props }) => <ol className="list-decimal pl-6 mb-4 space-y-1" {...props} />,
                  li: ({ node, ...props }) => <li className="marker:text-zinc-500" {...props} />,
                  a: ({ node, ...props }) => <a className="text-emerald-500 hover:text-emerald-400 underline underline-offset-2" target="_blank" rel="noopener noreferrer" {...props} />,
                  strong: ({ node, ...props }) => <strong className="font-semibold text-zinc-50" {...props} />,
                  blockquote: ({ node, ...props }) => <blockquote className="border-l-4 border-zinc-700 pl-4 italic text-zinc-400 mb-4" {...props} />,
                  table: ({ node, ...props }) => <div className="overflow-x-auto mb-4"><table className="w-full text-left border-collapse border border-zinc-700" {...props} /></div>,
                  th: ({ node, ...props }) => <th className="border border-zinc-700 bg-zinc-800/50 px-4 py-2 font-semibold" {...props} />,
                  td: ({ node, ...props }) => <td className="border border-zinc-700 px-4 py-2" {...props} />,
                  code: ({ node, className, children, ...props }) => {
                    const match = /language-(\w+)/.exec(className || '');
                    const isInline = !match && !className?.includes('language-');
                    
                    if (isInline) {
                      return (
                        <code className="bg-zinc-800 text-emerald-400 px-1.5 py-0.5 rounded-md text-sm font-mono" {...props}>
                          {children}
                        </code>
                      );
                    }
                    
                    return (
                      <CodeBlock 
                        language={match ? match[1] : 'text'} 
                        codeString={String(children).replace(/\n$/, '')} 
                      />
                    );
                  },
                }}
              >
                {message.content}
              </ReactMarkdown>
            </div>
          )}
        </div>
      </div>
    </motion.div>
  );
}