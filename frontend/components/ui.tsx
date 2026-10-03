import type { ButtonHTMLAttributes, ReactNode, HTMLAttributes } from "react";
import { LoaderIcon } from "./icons";

export function Button({
  variant = "secondary",
  size = "md",
  className = "",
  isLoading = false,
  children,
  disabled,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost" | "danger";
  size?: "sm" | "md" | "lg" | "icon";
  isLoading?: boolean;
  children: ReactNode;
}) {
  const baseStyles =
    "inline-flex items-center justify-center gap-2 font-semibold transition-[color,background-color,opacity,transform,box-shadow] active:scale-[.98] disabled:pointer-events-none disabled:opacity-50 outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-canvas";

  const variants = {
    primary: "bg-brand text-white shadow-sm hover:bg-brand-hover hover:shadow-md",
    secondary:
      "border border-line bg-canvas hover:bg-subtle hover:border-muted/30 text-ink shadow-sm",
    ghost: "text-muted hover:text-ink hover:bg-subtle",
    danger:
      "bg-red-50 text-red-600 hover:bg-red-100 dark:bg-red-950/30 dark:hover:bg-red-900/50 dark:text-red-400",
  };

  const sizes = {
    sm: "h-9 rounded-full px-4 text-xs",
    md: "h-11 rounded-full px-6 text-[0.8125rem]",
    lg: "h-14 rounded-full px-8 text-[0.9375rem]",
    icon: "h-10 w-10 rounded-full",
  };

  return (
    <button
      className={`${baseStyles} ${variants[variant]} ${sizes[size]} ${className}`}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading && <LoaderIcon className="animate-spin size-4" />}
      {!isLoading && children}
    </button>
  );
}

export function SectionLabel({
  children,
  className = "",
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={`mb-2 text-xs font-bold uppercase tracking-[0.15em] text-muted ${className}`}>
      {children}
    </div>
  );
}

export function Skeleton({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={`animate-pulse rounded-md bg-muted/20 ${className}`} {...props} />;
}

export function Badge({
  children,
  variant = "default",
}: {
  children: ReactNode;
  variant?: "default" | "success" | "danger" | "warning";
}) {
  const variants = {
    default: "bg-subtle text-muted",
    success: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400",
    danger: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
    warning: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
  };

  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-[0.625rem] font-bold tracking-wide uppercase ${variants[variant]}`}
    >
      {children}
    </span>
  );
}
