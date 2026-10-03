import { useSyncExternalStore } from "react";

export type Theme = "light" | "dark";

const themeChangeEvent = "hashtune-theme-change";

function subscribe(onChange: () => void) {
  window.addEventListener(themeChangeEvent, onChange);
  return () => window.removeEventListener(themeChangeEvent, onChange);
}

function getTheme(): Theme {
  return document.documentElement.classList.contains("dark") ? "dark" : "light";
}

function getServerTheme(): Theme {
  return "light";
}

export function useTheme() {
  return useSyncExternalStore(subscribe, getTheme, getServerTheme);
}

export function setTheme(theme: Theme) {
  document.documentElement.classList.toggle("dark", theme === "dark");
  localStorage.setItem("theme", theme);
  window.dispatchEvent(new Event(themeChangeEvent));
}
