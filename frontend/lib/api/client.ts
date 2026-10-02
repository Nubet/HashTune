import { problemSchema } from "./contracts";

const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://127.0.0.1:8080";

export class ApiError extends Error {
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
    response = await fetch(`${baseUrl}${path}`, options);
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
  const response = await fetch(`${baseUrl}${path}`, options);
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

export function apiUrl(path: string) {
  return `${baseUrl}${path}`;
}
