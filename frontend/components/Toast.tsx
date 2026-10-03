import { useEffect, useState } from "react";
import { InfoIcon } from "./icons";

function useToastLifecycle(message: string, onClose: () => void) {
  const [isLeaving, setIsLeaving] = useState(false);

  useEffect(() => {
    setIsLeaving(false);
    if (!message) return;
    const timer = setTimeout(() => {
      setIsLeaving(true);
    }, 2700);

    const removeTimer = setTimeout(() => {
      onClose();
    }, 3000);

    return () => {
      clearTimeout(timer);
      clearTimeout(removeTimer);
    };
  }, [message, onClose]);

  return isLeaving;
}

export function Toast({ message, onClose }: { message: string; onClose: () => void }) {
  const isLeaving = useToastLifecycle(message, onClose);

  if (!message) return null;

  return (
    <div
      className={`fixed bottom-6 left-1/2 z-50 flex -translate-x-1/2 items-center gap-2 rounded-full bg-ink/95 px-5 py-3 shadow-lg backdrop-blur-md ${isLeaving ? "animate-toast-leave" : "animate-toast-enter"}`}
      role="status"
    >
      <InfoIcon className="size-4 text-brand-pale-blue" />
      <span className="text-[0.8125rem] font-semibold tracking-wide text-white">{message}</span>
    </div>
  );
}
