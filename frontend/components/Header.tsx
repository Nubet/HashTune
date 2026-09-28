import { ThemeToggle } from "./ThemeToggle";

export type Page = "listen" | "library" | "history";

export function Header({ page, onPageChange }: { page: Page; onPageChange: (page: Page) => void }) {
  const links: { id: Page; label: string }[] = [
    { id: "listen", label: "Listen" },
    { id: "library", label: "Music Library" },
    { id: "history", label: "Recognition History" },
  ];
  return (
    <header className="h-16 border-b border-line bg-canvas">
      <div className="mx-auto flex h-full max-w-[1200px] items-center px-6 lg:px-8">
        <div className="flex items-center gap-2 font-bold tracking-[-.03em]">
          <img
            src="/hashtune-logo-blue.svg"
            alt=""
            aria-hidden="true"
            className="size-7 dark:hidden"
          />
          <img
            src="/hashtune-logo-white.svg"
            alt=""
            aria-hidden="true"
            className="size-7 hidden dark:block"
          />
          HashTune
        </div>
        <nav className="ml-10 flex gap-7">
          {links.map((link) => (
            <button
              key={link.id}
              onClick={() => onPageChange(link.id)}
              className={`relative text-[13px] font-semibold transition-colors ${page === link.id ? "text-ink after:absolute after:-bottom-[23px] after:left-0 after:right-0 after:h-[2px] after:bg-ink" : "text-muted hover:text-ink"}`}
            >
              {link.label}
            </button>
          ))}
        </nav>
        <div className="ml-auto flex items-center gap-4 text-[11px] text-muted">
          <div className="flex items-center gap-2">
            <i className="size-1.5 rounded-full bg-success" />
            500 tracks indexed
          </div>
          <div className="h-4 w-px bg-line" />
          <ThemeToggle />
        </div>
      </div>
    </header>
  );
}
