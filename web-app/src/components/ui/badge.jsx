import { cva } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-extrabold transition-colors focus:outline-none focus:ring-2 focus:ring-[rgb(var(--ring))] focus:ring-offset-2",
  {
    variants: {
      variant: {
        default:
          "border-transparent bg-[rgb(var(--primary))] text-[rgb(var(--primary-foreground))]",
        secondary:
          "border-transparent bg-[rgb(var(--secondary))] text-[rgb(var(--secondary-foreground))]",
        destructive:
          "border-transparent bg-[rgb(var(--destructive))] text-[rgb(var(--destructive-foreground))]",
        outline: "border-[rgb(var(--border))] text-[rgb(var(--foreground))]",
        success: "border-transparent bg-emerald-50 text-emerald-700",
        warning: "border-transparent bg-amber-50 text-amber-700",
        info: "border-transparent bg-blue-50 text-blue-700",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  },
);

function Badge({ className, variant, ...props }) {
  return <div className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge };
