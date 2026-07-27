import { cn } from "@/lib/utils";

function Card({ className, ...props }) {
  return (
    <div
      className={cn(
        "rounded-2xl border border-[rgb(var(--border))] bg-[rgb(var(--card))] text-[rgb(var(--card-foreground))] shadow-[0_20px_60px_rgba(15,23,42,0.06)]",
        className,
      )}
      {...props}
    />
  );
}

function CardHeader({ className, ...props }) {
  return <div className={cn("flex flex-col space-y-1.5 p-6", className)} {...props} />;
}

function CardTitle({ className, ...props }) {
  return (
    <h3
      className={cn("text-xl font-black leading-none tracking-tight", className)}
      {...props}
    />
  );
}

function CardDescription({ className, ...props }) {
  return (
    <p
      className={cn("text-sm text-[rgb(var(--muted-foreground))]", className)}
      {...props}
    />
  );
}

function CardContent({ className, ...props }) {
  return <div className={cn("p-6 pt-0", className)} {...props} />;
}

function CardFooter({ className, ...props }) {
  return <div className={cn("flex items-center p-6 pt-0", className)} {...props} />;
}

export { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle };
