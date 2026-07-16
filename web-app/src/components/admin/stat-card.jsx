import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

function StatCard({ label, value, helper, icon, tone = "blue", className }) {
  const tones = {
    blue: "bg-blue-50 text-blue-700",
    green: "bg-emerald-50 text-emerald-700",
    orange: "bg-orange-50 text-orange-700",
    zinc: "bg-zinc-100 text-zinc-700",
  };

  return (
    <Card className={cn("min-h-36", className)}>
      <CardContent className="p-5">
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.08em] text-zinc-400">
              {label}
            </p>
            <p className="mt-4 text-3xl font-black text-zinc-950">{value}</p>
          </div>
          {icon && (
            <div
              className={cn(
                "grid h-10 w-10 place-items-center rounded-2xl",
                tones[tone] || tones.blue,
              )}
            >
              {icon}
            </div>
          )}
        </div>
        {helper && <p className="mt-4 text-sm text-zinc-500">{helper}</p>}
      </CardContent>
    </Card>
  );
}

export { StatCard };
