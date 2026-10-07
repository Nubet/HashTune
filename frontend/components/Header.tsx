import { ThemeToggle } from "./ThemeToggle";
import { AuthControl } from "./AuthControl";

export type Page = "listen" | "library" | "history";

export function Header({ page, onPageChange }: { page: Page; onPageChange: (page: Page) => void }) {
  const links: { id: Page; label: string }[] = [
    { id: "listen", label: "Identify" },
    { id: "library", label: "Library" },
    { id: "history", label: "History" },
  ];

  return (
    <header className="sticky top-0 z-40 border-b border-line/70 bg-canvas/80 text-ink backdrop-blur-2xl transition-colors duration-300 dark:border-white/[0.04]">
      <div className="mx-auto flex h-[4.5rem] max-w-[87.5rem] items-center px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-6 lg:gap-10">
          <div className="flex items-center gap-2.5 font-bold tracking-tight text-base">
            <img
              src="/hashtune-logo-blue.svg"
              alt=""
              aria-hidden="true"
              className="app-logo-light size-7"
            />
            <img
              src="/hashtune-logo-white.svg"
              alt=""
              aria-hidden="true"
              className="app-logo-dark size-7"
            />
            <span className="hidden sm:inline-block">HashTune</span>
          </div>

          <nav className="flex gap-1 overflow-x-auto no-scrollbar sm:gap-2">
            {links.map((link) => (
              <button
                key={link.id}
                onClick={() => onPageChange(link.id)}
                className={`relative rounded-full px-3.5 py-2 text-[0.8125rem] font-semibold transition-colors sm:text-sm ${
                  page === link.id
                    ? "bg-ink text-canvas shadow-[0_0.25rem_1rem_rgba(0,0,0,0.18)]"
                    : "text-muted hover:bg-subtle hover:text-ink"
                }`}
              >
                {link.label}
              </button>
            ))}
          </nav>
        </div>

        <div className="ml-auto flex items-center gap-2">
          <AuthControl />
          <ThemeToggle />
        </div>
      </div>
    </header>
  );
}
