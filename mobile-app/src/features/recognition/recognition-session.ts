import { ApiError } from "../../lib/api/client";
import type { Recognition } from "../../lib/api/contracts";

export const sampleDurationSeconds = 12;

export type RecognitionState =
  | { status: "idle" | "preparing" | "recording" | "processing" | "canceling" }
  | { status: "success"; result: Recognition }
  | { status: "error"; error: Error };

export type RecognitionPorts = {
  prepare: (signal: AbortSignal) => Promise<void>;
  record: (durationSeconds: number) => void;
  stop: () => Promise<void>;
  uri: () => string | null;
  discard: () => Promise<void>;
  recognize: (uri: string, signal: AbortSignal) => Promise<Recognition>;
  onRecognized: () => void;
};

export class RecognitionSession {
  private state: RecognitionState = { status: "idle" };
  private listeners = new Set<() => void>();
  private operation: AbortController | null = null;
  private preparation: Promise<void> | null = null;
  private completion: Promise<void> | null = null;
  private cancellation: Promise<void> | null = null;
  private retryAt = 0;

  constructor(private readonly ports: RecognitionPorts) {}

  getSnapshot = () => this.state;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  private update(state: RecognitionState) {
    this.state = state;
    this.listeners.forEach((listener) => listener());
  }

  async start() {
    if (this.operation || this.cancellation) return;
    if (Date.now() < this.retryAt) {
      this.update({
        status: "error",
        error: new Error(
          `Please wait ${Math.ceil((this.retryAt - Date.now()) / 1000)} seconds before trying again.`,
        ),
      });
      return;
    }
    const operation = new AbortController();
    this.operation = operation;
    this.update({ status: "preparing" });
    try {
      this.preparation = this.ports.prepare(operation.signal);
      await this.preparation;
      if (operation.signal.aborted) return;
      this.update({ status: "recording" });
      this.ports.record(sampleDurationSeconds);
    } catch (error) {
      if (!operation.signal.aborted) {
        this.update({
          status: "error",
          error:
            error instanceof Error
              ? error
              : new Error("Recording could not start."),
        });
        await this.release();
        this.operation = null;
      }
    } finally {
      this.preparation = null;
    }
  }

  finish() {
    if (this.state.status !== "recording" || !this.operation)
      return this.completion ?? Promise.resolve();
    const operation = this.operation;
    this.update({ status: "processing" });
    this.completion = this.process(operation).finally(() => {
      this.completion = null;
    });
    return this.completion;
  }

  private async process(operation: AbortController) {
    try {
      await this.ports.stop();
      const uri = this.ports.uri();
      if (operation.signal.aborted) return;
      if (!uri) throw new Error("No recording was captured. Please try again.");
      const result = await this.ports.recognize(uri, operation.signal);
      if (operation.signal.aborted) return;
      this.update({ status: "success", result });
      this.ports.onRecognized();
    } catch (error) {
      if (!operation.signal.aborted) {
        if (error instanceof ApiError && error.status === 429)
          this.retryAt = Date.now() + (error.retryAfterSeconds ?? 30) * 1000;
        this.update({
          status: "error",
          error:
            error instanceof Error
              ? error
              : new Error("The audio could not be identified."),
        });
      }
    } finally {
      await this.release();
      if (this.operation === operation && !operation.signal.aborted)
        this.operation = null;
    }
  }

  cancel(reason?: string) {
    if (this.cancellation) return this.cancellation;
    if (!this.operation) return Promise.resolve();
    const operation = this.operation;
    operation.abort();
    this.update({ status: "canceling" });
    this.cancellation = (async () => {
      await this.preparation?.catch(() => undefined);
      if (this.completion) await this.completion;
      else await this.release();
      this.operation = null;
      this.update(
        reason
          ? { status: "error", error: new Error(reason) }
          : { status: "idle" },
      );
    })().finally(() => {
      this.cancellation = null;
    });
    return this.cancellation;
  }

  private async release() {
    try {
      await this.ports.stop();
    } catch (error) {
      console.error("Unable to stop the microphone", error);
    }
    try {
      await this.ports.discard();
    } catch (error) {
      console.error("Unable to remove the temporary microphone sample", error);
    }
  }
}
