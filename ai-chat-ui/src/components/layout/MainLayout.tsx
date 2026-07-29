// Notice we added the word 'type' right before ReactNode!
import { type ReactNode, useState } from 'react';
import { Menu, X } from 'lucide-react';

interface MainLayoutProps {
  sidebar: ReactNode;
  children: ReactNode;
}

export function MainLayout({ sidebar, children }: MainLayoutProps) {
  // 1. STATE: Keep track of whether the mobile sidebar is open or closed
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);

  return (
    // relative: Establishes a coordinate system for the absolute/fixed children
    <div className="flex h-screen w-full bg-zinc-950 text-zinc-50 font-sans overflow-hidden relative">
      
      {/* 2. MOBILE BACKDROP OVERLAY */}
      {/* 
        This only renders if the sidebar is open. 
        fixed inset-0: Stretches to cover the entire screen.
        md:hidden: Ensures it never accidentally shows on desktop.
      */}
      {isMobileSidebarOpen && (
        <div 
          className="fixed inset-0 bg-black/60 z-40 md:hidden backdrop-blur-sm transition-opacity"
          onClick={() => setIsMobileSidebarOpen(false)} // Clicking the dark background closes the sidebar
        />
      )}

      {/* 3. THE SIDEBAR CONTAINER (Desktop & Mobile) */}
      <div 
        // We use onClick capture to automatically close the mobile sidebar if the user clicks a chat link inside it
        onClickCapture={() => {
          if (window.innerWidth < 768) { // 768px is the 'md' breakpoint
            setIsMobileSidebarOpen(false);
          }
        }}
        className={`
          fixed inset-y-0 left-0 z-50 w-[260px] bg-zinc-900 border-r border-zinc-800 
          transform transition-transform duration-300 ease-in-out flex flex-col
          md:relative md:translate-x-0 
          ${isMobileSidebarOpen ? 'translate-x-0 shadow-2xl' : '-translate-x-full'}
        `}
      >
        {/* Mobile Close 'X' Button (Only visible on small screens) */}
        <div className="absolute top-3 right-3 md:hidden z-50">
          <button 
            onClick={() => setIsMobileSidebarOpen(false)} 
            className="p-1 rounded-md text-zinc-400 hover:text-white hover:bg-zinc-800 transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        {/* The actual Sidebar.tsx content passed in from App.tsx */}
        {sidebar}
      </div>

      {/* 4. MAIN CONTENT AREA */}
      <div className="flex flex-col flex-1 h-full min-w-0">
        
        {/* MOBILE TOP NAVIGATION BAR */}
        {/* hidden on desktop (md:hidden), visible on mobile */}
        <div className="md:hidden flex items-center justify-between p-3 border-b border-zinc-800 bg-zinc-900/80 backdrop-blur-md z-10">
          <div className="flex items-center gap-3">
            <button 
              onClick={() => setIsMobileSidebarOpen(true)}
              className="p-1.5 rounded-md text-zinc-400 hover:text-white hover:bg-zinc-800 transition-colors"
            >
              <Menu size={20} />
            </button>
            <span className="font-semibold text-zinc-200 text-sm">AI Chat</span>
          </div>
        </div>

        {/* The ChatWindow and ChatInput passed in from App.tsx */}
        {children}
      </div>

    </div>
  );
}