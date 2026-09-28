import type { ButtonHTMLAttributes, ReactNode } from "react";

export function Button({
  variant = "secondary",
  className = "",
  children,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary";
  children: ReactNode;
}) {
  return (
    <button
      className={`inline-flex h-11 items-center justify-center gap-2 rounded-full px-5 text-[12px] font-bold transition-colors active:scale-[.98] ${variant === "primary" ? "bg-brand text-white hover:bg-brand-hover" : "border border-[#d9d9dd] bg-canvas hover:bg-[#f8f8f8]"} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}

export function SectionLabel({ children }: { children: ReactNode }) {
  return (
    <div className="text-[10px] font-bold uppercase tracking-[.13em] text-muted">{children}</div>
  );
}
