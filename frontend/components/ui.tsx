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
      className={`inline-flex h-12 items-center justify-center gap-2 rounded-full px-6 text-[13px] font-bold transition-colors active:scale-[.98] ${variant === "primary" ? "bg-brand text-white hover:bg-brand-hover" : "border border-line bg-canvas hover:bg-subtle"} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}

export function SectionLabel({ children }: { children: ReactNode }) {
  return (
    <div className="text-[11px] font-bold uppercase tracking-[.13em] text-muted">{children}</div>
  );
}
