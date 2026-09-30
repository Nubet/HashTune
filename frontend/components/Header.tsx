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
    <header className="h-16 border-b border-line bg-canvas">
      <div className="mx-auto flex h-full max-w-300 items-center px-6 lg:px-8">
        <div className="flex items-center gap-2 font-bold tracking-[-.03em]">
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
          HashTune
        </div>
        <nav className="ml-10 flex gap-7">
          {links.map((link) => (
            <button
              key={link.id}
              onClick={() => onPageChange(link.id)}
              className={`relative text-[13px] font-semibold transition-colors ${page === link.id ? "text-ink after:absolute after:-bottom-5.75 after:left-0 after:right-0 after:h-0.5 after:bg-ink" : "text-muted hover:text-ink"}`}
            >
              {link.label}
            </button>
          ))}
        </nav>
        <div className="ml-auto flex items-center gap-4 text-[11px] text-muted">
          <div className="flex items-center gap-2">
            <i className="size-1.5 rounded-full bg-success" />
            {trackCount === null ? "—" : trackCount} {trackCount === 1 ? "TRACK" : "TRACKS"}
          </div>
          <div className="h-4 w-px bg-line" />
          <ThemeToggle />
        </div>
      </div>
    </header>
  );
}
