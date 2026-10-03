export type Page = "listen" | "library" | "history";

export function Header({ page, onPageChange }: { page: Page; onPageChange: (page: Page) => void }) {
  const links: { id: Page; label: string }[] = [
    { id: "listen", label: "Identify" },
    { id: "library", label: "Library" },
    { id: "history", label: "History" },
  ];

  return (
    <header className="sticky top-0 z-40 border-b border-white/[0.04] bg-[#0a0a0c]/40 text-white backdrop-blur-2xl">
      <div className="mx-auto flex h-[4.5rem] max-w-[87.5rem] items-center px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-6 lg:gap-10">
          <div className="flex items-center gap-2.5 font-bold tracking-tight text-base">
            <img src="/hashtune-logo-white.svg" alt="" aria-hidden="true" className="size-7" />
            <span className="hidden sm:inline-block">HashTune</span>
          </div>

          <nav className="flex gap-1 overflow-x-auto no-scrollbar sm:gap-2">
            {links.map((link) => (
              <button
                key={link.id}
                onClick={() => onPageChange(link.id)}
                className={`relative rounded-full px-3.5 py-2 text-[0.8125rem] font-semibold transition-colors sm:text-sm ${
                  page === link.id
                    ? "bg-white text-[#0b2544] shadow-[0_0.25rem_1rem_rgba(0,0,0,0.18)]"
                    : "text-white/65 hover:bg-white/10 hover:text-white"
                }`}
              >
                {link.label}
              </button>
            ))}
          </nav>
        </div>
      </div>
    </header>
  );
}
