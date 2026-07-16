import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

function Surface({ className, ...props }) {
  return (
    <Card
      className={cn("overflow-hidden rounded-2xl border-zinc-200 bg-white", className)}
      {...props}
    />
  );
}

export { Surface };
