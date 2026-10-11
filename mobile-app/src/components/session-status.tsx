import { useSession } from "@/lib/auth/use-session";
import { Button, Message } from "./ui";

export function SessionStatus() {
  const session = useSession();
  if (session.isPending)
    return (
      <Message
        title="Connecting…"
        description="Restoring your private session."
      />
    );
  if (session.error)
    return (
      <>
        <Message
          error
          title="Unable to connect"
          description={session.error.message}
        />
        <Button
          label="Try again"
          onPress={() => {
            void session.refetch();
          }}
        />
      </>
    );
  return null;
}
