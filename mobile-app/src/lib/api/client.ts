import type { z } from "zod";
import { problemSchema } from "./contracts";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code: string,
    readonly retryAfterSeconds: number | null = null,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function retryAfterSeconds(
  value: string | null,
  now = Date.now(),
): number | null {
  if (!value) return null;
  const seconds = Number(value);
  if (Number.isFinite(seconds) && seconds >= 0) return Math.ceil(seconds);
  const date = Date.parse(value);
  return Number.isFinite(date)
    ? Math.max(0, Math.ceil((date - now) / 1000))
    : null;
}

export function createApiClient({
  baseUrl,
  fetch,
  getToken,
  refreshToken,
}: {
  baseUrl: () => string;
  fetch: (url: string, options: RequestInit) => Promise<Response>;
  getToken: () => Promise<string>;
  refreshToken: () => Promise<string>;
}) {
  return async function request<T>(
    path: string,
    schema: z.ZodType<T>,
    options: RequestInit = {},
    authenticated = true,
  ): Promise<T> {
    const controller = new AbortController();
    const abort = () => controller.abort();
    const timeout = setTimeout(abort, 60_000);
    options.signal?.addEventListener("abort", abort, { once: true });
    if (options.signal?.aborted) abort();

    try {
      const headers = new Headers(options.headers);
      if (authenticated)
        headers.set("Authorization", `Bearer ${await getToken()}`);
      const url = `${baseUrl().replace(/\/$/, "")}${path}`;
      const send = () =>
        fetch(url, { ...options, headers, signal: controller.signal });
      let response = await send();
      if (authenticated && response.status === 401) {
        headers.set("Authorization", `Bearer ${await refreshToken()}`);
        response = await send();
      }
      if (!response.ok) {
        const problem = problemSchema.safeParse(
          await response.json().catch(() => null),
        );
        const wait = retryAfterSeconds(response.headers.get("Retry-After"));
        const message =
          response.status === 429
            ? wait === null
              ? "Too many requests. Please try again later."
              : `Too many requests. Try again in ${wait} seconds.`
            : (problem.data?.detail ??
              (response.status === 401
                ? "Your session could not be verified. Please try again."
                : "The request could not be completed."));
        throw new ApiError(
          message,
          response.status,
          problem.data?.code ?? "HTTP_ERROR",
          wait,
        );
      }
      const body: unknown =
        response.status === 204
          ? undefined
          : await response.json().catch(() => {
              throw new ApiError(
                "The backend returned an unexpected response.",
                response.status,
                "INVALID_RESPONSE",
              );
            });
      const parsed = schema.safeParse(body);
      if (!parsed.success)
        throw new ApiError(
          "The backend returned an unexpected response.",
          response.status,
          "INVALID_RESPONSE",
        );
      return parsed.data;
    } catch (error) {
      if (error instanceof ApiError) throw error;
      if (controller.signal.aborted) {
        throw new ApiError(
          options.signal?.aborted
            ? "Request canceled."
            : "The request timed out. Check your connection and try again.",
          0,
          options.signal?.aborted ? "CANCELED" : "TIMEOUT",
        );
      }
      if (error instanceof TypeError)
        throw new ApiError(
          "Unable to reach HashTune. Check your connection and try again.",
          0,
          "NETWORK_ERROR",
        );
      throw error;
    } finally {
      clearTimeout(timeout);
      options.signal?.removeEventListener("abort", abort);
    }
  };
}
