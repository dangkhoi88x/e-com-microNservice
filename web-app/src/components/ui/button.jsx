import { Slot } from "@radix-ui/react-slot";
import { cva } from "class-variance-authority";
import * as React from "react";
import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-[var(--radius)] text-sm font-bold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[rgb(var(--ring))] focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        default: "bg-[rgb(var(--primary))] text-[rgb(var(--primary-foreground))] hover:bg-zinc-800",
        destructive:
          "bg-[rgb(var(--destructive))] text-[rgb(var(--destructive-foreground))] hover:bg-red-700",
        outline:
          "border border-[rgb(var(--border))] bg-[rgb(var(--card))] text-[rgb(var(--foreground))] hover:bg-[rgb(var(--secondary))]",
        secondary:
          "bg-[rgb(var(--secondary))] text-[rgb(var(--secondary-foreground))] hover:bg-zinc-200",
        ghost: "text-[rgb(var(--foreground))] hover:bg-[rgb(var(--secondary))]",
        link: "text-[rgb(var(--accent-foreground))] underline-offset-4 hover:underline",
      },
      size: {
        default: "h-10 px-4 py-2",
        sm: "h-8 rounded-xl px-3 text-xs",
        lg: "h-12 rounded-2xl px-6",
        icon: "h-10 w-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
);

function Button({ className, variant, size, asChild = false, ...props }) {
  const Comp = asChild ? Slot : "button";

  return (
    <Comp
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  );
}

export { Button, buttonVariants };
