import { describe, expect, it } from "vitest";
import { parseConfiguration } from "./config";

const environment = {
  EXPO_PUBLIC_API_URL: "http://192.168.1.100:8080",
  EXPO_PUBLIC_SUPABASE_URL: "https://project.supabase.co",
  EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY: "publishable-key",
};

describe("native configuration", () => {
  it("accepts a local API origin for development but requires HTTPS in release builds", () => {
    expect(parseConfiguration(environment).success).toBe(true);
    expect(parseConfiguration(environment, true).success).toBe(false);
  });

  it("rejects missing configuration and origins containing credentials or paths", () => {
    expect(parseConfiguration({}).success).toBe(false);
    expect(
      parseConfiguration({
        ...environment,
        EXPO_PUBLIC_API_URL: "https://token@api.test",
      }).success,
    ).toBe(false);
    expect(
      parseConfiguration({
        ...environment,
        EXPO_PUBLIC_API_URL: "https://api.test/api/v1",
      }).success,
    ).toBe(false);
  });
});
