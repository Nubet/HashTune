import type { Recognition } from "../lib/music";
import { SectionLabel } from "./ui";

export function HistoryView({
  history,
  onClear,
  onToast,
}: {
  history: Recognition[];
  onClear: () => void;
  onToast: (message: string) => void;
}) {
  return (
    <section>
      <div className="mx-auto max-w-[1040px] px-6 py-11">
        <div className="flex items-end justify-between">
          <div>
            <SectionLabel>Recognition log</SectionLabel>
            <h1 className="mt-1 text-[32px] font-bold tracking-[-.045em]">Recognition History</h1>
          </div>
          <button
            className="text-[11px] font-bold text-brand"
            onClick={() => {
              onClear();
              onToast("History cleared");
            }}
          >
            Clear history
          </button>
        </div>
        <div className="mt-8 border-t border-line">
          {history.length ? (
            history.map((item) => (
              <div
                key={`${item.title}-${item.time}-${item.source}`}
                className="grid grid-cols-[44px_1fr_100px_110px_80px] items-center gap-4 border-b border-line py-4"
              >
                <div className="size-10" style={{ background: item.color }} />
                <div>
                  <b className="text-[12px]">{item.title}</b>
                  <div className="text-[10px] text-muted">{item.artist}</div>
                </div>
                <span className="text-[10px]">{item.score}</span>
                <span className="text-[10px] text-muted">{item.source}</span>
                <span className="text-right text-[10px] text-[#999]">{item.time}</span>
              </div>
            ))
          ) : (
            <div className="py-20 text-center text-[12px] text-[#999]">No recognitions yet.</div>
          )}
        </div>
      </div>
    </section>
  );
}
