import { problemSchema } from "./contracts";
import { authStore } from "../auth/store";

const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? "";

function withAuthorization(options: RequestInit): RequestInit {
  const headers = new Headers(options.headers);
  const accessToken = authStore.getSnapshot().accessToken;
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }
  return { ...options, headers, credentials: "omit" };
}

class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function request<T>(
  path: string,
  options: RequestInit,
  schema: { parse: (value: unknown) => T },
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, withAuthorization(options));
  } catch {
    throw new ApiError("Backend is unavailable", 0, "NETWORK_ERROR");
  }

  if (!response.ok) {
    const body = problemSchema.safeParse(await response.json().catch(() => null));
    throw new ApiError(
      body.data?.detail ?? "Request failed",
      response.status,
      body.data?.code ?? body.data?.title,
    );
  }

  return schema.parse(await response.json());
}

export async function requestVoid(path: string, options: RequestInit = {}) {
  const response = await fetch(`${baseUrl}${path}`, withAuthorization(options));
  if (!response.ok) {
    const body = problemSchema.safeParse(await response.json().catch(() => null));
    throw new ApiError(
      body.data?.detail ?? "Request failed",
      response.status,
      body.data?.code ?? body.data?.title,
    );
  }
}

export function multipart(file: File, field = "file") {
  const body = new FormData();
  body.append(field, file);
  return body;
}
