import { cn } from "@/lib/utils";

function Input({ className, type, ...props }) {
  return (
    <input
      type={type}
      className={cn(
        "flex h-11 w-full rounded-[var(--radius)] border border-[rgb(var(--input))] bg-white px-3 py-2 text-sm text-[rgb(var(--foreground))] shadow-sm transition-colors placeholder:text-[rgb(var(--muted-foreground))] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[rgb(var(--ring))] disabled:cursor-not-allowed disabled:opacity-50",
        className,
      )}
      {...props}
    />
  );
}

export { Input };
