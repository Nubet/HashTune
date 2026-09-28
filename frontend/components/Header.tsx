import { WaveIcon } from "./icons";

export type Page = "listen" | "library" | "history";

export function Header({ page, onPageChange }: { page: Page; onPageChange: (page: Page) => void }) {
  const links: { id: Page; label: string }[] = [
    { id: "listen", label: "Listen" },
    { id: "library", label: "Music Library" },
    { id: "history", label: "Recognition History" },
  ];
  return (
    <header className="h-14 border-b border-line bg-canvas">
      <div className="mx-auto flex h-full max-w-[1040px] items-center px-6">
        <div className="flex items-center gap-2 font-bold tracking-[-.03em]">
          <span className="grid size-7 place-items-center rounded-full bg-brand text-white">
            <WaveIcon />
          </span>
          HashTune
        </div>
        <nav className="ml-10 flex gap-7">
          {links.map((link) => (
            <button
              key={link.id}
              onClick={() => onPageChange(link.id)}
              className={`relative text-[12px] font-semibold transition-colors ${page === link.id ? "text-ink after:absolute after:-bottom-[19px] after:left-0 after:right-0 after:h-[2px] after:bg-ink" : "text-muted hover:text-ink"}`}
            >
              {link.label}
            </button>
          ))}
        </nav>
        <div className="ml-auto flex items-center gap-2 text-[10px] text-muted">
          <i className="size-1.5 rounded-full bg-[#27a36a]" />
          500 tracks indexed
        </div>
      </div>
    </header>
  );
}
