import { useQuery } from "@tanstack/react-query";
import { getSession } from "./supabase";

export function useSession() {
  return useQuery({
    queryKey: ["session"],
    queryFn: getSession,
    staleTime: Infinity,
    retry: false,
  });
}
