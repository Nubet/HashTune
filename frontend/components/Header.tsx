import { ThemeToggle } from "./ThemeToggle";

export type Page = "listen" | "library" | "history";

export function Header({
  page,
  onPageChange,
  trackCount,
}: {
  page: Page;
  onPageChange: (page: Page) => void;
  trackCount: number | null;
}) {
  const links: { id: Page; label: string }[] = [
    { id: "listen", label: "Identify" },
    { id: "library", label: "Library" },
    { id: "history", label: "History" },
  ];

  return (
    <header className="sticky top-0 z-40 border-b border-line bg-canvas/80 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-300 items-center justify-between px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-6 lg:gap-10">
          <div className="flex items-center gap-2 font-bold tracking-tight text-[15px]">
            <img
              src="/hashtune-logo-blue.svg"
              alt=""
              aria-hidden="true"
              className="logo-mark logo-mark-blue size-7"
            />
            <img
              src="/hashtune-logo-white.svg"
              alt=""
              aria-hidden="true"
              className="logo-mark logo-mark-white size-7"
            />
            <span className="hidden sm:inline-block">HashTune</span>
          </div>

          <nav className="flex gap-1 sm:gap-4 overflow-x-auto no-scrollbar">
            {links.map((link) => (
              <button
                key={link.id}
                onClick={() => onPageChange(link.id)}
                className={`relative rounded-full px-3 py-1.5 text-[13px] font-semibold transition-colors ${
                  page === link.id
                    ? "bg-ink text-canvas shadow-sm"
                    : "text-muted hover:text-ink hover:bg-subtle"
                }`}
              >
                {link.label}
              </button>
            ))}
          </nav>
        </div>

        <div className="flex items-center gap-4 text-[11px] text-muted">
          <div className="hidden sm:flex items-center gap-2 font-medium tracking-wide">
            <i
              className={`size-2 rounded-full ${trackCount === null ? "bg-muted" : "bg-success animate-pulse"}`}
            />
            {trackCount === null ? "—" : trackCount} {trackCount === 1 ? "TRACK" : "TRACKS"}
          </div>
          <div className="hidden sm:block h-4 w-px bg-line" />
          <ThemeToggle />
        </div>
      </div>
    </header>
  );
}
