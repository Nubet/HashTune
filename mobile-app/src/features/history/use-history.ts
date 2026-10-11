import {
  useInfiniteQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";
import { musicApi } from "@/lib/api/music-api";
import { useSession } from "@/lib/auth/use-session";

export function useHistory() {
  const session = useSession();
  const queryClient = useQueryClient();
  const queryKey = ["history", session.data?.user.id];
  const history = useInfiniteQuery({
    queryKey,
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => musicApi.history(pageParam, signal),
    enabled: !!session.data,
    getNextPageParam: (lastPage, _pages, lastOffset) =>
      lastPage.length === 25 ? lastOffset + lastPage.length : undefined,
  });
  const clear = useMutation({
    mutationFn: musicApi.clearHistory,
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey });
    },
    onSuccess: async () => {
      await queryClient.cancelQueries({ queryKey });
      queryClient.setQueryData(queryKey, { pages: [[]], pageParams: [0] });
    },
  });
  return { session, history, clear };
}
