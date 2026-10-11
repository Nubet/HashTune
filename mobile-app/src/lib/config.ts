import { z } from "zod";

const originSchema = z.url().refine((value) => {
  const url = new URL(value);
  return (
    ["http:", "https:"].includes(url.protocol) &&
    !url.username &&
    !url.password &&
    url.pathname === "/" &&
    !url.search &&
    !url.hash
  );
}, "Use an HTTP(S) origin without credentials, a path or query parameters.");

export function parseConfiguration(
  environment: Record<string, string | undefined>,
  production = false,
) {
  const schema = z
    .object({
      apiUrl: originSchema,
      supabaseUrl: originSchema,
      supabasePublishableKey: z.string().min(1),
    })
    .refine(
      (value) =>
        !production ||
        [value.apiUrl, value.supabaseUrl].every((url) =>
          url.startsWith("https://"),
        ),
      "Production requires HTTPS.",
    );
  return schema.safeParse({
    apiUrl: environment.EXPO_PUBLIC_API_URL,
    supabaseUrl: environment.EXPO_PUBLIC_SUPABASE_URL,
    supabasePublishableKey: environment.EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY,
  });
}

export const configuration = parseConfiguration(
  {
    EXPO_PUBLIC_API_URL: process.env.EXPO_PUBLIC_API_URL,
    EXPO_PUBLIC_SUPABASE_URL: process.env.EXPO_PUBLIC_SUPABASE_URL,
    EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY:
      process.env.EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY,
  },
  typeof __DEV__ !== "undefined" && !__DEV__,
);

export function requireConfiguration() {
  if (!configuration.success) {
    throw new Error(
      "HashTune is not configured. Set the API URL and Supabase values in mobile-app/.env and restart Expo.",
    );
  }
  return configuration.data;
}
